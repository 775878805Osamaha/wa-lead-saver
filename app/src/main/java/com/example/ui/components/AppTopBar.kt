package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandGreenGradientVertical
import com.example.ui.theme.ColorBrandGreen
import com.example.ui.theme.ColorCardBackground
import com.example.ui.theme.ColorCardBorder
import com.example.ui.theme.ColorHeaderEnd
import com.example.ui.theme.ColorHeaderStart
import com.example.ui.theme.ColorTextPrimary
import com.example.ui.theme.ColorTextSecondary
import com.example.ui.theme.HeaderGradient

@Composable
fun AppTopBar(
    isActive: Boolean,
    onStatusClick: () -> Unit,
    onCameraClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onWhatsAppClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLoginClick: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderGradient)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right side (RTL Start): App title and status + WhatsApp Logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // WhatsApp Brand Icon Badge with soft green glow shadow
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    ambientColor = Color(0x502EE671),
                                    spotColor = Color(0x702EE671)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandGreenGradientVertical)
                                .border(1.dp, Color(0x332EE671), RoundedCornerShape(12.dp))
                                .clickable { onWhatsAppClick() }
                                .testTag("top_bar_whatsapp_icon"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "WhatsApp",
                                tint = Color(0xFF0A0D0B),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // App Title & Dynamic Status
                        Column(
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "WA Lead Saver",
                                color = ColorTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onStatusClick() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isActive) ColorBrandGreen else Color(0xFFE6A032))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isActive) "يراقب واتساب الآن" else "المراقبة متوقفة",
                                    color = if (isActive) ColorBrandGreen else ColorTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Left side (RTL End): Action buttons (Search & Settings)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Button in rounded card style
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorCardBackground)
                                .border(1.dp, ColorCardBorder, RoundedCornerShape(12.dp))
                                .clickable { onSearchClick() }
                                .testTag("top_bar_search_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = ColorTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Settings Button in rounded card style
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorCardBackground)
                                .border(1.dp, ColorCardBorder, RoundedCornerShape(12.dp))
                                .clickable { onSettingsClick() }
                                .testTag("top_bar_settings_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات",
                                tint = ColorTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
