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
import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName

internal fun KSType.toDataStoreKey(
    resolverBuiltIns: KSBuiltIns,
    preferenceKeyName: String
): ClassName {
    return when (this) {
        resolverBuiltIns.intType -> intPreferencesKey(preferenceKeyName)::class.asClassName()
        resolverBuiltIns.stringType -> stringPreferencesKey(preferenceKeyName)::class.asClassName()
        resolverBuiltIns.doubleType -> doublePreferencesKey(preferenceKeyName)::class.asClassName()
        resolverBuiltIns.booleanType -> booleanPreferencesKey(preferenceKeyName)::class.asClassName()
        resolverBuiltIns.floatType -> floatPreferencesKey(preferenceKeyName)::class.asClassName()
        resolverBuiltIns.longType -> longPreferencesKey(preferenceKeyName)::class.asClassName()
        else -> throw UnknownError()
    }
}

internal fun KSType.toKClass(
    resolverBuiltIns: KSBuiltIns
): ClassName? {
    return when (this) {
        resolverBuiltIns.intType -> Int::class.asClassName()
        resolverBuiltIns.stringType -> String::class.asClassName()
        resolverBuiltIns.doubleType -> Double::class.asClassName()
        resolverBuiltIns.booleanType -> Boolean::class.asClassName()
        resolverBuiltIns.floatType -> Float::class.asClassName()
        resolverBuiltIns.longType -> Long::class.asClassName()
        else -> return null
    }
}
