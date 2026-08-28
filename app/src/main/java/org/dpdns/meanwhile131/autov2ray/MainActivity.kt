package org.dpdns.meanwhile131.autov2ray

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dpdns.meanwhile131.autov2ray.ui.theme.AutoV2RayTheme

class MainActivity : ComponentActivity() {
    private var urls =
        arrayOf("https://raw.githubusercontent.com/whoahaow/rjsxrd/refs/heads/main/githubmirror/bypass/bypass-1.txt")
    private lateinit var getVPNResult: ActivityResultLauncher<Intent>
    private lateinit var service: XRayVPN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getVPNResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode != Activity.RESULT_OK) {
                    Log.e("vpn", "vpn request intent failed")
                    return@registerForActivityResult
                }
                connectVPN()
            }
        enableEdgeToEdge()
        setContent {
            AutoV2RayTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Connect(
                        modifier = Modifier.padding(innerPadding),
                        callback = {
                            configureVPNPermissions()
                        }
                    )
                }
            }
        }
    }

    fun configureVPNPermissions() {
        Log.d("connect", "Connect clicked")
        val intent = VpnService.prepare(this)

        if (intent == null) {
            connectVPN()
            return
        }
        Log.d("vpn", "intent")
        getVPNResult.launch(intent)
    }

    fun connectVPN() {
        Log.i("vpn", "starting vpn")
        val intent = Intent(this, XRayVPN::class.java).apply {
            putExtra("urls", urls)
        }
        this.startForegroundService(intent)
    }
}

@Composable
fun Connect(modifier: Modifier = Modifier, callback: () -> Unit) {
    Button(
        onClick = { callback() },
        modifier = modifier
    ) { Text("Connect") }
}