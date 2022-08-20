/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.toClassName
import kasem.sm.easystore.core.Link
import kasem.sm.easystore.core.Store
import kasem.sm.easystore.processor.generator.DsFactoryClassGenerator
import kasem.sm.easystore.processor.ksp.getStoreAnnotationArgs
import kasem.sm.easystore.processor.ksp.isDataClass
import kasem.sm.easystore.processor.ksp.isEnumClass
import kasem.sm.easystore.processor.ksp.supportedTypes

class StoreVisitor(
    private val logger: KSPLogger
) : KSVisitorVoid() {

    internal lateinit var className: String
    internal lateinit var packageName: String

    internal val generatedFunctions = mutableListOf<FunSpec>()
    internal val generatedProperties = mutableListOf<PropertySpec>()
    internal val generatedImportNames = mutableListOf<String>()

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        if (classDeclaration.classKind != ClassKind.INTERFACE) {
            logger.error("Only interface can be annotated with @EasyStore", classDeclaration)
            return
        }

        packageName = classDeclaration.packageName.asString()
        className = "${classDeclaration.simpleName.asString()}Factory"

        val functions = classDeclaration.getAllFunctions().toList()

        // Separate Store and Map annotated functions
        functions.filter {
            it.annotations.firstOrNull()?.shortName?.asString() == Store::class.simpleName
        }.forEach {
            val annotationArguments = it.annotations.firstOrNull()?.arguments ?: return
            val (preferenceKeyName, getterFunctionName) = annotationArguments.getStoreAnnotationArgs()

            DsFactoryClassGenerator(it, logger)
                .initiate(listOf(preferenceKeyName), emptyList(), getterFunctionName)
                .also { triple ->
                    triple?.let {
                        generatedFunctions.addAll(it.first)
                        generatedProperties.addAll(it.second)
                        generatedImportNames.addAll(it.third)
                    }
                }
        }

        functions.filter {
            it.annotations.firstOrNull()?.shortName?.asString() == Link::class.simpleName
        }.forEach {
            val functionArgs = it.annotations.toList()[0].arguments.firstOrNull() ?: return

            val parameter = it.parameters[0].type.resolve()
            if (parameter.toClassName().simpleName == (functionArgs.value as KSType).toClassName().simpleName) {
                val showError = when {
                    supportedTypes.find { type -> parameter.toClassName() == type } != null -> false
                    parameter.isEnumClass -> false
                    parameter.isDataClass -> false
                    else -> true
                }

                if (showError) {
                    logger.error("Function ${it.simpleName.asString()} parameter type $parameter is not supported by Datastore yet!")
                    return
                }

                if (parameter.isDataClass) {
                    val dataClass: KSClassDeclaration = parameter.declaration as KSClassDeclaration
                    val annotation = dataClass.annotations.filter { ks ->
                        ks.shortName.asString() == Store::class.simpleName
                    }.firstOrNull()

                    if (annotation != null) {
                        if (annotation.shortName.asString() == Store::class.simpleName) {
                            val annotationArguments = annotation.arguments
                            val (preferenceKeyName, getterFunctionName) = annotationArguments.getStoreAnnotationArgs()

                            val factoryClassGenerator = DsFactoryClassGenerator(it, logger)

                            val unSupportedParamName = mutableListOf<String>()

                            val areDataClassPropertiesSupported =
                                dataClass.getAllProperties().toList()
                                    .map { property ->
                                        val toClass = property.type.resolve()
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
                                logger.error("$unSupportedParamName parameter(s) of the class linked to the function ${it.simpleName.asString()} are not supported by Datastore yet!")
                                return
                            }

                            val preferenceKeysFromDataClass =
                                dataClass.getAllProperties().toList().map { property ->
                                    preferenceKeyName + "_" + property.simpleName.asString()
                                }

                            val preferenceKeyTypeFromDataClass =
                                dataClass.getAllProperties().toList().map { property ->
                                    property.type.resolve()
                                }

                            val returns = factoryClassGenerator.initiate(
                                preferenceKeyName = preferenceKeysFromDataClass,
                                getterFunctionName = getterFunctionName,
                                preferenceKeyType = preferenceKeyTypeFromDataClass
                            )

                            if (returns != null) {
                                generatedFunctions.addAll(returns.first)
                                generatedProperties.addAll(returns.second)
                                generatedImportNames.addAll(returns.third)
                            } else {
                                logger.error("Factory returned null values")
                                return
                            }
                        }
                    } else {
                        logger.error("The class linked inside @Link should be annotated with @Store")
                        return
                    }
                }
            } else {
                logger.error("The function parameter type and the class linked with @Link are not of the same type.")
                return
            }
        }
    }
}
