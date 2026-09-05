package org.dpdns.meanwhile131.autov2ray

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class ShareLinksRemoteDataSource {
    suspend fun getLinks(urls: Array<String>): String {
        val client = HttpClient(CIO)
        val allLinks = StringBuilder()
        for (url in urls) {
            Log.d("vpn", "fetching $url")
            val resp = client.get(url)
            val links = resp.bodyAsText()
            allLinks.append(links).append("\n")
        }
        return allLinks.toString()
    }
}