/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.processor.validators

import com.google.devtools.ksp.processing.KSPLogger
import kasem.sm.easystore.core.Store
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

    if (preferenceKeyName.containsSpecialChars()) {
        logger.error("preferenceKeyName should not contain any special characters.")
        return
    }
}

internal fun List<PropKey>.validatePreferenceKeyIsUniqueOrNot(
    currentPropertyName: String,
    logger: KSPLogger
) {
    filter {
        it.annotationName == Store::class.simpleName && it.spec.name == currentPropertyName
    }.also {
        if (it.size > 1) {
            logger.error("preferenceKeyName is not unique. Every function that are annotated with @Store should have a unique key name.")
            return
        }
    }

    filter {
        it.annotationName == Retention::class.simpleName && it.spec.name == currentPropertyName
    }.also {
        if (it.size > 1) {
            logger.error("preferenceKeyName is not unique. Every function that are annotated with @Retrieve should have a unique key name.")
            return
        }
    }
}

internal fun String.containsSpecialChars(): Boolean {
    val regex = Regex("^[a-zA-Z0-9_]*$")
    return !regex.matches(this)
}
