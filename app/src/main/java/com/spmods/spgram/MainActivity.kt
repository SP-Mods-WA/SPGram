package com.spmods.spgram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.spmods.spgram.engine.TelegramManager
import com.spmods.spgram.ui.screens.ChatListScreen
import com.spmods.spgram.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class MainActivity : ComponentActivity() {
    private lateinit var telegramManager: TelegramManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        telegramManager = TelegramManager(applicationContext)
        telegramManager.initClient()

        setContent {
            // Load saved theme — default dark
            var isDark by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                isDark = ThemePreference.isDarkFlow(applicationContext).first()
            }

            val bgColor = if (isDark) DarkBackground else LightBackground
            enableEdgeToEdge(
                statusBarStyle = if (isDark)
                    SystemBarStyle.dark(bgColor.toArgb())
                else
                    SystemBarStyle.light(bgColor.toArgb(), bgColor.toArgb())
            )

            SPGramTheme(isDark = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authState by telegramManager.authState.collectAsState()

                    Crossfade(
                        targetState = authState,
                        animationSpec = tween(300),
                        label = "auth"
                    ) { state ->
                        when (state?.constructor) {
                            TdApi.AuthorizationStateReady.CONSTRUCTOR ->
                                ChatListScreen(
                                    manager = telegramManager,
                                    isDark  = isDark,
                                    onToggleTheme = {
                                        isDark = !isDark
                                        // Save to DataStore
                                        lifecycleScope.launch {
                                            ThemePreference.save(applicationContext, isDark)
                                        }
                                    }
                                )
                            else ->
                                AuthScreen(state, telegramManager)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthScreen(authState: TdApi.AuthorizationState?, manager: TelegramManager) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SPGram", fontWeight = FontWeight.Bold, fontSize = 32.sp, color = OnBackground)
        Spacer(Modifier.height(8.dp))

        when (authState?.constructor) {
            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                var phone by remember { mutableStateOf("") }
                Text("Sign in", color = OnSurfaceVar, fontSize = 15.sp)
                Spacer(Modifier.height(28.dp))
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Phone (+CountryCode)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { manager.sendPhoneNumber(phone) },
                    modifier = Modifier.fillMaxWidth(), enabled = phone.isNotBlank()
                ) { Text("Send Code") }
            }
            TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                var code by remember { mutableStateOf("") }
                Text("Enter verification code", color = OnSurfaceVar, fontSize = 15.sp)
                Spacer(Modifier.height(28.dp))
                OutlinedTextField(
                    value = code, onValueChange = { code = it },
                    label = { Text("Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { manager.sendVerificationCode(code) },
                    modifier = Modifier.fillMaxWidth(), enabled = code.isNotBlank()
                ) { Text("Verify") }
            }
            TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                var pass by remember { mutableStateOf("") }
                Text("Two-step verification", color = OnSurfaceVar, fontSize = 15.sp)
                Spacer(Modifier.height(28.dp))
                OutlinedTextField(
                    value = pass, onValueChange = { pass = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { manager.sendPassword(pass) },
                    modifier = Modifier.fillMaxWidth(), enabled = pass.isNotBlank()
                ) { Text("Submit") }
            }
            else -> {
                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator(color = Primary)
                Spacer(Modifier.height(16.dp))
                Text("Starting engine...", color = OnSurfaceVar, fontSize = 13.sp)
            }
        }
    }
}
