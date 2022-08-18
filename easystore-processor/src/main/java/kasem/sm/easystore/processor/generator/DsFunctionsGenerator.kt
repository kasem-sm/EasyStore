/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.generator

import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kasem.sm.easystore.processor.ksp.toDataStoreKey
import kasem.sm.easystore.processor.ksp.toKClass2
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

        val codeBlock = when {
            functionParameterType == resolverBuiltIns.intType -> """
                     intPreferencesKey("$preferenceKeyName")
            """.trimIndent()
            functionParameterType == resolverBuiltIns.stringType -> """
                     stringPreferencesKey("$preferenceKeyName")
            """.trimIndent()
            functionParameterType == resolverBuiltIns.doubleType -> """
                    doublePreferencesKey("$preferenceKeyName")
            """.trimIndent()
            functionParameterType == resolverBuiltIns.booleanType -> """
                    booleanPreferencesKey("$preferenceKeyName")
            """.trimIndent()
            functionParameterType == resolverBuiltIns.floatType -> """
                        floatPreferencesKey("$preferenceKeyName")
            """.trimIndent()
            functionParameterType == resolverBuiltIns.longType -> """
                        longPreferencesKey("$preferenceKeyName")
            """.trimIndent()
            functionParameterType.declaration.modifiers.first() == Modifier.ENUM -> """
                     stringPreferencesKey("$preferenceKeyName")
            """.trimIndent()
            else -> {
                throw UnknownError()
            }
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
        preferenceKeyPropertyName: String,
        resolverBuiltIns: KSBuiltIns
    ): FunSpec {
        val functionParamType = function.parameters[0].type.resolve()

        val actualFunctionParameterName = function.parameters[0].name?.getShortName()
        val actualFunctionType = functionParamType.toClassName()
        val actualFunctionName = function.simpleName.asString()
        // Check if it's enum and not String::class
        val afterElvis =
            if (functionParamType.toKClass2(resolverBuiltIns)!! == Enum::class.asClassName()) {
                "value.name"
            } else "value"

        return FunSpec.builder(
            name = actualFunctionName
        ).apply {
            addModifiers(KModifier.SUSPEND)
            addParameter(
                name = actualFunctionParameterName ?: "value",
                type = actualFunctionType
            )
            addCode(
                CodeBlock.of(
                    """
                            dataStore.edit { preferences ->
                                preferences[$preferenceKeyPropertyName] = $afterElvis
                            }
                    """.trimIndent()
                )
            )
        }.build()
    }

    fun generateDSGetFunction(
        functionParameterType: ClassName,
        functionName: String,
        preferenceKeyPropertyName: String,
        parameterType: ClassName
    ): FunSpec {
        val paramType = if (parameterType == Enum::class.asClassName()) {
            functionParameterType
        } else parameterType

        val codeBlock = if (parameterType == Enum::class.asClassName()) {
            """
                return dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { preference ->
                    $paramType.valueOf(preference[$preferenceKeyPropertyName] ?: defaultValue.name)
                }
            """.trimIndent()
        } else {
            """
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
        }

        return FunSpec.builder(
            name = functionName
        ).apply {
            addParameter("defaultValue", paramType)
            returns(Flow::class.asClassName().parameterizedBy(paramType))
            addCode(codeBlock)
        }.build()
    }
}
