/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package kasem.sm.easystore.sample

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kasem.sm.easystore.core.EasyStore
import kasem.sm.easystore.core.Retrieve
import kasem.sm.easystore.core.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {

    private val Context.dataStore: DataStore<Preferences>
            by preferencesDataStore(name = "my_prefs")

//    private val preferences: SettingPreferences by lazy {
//        SettingPreferencesImpl(applicationContext.dataStore)
//    }

//    private val userPreferences: UserPreferences by lazy {
//        UserPreferencesImpl(applicationContext.dataStore)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.myButton).setOnClickListener {
            lifecycleScope.launchWhenStarted {
//                preferences.addDataClass(UserPref("qasim", -1))
            }
        }

        lifecycleScope.launchWhenStarted {
//            preferences.getDataClass(UserPref("", -1)).collectLatest {
//                findViewById<TextView>(R.id.myTextView).text = it.toString()
//            }
        }
    }
}

@EasyStore
interface UserPreferences {
    @Store(preferenceKeyName = "data_class")
    suspend fun addDataClass(userPref: UserPref)

    @Retrieve(preferenceKeyName = "data_class")
    fun getDataClass(default: UserPref): Flow<UserPref>
}

@EasyStore
interface SettingPreferences {
    @Store("app_theme")
    suspend fun updateTheme(theme: Theme)

    @Retrieve(preferenceKeyName = "app_theme")
    fun getTheme(default: Theme): Flow<Theme>
}

data class UserPref(
    val name: String,
    val age: Int,
)

enum class Theme {
    DARK, LIGHT, SYSTEM
}
