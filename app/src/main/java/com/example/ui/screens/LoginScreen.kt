package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.auth.AuthState
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import com.example.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

    var accountOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var loginMethod by remember { mutableStateOf(LoginMethod.ACCOUNT_NUMBER) }

    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("login_screen"),
        color = Color(0xFFF8F9FA)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Brand Header Icon
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(WhatsAppGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "App Logo",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = WhatsAppTeal
                )

                Text(
                    text = stringResource(R.string.auth_welcome_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Login Card Form
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.auth_login_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Login Method Switcher (Account Number vs Email)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                .padding(4.dp)
                                .testTag("login_method_selector"),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { loginMethod = LoginMethod.ACCOUNT_NUMBER },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("tab_account_number"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (loginMethod == LoginMethod.ACCOUNT_NUMBER) WhatsAppTeal else Color.Transparent,
                                    contentColor = if (loginMethod == LoginMethod.ACCOUNT_NUMBER) Color.White else Color(0xFF64748B)
                                ),
                                elevation = if (loginMethod == LoginMethod.ACCOUNT_NUMBER) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.auth_tab_account_number),
                                    fontSize = 13.sp,
                                    fontWeight = if (loginMethod == LoginMethod.ACCOUNT_NUMBER) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            Button(
                                onClick = { loginMethod = LoginMethod.EMAIL },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("tab_email"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (loginMethod == LoginMethod.EMAIL) WhatsAppTeal else Color.Transparent,
                                    contentColor = if (loginMethod == LoginMethod.EMAIL) Color.White else Color(0xFF64748B)
                                ),
                                elevation = if (loginMethod == LoginMethod.EMAIL) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.auth_tab_email),
                                    fontSize = 13.sp,
                                    fontWeight = if (loginMethod == LoginMethod.EMAIL) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic Input Field (Account Number or Email)
                        val inputLabel = if (loginMethod == LoginMethod.ACCOUNT_NUMBER) {
                            stringResource(R.string.auth_account_or_email_label)
                        } else {
                            stringResource(R.string.auth_email_label)
                        }
                        val inputPlaceholder = if (loginMethod == LoginMethod.ACCOUNT_NUMBER) {
                            stringResource(R.string.auth_account_placeholder)
                        } else {
                            stringResource(R.string.auth_email_placeholder)
                        }
                        val inputIcon = if (loginMethod == LoginMethod.ACCOUNT_NUMBER) {
                            Icons.Default.Badge
                        } else {
                            Icons.Default.Email
                        }
                        val inputKeyboardType = if (loginMethod == LoginMethod.ACCOUNT_NUMBER) {
                            KeyboardType.Text
                        } else {
                            KeyboardType.Email
                        }

                        OutlinedTextField(
                            value = accountOrEmail,
                            onValueChange = { accountOrEmail = it },
                            label = { Text(inputLabel) },
                            placeholder = { Text(inputPlaceholder) },
                            leadingIcon = {
                                Icon(inputIcon, contentDescription = null, tint = WhatsAppTeal)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = inputKeyboardType,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WhatsAppTeal,
                                focusedLabelColor = WhatsAppTeal
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_number_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Input
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.auth_password_label)) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppTeal)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    authViewModel.login(accountOrEmail, password, rememberMe)
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WhatsAppTeal,
                                focusedLabelColor = WhatsAppTeal
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Remember Me Checkbox
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = WhatsAppGreen),
                                modifier = Modifier.testTag("remember_me_checkbox")
                            )
                            Text(
                                text = stringResource(R.string.auth_remember_me),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF334155)
                            )
                        }

                        // Error message display
                        if (!errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFB91C1C),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }

                        // Account status messages if blocked/expired/device limit
                        when (val state = authState) {
                            is AuthState.AccountInactive -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                StatusAlertCard(
                                    title = stringResource(R.string.auth_account_inactive_title),
                                    message = state.message,
                                    bgColor = Color(0xFFFEF3C7),
                                    textColor = Color(0xFFB45309)
                                )
                            }
                            is AuthState.AccountExpired -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                StatusAlertCard(
                                    title = stringResource(R.string.auth_account_expired_title),
                                    message = state.message,
                                    bgColor = Color(0xFFFEE2E2),
                                    textColor = Color(0xFFB91C1C)
                                )
                            }
                            is AuthState.DeviceLimitExceeded -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                StatusAlertCard(
                                    title = stringResource(R.string.auth_device_limit_title),
                                    message = state.message,
                                    bgColor = Color(0xFFFEF3C7),
                                    textColor = Color(0xFFB45309)
                                )
                            }
                            else -> {}
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Login Button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                authViewModel.login(accountOrEmail, password, rememberMe)
                            },
                            enabled = !isLoading && accountOrEmail.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppGreen,
                                disabledContainerColor = Color(0xFFCBD5E1)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.auth_login_button),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusAlertCard(title: String, message: String, bgColor: Color, textColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, color = textColor, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = message, color = textColor, style = MaterialTheme.typography.bodySmall)
        }
    }
}

enum class LoginMethod {
    ACCOUNT_NUMBER,
    EMAIL
}
