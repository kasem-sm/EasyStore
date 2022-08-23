/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
@file:Suppress("CAST_NEVER_SUCCEEDS")

package kasem.sm.easystore.processor.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ksp.toClassName

internal fun KSFunctionDeclaration.checkIfReturnTypeExists(logger: KSPLogger) {
    val returnTypeAsString = returnType?.resolve()?.declaration?.simpleName?.asString()
    if (returnTypeAsString != null && returnTypeAsString != "Unit") {
        logger.error("You shouldn't add any return value to the function annotated with @Store.")
        return
    }
}

fun KSType.getAllProperties(): List<KSPropertyDeclaration> {
    if (isDataClass) {
        val dataClass: KSClassDeclaration = declaration as KSClassDeclaration
        return dataClass.getAllProperties().toList()
    }
    return emptyList()
}

internal val KSType.isEnumClass get() = declaration.modifiers.firstOrNull() == Modifier.ENUM

internal val KSType.isDataClass get() = declaration.modifiers.firstOrNull() == Modifier.DATA

internal val KSType.isStringSet get() = toClassName().simpleName == "Set"
