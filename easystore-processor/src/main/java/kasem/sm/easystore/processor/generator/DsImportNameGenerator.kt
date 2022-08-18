/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.generator

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier

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
        else -> {
            if (declaration.modifiers.first() == Modifier.ENUM) {
                import("stringPreferencesKey")
            }
        }
    }
}
