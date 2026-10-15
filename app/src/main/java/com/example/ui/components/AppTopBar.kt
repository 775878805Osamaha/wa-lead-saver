package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrmBorder
import com.example.ui.theme.CrmCardElevated
import com.example.ui.theme.CrmDanger
import com.example.ui.theme.CrmHeader
import com.example.ui.theme.CrmPrimary
import com.example.ui.theme.CrmTextPrimary
import com.example.ui.theme.CrmTextSecondary

@Composable
fun AppTopBar(
    isActive: Boolean,
    onStatusClick: () -> Unit,
    onCameraClick: () -> Unit,
    onExportClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onLoginClick: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CrmHeader,
        shadowElevation = 2.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            val isNarrow = maxWidth < 370.dp
            val actionBtnSize = if (isNarrow) 32.dp else 34.dp
            val actionIconSize = if (isNarrow) 17.dp else 19.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. App Icon
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x2EFFFFFF))
                        .border(1.dp, Color(0x3DFFFFFF), RoundedCornerShape(8.dp))
                        .testTag("app_logo_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = "App Logo",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 2. Title & Subtitle with Status Pill
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WA Lead Saver",
                            color = Color.White,
                            fontSize = if (isNarrow) 14.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Status indicator pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isActive) Color(0x3310B981) else Color(0x33EF4444))
                                .clickable { onStatusClick() }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                .testTag("status_indicator_pill"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) Color(0xFF34D399) else Color(0xFFF87171))
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isActive) "نشط" else "متوقف",
                                color = if (isActive) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "الالتقاط الذكي لأرقام العملاء وواتساب",
                        color = Color(0xFFD1E7DD),
                        fontSize = if (isNarrow) 9.5.sp else 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. Compact Action Icons on the right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // Search
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .size(actionBtnSize)
                            .testTag("topbar_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(actionIconSize)
                        )
                    }

                    // Share / Export CSV
                    IconButton(
                        onClick = onExportClick,
                        modifier = Modifier
                            .size(actionBtnSize)
                            .testTag("topbar_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export History",
                            tint = Color.White,
                            modifier = Modifier.size(actionIconSize)
                        )
                    }

                    // Snap & Save Camera OCR
                    IconButton(
                        onClick = onCameraClick,
                        modifier = Modifier
                            .size(actionBtnSize)
                            .testTag("topbar_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Snap and Save",
                            tint = Color.White,
                            modifier = Modifier.size(actionIconSize)
                        )
                    }

                    // WhatsApp Quick Launcher
                    IconButton(
                        onClick = onWhatsAppClick,
                        modifier = Modifier
                            .size(actionBtnSize)
                            .testTag("topbar_whatsapp_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Open WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(actionIconSize)
                        )
                    }

                    // Settings
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(actionBtnSize)
                            .testTag("topbar_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(actionIconSize)
                        )
                    }

                    // Auth login/logout
                    if (onLogoutClick != null) {
                        IconButton(
                            onClick = onLogoutClick,
                            modifier = Modifier
                                .size(actionBtnSize)
                                .testTag("topbar_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(actionIconSize)
                            )
                        }
                    } else if (onLoginClick != null) {
                        IconButton(
                            onClick = onLoginClick,
                            modifier = Modifier
                                .size(actionBtnSize)
                                .testTag("topbar_login_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Login",
                                tint = Color.White,
                                modifier = Modifier.size(actionIconSize)
                            )
                        }
                    }
                }
            }
        }
    }
}
