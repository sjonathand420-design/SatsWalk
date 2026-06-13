package com.example

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.*
import com.example.ui.SatsWalkViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

// Bottom Tab Navigation Tokens
private enum class SelectedTab {
    DASHBOARD, UPGRADES, GAMES, WALLET
}

@Composable
fun MainAppScreen(
    viewModel: SatsWalkViewModel = viewModel()
) {
    val userProgress by viewModel.userProgress.collectAsState()
    val todayClaims by viewModel.todayClaims.collectAsState()
    val withdrawals by viewModel.withdrawals.collectAsState()
    val pointTransactions by viewModel.pointTransactions.collectAsState()
    val wheelUpgradePurchased by viewModel.wheelUpgradePurchased.collectAsState()

    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val isAdPlaying by viewModel.isAdPlaying.collectAsState()
    val adPlacement by viewModel.adPlacement.collectAsState()

    var currentTab by remember { mutableStateOf(SelectedTab.DASHBOARD) }

    // Dialog sheets
    var claimDialogMilestone by remember { mutableStateOf<Int?>(null) }
    var showDevTools by remember { mutableStateOf(false) }
    var showLinkedDevices by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(1.dp, BorderColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = currentTab == SelectedTab.DASHBOARD,
                    onClick = { currentTab = SelectedTab.DASHBOARD },
                    label = { Text("Walk", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Text("👣", fontSize = 20.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = BitcoinOrange,
                        indicatorColor = BitcoinOrange,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_walk")
                )

                NavigationBarItem(
                    selected = currentTab == SelectedTab.UPGRADES,
                    onClick = { currentTab = SelectedTab.UPGRADES },
                    label = { Text("Shop", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Text("⭐", fontSize = 20.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = BitcoinGold,
                        indicatorColor = BitcoinGold,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_shop")
                )

                NavigationBarItem(
                    selected = currentTab == SelectedTab.GAMES,
                    onClick = { currentTab = SelectedTab.GAMES },
                    label = { Text("Play", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Text("🎮", fontSize = 20.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = AmberYellow,
                        indicatorColor = AmberYellow,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_play")
                )

                NavigationBarItem(
                    selected = currentTab == SelectedTab.WALLET,
                    onClick = { currentTab = SelectedTab.WALLET },
                    label = { Text("Wallet", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Text("⚡", fontSize = 20.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = BitcoinOrange,
                        indicatorColor = BitcoinOrange,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_wallet")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
        ) {
            // Main views switcher
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(250),
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    SelectedTab.DASHBOARD -> DashboardTab(
                        userProgress = userProgress,
                        todayClaims = todayClaims,
                        viewModel = viewModel,
                        onClaimClick = { claimDialogMilestone = it },
                        onDevClick = { showDevTools = !showDevTools },
                        onLinkClick = { showLinkedDevices = true }
                    )
                    SelectedTab.UPGRADES -> UpgradesTab(
                        userProgress = userProgress,
                        wheelUpgradePurchased = wheelUpgradePurchased,
                        onLevelUpgrade = { viewModel.upgradeAccountLevel() },
                        onLimitUpgrade = { viewModel.upgradeWithdrawalLimit() },
                        onStepLimitUpgrade = { viewModel.upgradeStepLimit() },
                        onWheelUpgrade = { viewModel.buyWheelUpgrade() }
                    )
                    SelectedTab.GAMES -> GamesTab(
                        viewModel = viewModel
                    )
                    SelectedTab.WALLET -> {
                        val btcPriceUsd by viewModel.btcPriceUsd.collectAsState()
                        val btcPriceCad by viewModel.btcPriceCad.collectAsState()
                        WalletTab(
                            userProgress = userProgress,
                            withdrawals = withdrawals,
                            pointTransactions = pointTransactions,
                            btcPriceUsd = btcPriceUsd,
                            btcPriceCad = btcPriceCad,
                            viewModel = viewModel,
                            onCashoutRequest = { invoice, sats ->
                                viewModel.cashoutToLightning(invoice, sats)
                            }
                        )
                    }
                }
            }

            // Global Error Overlay Message
            errorMessage?.let { message ->
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("OK", color = BitcoinOrange)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(message)
                }
            }

            // Global Success Overlay Message
            successMessage?.let { message ->
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("DISMISS", color = AccentGreen)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = SurfaceDarkVariant,
                    contentColor = TextPrimary
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, "Success Icon", tint = AccentGreen, modifier = Modifier.padding(end = 8.dp))
                        Text(message, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Claim Reward Selector Dialog Box
            claimDialogMilestone?.let { milestone ->
                MilestoneClaimSelectionDialog(
                    milestone = milestone,
                    onClaimBase = {
                        claimDialogMilestone = null
                        viewModel.claimMilestoneReward(milestone, watchAd = false)
                    },
                    onClaimMultiplier = {
                        claimDialogMilestone = null
                        viewModel.claimMilestoneReward(milestone, watchAd = true)
                    },
                    onDismiss = { claimDialogMilestone = null }
                )
            }

            // Fullscreen Ad Playback Simulator Screen
            if (isAdPlaying) {
                FullAdSimulatorOverlay(
                    placement = adPlacement ?: "VIDEO_AD",
                    onAdCompleted = { viewModel.completeAdPlayback() },
                    onAdSkipped = { viewModel.cancelAdPlayback() }
                )
            }

            // Collapsible Floating Dev Simulation Controller panel
            AnimatedVisibility(
                visible = showDevTools,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(SurfaceDark)
                    .border(2.dp, BitcoinOrange, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(enabled = false) {}
                    .padding(24.dp)
            ) {
                DevToolsSimulationCenter(
                    currentSteps = userProgress?.currentSteps ?: 0,
                    onAddSteps = { viewModel.addWalkSteps(it) },
                    onSetSteps = { viewModel.setWalkSteps(it) },
                    onResetAll = { viewModel.devResetProgress() },
                    onClose = { showDevTools = false }
                )
            }

            // Linked Devices Dialog Screen
            if (showLinkedDevices) {
                LinkedDevicesDialog(
                    viewModel = viewModel,
                    onDismissRequest = { showLinkedDevices = false }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 1. DASHBOARD TAB VIEW
// -----------------------------------------------------------------------------
@Composable
private fun DashboardTab(
    userProgress: UserProgressEntity?,
    todayClaims: List<MilestoneClaimEntity>,
    viewModel: SatsWalkViewModel,
    onClaimClick: (Int) -> Unit,
    onDevClick: () -> Unit,
    onLinkClick: () -> Unit
) {
    val steps = userProgress?.currentSteps ?: 0
    val targetSteps = 20000

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // App title header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SATS WALK",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = BitcoinOrange,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Proof-of-Walk Consensus",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Linked Devices button
                    IconButton(
                        onClick = onLinkClick,
                        modifier = Modifier
                            .background(GlowOrange, CircleShape)
                            .border(1.dp, BitcoinOrange, CircleShape)
                            .testTag("open_linked_devices_button")
                    ) {
                        Text("🔗", fontSize = 16.sp)
                    }

                    // Emulator tool trigger
                    IconButton(
                        onClick = onDevClick,
                        modifier = Modifier
                            .background(GlowOrange, CircleShape)
                            .border(1.dp, BitcoinOrange, CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Open Simulator Setup", tint = BitcoinOrange)
                    }
                }
            }
        }

        // Circular steps dial widget
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(190.dp)
                    ) {
                        // Background circle
                        Canvas(modifier = Modifier.size(175.dp)) {
                            drawCircle(
                                color = BorderColor,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Glow indicator
                        val progressSweep = (steps.toFloat() / targetSteps.toFloat()).coerceIn(0f, 1f) * 360f

                        Canvas(modifier = Modifier.size(175.dp)) {
                            drawArc(
                                color = BitcoinOrange,
                                startAngle = -90f,
                                sweepAngle = progressSweep,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Inner metric stats display
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "👣",
                                fontSize = 28.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "%,d".format(steps),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "of %,d Daily Steps".format(targetSteps),
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Account stats mini bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CURRENT PT Balance", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Text("${userProgress?.currentPoints ?: 0} PT", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BitcoinOrange)
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderColor))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ecosystem Level", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Text("LEVEL ${userProgress?.level ?: 1} / 10", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BitcoinGold)
                        }
                    }
                }
            }
        }

        // Section label for walk claim milestones
        item {
            Text(
                text = "DAILY STEP REDEMPTIONS (500 Step Intervals)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Grid-like listing of the 40 step milestones from 500 up to 20,000 steps
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Generate all 40 milestones in batches of 4 columns for easy visual paging
                for (offset in 500..20000 step 2000) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0 until 4) {
                            val milestone = offset + (i * 500)
                            if (milestone <= 20000) {
                                Box(modifier = Modifier.weight(1f)) {
                                    MilestoneGridCell(
                                        milestone = milestone,
                                        currentSteps = steps,
                                        maxClaimableSteps = userProgress?.maxClaimableSteps ?: 5000,
                                        alreadyClaimed = todayClaims.any { it.stepMilestone == milestone },
                                        onClaim = { onClaimClick(milestone) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            LegalDisclaimerCard(
                viewModel = viewModel,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun MilestoneGridCell(
    milestone: Int,
    currentSteps: Int,
    maxClaimableSteps: Int,
    alreadyClaimed: Boolean,
    onClaim: () -> Unit
) {
    val isCapped = milestone > maxClaimableSteps
    val isAvailable = currentSteps >= milestone && !alreadyClaimed && !isCapped

    val displayLabel = if (milestone >= 1000) {
        val decimals = if (milestone % 1000 == 500) ".5k" else "k"
        "${milestone / 1000}$decimals"
    } else {
        "$milestone"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                alreadyClaimed -> DarkBg
                isCapped -> DisabledBg.copy(alpha = 0.5f)
                isAvailable -> SurfaceDarkVariant
                else -> DisabledBg
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                alreadyClaimed -> AccentGreen.copy(alpha = 0.5f)
                isCapped -> Color.Red.copy(alpha = 0.3f)
                isAvailable -> BitcoinOrange
                else -> BorderColor
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = displayLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (alreadyClaimed || isAvailable) TextPrimary else TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Claim buttons states
            when {
                alreadyClaimed -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Reward claimed indicator",
                            tint = AccentGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            "CLAIMED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }
                }

                isCapped -> {
                    // Locked by shop Upgrade limit
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            "LOCKED (LIMIT)",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Red
                        )
                    }
                }

                isAvailable -> {
                    Button(
                        onClick = onClaim,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .testTag("claim_btn_$milestone")
                    ) {
                        Text(
                            "CLAIM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = DarkBg
                        )
                    }
                }

                else -> {
                    // Locked progress state
                    val progressValue = (currentSteps.toFloat() / milestone.toFloat()).coerceIn(0f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progressValue },
                            color = DisabledText,
                            trackColor = BorderColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${(progressValue * 100).toInt()}%",
                            fontSize = 8.sp,
                            color = DisabledText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 2. UPGRADES (SHOP) TAB VIEW
// -----------------------------------------------------------------------------
@Composable
private fun UpgradesTab(
    userProgress: UserProgressEntity?,
    wheelUpgradePurchased: Boolean,
    onLevelUpgrade: () -> Unit,
    onLimitUpgrade: () -> Unit,
    onStepLimitUpgrade: () -> Unit,
    onWheelUpgrade: () -> Unit
) {
    val currentLevel = userProgress?.level ?: 1
    val currentBalance = userProgress?.currentPoints ?: 0
    val currentLimit = userProgress?.withdrawalLimitSats ?: 100

    // Pricing formulas
    val cashoutCost = currentLevel.toLong() * 50000L
    val limitCost = 100000L
    val stepLimitCost = 50000L

    val currentRate = currentLevel.toDouble() * 0.5

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Text(
                "SHOP NODE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = BitcoinGold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                "Expend points to upgrade transaction multipliers and parameters.",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Points available banner card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("AVAILABLE SPEND CREDITS", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text("$currentBalance PTS", fontSize = 28.sp, fontWeight = FontWeight.Black, color = BitcoinOrange)
                    }
                    Box(
                        modifier = Modifier
                            .background(GlowOrange, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Active Ledger", color = BitcoinOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // UPGRADE ACCOUNT LEVEL CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, if (currentLevel < 10) BitcoinGold else BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, "Level upgrades badge", tint = BitcoinGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Node Verification Level", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Increases Satoshi payout rates for walking and games.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        // Current Level Indicator badge
                        Box(
                            modifier = Modifier
                                .background(BitcoinGold, RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("LVL $currentLevel", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metrics Comparison
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CURRENT RATE", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("$currentRate Sats/1k pts", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(16.dp)
                        ) {
                            Text("→", color = BitcoinGold, fontWeight = FontWeight.Black)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NEXT LEVEL RATE", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            val nextRate = (currentLevel + 1).toDouble() * 0.5
                            Text(
                                text = if (currentLevel < 10) "$nextRate Sats/1k pts" else "MAX LEVEL REACHED",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (currentLevel < 10) BitcoinGold else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentLevel < 10) {
                        Button(
                            onClick = onLevelUpgrade,
                            colors = ButtonDefaults.buttonColors(containerColor = BitcoinGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("upgrade_level_button")
                        ) {
                            Text(
                                text = "LEVEL UP FOR ${"%,d".format(cashoutCost)} POINTS",
                                fontWeight = FontWeight.Black,
                                color = DarkBg,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(disabledContainerColor = DisabledBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("MAXIMUM VERIFICATION LEVEL 10 ACHIEVED", color = DisabledText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // UPGRADE WITHDRAWAL LIMIT CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, if (currentLimit < 300) BitcoinOrange else BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, "Limiter upgrades badge", tint = BitcoinOrange, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sats Daily Limit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Expands maximum daily allowable withdrawal capacity.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(BitcoinOrange, RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("$currentLimit SATS/DAY", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metrics Comparison
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CURRENT CAP", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("$currentLimit Satoshis", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(16.dp)
                        ) {
                            Text("→", color = BitcoinOrange, fontWeight = FontWeight.Black)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("EXPANDED CAP", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            val nextLimit = currentLimit + 10
                            Text(
                                text = if (currentLimit < 300) "$nextLimit Satoshis" else "MAX WINDOW REACHED",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (currentLimit < 300) BitcoinOrange else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentLimit < 300) {
                        Button(
                            onClick = onLimitUpgrade,
                            colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("upgrade_limit_button")
                        ) {
                            Text(
                                text = "EXPAND LIMIT (+10 SATS) FOR ${"%,d".format(limitCost)} PTS",
                                fontWeight = FontWeight.Black,
                                color = DarkBg,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(disabledContainerColor = DisabledBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("MAXIMUM WITHDRAWAL CEILING (300 SATS) ACHIEVED", color = DisabledText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // UPGRADE STEP REWARD LIMIT CARD
        item {
            val maxClaimableSteps = userProgress?.maxClaimableSteps ?: 5000
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, if (maxClaimableSteps < 20000) BitcoinOrange else BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, "Steps limiter upgrades badge", tint = BitcoinOrange, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Step Rewards Cap", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Unlocks further high-step milestones (up to 20k steps) for claimable points.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(BitcoinOrange, RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("$maxClaimableSteps STEPS", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metrics Comparison
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CURRENT CAP", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("$maxClaimableSteps Steps", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(16.dp)
                        ) {
                            Text("→", color = BitcoinOrange, fontWeight = FontWeight.Black)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("EXPANDED CAP", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            val nextStepLimit = maxClaimableSteps + 1000
                            Text(
                                text = if (maxClaimableSteps < 20000) "$nextStepLimit Steps" else "MAX TIER REACHED",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (maxClaimableSteps < 20000) BitcoinOrange else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (maxClaimableSteps < 20000) {
                        Button(
                            onClick = onStepLimitUpgrade,
                            colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("upgrade_step_limit_button")
                        ) {
                            Text(
                                text = "EXPAND REWARDS (+1,000 STEPS) FOR ${"%,d".format(stepLimitCost)} PTS",
                                fontWeight = FontWeight.Black,
                                color = DarkBg,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(disabledContainerColor = DisabledBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("MAXIMUM REWARDS THRESHOLD (20,000 STEPS) UNLOCKED", color = DisabledText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // UPGRADE SPINNING WHEEL LIMIT CARD
        item {
            val wheelUpgradeCost = 20000L
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, if (!wheelUpgradePurchased) AmberYellow else BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, "Spin upgrades badge", tint = AmberYellow, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Spin Wheel Volatility Upgrade",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(AmberYellow.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("BOOSTER", color = AmberYellow, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Further boost your daily capacity on the Sats Spinning Wheel. This upgrade permanently increases your daily spins limit from 10 to 15 plays.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("STANDARD SPIN LIMIT", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("10 Spins / Day", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(16.dp)
                        ) {
                            Text("→", color = AmberYellow, fontWeight = FontWeight.Black)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("UPGRADED SPIN LIMIT", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                text = "15 Spins / Day",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (wheelUpgradePurchased) TextSecondary else AmberYellow
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!wheelUpgradePurchased) {
                        Button(
                            onClick = onWheelUpgrade,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberYellow),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("upgrade_wheel_limit_button")
                        ) {
                            Text(
                                text = "UNLOCK DOUBLE SPIN CAP FOR ${"%,d".format(wheelUpgradeCost)} PTS",
                                fontWeight = FontWeight.Black,
                                color = DarkBg,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(disabledContainerColor = DisabledBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("GOLDEN DOUBLE SPIN LIMIT (10 SPINS) UNLOCKED", color = DisabledText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 3. PLAY GAMES TAB VIEW
// -----------------------------------------------------------------------------
@Composable
private fun GamesTab(
    viewModel: SatsWalkViewModel
) {
    var activeGame by remember { mutableStateOf<String?>(null) }

    if (activeGame == null) {
        GameSelectorHub(
            viewModel = viewModel,
            onSelectGame = { activeGame = it }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { activeGame = null },
                    modifier = Modifier.background(SurfaceDark, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back to games list",
                        tint = AmberYellow
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "BACK TO ARCADE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            when (activeGame) {
                "btc_predictor" -> BtcPredictorGame(viewModel)
                "coin_flip" -> CoinFlipGame(viewModel)
                "spinning_wheel" -> SpinningWheelGame(viewModel)
                "dice_game" -> DiceGame(viewModel)
            }
        }
    }
}

@Composable
private fun GameSelectorHub(
    viewModel: SatsWalkViewModel,
    onSelectGame: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Text(
                text = "SATS WALKING ARCADE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = AmberYellow,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                text = "Boost your point accumulation by playing daily interactive arcade activities. Earn standard points or watch optional ads for 5x multipliers!",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Game Card 1: BTC 1PM DAILY PREDICTOR
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BitcoinOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Price prediction badge",
                                tint = BitcoinOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "BTC Daily Predictor",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("ACTIVE", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Predict if the live BTC USD price is higher or lower than the official baseline from yesterday at 1:00 PM EST.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onSelectGame("btc_predictor") },
                        colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("launch_btc_predictor_btn")
                    ) {
                        Text("LAUNCH GAME", fontWeight = FontWeight.Black, color = DarkBg, fontSize = 12.sp)
                    }
                }
            }
        }

        // Game Card 2: LUCKY SATS COIN FLIP
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Coin Flip badge",
                                tint = AmberYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Lucky Sats Coin Toss",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("ACTIVE", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Flip a physical golden coin and guess if it lands on HEADS (the bitcoin logo) or TAILS. 50/50 odds to win and claim!",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onSelectGame("coin_flip") },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberYellow),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("launch_coin_flip_btn")
                    ) {
                        Text("LAUNCH GAME", fontWeight = FontWeight.Black, color = DarkBg, fontSize = 12.sp)
                    }
                }
            }
        }

        // Game Card 3: SATS SPINNING WHEEL
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, AmberYellow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Spin wheel badge",
                                tint = AmberYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Sats Spinning Wheel",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("ACTIVE", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Spin our 10-slot volatility wheel to win up to 250 credit points! Upgrade available to raise limits.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onSelectGame("spinning_wheel") },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberYellow),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("launch_spinning_wheel_btn")
                    ) {
                        Text("LAUNCH WHEEL", fontWeight = FontWeight.Black, color = DarkBg, fontSize = 12.sp)
                    }
                }
            }
        }

        // Game Card 4: SATS LUCKY DICE TOSS
        item {
            val userProgress by viewModel.userProgress.collectAsState()
            val stepsToday = userProgress?.currentSteps ?: 0
            val isUnlocked = stepsToday >= 3000
            val dicePlaysRemaining by viewModel.dicePlaysRemainingToday.collectAsState()
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked) SurfaceDark else SurfaceDark.copy(alpha = 0.6f)
                ),
                border = BorderStroke(
                    1.dp, 
                    if (isUnlocked) AmberYellow else BorderColor.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Dice Game Badge",
                                tint = if (isUnlocked) AmberYellow else TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Lucky Dice Roll",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) TextPrimary else TextSecondary
                            )
                        }

                        if (isUnlocked) {
                            Box(
                                modifier = Modifier
                                    .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("UNLOCKED", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("LOCKED", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Step reward! Unlock 3 free dice rolls daily once you walk 3,000+ steps. No ads required! Win 50 points × value of the rolled die (up to 300 points per roll)!",
                        fontSize = 12.sp,
                        color = if (isUnlocked) TextSecondary else DisabledText
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress metric
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TODAY'S STEPS: $stepsToday / 3,000",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) AccentGreen else AmberYellow
                        )
                        if (isUnlocked) {
                            Text(
                                text = "$dicePlaysRemaining/3 ROLLS LEFT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AmberYellow
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = (stepsToday.toFloat() / 3000f).coerceIn(0f, 1f),
                        color = if (isUnlocked) AccentGreen else AmberYellow,
                        trackColor = BorderColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onSelectGame("dice_game") },
                        enabled = isUnlocked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberYellow,
                            disabledContainerColor = DisabledBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("launch_dice_game_btn")
                    ) {
                        Text(
                            text = if (isUnlocked) "LAUNCH GAME" else "LOCKED: WALK 3,000 STEPS", 
                            fontWeight = FontWeight.Black, 
                            color = if (isUnlocked) DarkBg else DisabledText, 
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BtcPredictorGame(
    viewModel: SatsWalkViewModel
) {
    val btcRealUsd by viewModel.btcPriceUsd.collectAsState()
    val btcYesterday1PmUsd by viewModel.btcYesterday1PmUsd.collectAsState()
    
    val predTargetDate by viewModel.predTargetDate.collectAsState()
    val predChoice by viewModel.predChoice.collectAsState()
    val predBaselinePrice by viewModel.predBaselinePrice.collectAsState()
    val predStatus by viewModel.predStatus.collectAsState()
    val predResolvedPrice by viewModel.predResolvedPrice.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    var currentBtcPrice by remember { mutableDoubleStateOf(0.0) }
    var showRewardsPopup by remember { mutableStateOf(false) }

    LaunchedEffect(btcRealUsd) {
        if (btcRealUsd > 0.0) {
            currentBtcPrice = btcRealUsd
        }
    }

    val priceHistory = remember { mutableStateListOf<Double>() }
    val yesterdayTarget = btcYesterday1PmUsd ?: 92518.40

    if (priceHistory.isEmpty() && currentBtcPrice > 0.0) {
        val base = yesterdayTarget
        for (i in 0..12) {
            priceHistory.add(base - 150.0 + (i * 20) + (sin(i.toDouble()) * 120))
        }
        if (priceHistory.isNotEmpty()) {
            priceHistory[priceHistory.size - 1] = currentBtcPrice
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "BTC 1PM DAILY PREDICTOR",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = AmberYellow,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Forecast if the BTC USD price will be HIGHER ▲ or LOWER ▼ at 1:00 PM EST today than yesterday's 1:00 PM baseline. Earn 50 base points + multipliers!",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                
                IconButton(
                    onClick = {
                        viewModel.fetchBtcPrices()
                    },
                    modifier = Modifier.background(SurfaceDark, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh bitcoin prices",
                        tint = AmberYellow
                    )
                }
            }
        }

        // Live Price & Baseline Board
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "BASELINE (YESTERDAY 1:00 PM EST)",
                                fontSize = 9.sp,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$${"%,.2f".format(yesterdayTarget)} USD",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "CURRENT LIVE BTC PRICE",
                                fontSize = 9.sp,
                                color = AccentGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$${"%,.2f".format(currentBtcPrice)} USD",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = AmberYellow
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Line Chart Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val path = Path()
                            val width = size.width
                            val height = size.height

                            val pricesList = (priceHistory.toList() + yesterdayTarget).filter { it > 0.0 }
                            val maxPrice = (pricesList.maxOrNull() ?: (yesterdayTarget + 100)) + 60
                            val minPrice = (pricesList.minOrNull() ?: (yesterdayTarget - 100)) - 60
                            val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

                            val lineY = height - (((yesterdayTarget - minPrice) / priceRange) * height).toFloat()
                            drawLine(
                                color = Color.Red,
                                start = Offset(0f, lineY),
                                end = Offset(width, lineY),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            )

                            if (priceHistory.size > 1) {
                                val stepX = width / (priceHistory.size - 1)
                                priceHistory.forEachIndexed { index, prc ->
                                    val x = index * stepX
                                    val y = height - (((prc - minPrice) / priceRange) * height).toFloat()
                                    if (index == 0) {
                                        path.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                    }
                                }

                                drawPath(
                                    path = path,
                                    color = AmberYellow,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )

                                val lastX = width
                                val lastY = height - (((priceHistory.last() - minPrice) / priceRange) * height).toFloat()
                                drawCircle(
                                    color = AmberYellow,
                                    radius = 6.dp.toPx(),
                                    center = Offset(lastX - 2.dp.toPx(), lastY)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "--- Baseline Target Reference",
                            fontSize = 9.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Live Trajectory",
                            fontSize = 9.sp,
                            color = AmberYellow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Gameplay Active Predictor state UI switcher card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (predStatus) {
                        "PENDING" -> {
                            val targetDateStr = predTargetDate ?: ""
                            val choice = predChoice ?: "HIGHER"
                            val baselineVal = predBaselinePrice
                            
                            Text(
                                text = "ACTIVE FORECAST SUBMITTED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberYellow,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier
                                    .background(DarkBg, RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("TARGET PAYOUT DATE", fontSize = 9.sp, color = TextSecondary)
                                    Text(
                                        text = "$targetDateStr @ 1:00 PM EST",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("BASELINE PRICE", fontSize = 9.sp, color = TextSecondary)
                                    Text(
                                        text = "$${"%,.2f".format(baselineVal)} USD",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = TextPrimary
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("YOUR FORECAST", fontSize = 9.sp, color = TextSecondary)
                                    val isHigher = choice == "HIGHER"
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isHigher) AccentGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isHigher) "HIGHER ▲" else "LOWER ▼",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = if (isHigher) AccentGreen else Color.Red
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { viewModel.checkAndResolvePrediction() },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberYellow),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("check_prediction_btn")
                            ) {
                                Text("CHECK PAYOUT & RESOLVE", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Payout requests can only be completed after 1:00 PM EST of the target prediction date when crypto feeds compile real data.",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp
                            )
                        }

                        "RESOLVED_WIN" -> {
                            val choice = predChoice ?: "HIGHER"
                            val baselineVal = predBaselinePrice
                            val actualVal = predResolvedPrice

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success check icon",
                                    tint = AccentGreen,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "PREDICTION MATCHED!",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = AccentGreen
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your daily forecast of $choice has been checked successfully against official index records.\nBaseline: $${"%,.2f".format(baselineVal)} | Checked Price: $${"%,.2f".format(actualVal)}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = { showRewardsPopup = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("guess_claim_trigger")
                                ) {
                                    Text("CLAIM REWARD POINTS", color = DarkBg, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        "RESOLVED_LOSE" -> {
                            val choice = predChoice ?: "HIGHER"
                            val baselineVal = predBaselinePrice
                            val actualVal = predResolvedPrice

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Failed prediction icon",
                                    tint = Color.Red,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "PREDICTION FAILED",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color.Red
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "The actual outcome did not match your forecast of $choice.\nBaseline: $${"%,.2f".format(baselineVal)} | Actual Price: $${"%,.2f".format(actualVal)}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = { viewModel.resetPredictionAfterLoss() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("loss_continue_btn")
                                ) {
                                    Text("CONTINUE ARCADE", color = TextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        else -> {
                            val tz = TimeZone.getTimeZone("America/New_York")
                            val nowCal = Calendar.getInstance(tz)
                            val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)
                            val predTargetLabel = if (currentHour < 13) "TODAY" else "TOMORROW"
                            
                            Text(
                                text = "PLACE DAILY 1:00 PM EST PREDICTION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            Text(
                                text = "Predict for: $predTargetLabel @ 1:00 PM EST",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = AmberYellow,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.placePrediction("HIGHER") },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("guess_up_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("▲", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("HIGHER", fontWeight = FontWeight.Black, color = DarkBg)
                                    }
                                }

                                Button(
                                    onClick = { viewModel.placePrediction("LOWER") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("guess_down_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("▼", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("LOWER", fontWeight = FontWeight.Black, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Developer Cheat Resolution Bypass Section for Testing (Always extremely polite & structured)
        if (predStatus == "PENDING") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🛠️ TESTER TIME FAST-FORWARD TOOL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BitcoinOrange,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Avoid waiting 24 hours! Simulate the arrival of 1:00 PM EST immediately to test the payouts engine.",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.devSimulateResolution(simulateWin = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("dev_force_win_btn")
                            ) {
                                Text("SIMULATE WIN", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.devSimulateResolution(simulateWin = false) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("dev_force_loss_btn")
                            ) {
                                Text("SIMULATE LOSS", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive claim points dialog popup for games victories
    if (showRewardsPopup) {
        Dialog(
            onDismissRequest = { showRewardsPopup = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Confirm Icon",
                        tint = AccentGreen,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Claim Victory Points!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose your reward allocation option for this win. Playing video ads from AppLovin/Unity grants a 5x multiplier bonus!",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showRewardsPopup = false
                            viewModel.claimPredictionReward(watchAd = false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("Get 50 Base Points", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            showRewardsPopup = false
                            viewModel.claimPredictionReward(watchAd = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("Watch AD to get 5x (250 PTS!)", color = DarkBg, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoinFlipGame(
    viewModel: SatsWalkViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val coinFlipPlaysUsed by viewModel.coinFlipPlaysUsedOfToday.collectAsState()
    val playsRemaining = (20 - coinFlipPlaysUsed).coerceAtLeast(0)

    var isFlipping by remember { mutableStateOf(false) }
    var coinFace by remember { mutableStateOf("BTC") } // "BTC" or "TAILS"
    var selectedSide by remember { mutableStateOf<String?>(null) } // "HEADS" or "TAILS"
    var flipResultState by remember { mutableStateOf<String?>(null) } // null, "WIN" or "LOSE"
    var showRewardsPopup by remember { mutableStateOf(false) }

    fun performFlip(guess: String) {
        if (playsRemaining <= 0) return
        selectedSide = guess
        isFlipping = true
        flipResultState = null
        viewModel.incrementCoinFlipPlaysUsedToday()
        
        coroutineScope.launch {
            for (i in 1..10) {
                coinFace = if (i % 2 == 0) "BTC" else "TAILS"
                delay(120)
            }
            val finalSideValue = if (Math.random() > 0.5) "HEADS" else "TAILS"
            coinFace = if (finalSideValue == "HEADS") "BTC" else "TAILS"
            isFlipping = false
            
            if (guess == finalSideValue) {
                flipResultState = "WIN"
            } else {
                flipResultState = "LOSE"
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Text(
                "LUCKY SATS COIN FLIP",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = AmberYellow,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                "Choose HEADS (indicated by the golden Bitcoin symbol) or TAILS. If the flipped physical coin matches your prediction, win 50 base points + multipliers!",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        // Remaining plays widget card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "DAILY COIN FLIP LIMIT CAP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$playsRemaining ",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = if (playsRemaining > 0) AmberYellow else Color.Red
                            )
                            Text(
                                text = "/ 20 FLIPS LEFT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (playsRemaining > 0) AccentGreen.copy(alpha = 0.12f) else Color.Red.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (playsRemaining > 0) "FLIPS ACTIVE" else "LIMIT EXCEEDED",
                            color = if (playsRemaining > 0) AccentGreen else Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .background(
                                color = if (isFlipping) BitcoinGold.copy(alpha = 0.8f) else BitcoinGold,
                                shape = CircleShape
                            )
                            .border(6.dp, AmberYellow, CircleShape)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (coinFace == "BTC") "₿" else "TAILS",
                            fontSize = if (coinFace == "BTC") 64.sp else 24.sp,
                            fontWeight = FontWeight.Black,
                            color = DarkBg,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isFlipping) {
                        CircularProgressIndicator(color = AmberYellow, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("FLIPPING THE SATS COIN...", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else if (flipResultState != null) {
                        val isWin = flipResultState == "WIN"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isWin) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = "Coin Toss Result Badge",
                                tint = if (isWin) AccentGreen else Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isWin) "RESULT: MATCHED!" else "RESULT: OPPOSITE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isWin) AccentGreen else Color.Red
                            )
                        }
                        
                        Text(
                            text = if (isWin) {
                                "Congratulations! The coin landed on ${if (coinFace == "BTC") "HEADS (₿)" else "TAILS"}."
                            } else {
                                "The coin landed on ${if (coinFace == "BTC") "HEADS (₿)" else "TAILS"} but you guessed $selectedSide."
                            },
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    flipResultState = null
                                    selectedSide = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                                modifier = Modifier.testTag("coin_reset_btn")
                            ) {
                                Text("Flip Again", color = TextPrimary)
                            }

                            if (isWin) {
                                Button(
                                    onClick = { showRewardsPopup = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange),
                                    modifier = Modifier.testTag("coin_claim_btn")
                                ) {
                                    Text("CLAIM REWARDS", color = DarkBg, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = if (playsRemaining > 0) "GUESS THE TOSS OUTCOME TO BEGIN" else "🔒 DAILY COIN TOSS CAP REACHED (20/20)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (playsRemaining > 0) TextSecondary else Color.Red
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { performFlip("HEADS") },
                                enabled = playsRemaining > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGreen,
                                    disabledContainerColor = AccentGreen.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("coin_heads_btn")
                            ) {
                                Text("HEADS (₿)", color = if (playsRemaining > 0) DarkBg else TextSecondary.copy(alpha = 0.5f), fontWeight = FontWeight.Black)
                            }

                            Button(
                                onClick = { performFlip("TAILS") },
                                enabled = playsRemaining > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BorderColor,
                                    disabledContainerColor = BorderColor.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("coin_tails_btn")
                            ) {
                                Text("TAILS", color = if (playsRemaining > 0) TextPrimary else TextSecondary.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRewardsPopup) {
        Dialog(
            onDismissRequest = { showRewardsPopup = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Confirm Icon",
                        tint = AccentGreen,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Claim Victory Points!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose your reward allocation option for this win. Playing video ads from AppLovin/Unity grants a 5x multiplier bonus!",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showRewardsPopup = false
                            flipResultState = null
                            selectedSide = null
                            viewModel.reportGameWinReward(watchAd = false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("Get 50 Base Points", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            showRewardsPopup = false
                            flipResultState = null
                            selectedSide = null
                            viewModel.reportGameWinReward(watchAd = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("Watch AD to get 5x (250 PTS!)", color = DarkBg, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 4. WALLET TAB VIEW
// -----------------------------------------------------------------------------
@Composable
private fun WalletTab(
    userProgress: UserProgressEntity?,
    withdrawals: List<WithdrawalEntity>,
    pointTransactions: List<PointsTransactionEntity>,
    btcPriceUsd: Double,
    btcPriceCad: Double,
    viewModel: SatsWalkViewModel,
    onCashoutRequest: (String, Int) -> Unit
) {
    val totalPoints = userProgress?.currentPoints ?: 0
    val userLevel = userProgress?.level ?: 1
    val limit = userProgress?.withdrawalLimitSats ?: 100

    // Conversions
    val currentRate = userLevel.toDouble() * 0.5
    val currentMaxSatsValue = (totalPoints.toDouble() * currentRate) / 1000.0

    var invoiceInput by remember { mutableStateOf("") }
    var satInputString by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Text(
                "LIGHTNING PAYOUT NODE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = BitcoinOrange,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                "Cash out your accrued walking and task points directly to Bitcoin Satoshis.",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Live Price Index Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("LIVE BITCOIN INDEX", fontSize = 9.sp, color = BitcoinOrange, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(AccentGreen, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Realtime ticker feed connected", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${"%,.2f".format(btcPriceCad)} CAD",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = BitcoinGold
                        )
                        Text(
                            text = "$${"%,.2f".format(btcPriceUsd)} USD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Converted Satoshi Balance Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text("CONVERTED LIGHTNING VALUE", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "%,.2f SATS".format(currentMaxSatsValue),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = BitcoinOrange
                            )
                            Text(
                                "Rate: $currentRate Sats per 1,000 Points (Lvl $userLevel)",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(54.dp)
                                .background(GlowOrange, CircleShape)
                                .border(1.dp, BitcoinOrange, CircleShape)
                        ) {
                            Text("₿", color = BitcoinOrange, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Account points balance", fontSize = 11.sp, color = TextSecondary)
                        Text("$totalPoints PT", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daily Payout Capability Remaining", fontSize = 11.sp, color = TextSecondary)
                        Text("$limit Satoshis", fontSize = 11.sp, color = BitcoinGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // CASH OUT CHEKOUT CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text("EXECUTE WITHDRAWAL (LIGHTNING NETWORK)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Lightning Address
                    OutlinedTextField(
                        value = invoiceInput,
                        onValueChange = { invoiceInput = it },
                        label = { Text("LN Invoice (lnbc...) or LN Address (user@node.com)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BitcoinOrange,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = BitcoinOrange,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_bolt_invoice")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Satoshis to withdraw
                    OutlinedTextField(
                        value = satInputString,
                        onValueChange = { satInputString = it },
                        label = { Text("Satoshis to Withdraw") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BitcoinOrange,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = BitcoinOrange,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_satoshi_withdraw")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val satsToWithdraw = satInputString.toIntOrNull() ?: 0
                            onCashoutRequest(invoiceInput, satsToWithdraw)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cashout_action_button")
                    ) {
                        Text("SIGN & DISPATCH LIGHTNING PAYMENT", fontWeight = FontWeight.Black, color = DarkBg, fontSize = 11.sp)
                    }
                }
            }
        }

        // Transactions lists sections
        item {
            Text(
                "RECENT PAYOUT TRANSACTIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (withdrawals.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text("No cashout records verified on device ledger yet.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(withdrawals) { tx ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Lightning Cashout Completed",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Invoice: ${tx.invoice.take(20)}...",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Value: $${"%.5f".format(tx.cadValue)} CAD / $${"%.5f".format(tx.usdValue)} USD",
                                fontSize = 10.sp,
                                color = BitcoinGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(tx.timestamp)),
                                fontSize = 9.sp,
                                color = DisabledText
                            )
                        }

                        Text(
                            text = "+${tx.satoshis} Sats",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = AccentGreen
                        )
                    }
                }
            }
        }

        // Point Ledger
         item {
            Text(
                "POINT BALANCE AUDIT TRAILS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (pointTransactions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text("No point updates recorded.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(pointTransactions) { pt ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pt.detail,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Type: ${pt.type} | Device Verifier Signed",
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = if (pt.pointsChange > 0) "+${pt.pointsChange} PT" else "${pt.pointsChange} PT",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = if (pt.pointsChange > 0) BitcoinOrange else Color.Red,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            LegalDisclaimerCard(
                viewModel = viewModel,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// AI-STUDIO INTEGRATED FULL-SCREEN VIDEO AD SIMULATOR OVERLAY
// -----------------------------------------------------------------------------
@Composable
private fun FullAdSimulatorOverlay(
    placement: String,
    onAdCompleted: () -> Unit,
    onAdSkipped: () -> Unit
) {
    var countdownSeconds by remember { mutableStateOf(5) }
    var hasRewardConfirmed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (countdownSeconds > 0) {
            delay(1000)
            countdownSeconds--
        }
        hasRewardConfirmed = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) {} // block baseline taps
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Network Brand Name
            Box(
                modifier = Modifier
                    .background(SurfaceDarkVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Google AdMob & Unity Live Stream",
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simulated Video Block Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(260.dp)
                    .background(SurfaceDark, RoundedCornerShape(16.dp))
                    .border(2.dp, BitcoinOrange, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👣", fontSize = 54.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "SatsWalk Ad Partner Stream",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Earn x5 multiplier instantly when checking claims!",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Playback progress indicator
            val progressPercent = (5f - countdownSeconds.toFloat()) / 5f
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(0.81f)
            ) {
                LinearProgressIndicator(
                    progress = { progressPercent },
                    color = BitcoinOrange,
                    trackColor = BorderColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (countdownSeconds > 0) "Ad Reward verifying in $countdownSeconds seconds..." else "AD WATCH SECURELY VERIFIED BY NODE!",
                    color = if (countdownSeconds > 0) TextSecondary else AccentGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer control buttons
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAdSkipped,
                    colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("Skip Video", color = TextPrimary)
                }

                Button(
                    onClick = onAdCompleted,
                    enabled = hasRewardConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        disabledContainerColor = DisabledBg
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("ad_claim_complete_btn")
                ) {
                    Text(
                        text = if (hasRewardConfirmed) "CONFIRM" else "CLAIM (Locked)",
                        fontWeight = FontWeight.Bold,
                        color = if (hasRewardConfirmed) DarkBg else DisabledText
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// STEP CLAIM SELECTOR POPUP MODAL DIALOG
// -----------------------------------------------------------------------------
@Composable
private fun MilestoneClaimSelectionDialog(
    milestone: Int,
    onClaimBase: () -> Unit,
    onClaimMultiplier: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("👣", fontSize = 48.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Claim Proof of Walk!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Verify your milestones at $milestone steps of everyday walking.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onClaimBase,
                    colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("claim_base_reward")
                ) {
                    Text("Get 50 Base Points", color = TextPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onClaimMultiplier,
                    colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("claim_ad_reward")
                ) {
                    Text("Watch AD to get 5x (250 PTS!)", color = DarkBg, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// DEVELOPMENT DEV TOOLS / SIMULATOR CENTER PANEL
// -----------------------------------------------------------------------------
@Composable
private fun DevToolsSimulationCenter(
    currentSteps: Int,
    onAddSteps: (Int) -> Unit,
    onSetSteps: (Int) -> Unit,
    onResetAll: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .background(BorderColor)
                .clip(RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WALK SIMULATOR CENTER",
                fontWeight = FontWeight.Black,
                color = BitcoinOrange,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )

            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Dismiss dev panel", tint = TextSecondary)
            }
        }

        Text(
            text = "Inject walking steps into the database node to verify claiming lock states. Emulator current counts: $currentSteps steps.",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Trigger buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onAddSteps(500) },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkVariant),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("sim_add_500")
            ) {
                Text("+500 Steps", fontSize = 11.sp, color = TextPrimary)
            }

            Button(
                onClick = { onAddSteps(1500) },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkVariant),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("sim_add_1500")
            ) {
                Text("+1,500 Steps", fontSize = 11.sp, color = TextPrimary)
            }

            Button(
                onClick = { onSetSteps(10200) },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkVariant),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("sim_set_10k")
            ) {
                Text("Set 10,200 steps", fontSize = 10.sp, color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onSetSteps(20050) },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkVariant),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("sim_set_20k")
            ) {
                Text("Set 20,050 (Unlock 40)", fontSize = 10.sp, color = TextPrimary)
            }

            Button(
                onClick = onResetAll,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("sim_reset_ledger")
            ) {
                Text("Reset Ecosystem", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SATS SPINNING WHEEL GAME IMPLEMENTATION
// -----------------------------------------------------------------------------
data class WheelSlice(val textLabel: String, val pointValue: Int, val sliceColor: Color)

@Composable
private fun SpinningWheelGame(
    viewModel: SatsWalkViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    
    // State collection
    val spinsRemaining by viewModel.wheelSpinsRemainingToday.collectAsState()
    val maxSpins by viewModel.wheelMaxDailySpins.collectAsState()
    val wheelReadyToSpin by viewModel.wheelReadyToSpin.collectAsState()

    // Internal game visual state
    val rotation = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var targetSlotSelected by remember { mutableStateOf<Int?>(null) }
    var showPrizePopup by remember { mutableStateOf(false) }
    var prizeAwardedAmt by remember { mutableStateOf(0) }

    // Wheel slices: 10 slots
    val slices = remember {
        listOf(
            WheelSlice("50 PTS", 50, Color(0xFFF7931A)),    // Slot 0 - Orange
            WheelSlice("50 PTS", 50, Color(0xFFE28413)),    // Slot 1 - Warm Orange
            WheelSlice("50 PTS", 50, Color(0xFFF7931A)),    // Slot 2 - Orange
            WheelSlice("50 PTS", 50, Color(0xFFE28413)),    // Slot 3 - Warm Orange
            WheelSlice("50 PTS", 50, Color(0xFFF7931A)),    // Slot 4 - Orange
            WheelSlice("250 PTS", 250, Color(0xFFFFD700)),  // Slot 5 - Golden Gem (0.1% chance)
            WheelSlice("100 PTS", 100, Color(0xFF1E88E5)),  // Slot 6 - Sapphire Blue
            WheelSlice("100 PTS", 100, Color(0xFF1976D2)),  // Slot 7 - Dark Blue
            WheelSlice("150 PTS", 150, Color(0xFF388E3C)),  // Slot 8 - Emerald Green
            WheelSlice("TRY AGAIN", 0, Color(0xFF3E2723))   // Slot 9 - Walnut Charcoal
        )
    }

    // Dynamic rotation indicators while spinning
    val liveApproximatedSliceIndex = remember(rotation.value) {
        val normalizedRotation = ((270f - rotation.value) % 360f + 360f) % 360f
        val sliceSize = 36f
        val index = (normalizedRotation / sliceSize).toInt()
        index.coerceIn(0, 9)
    }

    // React to ad completion ("wheelReadyToSpin" transitions from false to true)
    LaunchedEffect(wheelReadyToSpin) {
        if (wheelReadyToSpin) {
            viewModel.clearWheelReadyToSpin()
            
            // Choose target slot based on exact probabilities requested:
            // 5 slots for 50 PTS (90% cumulative chance)
            // 1 slot for 250 PTS (0.1% chance)
            // 2 slots for 100 PTS (5% cumulative chance)
            // 1 slot for 150 PTS (3% chance)
            // 1 slot for TRY AGAIN (remaining 1.9% chance)
            val rand = Math.random()
            val targetSlot = when {
                rand < 0.90 -> (0..4).random()
                rand < 0.901 -> 5
                rand < 0.951 -> listOf(6, 7).random()
                rand < 0.981 -> 8
                else -> 9
            }
            
            targetSlotSelected = targetSlot
            isSpinning = true
            
            // Perform smooth physical rotation animation
            // targetAngle is (8 full spins = 2880 degrees) + offset to top pointer
            val targetAngle = (360f * 8) + 270f - (targetSlot * 36f + 18f)
            
            coroutineScope.launch {
                rotation.snapTo(rotation.value % 360f)
                rotation.animateTo(
                    targetValue = targetAngle,
                    animationSpec = tween(
                        durationMillis = 4200,
                        easing = CubicBezierEasing(0.12f, 0.8f, 0.15f, 1.0f)
                    )
                )
                
                // Animation completed! Award points on backend and show Dialog
                val landedSlice = slices[targetSlot]
                prizeAwardedAmt = landedSlice.pointValue
                viewModel.awardWheelPoints(landedSlice.pointValue)
                showPrizePopup = true
                isSpinning = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text(
                text = "SATS SPINNING WHEEL",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = AmberYellow,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "Launch high volatility point multipliers! There are 10 slots on the wheel. Watch an ad before your spin. Max 10 spins per 24 hours (upgrades raise capacity to 15).",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        // Remaining spins widget card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "DAILY SPIN LIMIT CAP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$spinsRemaining ",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = if (spinsRemaining > 0) AmberYellow else Color.Red
                            )
                            Text(
                                text = "/ $maxSpins SPINS LEFT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (spinsRemaining > 0) AccentGreen.copy(alpha = 0.12f) else Color.Red.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (spinsRemaining > 0) "SPINS ACTIVE" else "LIMIT EXCEEDED",
                            color = if (spinsRemaining > 0) AccentGreen else Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Visual Wheel Interactive Arena
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    
                    // Wheel Box
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .drawBehind {
                                // Draw a soft radial glow background
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(AmberYellow.copy(alpha = 0.12f), Color.Transparent),
                                        center = center,
                                        radius = size.minDimension * 0.7f
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing the wheel slices on the custom Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("spinning_wheel_canvas")
                        ) {
                            val canvasRadius = size.minDimension / 2f
                            val centerOffset = Offset(size.width / 2f, size.height / 2f)
                            
                            // 1. Draw actual slices with their calculated colors
                            val rotAngle = rotation.value
                            slices.forEachIndexed { idx, slice ->
                                val startAng = idx * 36f + rotAngle
                                drawArc(
                                    color = slice.sliceColor,
                                    startAngle = startAng,
                                    sweepAngle = 36f,
                                    useCenter = true
                                )
                            }
                            
                            // 2. Draw separation lines
                            slices.forEachIndexed { idx, _ ->
                                val separatorAng = idx * 36f + rotAngle
                                val rad = Math.toRadians(separatorAng.toDouble())
                                val endX = centerOffset.x + (canvasRadius * Math.cos(rad)).toFloat()
                                val endY = centerOffset.y + (canvasRadius * Math.sin(rad)).toFloat()
                                drawLine(
                                    color = DarkBg,
                                    start = centerOffset,
                                    end = Offset(endX, endY),
                                    strokeWidth = 3.5f
                                )
                            }

                            // 3. Draw Outer Gold Ring Frame
                            drawCircle(
                                color = BorderColor,
                                radius = canvasRadius,
                                style = Stroke(width = 6f)
                            )
                            drawCircle(
                                color = AmberYellow,
                                radius = canvasRadius - 3f,
                                style = Stroke(width = 2f)
                            )

                            // 4. Draw Slice Labels with correct dynamic projection
                            // We use drawIntoCanvas to draw labels angled cleanly from the center of each wedge
                            drawIntoCanvas { canvas ->
                                val nativeCanvas = canvas.nativeCanvas
                                slices.forEachIndexed { idx, slice ->
                                    val midAngle = idx * 36f + 18f + rotAngle
                                    val angleRad = Math.toRadians(midAngle.toDouble())
                                    
                                    // Project label coordinates
                                    val labelRadius = canvasRadius * 0.65f
                                    val textX = centerOffset.x + (labelRadius * Math.cos(angleRad)).toFloat()
                                    val textY = centerOffset.y + (labelRadius * Math.sin(angleRad)).toFloat()

                                    nativeCanvas.save()
                                    // Rotate text inside slice to align with the radius direction
                                    nativeCanvas.rotate(midAngle + 180f, textX, textY)
                                    
                                    val isBigWin = slice.pointValue >= 150
                                    val textPaint = android.graphics.Paint().apply {
                                        color = if (isBigWin) android.graphics.Color.YELLOW else android.graphics.Color.WHITE
                                        textSize = density * 12.5f
                                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }
                                    
                                    nativeCanvas.drawText(slice.textLabel, textX, textY + 4f * density, textPaint)
                                    nativeCanvas.restore()
                                }
                            }
                        }

                        // 5. Draw the Center Pin overlay
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(DarkBg, CircleShape)
                                .border(2.dp, AmberYellow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚡",
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        // 6. Draw Top Pointer Arrow pointing straight down (at 270 degrees)
                        Canvas(
                            modifier = Modifier
                                .size(280.dp)
                        ) {
                            val pointerPath = Path().apply {
                                moveTo(size.width / 2f - 16f, 0f)
                                lineTo(size.width / 2f + 16f, 0f)
                                lineTo(size.width / 2f, 22f)
                                close()
                            }
                            drawPath(
                                path = pointerPath,
                                color = Color.Red
                            )
                            drawPath(
                                path = pointerPath,
                                color = TextPrimary,
                                style = Stroke(width = 2f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live tracker label under the wheel
                    Surface(
                        color = SurfaceDarkVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = if (isSpinning) {
                                "PASSING: ${slices[liveApproximatedSliceIndex].textLabel}"
                            } else {
                                "READY TO SPIN"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isSpinning) AmberYellow else TextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Action Trigger Button
        item {
            Button(
                onClick = { viewModel.playWheelAd() },
                enabled = !isSpinning && spinsRemaining > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberYellow,
                    disabledContainerColor = DisabledBg
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(49.dp)
                    .testTag("request_wheel_spin_button")
            ) {
                if (isSpinning) {
                    Text("SPINNING VOLATILITY WHEEL...", fontWeight = FontWeight.Bold, color = DisabledText)
                } else if (spinsRemaining == 0) {
                    Text("DAILY LIMIT EXCEEDED (RESETS IN 24H)", fontWeight = FontWeight.Bold, color = DisabledText)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SPIN WHEEL ", fontWeight = FontWeight.Black, color = DarkBg)
                        Text("(WATCH AD)", fontWeight = FontWeight.Bold, color = DarkBg.copy(alpha = 0.75f), fontSize = 11.sp)
                    }
                }
            }
        }

        // Probabilities disclosure table
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(8.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "PROBABILITY FAIR DISTRIBUTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProbabilityRow("5 Slots: 50 PTS", "90% Total Chance")
                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 6.dp))
                ProbabilityRow("2 Slots: 100 PTS", "5% Total Chance")
                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 6.dp))
                ProbabilityRow("1 Slot: 150 PTS", "3% Total Chance")
                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 6.dp))
                ProbabilityRow("1 Slot: 250 PTS", "0.1% Rare Chance")
                Divider(color = BorderColor, modifier = Modifier.padding(vertical = 6.dp))
                ProbabilityRow("1 Slot: TRY AGAIN (0 PTS)", "1.9% Chance")
            }
        }
    }

    // Celebration Alert popup Dialog
    if (showPrizePopup) {
        Dialog(
            onDismissRequest = { showPrizePopup = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .border(2.dp, if (prizeAwardedAmt > 0) AmberYellow else BorderColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (prizeAwardedAmt > 100) "🎉 JACKPOT LANDED! 🎉" else if (prizeAwardedAmt > 0) "✨ REWARD WINNER! ✨" else "TRY AGAIN NEXT TIME",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (prizeAwardedAmt > 0) AmberYellow else TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = if (prizeAwardedAmt > 0) "$prizeAwardedAmt PTS" else "0 PTS",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = if (prizeAwardedAmt > 0) AccentGreen else TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (prizeAwardedAmt > 0) {
                                "Congratulations! This reward has been added to your credit points balance instantly."
                            } else {
                                "Oops! You did not hit a rewards wedge on this slide. No worries! You can spin again up to your daily capacity limits!"
                            },
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = { showPrizePopup = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (prizeAwardedAmt > 0) AmberYellow else SurfaceDarkVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("dismiss_prize_popup_btn")
                        ) {
                            Text(
                                "AWESOME",
                                fontWeight = FontWeight.Bold,
                                color = if (prizeAwardedAmt > 0) DarkBg else TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProbabilityRow(label: String, chance: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(chance, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DiceFaceWidget(number: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(96.dp)
            .background(AmberYellow, RoundedCornerShape(12.dp))
            .border(3.dp, DarkBg, RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = 8.dp.toPx()
            val spacing = size.width / 4f
            
            val dots = when (number) {
                1 -> listOf(Offset(size.width / 2, size.height / 2))
                2 -> listOf(
                    Offset(spacing, spacing),
                    Offset(size.width - spacing, size.height - spacing)
                )
                3 -> listOf(
                    Offset(spacing, spacing),
                    Offset(size.width / 2, size.height / 2),
                    Offset(size.width - spacing, size.height - spacing)
                )
                4 -> listOf(
                    Offset(spacing, spacing),
                    Offset(size.width - spacing, spacing),
                    Offset(spacing, size.height - spacing),
                    Offset(size.width - spacing, size.height - spacing)
                )
                5 -> listOf(
                    Offset(spacing, spacing),
                    Offset(size.width - spacing, spacing),
                    Offset(size.width / 2, size.height / 2),
                    Offset(spacing, size.height - spacing),
                    Offset(size.width - spacing, size.height - spacing)
                )
                6 -> listOf(
                    Offset(spacing, spacing),
                    Offset(size.width - spacing, spacing),
                    Offset(spacing, size.height / 2),
                    Offset(size.width - spacing, size.height / 2),
                    Offset(spacing, size.height - spacing),
                    Offset(size.width - spacing, size.height - spacing)
                )
                else -> emptyList()
            }
            
            dots.forEach { offset ->
                drawCircle(
                    color = DarkBg,
                    radius = radius,
                    center = offset
                )
            }
        }
    }
}

@Composable
private fun DiceGame(
    viewModel: SatsWalkViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val dicePlaysRemaining by viewModel.dicePlaysRemainingToday.collectAsState()
    val userProgress by viewModel.userProgress.collectAsState()
    val stepsToday = userProgress?.currentSteps ?: 0
    val isLocked = stepsToday < 3000

    var isRolling by remember { mutableStateOf(false) }
    var currentRollNumber by remember { mutableStateOf(1) }
    var rolledValue by remember { mutableStateOf<Int?>(null) }
    
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRolling) 1440f else 0f,
        animationSpec = if (isRolling) {
            infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        },
        label = "rotationAngle"
    )
    
    val scaleVal by animateFloatAsState(
        targetValue = if (isRolling) 1.25f else 1.0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "scaleVal"
    )

    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    fun rollDice() {
        if (dicePlaysRemaining <= 0) return
        isRolling = true
        rolledValue = null
        viewModel.clearMessages()

        coroutineScope.launch {
            var t = 50L
            repeat(15) {
                currentRollNumber = (1..6).random()
                delay(t)
                t += 20L
            }
            
            val finalRoll = (1..6).random()
            currentRollNumber = finalRoll
            rolledValue = finalRoll
            isRolling = false
            
            viewModel.playDiceGame(finalRoll)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Text(
                text = "LUCKY STEPS DICE ROLL",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = AmberYellow,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "Reward your physical activity! Once you cross the 3,000 steps today milestone, you earn 3 complimentary ad-free rolls on the golden die.",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DIFFICULTY: 3,000 STEPS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLocked) Color.Red else AccentGreen,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isLocked) Color.Red.copy(alpha = 0.15f) else AccentGreen.copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isLocked) "LOCKED" else "ACTIVE",
                                color = if (isLocked) Color.Red else AccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .graphicsLayer(
                                rotationZ = rotationAngle,
                                scaleX = scaleVal,
                                scaleY = scaleVal
                            )
                            .testTag("interactive_dice_cube"),
                        contentAlignment = Alignment.Center
                    ) {
                        DiceFaceWidget(number = currentRollNumber)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Current Value: $currentRollNumber",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Earns: ${currentRollNumber * 50} pts on landing!",
                        fontSize = 11.sp,
                        color = AmberYellow,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!isLocked && dicePlaysRemaining > 0) {
                        Text(
                            text = "You have $dicePlaysRemaining / 3 plays left today!",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (!isLocked) {
                        Text(
                            text = "All daily plays used! Come back tomorrow.",
                            color = Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LOCKED: You walked $stepsToday / 3,000 steps today.",
                                color = Color.Red,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Walk ${3000 - stepsToday} more steps to unlock 3 plays!",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { rollDice() },
                        enabled = !isRolling && !isLocked && dicePlaysRemaining > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberYellow,
                            disabledContainerColor = DisabledBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("roll_dice_action_btn")
                    ) {
                        Text(
                            text = if (isRolling) "ROLLING GOLDEN DIE..." else "ROLL DIE",
                            fontWeight = FontWeight.Black,
                            color = if (!isRolling && !isLocked && dicePlaysRemaining > 0) DarkBg else DisabledText
                        )
                    }
                }
            }
        }

        if (successMessage != null || errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (successMessage != null) AccentGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (successMessage != null) AccentGreen else Color.Red
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (successMessage != null) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = "Status Response",
                            tint = if (successMessage != null) AccentGreen else Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = successMessage ?: errorMessage ?: "",
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "DICE ROLL Payout Ledger:",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "🎲 Face 1" to "50 Points",
                        "🎲 Face 2" to "100 Points",
                        "🎲 Face 3" to "150 Points",
                        "🎲 Face 4" to "200 Points",
                        "🎲 Face 5" to "250 Points",
                        "🎲 Face 6" to "300 Points"
                    ).forEach { (face, reward) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(face, color = TextSecondary, fontSize = 12.sp)
                            Text(reward, color = AmberYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinkedDevicesDialog(
    viewModel: SatsWalkViewModel,
    onDismissRequest: () -> Unit
) {
    val activeDeviceId = viewModel.activeDeviceId
    val simulatedDeviceId by viewModel.simulatedDeviceId.collectAsState()
    val tempCode by viewModel.tempCode.collectAsState()
    val isGeneratingTempCode by viewModel.isGeneratingTempCode.collectAsState()
    val linkedDevicesList by viewModel.linkedDevicesList.collectAsState()
    val isLoadingDevices by viewModel.isLoadingDevices.collectAsState()
    val userProgress by viewModel.userProgress.collectAsState()

    var inputCode by remember { mutableStateOf("") }
    var inputAlias by remember { mutableStateOf("") }

    // Fetch device list on open
    LaunchedEffect(activeDeviceId) {
        viewModel.fetchLinkedDevices()
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .testTag("linked_devices_dialog_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "LINKED DEVICES",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = BitcoinOrange,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = onDismissRequest) {
                        Text("❌", fontSize = 16.sp)
                    }
                }

                // SIMULATOR DEVICE SWITCHER (High Fidelity Testing Tool!)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "📲 CURRENT SIMULATED DEVICE",
                            fontSize = 11.sp,
                            color = BitcoinGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "ID: $activeDeviceId ${if (simulatedDeviceId == null) "(Primary)" else "(Simulated)"}",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Sync Balance: ${userProgress?.currentPoints ?: 0} PTS (Lv ${userProgress?.level ?: 1})",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.setSimulatedDeviceId(null) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (simulatedDeviceId == null) BitcoinOrange else SurfaceDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(36.dp).testTag("select_device_primary"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Phone A", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Button(
                                onClick = { viewModel.setSimulatedDeviceId("simulated_handset_b") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (simulatedDeviceId == "simulated_handset_b") BitcoinOrange else SurfaceDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(36.dp).testTag("select_device_b"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Phone B", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Button(
                                onClick = { viewModel.setSimulatedDeviceId("simulated_handset_c") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (simulatedDeviceId == "simulated_handset_c") BitcoinOrange else SurfaceDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(36.dp).testTag("select_device_c"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Phone C", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // QR CODE & CODE GENERATION SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "📲 GENERATE QR CODES FOR TEMPORARY LOGIN",
                            fontSize = 11.sp,
                            color = BitcoinGold,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Valid for 10 minutes. Scan/Enter this temporary link on any other handset to share progress levels, points ledger & transaction sync.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        if (tempCode == null) {
                            Button(
                                onClick = { viewModel.createTempCode() },
                                colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("generate_temp_code_button"),
                                enabled = !isGeneratingTempCode
                            ) {
                                if (isGeneratingTempCode) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("🔑 Generate Linking Code", fontSize = 14.sp)
                                }
                            }
                        } else {
                            // Render fully stylized simulated QR code via canvas!
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val sizeX = size.width
                                    val sizeY = size.height
                                    val sqSize = sizeX / 7f

                                    // Top-Left Finder
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(sqSize*2, sqSize*2))
                                    drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(sqSize*0.4f, sqSize*0.4f), size = androidx.compose.ui.geometry.Size(sqSize*1.2f, sqSize*1.2f))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(sqSize*0.7f, sqSize*0.7f), size = androidx.compose.ui.geometry.Size(sqSize*0.6f, sqSize*0.6f))

                                    // Top-Right Finder
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(sizeX - sqSize*2, 0f), size = androidx.compose.ui.geometry.Size(sqSize*2, sqSize*2))
                                    drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(sizeX - sqSize*1.6f, sqSize*0.4f), size = androidx.compose.ui.geometry.Size(sqSize*1.2f, sqSize*1.2f))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(sizeX - sqSize*1.3f, sqSize*0.7f), size = androidx.compose.ui.geometry.Size(sqSize*0.6f, sqSize*0.6f))

                                    // Bottom-Left Finder
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, sizeY - sqSize*2), size = androidx.compose.ui.geometry.Size(sqSize*2, sqSize*2))
                                    drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(sqSize*0.4f, sizeY - sqSize*1.6f), size = androidx.compose.ui.geometry.Size(sqSize*1.2f, sqSize*1.2f))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(sqSize*0.7f, sizeY - sqSize*1.3f), size = androidx.compose.ui.geometry.Size(sqSize*0.6f, sqSize*0.6f))

                                    // Draw randomized mockup points in QR code grid
                                    val rand = java.util.Random(tempCode?.code.hashCode().toLong())
                                    for (x in 2..8) {
                                        for (y in 2..8) {
                                            if (rand.nextBoolean()) {
                                                drawRect(
                                                    Color.Black,
                                                    topLeft = androidx.compose.ui.geometry.Offset(x * sqSize, y * sqSize),
                                                    size = androidx.compose.ui.geometry.Size(sqSize * 0.9f, sqSize * 0.9f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            SelectionContainer {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        tempCode?.code ?: "",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BitcoinOrange,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.testTag("temp_linking_code_text")
                                    )
                                    val minsLeft = ((tempCode?.expiresAt ?: 0L) - System.currentTimeMillis()) / 60000L
                                    Text(
                                        "Code Expires in: ${minsLeft.coerceAtLeast(0)} Mins",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // ENTER LINKING CODE SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "🔌 LINK TO ANOTHER PHONE",
                            fontSize = 11.sp,
                            color = BitcoinGold,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = inputCode,
                            onValueChange = { inputCode = it.uppercase() },
                            placeholder = { Text("Enter 4-Digit Linking Code", color = TextSecondary) },
                            singleLine = true,
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BitcoinOrange,
                                unfocusedBorderColor = BorderColor,
                                cursorColor = BitcoinOrange
                            ),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("link_code_input")
                        )

                        OutlinedTextField(
                            value = inputAlias,
                            onValueChange = { inputAlias = it },
                            placeholder = { Text("Device Name (e.g. Pixel 8 Pro)", color = TextSecondary) },
                            singleLine = true,
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BitcoinOrange,
                                unfocusedBorderColor = BorderColor,
                                cursorColor = BitcoinOrange
                            ),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("device_alias_input")
                        )

                        Button(
                            onClick = {
                                if (inputCode.trim().isNotEmpty()) {
                                    viewModel.linkDevice(
                                        inputCode.trim(),
                                        inputAlias.trim().ifEmpty { "Walk Terminal Phone" }
                                    )
                                    inputCode = ""
                                    inputAlias = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BitcoinOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("confirm_device_link_button"),
                            enabled = inputCode.trim().isNotEmpty()
                        ) {
                            Text("🔗 Link Devices", fontSize = 14.sp)
                        }
                    }
                }

                // CURRENTLY LINKED DEVICES (Up to 3)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "📑 LINKED NETWORKS (${linkedDevicesList.size} / 3)",
                            fontSize = 11.sp,
                            color = BitcoinGold,
                            fontWeight = FontWeight.Bold
                        )

                        if (isLoadingDevices) {
                            Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = BitcoinOrange, modifier = Modifier.size(24.dp))
                            }
                        } else if (linkedDevicesList.isEmpty()) {
                            Text(
                                "No other devices linked to your account yet.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        } else {
                            linkedDevicesList.forEach { device ->
                                val isCurrent = device.deviceId == activeDeviceId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isCurrent) GlowOrange.copy(alpha = 0.05f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .border(0.5.dp, if (isCurrent) BitcoinOrange.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.alias,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) BitcoinOrange else TextPrimary
                                        )
                                        Text(
                                            text = "ID: ${device.deviceId.take(12)}...",
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    if (isCurrent) {
                                        Text(
                                            "ACTIVE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BitcoinOrange,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    } else {
                                        IconButton(
                                            onClick = { viewModel.unlinkDevice(device.deviceId) },
                                            modifier = Modifier.size(32.dp).testTag("unlink_device_btn_${device.deviceId}")
                                        ) {
                                            Text("🚪", fontSize = 14.sp)
                                        }
                                    }
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
fun LegalDisclaimerCard(
    viewModel: SatsWalkViewModel,
    modifier: Modifier = Modifier
) {
    val isAccepted by viewModel.isDisclaimerAccepted.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("legal_disclaimer_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(
            1.dp,
            if (isAccepted) AccentGreen.copy(alpha = 0.5f) else AmberYellow.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚖️", fontSize = 18.sp)
                    Column {
                        Text(
                            "LEGAL LIABILITY DISCLAIMER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = BitcoinOrange,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "US & Canadian Compliance Shield",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = if (isAccepted) AccentGreen.copy(alpha = 0.12f) else AmberYellow.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isAccepted) "COMPLIANT" else "ACTION REQUIRED",
                        color = if (isAccepted) AccentGreen else AmberYellow,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Text(
                text = "This app is provided \"as is\" and utilized entirely at your own risk. Points and satoshis accumulated herein represent a gamified promotional loyalty program, NOT a financial investment or security product.",
                fontSize = 11.sp,
                color = TextSecondary
            )

            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, RoundedCornerShape(8.dp))
                        .border(0.5.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "1. WARRANTY LIMITATION (\"AS IS\")",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = BitcoinGold
                        )
                        Text(
                            "This application and its services are provided on an \"AS IS\" and \"AS AVAILABLE\" basis without representation, warranties, or conditions of any kind, whether express, implied, or statutory. Any participation in walking, games, point redemption, or other features is at the user's sole risk.",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )

                        Text(
                            "2. ABSOLUTE EXCLUSION OF LIABILITY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = BitcoinGold
                        )
                        Text(
                            "Under no circumstances shall the developer, platform operators, or affiliates be liable for any personal injury, medical condition, cardiovascular event, accidents, device failure, or any direct, indirect, incidental, or consequential damages resulting from your use of this app or tracking activities.",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )

                        Text(
                            "3. LOYALTY REWARDS STATUS (NON-INVESTMENT)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = BitcoinGold
                        )
                        Text(
                            "All points, virtual levels, spinning wheel payouts, and distributed satoshis are purely promotional elements of a gamified loyalty reward program. They carry no cash, equity, or monetary deposit rights, do not represent an investment, security, share, or financial instrument under United States, State, Canadian federal, or Provincial securities laws, and contain no guarantees of tradeable value.",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )

                        Text(
                            "4. REGULATORY JURISDICTION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = BitcoinGold
                        )
                        Text(
                            "This agreement is designed to comply with guidelines set by the U.S. Federal Trade Commission (FTC), State laws, the Securities and Exchange Commission (SEC), and the Canadian Competition Bureau, Provincial Consumer Protection acts, and Canadian Securities Administrators (CSA) treating gamified reward programs as marketing incentives rather than financial activities.",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Hide Full Disclosures ▲" else "Read Full Legal Terms ▼",
                        fontSize = 11.sp,
                        color = BitcoinGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isAccepted) {
                    Button(
                        onClick = { viewModel.setDisclaimerAccepted(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberYellow),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("accept_disclaimer_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "Agree & Accept Risk",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("✅", fontSize = 12.sp)
                        Text(
                            "Accepted & Acknowledged",
                            fontSize = 10.sp,
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { viewModel.setDisclaimerAccepted(false) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Reset",
                                fontSize = 9.sp,
                                color = DisabledText,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
