package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContactPhone
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkTealHeader
import com.example.ui.theme.DarkTealHeaderDarker
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.WhatsAppGreen

@Composable
fun AppTopBar(
    isActive: Boolean,
    onStatusClick: () -> Unit,
    onCameraClick: () -> Unit,
    onExportClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkTealHeader
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkTealHeaderDarker)
                        .testTag("app_logo_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = "App Logo",
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "WA Lead Saver",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkTealHeaderDarker)
                            .clickable { onStatusClick() }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("status_indicator_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isActive) StatusActiveGreen else Color(0xFFE53935))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isActive) "Active" else "Listener Disabled",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onCameraClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("topbar_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Snap and Save",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onExportClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("topbar_export_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export History",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("topbar_whatsapp_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Open WhatsApp",
                        tint = WhatsAppGreen
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("topbar_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
