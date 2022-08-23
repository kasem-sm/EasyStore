/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.sample

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kasem.sm.easystore.core.EasyStore
import kasem.sm.easystore.core.Retrieve
import kasem.sm.easystore.core.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// EasyStore will generate an implementation of this interface
@EasyStore
interface AppPreferences {
    @Store(preferenceKeyName = "app_theme")
    suspend fun updateTheme(theme: Theme)

    // Automatic Enum mapping (Also supports Data Class)
    @Retrieve(preferenceKeyName = "app_theme")
    fun getAppTheme(default: Theme): Flow<Theme>
}

enum class Theme {
    DARK, LIGHT, SYSTEM
}

// Single Instance of DataStore
private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "my_prefs")

class MainActivity : AppCompatActivity() {

    private val appPreferences: AppPreferences by lazy {
        AppPreferencesImpl(application.dataStore)
    }

    private lateinit var currentTheme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val changeThemeBtn = findViewById<Button>(R.id.changeThemeBtn)

        withRepeatOnLifecycle {
            appPreferences.getAppTheme(default = Theme.SYSTEM).collectLatest { theme ->
                currentTheme = theme
                applyTheme(currentTheme)
            }
        }

        changeThemeBtn.setOnClickListener {
            dialogWithOptions(
                items = arrayOf("Light", "Dark", "System"),
                singleChoiceItems = true,
                selectedItem = getSelectedThemeOption(currentTheme),
                title = "Change Theme"
            ) { selectedOption, d ->
                d.dismiss()
                when (selectedOption) {
                    0 -> updateTheme(Theme.LIGHT)
                    1 -> updateTheme(Theme.DARK)
                    2 -> updateTheme(Theme.SYSTEM)
                }
            }
        }
    }

    private fun applyTheme(theme: Theme) {
        findViewById<TextView>(R.id.currentThemeTv).text = theme.name

        when (theme) {
            Theme.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Theme.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Theme.SYSTEM -> {
                if (Build.VERSION.SDK_INT >= 29) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
        }
    }

    private fun updateTheme(theme: Theme) {
        lifecycleScope.launch {
            appPreferences.updateTheme(theme)
        }
    }
}
