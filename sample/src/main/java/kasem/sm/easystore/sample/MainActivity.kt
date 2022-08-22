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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
