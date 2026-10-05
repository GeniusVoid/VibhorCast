package com.vibhor.cast.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibhor.cast.cast.ExoAirPlayerClient
import com.vibhor.cast.cast.TVDiscovery
import com.vibhor.cast.model.VideoFile
import com.vibhor.cast.util.MediaScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    tvIp: String,
    tvPort: String,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var videos by remember { mutableStateOf<List<VideoFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isConnected by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<VideoFile?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    val client = remember(tvIp, tvPort) { ExoAirPlayerClient("$tvIp:$tvPort") }
    val localIp = remember { TVDiscovery.getLocalIpAddress() ?: "127.0.0.1" }

    LaunchedEffect(Unit) {
        videos = MediaScanner.scanVideos(context.contentResolver)
        isLoading = false
    }

    LaunchedEffect(tvIp, tvPort) {
        while (isActive) {
            isConnected = client.isReachable()
            delay(3000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("VibhorCast")
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (isConnected) Color.Green else Color.Red,
                            modifier = Modifier.size(12.dp)
                        ) {}
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (isPlaying) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            scope.launch {
                                client.togglePause()
                            }
                        }) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause/Resume")
                        }
                        IconButton(onClick = {
                            scope.launch {
                                client.stop()
                                isPlaying = false
                            }
                        }) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                items(videos) { video ->
                    VideoItem(
                        video = video,
                        isSelected = selectedVideo == video,
                        onClick = { selectedVideo = if (selectedVideo == video) null else video },
                        onPlay = {
                            scope.launch {
                                val url = "http://$localIp:8080${video.path}"
                                val result = client.play(url)
                                result.onSuccess {
                                    Toast.makeText(context, "Playing on TV", Toast.LENGTH_SHORT).show()
                                    isPlaying = true
                                }.onFailure {
                                    Toast.makeText(context, "Failed to play", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onQueue = {
                            scope.launch {
                                val url = "http://$localIp:8080${video.path}"
                                val result = client.queue(url)
                                result.onSuccess {
                                    Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "Failed to queue", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun VideoItem(
    video: VideoFile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onQueue: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = video.name,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${video.formattedSize} • ${video.formattedDuration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (video.resolution.isNotEmpty()) {
                    Text(
                        text = video.resolution,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PLAY ON TV")
                    }
                    OutlinedButton(onClick = onQueue) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("QUEUE")
                    }
                }
            }
        }
    }
}
