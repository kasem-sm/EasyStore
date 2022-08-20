/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.generator

import com.google.devtools.ksp.symbol.KSType
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
import kasem.sm.easystore.processor.ksp.toPreferenceKeyCode
import kotlinx.coroutines.flow.Flow

internal class DsFunctionsGenerator {

    fun generateDSKeyProperty(
        preferenceKeyType: List<KSType>,
        preferenceKeyName: List<String>
    ): List<PropertySpec> {
        return preferenceKeyType.zip(preferenceKeyName).map { (type, keyName) ->
            val preferenceKeyPropertyName = "${keyName.uppercase()}_KEY"

            buildPropertyType(
                ksType = type,
                preferenceKeyName = keyName,
                propertyName = preferenceKeyPropertyName
            )
        }
    }

    fun generateDSKeyProperty(
        functionParameterType: KSType,
        preferenceKeyName: String
    ): PropertySpec {
        val preferenceKeyPropertyName = "${preferenceKeyName.uppercase()}_KEY"

        return buildPropertyType(
            ksType = functionParameterType,
            preferenceKeyName = preferenceKeyName,
            propertyName = preferenceKeyPropertyName
        )
    }

    fun generateDSAddFunction(
        actualFunctionName: String,
        actualFunctionParameterName: String?,
        functionParamType: KSType,
        preferenceKeyPropertyName: List<String>
    ): FunSpec {
        val isEnum = functionParamType.isEnumClass
        val isDataClass = functionParamType.isDataClass

        // Check if it's enum and not String::class
        val afterElvis = if (isEnum) {
            (actualFunctionParameterName ?: "value") + ".name"
        } else actualFunctionParameterName ?: "value"

        val editBlock = if (isDataClass) {
            var addBlock = ""
            functionParamType.getAllProperties().zip(preferenceKeyPropertyName).forEach {
                val type = it.first.type.resolve()
                val afterInnerElvis = if (type.isEnumClass) {
                    "$actualFunctionParameterName.${it.first.simpleName.asString()}.name\n"
                } else "$actualFunctionParameterName.${it.first.simpleName.asString()}\n"
                addBlock += "preferences[${it.second}] ?: $afterInnerElvis"
            }
            addBlock
        } else "preferences[${preferenceKeyPropertyName[0]}] = $afterElvis"

        val codeBlock = """ 
                    |dataStore.edit { preferences ->
                    |    $editBlock
                    |}
        """.trimMargin()

        return FunSpec.builder(
            name = actualFunctionName
        ).apply {
            addModifiers(KModifier.SUSPEND)
            addParameter(
                name = actualFunctionParameterName ?: "value",
                type = functionParamType.toClassName()
            )
            addCode(CodeBlock.of(codeBlock))
        }.build()
    }

    fun generateDSGetFunction(
        functionParameterType: KSType,
        functionName: String,
        preferenceKeyPropertyName: List<String>
    ): FunSpec {
        val paramType = functionParameterType.toClassName()

        val mapBlock = if (!functionParameterType.isDataClass) {
            if (functionParameterType.isEnumClass) {
                "$paramType.valueOf(preference[${preferenceKeyPropertyName[0]}] ?: defaultValue.name)"
            } else "preference[${preferenceKeyPropertyName[0]}] ?: defaultValue"
        } else {
            var codeBlock = ""
            functionParameterType.getAllProperties().zip(preferenceKeyPropertyName).forEach {
                val type = it.first.type.resolve()
                codeBlock += if (type.isEnumClass) {
                    "${type.toClassName()}.valueOf(preference[${preferenceKeyPropertyName[0]}] ?: defaultValue.${it.first.simpleName.asString()}.name),\n"
                } else "preference[${it.second}] ?: defaultValue.${it.first.simpleName.asString()},\n"
            }
            "$paramType($codeBlock)"
        }

        val codeBlock =
            """ |return dataStore.data
                |.catch { exception ->
                |    if (exception is IOException) {
                |        emit(emptyPreferences())
                |    } else {
                |        throw exception
                |    }
                |}.map { preference ->
                |    $mapBlock
                |}
            """.trimMargin()

        return FunSpec.builder(
            name = functionName
        ).apply {
            addParameter("defaultValue", paramType)
            returns(Flow::class.asClassName().parameterizedBy(paramType))
            addCode(codeBlock)
        }.build()
    }

    companion object {
        private fun buildPropertyType(
            ksType: KSType,
            preferenceKeyName: String,
            propertyName: String
        ): PropertySpec {
            val dataStoreKeyType = ksType.toDataStoreKey().parameterizedBy(
                if (ksType.isEnumClass) {
                    String::class.asClassName()
                } else ksType.toClassName()
            )

            val codeBlock = ksType.declaration.simpleName.asString()
                .toPreferenceKeyCode(
                    preferenceKeyName = preferenceKeyName,
                    isEnum = ksType.isEnumClass
                )

            return PropertySpec.builder(
                name = propertyName,
                type = dataStoreKeyType
            ).apply {
                addModifiers(KModifier.PRIVATE)
                initializer(CodeBlock.of(codeBlock))
            }.build()
        }
    }
}
