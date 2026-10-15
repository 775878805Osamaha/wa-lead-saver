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

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    reasonMessage: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by authViewModel.successMessage.collectAsStateWithLifecycle()
    val unconfirmedEmail by authViewModel.unconfirmedEmail.collectAsStateWithLifecycle()
    val registrationSuccessMessage by authViewModel.registrationSuccessMessage.collectAsStateWithLifecycle()
    val registrationErrorMessage by authViewModel.registrationErrorMessage.collectAsStateWithLifecycle()
    val recoverySuccessMessage by authViewModel.recoverySuccessMessage.collectAsStateWithLifecycle()
    val recoveryErrorMessage by authViewModel.recoveryErrorMessage.collectAsStateWithLifecycle()

    var isRegisterMode by remember { mutableStateOf(false) }
    var isRecoveryMode by remember { mutableStateOf(false) }

    // Login state
    var accountOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var loginMethod by remember { mutableStateOf(LoginMethod.ACCOUNT_NUMBER) }

    // Recovery state
    var recoveryEmail by remember { mutableStateOf("") }
    var recoveryEmailSent by remember { mutableStateOf(false) }

    // Register state
    var regFullName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regValidationErr by remember { mutableStateOf<String?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current

    androidx.activity.compose.BackHandler(enabled = isRecoveryMode) {
        isRecoveryMode = false
        recoveryEmailSent = false
        authViewModel.clearRecoveryMessages()
    }

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
                // Dismiss / Continue as Guest button if called from inside the app
                if (onDismiss != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("login_back_dismiss_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = WhatsAppTeal
                            )
                        }
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("login_guest_continue_btn")
                        ) {
                            Text(
                                text = stringResource(R.string.auth_continue_as_guest),
                                color = WhatsAppTeal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

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

                if (!reasonMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .testTag("login_reason_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reasonMessage,
                                color = Color(0xFF92400E),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

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
                            text = when {
                                isRecoveryMode -> stringResource(R.string.auth_recovery_title)
                                isRegisterMode -> stringResource(R.string.auth_register_title)
                                else -> stringResource(R.string.auth_login_title)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isRecoveryMode) {
                            // Dedicated "استعادة كلمة المرور" Form View
                            Text(
                                text = stringResource(R.string.auth_recovery_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                textAlign = TextAlign.Start
                            )

                            // Email input for password recovery
                            OutlinedTextField(
                                value = recoveryEmail,
                                onValueChange = {
                                    recoveryEmail = it
                                    if (recoveryErrorMessage != null) {
                                        authViewModel.clearRecoveryMessages()
                                    }
                                },
                                label = { Text(stringResource(R.string.auth_email_label)) },
                                placeholder = { Text(stringResource(R.string.auth_email_placeholder)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = WhatsAppTeal)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                        if (recoveryEmail.isNotBlank()) {
                                            authViewModel.sendPasswordRecoveryEmail(recoveryEmail) {
                                                recoveryEmailSent = true
                                            }
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WhatsAppTeal,
                                    focusedLabelColor = WhatsAppTeal
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("recovery_email_input")
                            )

                            // Error message banner
                            if (!recoveryErrorMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("recovery_error_card"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                                ) {
                                    Text(
                                        text = recoveryErrorMessage!!,
                                        color = Color(0xFFB91C1C),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }

                            // Success message banner
                            if (!recoverySuccessMessage.isNullOrBlank() || recoveryEmailSent) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("recovery_success_card"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.auth_recovery_email_sent_title),
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D),
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = recoverySuccessMessage ?: stringResource(R.string.auth_recovery_email_sent_desc),
                                                color = Color(0xFF166534),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Send Recovery Link Button: "إرسال رابط الاستعادة"
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    authViewModel.sendPasswordRecoveryEmail(recoveryEmail) {
                                        recoveryEmailSent = true
                                    }
                                },
                                enabled = !isLoading && recoveryEmail.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("send_recovery_link_button"),
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
                                        text = stringResource(R.string.auth_recovery_send_button),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Back-to-login action
                            TextButton(
                                onClick = {
                                    keyboardController?.hide()
                                    isRecoveryMode = false
                                    recoveryEmailSent = false
                                    authViewModel.clearRecoveryMessages()
                                },
                                modifier = Modifier.testTag("recovery_back_to_login_btn")
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_recovery_back_to_login),
                                    color = WhatsAppTeal,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        } else if (isRegisterMode) {
                            // Register Form Fields
                            OutlinedTextField(
                                value = regFullName,
                                onValueChange = { regFullName = it },
                                label = { Text(stringResource(R.string.auth_register_name_label)) },
                                placeholder = { Text("مثال: متجر السعادة أو أحمد علي") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = WhatsAppTeal)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WhatsAppTeal,
                                    focusedLabelColor = WhatsAppTeal
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_name_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = { regEmail = it },
                                label = { Text(stringResource(R.string.auth_email_label)) },
                                placeholder = { Text(stringResource(R.string.auth_email_placeholder)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = WhatsAppTeal)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WhatsAppTeal,
                                    focusedLabelColor = WhatsAppTeal
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_email_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                label = { Text(stringResource(R.string.auth_password_label)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppTeal)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                        Icon(
                                            imageVector = if (regPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WhatsAppTeal,
                                    focusedLabelColor = WhatsAppTeal
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_password_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = regConfirmPassword,
                                onValueChange = { regConfirmPassword = it },
                                label = { Text(stringResource(R.string.auth_register_confirm_password_label)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppTeal)
                                },
                                singleLine = true,
                                visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WhatsAppTeal,
                                    focusedLabelColor = WhatsAppTeal
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_confirm_password_input")
                            )
                        } else {
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

                            // "نسيت كلمة المرور؟" clickable Arabic text link
                            // Placed directly below password field and above remember-me checkbox
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        keyboardController?.hide()
                                        // Prefill recovery email if user already typed an email
                                        if (loginMethod == LoginMethod.EMAIL && accountOrEmail.contains("@")) {
                                            recoveryEmail = accountOrEmail.trim()
                                        }
                                        authViewModel.clearMessages()
                                        isRecoveryMode = true
                                    },
                                    modifier = Modifier.testTag("forgot_password_link")
                                ) {
                                    Text(
                                        text = stringResource(R.string.auth_forgot_password),
                                        color = WhatsAppTeal,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

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
                        }

                        // Registration mode messages
                        if (isRegisterMode) {
                            val activeRegError = regValidationErr ?: registrationErrorMessage
                            if (!activeRegError.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("register_error_card"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                                ) {
                                    Text(
                                        text = activeRegError,
                                        color = Color(0xFFB91C1C),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        } else {
                            // Login mode messages:
                            // 1. Show registration success banner if user just registered
                            if (!registrationSuccessMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("registration_success_card"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                                ) {
                                    Text(
                                        text = registrationSuccessMessage!!,
                                        color = Color(0xFF15803D),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }

                            // 2. Email confirmation required banner with resend button
                            if (!unconfirmedEmail.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("unconfirmed_email_card"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "تأكيد البريد الإلكتروني مطلوب",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "يجب تأكيد البريد الإلكتروني أولًا. تحقق من بريدك الإلكتروني.",
                                            color = Color(0xFF92400E),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                authViewModel.resendConfirmationEmail(unconfirmedEmail ?: accountOrEmail)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("resend_confirmation_button"),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB45309))
                                        ) {
                                            Text(text = "إعادة إرسال رسالة التأكيد", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            } else if (!errorMessage.isNullOrBlank()) {
                                // 3. Standard Login error (invalid credentials, network error, etc.)
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("login_error_card"),
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

                            // 4. Other success messages (e.g. resend confirmation email success)
                            if (!successMessage.isNullOrBlank() && registrationSuccessMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                                ) {
                                    Text(
                                        text = successMessage!!,
                                        color = Color(0xFF15803D),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }

                        if (!isRecoveryMode) {
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

                            if (isRegisterMode) {
                                // Register Button
                                Button(
                                    onClick = {
                                        keyboardController?.hide()
                                        regValidationErr = null
                                    if (regPassword != regConfirmPassword) {
                                        regValidationErr = "كلمتا المرور غير متطابقتين"
                                        return@Button
                                    }
                                    if (regPassword.length < 6) {
                                        regValidationErr = "يجب ألا تقل كلمة المرور عن 6 أحرف"
                                        return@Button
                                    }
                                    authViewModel.registerUser(regFullName, regEmail, regPassword) {
                                        isRegisterMode = false
                                        loginMethod = LoginMethod.EMAIL
                                        accountOrEmail = regEmail
                                        regFullName = ""
                                        regPassword = ""
                                        regConfirmPassword = ""
                                    }
                                },
                                enabled = !isLoading && regFullName.isNotBlank() && regEmail.isNotBlank() && regPassword.isNotBlank() && regConfirmPassword.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("register_button"),
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
                                        text = stringResource(R.string.auth_register_title),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Switch between Login and Register
                        TextButton(
                            onClick = {
                                isRegisterMode = !isRegisterMode
                                regValidationErr = null
                                authViewModel.clearMessages()
                            },
                            modifier = Modifier.testTag("toggle_register_mode_btn")
                        ) {
                            Text(
                                text = if (isRegisterMode) stringResource(R.string.auth_login_title) else stringResource(R.string.auth_register_prompt),
                                color = WhatsAppTeal,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
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
