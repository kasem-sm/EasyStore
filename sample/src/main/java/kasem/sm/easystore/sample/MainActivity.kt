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
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "my_prefs")

    private val preferences: SettingPreferences by lazy {
        SettingPreferencesImpl(applicationContext.dataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.myButton).setOnClickListener {
            lifecycleScope.launchWhenStarted {
                preferences.addUserName(Random.nextInt(1, 1000).toString())
            }
        }

        lifecycleScope.launchWhenStarted {
            preferences.getUserName(defaultValue = "def").collectLatest {
                findViewById<TextView>(R.id.myTextView).text = it
            }
        }
    }
}

@EasyStore
interface SettingPreferences {
    @Store(preferenceKeyName = "user_pref")
    suspend fun addUserPref(userPref: UserPref)

    @Retrieve(preferenceKeyName = "user_pref")
    fun getUserPref(defaultValue: UserPref): Flow<UserPref>

    @Store(preferenceKeyName = "user_2222_name")
    suspend fun addUserName(str: String)

    @Retrieve(preferenceKeyName = "user_2222_name")
    fun getUserName(defaultValue: String): Flow<String>
}

data class UserPref(
    val name: String,
    val age: Int,
    val themeChoice: Theme = Theme.DARK
)

enum class Theme {
    DARK, LIGHT, SYSTEM
}
