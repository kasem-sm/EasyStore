/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import kasem.sm.easystore.processor.generator.DsFunctionsGenerator
import kasem.sm.easystore.processor.generator.dsImportNameGenerator
import kasem.sm.easystore.processor.ksp.checkIfReturnTypeExists
import kasem.sm.easystore.processor.ksp.getStoreAnnotationArgs
import kasem.sm.easystore.processor.ksp.toKClass
import kasem.sm.easystore.processor.validators.validateFunctionNameAlreadyExistsOrNot
import kasem.sm.easystore.processor.validators.validatePreferenceKeyIsUniqueOrNot
import kasem.sm.easystore.processor.validators.validateStoreArgs

internal data class Imports(
    val packageName: String,
    val names: List<String>
)

class StoreVisitor(
    private val logger: KSPLogger,
    private val resolver: Resolver
) : KSVisitorVoid() {

    internal lateinit var className: String
    internal lateinit var packageName: String

    internal val generatedFunctions = mutableListOf<FunSpec>()
    internal val generatedProperties = mutableListOf<PropertySpec>()
    internal val generatedImports = mutableListOf<Imports>()

    private val generator = DsFunctionsGenerator()

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        if (classDeclaration.classKind != ClassKind.INTERFACE) {
            logger.error("Only interface can be annotated with @EasyStore", classDeclaration)
        }

        packageName = classDeclaration.packageName.asString()
        className = "${classDeclaration.simpleName.asString()}Factory"

        classDeclaration
            .getAllFunctions()
            .toList()
            .forEach {
                visitFunctionDeclaration(it, Unit)
            }
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        val annotationArguments = function.annotations.firstOrNull()?.arguments ?: return
        val (preferenceKeyName, getterFunctionName) = annotationArguments.getStoreAnnotationArgs()
        val preferenceKeyPropertyName = "${preferenceKeyName.uppercase()}_KEY"
        val functionName = function.simpleName.getShortName()

        validateStoreArgs(
            preferenceKeyName = preferenceKeyName,
            getterFunctionName = getterFunctionName,
            functionName = functionName
        ) { logger.error(it) }

        generatedFunctions.validateFunctionNameAlreadyExistsOrNot(
            currentFunctionName = getterFunctionName
        ) { logger.error(it) }

        generatedProperties.validatePreferenceKeyIsUniqueOrNot(
            currentPropertyName = preferenceKeyPropertyName
        ) { logger.error(it) }

        if (function.parameters.isEmpty()) {
            logger.error(
                "Functions annotated with @Store should have at least one parameter. " +
                    "The function, $functionName doesn't have any."
            )
            return
        }

        if (function.parameters.size > 1) {
            logger.error(
                "Functions annotated with @Store can only have one parameter. " +
                    "The function, $functionName has more than one."
            )
            return
        }

        function.checkIfReturnTypeExists(logger)

        val resolverBuiltIns = resolver.builtIns
        val functionParameterType = function.parameters[0].type.resolve()

        val functionParameterKClass = functionParameterType.toKClass(resolverBuiltIns)
        if (functionParameterKClass == null) {
            logger.error("$functionName parameter type $functionParameterType is not supported by Datastore yet!")
            return
        }

        functionParameterType.dsImportNameGenerator(resolverBuiltIns) { name ->
            generatedImports.add(Imports("androidx.datastore.preferences.core", listOf(name)))
        }

        generator
            .generateDSAddFunction(
                function = function,
                preferenceKeyPropertyName = preferenceKeyPropertyName
            ).apply {
                generatedFunctions.add(this)
            }

        generator
            .generateDSGetFunction(
                functionName = getterFunctionName,
                preferenceKeyPropertyName = preferenceKeyPropertyName,
                parameterType = functionParameterKClass
            ).apply {
                generatedFunctions.add(this)
            }

        generator
            .generateDSKeyProperty(
                functionParameterType = functionParameterType,
                resolverBuiltIns = resolverBuiltIns,
                preferenceKeyName = preferenceKeyName,
                functionParameterKClass = functionParameterKClass
            ).apply {
                generatedProperties.add(this)
            }
    }
}
