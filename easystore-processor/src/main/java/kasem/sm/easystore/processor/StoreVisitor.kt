/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import kasem.sm.easystore.core.Link
import kasem.sm.easystore.core.Store
import kasem.sm.easystore.processor.generator.DsFactoryClassGenerator
import kasem.sm.easystore.processor.ksp.getStoreAnnotationArgs
import kasem.sm.easystore.processor.ksp.isDataClass

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

//        var kClass: ClassName
        functions.filter {
            it.annotations.firstOrNull()?.shortName?.asString() == Link::class.simpleName
        }.forEach {
//            val functionArgs = it.annotations.toList()[0].arguments.firstOrNull() ?: return
//            kClass = (functionArgs.value as KSType)::class.asClassName()

            val parameter = it.parameters[0].type.resolve()
//            if (parameter::class.asClassName() == kClass) {
            if (parameter.isDataClass) {
                val dataClass: KSClassDeclaration = parameter.declaration as KSClassDeclaration
                val annotation = dataClass.annotations.firstOrNull()

                if (annotation?.shortName?.asString() == Store::class.simpleName) {
                    val annotationArguments = annotation?.arguments ?: return
                    val (preferenceKeyName, getterFunctionName) = annotationArguments.getStoreAnnotationArgs()

                    val factoryClassGenerator = DsFactoryClassGenerator(it, logger)

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
            }
        }
    }
}
