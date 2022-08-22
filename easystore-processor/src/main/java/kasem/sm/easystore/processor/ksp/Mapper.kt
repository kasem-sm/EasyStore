/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.ksp

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName

// TODO("Add custom exceptions")

internal fun KSType.toDataStoreKey(): ClassName {
    return when (declaration.simpleName.asString()) {
        Int::class.simpleName -> intPreferencesKey("")::class
        String::class.simpleName -> stringPreferencesKey("")::class
        Double::class.simpleName -> doublePreferencesKey("")::class
        Boolean::class.simpleName -> booleanPreferencesKey("")::class
        Float::class.simpleName -> floatPreferencesKey("")::class
        Long::class.simpleName -> longPreferencesKey("")::class
        else -> {
            if (isEnumClass || isDataClass) {
                stringPreferencesKey("")::class
            } else throw UnknownError()
        }
    }.asClassName()
}

internal val supportedTypes = listOf(
    Int::class,
    String::class,
    Double::class,
    Boolean::class,
    Float::class,
    Long::class,
    Enum::class
).map {
    it.asClassName()
}

internal fun String.toPreferenceKeyCode(preferenceKeyName: String, isEnum: Boolean): String {
    return when (this) {
        Int::class.simpleName -> "intPreferencesKey(\"$preferenceKeyName\")"
        String::class.simpleName -> "stringPreferencesKey(\"$preferenceKeyName\")"
        Double::class.simpleName -> "doublePreferencesKey(\"$preferenceKeyName\")"
        Boolean::class.simpleName -> "booleanPreferencesKey(\"$preferenceKeyName\")"
        Float::class.simpleName -> "floatPreferencesKey(\"$preferenceKeyName\")"
        Long::class.simpleName -> "longPreferencesKey(\"$preferenceKeyName\")"
        else -> {
            if (isEnum) {
                "stringPreferencesKey(\"$preferenceKeyName\")"
            } else throw Exception()
        }
    }
}
