package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val TELEGRAM_EXECUTIVE_CHAT = "https://t.me/+2347016435125"
const val BOT_LOGO_URL = "https://i.postimg.co/pXRLx9vM/DIDS-BOT-LOGO.png"

@Composable
fun DidsAppContent(viewModel: DidsBoltViewModel) {
    val currentView = viewModel.currentView
    val toastState = viewModel.toastMessage

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Slate950, Slate900)
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950),
        bottomBar = {
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundBrush)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (currentView != "login" && viewModel.currentUser != null) {
                    DidsBoltHeader(viewModel = viewModel)
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentView) {
                        "login" -> LoginView(viewModel = viewModel)
                        "dashboard" -> DashboardView(viewModel = viewModel)
                        "paywall" -> PaywallView(viewModel = viewModel)
                        "settings" -> SettingsView(viewModel = viewModel)
                        "admin" -> AdminView(viewModel = viewModel)
                    }
                }
            }

            AnimatedVisibility(
                visible = toastState != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .zIndex(50f)
            ) {
                toastState?.let { toast ->
                    ToastWidget(toast = toast, onDismiss = { viewModel.clearToast() })
                }
            }
        }
    }
}

@Composable
fun DidsBoltHeader(viewModel: DidsBoltViewModel) {
    val user = viewModel.currentUser ?: return
    val hasAccess = viewModel.hasAccess()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Slate800, shape = RoundedCornerShape(0.dp)),
        color = Slate950.copy(alpha = 0.85f),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clickable {
                        if (user.id == "ADMIN_ROOT") viewModel.setView("admin")
                        else viewModel.setView("dashboard")
                    }
                    .testTag("header_brand_trigger")
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .background(Slate950),
                    contentAlignment = Alignment.Center
                ) {
                    LogoImage(modifier = Modifier.fillMaxSize())
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DIDS",
                        color = CyanAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "BOLT",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.SansSerif
                    )

                    if (user.id == "ADMIN_ROOT") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "ADM",
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (user.id == "ADMIN_ROOT") {
                    Box(
                        modifier = Modifier
                            .background(CyanAccentMuted, RoundedCornerShape(20.dp))
                            .border(0.5.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Console active",
                                tint = CyanAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Console",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (hasAccess) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(EmeraldGreen.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, EmeraldGreen.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Subscription Active Check",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(RubyRose.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, RubyRose.copy(alpha = 0.5f), CircleShape)
                            .clickable { viewModel.setView("paywall") }
                            .testTag("paywall_gate_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Subscription Locked Indicator",
                            tint = RubyRose,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.setView("settings") },
                    modifier = Modifier
                        .background(Slate900, CircleShape)
                        .border(1.dp, Slate800, CircleShape)
                        .size(36.dp)
                        .testTag("settings_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Account Settings Dashboard",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardView(viewModel: DidsBoltViewModel) {
    val assets by viewModel.assets.collectAsState()
    val announcementState by viewModel.announcement.collectAsState()
    val hasAccess = viewModel.hasAccess()
    val uriHandler = LocalUriHandler.current

    val announcementText = announcementState?.value ?: "🚨 LIVE DIDSBOLT TERMINAL SECURE — REDEEM ACCESS CODE TO BEGIN GENERATING AUTOMATED SIGNALS"

    val filteredAssets = remember(assets, viewModel.currentMarketType) {
        assets.filter {
            if (viewModel.currentMarketType == "OTC") it.isOtc else !it.isOtc
        }.map { it.name }
    }

    LaunchedEffect(filteredAssets) {
        if (filteredAssets.isNotEmpty() && !filteredAssets.contains(viewModel.selectedAsset)) {
            viewModel.selectedAsset = filteredAssets.first()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_root_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate950.copy(alpha = 0.9f)),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alert logo",
                        tint = CyanAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                    ) {
                        Text(
                            text = announcementText,
                            color = CyanAccent.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate950, RoundedCornerShape(10.dp))
                            .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (viewModel.currentMarketType == "Standard") Slate800 else Color.Transparent)
                                .border(
                                    width = if (viewModel.currentMarketType == "Standard") 0.5.dp else 0.dp,
                                    color = if (viewModel.currentMarketType == "Standard") Slate700 else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = viewModel.activeSignalState != "analyzing") {
                                    viewModel.currentMarketType = "Standard"
                                }
                                .padding(vertical = 8.dp)
                                .testTag("toggle_market_standard"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Standard Market",
                                color = if (viewModel.currentMarketType == "Standard") Color.White else Slate500,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (viewModel.currentMarketType == "OTC") CyanAccent.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    width = if (viewModel.currentMarketType == "OTC") 0.5.dp else 0.dp,
                                    color = if (viewModel.currentMarketType == "OTC") CyanAccent.copy(alpha = 0.3f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = viewModel.activeSignalState != "analyzing") {
                                    viewModel.currentMarketType = "OTC"
                                }
                                .padding(vertical = 8.dp)
                                .testTag("toggle_market_otc"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "OTC Market",
                                color = if (viewModel.currentMarketType == "OTC") CyanAccent else Slate500,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Select Asset Pair",
                            color = Slate500,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate950, RoundedCornerShape(12.dp))
                                    .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                    .clickable(enabled = viewModel.activeSignalState != "analyzing") {
                                        expanded = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .testTag("asset_picker_trigger"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (viewModel.selectedAsset.isNotEmpty()) viewModel.selectedAsset else "No asset configured",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand asset list",
                                    tint = CyanAccent
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(Slate900)
                                    .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                            ) {
                                if (filteredAssets.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No assets on configuration", color = Slate500, fontSize = 12.sp) },
                                        onClick = { expanded = false }
                                    )
                                } else {
                                    filteredAssets.forEach { assetName ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = assetName,
                                                    color = if (viewModel.selectedAsset == assetName) CyanAccent else Color.White,
                                                    fontWeight = if (viewModel.selectedAsset == assetName) FontWeight.Bold else FontWeight.Normal,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            onClick = {
                                                viewModel.selectedAsset = assetName
                                                expanded = false
                                            },
                                            modifier = Modifier.testTag("asset_option_$assetName")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "Expiration Time",
                            color = Slate500,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val timeframes = listOf(
                            "S5", "S10", "S15", "S30",
                            "M1", "M2", "M3", "M5", "M10",
                            "D1", "D2", "D3", "D5"
                        )
                        var tfExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate950, RoundedCornerShape(12.dp))
                                    .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                    .clickable(enabled = viewModel.activeSignalState != "analyzing") {
                                        tfExpanded = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .testTag("timeframe_picker_trigger"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = viewModel.selectedTimeframe,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand timeframe values",
                                    tint = CyanAccent
                                )
                            }

                            DropdownMenu(
                                expanded = tfExpanded,
                                onDismissRequest = { tfExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(Slate900)
                                    .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                            ) {
                                timeframes.forEach { tf ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = tf,
                                                color = if (viewModel.selectedTimeframe == tf) CyanAccent else Color.White,
                                                fontWeight = if (viewModel.selectedTimeframe == tf) FontWeight.Bold else FontWeight.Normal,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            viewModel.selectedTimeframe = tf
                                            tfExpanded = false
                                        },
                                        modifier = Modifier.testTag("timeframe_option_$tf")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .border(1.dp, Slate800, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineCount = 8
                    val lineGap = size.height / lineCount
                    for (i in 0 until lineCount) {
                        drawLine(
                            color = Color(0x06BB86FC),
                            start = Offset(0f, i * lineGap),
                            end = Offset(size.width, i * lineGap),
                            strokeWidth = 1f
                        )
                    }
                    val verticalCount = 10
                    val vGap = size.width / verticalCount
                    for (i in 0 until verticalCount) {
                        drawLine(
                            color = Color(0x06BB86FC),
                            start = Offset(i * vGap, 0f),
                            end = Offset(i * vGap, size.height),
                            strokeWidth = 1f
                        )
                    }
                }

                when (viewModel.activeSignalState) {
                    "idle" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Idle graph line",
                                tint = Slate700,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Ready to analyze ${if (viewModel.selectedAsset.isNotEmpty()) viewModel.selectedAsset else "assets"}",
                                color = Slate500,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    "analyzing" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val angle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                )
                            )
                            val heartBeatPulse by infiniteTransition.animateFloat(
                                initialValue = 0.8f,
                                targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )

                            Box(
                                modifier = Modifier.size(96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { 0.75f },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .rotate(angle),
                                    color = CyanAccent,
                                    strokeWidth = 4.dp,
                                    trackColor = Slate800,
                                    strokeCap = Stroke.DefaultCap
                                )

                                Icon(
                                    imageVector = Icons.Default.ShowChart,
                                    contentDescription = "Analyzing ticker pulse",
                                    tint = CyanAccent,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .alpha(heartBeatPulse)
                                )
                            }

                            Text(
                                text = "Ingesting Market Vectors...",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                                modifier = Modifier.alpha(heartBeatPulse)
                            )
                        }
                    }

                    "result" -> {
                        viewModel.currentSignal?.let { signal ->
                            val isCall = signal.direction == "CALL"
                            val accentColor = if (isCall) EmeraldGreen else RubyRose

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Slate900, RoundedCornerShape(20.dp))
                                        .border(1.dp, Slate800, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${signal.pair} • ${signal.tf}",
                                        color = Slate400,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                val infiniteTransition = rememberInfiniteTransition()
                                val bounceOffset by infiniteTransition.animateFloat(
                                    initialValue = -8f,
                                    targetValue = 8f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(650, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .offset(y = bounceOffset.dp)
                                        .drawBehind {
                                            drawCircle(
                                                color = accentColor.copy(alpha = 0.15f),
                                                radius = 90.dp.toPx(),
                                                center = Offset(size.width / 2, size.height / 2)
                                            )
                                        }
                                ) {
                                    Icon(
                                        imageVector = if (isCall) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = "Direction arrow",
                                        tint = accentColor,
                                        modifier = Modifier.size(80.dp)
                                    )
                                    Text(
                                        text = signal.direction,
                                        color = accentColor,
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1).sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(0.75f)
                                        .background(Slate900, RoundedCornerShape(12.dp))
                                        .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                        .padding(vertical = 10.dp, horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = "Speed needle metrics",
                                            tint = CyanAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "STRENGTH",
                                            color = Slate500,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Text(
                                        text = "${signal.confidence}% ACCURACY",
                                        color = CyanAccent,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(0.75f),
                                    colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.5f)),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Timer,
                                                    contentDescription = "Hours alarm timer",
                                                    tint = Slate500,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = "EXPIRY TIME",
                                                    color = Slate500,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            Text(
                                                text = formatTimeLeft(viewModel.signalTimeLeft),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        val percentage = if (viewModel.signalTotalDuration > 0) {
                                            viewModel.signalTimeLeft.toFloat() / viewModel.signalTotalDuration.toFloat()
                                        } else 0f

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .background(Slate950, CircleShape)
                                                .border(1.dp, Slate800, CircleShape)
                                                .padding(2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(percentage)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            colors = listOf(accentColor, accentColor.copy(alpha = 0.6f))
                                                        )
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "expired" -> {
                        viewModel.currentSignal?.let { signal ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Slate900, CircleShape)
                                        .border(1.dp, Slate800, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Check completed contracts",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Text(
                                    text = "CONTRACT EXPIRED",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )

                                Text(
                                    text = "The signal generation window for ${signal.pair} has closed.",
                                    color = Slate500,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Button(
                                    onClick = { viewModel.resetTerminal() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                                    border = BorderStroke(1.dp, Slate800),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Reset Terminal",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (!hasAccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Slate950.copy(alpha = 0.96f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(RubyRose.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                    .border(1.dp, RubyRose.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock padlock",
                                    tint = RubyRose,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Text(
                                text = "PRO ACCESS LOCK",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )

                            Text(
                                text = "You must purchase a plan or activate a code to unlock live analytical signal generation.",
                                color = Slate400,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )

                            Button(
                                onClick = { viewModel.setView("paywall") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(48.dp)
                                    .testTag("unlock_engine_trigger")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(CyanAccent, DarkBlueAccent)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Unlock Access Terminal",
                                        color = CarbonBlack,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.triggerGenerateSignal() },
                enabled = viewModel.activeSignalState != "analyzing" && hasAccess,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Slate800
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("generate_signal_action_button")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (viewModel.activeSignalState == "analyzing") {
                                Brush.linearGradient(listOf(Slate800, Slate800))
                            } else if (hasAccess) {
                                Brush.horizontalGradient(listOf(EmeraldGreen, Color(0xFF047857)))
                            } else {
                                Brush.horizontalGradient(listOf(CyanAccent, DarkBlueAccent))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (viewModel.activeSignalState == "analyzing") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Slate500,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "ANALYZING...",
                                color = Slate500,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        } else if (!hasAccess) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock trigger badge",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "UNLOCK ENGINE",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Bolt actions",
                                tint = CarbonBlack,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "GENERATE ACTION SIGNAL",
                                color = CarbonBlack,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        item {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )
            val borderGlowColor by infiniteTransition.animateColor(
                initialValue = CyanAccent.copy(alpha = 0.2f),
                targetValue = CyanAccent.copy(alpha = 0.8f),
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "borderGlow"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pocket_option_referral_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, borderGlowColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SuccessGreenBg, shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Market growth icon",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "OFFICIAL PARTNER BROKER",
                                    color = EmeraldGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .background(RubyRose, shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .alpha(pulseAlpha)
                                ) {
                                    Text(
                                        text = "FLASHING LINK",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = "Pocket Option Trading Platform",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Sign up using our official partner link to synchronize real-time automated trading signals directly with high-execution capability.",
                        color = Slate505,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    val poLinkState by viewModel.pocketOptionLink.collectAsState()
                    val targetLink = poLinkState?.value ?: "https://pocketoption.com/register/"

                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri(targetLink)
                            } catch (e: Exception) {
                                viewModel.showToast("Could not open partner link: browser application not found.", ToastType.ERROR)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkBlueAccent.copy(alpha = pulseAlpha)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("click_po_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "CLICK PO",
                                color = CarbonBlack,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            Icon(
                                imageVector = Icons.Default.DoubleArrow,
                                contentDescription = "Double Arrow Icon",
                                tint = CarbonBlack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaywallView(viewModel: DidsBoltViewModel) {
    val pricingPlans by viewModel.pricingPlans.collectAsState()
    val pendingPlan = viewModel.pendingPlan
    val uriHandler = LocalUriHandler.current

    var voucherInput by remember { mutableStateOf("") }
    var accessCodeInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("paywall_screen_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "← Back to Dashboard",
                color = Slate400,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { viewModel.setView("dashboard") }
                    .padding(vertical = 4.dp)
                    .testTag("paywall_back_trigger")
            )
        }

        if (pendingPlan == null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Activate Pro Access",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Instant automation and live prediction capabilities.",
                        color = Slate400,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Redeem Access Voucher",
                            color = Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = voucherInput,
                                onValueChange = { voucherInput = it },
                                placeholder = { Text("e.g., DIDS-FREE-PASS", color = Slate500, fontSize = 13.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Slate950,
                                    unfocusedContainerColor = Slate950
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("voucher_input_field"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                            )

                            Button(
                                onClick = {
                                    if (voucherInput.isNotBlank()) {
                                        viewModel.redeemVoucher(voucherInput)
                                        voucherInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("voucher_submit_button")
                            ) {
                                Text("Redeem", color = CarbonBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Click Plan to Order via Telegram",
                    color = Slate500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(pricingPlans) { plan ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.selectPlan(plan)
                            try {
                                uriHandler.openUri(TELEGRAM_EXECUTIVE_CHAT)
                            } catch (e: Exception) {
                                viewModel.showToast("Could not open Telegram: browser application not found.", ToastType.ERROR)
                            }
                        }
                        .testTag("price_plan_card_${plan.days}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.5.dp, Slate800)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(CyanAccentMuted, RoundedCornerShape(10.dp))
                                .border(0.5.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Buy from @DIDSBOLT",
                                color = CyanAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 110.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = plan.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = plan.price,
                                color = CyanAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = plan.desc,
                                color = Slate400,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        border = BorderStroke(1.dp, Slate800)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = "Wallet cards info",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Pro Activation Details",
                                    color = CyanAccent,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Text(
                                text = "Complete manual or signal wallet transfer matching your chosen tier.",
                                color = Slate400,
                                fontSize = 12.sp
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate950, RoundedCornerShape(12.dp))
                                    .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "VERIFICATION ID / REFERENCE",
                                        color = Slate500,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "@DIDSBOLT",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Column {
                                    Text(
                                        text = "SELECTED CONFIGURATION",
                                        color = Slate500,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "${pendingPlan.name} (${pendingPlan.price})",
                                        color = WhiteNeutral,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ENTER CONFIRMATION ACCESS CODE",
                                    color = Slate500,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )

                                OutlinedTextField(
                                    value = accessCodeInput,
                                    onValueChange = { accessCodeInput = it },
                                    placeholder = { Text("e.g. DIDS-XXXX-XXXX", color = Slate500, fontSize = 13.sp) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyanAccent,
                                        unfocusedBorderColor = Slate700,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Slate950,
                                        unfocusedContainerColor = Slate950
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("checkout_access_code_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                )

                                Text(
                                    text = "Type the verified Access Code received from Telegram admin (@DIDSBOLT) to authorize your payment configuration.",
                                    color = Slate500,
                                    fontSize = 11.sp
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.confirmPayment(accessCodeInput) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("confirm_payment_trigger")
                                ) {
                                    Text("PAID & CONFIRM ACCESS", color = CarbonBlack, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { viewModel.cancelPendingPayment() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    border = BorderStroke(1.dp, Slate700),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("cancel_checkout_trigger")
                                ) {
                                    Text("Choose Alternative Plan", color = Slate400, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsView(viewModel: DidsBoltViewModel) {
    val user = viewModel.currentUser ?: return
    var subSectionView by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current

    if (subSectionView == "2fa") {
        TwoFactorView(onBack = { subSectionView = null })
    } else if (subSectionView == "broker") {
        LinkBrokerView(onBack = { subSectionView = null })
    } else if (subSectionView == "history") {
        SignalHistoryView(viewModel = viewModel, onBack = { subSectionView = null })
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_screen_root"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account Settings",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )

                    Button(
                        onClick = { viewModel.setView("dashboard") },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Done", color = WhiteNeutral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Slate800, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.username.drop(1).take(1).uppercase(Locale.getDefault()),
                                    color = CyanAccent,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Text(
                                    text = user.username,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (user.id == "ADMIN_ROOT") "Root Administrative Officer" else "Verified Node Profile",
                                    color = Slate500,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (user.id != "ADMIN_ROOT") {
                            HorizontalDivider(color = Slate800)

                            Column {
                                Text(
                                    text = "SUBSCRIPTION STATUS",
                                    color = Slate500,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                val isPro = viewModel.hasAccess()
                                if (isPro && user.accessExpiresAt != null) {
                                    val formattedDate = remember(user.accessExpiresAt) {
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(user.accessExpiresAt))
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SuccessGreenBg, RoundedCornerShape(10.dp))
                                            .border(0.5.dp, EmeraldGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "PRO Active",
                                            color = EmeraldGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Ends $formattedDate",
                                            color = Slate400,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(ErrorRedBg, RoundedCornerShape(10.dp))
                                            .border(0.5.dp, RubyRose.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Inactive / Demo Only",
                                            color = RubyRose,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsLinkRow(
                        icon = Icons.Default.QrCode,
                        label = "2FA Security",
                        onClick = { subSectionView = "2fa" },
                        tag = "settings_2fa_trigger"
                    )

                    SettingsLinkRow(
                        icon = Icons.Default.Link,
                        label = "Link Broker Account",
                        onClick = { subSectionView = "broker" },
                        tag = "settings_broker_trigger"
                    )

                    SettingsLinkRow(
                        icon = Icons.Default.History,
                        label = "Signal History",
                        onClick = { subSectionView = "history" },
                        tag = "settings_history_trigger"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate900, RoundedCornerShape(12.dp))
                            .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.showToast("Redirecting to Telegram config channels...", ToastType.INFO)
                                try {
                                    uriHandler.openUri(TELEGRAM_EXECUTIVE_CHAT)
                                } catch (e: Exception) {
                                    viewModel.showToast("Could not open Telegram: browser application not found.", ToastType.ERROR)
                                }
                            }
                            .padding(14.dp)
                            .testTag("settings_support_trigger"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.HeadsetMic, "Help desk headset", tint = CyanAccent, modifier = Modifier.size(18.dp))
                            Text("Support (Telegram)", color = Slate400, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Chat with (@DIDSBOLT)",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.AutoMirrored.Filled.Send, "Send icon badge", tint = CyanAccent, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = RubyRose.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RubyRose.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("settings_logout_trigger")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out dial mark", tint = RubyRose)
                        Text(text = "Logout", color = RubyRose, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsLinkRow(icon: ImageVector, label: String, onClick: () -> Unit, tag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate900, RoundedCornerShape(12.dp))
            .border(1.dp, Slate800, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
            Text(text = label, color = Slate400, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open dial panel", tint = Slate700)
    }
}

@Composable
fun TwoFactorView(onBack: () -> Unit) {
    var hasEnabled2fa by remember { mutableStateOf(false) }
    var codeValue by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onBack() }
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left arrow", tint = Slate400)
                Text("Back", color = Slate400, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        item {
            Text(
                text = "2FA Security",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }

        item {
            if (!hasEnabled2fa) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Qr authentication mockup",
                            tint = Slate500,
                            modifier = Modifier.size(120.dp)
                        )

                        OutlinedTextField(
                            value = codeValue,
                            onValueChange = { codeValue = it.take(6) },
                            placeholder = { Text("Enter 6-digit code", color = Slate505, fontSize = 14.sp) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Slate950,
                                unfocusedContainerColor = Slate950
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )

                        Button(
                            onClick = {
                                if (codeValue.length == 6) {
                                    hasEnabled2fa = true
                                    codeValue = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Enable 2FA", color = CarbonBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Shield checks verified logo",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(64.dp)
                        )

                        Text(
                            text = "2FA Protection Activated",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { hasEnabled2fa = false },
                            colors = ButtonDefaults.buttonColors(containerColor = RubyRose.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, RubyRose.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("Disable 2FA", color = RubyRose, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinkBrokerView(onBack: () -> Unit) {
    var sessionKeyInput by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("Pocket Option") }
    var expanded by remember { mutableStateOf(false) }

    val platformOptions = listOf("Pocket Option", "Quotex", "Exnova", "Binomo")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onBack() }
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left navigation anchor", tint = Slate400)
                Text("Back", color = Slate400, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        item {
            Text(
                text = "Link Broker",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Select Platform",
                            color = Slate500,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate950, RoundedCornerShape(12.dp))
                                    .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                    .clickable { expanded = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedPlatform, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ArrowDropDown, "Platform picker arrow drop down", tint = CyanAccent)
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(Slate900)
                                    .border(1.dp, Slate800)
                            ) {
                                platformOptions.forEach { platform ->
                                    DropdownMenuItem(
                                        text = { Text(platform, color = Color.White, fontSize = 13.sp) },
                                        onClick = {
                                            selectedPlatform = platform
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "API KEY / SESSION TOKEN",
                            color = Slate500,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        OutlinedTextField(
                            value = sessionKeyInput,
                            onValueChange = { sessionKeyInput = it },
                            placeholder = { Text("Enter session token key...", color = Slate505, fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Slate950,
                                unfocusedContainerColor = Slate950
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (sessionKeyInput.isNotBlank()) {
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Connect Account", color = CarbonBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SignalHistoryView(viewModel: DidsBoltViewModel, onBack: () -> Unit) {
    val history by viewModel.signalHistory.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onBack() }
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Return arrow navigation back", tint = Slate400)
                Text("Back", color = Slate400, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        item {
            Text(
                text = "Signal History",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }

        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No trading positions executed yet.",
                        color = Slate500,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(history) { signal ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = signal.pair,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = signal.time,
                                color = Slate500,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = signal.direction,
                                color = if (signal.direction == "CALL") EmeraldGreen else RubyRose,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = signal.result,
                                color = if (signal.result == "WIN") EmeraldGreen else RubyRose,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginView(viewModel: DidsBoltViewModel) {
    var username by remember { mutableStateOf("") }
    var passwordOverrideInput by remember { mutableStateOf("") }
    var isAdminStateMode by remember { mutableStateOf(false) }
    var secretsTapCount by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("login_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        color = CyanAccent.copy(alpha = 0.05f),
                        radius = 220.dp.toPx(),
                        center = Offset(-100f, 200f)
                    )
                    drawCircle(
                        color = DarkBlueAccent.copy(alpha = 0.05f),
                        radius = 240.dp.toPx(),
                        center = Offset(size.width + 100f, size.height - 200f)
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .border(1.dp, CyanAccent.copy(alpha = 0.35f), RoundedCornerShape(32.dp))
                        .background(Slate950)
                        .padding(4.dp)
                ) {
                    LogoImage(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)))
                }

                Text(
                    text = "DIDSBOLT",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isAdminStateMode) "Authorized Executive Deck" else "Professional OTC Trading Analytics",
                    color = if (isAdminStateMode) AmberVibe else Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.alpha(0.85f)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, if (isAdminStateMode) AmberVibe.copy(alpha = 0.2f) else CyanAccent.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = if (isAdminStateMode) "Admin ID" else "Telegram / Username",
                            color = Slate500,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = { Text(if (isAdminStateMode) "admin" else "@username", color = Slate505, fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isAdminStateMode) AmberVibe else CyanAccent,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Slate950,
                                unfocusedContainerColor = Slate950
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Person, "User inputs card ID indicator icon", tint = Slate500, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_username_input")
                        )
                    }

                    if (isAdminStateMode) {
                        Column(
                            modifier = Modifier.animateContentSize()
                        ) {
                            Text(
                                text = "Security Override Access Key",
                                color = Slate500,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            OutlinedTextField(
                                value = passwordOverrideInput,
                                onValueChange = { passwordOverrideInput = it },
                                placeholder = { Text("••••••••••••", color = Slate505) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberVibe,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Slate950,
                                    unfocusedContainerColor = Slate950
                                ),
                                leadingIcon = {
                                    Icon(Icons.Default.VpnKey, "Key security icons override input", tint = Slate505, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("login_password_input")
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.login(username, isAdminStateMode, passwordOverrideInput)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAdminStateMode) AmberVibe else CyanAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_trigger")
                    ) {
                        Text(
                            text = if (isAdminStateMode) "Verify Access" else "Establish Connection",
                            color = CarbonBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Slate800.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                secretsTapCount += 1
                                if (secretsTapCount >= 3) {
                                    isAdminStateMode = !isAdminStateMode
                                    secretsTapCount = 0
                                }
                            }
                            .testTag("hidden_admin_trigger_node")
                    )

                    if (secretsTapCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$secretsTapCount/3 Taps",
                            color = Slate500,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

val Slate505 = Slate500.copy(alpha = 0.5f)

@Composable
fun LogoImage(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.dids_bolt_logo_1779791640006),
        contentDescription = "DIDS BOLT SIGNAL Premium Mascot Logo",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

fun formatTimeLeft(seconds: Int): String {
    if (seconds >= 3600) {
        val h = (seconds / 3600).toString().padStart(2, '0')
        val m = ((seconds % 3600) / 60).toString().padStart(2, '0')
        val s = (seconds % 60).toString().padStart(2, '0')
        return "$h:$m:$s"
    }
    val m = (seconds / 60).toString().padStart(2, '0')
    val s = (seconds % 60).toString().padStart(2, '0')
    return "$m:$s"
}

@Composable
fun ToastWidget(toast: ToastState, onDismiss: () -> Unit) {
    val containerBg = when (toast.type) {
        ToastType.SUCCESS -> SuccessGreenBg
        ToastType.ERROR -> ErrorRedBg
        ToastType.INFO -> InfoCyanBg
    }
    val borderColor = when (toast.type) {
        ToastType.SUCCESS -> EmeraldGreen
        ToastType.ERROR -> RubyRose
        ToastType.INFO -> CyanAccent
    }
    val textColor = when (toast.type) {
        ToastType.SUCCESS -> Color(0xFFD1FAE5)
        ToastType.ERROR -> Color(0xFFFFE4E6)
        ToastType.INFO -> Color(0xFFECFEFF)
    }

    LaunchedEffect(toast) {
        delay(3200)
        onDismiss()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = borderColor.copy(alpha = 0.5f), shape = RoundedCornerShape(50.dp))
            .clickable { onDismiss() }
            .testTag("floating_global_toast"),
        color = containerBg.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(50.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = when (toast.type) {
                    ToastType.SUCCESS -> Icons.Default.CheckCircle
                    ToastType.ERROR -> Icons.Default.Error
                    ToastType.INFO -> Icons.Default.Info
                },
                contentDescription = "Notification type status icons",
                tint = borderColor,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = toast.message,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
