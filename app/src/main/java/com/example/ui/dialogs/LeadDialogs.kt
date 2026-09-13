package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.database.entity.LeadEntity
import com.example.data.model.LeadSource
import com.example.data.model.LeadStatus
import com.example.ui.components.LeadStatusBadge
import com.example.ui.theme.RemoveRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLeadDialog(
    onDismiss: () -> Unit,
    onConfirm: (phone: String, name: String, source: LeadSource, status: LeadStatus, notes: String, tags: String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(LeadSource.MANUAL) }
    var selectedStatus by remember { mutableStateOf(LeadStatus.NEW) }
    var notes by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_lead),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = RemoveRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.phone_number_label)) },
                    placeholder = { Text("e.g. +966501234567") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_lead_phone")
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.display_name_label)) },
                    placeholder = { Text("e.g. أومكس أو أحمد") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_lead_name")
                )

                // Source Dropdown
                ExposedDropdownMenuBox(
                    expanded = sourceDropdownExpanded,
                    onExpandedChange = { sourceDropdownExpanded = !sourceDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = stringResource(selectedSource.titleRes),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.source_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_lead_source")
                    )
                    ExposedDropdownMenu(
                        expanded = sourceDropdownExpanded,
                        onDismissRequest = { sourceDropdownExpanded = false }
                    ) {
                        LeadSource.entries.forEach { src ->
                            DropdownMenuItem(
                                text = { Text(stringResource(src.titleRes)) },
                                onClick = {
                                    selectedSource = src
                                    sourceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = stringResource(selectedStatus.titleRes),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.status_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_lead_status")
                    )
                    ExposedDropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        LeadStatus.entries.forEach { st ->
                            DropdownMenuItem(
                                text = { LeadStatusBadge(status = st) },
                                onClick = {
                                    selectedStatus = st
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_label)) },
                    placeholder = { Text(stringResource(R.string.notes_hint)) },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_lead_notes")
                )

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(stringResource(R.string.tags_label)) },
                    placeholder = { Text("VIP, مهتم, عميل جديد") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_lead_tags")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phone.isBlank()) {
                        errorMessage = "رقم الهاتف مطلوب"
                        return@Button
                    }
                    onConfirm(phone, name, selectedSource, selectedStatus, notes, tags)
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                modifier = Modifier.testTag("button_confirm_add_lead")
            ) {
                Text(stringResource(R.string.add_lead))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_cancel_add_lead")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun EditLeadDialog(
    lead: LeadEntity,
    onDismiss: () -> Unit,
    onConfirm: (updatedLead: LeadEntity) -> Unit
) {
    var name by remember { mutableStateOf(lead.effectiveName) }
    var phone by remember { mutableStateOf(lead.phoneNumber) }
    var notes by remember { mutableStateOf(lead.notes) }
    var tags by remember { mutableStateOf(lead.tags) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit_lead),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = RemoveRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.display_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_lead_name")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.phone_number_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_lead_phone")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_label)) },
                    placeholder = { Text(stringResource(R.string.notes_hint)) },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_lead_notes")
                )

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(stringResource(R.string.tags_label)) },
                    placeholder = { Text("VIP, مهتم, متابعة") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_lead_tags")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phone.isBlank()) {
                        errorMessage = "رقم الهاتف مطلوب"
                        return@Button
                    }
                    val updated = lead.copy(
                        displayName = name.trim(),
                        contactName = name.trim(),
                        phoneNumber = phone.trim(),
                        notes = notes.trim(),
                        tags = tags.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                    onConfirm(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                modifier = Modifier.testTag("button_confirm_edit_lead")
            ) {
                Text(stringResource(R.string.save_changes))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_cancel_edit_lead")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ChangeStatusDialog(
    currentStatus: LeadStatus,
    onDismiss: () -> Unit,
    onSelectStatus: (LeadStatus) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.change_status),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LeadStatus.entries.forEach { status ->
                    val isSelected = status == currentStatus
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) WhatsAppTeal else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectStatus(status)
                            }
                            .testTag("status_option_${status.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LeadStatusBadge(status = status)
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = WhatsAppTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_cancel_change_status")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTagDialog(
    currentTags: String,
    onDismiss: () -> Unit,
    onConfirm: (newTags: String) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    val existingList = remember {
        currentTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    }

    val quickTags = listOf(
        stringResource(R.string.tag_new_customer),
        stringResource(R.string.tag_interested),
        stringResource(R.string.tag_price_request),
        stringResource(R.string.tag_product_request),
        stringResource(R.string.tag_follow_up),
        stringResource(R.string.tag_vip)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_tag),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text(stringResource(R.string.tags_label)) },
                    placeholder = { Text(stringResource(R.string.tags_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_single_tag")
                )

                Text(
                    text = stringResource(R.string.quick_tags),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTags.forEach { quickTag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clickable {
                                    if (!existingList.contains(quickTag)) {
                                        existingList.add(quickTag)
                                    }
                                    onConfirm(existingList.joinToString(", "))
                                }
                                .testTag("quick_tag_$quickTag")
                        ) {
                            Text(
                                text = "+ $quickTag",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tagInput.isNotBlank()) {
                        val parts = tagInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        for (p in parts) {
                            if (!existingList.contains(p)) {
                                existingList.add(p)
                            }
                        }
                    }
                    onConfirm(existingList.joinToString(", "))
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                modifier = Modifier.testTag("button_confirm_add_tag")
            ) {
                Text(stringResource(R.string.add_tag))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_cancel_add_tag")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun EditNoteDialog(
    initialNote: String,
    onDismiss: () -> Unit,
    onConfirm: (note: String) -> Unit
) {
    var noteText by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialNote.isBlank()) stringResource(R.string.add_note) else stringResource(R.string.edit_note),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(stringResource(R.string.notes_label)) },
                placeholder = { Text(stringResource(R.string.notes_hint)) },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_note_text")
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(noteText.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                modifier = Modifier.testTag("button_confirm_note")
            ) {
                Text(stringResource(R.string.save_changes))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_cancel_note")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun DeleteLeadConfirmDialog(
    leadName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.delete_lead),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "${stringResource(R.string.delete_lead_confirm)}\n($leadName)",
                fontSize = 14.sp,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RemoveRed),
                modifier = Modifier.testTag("button_confirm_delete_lead")
            ) {
                Text(stringResource(R.string.delete_lead))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_cancel_delete_lead")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
