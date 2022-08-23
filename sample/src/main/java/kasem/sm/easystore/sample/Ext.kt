/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.sample

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun AppCompatActivity.withRepeatOnLifecycle(
    state: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch(block = { repeatOnLifecycle(state, block) })
}

fun AppCompatActivity.dialogWithOptions(
    items: Array<String>,
    singleChoiceItems: Boolean = true,
    selectedItem: Int?,
    title: String,
    setOnItemClick: (position: Int, dialog: DialogInterface) -> Unit,
) {
    MaterialAlertDialogBuilder(this).apply {
        setTitle(title)
        if (singleChoiceItems) {
            setSingleChoiceItems(items, selectedItem ?: -1) { d, pos ->
                setOnItemClick(pos, d)
            }
        } else {
            setItems(items) { d, pos ->
                setOnItemClick(pos, d)
            }
        }
        show()
    }
}

fun getSelectedThemeOption(themes: Theme): Int {
    return when (themes) {
        Theme.LIGHT -> 0
        Theme.DARK -> 1
        Theme.SYSTEM -> 2
    }
}
