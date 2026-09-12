package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.auth.AccountStatus
import com.example.data.auth.CustomerAccount
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal

@Composable
fun EditAccountDialog(
    account: CustomerAccount,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, expiresAt: String?, maxDevices: Int, status: AccountStatus) -> Unit
) {
    var customerName by remember { mutableStateOf(account.customerName) }
    var email by remember { mutableStateOf(account.email) }
    var expiresAt by remember { mutableStateOf(account.expiresAt.orEmpty()) }
    var maxDevicesStr by remember { mutableStateOf(account.maxDevices.toString()) }
    var status by remember { mutableStateOf(account.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${stringResource(R.string.admin_edit_account_title)}: ${account.accountNumber}",
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
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.admin_customer_email_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = expiresAt,
                        onValueChange = { expiresAt = it },
                        label = { Text(stringResource(R.string.admin_expiration_label)) },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )

                    OutlinedTextField(
                        value = maxDevicesStr,
                        onValueChange = { maxDevicesStr = it },
                        label = { Text(stringResource(R.string.admin_max_devices_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.admin_account_status_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = status == AccountStatus.ACTIVE,
                        onClick = { status = AccountStatus.ACTIVE },
                        colors = RadioButtonDefaults.colors(selectedColor = WhatsAppGreen)
                    )
                    Text(text = stringResource(R.string.active), style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(8.dp))

                    RadioButton(
                        selected = status == AccountStatus.INACTIVE,
                        onClick = { status = AccountStatus.INACTIVE },
                        colors = RadioButtonDefaults.colors(selectedColor = Color.Red)
                    )
                    Text(text = stringResource(R.string.admin_status_inactive), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val maxDev = maxDevicesStr.toIntOrNull() ?: 1
                    onSave(customerName, email, expiresAt.takeIf { it.isNotBlank() }, maxDev, status)
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                modifier = Modifier.testTag("admin_save_edit_account_button")
            ) {
                Text(stringResource(R.string.confirm), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
