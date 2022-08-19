/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.generator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.toClassName
import kasem.sm.easystore.processor.ksp.checkIfReturnTypeExists
import kasem.sm.easystore.processor.ksp.getAllProperties
import kasem.sm.easystore.processor.ksp.isDataClass
import kasem.sm.easystore.processor.ksp.isEnumClass
import kasem.sm.easystore.processor.ksp.supportedTypes
import kasem.sm.easystore.processor.validators.validateFunctionNameAlreadyExistsOrNot
import kasem.sm.easystore.processor.validators.validatePreferenceKeyIsUniqueOrNot
import kasem.sm.easystore.processor.validators.validateStoreArgs

internal class DsFactoryClassGenerator(
    private val function: KSFunctionDeclaration,
    private val logger: KSPLogger
) {
    private val generatedFunctions = mutableListOf<FunSpec>()
    private val generatedProperties = mutableListOf<PropertySpec>()
    private val generatedImportNames = mutableListOf<String>()
    private val generator = DsFunctionsGenerator()

    fun initiate(
        preferenceKeyName: List<String>,
        preferenceKeyType: List<KSType>,
        getterFunctionName: String
    ): Triple<List<FunSpec>, List<PropertySpec>, List<String>>? {
        val preferenceKeyPropertyName = preferenceKeyName.map {
            "${it.uppercase()}_KEY"
        }

        val functionName = function.simpleName.getShortName()

        preferenceKeyName.forEach {
            validateStoreArgs(
                preferenceKeyName = it,
                getterFunctionName = getterFunctionName,
                functionName = functionName
            ) { logger.error(it) }
        }

        generatedFunctions.validateFunctionNameAlreadyExistsOrNot(
            currentFunctionName = getterFunctionName
        ) { logger.error(it) }

        preferenceKeyPropertyName.forEach {
            generatedProperties.validatePreferenceKeyIsUniqueOrNot(
                currentPropertyName = it
            ) { logger.error(it) }
        }

        val functionParameterType =
            function.parameters.getOrNull(0)?.type?.resolve() ?: kotlin.run {
                logger.error(
                    "Functions annotated with @Store should have at least one parameter. " +
                        "The function, $functionName doesn't have any."
                )
                return null
            }

        if (function.parameters.size > 1) {
            logger.error(
                "Functions annotated with @Store can only have one parameter. " +
                    "The function, $functionName has more than one."
            )
            return null
        }

        function.checkIfReturnTypeExists(logger)

        val functionParameterKClass = functionParameterType.toClassName()

        val showError = when {
            supportedTypes.find { functionParameterKClass == it } != null -> false
            functionParameterType.isEnumClass -> false
            functionParameterType.isDataClass -> false
            else -> true
        }

        if (showError) {
            logger.error("$functionName parameter type $functionParameterType is not supported by Datastore yet!")
            return null
        }

        if (preferenceKeyPropertyName.isNotEmpty()) {
            if (functionParameterType.isDataClass) {
                functionParameterType.getAllProperties().map {
                    it.type.resolve()
                }.forEach {
                    it.dsImportNameGenerator { generatedImportNames.add(it) }
                }
            }
        }

        functionParameterType.dsImportNameGenerator { name ->
            generatedImportNames.add(name)
        }

        generator
            .generateDSAddFunction(
                actualFunctionName = functionName,
                actualFunctionParameterName = function.parameters[0].name?.getShortName(),
                functionParamType = functionParameterType,
                preferenceKeyPropertyName = preferenceKeyPropertyName
            ).apply {
                generatedFunctions.add(this)
            }

        generator
            .generateDSGetFunction(
                functionParameterType = functionParameterType,
                functionName = getterFunctionName,
                preferenceKeyPropertyName = preferenceKeyPropertyName
            ).apply {
                generatedFunctions.add(this)
            }

        if (functionParameterType.isDataClass) {
            preferenceKeyType.zip(preferenceKeyName).forEach { (type, keyName) ->
                generator
                    .generateDSKeyProperty(
                        functionParameterType = functionParameterType,
                        preferenceKeyName = keyName,
                        preferenceKeyType = type
                    ).apply {
                        generatedProperties.add(this)
                    }
            }
        } else {
            preferenceKeyName.forEach {
                generator
                    .generateDSKeyProperty(
                        functionParameterType = functionParameterType,
                        preferenceKeyName = it,
                        preferenceKeyType = null
                    ).apply {
                        generatedProperties.add(this)
                    }
            }
        }

        return Triple(generatedFunctions, generatedProperties, generatedImportNames)
    }
}
