package com.docufind.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import com.docufind.app.ui.theme.DocuFindVaultGlow
import com.docufind.app.ui.theme.DocuFindWhite
import androidx.compose.ui.unit.dp

@Composable
fun PinDotIndicator(
    filledCount: Int,
    pinLength: Int = 6,
    modifier: Modifier = Modifier,
    lightMode: Boolean = false,
    errorShakeKey: Int = 0
) {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(errorShakeKey) {
        if (errorShakeKey > 0) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    14f at 80
                    (-14f) at 160
                    10f at 240
                    (-10f) at 320
                    0f at 400
                }
            )
        }
    }

    Row(
        modifier = modifier.offset(x = shakeOffset.value.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(pinLength) { index ->
            Surface(
                modifier = Modifier.size(14.dp),
                shape = CircleShape,
                color = if (index < filledCount) {
                    if (lightMode) DocuFindVaultGlow else MaterialTheme.colorScheme.primary
                } else {
                    if (lightMode) {
                        DocuFindWhite.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    }
                }
            ) {}
        }
    }
}

@Composable
fun PinKeypad(
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    lightKeys: Boolean = false,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val keys = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9),
        listOf(-1, 0, -2)
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    when (key) {
                        -1 -> Spacer(modifier = Modifier.size(64.dp))
                        -2 -> KeyButton(
                            text = "⌫",
                            onClick = {
                                if (enabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDelete()
                                }
                            },
                            lightKeys = lightKeys,
                            enabled = enabled
                        )
                        else -> KeyButton(
                            text = key.toString(),
                            onClick = {
                                if (enabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDigit(key)
                                }
                            },
                            lightKeys = lightKeys,
                            enabled = enabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    text: String,
    onClick: () -> Unit,
    lightKeys: Boolean,
    enabled: Boolean
) {
    TextButton(onClick = onClick, modifier = Modifier.size(64.dp), enabled = enabled) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = if (lightKeys) {
                DocuFindWhite
            } else {
                MaterialTheme.colorScheme.onBackground
            }
        )
    }
}
