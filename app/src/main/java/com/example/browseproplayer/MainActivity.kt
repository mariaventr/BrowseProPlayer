package com.example.browseproplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.*
import com.example.browseproplayer.ui.theme.BrowseProPlayerTheme
import androidx.compose.animation.core.*

private val VIDEO_EXTENSIONS = listOf(".mp4", ".m3u8", ".mpd", ".webm", ".mkv", ".ts")

data class DetectedVideo(
    val title: String,
    val url: String,
    val type: String,
    val duration: String = "00:00:00",
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_BrowseProPlayer)
        super.onCreate(savedInstanceState)
        setContent {
            BrowseProPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    BrowserScreen(initialUrl = "https://www.playhubmax.com/")
}

@Composable
fun LoadingSpinner(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.size(48.dp).rotate(rotation)) {
        drawArc(
            color = Color.White,
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BrowserScreen(initialUrl: String) {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf(initialUrl) }
    val detectedVideos = remember { mutableStateListOf<DetectedVideo>() }
    val focusRequester = remember { FocusRequester() }
    val sidePanelFocusRequester = remember { FocusRequester() }

    var cursorX by remember { mutableFloatStateOf(500f) }
    var cursorY by remember { mutableFloatStateOf(300f) }
    val step = 30f

    var showOverlay by remember { mutableStateOf(false) }
    var isSidePanelFocused by remember { mutableStateOf(false) }
    var isPageLoading by remember { mutableStateOf(true) }

    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            isFocusable = false
            isFocusableInTouchMode = false
            loadUrl(initialUrl)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Auto-focus side panel when first video is detected
    LaunchedEffect(detectedVideos.size) {
        if (detectedVideos.isNotEmpty() && !isSidePanelFocused && !showOverlay) {
            isSidePanelFocused = true
            sidePanelFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_BACK -> {
                            if (isSidePanelFocused) {
                                isSidePanelFocused = false
                                detectedVideos.clear() // Dismiss the panel
                                focusRequester.requestFocus()
                            } else {
                                showOverlay = !showOverlay
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (!showOverlay && !isSidePanelFocused) {
                                cursorY = (cursorY - step).coerceAtLeast(0f)
                                if (cursorY < 100f) webView.scrollBy(0, -200)
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!showOverlay && !isSidePanelFocused) {
                                cursorY = (cursorY + step).coerceAtMost(1080f)
                                if (cursorY > 980f) webView.scrollBy(0, 200)
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (!showOverlay && !isSidePanelFocused) {
                                cursorX = (cursorX - step).coerceAtLeast(0f)
                                true
                            } else if (isSidePanelFocused) {
                                isSidePanelFocused = false
                                focusRequester.requestFocus()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (!showOverlay && !isSidePanelFocused) {
                                cursorX = (cursorX + step).coerceAtMost(1920f)
                                if (cursorX > 1600f && detectedVideos.isNotEmpty()) {
                                    isSidePanelFocused = true
                                    sidePanelFocusRequester.requestFocus()
                                }
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER -> {
                            if (!showOverlay && !isSidePanelFocused) {
                                simulateClick(webView, cursorX, cursorY)
                                true
                            } else false
                        }
                        else -> false
                    }
                } else true
            }
    ) {
        // Main Web Content
        AndroidView(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            factory = {
                webView.apply {
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                            val requestUrl = request.url.toString()
                            if (esUrlDeVideo(requestUrl)) {
                                val video = DetectedVideo(
                                    title = requestUrl.substringAfterLast("/").substringBefore("?"),
                                    url = requestUrl,
                                    type = requestUrl.substringAfterLast(".").uppercase()
                                )
                                view.post {
                                    if (detectedVideos.none { it.url == requestUrl }) detectedVideos.add(video)
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            detectedVideos.clear()
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            urlText = url ?: ""
                            isPageLoading = false
                        }
                    }
                }
                webView
            }
        )

        // Overlay: Detected Videos Panel
        if (detectedVideos.isNotEmpty() && !showOverlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 40.dp)
                    .width(360.dp)
                    .focusRequester(sidePanelFocusRequester)
            ) {
                DetectedVideosPanel(
                    videos = detectedVideos,
                    domain = urlText.substringAfter("://").substringBefore("/"),
                    onVideoClick = { launchPlayer(context, it.url) }
                )
            }
        }

        // ADVANCED CONTROL OVERLAY
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            BrowserControlOverlay(
                initialUrl = urlText,
                onNavigate = {
                    webView.loadUrl(it)
                    showOverlay = false
                },
                onBack = { if (webView.canGoBack()) webView.goBack() },
                onForward = { if (webView.canGoForward()) webView.goForward() },
                onRefresh = { webView.reload() },
                onStop = { webView.stopLoading() }
            )
        }

        // The Cursor
        if (!showOverlay && !isSidePanelFocused && !isPageLoading) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                modifier = Modifier
                    .offset { IntOffset(cursorX.toInt(), cursorY.toInt()) }
                    .size(24.dp),
                tint = Color.White
            )
        }

        // Loading Spinner
        if (isPageLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                LoadingSpinner()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BrowserControlOverlay(
    initialUrl: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onStop: () -> Unit
) {
    var text by remember { mutableStateOf(initialUrl) }
    val textFieldFocusRequester = remember { FocusRequester() }
    var isTextFieldFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        textFieldFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(550.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF121212).copy(alpha = 0.98f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Search / URL Bar (Contenedor manual para evitar problemas de foco de Surface)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(44.dp)
                    .background(
                        color = if (isTextFieldFocused) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isTextFieldFocused) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(textFieldFocusRequester)
                            .onFocusChanged { isTextFieldFocused = it.isFocused },
                        singleLine = true,
                        cursorBrush = solidColorBrushCompat(Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                if (text.isNotBlank()) {
                                    var url = text.trim()
                                    if (!url.startsWith("http")) url = "https://google.com/search?q=$url"
                                    onNavigate(url)
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (text.isEmpty()) {
                                Text("URL o Buscar...", color = Color.Gray, fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Icons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlIcon(Icons.Default.Stop, "Stop", onStop)
                ControlIcon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", onBack)
                ControlIcon(Icons.AutoMirrored.Filled.ArrowForward, "Adelante", onForward)
                ControlIcon(Icons.Default.Refresh, "Refresh", onRefresh)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ControlIcon(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.05f),
                focusedContainerColor = Color.White,
                contentColor = Color.White,
                focusedContentColor = Color.Black
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetectedVideosPanel(videos: List<DetectedVideo>, domain: String, onVideoClick: (DetectedVideo) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0A0A0A).copy(alpha = 0.95f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2962FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Detected Videos", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Domain Toggle Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                colors = ClickableSurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.05f)),
                onClick = {}
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Enable pop-up for $domain",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Switch(checked = true, onCheckedChange = {})
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.heightIn(max = 500.dp)) {
                items(videos) { video ->
                    VideoCard(video, onVideoClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoCard(video: DetectedVideo, onClick: (DetectedVideo) -> Unit) {
    Surface(
        onClick = { onClick(video) },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF1A1A1A),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail Area
            Box(
                modifier = Modifier
                    .size(100.dp, 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2D2D44)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(32.dp))
                Text(
                    video.duration,
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).background(Color.Black.copy(0.7f), RoundedCornerShape(2.dp)).padding(horizontal = 4.dp),
                    fontSize = 10.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(video.title, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.border(1.dp, Color.Gray.copy(0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(video.type, fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

private fun launchPlayer(context: Context, url: String) {
    val intent = Intent(context, PlayerActivity::class.java)
    intent.putExtra(EXTRA_VIDEO_URL, url)
    context.startActivity(intent)
}

private fun simulateClick(view: View, x: Float, y: Float) {
    val downTime = SystemClock.uptimeMillis()
    val eventTime = SystemClock.uptimeMillis()
    val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
    view.dispatchTouchEvent(downEvent)
    view.postDelayed({
        val upEvent = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }, 50)
    downEvent.recycle()
}

private fun esUrlDeVideo(url: String): Boolean {
    val lower = url.lowercase()
    return VIDEO_EXTENSIONS.any { lower.contains(it) }
}

private fun solidColorBrushCompat(color: Color): Brush = Brush.verticalGradient(listOf(color, color))
