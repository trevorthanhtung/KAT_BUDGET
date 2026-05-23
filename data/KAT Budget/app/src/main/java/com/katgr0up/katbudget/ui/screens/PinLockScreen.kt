package com.katgr0up.katbudget.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.utils.KatSpringSpec
import com.katgr0up.katbudget.ui.utils.katStringResource
import kotlinx.coroutines.delay

@Composable
fun PinLockScreen(
    isEng: Boolean,
    colors: BudgetColors,
    inputPin: String,
    isBiometricEnabled: Boolean,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit,
    getSavedPin: () -> String?
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var isError by remember { mutableStateOf(false) }
    var hasPromptedBiometric by remember { mutableStateOf(false) }
    var biometricPromptRequest by remember { mutableIntStateOf(0) }

    val fragmentActivity = remember(context) { context.findFragmentActivity() }
    val canUseBiometric = remember(context, fragmentActivity, isBiometricEnabled) {
        isBiometricEnabled &&
            fragmentActivity != null &&
            BiometricManager.from(context).canAuthenticate(BIOMETRIC_AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    val biometricTitle = katStringResource(id = R.string.pin_biometric_title, isEng = isEng)
    val biometricSubtitle = katStringResource(id = R.string.pin_biometric_subtitle, isEng = isEng)
    val biometricCancel = katStringResource(id = R.string.btn_cancel, isEng = isEng)
    val biometricRetry = katStringResource(id = R.string.pin_biometric_retry, isEng = isEng)

    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric && !hasPromptedBiometric) {
            hasPromptedBiometric = true
            biometricPromptRequest++
        }
    }

    LaunchedEffect(biometricPromptRequest) {
        if (biometricPromptRequest <= 0 || !canUseBiometric) return@LaunchedEffect

        val activity = fragmentActivity ?: return@LaunchedEffect
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onUnlock()
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(biometricTitle)
            .setSubtitle(biometricSubtitle)
            .setAllowedAuthenticators(BIOMETRIC_AUTHENTICATORS)
            .setNegativeButtonText(biometricCancel)
            .setConfirmationRequired(false)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(inputPin) {
        if (inputPin.length == PIN_LENGTH) {
            if (inputPin == getSavedPin()) {
                onUnlock()
            } else {
                isError = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(400)
                onPinChange("")
                isError = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundBrush),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LockOrb(colors = colors)

        Spacer(Modifier.height(18.dp))

        Text(
            text = katStringResource(id = R.string.pin_title_lock, isEng = isEng),
            color = colors.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = katStringResource(id = R.string.pin_desc_lock, isEng = isEng),
            color = colors.subText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        if (canUseBiometric) {
            Spacer(Modifier.height(14.dp))

            Text(
                text = biometricRetry,
                color = colors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    biometricPromptRequest++
                }
            )
        }

        Spacer(Modifier.height(44.dp))

        PinDots(inputPin.length, isError, colors)

        Spacer(Modifier.height(54.dp))

        PinKeypad(
            isEng = isEng,
            colors = colors,
            onNumberClick = { number ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (inputPin.length < PIN_LENGTH) onPinChange(inputPin + number)
            },
            onDeleteClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (inputPin.isNotEmpty()) onPinChange(inputPin.dropLast(1))
            }
        )
    }
}

@Composable
private fun LockOrb(colors: BudgetColors) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = colors.accent.copy(alpha = 0.28f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
private fun PinDots(
    inputLength: Int,
    isError: Boolean,
    colors: BudgetColors
) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(PIN_LENGTH) { index ->
            val isFilled = index < inputLength
            val dotColor by animateColorAsState(
                targetValue = when {
                    isError -> colors.negative
                    isFilled -> colors.accent
                    else -> colors.card.copy(alpha = 0.72f)
                },
                label = "pinDotColor$index"
            )
            val borderColor by animateColorAsState(
                targetValue = when {
                    isError -> colors.negative
                    isFilled -> colors.accent.copy(alpha = 0.52f)
                    else -> colors.border.copy(alpha = 0.62f)
                },
                label = "pinDotBorder$index"
            )

            Box(
                modifier = Modifier
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun PinKeypad(
    isEng: Boolean,
    colors: BudgetColors,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "", "0", "DEL"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { key ->
                    when {
                        key.isBlank() -> Spacer(Modifier.size(70.dp))
                        key == "DEL" -> PinKey(
                            isEng = isEng,
                            label = key,
                            isDelete = true,
                            colors = colors,
                            onClick = onDeleteClick
                        )
                        else -> PinKey(
                            isEng = isEng,
                            label = key,
                            isDelete = false,
                            colors = colors,
                            onClick = { onNumberClick(key) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKey(
    isEng: Boolean,
    label: String,
    isDelete: Boolean,
    colors: BudgetColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = KatSpringSpec,
        label = "pinKeyScale"
    )

    Box(
        modifier = Modifier
            .size(70.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isPressed) {
                    colors.accent.copy(alpha = 0.16f)
                } else {
                    colors.card.copy(alpha = 0.64f)
                }
            )
            .border(
                width = 1.dp,
                color = if (isPressed) colors.accent.copy(alpha = 0.34f) else colors.border.copy(alpha = 0.62f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isDelete) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = katStringResource(id = R.string.pin_accessibility_delete, isEng = isEng),
                tint = colors.text,
                modifier = Modifier.size(27.dp)
            )
        } else {
            Text(
                text = label,
                color = colors.text,
                fontSize = 27.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private const val PIN_LENGTH = 4
private const val BIOMETRIC_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

private tailrec fun Context.findFragmentActivity(): FragmentActivity? {
    return when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }
}
