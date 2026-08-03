package com.example.skimguard

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.skimguard.ui.theme.ObsidianDanger
import com.example.skimguard.ui.theme.ObsidianOnSurfaceMuted
import com.example.skimguard.ui.theme.ObsidianOutline
import com.example.skimguard.ui.theme.ObsidianSuccess
import com.example.skimguard.ui.theme.ObsidianWhite
import com.example.skimguard.ui.theme.SkimGuardTheme
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Gate { Splash, Loading, Login, SignUp, App }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkimGuardTheme {
                var showSplash by remember { mutableStateOf(true) }
                // null = still checking stored session; true/false once resolved.
                var loggedIn by remember { mutableStateOf<Boolean?>(null) }
                var showSignUp by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    supabase.auth.awaitInitialization()
                    loggedIn = supabase.auth.currentSessionOrNull() != null
                }

                val gate: Gate = when {
                    showSplash -> Gate.Splash
                    loggedIn == null -> Gate.Loading
                    loggedIn == true -> Gate.App
                    showSignUp -> Gate.SignUp
                    else -> Gate.Login
                }

                Crossfade(
                    targetState = gate,
                    animationSpec = tween(400),
                    label = "gate-crossfade",
                ) { current ->
                    when (current) {
                        Gate.Splash -> SplashScreen(onContinue = { showSplash = false })
                        Gate.Loading -> AuthLoadingScreen()
                        Gate.Login -> LoginScreen(
                            onLoggedIn = { loggedIn = true },
                            onGoSignUp = { showSignUp = true },
                        )
                        Gate.SignUp -> SignUpScreen(
                            onSignedUp = { loggedIn = true },
                            onGoLogin = { showSignUp = false },
                        )
                        Gate.App -> SkimGuardScreen(
                            onLogOut = {
                                scope.launch {
                                    authSignOut()
                                    showSignUp = false
                                    loggedIn = false
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onContinue: () -> Unit) {
    var stage by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        val step = 200L
        repeat(4) { delay(step); stage++ }
    }
    val alphas = List(4) { i ->
        animateFloatAsState(
            targetValue = if (stage > i) 1f else 0f,
            animationSpec = tween(600),
            label = "splash-alpha-$i",
        ).value
    }
    val offsets = List(4) { i ->
        animateFloatAsState(
            targetValue = if (stage > i) 0f else 12f,
            animationSpec = tween(600),
            label = "splash-offset-$i",
        ).value
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp, bottom = 40.dp),
        ) {
            Text(
                text = "PROTECT\nYOUR CARD.",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 56.sp,
                    lineHeight = 60.sp,
                    letterSpacing = (-1).sp,
                    color = ObsidianWhite,
                ),
                modifier = Modifier
                    .graphicsAlpha(alphas[0])
                    .graphicsTranslateY(offsets[0]),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Protect Your Money.",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = ObsidianWhite,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsAlpha(alphas[1])
                    .graphicsTranslateY(offsets[1]),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsAlpha(alphas[2])
                    .graphicsTranslateY(offsets[2]),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .border(1.dp, ObsidianOnSurfaceMuted, RoundedCornerShape(3.dp)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "MILITARY GRADE ENCRYPTION",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        color = ObsidianOnSurfaceMuted,
                    ),
                )
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onContinue,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .graphicsAlpha(alphas[3])
                    .graphicsTranslateY(offsets[3]),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianWhite,
                    contentColor = Color.Black,
                ),
            ) {
                Text(
                    text = "CONTINUE",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        letterSpacing = 3.sp,
                    ),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "SKIMGUARD  V2.4.0  ©  2026",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = ObsidianOnSurfaceMuted.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsAlpha(alphas[3]),
            )
        }
    }
}

private fun Modifier.graphicsAlpha(alpha: Float): Modifier =
    this.then(Modifier.graphicsLayer { this.alpha = alpha })

private fun Modifier.graphicsTranslateY(dpOffset: Float): Modifier =
    this.then(Modifier.graphicsLayer { translationY = dpOffset * density })

data class LogEntry(
    val name: String,
    val address: String,
    val rssi: Int,
    val isSuspect: Boolean,
    val isSystem: Boolean = false,
    val systemText: String? = null,
)

private enum class ScanStatus { IDLE, SCANNING, SAFE, ALERT }

private sealed class Screen {
    data object Scanner : Screen()
    data class Danger(val name: String, val address: String, val rssi: Int) : Screen()
    data object NoThreats : Screen()
}

@Composable
fun SkimGuardScreen(onLogOut: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    var logLines by remember { mutableStateOf(emptyList<LogEntry>()) }
    var isScanning by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf<Screen>(Screen.Scanner) }
    // Dedup key = lowercased name if usable, else MAC. Cleared per scan session in beginScan().
    // This absorbs BLE MAC rotation: the same "HC-05" advertising with a new random MAC
    // is still deduped by its stable name.
    val seenKeys = remember { mutableSetOf<String>() }

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = try {
                    result.scanRecord?.deviceName ?: device.name
                } catch (_: SecurityException) { null }
                val address = device.address
                val rssi = result.rssi

                val displayName = name ?: "Unknown Device"
                val dedupKey = if (!name.isNullOrBlank()) name.lowercase() else address
                if (!seenKeys.add(dedupKey)) return

                val suspect = !name.isNullOrBlank() && when {
                    name.equals("HC-05", ignoreCase = true) -> true
                    name.equals("HC-06", ignoreCase = true) -> true
                    else -> false
                }

                logLines = logLines + LogEntry(
                    name = displayName,
                    address = address,
                    rssi = rssi,
                    isSuspect = suspect,
                )

                if (suspect) {
                    try { bluetoothAdapter?.bluetoothLeScanner?.stopScan(this) } catch (_: SecurityException) {}
                    isScanning = false
                    screen = Screen.Danger(displayName, address, rssi)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                logLines = logLines + LogEntry(
                    name = "", address = "", rssi = 0, isSuspect = false,
                    isSystem = true, systemText = "Scan failed (code $errorCode)"
                )
                isScanning = false
            }
        }
    }

    val scanPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun stopScan() {
        try { bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback) } catch (_: SecurityException) {}
        isScanning = false
    }

    fun addSystem(text: String) {
        logLines = logLines + LogEntry(
            name = "", address = "", rssi = 0, isSuspect = false,
            isSystem = true, systemText = text
        )
    }

    fun beginScan() {
        try {
            logLines = emptyList()
            seenKeys.clear()
            bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
            isScanning = true
        } catch (e: SecurityException) {
            addSystem("Permission error: ${e.message}")
        }
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            beginScan()
        } else {
            addSystem("Bluetooth must be on to scan.")
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            if (bluetoothAdapter?.isEnabled == true) {
                beginScan()
            } else {
                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        } else {
            addSystem("Permissions denied — cannot scan.")
        }
    }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            delay(12_000)
            if (isScanning) {
                stopScan()
                addSystem("Scan complete.")
                // Reached the full 12s without a suspect detection (which would have
                // flipped isScanning false and cancelled this coroutine before delay).
                screen = Screen.NoThreats
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_DESTROY) {
                stopScan()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopScan()
        }
    }

    val hasSuspect = logLines.any { it.isSuspect }
    val hasDeviceResults = logLines.any { !it.isSystem }
    val status = when {
        hasSuspect -> ScanStatus.ALERT
        isScanning -> ScanStatus.SCANNING
        hasDeviceResults -> ScanStatus.SAFE
        else -> ScanStatus.IDLE
    }

    val dismissToScanner: () -> Unit = {
        logLines = emptyList()
        screen = Screen.Scanner
    }
    BackHandler(enabled = screen != Screen.Scanner) { dismissToScanner() }

    Crossfade(
        targetState = screen,
        animationSpec = tween(350),
        label = "screen-crossfade",
    ) { current ->
        when (current) {
            is Screen.Scanner -> Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp),
                ) {
                    HeaderBar(onLogOut = onLogOut)
                    Spacer(Modifier.height(8.dp))
                    RadarTarget(
                        isScanning = isScanning,
                        isAlert = hasSuspect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "SCAN FOR\nNEARBY DEVICES",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            lineHeight = 40.sp,
                            color = ObsidianWhite,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Detect hidden Bluetooth skimmers near you",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            color = ObsidianOnSurfaceMuted,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(20.dp))
                    ScanButton(
                        isScanning = isScanning,
                        onClick = {
                            when {
                                isScanning -> {
                                    stopScan()
                                    addSystem("Scan stopped.")
                                }
                                bluetoothAdapter == null ->
                                    addSystem("Bluetooth not available on this device.")
                                bluetoothAdapter.bluetoothLeScanner == null ->
                                    addSystem("BLE not available on this device.")
                                else -> permLauncher.launch(scanPermissions)
                            }
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    StatusPill(status = status, suspectCount = logLines.count { it.isSuspect })
                    Spacer(Modifier.height(16.dp))
                    DeviceList(
                        entries = logLines,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            is Screen.Danger -> DangerScreen(
                deviceName = current.name,
                address = current.address,
                rssi = current.rssi,
                onDismiss = dismissToScanner,
            )
            is Screen.NoThreats -> NoThreatsScreen(
                onDismiss = dismissToScanner,
            )
        }
    }
}

@Composable
private fun HeaderBar(onLogOut: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(1.5.dp, ObsidianWhite, RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "SKIMGUARD",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.5.sp,
                color = ObsidianWhite,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "LOG OUT",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                color = ObsidianOnSurfaceMuted,
            ),
            modifier = Modifier
                .clickable(onClick = onLogOut)
                .padding(8.dp),
        )
    }
}

@Composable
private fun RadarTarget(
    isScanning: Boolean,
    isAlert: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isScanning) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radar-pulse",
    )
    val ringColor = if (isAlert) ObsidianDanger else ObsidianWhite
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = size.minDimension / 2f
            // Static concentric rings
            listOf(0.42f, 0.62f, 0.85f).forEach { fraction ->
                drawCircle(
                    color = ringColor.copy(alpha = 0.18f),
                    radius = maxR * fraction,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.2.dp.toPx()),
                )
            }
            if (isScanning) {
                val r = maxR * (0.35f + pulse * 0.6f)
                val alpha = (1f - pulse) * 0.55f
                drawCircle(
                    color = ringColor.copy(alpha = alpha),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
            // Center filled disc
            drawCircle(
                color = ringColor.copy(alpha = 0.10f),
                radius = maxR * 0.30f,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = ringColor,
                radius = maxR * 0.06f,
                center = Offset(cx, cy),
            )
        }
    }
}

@Composable
private fun ScanButton(isScanning: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = if (isScanning) {
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = ObsidianDanger,
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = ObsidianWhite,
                contentColor = Color.Black,
            )
        },
        border = if (isScanning) androidx.compose.foundation.BorderStroke(1.5.dp, ObsidianDanger) else null,
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        Text(
            text = if (isScanning) "STOP SCAN" else "START SCAN",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 2.sp,
            ),
        )
    }
}

@Composable
private fun StatusPill(status: ScanStatus, suspectCount: Int) {
    val (accent, title, subtitle) = when (status) {
        ScanStatus.ALERT -> Triple(
            ObsidianDanger,
            if (suspectCount > 1) "$suspectCount POTENTIAL SKIMMERS" else "POTENTIAL SKIMMER FOUND",
            "Review flagged devices below.",
        )
        ScanStatus.SAFE -> Triple(
            ObsidianSuccess,
            "ENVIRONMENT SECURE",
            "No active skimmers identified.",
        )
        ScanStatus.SCANNING -> Triple(
            ObsidianOnSurfaceMuted,
            "SCANNING…",
            "Listening for nearby BLE advertisements.",
        )
        ScanStatus.IDLE -> Triple(
            ObsidianOnSurfaceMuted,
            "READY",
            "Press START SCAN to begin.",
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianOutline, RoundedCornerShape(28.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f))
                .border(1.dp, accent, CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = accent,
                ),
            )
            Text(
                text = subtitle,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = ObsidianOnSurfaceMuted,
                ),
            )
        }
    }
}

@Composable
private fun DeviceList(entries: List<LogEntry>, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) return
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(entries) { entry ->
            if (entry.isSystem) {
                Text(
                    text = entry.systemText.orEmpty(),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = ObsidianOnSurfaceMuted,
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            } else {
                DeviceRow(entry)
            }
        }
    }
}

@Composable
private fun DeviceRow(entry: LogEntry) {
    val accent = if (entry.isSuspect) ObsidianDanger else ObsidianOutline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (entry.isSuspect) "⚠  ${entry.name}" else entry.name,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (entry.isSuspect) ObsidianDanger else ObsidianWhite,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = entry.address,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = ObsidianOnSurfaceMuted,
                ),
            )
        }
        Text(
            text = "${entry.rssi} dBm",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (entry.isSuspect) ObsidianDanger else ObsidianWhite,
            ),
        )
    }
}

@Composable
private fun DangerScreen(
    deviceName: String,
    address: String,
    rssi: Int,
    onDismiss: () -> Unit,
) {
    ResultShell(
        accent = ObsidianDanger,
        title = deviceName,
        headline = "HIGH RISK\nDETECTED",
        description = "This device is broadcasting and matches known skimmer hardware patterns.",
        icon = Icons.Filled.Warning,
        info = listOf(
            "MAC ADDRESS" to address,
            "SIGNAL STRENGTH" to "$rssi dBm",
            "STATUS" to "Active Broadcasting",
        ),
        primaryLabel = "MARK AS SAFE",
        onPrimary = onDismiss,
        secondaryLabel = "CONTACT AUTHORITIES",
        onSecondary = onDismiss,
    )
}

@Composable
private fun NoThreatsScreen(onDismiss: () -> Unit) {
    ResultShell(
        accent = ObsidianSuccess,
        title = "Scan Complete",
        headline = "NO THREATS\nDETECTED",
        description = "No skimmer signatures were found in this area. You are clear to proceed.",
        icon = Icons.Filled.Check,
        info = emptyList(),
        primaryLabel = "DONE",
        onPrimary = onDismiss,
        secondaryLabel = null,
        onSecondary = null,
    )
}

@Composable
private fun ResultShell(
    accent: Color,
    title: String,
    headline: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    info: List<Pair<String, String>>,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String?,
    onSecondary: (() -> Unit)?,
) {
    val pulseTransition = rememberInfiniteTransition(label = "result-pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse-fraction",
    )

    Scaffold(containerColor = Color.Black) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "←",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = ObsidianWhite,
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPrimary),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = ObsidianWhite,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.5.dp, accent, RoundedCornerShape(6.dp)),
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = size.minDimension / 2f
                    listOf(0.42f, 0.62f, 0.85f).forEach { fraction ->
                        drawCircle(
                            color = accent.copy(alpha = 0.30f),
                            radius = maxR * fraction,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.4.dp.toPx()),
                        )
                    }
                    // Pulsing ring: expands outward, fades to transparent.
                    val pulseR = maxR * (0.35f + pulse * 0.55f)
                    val pulseAlpha = (1f - pulse) * 0.55f
                    drawCircle(
                        color = accent.copy(alpha = pulseAlpha),
                        radius = pulseR,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.8.dp.toPx()),
                    )
                    // Center disc + subtle scale pulse on its alpha for extra life.
                    drawCircle(
                        color = accent.copy(alpha = 0.12f + (1f - pulse) * 0.08f),
                        radius = maxR * 0.32f,
                        center = Offset(cx, cy),
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(56.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = headline,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    color = ObsidianWhite,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = description,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = ObsidianOnSurfaceMuted,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (info.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianOutline, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    info.forEachIndexed { i, (label, value) ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = ObsidianOnSurfaceMuted,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = value,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (i == info.lastIndex) accent else ObsidianWhite,
                                ),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianWhite,
                    contentColor = Color.Black,
                ),
            ) {
                Text(
                    text = primaryLabel,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                    ),
                )
            }
            if (secondaryLabel != null && onSecondary != null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onSecondary,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = accent,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, accent),
                ) {
                    Text(
                        text = secondaryLabel,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
