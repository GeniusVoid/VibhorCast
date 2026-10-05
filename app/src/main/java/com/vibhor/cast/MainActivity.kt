package com.vibhor.cast

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.vibhor.cast.server.MediaServerService
import com.vibhor.cast.ui.screens.HomeScreen
import com.vibhor.cast.ui.screens.SettingsDialog
import com.vibhor.cast.ui.theme.VibhorCastTheme

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startMediaServer()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefs = getSharedPreferences("vibhor_cast_prefs", Context.MODE_PRIVATE)
        
        checkPermissionsAndStartServer()

        setContent {
            VibhorCastTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSettings by remember { mutableStateOf(false) }
                    var tvIp by remember { mutableStateOf(prefs.getString("tv_ip", "172.24.238.117") ?: "172.24.238.117") }
                    var tvPort by remember { mutableStateOf(prefs.getString("tv_port", "8192") ?: "8192") }

                    if (showSettings) {
                        SettingsDialog(
                            currentIp = tvIp,
                            currentPort = tvPort,
                            onDismiss = { showSettings = false },
                            onSave = { ip, port ->
                                tvIp = ip
                                tvPort = port
                                prefs.edit()
                                    .putString("tv_ip", ip)
                                    .putString("tv_port", port)
                                    .apply()
                                showSettings = false
                            }
                        )
                    }

                    HomeScreen(
                        tvIp = tvIp,
                        tvPort = tvPort,
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }

    private fun checkPermissionsAndStartServer() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            startMediaServer()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun startMediaServer() {
        val intent = Intent(this, MediaServerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
