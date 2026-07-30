package com.example.browseproplayer

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.example.browseproplayer.ui.theme.BrowseProPlayerTheme
import kotlinx.coroutines.delay
import java.util.Locale

const val EXTRA_VIDEO_URL = "extra_video_url"

class PlayerActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
        if (videoUrl.isNullOrBlank()) {
            Toast.makeText(this, "No se recibió ninguna URL de video", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContent {
            BrowseProPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VideoPlayerScreen(videoUrl = videoUrl)
                }
            }
        }
    }
}

@OptIn(UnstableApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun VideoPlayerScreen(videoUrl: String) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            playWhenReady = true
            prepare()
            play()
        }
    }

    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var playbackState by remember { mutableIntStateOf(player.playbackState) }
    var contentPosition by remember { mutableLongStateOf(0L) }
    var contentDuration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var isControllerVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val resizeModes = remember {
        intArrayOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            AspectRatioFrameLayout.RESIZE_MODE_FILL
        )
    }
    var resizeModeIndex by remember { mutableIntStateOf(0) }

    val focusRequester = remember { FocusRequester() }
    val rootFocusRequester = remember { FocusRequester() }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                contentDuration = player.duration.coerceAtLeast(0L)
                when (state) {
                    Player.STATE_READY -> player.play()
                    Player.STATE_BUFFERING -> { /* Showing indicator */ }
                    Player.STATE_IDLE -> player.prepare()
                    Player.STATE_ENDED -> isControllerVisible = true
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Toast.makeText(context, "Error de reproducción: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Force play on mount
    LaunchedEffect(Unit) {
        delay(1000)
        player.play()
    }

    // Polling for progress
    LaunchedEffect(player) {
        while (true) {
            contentPosition = player.currentPosition
            bufferedPosition = player.bufferedPosition
            delay(500)
        }
    }

    // Auto-hide controller
    LaunchedEffect(lastInteractionTime, isPlaying) {
        if (isPlaying) { // Only auto-hide if playing
            while (true) {
                delay(1000)
                if (isControllerVisible && System.currentTimeMillis() - lastInteractionTime >= 5000) {
                    isControllerVisible = false
                }
            }
        }
    }

    BackHandler(enabled = isControllerVisible) {
        isControllerVisible = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                lastInteractionTime = System.currentTimeMillis()
                if (!isControllerVisible) {
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        isControllerVisible = true
                        if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                            if (player.isPlaying) player.pause() else player.play()
                        }
                        true
                    } else false
                } else {
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        lastInteractionTime = System.currentTimeMillis()
                    }
                    false
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                }
            },
            update = { view -> 
                view.resizeMode = resizeModes[resizeModeIndex]
            }
        )

        // Loading Indicator
        if (playbackState == Player.STATE_BUFFERING) {
            Box(
                modifier = Modifier.align(Alignment.Center).size(64.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("Cargando...", color = Color.White, fontSize = 12.sp)
            }
        }

        // CUSTOM COMPOSE CONTROLS
        AnimatedVisibility(
            visible = isControllerVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .focusProperties { canFocus = isControllerVisible }
            ) {
                // Top Exit Button
                IconButton(
                    onClick = { (context as? ComponentActivity)?.finish() },
                    modifier = Modifier.align(Alignment.TopStart).padding(32.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                // 1. Central Playback Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { player.seekBack() }) {
                        Icon(Icons.Default.Replay, null, modifier = Modifier.size(40.dp), tint = Color.White)
                    }
                    
                    Surface(
                        onClick = { 
                            if (player.playbackState == Player.STATE_ENDED) {
                                player.seekTo(0)
                                player.play()
                            } else {
                                if (player.isPlaying) player.pause() else player.play()
                            }
                        },
                        shape = ClickableSurfaceDefaults.shape(CircleShape),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            focusedContainerColor = Color.White,
                            contentColor = Color.White,
                            focusedContentColor = Color.Black
                        ),
                        modifier = Modifier.size(56.dp).focusRequester(focusRequester)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    IconButton(onClick = { player.seekForward() }) {
                        Icon(Icons.Default.FastForward, null, modifier = Modifier.size(40.dp), tint = Color.White)
                    }
                }

                // 2. Bottom Area: Actions + Seekbar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 64.dp, end = 64.dp, bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Action Buttons Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerActionButton("Subtítulos") { showTrackSelectionDialog(context, player, C.TRACK_TYPE_TEXT, "Subtítulos") }
                        PlayerActionButton("Audio") { showTrackSelectionDialog(context, player, C.TRACK_TYPE_AUDIO, "Audio") }
                        PlayerActionButton("Resolución") { showTrackSelectionDialog(context, player, C.TRACK_TYPE_VIDEO, "Resolución") }
                        PlayerActionButton("Aspecto") {
                            resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Custom Seekbar
                    CustomSeekbar(
                        position = contentPosition,
                        duration = contentDuration,
                        buffered = bufferedPosition,
                        onSeek = { player.seekTo(it) }
                    )
                }
            }
        }
    }

    LaunchedEffect(isControllerVisible) {
        if (isControllerVisible) {
            focusRequester.requestFocus()
        } else {
            rootFocusRequester.requestFocus()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF1E1E1E),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        ),
        shape = ButtonDefaults.shape(RoundedCornerShape(12.dp))
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CustomSeekbar(
    position: Long,
    duration: Long,
    buffered: Long,
    onSeek: (Long) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val progress = if (duration > 0) position.toFloat() / duration else 0f
    val bufferProgress = if (duration > 0) buffered.toFloat() / duration else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (isFocused && keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            val newPos = (position - 10000).coerceAtLeast(0)
                            onSeek(newPos)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            val newPos = (position + 10000).coerceAtMost(duration)
                            onSeek(newPos)
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(position), color = Color.White, fontSize = 12.sp)
            Text(formatTime(duration), color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFocused) 8.dp else 4.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {
            // Buffer
            Box(
                modifier = Modifier
                    .fillMaxWidth(bufferProgress)
                    .fillMaxHeight()
                    .background(Color.Gray.copy(alpha = 0.5f))
            )
            // Progress
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(if (isFocused) Color.White else Color.Red)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

@OptIn(UnstableApi::class)
private fun showTrackSelectionDialog(
    context: android.content.Context,
    player: ExoPlayer,
    trackType: Int,
    title: String
) {
    val groups = player.currentTracks.groups.filter { it.type == trackType }
    val labels = mutableListOf<String>()
    val actions = mutableListOf<() -> Unit>()

    if (trackType == C.TRACK_TYPE_TEXT) {
        labels.add("Desactivar subtítulos")
        actions.add {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .build()
        }
    }

    if (trackType == C.TRACK_TYPE_VIDEO) {
        labels.add("Automática (recomendada)")
        actions.add {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .build()
        }
    }

    groups.forEach { group ->
        for (i in 0 until group.length) {
            if (group.isTrackSupported(i)) {
                val format = group.getTrackFormat(i)
                val label = when (trackType) {
                    C.TRACK_TYPE_VIDEO -> {
                        val res = if (format.height > 0) "${format.height}p" else "Calidad ${labels.size + 1}"
                        val br = if (format.bitrate > 0) " · ${format.bitrate / 1000} kbps" else ""
                        res + br
                    }
                    else -> format.label ?: format.language?.uppercase(Locale.getDefault()) ?: "Pista ${labels.size + 1}"
                }
                labels.add(label)
                actions.add {
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(trackType, false)
                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                        .build()
                }
            }
        }
    }

    if (labels.isEmpty()) {
        Toast.makeText(context, "No hay opciones disponibles", Toast.LENGTH_SHORT).show()
        return
    }

    AlertDialog.Builder(context)
        .setTitle(title)
        .setItems(labels.toTypedArray()) { _, which -> actions[which]() }
        .show()
}
