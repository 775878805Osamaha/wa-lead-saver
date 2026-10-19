package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.auth.AuthState
import com.example.ui.theme.BrandGreenGradient
import com.example.ui.theme.BrandGreenGradientVertical
import com.example.ui.theme.ColorAccentBlue
import com.example.ui.theme.ColorAccentBlueTranslucent
import com.example.ui.theme.ColorAlertAmber
import com.example.ui.theme.ColorAlertAmberTranslucent
import com.example.ui.theme.ColorBrandGreen
import com.example.ui.theme.ColorBrandGreenTranslucent
import com.example.ui.theme.ColorCardBackground
import com.example.ui.theme.ColorCardBorder
import com.example.ui.theme.ColorDangerRed
import com.example.ui.theme.ColorDangerRedTranslucent
import com.example.ui.theme.ColorScreenBackground
import com.example.ui.theme.ColorTextMuted
import com.example.ui.theme.ColorTextPrimary
import com.example.ui.theme.ColorTextSecondary
import com.example.viewmodel.AuthViewModel

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
    var phoneOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var loginMethod by remember { mutableStateOf(LoginMethod.PHONE) }

    // Recovery state
    var recoveryEmail by remember { mutableStateOf("") }
    var recoveryEmailSent by remember { mutableStateOf(false) }

    // Register state
    var regFullName by remember { mutableStateOf("") }
    var regPhoneOrEmail by remember { mutableStateOf("") }
    var regMethod by remember { mutableStateOf(LoginMethod.PHONE) }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regValidationErr by remember { mutableStateOf<String?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-dismiss and transition to Main Screen immediately upon successful authentication
    androidx.compose.runtime.LaunchedEffect(authState) {
        if (authState is AuthState.AuthenticatedUser || authState is AuthState.AuthenticatedAdmin) {
            onDismiss?.invoke()
        }
    }

    androidx.activity.compose.BackHandler(enabled = isRecoveryMode) {
        isRecoveryMode = false
        recoveryEmailSent = false
        authViewModel.clearRecoveryMessages()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("login_screen"),
            color = ColorScreenBackground
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Dismiss / Continue as Guest
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
                                    tint = ColorTextSecondary
                                )
                            }
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("login_guest_continue_btn")
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_continue_as_guest),
                                    color = ColorBrandGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Brand Header Logo Circle with Green Gradient and Glowing Shadow (box-shadow: 0 0 26px rgba(46,230,113,0.28))
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                ambientColor = Color(0x472EE671),
                                spotColor = Color(0x472EE671)
                            )
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x472EE671), Color(0x152EE671), Color.Transparent),
                                        radius = size.maxDimension * 0.95f
                                    ),
                                    radius = size.maxDimension * 0.95f
                                )
                            }
                            .clip(CircleShape)
                            .background(BrandGreenGradientVertical)
                            .border(1.dp, Color(0x332EE671), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "App Logo",
                            tint = Color(0xFF06130A),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.auth_welcome_subtitle),
                        fontSize = 13.sp,
                        color = ColorTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 18.dp)
                    )

                    // Amber Alert Banner if reasonMessage exists (rgba(230,160,50,0.08) with 30% border)
                    if (!reasonMessage.isNullOrBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .testTag("login_reason_banner"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x14E6A032)),
                            border = BorderStroke(1.dp, Color(0x4DE6A032))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = ColorAlertAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = reasonMessage,
                                    color = ColorAlertAmber,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Main Form Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorCardBackground),
                        border = BorderStroke(1.dp, ColorCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = when {
                                    isRecoveryMode -> stringResource(R.string.auth_recovery_title)
                                    isRegisterMode -> stringResource(R.string.auth_register_title)
                                    else -> stringResource(R.string.auth_login_title)
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorTextPrimary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isRecoveryMode) {
                                // Password Recovery
                                Text(
                                    text = stringResource(R.string.auth_recovery_subtitle),
                                    fontSize = 13.sp,
                                    color = ColorTextSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    textAlign = TextAlign.Start
                                )

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
                                        Icon(Icons.Default.Email, contentDescription = null, tint = ColorBrandGreen)
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
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("recovery_email_input")
                                )

                                if (!recoveryErrorMessage.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    AuthErrorCard(recoveryErrorMessage!!)
                                }

                                if (!recoverySuccessMessage.isNullOrBlank() || recoveryEmailSent) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    AuthSuccessCard(recoverySuccessMessage ?: stringResource(R.string.auth_recovery_email_sent_desc))
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Send recovery button
                                val isRecoveryValid = !isLoading && recoveryEmail.isNotBlank()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .shadow(
                                            elevation = if (isRecoveryValid) 8.dp else 0.dp,
                                            shape = RoundedCornerShape(12.dp),
                                            ambientColor = Color(0x402EE671),
                                            spotColor = Color(0x402EE671)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isRecoveryValid) Brush.horizontalGradient(listOf(Color(0xFF2EE671), Color(0xFF16A34A)))
                                            else Brush.linearGradient(listOf(Color(0xFF1F271F), Color(0xFF1F271F)))
                                        )
                                        .clickable(enabled = isRecoveryValid) {
                                            keyboardController?.hide()
                                            authViewModel.sendPasswordRecoveryEmail(recoveryEmail) {
                                                recoveryEmailSent = true
                                            }
                                        }
                                        .testTag("send_recovery_link_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF06130A), strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            text = stringResource(R.string.auth_recovery_send_button),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRecoveryValid) Color(0xFF06130A) else Color(0xFF7A8580)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

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
                                        color = ColorBrandGreen,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            } else if (isRegisterMode) {
                                // Register Form
                                OutlinedTextField(
                                    value = regFullName,
                                    onValueChange = { regFullName = it },
                                    label = { Text(stringResource(R.string.auth_register_name_label)) },
                                    placeholder = { Text("مثال: متجر السعادة أو أحمد علي") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = ColorBrandGreen)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("reg_name_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Register Method Selector (Phone vs Email) - Fixed 50%/50% Width
                                val regTabGradient = Brush.horizontalGradient(listOf(Color(0xFF2EE671), Color(0xFF16A34A)))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0E100E), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFF1F271F), RoundedCornerShape(10.dp))
                                        .padding(4.dp)
                                        .testTag("reg_method_selector"),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f, fill = true)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (regMethod == LoginMethod.PHONE) regTabGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                                            .clickable { regMethod = LoginMethod.PHONE }
                                            .testTag("reg_tab_phone"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = if (regMethod == LoginMethod.PHONE) Color(0xFF06130A) else Color(0xFF7A8580),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.auth_tab_phone),
                                                fontSize = 12.sp,
                                                fontWeight = if (regMethod == LoginMethod.PHONE) FontWeight.Bold else FontWeight.Medium,
                                                color = if (regMethod == LoginMethod.PHONE) Color(0xFF06130A) else Color(0xFF7A8580)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f, fill = true)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (regMethod == LoginMethod.EMAIL) regTabGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                                            .clickable { regMethod = LoginMethod.EMAIL }
                                            .testTag("reg_tab_email"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = null,
                                                tint = if (regMethod == LoginMethod.EMAIL) Color(0xFF06130A) else Color(0xFF7A8580),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.auth_tab_email),
                                                fontSize = 12.sp,
                                                fontWeight = if (regMethod == LoginMethod.EMAIL) FontWeight.Bold else FontWeight.Medium,
                                                color = if (regMethod == LoginMethod.EMAIL) Color(0xFF06130A) else Color(0xFF7A8580)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val regInputLabel = if (regMethod == LoginMethod.PHONE) stringResource(R.string.auth_phone_label) else stringResource(R.string.auth_email_label)
                                val regInputPlaceholder = if (regMethod == LoginMethod.PHONE) stringResource(R.string.auth_phone_placeholder) else stringResource(R.string.auth_email_placeholder)
                                val regInputIcon = if (regMethod == LoginMethod.PHONE) Icons.Default.Phone else Icons.Default.Email
                                val regInputKeyboard = if (regMethod == LoginMethod.PHONE) KeyboardType.Phone else KeyboardType.Email

                                OutlinedTextField(
                                    value = regPhoneOrEmail,
                                    onValueChange = { regPhoneOrEmail = it },
                                    label = { Text(regInputLabel) },
                                    placeholder = { Text(regInputPlaceholder) },
                                    leadingIcon = {
                                        Icon(regInputIcon, contentDescription = null, tint = ColorBrandGreen)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = regInputKeyboard, imeAction = ImeAction.Next),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag(if (regMethod == LoginMethod.PHONE) "reg_phone_input" else "reg_email_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = regPassword,
                                    onValueChange = { regPassword = it },
                                    label = { Text(stringResource(R.string.auth_password_label)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = ColorBrandGreen)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                            Icon(
                                                imageVector = if (regPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = ColorTextSecondary
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("reg_password_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = regConfirmPassword,
                                    onValueChange = { regConfirmPassword = it },
                                    label = { Text(stringResource(R.string.auth_register_confirm_password_label)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = ColorBrandGreen)
                                    },
                                    singleLine = true,
                                    visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("reg_confirm_password_input")
                                )

                                val activeRegError = regValidationErr ?: registrationErrorMessage
                                if (!activeRegError.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    AuthErrorCard(activeRegError)
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Register Action Button (Full width green gradient with shadow box-shadow: 0 6px 18px rgba(46,230,113,0.25))
                                val isRegValid = !isLoading && regFullName.isNotBlank() && regPhoneOrEmail.isNotBlank() && regPassword.isNotBlank()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .shadow(
                                            elevation = if (isRegValid) 8.dp else 0.dp,
                                            shape = RoundedCornerShape(12.dp),
                                            ambientColor = Color(0x402EE671),
                                            spotColor = Color(0x402EE671)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isRegValid) Brush.horizontalGradient(listOf(Color(0xFF2EE671), Color(0xFF16A34A)))
                                            else Brush.linearGradient(listOf(Color(0xFF1F271F), Color(0xFF1F271F)))
                                        )
                                        .clickable(enabled = isRegValid) {
                                            keyboardController?.hide()
                                            regValidationErr = null
                                            if (regPassword != regConfirmPassword) {
                                                regValidationErr = "كلمتا المرور غير متطابقتين"
                                                return@clickable
                                            }
                                            if (regPassword.length < 6) {
                                                regValidationErr = "يجب ألا تقل كلمة المرور عن 6 أحرف"
                                                return@clickable
                                            }
                                            authViewModel.registerUser(regFullName, regPhoneOrEmail, regPassword) {
                                                isRegisterMode = false
                                                loginMethod = regMethod
                                                phoneOrEmail = regPhoneOrEmail
                                                regFullName = ""
                                                regPhoneOrEmail = ""
                                                regPassword = ""
                                                regConfirmPassword = ""
                                            }
                                        }
                                        .testTag("register_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF06130A), strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            text = stringResource(R.string.auth_register_title),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRegValid) Color(0xFF06130A) else Color(0xFF7A8580)
                                        )
                                    }
                                }
                            } else {
                                // Login Mode Selector (Phone Number vs Email) - Fixed 50%/50% Width
                                val loginTabGradient = Brush.horizontalGradient(listOf(Color(0xFF2EE671), Color(0xFF16A34A)))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0E100E), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFF1F271F), RoundedCornerShape(10.dp))
                                        .padding(4.dp)
                                        .testTag("login_method_selector"),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f, fill = true)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (loginMethod == LoginMethod.PHONE) loginTabGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                                            .clickable { loginMethod = LoginMethod.PHONE }
                                            .testTag("tab_phone"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = if (loginMethod == LoginMethod.PHONE) Color(0xFF06130A) else Color(0xFF7A8580),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.auth_tab_phone),
                                                fontSize = 12.sp,
                                                fontWeight = if (loginMethod == LoginMethod.PHONE) FontWeight.Bold else FontWeight.Medium,
                                                color = if (loginMethod == LoginMethod.PHONE) Color(0xFF06130A) else Color(0xFF7A8580)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f, fill = true)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (loginMethod == LoginMethod.EMAIL) loginTabGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                                            .clickable { loginMethod = LoginMethod.EMAIL }
                                            .testTag("tab_email"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = null,
                                                tint = if (loginMethod == LoginMethod.EMAIL) Color(0xFF06130A) else Color(0xFF7A8580),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.auth_tab_email),
                                                fontSize = 12.sp,
                                                fontWeight = if (loginMethod == LoginMethod.EMAIL) FontWeight.Bold else FontWeight.Medium,
                                                color = if (loginMethod == LoginMethod.EMAIL) Color(0xFF06130A) else Color(0xFF7A8580)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                val inputLabel = if (loginMethod == LoginMethod.PHONE) {
                                    stringResource(R.string.auth_phone_label)
                                } else {
                                    stringResource(R.string.auth_email_label)
                                }
                                val inputPlaceholder = if (loginMethod == LoginMethod.PHONE) {
                                    stringResource(R.string.auth_phone_placeholder)
                                } else {
                                    stringResource(R.string.auth_email_placeholder)
                                }
                                val inputIcon = if (loginMethod == LoginMethod.PHONE) Icons.Default.Phone else Icons.Default.Email
                                val inputKeyboardType = if (loginMethod == LoginMethod.PHONE) KeyboardType.Phone else KeyboardType.Email

                                OutlinedTextField(
                                    value = phoneOrEmail,
                                    onValueChange = { phoneOrEmail = it },
                                    label = { Text(inputLabel) },
                                    placeholder = { Text(inputPlaceholder) },
                                    leadingIcon = {
                                        Icon(inputIcon, contentDescription = null, tint = ColorBrandGreen)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = inputKeyboardType, imeAction = ImeAction.Next),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag(if (loginMethod == LoginMethod.PHONE) "phone_input" else "email_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text(stringResource(R.string.auth_password_label)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = ColorBrandGreen)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = ColorTextSecondary
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            keyboardController?.hide()
                                            authViewModel.login(phoneOrEmail, password, rememberMe)
                                        }
                                    ),
                                    colors = authTextFieldColors(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("password_input")
                                )

                                // Forgot Password Link
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = stringResource(R.string.auth_forgot_password),
                                        color = ColorBrandGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .clickable {
                                                keyboardController?.hide()
                                                if (loginMethod == LoginMethod.EMAIL && phoneOrEmail.contains("@")) {
                                                    recoveryEmail = phoneOrEmail.trim()
                                                }
                                                authViewModel.clearMessages()
                                                isRecoveryMode = true
                                            }
                                            .testTag("forgot_password_link")
                                            .padding(vertical = 4.dp)
                                    )
                                }

                                // Remember Me
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = rememberMe,
                                        onCheckedChange = { rememberMe = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = ColorBrandGreen,
                                            checkmarkColor = Color(0xFF0C1512),
                                            uncheckedColor = ColorCardBorder
                                        ),
                                        modifier = Modifier.testTag("remember_me_checkbox")
                                    )
                                    Text(
                                        text = stringResource(R.string.auth_remember_me),
                                        color = ColorTextSecondary,
                                        fontSize = 13.sp
                                    )
                                }

                                if (!errorMessage.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AuthErrorCard(errorMessage!!)
                                }

                                if (!registrationSuccessMessage.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AuthSuccessCard(registrationSuccessMessage!!)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Full Width Login Button (Green gradient with shadow box-shadow: 0 6px 18px rgba(46,230,113,0.25))
                                val isLoginValid = !isLoading && phoneOrEmail.isNotBlank() && password.isNotBlank()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .shadow(
                                            elevation = if (isLoginValid) 8.dp else 0.dp,
                                            shape = RoundedCornerShape(12.dp),
                                            ambientColor = Color(0x402EE671),
                                            spotColor = Color(0x402EE671)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isLoginValid) Brush.horizontalGradient(listOf(Color(0xFF2EE671), Color(0xFF16A34A)))
                                            else Brush.linearGradient(listOf(Color(0xFF1F271F), Color(0xFF1F271F)))
                                        )
                                        .clickable(enabled = isLoginValid) {
                                            keyboardController?.hide()
                                            authViewModel.login(phoneOrEmail, password, rememberMe)
                                        }
                                        .testTag("login_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF06130A), strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            text = stringResource(R.string.auth_login_button),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLoginValid) Color(0xFF06130A) else Color(0xFF7A8580)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Switch between Login and Register in a single line
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        isRegisterMode = !isRegisterMode
                                        regValidationErr = null
                                        authViewModel.clearMessages()
                                    }
                                    .testTag("toggle_register_mode_btn")
                                    .padding(vertical = 6.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isRegisterMode) "لديك حساب بالفعل؟ " else "ليس لديك حساب؟ ",
                                    color = Color(0xFF7A8580),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                Text(
                                    text = if (isRegisterMode) "تسجيل الدخول" else "إنشاء حساب جديد",
                                    color = Color(0xFF2EE671),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
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
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF2EE671),
    unfocusedBorderColor = Color(0xFF1F271F),
    focusedContainerColor = Color(0xFF0E100E),
    unfocusedContainerColor = Color(0xFF0E100E),
    focusedTextColor = ColorTextPrimary,
    unfocusedTextColor = ColorTextPrimary,
    focusedLabelColor = Color(0xFF2EE671),
    unfocusedLabelColor = Color(0xFF7A8580),
    cursorColor = Color(0xFF2EE671)
)

@Composable
private fun AuthErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("auth_error_card"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ColorDangerRedTranslucent),
        border = BorderStroke(1.dp, ColorDangerRed)
    ) {
        Text(
            text = message,
            color = ColorDangerRed,
            fontSize = 12.sp,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun AuthSuccessCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("auth_success_card"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ColorBrandGreenTranslucent),
        border = BorderStroke(1.dp, ColorBrandGreen)
    ) {
        Text(
            text = message,
            color = ColorBrandGreen,
            fontSize = 12.sp,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
fun StatusAlertCard(title: String, message: String, bgColor: Color, textColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x14E6A032)),
        border = BorderStroke(1.dp, Color(0x4DE6A032))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, color = ColorAlertAmber, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = message, color = ColorAlertAmber, fontSize = 12.sp)
        }
    }
}

enum class LoginMethod {
    PHONE,
    EMAIL
}
