package com.example.expressora.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.expressora.dashboard.user.community_space.CommunitySpaceActivity
import com.example.expressora.ui.theme.ExpressoraTheme
import com.example.expressora.ui.theme.InterFontFamily
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class RegisterActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val GOOGLE_SIGN_IN_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            ExpressoraTheme {
                RegisterScreen(onGoogleSignInClick = { signInWithGoogle() })
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN_REQUEST) {
            val task: Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account?.email
                val name = account?.displayName

                Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, CommunitySpaceActivity::class.java)
                intent.putExtra("USER_NAME", name)
                intent.putExtra("USER_EMAIL", email)
                startActivity(intent)
                finish()

            } catch (e: ApiException) {
                val errorMessage = when (e.statusCode) {
                    7 -> "Network error. Please check your internet connection."
                    10 -> "Developer error: Check your OAuth configuration."
                    13 -> "Error: The sign-in process was canceled or interrupted."
                    12500 -> "Sign-in failed: Unknown issue occurred."
                    12501 -> "Sign-in canceled by the user."
                    12502 -> "Sign-in already in progress. Please wait."
                    else -> "Google Sign-In failed: ${e.message ?: "Unknown error"}"
                }

                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun RegisterScreen(onGoogleSignInClick: () -> Unit = {}) {
    val context = LocalContext.current
    val textColor = Color.Black
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFACC15), Color(0xFFF8F8F8)), startY = 0f, endY = 1000f
    )

    var step by remember { mutableStateOf(1) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val customSelectionColors = TextSelectionColors(
        handleColor = Color(0xFFFACC15), backgroundColor = Color(0x33FACC15)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
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
                        1 -> "Sign Up"
                        else -> "Enter OTP"
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
                        1 -> "Start your journey by joining us today"
                        else -> "We’ve sent a code to your email"
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
                        EmailField(
                            value = email, onValueChange = { email = it }, textColor = textColor
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        PasswordField(
                            value = password,
                            onValueChange = { password = it },
                            textColor = textColor,
                            passwordVisible = passwordVisible,
                            onVisibilityToggle = { passwordVisible = !passwordVisible })

                        Spacer(modifier = Modifier.height(24.dp))

                        YellowButton2("Sign Up") {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                step = 2
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Or sign up with",
                            fontSize = 14.sp,
                            color = textColor,
                            fontFamily = InterFontFamily
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(50.dp)
                                .clickable { onGoogleSignInClick() },
                            shape = CircleShape,
                            color = Color(0xFF0F172A),
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.google_logo),
                                    contentDescription = "Google Sign Up",
                                    modifier = Modifier.size(35.dp)
                                )
                            }
                        }
                    }

                    2 -> {
                        OTPInput2(
                            otpText = otp, onOtpChange = { if (it.length <= 5) otp = it })

                        Spacer(modifier = Modifier.height(24.dp))

                        YellowButton2("Verify") {
                            if (otp.length == 5) {
                                val intent = Intent(context, LoginActivity::class.java)
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommonTextFieldColors(textColor: Color): TextFieldColors {
    return TextFieldDefaults.colors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        focusedIndicatorColor = textColor,
        unfocusedIndicatorColor = textColor,
        cursorColor = textColor,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent
    )
}

@Composable
fun EmailField(value: String, onValueChange: (String) -> Unit, textColor: Color) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text("Email", color = textColor, fontFamily = InterFontFamily)
        },
        singleLine = true,
        modifier = Modifier.width(300.dp),
        colors = CommonTextFieldColors(textColor),
        textStyle = TextStyle(
            fontFamily = InterFontFamily, fontSize = 16.sp
        )
    )
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    textColor: Color,
    passwordVisible: Boolean,
    onVisibilityToggle: () -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text("Password", color = textColor, fontFamily = InterFontFamily)
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            IconButton(onClick = onVisibilityToggle) {
                Icon(
                    imageVector = icon, contentDescription = "Toggle Password", tint = textColor
                )
            }
        },
        singleLine = true,
        modifier = Modifier.width(300.dp),
        colors = CommonTextFieldColors(textColor),
        textStyle = TextStyle(
            fontFamily = InterFontFamily, fontSize = 16.sp
        )
    )
}

@Composable
fun OTPInput2(otpText: String, onOtpChange: (String) -> Unit) {
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

@Composable
fun YellowButton2(text: String, onClick: () -> Unit) {
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

@Preview(showBackground = true)
@Composable
fun RegisterPreview() {
    ExpressoraTheme {
        RegisterScreen()
    }
}
