/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.generator

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kasem.sm.easystore.processor.ksp.isEnumClass
import kasem.sm.easystore.processor.ksp.toDataStoreKey
import kotlinx.coroutines.flow.Flow

internal class DsFunctionsGenerator {

    fun generateDSKeyProperty(
        functionParameterType: KSType,
        preferenceKeyName: String
    ): PropertySpec {
        val preferenceKeyPropertyName = "${preferenceKeyName.uppercase()}_KEY"

        val dataStoreKeyType = functionParameterType.toDataStoreKey().parameterizedBy(
            if (functionParameterType.isEnumClass) {
                String::class.asClassName()
            } else functionParameterType.toClassName()
        )

        val codeBlock = when (functionParameterType.declaration.simpleName.asString()) {
            Int::class.simpleName -> "intPreferencesKey(\"$preferenceKeyName\")"
            String::class.simpleName -> "stringPreferencesKey(\"$preferenceKeyName\")"
            Double::class.simpleName -> "doublePreferencesKey(\"$preferenceKeyName\")"
            Boolean::class.simpleName -> "booleanPreferencesKey(\"$preferenceKeyName\")"
            Float::class.simpleName -> "floatPreferencesKey(\"$preferenceKeyName\")"
            Long::class.simpleName -> "longPreferencesKey(\"$preferenceKeyName\")"
            else -> {
                if (functionParameterType.declaration.modifiers.first() == Modifier.ENUM) {
                    "stringPreferencesKey(\"$preferenceKeyName\")"
                } else throw Exception()
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
        actualFunctionName: String,
        actualFunctionParameterName: String?,
        functionParamType: KSType,
        preferenceKeyPropertyName: String
    ): FunSpec {
        val isEnum = functionParamType.isEnumClass

        // Check if it's enum and not String::class
        val afterElvis = if (isEnum) {
            (actualFunctionParameterName ?: "value") + ".name"
        } else actualFunctionParameterName ?: "value"

        return FunSpec.builder(
            name = actualFunctionName
        ).apply {
            addModifiers(KModifier.SUSPEND)
            addParameter(
                name = actualFunctionParameterName ?: "value",
                type = functionParamType.toClassName()
            )
            addCode(
                CodeBlock.of(
                    """
                        dataStore.edit { preferences ->
                            preferences[$preferenceKeyPropertyName] = $afterElvis
                        }
                    """
                        .trimIndent()
                )
            )
        }.build()
    }

    fun generateDSGetFunction(
        functionParameterType: KSType,
        functionName: String,
        preferenceKeyPropertyName: String,
        actualFunctionParameter: KSType
    ): FunSpec {
        val paramType = if (functionParameterType.isEnumClass) {
            actualFunctionParameter.toClassName()
        } else functionParameterType.toClassName()

        val codeBlock = if (functionParameterType.isEnumClass) {
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
