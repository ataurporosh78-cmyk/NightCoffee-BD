package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.LocalAuthRepository
import com.example.ui.components.AnimatedSteamingCoffeeEmblem
import com.example.ui.components.EaseOutMotion
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.ClosedRed
import com.example.ui.theme.ClosedRedContainer
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.DeepRoastSurface
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.MochaCardSurface
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.OpenGreenContainer
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private enum class AuthMethodLoading {
    NONE,
    EMAIL,
    GOOGLE
}

internal object AuthInputValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && EMAIL_REGEX.matches(trimmed)
    }

    fun validateEmailAuth(
        isSignUpMode: Boolean,
        fullName: String,
        email: String,
        password: String
    ): String? {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim()
        return when {
            isSignUpMode && cleanName.length < 2 ->
                "Please enter your name (at least 2 characters)."
            !isValidEmail(cleanEmail) ->
                "Please enter a valid email address."
            password.length < 6 ->
                "Password must be at least 6 characters."
            else -> null
        }
    }

    fun validatePasswordResetEmail(email: String): String? {
        return if (!isValidEmail(email)) {
            "Enter your email address above first to reset your password."
        } else {
            null
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val localAuth = remember(context) { LocalAuthRepository(context.applicationContext) }

    var isSignUpMode by rememberSaveable { mutableStateOf(false) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var loadingMethod by remember { mutableStateOf(AuthMethodLoading.NONE) }
    var authError by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    val isBusy = loadingMethod != AuthMethodLoading.NONE

    val submitEmailAction: () -> Unit = {
        focusManager.clearFocus()
        authError = null
        infoMessage = null
        val cleanEmail = email.trim()
        val cleanName = fullName.trim()
        val validationError = AuthInputValidator.validateEmailAuth(
            isSignUpMode = isSignUpMode,
            fullName = cleanName,
            email = cleanEmail,
            password = password
        )

        if (validationError != null) {
            authError = validationError
        } else {
            loadingMethod = AuthMethodLoading.EMAIL
            performEmailAuth(
                repository = localAuth,
                isSignUp = isSignUpMode,
                displayName = cleanName,
                email = cleanEmail,
                password = password,
                scope = scope,
                onSuccess = {
                    loadingMethod = AuthMethodLoading.NONE
                    onAuthSuccess()
                },
                onError = { msg ->
                    loadingMethod = AuthMethodLoading.NONE
                    authError = msg
                }
            )
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DeepRoastSurface,
        unfocusedContainerColor = DeepRoastSurface,
        focusedBorderColor = CaramelGold.copy(alpha = 0.75f),
        unfocusedBorderColor = CocoaBorder,
        focusedTextColor = WarmCream,
        unfocusedTextColor = WarmCream,
        cursorColor = CaramelGold,
        focusedLabelColor = CaramelGold,
        unfocusedLabelColor = MutedCinnamon,
        focusedLeadingIconColor = CaramelGold,
        unfocusedLeadingIconColor = MutedCinnamon
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(EspressoBlack)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        EspressoRich.copy(alpha = 0.42f),
                        EspressoBlack,
                        EspressoBlack
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val scrollState = rememberScrollState()
        val minContentHeight = (maxHeight - 40.dp).coerceAtLeast(0.dp)
        val density = LocalDensity.current
        val enterOffsetPx = remember(density) { with(density) { 24.dp.toPx() } }
        val enterAlpha = remember { Animatable(0f) }
        val enterTranslateY = remember { Animatable(enterOffsetPx) }

        LaunchedEffect(Unit) {
            launch {
                enterAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 750, easing = EaseOutMotion)
                )
            }
            launch {
                enterTranslateY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 750, easing = EaseOutMotion)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .heightIn(min = minContentHeight)
                    .graphicsLayer {
                        alpha = enterAlpha.value
                        translationY = enterTranslateY.value
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
            ) {
                AnimatedSteamingCoffeeEmblem(
                    size = 132.dp,
                    modifier = Modifier.testTag("auth_steaming_coffee_emblem")
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displaySmall,
                        color = WarmCream,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.auth_welcome_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = CaramelGold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.auth_welcome_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ToastedLatte,
                        textAlign = TextAlign.Center
                    )
                }

                LiquidGlassSurface(
                    shape = RoundedCornerShape(26.dp),
                    accentGlow = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Sign In / Create Account Mode Switcher
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DeepRoastSurface,
                            border = BorderStroke(1.dp, CocoaBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AuthModeTab(
                                    label = stringResource(R.string.auth_sign_in_email),
                                    selected = !isSignUpMode,
                                    enabled = !isBusy,
                                    onClick = {
                                        isSignUpMode = false
                                        authError = null
                                        infoMessage = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("auth_mode_sign_in")
                                )
                                AuthModeTab(
                                    label = stringResource(R.string.auth_sign_up_email),
                                    selected = isSignUpMode,
                                    enabled = !isBusy,
                                    onClick = {
                                        isSignUpMode = true
                                        authError = null
                                        infoMessage = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("auth_mode_sign_up")
                                )
                            }
                        }

                        authError?.let { errorMsg ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ClosedRedContainer,
                                border = BorderStroke(1.dp, ClosedRed.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ClosedRed,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        infoMessage?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = OpenGreenContainer,
                                border = BorderStroke(1.dp, OpenGreen.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OpenGreen,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        AnimatedVisibility(visible = isSignUpMode) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { if (it.length <= 60) fullName = it },
                                label = { Text("Full Name") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Person, contentDescription = null)
                                },
                                singleLine = true,
                                enabled = !isBusy,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                shape = RoundedCornerShape(16.dp),
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_name_input")
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { if (it.length <= 120) email = it },
                            label = { Text("Email Address") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Email, contentDescription = null)
                            },
                            singleLine = true,
                            enabled = !isBusy,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { if (it.length <= 64) password = it },
                            label = { Text(if (isSignUpMode) "Password (min 6 chars)" else "Password") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = ToastedLatte
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            enabled = !isBusy,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { submitEmailAction() }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input")
                        )

                        if (!isSignUpMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    enabled = !isBusy,
                                    onClick = {
                                        authError = null
                                        infoMessage = null
                                        val cleanEmail = email.trim()
                                        val resetError = AuthInputValidator.validatePasswordResetEmail(cleanEmail)
                                        if (resetError != null) {
                                            authError = resetError
                                        } else {
                                            infoMessage = "Local accounts are stored on this device and cannot receive email reset links. Create a new account if you no longer know the password."
                                        }
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("forgot_password_button")
                                ) {
                                    Text(
                                        text = "Forgot Password?",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CaramelGold
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = submitEmailAction,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CaramelGold,
                                contentColor = DarkEspressoInk
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("email_auth_submit_button")
                        ) {
                            if (loadingMethod == AuthMethodLoading.EMAIL) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = DarkEspressoInk,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(
                                        if (isSignUpMode) R.string.auth_sign_up_email else R.string.auth_sign_in_email
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = CocoaBorder
                            )
                            Text(
                                text = "or",
                                style = MaterialTheme.typography.labelMedium,
                                color = MutedCinnamon
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = CocoaBorder
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                authError = null
                                infoMessage = null
                                loadingMethod = AuthMethodLoading.GOOGLE
                                scope.launch {
                                    runCatching { localAuth.continueWithLocalProfile() }
                                        .onSuccess {
                                            loadingMethod = AuthMethodLoading.NONE
                                            onAuthSuccess()
                                        }
                                        .onFailure {
                                            loadingMethod = AuthMethodLoading.NONE
                                            authError = "Unable to start a local profile. Please try again."
                                        }
                                }
                            },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmCream),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (loadingMethod == AuthMethodLoading.GOOGLE) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = CaramelGold,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.LocalCafe,
                                    contentDescription = null,
                                    tint = CaramelGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Continue with local profile",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Text(
                            text = "Local-only profile · not verified by Google",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedCinnamon,
                            textAlign = TextAlign.Center
                        )

                        TextButton(
                            enabled = !isBusy,
                            onClick = {
                                scope.launch {
                                    runCatching { localAuth.continueAsGuest() }
                                        .onSuccess {
                                            loadingMethod = AuthMethodLoading.NONE
                                            onAuthSuccess()
                                        }
                                        .onFailure {
                                            loadingMethod = AuthMethodLoading.NONE
                                            authError = "Unable to continue as guest. Please try again."
                                        }
                                }
                            },
                            modifier = Modifier.testTag("continue_as_guest_button")
                        ) {
                            Text("Continue as Guest", color = MutedCinnamon, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthModeTab(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) CaramelGold else DeepRoastSurface
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) DarkEspressoInk else ToastedLatte,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun performEmailAuth(
    repository: LocalAuthRepository,
    isSignUp: Boolean,
    displayName: String,
    email: String,
    password: String,
    scope: CoroutineScope,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    scope.launch {
        try {
            if (isSignUp) repository.createAccount(displayName, email, password.toCharArray())
            else repository.signIn(email, password.toCharArray())
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Unable to sign in locally. Please try again.")
        }
    }
}

fun signOut(
    context: Context,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        runCatching { LocalAuthRepository(context.applicationContext).signOut() }
        onSignOutComplete()
    }
}
