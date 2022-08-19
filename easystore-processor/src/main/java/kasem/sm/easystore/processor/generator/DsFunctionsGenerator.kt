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
import kasem.sm.easystore.processor.ksp.getAllProperties
import kasem.sm.easystore.processor.ksp.isDataClass
import kasem.sm.easystore.processor.ksp.isEnumClass
import kasem.sm.easystore.processor.ksp.toDataStoreKey
import kotlinx.coroutines.flow.Flow

internal class DsFunctionsGenerator {

    fun generateDSKeyProperty(
        functionParameterType: KSType,
        preferenceKeyName: String,
        preferenceKeyType: KSType?
    ): PropertySpec {
        val preferenceKeyPropertyName = "${preferenceKeyName.uppercase()}_KEY"

        val dataStoreKeyType = if (functionParameterType.isDataClass) {
            preferenceKeyType!!.toDataStoreKey().parameterizedBy(preferenceKeyType.toClassName())
        } else functionParameterType.toDataStoreKey().parameterizedBy(
            if (functionParameterType.isEnumClass) {
                String::class.asClassName()
            } else functionParameterType.toClassName()
        )

        fun String.toPreferenceKeyCode(): String {
            return when (this) {
                Int::class.simpleName -> "intPreferencesKey(\"$preferenceKeyName\")"
                String::class.simpleName -> "stringPreferencesKey(\"$preferenceKeyName\")"
                Double::class.simpleName -> "doublePreferencesKey(\"$preferenceKeyName\")"
                Boolean::class.simpleName -> "booleanPreferencesKey(\"$preferenceKeyName\")"
                Float::class.simpleName -> "floatPreferencesKey(\"$preferenceKeyName\")"
                Long::class.simpleName -> "longPreferencesKey(\"$preferenceKeyName\")"
                else -> {
                    if (functionParameterType.isEnumClass) {
                        "stringPreferencesKey(\"$preferenceKeyName\")"
                    } else throw Exception()
                }
            }
        }

        val codeBlock = if (functionParameterType.isDataClass) {
            preferenceKeyType!!.declaration.simpleName.asString().toPreferenceKeyCode()
        } else functionParameterType.declaration.simpleName.asString().toPreferenceKeyCode()

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
        preferenceKeyPropertyName: List<String>
    ): FunSpec {
        val isEnum = functionParamType.declaration.modifiers.firstOrNull() == Modifier.ENUM

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
            if (preferenceKeyPropertyName.size == 1) {
                addCode(
                    """ |dataStore.edit { preferences ->
                    |    preferences[${preferenceKeyPropertyName[0]}] = $afterElvis
                    |}
                    """.trimMargin()
                )
            } else {
                addCode(
                    """
                    // Data Class
                    """.trimIndent()
                )
            }
        }.build()
    }

    fun generateDSGetFunction(
        functionParameterType: KSType,
        functionName: String,
        preferenceKeyPropertyName: List<String>
    ): FunSpec {
        val paramType = if (functionParameterType.isEnumClass) {
            functionParameterType.toClassName()
        } else functionParameterType.toClassName()

        val codeBlock = if (preferenceKeyPropertyName.size == 1) {
            if (functionParameterType.isEnumClass) {
                """ |return dataStore.data
                |.catch { exception ->
                |    if (exception is IOException) {
                |        emit(emptyPreferences())
                |    } else {
                |        throw exception
                |    }
                |}.map { preference ->
                |    $paramType.valueOf(preference[${preferenceKeyPropertyName[0]}] ?: defaultValue.name)
                |}
                """.trimMargin()
            } else {
                """ |return dataStore.data
                |.catch { exception ->
                |    if (exception is IOException) {
                |        emit(emptyPreferences())
                |    } else {
                |        throw exception
                |    }
                |}.map { preference ->
                |    preference[${preferenceKeyPropertyName[0]}] ?: defaultValue
                |}
                """.trimMargin()
            }
        } else {
            var codeBlock = ""
            functionParameterType.getAllProperties().zip(preferenceKeyPropertyName).forEach {
                codeBlock += "preference[${it.second}] ?: defaultValue.${it.first.simpleName.asString()},\n"
            }

            """ |return dataStore.data
                |.catch { exception ->
                |    if (exception is IOException) {
                |        emit(emptyPreferences())
                |    } else {
                |        throw exception
                |    }
                |}.map { preference ->
                |   $paramType($codeBlock)
                |}
            """.trimMargin()
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
