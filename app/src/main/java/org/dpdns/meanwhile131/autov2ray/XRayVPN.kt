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
import io.ktor.client.network.sockets.ConnectTimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import libXray.LibXray
import org.dpdns.meanwhile131.autov2ray.libXray.Request
import org.dpdns.meanwhile131.autov2ray.libXray.runXray

class XRayVPN : VpnService() {
    val configRepository = ConfigRepository()
    var fd: ParcelFileDescriptor? = null
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val urls = intent?.getStringArrayExtra("urls") ?: return START_NOT_STICKY
        val channel =
            NotificationChannel(channelId, "VPN Service", NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        val notification =
            Notification.Builder(this, channelId).setContentTitle("AutoV2Ray enabled").build()
        startForeground(startId, notification)
        serviceScope.launch {
            try {
                val config = configRepository.getConfig(urls)
                startXray(config)
            } catch (e: ConnectTimeoutException) {
                Looper.prepare()
                Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_LONG).show()
                Log.e("vpn", e.toString())
            }
        }
        return START_STICKY
    }

    fun startXray(config: MutableMap<String, JsonElement>) {
        this.fd = this.Builder().addAddress("10.0.0.1", 24).addRoute("0.0.0.0", 0).establish()
        if (fd == null) {
            Log.e("vpn", "Buildier().establish() returned null fd")
            return
        }

        val env = mutableMapOf<String, String>()
        assert(this.fd != null)
        env["xray.tun.fd"] = this.fd?.fd.toString()
        env["XRAY_TUN_FD"] = this.fd?.fd.toString()
        config["env"] = Json.encodeToJsonElement(env)

        Log.i("vpn", "service start")
        val dialerController = dialerController@{ fd: Long ->
            return@dialerController protect(fd.toInt())
        }
        LibXray.registerDialerController(dialerController)
        LibXray.setDNS(dialerController, "8.8.8.8:53")
        val req = Request(
            method = "runXrayFromJson",
            payload = Json.encodeToJsonElement(runXray(Json.encodeToString(config)))
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