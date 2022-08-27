/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.generator

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

internal fun KSType.dsImportNameGenerator(
    import: (String) -> Unit
) {
    when (declaration.simpleName.asString()) {
        Int::class.simpleName -> import("intPreferencesKey")
        String::class.simpleName -> import("stringPreferencesKey")
        Double::class.simpleName -> import("doublePreferencesKey")
        Boolean::class.simpleName -> import("booleanPreferencesKey")
        Float::class.simpleName -> import("floatPreferencesKey")
        Long::class.simpleName -> import("longPreferencesKey")
        Set::class.asClassName().parameterizedBy(String::class.asTypeName()).rawType.simpleName -> import("stringSetPreferencesKey")
        else -> {
            if (declaration.modifiers.first() == Modifier.ENUM) {
                import("stringPreferencesKey")
            }
        }
    }
}
