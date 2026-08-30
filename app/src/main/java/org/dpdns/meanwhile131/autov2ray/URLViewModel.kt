package org.dpdns.meanwhile131.autov2ray

import android.app.Application
import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@OptIn(FlowPreview::class)
class URLViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private val URLS_KEY = stringPreferencesKey("urls")
    }

    private val dataStore = getApplication<Application>().applicationContext.dataStore
    var state = TextFieldState("")

    init {
        viewModelScope.launch {
            state.setTextAndPlaceCursorAtEnd(dataStore.data.map { preferences ->
                preferences[URLS_KEY] ?: ""
            }.first())
        }
        viewModelScope.launch {
            snapshotFlow { state.text.toString() }
                .debounce(0.5.seconds)
                .distinctUntilChanged()
                .collect { new ->
                    dataStore.updateData {
                        it.toMutablePreferences().also { preferences ->
                            preferences[URLS_KEY] = new
                        }
                    }
                }
        }
    }
}