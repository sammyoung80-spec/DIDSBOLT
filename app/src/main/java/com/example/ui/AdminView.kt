package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminView(viewModel: DidsBoltViewModel) {
    var activeTab by remember { mutableStateOf("users") } // "users", "codes", "config"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_screen_root")
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate950),
                        contentAlignment = Alignment.Center
                    ) {
                        LogoImage(modifier = Modifier.fillMaxSize())
                    }

                    Column {
                        Text(
                            text = "CONTROL DECK",
                            color = CyanAccent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "DIDSBOLT WP ADMINISTRATIVE INSTANCE",
                            color = Slate500,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = RubyRose.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, RubyRose.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("deauthorize_admin_button")
                ) {
                    Text("De-Authorize", color = RubyRose, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Slate900, RoundedCornerShape(12.dp))
                .border(width = 1.dp, color = Slate800, shape = RoundedCornerShape(12.dp))
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdminTabButton(
                icon = Icons.Default.People,
                label = "Users",
                active = activeTab == "users",
                onClick = { activeTab = "users" },
                tag = "admin_tab_users"
            )

            AdminTabButton(
                icon = Icons.Default.VpnKey,
                label = "Vouchers",
                active = activeTab == "codes",
                onClick = { activeTab = "codes" },
                tag = "admin_tab_vouchers"
            )

            AdminTabButton(
                icon = Icons.Default.Public,
                label = "Page Config",
                active = activeTab == "config",
                onClick = { activeTab = "config" },
                tag = "admin_tab_config"
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                "users" -> UsersTabContent(viewModel = viewModel)
                "codes" -> VouchersTabContent(viewModel = viewModel)
                "config" -> ConfigTabContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RowScope.AdminTabButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit, tag: String) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) CyanAccent else Color.Transparent)
            .padding(vertical = 10.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) CarbonBlack else Slate400,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = if (active) CarbonBlack else Slate400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UsersTabContent(viewModel: DidsBoltViewModel) {
    val usersList by viewModel.users.collectAsState()
    var usernameInput by remember { mutableStateOf("") }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_users_list_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(text = "Register Trader Instance", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            placeholder = { Text("@username or profile telegram handle", color = Slate505, fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Slate950,
                                unfocusedContainerColor = Slate950
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_add_user_input")
                        )

                        Button(
                            onClick = {
                                if (usernameInput.isNotBlank()) {
                                    viewModel.registerNewUser(usernameInput)
                                    usernameInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("admin_add_user_submit")
                        ) {
                            Text("Register", color = CarbonBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        editingUser?.let { user ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.5.dp, CyanAccent.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Inline Edit Profile Name", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = renameInput,
                                onValueChange = { renameInput = it },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Slate950,
                                    unfocusedContainerColor = Slate950
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("admin_rename_user_input")
                            )

                            IconButton(
                                onClick = {
                                    if (renameInput.isNotBlank()) {
                                        viewModel.updateUsername(user.id, renameInput)
                                        editingUser = null
                                    }
                                },
                                modifier = Modifier
                                    .background(EmeraldGreen, RoundedCornerShape(10.dp))
                                    .size(46.dp)
                                    .testTag("admin_rename_user_save")
                            ) {
                                Icon(Icons.Default.Check, "Save icon", tint = CarbonBlack)
                            }

                            IconButton(
                                onClick = { editingUser = null },
                                modifier = Modifier
                                    .background(Slate800, RoundedCornerShape(10.dp))
                                    .size(46.dp)
                            ) {
                                Icon(Icons.Default.Close, "Cancel edit icon", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Active Database Users",
                color = Slate500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        items(usersList) { u ->
            val isPro = u.accessExpiresAt != null && u.accessExpiresAt > System.currentTimeMillis()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_user_card_${u.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = u.username,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = {
                                        editingUser = u
                                        renameInput = u.username
                                    },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(Icons.Default.Edit, "Edit name dial symbol", tint = Slate500, modifier = Modifier.size(12.dp))
                                }
                            }
                            Text(
                                text = "UID: ${u.id}",
                                color = Slate500,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(if (isPro) SuccessGreenBg else Color.Transparent, RoundedCornerShape(4.dp))
                                    .border(
                                        width = 0.5.dp,
                                        color = if (isPro) EmeraldGreen else Slate700,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isPro) "PRO SUBSCRIPTION" else "NO ACCESS",
                                    color = if (isPro) EmeraldGreen else Slate400,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleUserActiveStatus(u.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (u.isActive) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                    contentDescription = "Active account toggles",
                                    tint = if (u.isActive) CyanAccent else Slate700,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.deleteUser(u.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete account profile",
                                    tint = Slate500,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 1.dp, color = Slate800.copy(alpha = 0.5f), shape = RoundedCornerShape(0.dp))
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.extendUserAccess(u.id, 1) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate950),
                            border = BorderStroke(0.5.dp, Slate800),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+1 Day", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.extendUserAccess(u.id, 7) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate950),
                            border = BorderStroke(0.5.dp, Slate800),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+7 Days", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.subtractUserAccess(u.id) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RubyRose.copy(alpha = 0.05f)),
                            border = BorderStroke(0.5.dp, RubyRose.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Revoke Sub", color = RubyRose, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VouchersTabContent(viewModel: DidsBoltViewModel) {
    val codesList by viewModel.vouchers.collectAsState()
    var selectedDays by remember { mutableIntStateOf(3) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_vouchers_list_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(text = "Setup Promo Voucher Access code", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Column {
                        Text(
                            text = "Voucher Access Grant Duration",
                            color = Slate500,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val daysOptions = listOf(1, 3, 7, 30)
                        var expanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate950, RoundedCornerShape(10.dp))
                                    .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                                    .clickable { expanded = true }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$selectedDays Days Full Premium",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, "Select day arrow drop down", tint = CyanAccent)
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(Slate900)
                                    .border(1.dp, Slate800)
                            ) {
                                daysOptions.forEach { days ->
                                    DropdownMenuItem(
                                        text = { Text("$days Days Full Premium", color = Color.White, fontSize = 13.sp) },
                                        onClick = {
                                            selectedDays = days
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.generatePromoVoucher(selectedDays) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("admin_generate_voucher_submit"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Generate Access Voucher Code", color = CarbonBlack, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "Active System Promo Vouchers",
                color = Slate500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        if (codesList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active vouchers generated.",
                        color = Slate500,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(codesList) { c ->
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
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = c.code,
                                    color = CyanAccent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(c.code))
                                        viewModel.showToast("Copied code: ${c.code}", ToastType.SUCCESS)
                                    },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, "Copy code elements", tint = Slate500, modifier = Modifier.size(12.dp))
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Days Added: ${c.days}",
                                    color = Slate500,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "•",
                                    color = Slate500,
                                    fontSize = 10.sp
                                )
                                if (c.isRedeemed) {
                                    Text(
                                        text = "Redeemed by ${c.redeemedBy}",
                                        color = AmberVibe,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = "Unused",
                                        color = EmeraldGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.revokePromoVoucher(c.code) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Purge promo voucher", tint = Slate500, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigTabContent(viewModel: DidsBoltViewModel) {
    val pricingPlans by viewModel.pricingPlans.collectAsState()
    val rawAssets by viewModel.assets.collectAsState()

    var announcementText by remember { mutableStateOf("") }
    val announcementState by viewModel.announcement.collectAsState()

    LaunchedEffect(announcementState) {
        announcementState?.let {
            announcementText = it.value
        }
    }

    val standardPairs = remember(rawAssets) { rawAssets.filter { !it.isOtc }.map { it.name } }
    val otcPairs = remember(rawAssets) { rawAssets.filter { it.isOtc }.map { it.name } }

    var editingPlanId by remember { mutableStateOf<Int?>(null) }
    var editPlanName by remember { mutableStateOf("") }
    var editPlanPrice by remember { mutableStateOf("") }
    var editPlanDays by remember { mutableStateOf("") }
    var editPlanDesc by remember { mutableStateOf("") }

    var newAssetTypeIsOtc by remember { mutableStateOf(false) }
    var newAssetNameInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_config_screen_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(text = "Top Announcement Banner", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = announcementText,
                        onValueChange = { announcementText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .testTag("admin_announcement_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950
                        )
                    )

                    Button(
                        onClick = { viewModel.saveAnnouncementConfig(announcementText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("admin_announcement_save"),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate950),
                        border = BorderStroke(1.dp, Slate700),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Save Announcement Banner", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(text = "Config Plan Pricing Tiers", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (editingPlanId != null) {
                        Column(
                            modifier = Modifier
                                .background(Slate950, RoundedCornerShape(10.dp))
                                .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "EDIT CONFIG: $editPlanName",
                                color = CyanAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )

                            OutlinedTextField(
                                value = editPlanName,
                                onValueChange = { editPlanName = it },
                                placeholder = { Text("Plan label name", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Slate900,
                                    unfocusedContainerColor = Slate900
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editPlanPrice,
                                    onValueChange = { editPlanPrice = it },
                                    placeholder = { Text("Price (e.g., $5.00)", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyanAccent,
                                        unfocusedBorderColor = Slate700,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Slate900,
                                        unfocusedContainerColor = Slate900
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                )

                                OutlinedTextField(
                                    value = editPlanDays,
                                    onValueChange = { editPlanDays = it },
                                    placeholder = { Text("Access Days", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyanAccent,
                                        unfocusedBorderColor = Slate700,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Slate900,
                                        unfocusedContainerColor = Slate900
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val idx = editingPlanId
                                        val daysInt = editPlanDays.toIntOrNull() ?: 1
                                        if (idx != null) {
                                            viewModel.editPricePlan(
                                                planId = idx,
                                                name = editPlanName,
                                                price = editPlanPrice,
                                                days = daysInt,
                                                desc = editPlanDesc
                                            )
                                            editingPlanId = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Save", color = CarbonBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { editingPlanId = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Cancel", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pricingPlans.forEach { plan ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Slate950, RoundedCornerShape(10.dp))
                                        .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(plan.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("${plan.price} • ${plan.days} Days Access", color = CyanAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }

                                    IconButton(
                                        onClick = {
                                            editingPlanId = plan.id
                                            editPlanName = plan.name
                                            editPlanPrice = plan.price
                                            editPlanDays = plan.days.toString()
                                            editPlanDesc = plan.desc
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, "Edit plan parameters", tint = Slate500, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(text = "Live Assets Configuration", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(
                        modifier = Modifier
                            .background(Slate950, RoundedCornerShape(10.dp))
                            .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate900, RoundedCornerShape(6.dp))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (!newAssetTypeIsOtc) CyanAccent else Color.Transparent)
                                    .clickable { newAssetTypeIsOtc = false }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Standard List", color = if (!newAssetTypeIsOtc) CarbonBlack else Slate500, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (newAssetTypeIsOtc) CyanAccent else Color.Transparent)
                                    .clickable { newAssetTypeIsOtc = true }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("OTC List", color = if (newAssetTypeIsOtc) CarbonBlack else Slate500, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newAssetNameInput,
                                onValueChange = { newAssetNameInput = it },
                                placeholder = { Text("e.g. EUR/USD", color = Slate505, fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Slate900,
                                    unfocusedContainerColor = Slate900
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            )

                            Button(
                                onClick = {
                                    if (newAssetNameInput.isNotBlank()) {
                                        viewModel.addCustomAsset(newAssetNameInput, newAssetTypeIsOtc)
                                        newAssetNameInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                modifier = Modifier.height(38.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("Add", color = CarbonBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Standard (${standardPairs.size})", color = Slate500, fontSize = 9.sp, fontWeight = FontWeight.Black)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .background(Slate950, RoundedCornerShape(8.dp))
                                    .border(width = 1.dp, color = Slate800, shape = RoundedCornerShape(8.dp))
                                    .padding(4.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(standardPairs) { asset ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = asset,
                                                color = Slate400,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )

                                            IconButton(
                                                onClick = { viewModel.removeCustomAsset(asset) },
                                                modifier = Modifier.size(14.dp)
                                            ) {
                                                Icon(Icons.Default.Close, "Delete asset from lists", tint = RubyRose, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("OTC (${otcPairs.size})", color = Slate500, fontSize = 9.sp, fontWeight = FontWeight.Black)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .background(Slate950, RoundedCornerShape(8.dp))
                                    .border(width = 1.dp, color = Slate800, shape = RoundedCornerShape(8.dp))
                                    .padding(4.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(otcPairs) { asset ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = asset,
                                                color = Slate400,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )

                                            IconButton(
                                                onClick = { viewModel.removeCustomAsset(asset) },
                                                modifier = Modifier.size(14.dp)
                                            ) {
                                                Icon(Icons.Default.Close, "Delete asset from lists", tint = RubyRose, modifier = Modifier.size(10.dp))
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
}
