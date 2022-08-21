/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.validators

import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import kasem.sm.easystore.processor.generator.PropKey

internal fun validateStoreArgs(
    preferenceKeyName: String,
    functionName: String,
    logger: KSPLogger
) {
    if (preferenceKeyName.isEmpty()) {
        logger.error("preferenceKeyName for $functionName is empty.")
        return
    }

    if (preferenceKeyName.containsSpecialChars() ) {
        logger.error("preferenceKeyName should not contain any special characters.")
        return
    }
}

internal fun List<PropKey>.validatePreferenceKeyIsUniqueOrNot(
    currentPropertyName: String,
    logger: KSPLogger
) {
    val spec = filter {
        it.annotationName == "Store"
    }.find {
        it.spec.name == currentPropertyName
    }

    if (spec != null) {
        logger.error("preferenceKeyName is not unique. Every function that are annotated with @Store should have a unique key name.")
        return
    }

    val spec2 = filter {
        it.annotationName == "Retrieve"
    }.find {
        it.spec.name == currentPropertyName
    }

    if (spec2 != null) {
        logger.error("preferenceKeyName is not unique. Every function that are annotated with @Retrieve should have a unique key name.")
        return
    }
}

internal fun String.containsSpecialChars(): Boolean {
    val regex = Regex("^[a-zA-Z0-9_]*$")
    return !regex.matches(this)
}
