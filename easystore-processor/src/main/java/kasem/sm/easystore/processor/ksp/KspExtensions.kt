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
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.Modifier

internal fun List<KSValueArgument>.getStoreAnnotationArgs(): Pair<String, String> {
    return Pair(get(0).value as String, get(1).value as String)
}

internal fun KSFunctionDeclaration.checkIfReturnTypeExists(logger: KSPLogger) {
    val functionName = simpleName.asString()
    val returnTypeAsString = returnType?.resolve()?.declaration?.simpleName?.asString()
    if (returnTypeAsString != null && returnTypeAsString != "Unit") {
        logger.error(
            "You shouldn't add any return value to the function annotated with @Store. " +
                "PS: The function's parameter type would also be it's return type but wrapped inside Kotlin Flow. " +
                "For example, the function $functionName would return Flow<$returnTypeAsString>."
        )
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
