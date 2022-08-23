/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.generator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.toClassName
import kasem.sm.easystore.core.Store
import kasem.sm.easystore.processor.ksp.getAllProperties
import kasem.sm.easystore.processor.ksp.isDataClass
import kasem.sm.easystore.processor.validators.validatePreferenceKeyIsUniqueOrNot
import kasem.sm.easystore.processor.validators.validateStoreArgs

data class PropKey(
    val spec: PropertySpec,
    val annotationName: String
)

internal class DsFactoryClassGenerator(
    private val logger: KSPLogger
) {
    internal val generatedFunctions = mutableListOf<FunSpec>()
    internal val generatedProperties = mutableListOf<PropKey>()
    internal val generatedImportNames = mutableListOf<String>()

    private val generator = DsFunctionsGenerator()
    private lateinit var preferenceKeyName: String
    private lateinit var preferenceKeyPropertyName: String
    private lateinit var functionName: String
    private lateinit var function: KSFunctionDeclaration
    private lateinit var functionParameterType: KSType
    private lateinit var functionAnnotationName: String

    fun initialize(
        function: KSFunctionDeclaration,
        preferenceKeyName: String,
        functionParameterType: KSType,
        functionAnnotationName: String
    ): DsFactoryClassGenerator = apply {
        this.functionAnnotationName = functionAnnotationName
        this.function = function
        this.preferenceKeyName = preferenceKeyName
        this.functionParameterType = functionParameterType

        preferenceKeyPropertyName = "${preferenceKeyName.uppercase()}_KEY"
        functionName = function.simpleName.getShortName()

        validateStoreArgs(
            preferenceKeyName = preferenceKeyName,
            functionName = functionName,
            logger = logger
        )

        generatedProperties.validatePreferenceKeyIsUniqueOrNot(
            currentPropertyName = preferenceKeyPropertyName,
            logger = logger
        )
    }

    fun initiateFunctionWithStoreArgs() {
        // Generate DS Key Properties
        if (functionParameterType.isDataClass) {
            val dataClass: KSClassDeclaration =
                functionParameterType.declaration as KSClassDeclaration

            // Generate imports for all Data class properties
            functionParameterType.getAllProperties().map { property ->
                val type = property.type.resolve()
                // Nested data class
                // Not supported as of now
                if (type.isDataClass) {
                    logger.error("Nested data class is not supported by EasyStore. (${type.toClassName().simpleName})")
                    return
                }
                type
            }.forEach {
                it.dsImportNameGenerator { generatedImportNames.add(it) }
            }

            val properties = dataClass.getAllProperties().toList()

            val preferenceKeyTypeFromDataClass = properties.map { property ->
                property.type.resolve()
            }

            val preferenceKeyPropertyNameFromDataClass = properties.map { property ->
                (functionParameterType.toClassName().simpleName + "_" + property.simpleName.asString() + "_key").uppercase()
            }

            // Generate Property Keys
            generator
                .generateDSKeyProperty(
                    preferenceKeyType = preferenceKeyTypeFromDataClass,
                    preferenceKeyName = preferenceKeyPropertyNameFromDataClass
                ).onEach {
                    generatedProperties.add(PropKey(it, functionAnnotationName))
                }

            // Generate Add Function
            generator
                .generateDSAddFunction(
                    functionName = functionName,
                    functionParamType = functionParameterType,
                    preferenceKeyPropertyName = preferenceKeyPropertyNameFromDataClass,
                    functionParameterName = function.parameters[0].name?.asString() ?: "value"
                ).apply {
                    generatedFunctions.add(this)
                }
        } else {
            functionParameterType.dsImportNameGenerator { name ->
                generatedImportNames.add(name)
            }

            // Generate Property Keys
            generator
                .generateDSKeyProperty(
                    functionParameterType = functionParameterType,
                    preferenceKeyName = preferenceKeyName
                ).apply {
                    generatedProperties.add(PropKey(this, functionAnnotationName))
                }

            // Generate Add Function
            generator
                .generateDSAddFunction(
                    functionName = functionName,
                    functionParamType = functionParameterType,
                    preferenceKeyPropertyName = listOf(preferenceKeyPropertyName),
                    functionParameterName = function.parameters[0].name?.asString() ?: "value",
                ).apply {
                    generatedFunctions.add(this)
                }
        }
    }

    fun initiateFunctionsWithRetrieveArgs() {
        if (functionParameterType.isDataClass) {
            val dataClass: KSClassDeclaration =
                functionParameterType.declaration as KSClassDeclaration

            val preferenceKeysFromDataClass =
                dataClass.getAllProperties().toList().map { property ->
                    (functionParameterType.toClassName().simpleName + "_" + property.simpleName.asString() + "_key").uppercase()
                }

            generatedProperties
                .filter { it.annotationName == Store::class.simpleName }
                .find { it.spec.name == preferenceKeysFromDataClass[0] }
                .also {
                    if (it == null) {
                        // TODO("Improve Error Message")
                        logger.error("Before retrieving any data, please use @Store with $preferenceKeyName preferenceKeyName.")
                        return
                    }
                }

            // Generate Get Function
            generator
                .generateDSGetFunction(
                    functionParameterType = functionParameterType,
                    functionName = functionName,
                    preferenceKeyPropertyName = preferenceKeysFromDataClass,
                    parameterName = function.parameters[0].name?.asString() ?: "defaultValue"
                ).apply {
                    generatedFunctions.add(this)
                }
        } else {
            generatedProperties
                .filter { it.annotationName == Store::class.simpleName }
                .find { it.spec.name == preferenceKeyPropertyName }
                .also {
                    if (it == null) {
                        // TODO("Improve Error Message")
                        logger.error("Before retrieving any data, please use @Store with $preferenceKeyName preferenceKeyName.")
                        return
                    }
                }

            // Generate Get Function
            generator
                .generateDSGetFunction(
                    functionParameterType = functionParameterType,
                    functionName = functionName,
                    preferenceKeyPropertyName = listOf(preferenceKeyPropertyName),
                    parameterName = function.parameters[0].name?.asString() ?: "defaultValue"
                ).apply {
                    generatedFunctions.add(this)
                }
        }
    }
}
