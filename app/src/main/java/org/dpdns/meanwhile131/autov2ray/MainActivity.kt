package org.dpdns.meanwhile131.autov2ray

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import org.dpdns.meanwhile131.autov2ray.ui.theme.AutoV2RayTheme

class MainActivity : ComponentActivity() {
    private lateinit var getVPNResult: ActivityResultLauncher<Intent>
    private var service: XRayVPN? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            this@MainActivity.service = (service as XRayVPN.LocalBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            this@MainActivity.service = null
        }
    }
    lateinit var urlsViewModel: URLViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        urlsViewModel = ViewModelProvider(this)[URLViewModel::class.java]
        Intent(this, XRayVPN::class.java).also { intent ->
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
        getVPNResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode != RESULT_OK) {
                    Log.e("vpn", "vpn request intent failed")
                    return@registerForActivityResult
                }
                connectVPN(urlsViewModel.state.text.toString())
            }
        setContent {
            AutoV2RayTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        val input = OutlinedTextField(
                            label = { Text("URLs") },
                            state = urlsViewModel.state,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .weight(1f),
                        )
                        ConnectDisconnect(
                            modifier = Modifier
                                .padding(innerPadding)
                                .align(Alignment.CenterHorizontally),
                            callback = {
                                if (!XRayVPN.isRunning.value)
                                    configureVPNPermissions(urlsViewModel.state.text.toString())
                                else
                                    disconnectVPN()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        unbindService(connection)
        super.onDestroy()
    }

    fun configureVPNPermissions(urls: String) {
        Log.d("connect", "Connect clicked")
        val intent = VpnService.prepare(this)

        if (intent == null) {
            connectVPN(urls)
            return
        }
        Log.d("vpn", "intent")
        getVPNResult.launch(intent)
    }

    fun connectVPN(urls: String) {
        Log.i("main", "starting vpn")
        val urls = urls.lines().toTypedArray()
        val intent = Intent(this, XRayVPN::class.java).apply {
            putExtra("urls", urls)
        }
        startForegroundService(intent)
    }

    fun disconnectVPN() {
        Log.i("main", "stopping vpn")
        if (service == null) {
            Log.e("main", "no vpn service found")
        }
        service?.stop()
    }
}

@Composable
fun ConnectDisconnect(modifier: Modifier = Modifier, callback: () -> Unit) {
    val vpnRunning by XRayVPN.isRunning.collectAsState()
    Button(
        onClick = { callback() },
        modifier = modifier
    ) {
        val text = if (vpnRunning) "Disconnect" else "Connect"
        Text(text)
    }
}