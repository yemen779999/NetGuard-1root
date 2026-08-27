package com.example.ui

import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper date formatter
private fun Long.formatTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}

// ============================================================================
// SCREEN 1: MAIN IDENTITY SCREEN (مركز الهوية والتحكم بالشبكة)
// ============================================================================
@Composable
fun MainIdentityScreen(viewModel: NetGuardViewModel) {
    val currentMac by viewModel.currentMac.collectAsStateWithLifecycle()
    val hardwareMac by viewModel.hardwareMac.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var macInput by remember { mutableStateOf("") }
    var ipInput by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf("") }

    LaunchedEffect(currentMac) {
        if (macInput.isEmpty()) {
            macInput = currentMac
        }
    }

    val isRooted by viewModel.isRootGranted.collectAsStateWithLifecycle()
    val connectionType by viewModel.connectionType.collectAsStateWithLifecycle()
    val interfaceStatus by viewModel.interfaceStatus.collectAsStateWithLifecycle()
    val isFirewallEnabled by viewModel.isFirewallEnabled.collectAsStateWithLifecycle()
    val localIp by viewModel.localIp.collectAsStateWithLifecycle()
    val selectedIface by viewModel.selectedInterface.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 🛡️ CYBERSECURITY HERO BANNER & APP ICON SHOWCASE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("cyber_security_banner_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cyber_hero_1787801239559),
                        contentDescription = "Cyber Security Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, CyberDarkBg.copy(alpha = 0.85f))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon_1787801224001),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, CyberGreen, RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "NETGUARD ROOT SECURITY",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyberGreen,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "درع الأمن السيبراني وهندسة الشبكات 3D",
                                fontSize = 11.sp,
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 🌐 LIVE NETWORK DASHBOARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("live_network_dashboard")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(CyberGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "Network",
                                    tint = CyberGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "مراقب اتصال وهندسة الشبكة",
                                color = CyberTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { viewModel.refreshNetworkInfo() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyberSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Info",
                                tint = CyberGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("نوع الاتصال", fontSize = 11.sp, color = CyberTextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = connectionType,
                                fontSize = 13.sp,
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("منفذ الشبكة", fontSize = 11.sp, color = CyberTextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$selectedIface: ",
                                    fontSize = 13.sp,
                                    color = CyberTextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = interfaceStatus,
                                    fontSize = 12.sp,
                                    color = if (interfaceStatus.contains("Up") || interfaceStatus.contains("نشط")) CyberGreen else CyberOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("عنوان IP الداخلي", fontSize = 11.sp, color = CyberTextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = localIp.ifEmpty { "127.0.0.1" },
                                fontSize = 13.sp,
                                color = CyberTextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Firewall",
                                tint = if (isFirewallEnabled) CyberGreen else CyberTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "جدار الحماية الفوري (Root Firewall)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextPrimary
                                )
                                Text(
                                    text = "فلترة وتصفية حزم البيانات عبر iptables",
                                    fontSize = 10.sp,
                                    color = CyberTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isFirewallEnabled,
                            onCheckedChange = { viewModel.toggleRootFirewall(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberDarkBg,
                                checkedTrackColor = CyberGreen,
                                uncheckedThumbColor = CyberTextSecondary,
                                uncheckedTrackColor = CyberSurface
                            ),
                            modifier = Modifier.testTag("root_firewall_toggle")
                        )
                    }
                }
            }
        }

        // 🆔 DIGITAL IDENTITY CARD (CURRENT MAC & HARDWARE MAC)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بطاقة الهوية الرقمية الحالية (Active Identity)",
                            color = CyberGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Badge(containerColor = CyberGreen.copy(alpha = 0.2f)) {
                            Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberGreen, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("عنوان الماك النشط (Active MAC)", fontSize = 11.sp, color = CyberTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentMac.ifEmpty { "قيد القراءة..." },
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary,
                                modifier = Modifier.testTag("current_mac_display")
                            )
                        }
                        IconButton(
                            onClick = {
                                if (currentMac.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(currentMac))
                                    Toast.makeText(context, "تم نسخ العنوان للمجلد", Toast.LENGTH_SHORT).show()
                                    viewModel.addLog("نسخ الماك النشط: $currentMac")
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CyberSurfaceVariant)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberGreen, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("العنوان الحقيقي للجهاز (Hardware MAC)", fontSize = 11.sp, color = CyberTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = hardwareMac.ifEmpty { "قيد القراءة..." },
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 🛠️ MAC CHANGE CONTROLLER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تغيير عنوان MAC المباشر (Spoof MAC)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "أدخل عنوان MAC جديد بالصيغة القياسية (XX:XX:XX:XX:XX:XX) أو ولد عنواناً عشوائياً.",
                        fontSize = 11.sp,
                        color = CyberTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = macInput,
                        onValueChange = { macInput = it },
                        label = { Text("عنوان MAC الجديد", fontSize = 12.sp) },
                        placeholder = { Text("00:11:22:33:44:55", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder,
                            focusedContainerColor = CyberSurfaceVariant,
                            unfocusedContainerColor = CyberSurfaceVariant,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mac_input_field")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                macInput = viewModel.generateRandomAndPreFill()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                            border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("generate_random_mac_btn")
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("عشوائي ذكي 🎲", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.randomizeMacViaLibSu()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberOrange),
                            border = BorderStroke(1.dp, CyberOrange.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("libsu_random_mac_btn")
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("عشوائي libsu ⚡", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.triggerMacChange(macInput)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberGreen,
                                contentColor = CyberDarkBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp)
                                .testTag("apply_mac_change_btn")
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تطبيق الهوية الجديدة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showSaveDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                            border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("save_mac_to_bookmarks_btn")
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ 🔖", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 🔀 ADVANCED IP REDIRECTION (NAT & IP SNOOPING)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إعادة توجيه IP وإخفاء الهوية (iptables IP NAT)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextPrimary
                        )
                        Badge(containerColor = CyberOrange.copy(alpha = 0.2f)) {
                            Text("IPTABLES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberOrange, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        label = { Text("عنوان IP الجديد المراد التوجيه إليه", fontSize = 12.sp) },
                        placeholder = { Text("192.168.1.188", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder,
                            focusedContainerColor = CyberSurfaceVariant,
                            unfocusedContainerColor = CyberSurfaceVariant,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_ip_input_field")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.changeLocalIpViaIptables(ipInput)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberSurfaceVariant,
                                contentColor = CyberGreen
                            ),
                            border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("apply_ip_nat_btn")
                        ) {
                            Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("توجيه IP (iptables)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.resetLocalIpConfig()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                            border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("reset_ip_nat_btn")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصفير IP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // SAVE MAC DIALOG
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text("حفظ عنوان MAC في المفضلة", color = CyberTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("أدخل اسماً توضيحياً لحفظ هذا العنوان في سجل العناوين المحفوظة:", fontSize = 12.sp, color = CyberTextSecondary)
                    OutlinedTextField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        label = { Text("اسم الجهاز / الوصف") },
                        placeholder = { Text("مثال: راوتر المنزل أو هاتف الاختبار") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("save_mac_title_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val macToSave = macInput.ifEmpty { currentMac }
                        viewModel.saveMacAddress(saveTitleInput, macToSave) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) showSaveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg)
                ) {
                    Text("حفظ الان", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("إلغاء", color = CyberTextSecondary)
                }
            },
            containerColor = CyberSurface
        )
    }
}

// ============================================================================
// SCREEN 2: SAVED MACS SCREEN (العناوين المحفوظة في Room)
// ============================================================================
@Composable
fun SavedMacsScreen(viewModel: NetGuardViewModel) {
    val savedMacs by viewModel.savedMacs.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newMac by remember { mutableStateOf("") }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportJsonContent by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }

    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val text = inputStream?.bufferedReader().use { reader -> reader?.readText() }
                if (!text.isNullOrBlank()) {
                    val success = viewModel.importSavedMacs(text, clearExisting = false)
                    if (success) {
                        Toast.makeText(context, "تم استيراد العناوين بنجاح من الملف!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "فشل استيراد الملف، تأكد من صحة تنسيق JSON", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ في قراءة الملف: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filteredList = remember(savedMacs, searchQuery) {
        if (searchQuery.isBlank()) savedMacs
        else savedMacs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.macAddress.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDarkBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "قائمة عناوين MAC المحفوظة (Room Database)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "إدارة وحفظ الهويات المفضلة للتنقل السريع بين بروفايلات الشبكة.",
                                    fontSize = 11.sp,
                                    color = CyberTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("بحث باسم الجهاز أو الماك...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CyberGreen) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberGreen,
                                    unfocusedBorderColor = CyberBorder,
                                    focusedContainerColor = CyberSurfaceVariant,
                                    unfocusedContainerColor = CyberSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("saved_macs_search_input")
                            )

                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("add_new_saved_mac_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    exportJsonContent = viewModel.exportSavedMacs()
                                    showExportDialog = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                                border = BorderStroke(1.dp, CyberGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("export_saved_macs_json_btn")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showImportDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberOrange),
                                border = BorderStroke(1.dp, CyberOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("import_saved_macs_json_btn")
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استيراد JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { jsonFilePickerLauncher.launch("application/json") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTextSecondary),
                                border = BorderStroke(1.dp, CyberBorder),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.width(48.dp).testTag("import_json_file_btn")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Open file", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "Empty",
                                tint = CyberTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "لا توجد نتائج تطابق البحث" else "لا توجد عناوين MAC محفوظة حالياً",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "اضغط على زر الإضافة بالنوع لإنشاء عنوان جديد في المفضلة.",
                                fontSize = 11.sp,
                                color = CyberTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredList) { savedMac ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("saved_mac_item_${savedMac.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = savedMac.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = savedMac.macAddress,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberGreen
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "تاريخ الحفظ: ${savedMac.timestamp.formatTimestamp()}",
                                    fontSize = 10.sp,
                                    color = CyberTextSecondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        viewModel.restoreMac(savedMac.macAddress)
                                        Toast.makeText(context, "جاري تطبيق ${savedMac.title}...", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.VpnKey, contentDescription = "Apply", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تطبيق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(savedMac.macAddress))
                                        Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberGreen, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.deleteSavedMac(savedMac)
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CyberRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Export Dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = {
                    Text("تصدير العناوين المحفوظة (JSON)", color = CyberTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text("يمكنك نسخ بيانات JSON أدناه أو مشاركتها لحفظ نسخة احتياطية من العناوين المفضلة:", fontSize = 12.sp, color = CyberTextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = exportJsonContent,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyberGreen),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberGreen,
                                unfocusedBorderColor = CyberBorder,
                                focusedContainerColor = CyberSurfaceVariant,
                                unfocusedContainerColor = CyberSurfaceVariant
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(exportJsonContent))
                            Toast.makeText(context, "تم نسخ بيانات JSON إلى الحافظة 📋", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ JSON", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("إغلاق", color = CyberTextSecondary)
                    }
                },
                containerColor = CyberSurface
            )
        }

        // Import Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = {
                    Text("استيراد العناوين (JSON)", color = CyberTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text("ألصق نص JSON الخاص بالعناوين المحفوظة أدناه، أو اضغط على استعراض لاختيار ملف من الجهاز:", fontSize = 12.sp, color = CyberTextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = importJsonInput,
                            onValueChange = { importJsonInput = it },
                            placeholder = { Text("أدخل نص JSON هنا...", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyberTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberOrange,
                                unfocusedBorderColor = CyberBorder,
                                focusedContainerColor = CyberSurfaceVariant,
                                unfocusedContainerColor = CyberSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { jsonFilePickerLauncher.launch("application/json") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberOrange),
                            border = BorderStroke(1.dp, CyberOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعراض ملف JSON من الجهاز...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importJsonInput.isNotBlank()) {
                                val success = viewModel.importSavedMacs(importJsonInput, clearExisting = false)
                                if (success) {
                                    Toast.makeText(context, "تم استيراد العناوين بنجاح من النص!", Toast.LENGTH_SHORT).show()
                                    showImportDialog = false
                                    importJsonInput = ""
                                } else {
                                    Toast.makeText(context, "فشل الاستيراد، تحقق من صحة تنسيق JSON", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "الرجاء إدخال نص JSON أو اختيار ملف", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberOrange, contentColor = CyberDarkBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("استيراد الآن", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("إلغاء", color = CyberTextSecondary)
                    }
                },
                containerColor = CyberSurface
            )
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("إضافة عنوان MAC جديد للمفضلة", color = CyberTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("اسم الجهاز") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newMac,
                            onValueChange = { newMac = it },
                            label = { Text("عنوان MAC") },
                            placeholder = { Text("00:11:22:33:44:55", fontFamily = FontFamily.Monospace) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveMacAddress(newTitle, newMac) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    newTitle = ""
                                    newMac = ""
                                    showAddDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg)
                    ) {
                        Text("حفظ", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("إلغاء", color = CyberTextSecondary) }
                },
                containerColor = CyberSurface
            )
        }
    }
}

// ============================================================================
// SCREEN 3: HISTORY SCREEN (سجل التغييرات والتدقيق الأمني)
// ============================================================================
@Composable
fun HistoryScreen(viewModel: NetGuardViewModel) {
    val macHistory by viewModel.macHistory.collectAsStateWithLifecycle()
    val configLogs by viewModel.configChangeLogs.collectAsStateWithLifecycle()
    val scannedDevices by viewModel.scannedDevicesHistory.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "سجل التغييرات والتدقيق الأمني (Audit History)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "تتبع كافة تعديلات عناوين MAC و IP والأجهزة المكتشفة بالشبكة.",
                                fontSize = 11.sp,
                                color = CyberTextSecondary
                            )
                        }

                        IconButton(
                            onClick = {
                                when (selectedTab) {
                                    0 -> viewModel.clearAllHistory()
                                    1 -> viewModel.clearConfigChangeLogs()
                                    2 -> viewModel.clearScannedDevicesHistory()
                                }
                                Toast.makeText(context, "تم مسح السجل المُنقّى", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CyberRed.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = CyberRed, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Selector Row
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = CyberSurfaceVariant,
                        contentColor = CyberGreen,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = CyberGreen
                                )
                            }
                        },
                        divider = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("تغييرات MAC (${macHistory.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("تدقيق IP/iptables (${configLogs.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("أجهزة مسجلة (${scannedDevices.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }

        // Tab 0: MAC Changes History
        if (selectedTab == 0) {
            if (macHistory.isEmpty()) {
                item {
                    EmptyHistoryCard("لا يوجد سجل لتغييرات MAC حتى الآن")
                }
            } else {
                items(macHistory) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Badge(containerColor = CyberGreen.copy(alpha = 0.2f)) {
                                        Text("MAC CHANGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberGreen, modifier = Modifier.padding(horizontal = 4.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item.timestamp.formatTimestamp(), fontSize = 10.sp, color = CyberTextSecondary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "الجديد: ${item.macAddress}",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberGreen
                                )
                                Text(
                                    text = "القديم: ${item.oldMacAddress.ifEmpty { "00:00:00:00:00:00" }}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextSecondary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteHistoryEntry(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CyberRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Tab 1: Config Changes (IP/iptables)
        if (selectedTab == 1) {
            if (configLogs.isEmpty()) {
                item {
                    EmptyHistoryCard("لا توجد سجلات لتغييرات إعدادات iptables و IP")
                }
            } else {
                items(configLogs) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Badge(containerColor = CyberOrange.copy(alpha = 0.2f)) {
                                        Text(item.changeType, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberOrange, modifier = Modifier.padding(horizontal = 4.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Dev: ${item.interfaceName}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberTextSecondary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(item.timestamp.formatTimestamp(), fontSize = 10.sp, color = CyberTextSecondary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "من [${item.oldValue}] -> إلى [${item.newValue}]",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextPrimary
                                )
                                if (item.details.isNotEmpty()) {
                                    Text(item.details, fontSize = 11.sp, color = CyberTextSecondary)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteConfigChangeLog(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CyberRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Tab 2: Scanned Device Records
        if (selectedTab == 2) {
            if (scannedDevices.isEmpty()) {
                item {
                    EmptyHistoryCard("لا يوجد سجل لأجهزة المسح المحفوظة")
                }
            } else {
                items(scannedDevices) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.deviceName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("IP: ${item.ip} | MAC: ${item.mac}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = CyberGreen)
                                Text(item.timestamp.formatTimestamp(), fontSize = 10.sp, color = CyberTextSecondary)
                            }
                            IconButton(onClick = { viewModel.deleteScannedDeviceRecord(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CyberRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, CyberBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = CyberTextSecondary, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, fontSize = 13.sp, color = CyberTextSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

// ============================================================================
// SCREEN 4: NETWORK RADAR SCREEN (NetCut Radar & AI Network Audit)
// ============================================================================
@Composable
fun NetworkRadarScreen(viewModel: NetGuardViewModel) {
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val subnet by viewModel.subnet.collectAsStateWithLifecycle()
    val selectedStrategy by viewModel.selectedScanStrategy.collectAsStateWithLifecycle()
    val aiState by viewModel.aiAnalysisState.collectAsStateWithLifecycle()
    val isLiveMonitoring by viewModel.isLiveMonitoring.collectAsStateWithLifecycle()
    val monitorInterval by viewModel.monitorIntervalSeconds.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var selectedDeviceForDetails by remember { mutableStateOf<NetworkDevice?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // RADAR CONTROL CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("network_radar_control_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Radar, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("رادار كشف وعزل أجهزة الشبكة", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                                Text("نطاق النطاق الفرعي: $subnet.0/24", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = CyberGreen)
                            }
                        }

                        if (scanState is ScanState.Scanning) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CyberGreen, strokeWidth = 2.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Monitoring Row
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isLiveMonitoring) Icons.Default.Sensors else Icons.Default.SensorsOff,
                                    contentDescription = null,
                                    tint = if (isLiveMonitoring) CyberGreen else CyberTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "المراقبة الحية المستمرة (${monitorInterval} ثوانٍ)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberTextPrimary
                                    )
                                    Text(
                                        if (isLiveMonitoring) "المراقبة نشطة - فحص دوري تلقائي" else "المراقبة متوقفة",
                                        fontSize = 10.sp,
                                        color = if (isLiveMonitoring) CyberGreen else CyberTextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = isLiveMonitoring,
                                onCheckedChange = { viewModel.toggleLiveMonitoring() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberDarkBg,
                                    checkedTrackColor = CyberGreen,
                                    uncheckedThumbColor = CyberTextSecondary,
                                    uncheckedTrackColor = CyberSurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("استراتيجية فحص الشبكة (Scan Strategy):", fontSize = 11.sp, color = CyberTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScanStrategy.values().forEach { strategy ->
                            val selected = selectedStrategy == strategy
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectScanStrategy(strategy) },
                                label = { Text(strategy.displayName.take(16), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberGreen,
                                    selectedLabelColor = CyberDarkBg,
                                    containerColor = CyberSurfaceVariant,
                                    labelColor = CyberTextSecondary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                                .testTag("start_radar_scan_btn")
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مسح الشبكة الآن 📡", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.analyzeNetworkWithAi() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                            border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("ai_analyze_network_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحليل الذكاء AI 🤖", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // AI ANALYSIS RESULT CARD
        if (aiState !is AiAnalysisState.Idle) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تقرير تحليل الأمان التفاعلي (Gemini 3.1 Pro)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberGreen)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        when (aiState) {
                            is AiAnalysisState.Analyzing -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CyberGreen, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("جاري قراءة هندسة الأجهزة وتوليد التحليل الأمني...", fontSize = 12.sp, color = CyberTextSecondary)
                                }
                            }
                            is AiAnalysisState.Success -> {
                                Text(
                                    text = (aiState as AiAnalysisState.Success).result,
                                    fontSize = 12.sp,
                                    color = CyberTextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                            is AiAnalysisState.Error -> {
                                Text(
                                    text = (aiState as AiAnalysisState.Error).message,
                                    fontSize = 12.sp,
                                    color = CyberRed
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // DISCOVERED DEVICES LIST
        val devices = when (scanState) {
            is ScanState.Success -> (scanState as ScanState.Success).devices
            else -> emptyList()
        }

        if (devices.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.WifiTethering, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (scanState is ScanState.Scanning) "جاري مسح الشبكة حالياً..." else "اضغط على زر المسح لبدء اكتشاف الأجهزة المتصلة",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextSecondary
                        )
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الأجهزة المكتشفة بالشبكة (${devices.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberGreen)
                    Text("تنسيق NetCut & OUI ⚡", fontSize = 10.sp, color = CyberTextSecondary)
                }
            }

            items(devices) { device ->
                val isBlocked = device.isBlocked

                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isBlocked) CyberRed.copy(alpha = 0.08f) else CyberSurface),
                    border = BorderStroke(1.dp, if (isBlocked) CyberRed.copy(alpha = 0.6f) else CyberBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedDeviceForDetails = device }
                        .testTag("scanned_device_${device.ip}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (device.type) {
                                        DeviceType.ROUTER -> Icons.Default.Router
                                        DeviceType.LAPTOP -> Icons.Default.Laptop
                                        DeviceType.TV -> Icons.Default.Tv
                                        DeviceType.SMART_HOME -> Icons.Default.SmartToy
                                        DeviceType.VIRTUAL_MACHINE -> Icons.Default.Computer
                                        else -> Icons.Default.Smartphone
                                    },
                                    contentDescription = null,
                                    tint = if (isBlocked) CyberRed else CyberGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(device.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                                        if (device.vendor != "Unknown") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Badge(containerColor = CyberGreen.copy(alpha = 0.15f)) {
                                                Text(device.vendor, fontSize = 9.sp, color = CyberGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                            }
                                        }
                                    }
                                    Text("IP: ${device.ip}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                                    Text("MAC: ${device.mac} • ${device.typeLabel}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberTextSecondary)
                                    if (device.hostname != "Unknown") {
                                        Text("Host: ${device.hostname}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberGreen.copy(alpha = 0.8f))
                                    }
                                }
                            }

                            Badge(
                                containerColor = if (isBlocked) CyberRed else CyberGreen,
                                contentColor = CyberDarkBg
                            ) {
                                Text(
                                    text = if (isBlocked) "مقطوع/محظور 🛑" else "متصل 🟢",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = CyberBorder.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.toggleDeviceBlock(device)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBlocked) CyberGreen else CyberRed,
                                    contentColor = if (isBlocked) CyberDarkBg else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBlocked) "إلغاء قطع الإنترنت" else "قطع الإنترنت (NetCut)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { selectedDeviceForDetails = device },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                                border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(0.7f)
                                    .height(38.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تفاصيل 🔍", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // DEVICE DETAILS DIALOG
    selectedDeviceForDetails?.let { dev ->
        AlertDialog(
            onDismissRequest = { selectedDeviceForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeviceHub, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تفاصيل الجهاز المكتشف", color = CyberTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow(label = "اسم الجهاز", value = dev.name)
                    DetailRow(label = "عنوان IP", value = dev.ip, canCopy = true) {
                        clipboardManager.setText(AnnotatedString(dev.ip))
                        Toast.makeText(context, "تم نسخ عنوان IP", Toast.LENGTH_SHORT).show()
                    }
                    DetailRow(label = "عنوان MAC", value = dev.mac, canCopy = true) {
                        clipboardManager.setText(AnnotatedString(dev.mac))
                        Toast.makeText(context, "تم نسخ عنوان MAC", Toast.LENGTH_SHORT).show()
                    }
                    DetailRow(label = "الشركة المصنعة (Vendor)", value = dev.vendor)
                    DetailRow(label = "نوع الجهاز", value = dev.typeLabel)
                    DetailRow(label = "اسم المضيف (Hostname)", value = dev.hostname)
                    DetailRow(label = "حالة الاتصال", value = if (dev.isBlocked) "مقطوع عبر NetCut 🛑" else "نشط ومتصل 🟢")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveMacAddress(dev.name, dev.mac) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        selectedDeviceForDetails = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg)
                ) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حفظ بالمفضلة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDeviceForDetails = null }) {
                    Text("إغلاق", color = CyberTextSecondary)
                }
            },
            containerColor = CyberSurface
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, canCopy: Boolean = false, onCopy: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = CyberTextSecondary)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CyberTextPrimary, fontFamily = if (label.contains("IP") || label.contains("MAC")) FontFamily.Monospace else FontFamily.Default)
        }
        if (canCopy && onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberGreen, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ============================================================================
// SCREEN 5: BLOCKED DEVICES SCREEN (NetCut Active Block List)
// ============================================================================
@Composable
fun BlockedDevicesScreen(viewModel: NetGuardViewModel) {
    val blockedDevices by viewModel.blockedDevices.collectAsStateWithLifecycle()
    val isAntiBan by viewModel.isAntiBanEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("blocked_devices_header_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = CyberRed, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("الأجهزة المحظورة ومقطوعة الاتصال (NetCut)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                                Text("إدارة حزم ARP Poisoning & iptables Reject", fontSize = 11.sp, color = CyberTextSecondary)
                            }
                        }

                        Badge(containerColor = CyberRed, contentColor = Color.White) {
                            Text("${blockedDevices.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberSurfaceVariant)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = if (isAntiBan) CyberGreen else CyberTextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حماية الهاتف من الهجمات المرتدة (Anti-NetCut)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        }

                        Switch(
                            checked = isAntiBan,
                            onCheckedChange = { viewModel.toggleAntiBan(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberDarkBg, checkedTrackColor = CyberGreen)
                        )
                    }
                }
            }
        }

        if (blockedDevices.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد أجهزة محظورة حالياً",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "جميع الأجهزة بالشبكة تتصل بصورة طبيعية. للحظر، توجه لرادار الشبكة.",
                            fontSize = 11.sp,
                            color = CyberTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(blockedDevices) { device ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberRed.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("IP: ${device.ip}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberRed)
                            Text("MAC: ${device.mac}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberTextSecondary)
                        }

                        Button(
                            onClick = {
                                viewModel.toggleDeviceBlock(device)
                                Toast.makeText(context, "تمت إعادة الاتصال لـ ${device.ip}", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إلغاء الحظر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SCREEN 6: SETTINGS SCREEN (الإعدادات والأمان وبروفايلات Room)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: NetGuardViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isAntiBan by viewModel.isAntiBanEnabled.collectAsStateWithLifecycle()
    val isRandomPrevent by viewModel.isRandomMacDisabled.collectAsStateWithLifecycle()
    val isDnsActive by viewModel.isDnsRedirectionActive.collectAsStateWithLifecycle()
    val activeDnsServer by viewModel.activeDnsServer.collectAsStateWithLifecycle()
    val isFirewallEnabled by viewModel.isFirewallEnabled.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val networkProfiles by viewModel.networkProfiles.collectAsStateWithLifecycle()
    val selectedIface by viewModel.selectedInterface.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var profileNameInput by remember { mutableStateOf("") }
    var profileMacInput by remember { mutableStateOf("") }
    var profileIpInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SECTION 1: THEME SELECTOR
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("theme_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("مظهر ونمط التطبيق (Theme Settings)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.values().forEach { mode ->
                            val selected = themeMode == mode
                            val modeLabel = when (mode) {
                                ThemeMode.SYSTEM -> "تلقائي النظام"
                                ThemeMode.LIGHT -> "مظهر فاتح ☀️"
                                ThemeMode.DARK -> "مظهر داكن 🌙"
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(modeLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberGreen,
                                    selectedLabelColor = CyberDarkBg,
                                    containerColor = CyberSurfaceVariant,
                                    labelColor = CyberTextSecondary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // SECTION 2: DISPLAY & EFFECTS
        item {
            val is3dEffects by viewModel.is3dEffectsEnabled.collectAsStateWithLifecycle()
            val animationLevel by viewModel.animationLevel.collectAsStateWithLifecycle()

            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("display_and_effects_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "العرض والتأثيرات (Display & Effects)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3D Effects Switch
                    SettingsSwitchRow(
                        title = "تأثيرات العمق 3D (3D Effects)",
                        subtitle = "تفعيل خلفيات الزجاج الشفاف، ظلال العمق والتوهج السيبراني",
                        checked = is3dEffects,
                        onCheckedChange = { viewModel.toggle3dEffects(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("مستوى الحركة والأنيميشن (Animation Level):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth().testTag("animation_level_segmented_button")
                    ) {
                        AnimationLevel.values().forEachIndexed { index, level ->
                            SegmentedButton(
                                selected = animationLevel == level,
                                onClick = { viewModel.setAnimationLevel(level) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = AnimationLevel.values().size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = CyberGreen,
                                    activeContentColor = CyberDarkBg,
                                    inactiveContainerColor = CyberSurfaceVariant,
                                    inactiveContentColor = CyberTextSecondary
                                )
                            ) {
                                Text(
                                    when (level) {
                                        AnimationLevel.FULL -> "Full"
                                        AnimationLevel.REDUCED -> "Reduced"
                                        AnimationLevel.OFF -> "Off"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 2: ROOT SECURITY RULES
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("root_security_rules_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("قواعد الحماية والأمان الجذري (Root Security)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberGreen)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Firewall Switch
                    SettingsSwitchRow(
                        title = "جدار الحماية الفوري (Root Firewall)",
                        subtitle = "تفعيل حظر الحزم غير المصرح بها عبر iptables",
                        checked = isFirewallEnabled,
                        onCheckedChange = { viewModel.toggleRootFirewall(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Anti-Ban Switch
                    SettingsSwitchRow(
                        title = "حماية Anti-NetCut من الهجمات المرتدة",
                        subtitle = "منع الأجهزة الأخرى من قطع اتصال جهازك عبر تثبيت جدول ARP",
                        checked = isAntiBan,
                        onCheckedChange = { viewModel.toggleAntiBan(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Random MAC Prevention
                    SettingsSwitchRow(
                        title = "منع الماك العشوائي بالنظام (Disable Random MAC)",
                        subtitle = "إجبار نظام الأندرويد على استخدام الماك المعين من قبل التطبيق فقط",
                        checked = isRandomPrevent,
                        onCheckedChange = { viewModel.toggleRandomMacPrevention(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secure DNS Redirection
                    SettingsSwitchRow(
                        title = "إعادة توجيه DNS الآمن ($activeDnsServer)",
                        subtitle = "توجيه طلبات DNS المشفرة عبر Cloudflare (1.1.1.1)",
                        checked = isDnsActive,
                        onCheckedChange = { viewModel.toggleDnsRedirection(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Biometric Authentication Switch
                    SettingsSwitchRow(
                        title = "تأمين بالبصمة والوجه (Biometric Prompt)",
                        subtitle = "حماية التطبيق بالبصمة قبل إظهار بيانات الشبكة الحساسة وأوامر الروت",
                        checked = isBiometricEnabled,
                        onCheckedChange = { viewModel.toggleBiometricEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { viewModel.flushIpAndIptablesViaLibSu() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberOrange),
                        border = BorderStroke(1.dp, CyberOrange.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تفريغ وتصفير كافة قواعد iptables و IP Flush 🧹", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // SECTION 3: NETWORK PROFILES MANAGER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("إدارة بروفايلات الشبكة (Room Network Profiles)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = profileNameInput,
                        onValueChange = { profileNameInput = it },
                        label = { Text("اسم البروفايل (مثال: بروفايل مقهى الإنترنت)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = profileMacInput,
                            onValueChange = { profileMacInput = it },
                            label = { Text("MAC البروفايل") },
                            placeholder = { Text("00:11:22:33:44:55", fontFamily = FontFamily.Monospace) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = profileIpInput,
                            onValueChange = { profileIpInput = it },
                            label = { Text("IP البروفايل (اختياري)") },
                            placeholder = { Text("192.168.1.150", fontFamily = FontFamily.Monospace) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.saveNetworkProfile(profileNameInput, selectedIface, profileMacInput, profileIpInput)
                            Toast.makeText(context, "تم حفظ البروفايل بنجاح", Toast.LENGTH_SHORT).show()
                            profileNameInput = ""
                            profileMacInput = ""
                            profileIpInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ البروفايل الجديد 💾", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // PROFILES LIST
        if (networkProfiles.isNotEmpty()) {
            item {
                Text("البروفايلات المحفوظة (${networkProfiles.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberGreen)
            }

            items(networkProfiles) { profile ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.profileName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                            Text("MAC: ${profile.customMac} | Dev: ${profile.interfaceName}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = CyberGreen)
                        }

                        Row {
                            Button(
                                onClick = { viewModel.applyNetworkProfile(profile) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("تطبيق", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { viewModel.deleteNetworkProfile(profile.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = CyberRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurfaceVariant)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            Text(subtitle, fontSize = 10.sp, color = CyberTextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = CyberDarkBg, checkedTrackColor = CyberGreen)
        )
    }
}

// ============================================================================
// SCREEN 7: ROOT CONSOLE SCREEN (موجه الأوامر المباشر والطرفية)
// ============================================================================
@Composable
fun RootConsoleScreen(viewModel: NetGuardViewModel) {
    val commandHistory by viewModel.commandHistory.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecutingCommand.collectAsStateWithLifecycle()
    val isRootGranted by viewModel.isRootGranted.collectAsStateWithLifecycle()
    var commandInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val presets = listOf("ip neigh show", "su -v", "getenforce", "iptables -L -n -v", "ip link show wlan0", "id")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!isRootGranted) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberRed.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("صلاحية الروت غير ممنوحة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberRed)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("التطبيق لا يعمل بصلاحيات الروت. اضغط الزر التالي لمحاولة طلب الصلاحيات.", fontSize = 14.sp, color = CyberTextPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.requestRootAccess() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("طلب صلاحيات الروت", color = CyberDarkBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // TERMINAL INPUT CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("root_console_input_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(CyberGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("طرفية الأوامر المباشرة (libsu Root Shell)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        }

                        IconButton(
                            onClick = { viewModel.clearCommandHistory() },
                            modifier = Modifier.clip(CircleShape).background(CyberRed.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear", tint = CyberRed, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("أوامر سريعة مقترحة:", fontSize = 11.sp, color = CyberTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            SuggestionChip(
                                onClick = { commandInput = preset },
                                label = { Text(preset, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = CyberSurfaceVariant, labelColor = CyberGreen),
                                border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = CyberBorder),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        label = { Text("أدخل أمر Terminal هنا", fontSize = 12.sp) },
                        placeholder = { Text("su -c ip link show wlan0", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = CyberBorder,
                            focusedContainerColor = CyberSurfaceVariant,
                            unfocusedContainerColor = CyberSurfaceVariant,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("terminal_command_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.executeShellCommand(commandInput)
                        },
                        enabled = !isExecuting && commandInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("execute_terminal_command_btn")
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CyberDarkBg, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري التنفيذ عبر libsu...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تنفيذ الأمر $", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // COMMAND RESULTS LIST
        if (commandHistory.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = CyberTextSecondary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لم يتم تنفيذ أي أمر حتى الآن", fontSize = 13.sp, color = CyberTextSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            items(commandHistory) { result ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, if (result.isSuccess) CyberGreen.copy(alpha = 0.5f) else CyberRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$ ${result.command}",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CyberGreen
                            )
                            Badge(
                                containerColor = if (result.isSuccess) CyberGreen else CyberRed,
                                contentColor = CyberDarkBg
                            ) {
                                Text(
                                    text = if (result.isSimulated) "SIMULATED" else if (result.isSuccess) "EXIT 0" else "CODE ${result.code}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("المدة: ${result.durationMs}ms", fontSize = 10.sp, color = CyberTextSecondary)

                        if (result.stdout.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = result.stdout.joinToString("\n"),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberGreen
                                )
                            }
                        }

                        if (result.stderr.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = result.stderr.joinToString("\n"),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SCREEN 8: SIGN IN SCREEN (الحساب والتزامن السحابي)
// ============================================================================
@Composable
fun SignInScreen(viewModel: NetGuardViewModel) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = BorderStroke(1.dp, CyberBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CyberGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "الحساب والتزامن السحابي",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "مزامنة عناوين MAC المحفوظة وبروفايلات الشبكة عبر سحابة Google Secure Cloud.",
                    fontSize = 12.sp,
                    color = CyberTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        Toast.makeText(context, "تم إطلاق مزامنة Google الحساب بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("google_signin_button")
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الدخول باستخدام Google", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================================
// BIOMETRIC LOCK SCREEN OVERLAY
// ============================================================================
@Composable
fun BiometricLockScreen(viewModel: NetGuardViewModel) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    LaunchedEffect(Unit) {
        activity?.let { viewModel.triggerBiometricPrompt(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = CyberSurfaceVariant,
                border = BorderStroke(2.dp, CyberGreen),
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "قفل البصمة",
                        tint = CyberGreen,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "تأمين NetGuard Root",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "تطبيق حماية الشبكة مؤمن بالكامل عبر بروتوكول المصادقة البيومترية (Biometric & Device Security).",
                fontSize = 13.sp,
                color = CyberTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    activity?.let { viewModel.triggerBiometricPrompt(it) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("biometric_unlock_button")
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("فتح القفل بواسطة البصمة / الوجه 🔓", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    viewModel.setBiometricAuthenticated(true)
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberOrange),
                border = BorderStroke(1.dp, CyberOrange.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("biometric_fallback_button")
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تجاوز المصادقة (وضع الديمو/المحاكاة)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NetworkIdentityScreen(viewModel: NetGuardViewModel) {
    var macAddress by remember { mutableStateOf("") }
    var ipAddress by remember { mutableStateOf("") }
    val context = LocalContext.current
    val isRootGranted by viewModel.isRootGranted.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isRootGranted) CyberGreen.copy(alpha = 0.1f) else CyberRed.copy(alpha = 0.1f))
                .padding(8.dp)
        ) {
            Icon(
                imageVector = if (isRootGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isRootGranted) CyberGreen else CyberRed
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRootGranted) "صلاحيات الروت: مفعّلة" else "صلاحيات الروت: غير مفعّلة",
                color = if (isRootGranted) CyberGreen else CyberRed,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("تغيير هوية الشبكة", style = MaterialTheme.typography.titleLarge, color = CyberTextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = macAddress,
            onValueChange = { macAddress = it },
            label = { Text("عنوان MAC الجديد (مثال: AA:BB:CC:DD:EE:FF)") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CyberSurface,
                unfocusedContainerColor = CyberSurface
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.triggerMacChange(macAddress)
                Toast.makeText(context, "تم إرسال أمر تغيير MAC", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تطبيق عنوان MAC", color = CyberDarkBg, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("عنوان IP الجديد (مثال: 192.168.1.100)") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CyberSurface,
                unfocusedContainerColor = CyberSurface
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.changeLocalIpViaIptables(ipAddress)
                Toast.makeText(context, "تم إرسال أمر تغيير IP", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تطبيق عنوان IP", color = CyberDarkBg, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DiagnosticMetricCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    isPositive: Boolean
) {
    Surface(
        color = CyberSurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isPositive) CyberGreen.copy(alpha = 0.3f) else CyberOrange.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = CyberTextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                color = if (isPositive) CyberGreen else CyberOrange,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RootRequiredScreen(viewModel: NetGuardViewModel) {
    val rootStatus by viewModel.rootVerificationStatus.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (rootStatus == RootVerificationStatus.DENIED) Icons.Default.Block else Icons.Default.Security,
            contentDescription = null,
            tint = CyberRed,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (rootStatus == RootVerificationStatus.DENIED) "تم رفض صلاحية الروت (Access Denied)" else "صلاحيات الروت غير متاحة",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (rootStatus == RootVerificationStatus.DENIED)
                "تم رفض طلب صلاحيات السوبر يوزر بواسطة Magisk/KernelSU أو انتهت مهلة الفحص على Android 12+. يمكنك المتابعة بوضع المحاكاة أو إعادة طلب الإذن."
            else
                "هذا التطبيق يستفيد من صلاحيات الجذر (Root/libsu) للتحكم المباشر بالشبكة. يمكنك استخدام وضع المحاكاة دون الحاجة لروت.",
            fontSize = 14.sp,
            color = CyberTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.requestRootAccess() },
            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("retry_root_check_button")
        ) {
            Text("إعادة التحقق من الروت (libsu Check)", fontWeight = FontWeight.Bold, color = CyberDarkBg)
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { viewModel.refreshNetworkInfo() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberOrange),
            border = BorderStroke(1.dp, CyberOrange),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("continue_simulation_button")
        ) {
            Text("المتابعة بوضع المحاكاة (Simulation Mode)", fontWeight = FontWeight.Bold)
        }
    }
}

