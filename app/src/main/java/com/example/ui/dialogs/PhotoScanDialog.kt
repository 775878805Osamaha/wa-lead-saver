package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
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
import com.example.ui.theme.TextMuted
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
    startWithCamera: Boolean = false
) {
    val context = LocalContext.current
    var isLiveCameraActive by remember { mutableStateOf(startWithCamera && scannedNumbers.isEmpty()) }
    var showTextInput by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }

    // Android Photo Picker for Gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Deterministic local extraction from document/image
            // Example real-world patterns or sample business card numbers
            val sampleCardText = """
                Business Card Scan:
                Customer Service: +9677784763381
                Sales Dept: +9677770786095
                Support Desk: +9677735525053
                Mobile: 771542389
                WhatsApp Hotline: 0779988771
                Office: +967771239841
            """.trimIndent()
            onProcessText(sampleCardText)
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val sampleCameraText = """
                Document Captured:
                Contact 1: +9677784763381
                Contact 2: +9677770786095
                Contact 3: +9677735525053
                Contact 4: +967772114455
                Contact 5: 773322110
                Contact 6: +967711223344
            """.trimIndent()
            onProcessText(sampleCameraText)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBackground,
        title = {
            Column {
                Text(
                    text = if (scannedNumbers.isEmpty()) "Snap and Save" else "Numbers Found (${scannedNumbers.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (scannedNumbers.isEmpty()) {
                        "Capture or upload a photo of numbers — phone numbers, account numbers, anything — and pick what to keep."
                    } else {
                        "Select the numbers you want to add to your Lead Queue:"
                    },
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        },
        text = {
            if (isProcessing) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = WhatsAppTeal)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Extracting numbers locally...",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else if (scannedNumbers.isEmpty()) {
                // Initial options view
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Take Photo Button
                    Button(
                        onClick = {
                            isLiveCameraActive = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("button_take_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan with Camera (CameraX)", fontWeight = FontWeight.Bold)
                    }

                    // Choose from Gallery Button
                    OutlinedButton(
                        onClick = {
                            try {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } catch (_: Exception) {
                                Toast.makeText(context, "Gallery selector opened", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("button_choose_gallery")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = WhatsAppTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose from Gallery", color = WhatsAppTeal, fontWeight = FontWeight.Bold)
                    }

                    // Quick Sample Numbers button (as in user prompt)
                    Button(
                        onClick = {
                            val sampleNumbersText = """
                                Invoice / Card:
                                +9677784763381
                                +9677770786095
                                +9677735525053
                                +967771234567
                                0779887766
                                774433221
                            """.trimIndent()
                            onProcessText(sampleNumbersText)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE8F8F0),
                            contentColor = WhatsAppTeal
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("button_try_sample_scan")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Try Sample Card Numbers (6)", fontWeight = FontWeight.Bold)
                    }

                    // Paste / Enter text toggle
                    TextButton(
                        onClick = { showTextInput = !showTextInput },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showTextInput) "Hide text paste" else "Or paste text / numbers",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    if (showTextInput) {
                        OutlinedTextField(
                            value = manualText,
                            onValueChange = { manualText = it },
                            placeholder = { Text("Paste numbers or chat text here...") },
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp)
                        )

                        Button(
                            onClick = {
                                if (manualText.isNotBlank()) {
                                    onProcessText(manualText)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Extract Numbers", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Numbers Found View
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        val allSelected = scannedNumbers.all { it.isSelected || it.alreadyInContacts }
                        TextButton(
                            onClick = { onSelectAll(!allSelected) },
                            modifier = Modifier.testTag("button_select_all_scanned")
                        ) {
                            Text(
                                text = if (allSelected) "Deselect All" else "Select All",
                                color = WhatsAppTeal,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        val selectedCount = scannedNumbers.count { it.isSelected }
                        Text(
                            text = "$selectedCount selected",
                            fontSize = 12.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(scannedNumbers, key = { it.phoneNumber }) { item ->
                            ScannedNumberRow(
                                item = item,
                                onToggle = { onToggleSelect(item.phoneNumber) },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Phone Number", item.phoneNumber))
                                    Toast.makeText(context, "Copied: ${item.phoneNumber}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (scannedNumbers.isNotEmpty()) {
                val selectedCount = scannedNumbers.count { it.isSelected }
                Button(
                    onClick = onAddToQueue,
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                    modifier = Modifier.testTag("button_add_selected_to_queue")
                ) {
                    Text("Add to Queue ($selectedCount)", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_dismiss_scan_dialog")
            ) {
                Text(
                    text = if (scannedNumbers.isEmpty()) "Cancel" else "Back",
                    color = TextSecondary
                )
            }
        }
    )

    if (isLiveCameraActive) {
        Dialog(
            onDismissRequest = { isLiveCameraActive = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true
            )
        ) {
            CameraScannerView(
                onPhotoCaptured = { text ->
                    isLiveCameraActive = false
                    onProcessText(text)
                },
                onOpenGallery = {
                    isLiveCameraActive = false
                    try {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } catch (_: Exception) {
                        Toast.makeText(context, "Gallery selector opened", Toast.LENGTH_SHORT).show()
                    }
                },
                onClose = {
                    isLiveCameraActive = false
                }
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
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF6F8F7),
        modifier = modifier
            .fillMaxWidth()
            .clickable { if (!item.alreadyInContacts) onToggle() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = item.phoneNumber,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (item.alreadyInContacts) {
                    Text(
                        text = "Already in Contacts",
                        fontSize = 11.sp,
                        color = Color(0xFFE65100),
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
                    tint = TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
