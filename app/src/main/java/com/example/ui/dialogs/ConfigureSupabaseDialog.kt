package com.example.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.auth.SupabaseConfig
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal

@Composable
fun ConfigureSupabaseDialog(
    initialUrl: String = "",
    initialAnonKey: String = "",
    onDismiss: () -> Unit,
    onSave: (url: String, anonKey: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var url by remember { mutableStateOf(initialUrl) }
    var anonKey by remember { mutableStateOf(initialAnonKey) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (url.isBlank()) {
            url = SupabaseConfig.getUrl(context)
        }
        if (anonKey.isBlank()) {
            anonKey = SupabaseConfig.getAnonKey(context)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.auth_server_settings_title),
                style = MaterialTheme.typography.titleMedium,
                color = WhatsAppTeal
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.auth_server_settings_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Supabase URL") },
                    placeholder = { Text("https://xyz.supabase.co") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_url_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = anonKey,
                    onValueChange = { anonKey = it },
                    label = { Text("Supabase Anon Public Key") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_anon_key_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(url, anonKey)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_server_config_button")
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
