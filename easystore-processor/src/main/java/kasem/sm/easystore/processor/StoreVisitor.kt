/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor

import com.google.devtools.ksp.innerArguments
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.toClassName
import kasem.sm.easystore.core.Retrieve
import kasem.sm.easystore.core.Store
import kasem.sm.easystore.processor.generator.DsFactoryClassGenerator
import kasem.sm.easystore.processor.ksp.checkIfReturnTypeExists
import kasem.sm.easystore.processor.ksp.isDataClass
import kasem.sm.easystore.processor.ksp.isEnumClass
import kasem.sm.easystore.processor.ksp.supportedTypes

class StoreVisitor(
    private val logger: KSPLogger,
    resolver: Resolver
) : KSVisitorVoid() {

    internal lateinit var className: ClassName
    internal lateinit var packageName: String
    private val factoryClassGenerator = DsFactoryClassGenerator(logger, resolver)

    internal var generatedFunctions = listOf<FunSpec>()
    internal var generatedProperties = listOf<PropertySpec>()
    internal var generatedImportNames = listOf<String>()

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        if (classDeclaration.classKind != ClassKind.INTERFACE) {
            logger.error("Only interface can be annotated with @EasyStore", classDeclaration)
            return
        }

        packageName = classDeclaration.packageName.asString()
        className = classDeclaration.toClassName()

        val functions = classDeclaration.getAllFunctions().toList()

        functions
            .filter {
                it.annotations.firstOrNull()?.shortName?.asString() == Store::class.simpleName
            }.filter {
                if (it.modifiers.firstOrNull() != Modifier.SUSPEND) {
                    logger.error("Functions annotated with @Store should be used with suspend keyword.")
                    return
                }
                it.modifiers.firstOrNull() == Modifier.SUSPEND
            }
            .forEach {
                visitFunctionDeclaration(it, Unit)
            }

        functions
            .filter {
                it.annotations.firstOrNull()?.shortName?.asString() == Retrieve::class.simpleName
            }
            .filter {
                if (it.modifiers.firstOrNull() == Modifier.SUSPEND) {
                    logger.error("Functions annotated with @Retrieve should not have the suspend keyword.")
                    return
                }
                it.modifiers.firstOrNull() != Modifier.SUSPEND
            }
            .forEach {
                visitFunctionDeclaration(it, Unit)
            }
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        val functionName = function.simpleName.asString()
        val functionAnnotation = function.annotations
        val functionParameters = function.parameters
        val functionParameter = functionParameters.firstOrNull()
        val functionReturnType = function.returnType?.resolve()
        val functionAnnotationName = functionAnnotation.firstOrNull()?.shortName?.asString()

        if (functionAnnotationName == Store::class.simpleName) {
            val prefKeyName = functionAnnotation.first().arguments[0].value as String
            if (functionParameter == null) {
                logger.error(
                    "Functions annotated with @Store should have at least one parameter. " +
                            "The function, $functionName doesn't have any."
                )
                return
            }

            val parameter = functionParameter.type.resolve()

            if (functionParameters.size > 1) {
                logger.error(
                    "Functions annotated with @Store can only have one parameter. " +
                            "The function, $functionName has more than one."
                )
                return
            }

            function.checkIfReturnTypeExists(logger)

            when {
                supportedTypes.find { type -> parameter.toClassName() == type } != null -> false
                parameter.isEnumClass -> false
                parameter.isDataClass -> false
                else -> true
            }.also {
                if (it) {
                    logger.error("Function $functionName parameter type $parameter is not supported by Datastore yet!")
                    return
                }
            }

            factoryClassGenerator
                .initialize(
                    function = function,
                    preferenceKeyName = prefKeyName,
                    functionParameterType = parameter,
                    functionAnnotationName = functionAnnotationName!!
                )
                .initiateFunctionWithStoreArgs()
        } else if (functionAnnotationName == Retrieve::class.simpleName) {
            val prefKeyName = functionAnnotation.first().arguments[0].value as String
            if (functionParameter == null) {
                logger.error(
                    "Functions annotated with @Retrieve should have a parameter that is of the same type as the function's return type. " +
                            "For example: If a function returns Flow<String> then the parameter of the function should be of type String which will be used by EasyStore as the default value while retrieving nullable String data from DataStore Preferences. " +
                            "The function, $functionName doesn't have any."
                )
                return
            }

            val parameter = functionParameter.type.resolve()

            if (functionParameters.size > 1) {
                logger.error(
                    "Functions annotated with @Retrieve can only have one parameter. " +
                            "The function, $functionName has more than one."
                )
                return
            }

            // Retrieve
            if (functionReturnType == null || functionReturnType.toClassName().simpleName != "Flow") {
                logger.error(
                    "Functions annotated with @Retrieve should return Flow<PARAM_TYPE>. " +
                            "($functionName)"
                )
                return
            }

            if (functionReturnType.toClassName().simpleName == "Flow") {
                // Check for inner args
                val innerType = functionReturnType.innerArguments.first().type?.resolve() ?: return

                if (innerType.toClassName().simpleName != parameter.toClassName().simpleName
                ) {
                    logger.error("The return type for the function $functionName should be Flow<${parameter.toClassName().simpleName}> as the parameter type is ${parameter.toClassName().simpleName}.")
                    return
                }

                if (parameter.isDataClass) {
                    function.initiate(
                        parameter = parameter,
                        functionName = functionName,
                        preferenceKeyName = prefKeyName
                    )
                } else factoryClassGenerator
                    .initialize(
                        function = function,
                        preferenceKeyName = prefKeyName,
                        functionParameterType = parameter,
                        functionAnnotationName = functionAnnotationName!!
                    ).initiateFunctionsWithRetrieveArgs()
            } else {
                logger.error("return type name is not flow, ${functionReturnType.toClassName().simpleName}")
                return
            }
        } else return

        generatedFunctions = (factoryClassGenerator.generatedFunctions)
        generatedProperties = (factoryClassGenerator.generatedProperties.map { it.spec })
        generatedImportNames = (factoryClassGenerator.generatedImportNames)
    }

    private fun KSFunctionDeclaration.initiate(
        parameter: KSType,
        functionName: String,
        preferenceKeyName: String,
    ) {
        val dataClass: KSClassDeclaration = parameter.declaration as KSClassDeclaration

        val unSupportedParamName = mutableListOf<String>()

        val areDataClassPropertiesSupported =
            dataClass.getAllProperties().toList()
                .map { property ->
                    val toClass = property.type.resolve()
                    if (toClass.isDataClass) {
                        // Nested data class
                        // Not supported as of now
                        logger.error("Nested data class is not supported by EasyStore. (${toClass.toClassName().simpleName})")
                        return
                    }
                    Pair(
                        supportedTypes.find { type -> toClass.toClassName() == type } != null || toClass.isEnumClass,
                        property.simpleName.asString()
                    )
                }.also { list ->
                    list.filter { (b, _) ->
                        !b
                    }.forEach { (_, s) ->
                        unSupportedParamName.add(s)
                    }
                }

        if (areDataClassPropertiesSupported.any { (b, _) -> !b }) {
            logger.error("$unSupportedParamName parameter(s) of the class linked to the function $functionName are not supported by Datastore yet!")
            return
        }

        factoryClassGenerator
            .initialize(
                function = this,
                functionParameterType = parameter,
                functionAnnotationName = Retrieve::class.simpleName!!,
                preferenceKeyName = preferenceKeyName
            ).initiateFunctionsWithRetrieveArgs()
    }
}
