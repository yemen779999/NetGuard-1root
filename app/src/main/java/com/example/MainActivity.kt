package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ConfigChangeLog
import com.example.data.MacHistory
import com.example.data.NetworkProfile
import com.example.data.SavedMac
import com.example.data.ScannedDeviceRecord
import com.example.network.*
import com.example.ui.theme.*
import com.example.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : FragmentActivity() {
    private val viewModel: NetGuardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val useDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val is3dEffects by viewModel.is3dEffectsEnabled.collectAsStateWithLifecycle()
            val animationLevel by viewModel.animationLevel.collectAsStateWithLifecycle()

            MyApplicationTheme(
                darkTheme = useDarkTheme,
                is3dEffectsEnabled = is3dEffects,
                animationLevel = animationLevel
            ) {
                val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
                val isBiometricAuthenticated by viewModel.isBiometricAuthenticated.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (isBiometricEnabled && !isBiometricAuthenticated) {
                            BiometricLockScreen(viewModel)
                        } else {
                            NetGuardApp(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetGuardApp(viewModel: NetGuardViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen || currentScreen != Screen.Main) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.navigateBack()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isTablet,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CyberSurface,
                drawerContentColor = CyberTextPrimary,
                modifier = Modifier.width(310.dp)
            ) {
                AppNavigationDrawerContent(
                    currentScreen = currentScreen,
                    onScreenSelected = { screen ->
                        viewModel.setScreen(screen)
                        scope.launch { drawerState.close() }
                    },
                    viewModel = viewModel
                )
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (isTablet) {
                // Adaptive design: show Navigation Rail on tablet
                AppNavigationRail(
                    currentScreen = currentScreen,
                    onScreenSelected = { viewModel.setScreen(it) }
                )
                VerticalDivider(color = CyberBorder, thickness = 1.dp)
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Global App Header with drawer button
                AppHeader(
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )

                // Main Screen Body with transition
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(CyberDarkBg),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxSize()
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                            },
                            label = "screen_transition"
                        ) { screen ->
                            when (screen) {
                                Screen.Main -> MainIdentityScreen(viewModel)
                                Screen.MacManager -> MacManagerScreen(viewModel)
                                Screen.Logs -> MacManagerScreen(viewModel)
                                Screen.SavedMacs -> SavedMacsScreen(viewModel)
                                Screen.History -> HistoryScreen(viewModel)
                                Screen.NetworkRadar -> NetworkRadarScreen(viewModel)
                                Screen.BlockedDevices -> BlockedDevicesScreen(viewModel)
                                Screen.Settings -> SettingsScreen(viewModel)
                                Screen.RootConsole -> RootConsoleScreen(viewModel)
                                Screen.SignIn -> SignInScreen(viewModel)
                                Screen.NetworkIdentity -> NetworkIdentityScreen(viewModel)
                            }
                        }
                    }
                }

                // Real-time Mini Terminal Drawer (Highly engineer styled)
                CollapsibleConsoleLogs(viewModel)

                if (!isTablet) {
                    // Bottom navigation bar for mobile (4 core pages)
                    AppBottomNavigationBar(
                        currentScreen = currentScreen,
                        onScreenSelected = { viewModel.setScreen(it) }
                    )
                }
            }
        }
    }
}

// Navigation Drawer Content Component
@Composable
fun AppNavigationDrawerContent(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    viewModel: NetGuardViewModel
) {
    val rootStatus by viewModel.rootVerificationStatus.collectAsStateWithLifecycle()
    val blockedDevices by viewModel.blockedDevices.collectAsStateWithLifecycle()
    val localIp by viewModel.localIp.collectAsStateWithLifecycle()
    val currentMac by viewModel.currentMac.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Drawer Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
            border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("drawer_header_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "NetGuard Logo",
                            tint = CyberGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NetGuard Root",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextPrimary
                        )
                        Text(
                            text = "v2.5 Pro Security Utility",
                            fontSize = 11.sp,
                            color = CyberGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = CyberBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "IP: $localIp", fontSize = 10.sp, color = CyberTextSecondary, fontFamily = FontFamily.Monospace)
                        Text(text = "MAC: $currentMac", fontSize = 10.sp, color = CyberTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    Badge(
                        containerColor = if (rootStatus == RootVerificationStatus.VERIFIED_ROOT) CyberGreen else CyberOrange,
                        contentColor = CyberDarkBg
                    ) {
                        Text(
                            text = if (rootStatus == RootVerificationStatus.VERIFIED_ROOT) "ROOT ACTIVE" else "SIMULATION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Section 1: 📱 Bottom Bar Pages (الصفحات الرئيسية الاربع)
        Text(
            text = "الصفحات الرئيسية (الشريط السفلي)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTextSecondary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, end = 4.dp)
        )

        val bottomItems = getBottomNavigationItems()
        bottomItems.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(item.icon, contentDescription = item.label, tint = if (currentScreen == item.screen) CyberDarkBg else CyberGreen) },
                selected = currentScreen == item.screen,
                onClick = { onScreenSelected(item.screen) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = CyberGreen,
                    selectedIconColor = CyberDarkBg,
                    selectedTextColor = CyberDarkBg,
                    unselectedContainerColor = CyberSurface,
                    unselectedIconColor = CyberGreen,
                    unselectedTextColor = CyberTextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("drawer_item_${item.screen.name.lowercase()}")
            )
        }

        HorizontalDivider(color = CyberBorder, modifier = Modifier.padding(vertical = 4.dp))

        // Section 2: ⚡ Sidebar Pages (أدوات الروت والسجلات الثلاث)
        Text(
            text = "أدوات الروت والسجلات (القائمة الجانبية)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CyberGreen,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        val drawerItems = getDrawerNavigationItems()
        drawerItems.forEach { item ->
            NavigationDrawerItem(
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (item.screen == Screen.BlockedDevices && blockedDevices.isNotEmpty()) {
                            Badge(
                                containerColor = CyberRed,
                                contentColor = Color.White
                            ) {
                                Text(text = "${blockedDevices.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (currentScreen == item.screen) CyberDarkBg else if (item.screen == Screen.BlockedDevices) CyberRed else CyberGreen
                    )
                },
                selected = currentScreen == item.screen,
                onClick = { onScreenSelected(item.screen) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = CyberGreen,
                    selectedIconColor = CyberDarkBg,
                    selectedTextColor = CyberDarkBg,
                    unselectedContainerColor = CyberSurface,
                    unselectedIconColor = CyberGreen,
                    unselectedTextColor = CyberTextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("drawer_item_${item.screen.name.lowercase()}")
            )
        }

        HorizontalDivider(color = CyberBorder, modifier = Modifier.padding(vertical = 4.dp))

        // Section 3: Quick Action Enhancements
        Text(
            text = "إجراءات سريعة وتحسينات",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        OutlinedButton(
            onClick = {
                viewModel.startScan()
                onScreenSelected(Screen.NetworkRadar)
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
            border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("drawer_quick_scan_btn")
        ) {
            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("مسح فوري للشبكة الآن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = {
                viewModel.requestRootAccess()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberOrange),
            border = BorderStroke(1.dp, CyberOrange.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("drawer_verify_root_btn")
        ) {
            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("إعادة فحص صلاحيات libsu Root", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Global App Header
@Composable
fun AppHeader(
    viewModel: NetGuardViewModel,
    onOpenDrawer: () -> Unit = {}
) {
    val currentMac by viewModel.currentMac.collectAsStateWithLifecycle()
    val localIp by viewModel.localIp.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val rootStatus by viewModel.rootVerificationStatus.collectAsStateWithLifecycle()
    val rootInfo by viewModel.rootValidationInfo.collectAsStateWithLifecycle()
    var showInfoDialog by remember { mutableStateOf(false) }

    val statusColor = when (rootStatus) {
        RootVerificationStatus.VERIFIED_ROOT -> CyberGreen
        RootVerificationStatus.SIMULATION_MODE -> CyberOrange
        RootVerificationStatus.CHECKING -> CyberOrange
        RootVerificationStatus.DENIED -> CyberRed
    }

    val statusText = when (rootStatus) {
        RootVerificationStatus.VERIFIED_ROOT -> "صلاحيات روت: نشطة (libsu)"
        RootVerificationStatus.SIMULATION_MODE -> "وضع المحاكاة نشط"
        RootVerificationStatus.CHECKING -> "فحص صلاحية الروت..."
        RootVerificationStatus.DENIED -> "صلاحيات روت مرفوضة"
    }

    Surface(
        color = CyberSurface,
        border = BorderStroke(1.dp, CyberBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: Drawer Hamburger Button + Title & Status Indicators
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("open_drawer_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "القائمة الجانبية",
                        tint = CyberGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "الشعار",
                            tint = CyberGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NetGuard Root",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextPrimary,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CyberGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "IP: $localIp",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberTextSecondary
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                IconButton(
                    onClick = {
                        val nextMode = when (themeMode) {
                            ThemeMode.SYSTEM -> ThemeMode.DARK
                            ThemeMode.DARK -> ThemeMode.LIGHT
                            ThemeMode.LIGHT -> ThemeMode.SYSTEM
                        }
                        viewModel.setThemeMode(nextMode)
                    },
                    modifier = Modifier.testTag("header_theme_toggle")
                ) {
                    Icon(
                        imageVector = when (themeMode) {
                            ThemeMode.SYSTEM -> Icons.Default.Settings
                            ThemeMode.DARK -> Icons.Default.NightsStay
                            ThemeMode.LIGHT -> Icons.Default.WbSunny
                        },
                        contentDescription = "Theme Toggle",
                        tint = CyberGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CyberSurfaceVariant
                    ),
                    border = BorderStroke(
                        1.dp,
                        statusColor.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.testTag("root_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_alpha"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (rootStatus == RootVerificationStatus.VERIFIED_ROOT) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Status",
                        tint = statusColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "توثيق صلاحيات الروت (RootAccessManager)", color = CyberTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = when (rootStatus) {
                            RootVerificationStatus.VERIFIED_ROOT ->
                                "تم توثيق صلاحية الجذر (Superuser) بنجاح عبر مدير صلاحيات الروت (RootAccessManager/libsu). التطبيق يتصل مباشرة بـ binary su للنظام للتنفيذ الفعلي."
                            RootVerificationStatus.SIMULATION_MODE ->
                                "لم يتم منح صلاحيات الروت في هذا الجهاز. يعمل التطبيق في 'وضع المحاكاة الآمن' (Simulation Fallback Mode) عبر محرك libsu التخيلي لتجربة كافة الأوامر والوظائف دون مخاطر."
                            RootVerificationStatus.CHECKING ->
                                "جاري التحقق من تفويض صلاحيات السوبر يوزر بواسطة libsu عند إقلاع التطبيق..."
                            RootVerificationStatus.DENIED ->
                                "تم رفض إذن الروت أو تعذر الوصول إلى su. يعمل التطبيق في وضع المحاكاة."
                        },
                        color = CyberTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    rootInfo?.let { info ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkBg),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("تفاصيل التحقق بواسطة RootAccessManager:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberGreen)
                                Text("UID المحرك: ${info.shellUid}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberTextPrimary)
                                Text("إصدار su: ${info.shellVersion}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberTextPrimary)
                                Text("مسار الثنائي: ${info.suBinaryPath}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberTextPrimary)
                                Text("SELinux: ${info.selinuxMode}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberTextPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = CyberGreen)
                ) {
                    Text("إغلاق")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showInfoDialog = false
                        viewModel.requestRootAccess()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CyberGreen)
                ) {
                    Text("إعادة الفحص بواسطة RootAccessManager", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CyberSurface,
            textContentColor = CyberTextSecondary
        )
    }
}