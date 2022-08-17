/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.generator

import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kasem.sm.easystore.processor.ksp.toDataStoreKey
import kotlinx.coroutines.flow.Flow

internal class DsFunctionsGenerator {

    fun generateDSKeyProperty(
        functionParameterType: KSType,
        resolverBuiltIns: KSBuiltIns,
        preferenceKeyName: String,
        functionParameterKClass: ClassName
    ): PropertySpec {
        val preferenceKeyPropertyName = "${preferenceKeyName.uppercase()}_KEY"

        val dataStoreKeyType = functionParameterType.toDataStoreKey(
            resolverBuiltIns,
            preferenceKeyName
        ).parameterizedBy(functionParameterKClass)

        val codeBlock = when (functionParameterType) {
            resolverBuiltIns.intType -> """
                     intPreferencesKey("$preferenceKeyName")
            """.trimIndent()
            resolverBuiltIns.stringType -> """
                     stringPreferencesKey("$preferenceKeyName")
            """.trimIndent()
            resolverBuiltIns.doubleType -> """
                    doublePreferencesKey("$preferenceKeyName")
            """.trimIndent()
            resolverBuiltIns.booleanType -> """
                    booleanPreferencesKey("$preferenceKeyName")
            """.trimIndent()
            resolverBuiltIns.floatType -> """
                        floatPreferencesKey("$preferenceKeyName")
            """.trimIndent()
            resolverBuiltIns.longType -> """
                        longPreferencesKey("$preferenceKeyName")
            """.trimIndent()
            else -> throw UnknownError()
        }

        return PropertySpec.builder(
            name = preferenceKeyPropertyName,
            type = dataStoreKeyType
        ).apply {
            addModifiers(KModifier.PRIVATE)
            initializer(CodeBlock.of(codeBlock))
        }.build()
    }

    fun generateDSAddFunction(
        function: KSFunctionDeclaration,
        preferenceKeyPropertyName: String
    ): FunSpec {
        val actualFunctionParameter = function.parameters[0].name?.getShortName()
        val actualFunctionName = function.simpleName.asString()
        val type = function.parameters[0].type.resolve().toClassName()

        return FunSpec.builder(
            name = actualFunctionName
        ).apply {
            addModifiers(KModifier.SUSPEND)
            addParameter(
                name = actualFunctionParameter ?: "value",
                type = type
            )
            addCode(
                CodeBlock.of(
                    """
                            dataStore.edit { preferences ->
                                preferences[$preferenceKeyPropertyName] = $actualFunctionParameter
                            }
                    """.trimIndent()
                )
            )
        }.build()
    }

    fun generateDSGetFunction(
        functionName: String,
        preferenceKeyPropertyName: String,
        parameterType: ClassName
    ): FunSpec {
        val codeBlock = """
                return dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { preference ->
                    preference[$preferenceKeyPropertyName] ?: defaultValue
                }
        """.trimIndent()

        return FunSpec.builder(
            name = functionName
        ).apply {
            addParameter("defaultValue", parameterType)
            returns(Flow::class.asClassName().parameterizedBy(parameterType))
            addCode(codeBlock)
        }.build()
    }
}
