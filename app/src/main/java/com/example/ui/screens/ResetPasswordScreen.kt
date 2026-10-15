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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.auth.PasswordResetResult
import com.example.data.auth.RecoverySession
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import com.example.viewmodel.AuthViewModel

/**
 * Screen for entering and saving a new password following a Supabase recovery deep link.
 * Built with RTL layout, Arabic strings, and Material 3 design system.
 */
@Composable
fun ResetPasswordScreen(
    authViewModel: AuthViewModel,
    recoverySession: RecoverySession?,
    onResetComplete: () -> Unit,
    onCancel: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var localValidationErr by remember { mutableStateOf<String?>(null) }

    val resetResult by authViewModel.passwordResetResult.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    val isLoading = resetResult is PasswordResetResult.Loading
    val isSuccess = resetResult is PasswordResetResult.Success
    val serverErrorMessage = (resetResult as? PasswordResetResult.Error)?.message

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            // Give user a brief moment to see success or navigate directly
            kotlinx.coroutines.delay(1200L)
            onResetComplete()
        }
    }

    androidx.activity.compose.BackHandler {
        onCancel()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reset_password_screen"),
        color = Color(0xFFF4F7FA)
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
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F8EF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = WhatsAppTeal,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.auth_reset_screen_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172B3A)
                )

                Text(
                    text = stringResource(R.string.auth_reset_screen_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF718096),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // White Card Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_password_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (recoverySession == null || !recoverySession.isValid) {
                            // Invalid or missing session alert
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("invalid_session_card"),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEC))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "جلسة الاستعادة غير صالحة أو منتهية",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB91C1C),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "يبدو أن الرابط المستخدم غير صالح أو انتهت صلاحيته. يُرجى طلب رابط استعادة جديد من شاشة تسجيل الدخول.",
                                        color = Color(0xFF991B1B),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = onCancel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("reset_return_to_login_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal)
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_recovery_back_to_login),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else if (isSuccess) {
                            // Success state
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_success_card"),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F8EF))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF087F68),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.auth_password_reset_success),
                                        color = Color(0xFF065F46),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = onResetComplete,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("reset_success_continue_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_recovery_back_to_login),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            // New Password Field
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = {
                                    newPassword = it
                                    localValidationErr = null
                                },
                                label = { Text(stringResource(R.string.auth_new_password_label)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppTeal)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isNewPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                                    .testTag("new_password_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Confirm New Password Field
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    localValidationErr = null
                                },
                                label = { Text(stringResource(R.string.auth_confirm_new_password_label)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppTeal)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle confirm password visibility"
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                        if (newPassword.isNotBlank() && confirmPassword.isNotBlank() && !isLoading) {
                                            if (newPassword != confirmPassword) {
                                                localValidationErr = "كلمتا المرور غير متطابقتين"
                                            } else if (newPassword.length < 6) {
                                                localValidationErr = "يجب ألا تقل كلمة المرور عن 6 أحرف"
                                            } else {
                                                authViewModel.resetPassword(newPassword, confirmPassword)
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
                                    .testTag("confirm_new_password_input")
                            )

                            // Error Card (Local validation or Server response)
                            val activeError = localValidationErr ?: serverErrorMessage
                            if (!activeError.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reset_error_card"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEC))
                                ) {
                                    Text(
                                        text = activeError,
                                        color = Color(0xFFB91C1C),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Save Button: "حفظ كلمة المرور الجديدة"
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    localValidationErr = null
                                    if (newPassword != confirmPassword) {
                                        localValidationErr = "كلمتا المرور غير متطابقتين"
                                        return@Button
                                    }
                                    if (newPassword.length < 6) {
                                        localValidationErr = "يجب ألا تقل كلمة المرور عن 6 أحرف"
                                        return@Button
                                    }
                                    authViewModel.resetPassword(newPassword, confirmPassword)
                                },
                                enabled = !isLoading && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("save_new_password_btn"),
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
                                        text = stringResource(R.string.auth_save_new_password_btn),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier.testTag("reset_cancel_btn")
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_recovery_back_to_login),
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
