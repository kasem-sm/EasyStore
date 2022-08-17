/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.validators

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec

internal fun validateStoreArgs(
    preferenceKeyName: String,
    getterFunctionName: String,
    functionName: String,
    errorMessage: (String) -> Unit
) {
    if (preferenceKeyName.isEmpty()) {
        errorMessage("preferenceKeyName for $functionName is empty.")
        return
    }

    if (getterFunctionName.isEmpty()) {
        errorMessage("getterFunctionName for $functionName is empty.")
        return
    }

    if (preferenceKeyName.containsSpecialChars() || getterFunctionName.containsSpecialChars()) {
        errorMessage("preferenceKeyName or the getterFunctionName should not contain any special characters.")
        return
    }
}

internal fun List<FunSpec>.validateFunctionNameAlreadyExistsOrNot(
    currentFunctionName: String,
    errorMessage: (String) -> Unit
) {
    val spec = find {
        it.name == currentFunctionName
    }
    if (spec != null) {
        errorMessage("The getterFunctionName is same for two or more functions annotated with @Store.")
        return
    }
}

internal fun List<PropertySpec>.validatePreferenceKeyIsUniqueOrNot(
    currentPropertyName: String,
    errorMessage: (String) -> Unit
) {
    find {
        it.name == currentPropertyName
    }.apply {
        if (this != null) {
            errorMessage("preferenceKeyName is not unique. Every function that are annotated with @Store should have a unique key name.")
            return
        }
    }
}

internal fun String.containsSpecialChars(): Boolean {
    val regex = Regex("^[a-zA-Z0-9_]*$")
    return !regex.matches(this)
}
