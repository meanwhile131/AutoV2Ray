package org.dpdns.meanwhile131.autov2ray

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first

class ShareLinksRemoteDataSource(
    private val dataStore: DataStore<Preferences>
) {
    val LINKS_KEY = stringPreferencesKey("links")
    suspend fun getLinks(urls: Array<String>): String {
        val client = HttpClient(CIO) {
            expectSuccess = true
        }
        val allLinks = StringBuilder()
        for (url in urls) {
            try {
                Log.i("links", "fetching $url")
                val resp = client.get(url)
                if (resp.status == HttpStatusCode.OK) {
                    Log.e("links", "failed $url: ${resp.status}")
                    val links = resp.bodyAsText()
                    allLinks.append(links).append("\n")
                }
            } catch (e: Exception) {
                Log.e("links", "error fetching $url: $e")
            }
        }
        if (allLinks.isEmpty()) {
            allLinks.append(dataStore.data.first()[LINKS_KEY] ?: "")
            Log.i("links", "loaded links from cache because failed to fetch any")
        } else {
            dataStore.edit { data ->
                data[LINKS_KEY] = allLinks.toString()
            }
            Log.i("links", "saved links")
        }
        return allLinks.toString()
    }
}