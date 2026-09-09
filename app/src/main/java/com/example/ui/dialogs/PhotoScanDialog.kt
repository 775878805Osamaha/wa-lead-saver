package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.CameraScannerView
import com.example.ui.theme.CardBackground
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import com.example.viewmodel.ScannedNumberItem

@Composable
fun PhotoScanDialog(
    scannedNumbers: List<ScannedNumberItem>,
    isProcessing: Boolean,
    onProcessText: (String) -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onAddToQueue: () -> Unit,
    onDismiss: () -> Unit,
    startWithCamera: Boolean = true
) {
    val context = LocalContext.current
    var isLiveCameraActive by remember { mutableStateOf(startWithCamera) }
    var showTextInput by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onProcessText(
                "Business Card Scan:\nCustomer Service: +9677784763381\nSales Dept: +9677770786095\nSupport Desk: +9677735525053\nMobile: 771542389\nWhatsApp Hotline: 0779988771\nOffice: +967771239841"
            )
        }
    }

    val selectedCount = scannedNumbers.count { it.isSelected }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x2600A884)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = WhatsAppTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Snap & Save",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Extract contacts from camera or photo",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Live Camera
                Button(
                    onClick = { isLiveCameraActive = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("button_take_photo"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan with Camera (CameraX)",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Button 2: Gallery Picker
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("button_choose_gallery"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, WhatsAppTeal)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = WhatsAppTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Choose from Gallery",
                        color = WhatsAppTeal,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Button 3: Sample scan numbers
                Button(
                    onClick = {
                        onProcessText(
                            "Sample Document:\nSales: +9677784763381\nSupport: +9677770786095\nManager: +9677735525053\nAccounts: 771542389\nReception: 0779988771\nDispatch: +967771239841"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("button_try_sample_scan"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E242B))
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = WhatsAppGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Try Sample Card Numbers (6)",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Button 4: Toggle Manual Text Input
                TextButton(
                    onClick = { showTextInput = !showTextInput },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = WhatsAppTeal
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showTextInput) "Hide Text Input" else "Paste / Enter Text Directly",
                        color = WhatsAppTeal,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showTextInput) {
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = {
                            Text(
                                text = "Paste numbers or chat text here...",
                                color = TextSecondary
                            )
                        }
                    )

                    Button(
                        onClick = {
                            if (manualText.isNotBlank()) {
                                onProcessText(manualText)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal)
                    ) {
                        Text(
                            text = "Extract Numbers",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Processing Indicator
                if (isProcessing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = WhatsAppTeal,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Scanning for phone numbers...",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                // Scanned numbers list
                if (scannedNumbers.isNotEmpty()) {
                    val selectableItems = scannedNumbers.filter { !it.alreadyInContacts }
                    val allSelected = selectableItems.isNotEmpty() && selectableItems.all { it.isSelected }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected Numbers (${scannedNumbers.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )

                        TextButton(
                            onClick = { onSelectAll(!allSelected) },
                            modifier = Modifier.testTag("button_select_all_scanned")
                        ) {
                            Text(
                                text = if (allSelected) "Deselect All" else "Select All",
                                fontSize = 12.sp,
                                color = WhatsAppTeal
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(scannedNumbers, key = { it.phoneNumber }) { item ->
                            ScannedNumberRow(
                                item = item,
                                onToggle = { onToggleSelect(item.phoneNumber) },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Phone Number", item.phoneNumber)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied ${item.phoneNumber}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAddToQueue,
                enabled = selectedCount > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                modifier = Modifier.testTag("button_add_selected_to_queue")
            ) {
                Text("Add ($selectedCount) to Queue")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_dismiss_scan_dialog")
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBackground
    )

    // Full screen live camera dialog
    if (isLiveCameraActive) {
        Dialog(
            onDismissRequest = { isLiveCameraActive = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CameraScannerView(
                onPhotoCaptured = { capturedText ->
                    isLiveCameraActive = false
                    onProcessText(capturedText)
                },
                onOpenGallery = {
                    isLiveCameraActive = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClose = { isLiveCameraActive = false }
            )
        }
    }
}

@Composable
fun ScannedNumberRow(
    item: ScannedNumberItem,
    onToggle: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !item.alreadyInContacts) {
                if (!item.alreadyInContacts) {
                    onToggle()
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF7F8FA)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
                enabled = !item.alreadyInContacts,
                colors = CheckboxDefaults.colors(
                    checkedColor = WhatsAppTeal,
                    checkmarkColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.phoneNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (item.alreadyInContacts) TextSecondary else TextPrimary
                )
                if (item.alreadyInContacts) {
                    Text(
                        text = "Already in contacts",
                        fontSize = 11.sp,
                        color = WhatsAppGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy number",
                    modifier = Modifier.size(17.dp),
                    tint = TextSecondary
                )
            }
        }
    }
}
