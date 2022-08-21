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
import kasem.sm.easystore.processor.ksp.getAllProperties
import kasem.sm.easystore.processor.ksp.isDataClass
import kasem.sm.easystore.processor.validators.validatePreferenceKeyIsUniqueOrNot
import kasem.sm.easystore.processor.validators.validateStoreArgs

data class PropKey(
    val spec: PropertySpec,
    val annotationName: String
)

internal class DsFactoryClassGenerator(
    private val logger: KSPLogger,
) {
    internal val generatedFunctions = mutableListOf<FunSpec>()
    internal val generatedProperties = mutableListOf<PropKey>()
    internal val generatedImportNames = mutableListOf<String>()

    private val generator = DsFunctionsGenerator()
    private lateinit var preferenceKeyName: List<String>
    private lateinit var preferenceKeyPropertyName: List<String>
    private lateinit var functionName: String
    private lateinit var function: KSFunctionDeclaration
    private lateinit var functionParameterType: KSType
    private lateinit var functionAnnotationName: String

    fun initialize(
        function: KSFunctionDeclaration,
        preferenceKeyName: List<String>,
        functionParameterType: KSType,
        functionAnnotationName: String
    ) = apply {
        this.functionAnnotationName = functionAnnotationName
        this.function = function
        this.preferenceKeyName = preferenceKeyName
        preferenceKeyPropertyName = this.preferenceKeyName.map {
            "${it.uppercase()}_KEY"
        }
        this.functionParameterType = functionParameterType
        functionName = function.simpleName.getShortName()

        this.preferenceKeyName.forEach {
            validateStoreArgs(
                preferenceKeyName = it,
                functionName = functionName,
                logger = logger
            )
        }

        this.preferenceKeyPropertyName.forEach {
            generatedProperties.validatePreferenceKeyIsUniqueOrNot(
                currentPropertyName = it,
                logger = logger
            )
        }
    }

    fun initiateFunctionWithStoreArgs() {
        // Generate DS Key Properties
        if (functionParameterType.isDataClass) {
            val dataClass: KSClassDeclaration =
                functionParameterType.declaration as KSClassDeclaration

            // Generate imports for all Data class properties
            functionParameterType.getAllProperties().map {
                it.type.resolve()
            }.forEach {
                it.dsImportNameGenerator { generatedImportNames.add(it) }
            }

            val preferenceKeyTypeFromDataClass =
                dataClass.getAllProperties().toList().map { property ->
                    property.type.resolve()
                }

            val preferenceKeysFromDataClass =
                dataClass.getAllProperties().toList().map { property ->
                    (functionParameterType.toClassName().simpleName + "_" + property.simpleName.asString() + "_key").uppercase()
                }

            // Generate Keys
            generator
                .generateDSKeyProperty(
                    preferenceKeyType = preferenceKeyTypeFromDataClass,
                    preferenceKeyName = preferenceKeysFromDataClass
                ).onEach {
                    generatedProperties.add(PropKey(it, functionAnnotationName))
                }

            // Generate Add Function
            generator
                .generateDSAddFunction(
                    functionName = functionName,
                    functionParamType = functionParameterType,
                    preferenceKeyPropertyName = preferenceKeysFromDataClass,
                    functionParameterName = function.parameters[0].name?.asString() ?: "value"
                ).apply {
                    generatedFunctions.add(this)
                }
        } else {
            // Generate import or the param type
            generatedProperties.find {
                it.spec.name == preferenceKeyName[0]
            }.also {
                if (it != null){
                    generateImportStatementAndPropKey()
                }
            }

            // Generate Add Function
            generator
                .generateDSAddFunction(
                    functionName = functionName,
                    functionParamType = functionParameterType,
                    preferenceKeyPropertyName = preferenceKeyPropertyName,
                    functionParameterName = function.parameters[0].name?.asString() ?: "value"
                ).apply {
                    generatedFunctions.add(this)
                }
        }
    }

    private fun generateImportStatementAndPropKey() {
        functionParameterType.dsImportNameGenerator { name ->
            generatedImportNames.add(name)
        }

        // Generate Keys
        generator
            .generateDSKeyProperty(
                functionParameterType = functionParameterType,
                preferenceKeyName = preferenceKeyName[0]
            ).apply {
                generatedProperties.add(PropKey(this, functionAnnotationName))
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

            generator
                .generateDSGetFunction(
                    functionName = functionName,
                    functionParameterType = functionParameterType,
                    preferenceKeyPropertyName = preferenceKeysFromDataClass
                ).apply {
                    generatedFunctions.add(this)
                }
        } else {
            generatedProperties.find {
                it.spec.name == preferenceKeyName[0]
            }.also {
                if (it == null){
                    generateImportStatementAndPropKey()
                }
            }

            generator
                .generateDSGetFunction(
                    functionName = functionName,
                    functionParameterType = functionParameterType,
                    preferenceKeyPropertyName = preferenceKeyPropertyName
                ).apply {
                    generatedFunctions.add(this)
                }
        }
    }
}
