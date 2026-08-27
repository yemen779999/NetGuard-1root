package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.shadow
import com.example.network.AnimationLevel
import com.example.network.NetGuardViewModel
import com.example.network.Screen
import com.example.ui.theme.*

/**
 * Reusable GlassCard Composable component that reads the '3D Effects' state
 * to conditionally apply semi-transparent background blurs and elevated shadows,
 * aligning with the Obsidian Minimalist Cyber Security design language.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    accentGlow: Color = CyberGreen,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val is3dEnabled = Local3dEffectsEnabled.current

    val containerColor = if (is3dEnabled) {
        CyberSurface.copy(alpha = 0.85f)
    } else {
        CyberSurface
    }

    val cardElevation = if (is3dEnabled) elevation else 0.dp
    val borderBrush = if (is3dEnabled) {
        Brush.linearGradient(
            listOf(
                accentGlow.copy(alpha = 0.6f),
                CyberBorder,
                accentGlow.copy(alpha = 0.2f)
            )
        )
    } else {
        Brush.linearGradient(listOf(CyberBorder, CyberBorder))
    }

    Surface(
        modifier = modifier
            .then(
                if (is3dEnabled) {
                    Modifier.shadow(
                        elevation = cardElevation,
                        shape = shape,
                        ambientColor = accentGlow.copy(alpha = 0.25f),
                        spotColor = accentGlow.copy(alpha = 0.4f)
                    )
                } else Modifier
            )
            .border(
                width = if (is3dEnabled) 1.2.dp else 1.dp,
                brush = borderBrush,
                shape = shape
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        shape = shape,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun CyberGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    accentGlow: Color = CyberGreen,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        shape = shape,
        accentGlow = accentGlow,
        onClick = onClick,
        content = content
    )
}

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

fun getBottomNavigationItems() = listOf(
    NavigationItem("الرئيسية", Icons.Default.VpnKey, Screen.Main),
    NavigationItem("MacManager", Icons.Default.SettingsEthernet, Screen.MacManager),
    NavigationItem("سجل العمليات", Icons.Default.ListAlt, Screen.Logs),
    NavigationItem("رادار الشبكة", Icons.Default.Radar, Screen.NetworkRadar),
    NavigationItem("قطع النت", Icons.Default.Block, Screen.BlockedDevices)
)

fun getDrawerNavigationItems() = listOf(
    NavigationItem("MacManager (إدارة الماك)", Icons.Default.SettingsEthernet, Screen.MacManager),
    NavigationItem("سجلات عمليات الماك", Icons.Default.ListAlt, Screen.Logs),
    NavigationItem("الطرفية su", Icons.Default.Terminal, Screen.RootConsole),
    NavigationItem("المحفوظة", Icons.Default.Bookmark, Screen.SavedMacs),
    NavigationItem("السجل والتدقيق", Icons.Default.History, Screen.History),
    NavigationItem("تغيير الهوية", Icons.Default.NetworkWifi, Screen.NetworkIdentity),
    NavigationItem("الإعدادات", Icons.Default.Settings, Screen.Settings),
    NavigationItem("الحساب", Icons.Default.AccountCircle, Screen.SignIn)
)

@Composable
fun AppBottomNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    val items = getBottomNavigationItems()
    
    Surface(
        color = CyberSurface,
        border = BorderStroke(1.dp, CyberBorder),
        shadowElevation = 8.dp
    ) {
        NavigationBar(
            containerColor = CyberSurface,
            contentColor = CyberTextPrimary,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            items.forEach { item ->
                val selected = currentScreen == item.screen
                NavigationBarItem(
                    selected = selected,
                    onClick = { onScreenSelected(item.screen) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) CyberDarkBg else CyberGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) CyberGreen else CyberTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberDarkBg,
                        selectedTextColor = CyberGreen,
                        indicatorColor = CyberGreen,
                        unselectedIconColor = CyberGreen,
                        unselectedTextColor = CyberTextSecondary
                    ),
                    modifier = Modifier.testTag("bottom_nav_${item.screen.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
fun AppNavigationRail(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    val allItems = getBottomNavigationItems() + getDrawerNavigationItems()

    NavigationRail(
        containerColor = CyberSurface,
        contentColor = CyberTextPrimary,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyberGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "NetGuard Logo",
                        tint = CyberGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "NetGuard",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.weight(1f))
        allItems.forEach { item ->
            val selected = currentScreen == item.screen
            NavigationRailItem(
                selected = selected,
                onClick = { onScreenSelected(item.screen) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) CyberDarkBg else CyberGreen,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = CyberDarkBg,
                    selectedTextColor = CyberGreen,
                    indicatorColor = CyberGreen,
                    unselectedIconColor = CyberGreen,
                    unselectedTextColor = CyberTextSecondary
                ),
                modifier = Modifier.testTag("nav_rail_${item.screen.name.lowercase()}")
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun CollapsibleConsoleLogs(viewModel: NetGuardViewModel) {
    val logs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    var isExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Surface(
        color = CyberSurfaceVariant,
        border = BorderStroke(1.dp, CyberBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar (Click to toggle expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Console",
                        tint = CyberGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[SYS LOGS]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = logs.firstOrNull() ?: "لا توجد سجلات",
                        fontSize = 11.sp,
                        color = CyberTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = CyberGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${logs.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = CyberGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded Terminal Body
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberDarkBg)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سجل الأوامر المباشرة للنظام (Live Telemetry Log)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen,
                            fontFamily = FontFamily.Monospace
                        )

                        Row {
                            TextButton(
                                onClick = {
                                    val fullLogText = logs.joinToString("\n")
                                    clipboardManager.setText(AnnotatedString(fullLogText))
                                    Toast.makeText(context, "تم نسخ جميع السجلات", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ", fontSize = 10.sp, color = CyberGreen, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs) { log ->
                            val isError = log.contains("[خطأ") || log.contains("Error") || log.contains("FAILED")
                            val isWarning = log.contains("[تنبيه") || log.contains("Warning")
                            val isSuccess = log.contains("[نجاح") || log.contains("[تأكيد") || log.contains("SUCCESS")

                            val textColor = when {
                                isError -> CyberRed
                                isWarning -> CyberOrange
                                isSuccess -> CyberGreen
                                else -> CyberTextSecondary
                            }

                            Text(
                                text = log,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = textColor,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
