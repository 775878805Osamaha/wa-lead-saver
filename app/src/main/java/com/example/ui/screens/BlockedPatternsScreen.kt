package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.database.entity.BlockedPatternEntity
import com.example.ui.theme.AppBackground
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkTealHeader
import com.example.ui.theme.RemoveRed
import com.example.ui.theme.RemoveRedLight
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppTeal
import com.example.util.BlockedPatternHelper

@Composable
fun BlockedPatternsScreen(
    blockedPatterns: List<BlockedPatternEntity>,
    onAddPattern: (pattern: String, matchType: String, label: String) -> Unit,
    onTogglePattern: (id: Long, isEnabled: Boolean) -> Unit,
    onDeletePattern: (pattern: BlockedPatternEntity) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var testNumberInput by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<BlockedPatternEntity?>(null) }
    var hasTested by remember { mutableStateOf(false) }

    val activeCount = blockedPatterns.count { it.isEnabled }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkTealHeader)
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("btn_back_from_blocked_patterns")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_to_settings),
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.blocked_patterns_header_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.blocked_patterns_header_sub),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                modifier = Modifier.testTag("btn_open_add_blocked_pattern")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = DarkTealHeader,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.add_pattern),
                    color = DarkTealHeader,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Stats & Informational Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = WhatsAppTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.protection_banner_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.protection_banner_sub, activeCount, blockedPatterns.size),
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.protection_banner_desc),
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Quick Presets
            item {
                Text(
                    text = stringResource(R.string.quick_add_presets),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                val internalLineLabel = stringResource(R.string.preset_internal_line)
                val tollFreeLabel = stringResource(R.string.preset_toll_free)
                val shortcodeLabel = stringResource(R.string.preset_shortcode)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickPresetChip(
                        label = "+96770 ($internalLineLabel)",
                        onClick = {
                            onAddPattern("+96770", BlockedPatternEntity.MATCH_STARTS_WITH, internalLineLabel)
                        }
                    )
                    QuickPresetChip(
                        label = "0800 ($tollFreeLabel)",
                        onClick = {
                            onAddPattern("0800", BlockedPatternEntity.MATCH_STARTS_WITH, tollFreeLabel)
                        }
                    )
                    QuickPresetChip(
                        label = "1234 ($shortcodeLabel)",
                        onClick = {
                            onAddPattern("1234", BlockedPatternEntity.MATCH_STARTS_WITH, shortcodeLabel)
                        }
                    )
                }
            }

            // Interactive Pattern Tester Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAF9)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = WhatsAppTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.test_phone_number_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.test_phone_number_sub),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = testNumberInput,
                                onValueChange = {
                                    testNumberInput = it
                                    if (it.isNotBlank()) {
                                        val active = blockedPatterns.filter { p -> p.isEnabled }
                                        testResult = BlockedPatternHelper.findMatchingPattern(it, active)
                                        hasTested = true
                                    } else {
                                        hasTested = false
                                        testResult = null
                                    }
                                },
                                placeholder = { Text("+96770123456", fontSize = 13.sp, color = TextMuted) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_test_phone_number")
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val active = blockedPatterns.filter { it.isEnabled }
                                    testResult = BlockedPatternHelper.findMatchingPattern(testNumberInput, active)
                                    hasTested = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                                modifier = Modifier.testTag("btn_test_phone_number")
                            ) {
                                Text(stringResource(R.string.test_btn), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        AnimatedVisibility(visible = hasTested && testNumberInput.isNotBlank()) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                if (testResult != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFFEBEE))
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = RemoveRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val patternVal = testResult?.pattern.orEmpty()
                                        val matchVal = testResult?.matchType.orEmpty()
                                        val labelVal = testResult?.label.orEmpty().ifBlank { "-" }
                                        Text(
                                            text = stringResource(R.string.number_blocked_verdict, patternVal, matchVal, labelVal),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RemoveRed
                                        )
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE8F5E9))
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = StatusActiveGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.number_allowed_verdict),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1B5E20)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: Patterns List Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.configured_patterns_count, blockedPatterns.size),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Empty State
            if (blockedPatterns.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF0F4F3))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.empty_patterns_title),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.empty_patterns_desc),
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                                modifier = Modifier.testTag("btn_empty_add_pattern")
                            ) {
                                Text(stringResource(R.string.add_first_pattern))
                            }
                        }
                    }
                }
            } else {
                items(blockedPatterns, key = { it.id }) { pattern ->
                    BlockedPatternCard(
                        item = pattern,
                        onToggle = { isEnabled -> onTogglePattern(pattern.id, isEnabled) },
                        onDelete = { onDeletePattern(pattern) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAddDialog) {
        AddBlockedPatternDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { pattern, matchType, label ->
                onAddPattern(pattern, matchType, label)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun BlockedPatternCard(
    item: BlockedPatternEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_blocked_pattern_${item.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Match Type Pill
                    val (badgeBg, badgeColor, badgeText) = when (item.matchType) {
                        BlockedPatternEntity.MATCH_STARTS_WITH -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), stringResource(R.string.pattern_type_starts_with).uppercase())
                        BlockedPatternEntity.MATCH_CONTAINS -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), stringResource(R.string.pattern_type_contains).uppercase())
                        BlockedPatternEntity.MATCH_EXACT -> Triple(Color(0xFFF3E8FF), Color(0xFF7E22CE), stringResource(R.string.pattern_type_exact).uppercase())
                        BlockedPatternEntity.MATCH_REGEX -> Triple(Color(0xFFFFEDD5), Color(0xFFC2410C), "REGEX")
                        else -> Triple(Color(0xFFE2E8F0), Color(0xFF475569), item.matchType)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    if (item.label.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.pattern,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (item.isEnabled) TextPrimary else TextMuted
                )

                Text(
                    text = if (item.isEnabled) stringResource(R.string.rule_active_desc) else stringResource(R.string.rule_paused_desc),
                    fontSize = 11.sp,
                    color = if (item.isEnabled) StatusActiveGreen else TextMuted
                )
            }

            Switch(
                checked = item.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = WhatsAppTeal,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.testTag("switch_pattern_${item.id}")
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("btn_delete_pattern_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = RemoveRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun QuickPresetChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, WhatsAppTeal.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = WhatsAppTeal,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = WhatsAppTeal
            )
        }
    }
}

@Composable
fun AddBlockedPatternDialog(
    onDismiss: () -> Unit,
    onConfirm: (pattern: String, matchType: String, label: String) -> Unit
) {
    var patternInput by remember { mutableStateOf("") }
    var selectedMatchType by remember { mutableStateOf(BlockedPatternEntity.MATCH_STARTS_WITH) }
    var labelInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_add_pattern_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.dialog_add_pattern_desc),
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = patternInput,
                    onValueChange = {
                        patternInput = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.pattern_input_label)) },
                    placeholder = { Text(stringResource(R.string.dialog_pattern_hint)) },
                    singleLine = true,
                    isError = errorMessage != null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_dialog_pattern")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 11.sp,
                        color = RemoveRed
                    )
                }

                Text(
                    text = stringResource(R.string.dialog_match_type),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )

                // Match Type Selection Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedMatchType == BlockedPatternEntity.MATCH_STARTS_WITH,
                        onClick = { selectedMatchType = BlockedPatternEntity.MATCH_STARTS_WITH },
                        label = { Text(stringResource(R.string.pattern_type_starts_with), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedMatchType == BlockedPatternEntity.MATCH_CONTAINS,
                        onClick = { selectedMatchType = BlockedPatternEntity.MATCH_CONTAINS },
                        label = { Text(stringResource(R.string.pattern_type_contains), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedMatchType == BlockedPatternEntity.MATCH_EXACT,
                        onClick = { selectedMatchType = BlockedPatternEntity.MATCH_EXACT },
                        label = { Text(stringResource(R.string.pattern_type_exact), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Helper explanation for selected type
                val hint = when (selectedMatchType) {
                    BlockedPatternEntity.MATCH_STARTS_WITH -> stringResource(R.string.hint_starts_with)
                    BlockedPatternEntity.MATCH_CONTAINS -> stringResource(R.string.hint_contains)
                    BlockedPatternEntity.MATCH_EXACT -> stringResource(R.string.hint_exact)
                    else -> ""
                }
                Text(text = hint, fontSize = 11.sp, color = TextMuted)

                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text(stringResource(R.string.pattern_label_input)) },
                    placeholder = { Text(stringResource(R.string.dialog_label_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_dialog_label")
                )
            }
        },
        confirmButton = {
            val emptyErrorMsg = stringResource(R.string.dialog_error_empty)
            Button(
                onClick = {
                    if (patternInput.trim().isBlank()) {
                        errorMessage = emptyErrorMsg
                    } else {
                        onConfirm(patternInput.trim(), selectedMatchType, labelInput.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_add_pattern")
            ) {
                Text(stringResource(R.string.dialog_save_rule))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondary)
            }
        }
    )
}

