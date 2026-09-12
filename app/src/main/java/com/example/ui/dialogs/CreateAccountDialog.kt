package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal

@Composable
fun CreateAccountDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, email: String, password: String, expiresAt: String?, maxDevices: Int) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var expiresAt by remember { mutableStateOf("2027-01-01") }
    var maxDevicesStr by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.admin_create_account_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WhatsAppTeal
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text(stringResource(R.string.admin_customer_name_label)) },
                    placeholder = { Text("متجر أومكس / Ahmed Store") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_new_customer_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.admin_customer_email_label)) },
                    placeholder = { Text("customer@example.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_new_customer_email")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.admin_customer_password_label)) },
                    placeholder = { Text("********") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_new_customer_password")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = expiresAt,
                        onValueChange = { expiresAt = it },
                        label = { Text(stringResource(R.string.admin_expiration_label)) },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("admin_new_customer_expiry")
                    )

                    OutlinedTextField(
                        value = maxDevicesStr,
                        onValueChange = { maxDevicesStr = it },
                        label = { Text(stringResource(R.string.admin_max_devices_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_new_customer_max_devices")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val maxDev = maxDevicesStr.toIntOrNull() ?: 1
                    onCreate(customerName, email, password, expiresAt.takeIf { it.isNotBlank() }, maxDev)
                },
                enabled = customerName.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                modifier = Modifier.testTag("admin_confirm_create_account_button")
            ) {
                Text(stringResource(R.string.admin_create_button), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun AccountCreatedSuccessDialog(
    accountNumber: String,
    email: String,
    passwordMaskedOrPlain: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val copyCredentials = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(
            "Account Credentials",
            "Account Number: $accountNumber\nEmail: $email\nPassword: $passwordMaskedOrPlain"
        )
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.admin_account_created_success_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WhatsAppTeal
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.admin_account_created_success_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "رقم الحساب: ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = accountNumber,
                                fontWeight = FontWeight.ExtraBold,
                                color = WhatsAppTeal,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "البريد الإلكتروني: ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = email, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "كلمة المرور: ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = passwordMaskedOrPlain, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = copyCredentials,
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                modifier = Modifier.testTag("admin_copy_credentials_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.copy_phone_number), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
