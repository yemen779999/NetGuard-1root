package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MacManagerLog
import com.example.network.NetGuardViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacManagerScreen(viewModel: NetGuardViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentMac by viewModel.currentMac.collectAsStateWithLifecycle()
    val hardwareMac by viewModel.hardwareMac.collectAsStateWithLifecycle()
    val selectedInterface by viewModel.selectedInterface.collectAsStateWithLifecycle()
    val availableInterfaces by viewModel.availableInterfaces.collectAsStateWithLifecycle()
    val isRandomMacDisabled by viewModel.isRandomMacDisabled.collectAsStateWithLifecycle()
    val isRootGranted by viewModel.isRootGranted.collectAsStateWithLifecycle()
    val logs by viewModel.macManagerLogs.collectAsStateWithLifecycle()

    var macAddressInput by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Manager Controls, 1: Activity Logs
    var showLogFileViewer by remember { mutableStateOf(false) }
    var logFileContent by remember { mutableStateOf("") }
    var logFilter by remember { mutableStateOf("ALL") } // "ALL", "SUCCESS", "FAILED", "CHANGE_MAC", "RANDOM_MAC"
    var logSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(currentMac) {
        if (macAddressInput.isEmpty() && currentMac.isNotEmpty()) {
            macAddressInput = currentMac
        }
    }

    val isValid = viewModel.isValidMac(macAddressInput)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HEADER
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(CyberGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsEthernet,
                                    contentDescription = null,
                                    tint = CyberGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "MacManager Network Engine",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextPrimary
                                )
                                Text(
                                    text = "إدارة هوية بطاقة الشبكة ومنع العشوائية",
                                    fontSize = 12.sp,
                                    color = CyberTextSecondary
                                )
                            }
                        }

                        // Root Status Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isRootGranted) CyberGreen.copy(alpha = 0.15f) else CyberOrange.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (isRootGranted) CyberGreen.copy(alpha = 0.4f) else CyberOrange.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isRootGranted) CyberGreen else CyberOrange)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRootGranted) "ROOT ACTIVE" else "DEMO/SIM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRootGranted) CyberGreen else CyberOrange,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = CyberBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // CURRENT HARDWARE / ACTIVE MAC DISPLAY
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = CyberSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("الماك النشط الحالي", fontSize = 11.sp, color = CyberTextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentMac.ifEmpty { "00:00:00:00:00:00" },
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            color = CyberSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("ماك العتاد الأصلي", fontSize = 11.sp, color = CyberTextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = hardwareMac.ifEmpty { "70:A8:E3:42:C8:D1" },
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // TABS: CONTROLS & LOGS
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CyberSurface,
                contentColor = CyberGreen,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyberGreen
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("أدوات MacManager", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("سجل العمليات (${logs.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                )
            }
        }

        if (selectedTab == 0) {
            // ==========================================
            // TAB 0: MAC MANAGER CONTROLS
            // ==========================================
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "إعدادات بطاقة الشبكة والماك",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen
                        )

                        // 1. SPINNER / DROPDOWN FOR NETWORK INTERFACE (Default: 'wlan0')
                        Column {
                            Text(
                                text = "واجهة الشبكة (Network Interface)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            ExposedDropdownMenuBox(
                                expanded = isDropdownExpanded,
                                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("network_interface_spinner")
                            ) {
                                OutlinedTextField(
                                    value = selectedInterface,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Router,
                                            contentDescription = null,
                                            tint = CyberGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = CyberSurfaceVariant,
                                        unfocusedContainerColor = CyberSurfaceVariant,
                                        focusedBorderColor = CyberGreen,
                                        unfocusedBorderColor = CyberBorder,
                                        focusedTextColor = CyberTextPrimary,
                                        unfocusedTextColor = CyberTextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = isDropdownExpanded,
                                    onDismissRequest = { isDropdownExpanded = false },
                                    modifier = Modifier.background(CyberSurface)
                                ) {
                                    val interfacesList = if (availableInterfaces.isNotEmpty()) availableInterfaces else listOf("wlan0", "eth0", "wlan1", "p2p0")
                                    interfacesList.forEach { iface ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (iface.startsWith("wlan")) Icons.Default.Wifi else Icons.Default.SettingsEthernet,
                                                        contentDescription = null,
                                                        tint = if (selectedInterface == iface) CyberGreen else CyberTextSecondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = iface,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = if (selectedInterface == iface) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selectedInterface == iface) CyberGreen else CyberTextPrimary
                                                    )
                                                    if (iface == "wlan0") {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("(Default)", fontSize = 11.sp, color = CyberTextSecondary)
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectedInterface.value = iface
                                                isDropdownExpanded = false
                                                viewModel.refreshNetworkInfo()
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = CyberTextPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // 2. EDITTEXT FOR MAC ADDRESS
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "عنوان MAC المستهدف (MAC Address)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyberTextSecondary
                                )

                                if (macAddressInput.isNotEmpty()) {
                                    Text(
                                        text = if (isValid) "صيغة صالحة ✓" else "صيغة غير صحيحة ✗",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isValid) CyberGreen else CyberRed
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = macAddressInput,
                                onValueChange = { input ->
                                    val formatted = input.uppercase().filter { it.isLetterOrDigit() || it == ':' || it == '-' }
                                    macAddressInput = formatted
                                },
                                label = { Text("MAC Address (مثال: 02:1A:C2:7B:44:89)") },
                                placeholder = { Text("XX:XX:XX:XX:XX:XX") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VpnKey,
                                        contentDescription = null,
                                        tint = if (isValid) CyberGreen else CyberOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (macAddressInput.isNotEmpty()) {
                                            IconButton(onClick = { macAddressInput = "" }) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "Clear",
                                                    tint = CyberTextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        IconButton(onClick = {
                                            macAddressInput = viewModel.generateRandomAndPreFill()
                                        }) {
                                            Icon(
                                                Icons.Default.Shuffle,
                                                contentDescription = "Generate Random MAC",
                                                tint = CyberGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = CyberSurfaceVariant,
                                    unfocusedContainerColor = CyberSurfaceVariant,
                                    focusedBorderColor = if (isValid) CyberGreen else CyberRed,
                                    unfocusedBorderColor = if (macAddressInput.isEmpty()) CyberBorder else if (isValid) CyberGreen.copy(alpha = 0.5f) else CyberRed.copy(alpha = 0.5f),
                                    focusedTextColor = CyberTextPrimary,
                                    unfocusedTextColor = CyberTextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mac_address_input")
                            )
                        }

                        // QUICK MAC FILL CHIPS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    macAddressInput = viewModel.generateRandomAndPreFill()
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ماك عشوائي", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    macAddressInput = hardwareMac.ifEmpty { "70:A8:E3:42:C8:D1" }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CyberBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTextPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ماك العتاد", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 3. ACTION BUTTONS: 'Change MAC' and 'Disable Random MAC'
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (macAddressInput.isBlank()) {
                                        Toast.makeText(context, "الرجاء إدخال عنوان MAC", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.triggerMacChange(macAddressInput, selectedInterface)
                                    Toast.makeText(context, "تم إرسال أمر تغيير MAC وتسجيله", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberGreen,
                                    contentColor = CyberDarkBg
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("change_mac_button")
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change MAC", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.disableRandomMac(selectedInterface)
                                    Toast.makeText(context, "تم تنفيذ أمر تعطيل الماك العشوائي وتوثيقه", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberSurfaceVariant,
                                    contentColor = CyberOrange
                                ),
                                border = BorderStroke(1.dp, CyberOrange.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("disable_random_mac_button")
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Disable Random MAC", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Divider(color = CyberBorder.copy(alpha = 0.5f))

                        // 4. TOGGLEBUTTON / SWITCH & CHECKBOX FOR 'DISABLE RANDOM MAC'
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "خيارات التحكم بالعشوائية (Random MAC Randomization)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary
                            )

                            // TOGGLE / SWITCH
                            Surface(
                                color = CyberSurfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isRandomMacDisabled) CyberGreen.copy(alpha = 0.4f) else CyberBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isRandomMacDisabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = if (isRandomMacDisabled) CyberGreen else CyberTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Disable Random MAC (Switch Toggle)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyberTextPrimary
                                            )
                                            Text(
                                                text = if (isRandomMacDisabled) "معطّل العشوائي: الماك ثابت ومستقر 🔒" else "مفعّل العشوائي: النظام يغير الماك تلقائياً 🔄",
                                                fontSize = 11.sp,
                                                color = if (isRandomMacDisabled) CyberGreen else CyberTextSecondary
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isRandomMacDisabled,
                                        onCheckedChange = { isChecked ->
                                            viewModel.toggleRandomMacPrevention(isChecked, selectedInterface)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CyberDarkBg,
                                            checkedTrackColor = CyberGreen,
                                            uncheckedThumbColor = CyberTextSecondary,
                                            uncheckedTrackColor = CyberSurface
                                        ),
                                        modifier = Modifier.testTag("disable_random_mac_toggle")
                                    )
                                }
                            }

                            // CHECKBOX FOR DISABLE RANDOM MAC
                            Surface(
                                color = CyberSurfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.toggleRandomMacPrevention(!isRandomMacDisabled, selectedInterface)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isRandomMacDisabled,
                                        onCheckedChange = { isChecked ->
                                            viewModel.toggleRandomMacPrevention(isChecked, selectedInterface)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = CyberGreen,
                                            checkmarkColor = CyberDarkBg,
                                            uncheckedColor = CyberTextSecondary
                                        ),
                                        modifier = Modifier.testTag("disable_random_mac_checkbox")
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "تعطيل الماك العشوائي (CheckBox)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberTextPrimary
                                        )
                                        Text(
                                            text = "فرض عنوان الماك الحقيقي أو المخصص بدلاً من التوزيع العشوائي",
                                            fontSize = 11.sp,
                                            color = CyberTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // MINI RECENT LOGS SNIPPET (Direct Preview in Controls Tab)
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
                                text = "آخر محاولات تغيير الماك والشبكة",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary
                            )
                            TextButton(onClick = { selectedTab = 1 }) {
                                Text("عرض الكل (${logs.size})", color = CyberGreen, fontSize = 12.sp)
                            }
                        }

                        if (logs.isEmpty()) {
                            Text(
                                text = "لا توجد سجلات بعد. سيتم توثيق كل محاولة تغيير MAC أو تعطيل العشوائية هنا.",
                                fontSize = 12.sp,
                                color = CyberTextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            val recentLogs = logs.take(3)
                            recentLogs.forEach { logItem ->
                                LogItemRow(log = logItem)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // TAB 1: DEDICATED LOG SECTION
            // ==========================================
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mac_manager_logs_section")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "سجل عمليات MacManager الموثق",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberGreen
                                )
                                Text(
                                    text = "يوثق جميع المحاولات الناجحة والفاشلة بالوقت والواجهة",
                                    fontSize = 11.sp,
                                    color = CyberTextSecondary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        val fullText = viewModel.readMacManagerLogFile()
                                        logFileContent = fullText
                                        showLogFileViewer = true
                                    },
                                    modifier = Modifier.testTag("export_logs_button")
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = "View Log File",
                                        tint = CyberGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val allLogsText = logs.joinToString("\n") { log ->
                                            "[${log.timestamp.formatTimestamp()}] [${if (log.isSuccess) "SUCCESS" else "FAILED"}] [${log.action}] Iface: ${log.interfaceUsed} | MAC: ${log.targetMac} | ${log.message}"
                                        }
                                        clipboardManager.setText(AnnotatedString(allLogsText))
                                        Toast.makeText(context, "تم نسخ جميع السجلات إلى الحافظة", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("copy_logs_button")
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy Logs",
                                        tint = CyberTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.clearMacManagerLogs()
                                        Toast.makeText(context, "تم مسح كافة السجلات", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("clear_logs_button")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Clear Logs",
                                        tint = CyberRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // SEARCH BAR
                        OutlinedTextField(
                            value = logSearchQuery,
                            onValueChange = { logSearchQuery = it },
                            placeholder = { Text("بحث في السجلات (IP, MAC, الواجهة...)") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (logSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { logSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null, tint = CyberTextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CyberSurfaceVariant,
                                unfocusedContainerColor = CyberSurfaceVariant,
                                focusedBorderColor = CyberGreen,
                                unfocusedBorderColor = CyberBorder
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // FILTER CHIPS
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = logFilter == "ALL",
                                onClick = { logFilter = "ALL" },
                                label = { Text("الكل (${logs.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberGreen
                                )
                            )
                            FilterChip(
                                selected = logFilter == "SUCCESS",
                                onClick = { logFilter = "SUCCESS" },
                                label = { Text("ناجحة (${logs.count { it.isSuccess }})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberGreen
                                )
                            )
                            FilterChip(
                                selected = logFilter == "FAILED",
                                onClick = { logFilter = "FAILED" },
                                label = { Text("فاشلة (${logs.count { !it.isSuccess }})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberRed.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberRed
                                )
                            )
                            FilterChip(
                                selected = logFilter == "CHANGE_MAC",
                                onClick = { logFilter = "CHANGE_MAC" },
                                label = { Text("تغيير MAC", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberGreen
                                )
                            )
                            FilterChip(
                                selected = logFilter == "RANDOM_MAC",
                                onClick = { logFilter = "RANDOM_MAC" },
                                label = { Text("العشوائية", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberGreen
                                )
                            )
                        }
                    }
                }
            }

            // FILTERED LOG ITEMS
            val filteredLogs = logs.filter { log ->
                val matchesFilter = when (logFilter) {
                    "SUCCESS" -> log.isSuccess
                    "FAILED" -> !log.isSuccess
                    "CHANGE_MAC" -> log.action.contains("CHANGE_MAC")
                    "RANDOM_MAC" -> log.action.contains("RANDOM_MAC")
                    else -> true
                }
                val matchesSearch = if (logSearchQuery.isBlank()) true else {
                    log.targetMac.contains(logSearchQuery, ignoreCase = true) ||
                    log.interfaceUsed.contains(logSearchQuery, ignoreCase = true) ||
                    log.message.contains(logSearchQuery, ignoreCase = true) ||
                    log.action.contains(logSearchQuery, ignoreCase = true)
                }
                matchesFilter && matchesSearch
            }

            if (filteredLogs.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = CyberTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد سجلات مطابقة للفلتر المحدد",
                                fontSize = 13.sp,
                                color = CyberTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { logItem ->
                    LogItemRow(
                        log = logItem,
                        onDelete = { viewModel.deleteMacManagerLog(logItem.id) }
                    )
                }
            }
        }
    }

    // LOG FILE VIEWER DIALOG
    if (showLogFileViewer) {
        AlertDialog(
            onDismissRequest = { showLogFileViewer = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = CyberGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ملف السجلات: mac_manager_logs.txt", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "مسار الملف: ${viewModel.getMacManagerLogFilePath()}",
                        fontSize = 11.sp,
                        color = CyberTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = CyberSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyberBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = logFileContent.ifEmpty { "الملف فارغ حالياً." },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CyberTextPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(logFileContent))
                        Toast.makeText(context, "تم نسخ محتوى الملف", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberDarkBg)
                ) {
                    Text("نسخ المحتوى", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogFileViewer = false }) {
                    Text("إغلاق", color = CyberTextSecondary)
                }
            },
            containerColor = CyberSurface
        )
    }
}

@Composable
fun LogItemRow(
    log: MacManagerLog,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        color = CyberSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (log.isSuccess) CyberGreen.copy(alpha = 0.3f) else CyberRed.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (log.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (log.isSuccess) CyberGreen else CyberRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (log.action) {
                            "CHANGE_MAC" -> "تغيير MAC"
                            "DISABLE_RANDOM_MAC" -> "تعطيل العشوائية"
                            "ENABLE_RANDOM_MAC" -> "تفعيل العشوائية"
                            else -> log.action
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (log.isSuccess) CyberGreen else CyberRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CyberSurface,
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        Text(
                            text = log.interfaceUsed,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.timestamp.formatTimestamp(),
                        fontSize = 10.sp,
                        color = CyberTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = CyberTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MAC: ${log.targetMac}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberTextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (log.isSuccess) CyberGreen.copy(alpha = 0.15f) else CyberRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (log.isSuccess) "SUCCESS" else "FAILED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (log.isSuccess) CyberGreen else CyberRed,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (log.message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.message,
                    fontSize = 11.sp,
                    color = CyberTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun Long.formatTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}
