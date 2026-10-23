package com.example.skimguard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.skimguard.ui.theme.ObsidianDanger
import com.example.skimguard.ui.theme.ObsidianOnSurfaceMuted
import com.example.skimguard.ui.theme.ObsidianOutline
import com.example.skimguard.ui.theme.ObsidianWhite
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onGoSignUp: () -> Unit) {
    AuthShell(
        headline = "LOG IN",
        subtitle = "Access your Skimmy scanner.",
        primaryLabel = "LOG IN",
        switchPrompt = "New here?",
        switchLabel = "SIGN UP",
        onSwitch = onGoSignUp,
        action = { username, password -> authSignIn(username, password) },
        onSuccess = onLoggedIn,
    )
}

@Composable
fun SignUpScreen(onSignedUp: () -> Unit, onGoLogin: () -> Unit) {
    AuthShell(
        headline = "SIGN UP",
        subtitle = "Create your Skimmy account.",
        primaryLabel = "SIGN UP",
        switchPrompt = "Already have an account?",
        switchLabel = "LOG IN",
        onSwitch = onGoLogin,
        action = { username, password -> authSignUp(username, password) },
        onSuccess = onSignedUp,
    )
}

@Composable
private fun AuthShell(
    headline: String,
    subtitle: String,
    primaryLabel: String,
    switchPrompt: String,
    switchLabel: String,
    onSwitch: () -> Unit,
    action: suspend (String, String) -> String?,
    onSuccess: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 96.dp, bottom = 40.dp),
        ) {
            Text(
                text = headline,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    lineHeight = 44.sp,
                    color = ObsidianWhite,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = ObsidianOnSurfaceMuted,
                ),
            )
            Spacer(Modifier.height(36.dp))
            LabeledField(
                label = "USERNAME",
                value = username,
                onChange = { username = it; error = null },
                keyboardType = KeyboardType.Ascii,
            )
            Spacer(Modifier.height(20.dp))
            LabeledField(
                label = "PASSWORD",
                value = password,
                onChange = { password = it; error = null },
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
            )
            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = error!!,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = ObsidianDanger,
                    ),
                )
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    if (busy) return@Button
                    val v = validateUsername(username)
                    if (v != null) { error = v; return@Button }
                    if (password.length < 6) {
                        error = "Password must be at least 6 characters"
                        return@Button
                    }
                    busy = true
                    scope.launch {
                        val err = action(username, password)
                        busy = false
                        if (err == null) onSuccess() else error = err
                    }
                },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianWhite,
                    contentColor = Color.Black,
                ),
                enabled = !busy,
            ) {
                Text(
                    text = if (busy) "…" else primaryLabel,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                    ),
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = switchPrompt,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = ObsidianOnSurfaceMuted,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = switchLabel,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        letterSpacing = 1.5.sp,
                        color = ObsidianWhite,
                    ),
                    modifier = Modifier
                        .clickable(onClick = onSwitch)
                        .padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = ObsidianOnSurfaceMuted,
            ),
        )
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            cursorBrush = SolidColor(ObsidianWhite),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp,
                color = ObsidianWhite,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, ObsidianOutline, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) { inner() }
            },
        )
    }
}

@Composable
fun AuthLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "…",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 32.sp,
                color = ObsidianOnSurfaceMuted,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
