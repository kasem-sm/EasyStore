/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.generator

import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier

internal fun KSType.dsImportNameGenerator(
    resolverBuiltIns: KSBuiltIns,
    import: (String) -> Unit
) {
    when (this) {
        resolverBuiltIns.intType -> import("intPreferencesKey")
        resolverBuiltIns.stringType -> import("stringPreferencesKey")
        resolverBuiltIns.doubleType -> import("doublePreferencesKey")
        resolverBuiltIns.booleanType -> import("booleanPreferencesKey")
        resolverBuiltIns.floatType -> import("floatPreferencesKey")
        resolverBuiltIns.longType -> import("longPreferencesKey")
        else -> {
            if (declaration.modifiers.first() == Modifier.ENUM) {
                import("stringPreferencesKey")
            }
        }
    }
}
