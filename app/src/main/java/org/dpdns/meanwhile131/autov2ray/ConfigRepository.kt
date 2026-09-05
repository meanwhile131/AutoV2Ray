package org.dpdns.meanwhile131.autov2ray

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.dpdns.meanwhile131.autov2ray.libXray.Balancer
import org.dpdns.meanwhile131.autov2ray.libXray.BalancerStrategy
import org.dpdns.meanwhile131.autov2ray.libXray.BurstObservatory
import org.dpdns.meanwhile131.autov2ray.libXray.Inbound
import org.dpdns.meanwhile131.autov2ray.libXray.LogSettings
import org.dpdns.meanwhile131.autov2ray.libXray.Outbound
import org.dpdns.meanwhile131.autov2ray.libXray.PingConfig
import org.dpdns.meanwhile131.autov2ray.libXray.Request
import org.dpdns.meanwhile131.autov2ray.libXray.Response
import org.dpdns.meanwhile131.autov2ray.libXray.RoutingSettings
import org.dpdns.meanwhile131.autov2ray.libXray.Rule
import org.dpdns.meanwhile131.autov2ray.libXray.TunSettings
import org.dpdns.meanwhile131.autov2ray.libXray.convertShareLinksToXrayJson

class ConfigRepository {
    suspend fun getConfig(urls: Array<String>): MutableMap<String, JsonElement> {
        val client = HttpClient(CIO)
        val allLinks = StringBuilder()
        for (url in urls) {
            Log.d("vpn", "fetching $url")
            val resp = client.get(url)
            val links = resp.bodyAsText()
            allLinks.append(links).append("\n")
        }
        val req = Request(
            method = "convertShareLinksToXrayJson",
            payload = Json.encodeToJsonElement(convertShareLinksToXrayJson(allLinks.toString()))
        )
        val respJson = invoke(req)
        val resp = Json.decodeFromString<Response>(respJson)

        val config = resp.data.jsonObject.toMutableMap()
        val inbound = Inbound("tun", TunSettings("tun0"), "in")
        val inbounds = Json.encodeToJsonElement(arrayOf(inbound))
        config["inbounds"] = inbounds


        val routing = RoutingSettings(
            arrayOf(
                Rule(
                    inboundTag = arrayOf("in"),
                    balancerTag = "balancer",
                )
            ),
            balancers = arrayOf(
                Balancer(
                    tag = "balancer",
                    selector = arrayOf("out"),
                    strategy = BalancerStrategy(
                        type = "leastload"
                    ),
                    fallbackTag = "freedom"
                )
            )
        )
        config["routing"] = Json.encodeToJsonElement(routing)

        val burstObservatory = BurstObservatory(
            subjectSelector = arrayOf("out"),
            pingConfig = PingConfig(
                interval = "10m"
            )
        )
        config["burstObservatory"] = Json.encodeToJsonElement(burstObservatory)

        val outbounds = config["outbounds"]?.jsonArray
        val newOutbounds = outbounds?.mapIndexed { i, outbound ->
            val map = outbound.jsonObject.toMutableMap()
            val streamSettings = map["streamSettings"]
            if (streamSettings is JsonObject) {
                val streamSettingsMap = streamSettings.toMutableMap()
                val realitySettings = streamSettingsMap["realitySettings"]
                if (realitySettings is JsonObject) {
                    val realitySettingsMap = realitySettings.toMutableMap()
                    realitySettingsMap.remove("dest")
                    realitySettingsMap.remove("target")
                    streamSettingsMap["realitySettings"] = JsonObject(realitySettingsMap)
                }
                map["streamSettings"] = JsonObject(streamSettingsMap)
            }
            map.remove("sendThrough")
            map["tag"] = Json.encodeToJsonElement("out$i")
            JsonObject(map)
        }?.plus(
            Json.encodeToJsonElement(
                Outbound(
                    "freedom"
                )
            )
        )

        config["outbounds"] = Json.encodeToJsonElement(newOutbounds)
        config["log"] = Json.encodeToJsonElement(
            LogSettings(
                loglevel = "warning"
            )
        )

        return config
    }
}