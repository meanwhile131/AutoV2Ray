package org.dpdns.meanwhile131.autov2ray

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import libXray.LibXray
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
import org.dpdns.meanwhile131.autov2ray.libXray.runXray

class XRayVPN : VpnService() {
    var config: JsonElement? = null
    var fd: ParcelFileDescriptor? = null
    lateinit var urls: Array<String>
    val channelId = "vpn"
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val binder = LocalBinder()

    companion object {
        var isRunning = MutableStateFlow(false)
            private set
    }


    inner class LocalBinder : Binder() {
        fun getService(): XRayVPN = this@XRayVPN
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private suspend fun fetchShareLinks() {
        val client = HttpClient(CIO)
        val allLinks = StringBuilder()
        for (url in urls) {
            Log.d("vpn", "fetching $url")
            val resp = client.get(url)
            val links = resp.bodyAsText()
            Log.d("vpn", links)
            allLinks.append(links).append("\n")
        }
        val req = Request(
            method = "convertShareLinksToXrayJson",
            payload = Json.encodeToJsonElement(convertShareLinksToXrayJson(allLinks.toString()))
        )
        val respJson = invoke(req)
        val resp = Json.decodeFromString<Response>(respJson)
        config = resp.data
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val urls = intent?.getStringArrayExtra("urls") ?: return START_NOT_STICKY
        this.urls = urls
        val channel =
            NotificationChannel(channelId, "VPN Service", NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        val notification =
            Notification.Builder(this, channelId).setContentTitle("AutoV2Ray enabled").build()
        startForeground(startId, notification)
        serviceScope.launch {
            try {
                fetchShareLinks()
                startXray()
            } catch (e: ConnectTimeoutException) {
                Looper.prepare()
                Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_LONG).show()
                Log.e("vpn", e.toString())
            }
        }
        return START_STICKY
    }

    fun startXray() {
        this.fd = this.Builder().addAddress("10.0.0.1", 24).addRoute("0.0.0.0", 0).establish()
        if (fd == null) {
            Log.e("vpn", "Buildier().establish() returned null fd")
            return
        }
        val config = config!!.jsonObject.toMutableMap()
        val inbound = Inbound("tun", TunSettings("tun0"), "in")
        val inbounds = Json.encodeToJsonElement(arrayOf(inbound))
        config["inbounds"] = inbounds

        val env = mutableMapOf<String, String>()
        assert(this.fd != null)
        env["xray.tun.fd"] = this.fd?.fd.toString()
        env["XRAY_TUN_FD"] = this.fd?.fd.toString()
        config["env"] = Json.encodeToJsonElement(env)

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

        val configJson = Json.encodeToString(config)

        Log.i("vpn", "service start")
        val dialerController = dialerController@{ fd: Long ->
            return@dialerController protect(fd.toInt())
        }
        LibXray.registerDialerController(dialerController)
        LibXray.setDNS(dialerController, "8.8.8.8:53")
        val req = Request(
            method = "runXrayFromJson",
            payload = Json.encodeToJsonElement(runXray(configJson))
        )
        val respJson = invoke(req)
        isRunning.value = true
        Log.d("vpn", respJson)
    }

    private fun cleanup() {
        val req = Request(
            method = "stopXray",
            payload = null
        )
        val resp = invoke(req)
        Log.d("vpn", resp)
        LibXray.resetDNS()
        Log.i("vpn", "service stop")
        this.fd?.close()
        isRunning.value = false
    }

    fun stop() {
        cleanup()
        stopSelf()
    }

    override fun onRevoke() {
        cleanup()
    }

    override fun onDestroy() {
        cleanup()
    }
}

fun invoke(request: Request): String {
    val reqJson = Json.encodeToString(request)
    Log.d("vpn_invoke", reqJson)
    val resp = LibXray.invoke(reqJson)
    return resp
}