package com.example.expressora.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressora.R
import com.example.expressora.ui.theme.ExpressoraTheme
import com.example.expressora.ui.theme.InterFontFamily

class ResetPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpressoraTheme {
                ResetPasswordScreen()
            }
        }
    }
}

@Composable
fun ResetPasswordScreen() {
    val context = LocalContext.current
    val textColor = Color.Black
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFACC15), Color(0xFFF8F8F8)), startY = 0f, endY = 1000f
    )

    var step by remember { mutableStateOf(1) }
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(300.dp)
        ) {

            Image(
                painter = painterResource(id = R.drawable.expressora_logo),
                contentDescription = "Expressora Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))


            Text(
                text = when (step) {
                    1 -> "Reset Password"
                    2 -> "Enter OTP"
                    else -> "Set New Password"
                },
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontFamily = InterFontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))


            Text(
                text = when (step) {
                    1 -> "Enter your email to receive a reset code"
                    2 -> "We’ve sent a code to your email"
                    else -> "Choose a new password you’ll remember"
                },
                color = textColor,
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (step) {
                1 -> {
                    StyledTextField(
                        value = email, onValueChange = { email = it }, placeholder = "Email"
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    YellowButton("Next") {
                        if (email.isNotBlank()) step = 2
                    }
                }

                2 -> {
                    OTPInput(otpText = otp, onOtpChange = { if (it.length <= 5) otp = it })
                    Spacer(modifier = Modifier.height(24.dp))
                    YellowButton("Verify") {
                        if (otp.length == 5) step = 3
                    }
                }

                3 -> {
                    StyledTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = "New Password",
                        isPassword = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    StyledTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Confirm Password",
                        isPassword = true
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    YellowButton("Reset") {

                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }
                }
            }
        }
    }
}

@Composable
fun StyledTextField(
    value: String, onValueChange: (String) -> Unit, placeholder: String, isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val textColor = Color.Black

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = textColor, fontFamily = InterFontFamily)
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (isPassword) {
                val icon =
                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(icon, contentDescription = null, tint = textColor)
                }
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            cursorColor = textColor,
            focusedIndicatorColor = textColor,
            unfocusedIndicatorColor = textColor,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        )
    )
}

@Composable
fun YellowButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(150.dp)
            .height(35.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFACC15), contentColor = Color.Black
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun OTPInput(
    otpText: String, onOtpChange: (String) -> Unit
) {
    val textColor = Color.Black

    Box(
        modifier = Modifier
            .width(300.dp)
            .padding(horizontal = 8.dp)
    ) {
        BasicTextField(
            value = otpText, onValueChange = { onOtpChange(it) }, decorationBox = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        val char = otpText.getOrNull(index)?.toString() ?: ""
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, Color.Gray, RoundedCornerShape(10.dp))
                                .background(Color.White), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char, style = TextStyle(
                                    color = textColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    fontFamily = InterFontFamily
                                )
                            )
                        }
                    }
                }
            }, textStyle = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ResetPasswordPreview() {
    ExpressoraTheme {
        ResetPasswordScreen()
    }
}
