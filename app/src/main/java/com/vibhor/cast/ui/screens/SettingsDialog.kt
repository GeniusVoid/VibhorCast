package com.vibhor.cast.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibhor.cast.cast.TVDiscovery
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    currentIp: String,
    currentPort: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var ip by remember { mutableStateOf(currentIp) }
    var port by remember { mutableStateOf(currentPort) }
    var isScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TV Configuration") },
        text = {
            Column {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("TV IP Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("ExoAirPlayer Port") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isScanning = true
                        scope.launch {
                            val tvs = TVDiscovery.discoverTVs()
                            if (tvs.isNotEmpty()) {
                                val first = tvs.first().split(":")
                                ip = first[0]
                                if (first.size > 1) port = first[1]
                            }
                            isScanning = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isScanning
                ) {
                    Text(if (isScanning) "Scanning..." else "Scan for TV")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(ip, port) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
