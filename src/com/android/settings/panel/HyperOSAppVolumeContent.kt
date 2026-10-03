/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.panel

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.AppVolume
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.android.settings.R
import kotlin.math.roundToInt

object HyperOSAppVolumeViewHelper {
    @JvmStatic
    fun createView(activity: android.app.Activity): android.view.View {
        val composeView = androidx.compose.ui.platform.ComposeView(activity)
        composeView.setContent {
            HyperOSAppVolumeContent(
                onDismiss = { activity.finish() }
            )
        }
        return composeView
    }
}

@Composable
fun HyperOSAppVolumeContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val packageManager = remember(context) { context.packageManager }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Active apps playing volume
    val activeAppVolumes = remember(audioManager) {
        val list = mutableListOf<AppVolume>()
        try {
            for (vol in audioManager.listAppVolumes()) {
                if (vol.isActive && vol.packageName != "android") {
                    list.add(vol)
                }
            }
        } catch (e: Exception) {
        }
        list
    }

    val sliderHeight = if (isLandscape) 140.dp else 190.dp
    val sliderWidth = if (isLandscape) 52.dp else 60.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                detectTapGestures {
                    isVisible = false
                    onDismiss()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(
                initialScale = 0.85f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = scaleOut(
                targetScale = 0.85f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .pointerInput(Unit) {
                        // Consume taps inside so clicking between sliders doesn't dismiss
                        detectTapGestures {}
                    }
                    .padding(16.dp)
            ) {
                // 1. General Media Volume Slider (Speaker icon)
                HyperOSMediaVolumeSlider(
                    audioManager = audioManager,
                    sliderWidth = sliderWidth,
                    sliderHeight = sliderHeight,
                    view = view
                )

                // 2. Individual App Volume Sliders (One per active sound app)
                for (appVol in activeAppVolumes) {
                    HyperOSSingleAppVolumeSlider(
                        appVolume = appVol,
                        audioManager = audioManager,
                        packageManager = packageManager,
                        sliderWidth = sliderWidth,
                        sliderHeight = sliderHeight,
                        view = view
                    )
                }
            }
        }
    }
}

@Composable
private fun HyperOSMediaVolumeSlider(
    audioManager: AudioManager,
    sliderWidth: androidx.compose.ui.unit.Dp,
    sliderHeight: androidx.compose.ui.unit.Dp,
    view: android.view.View,
) {
    val streamType = AudioManager.STREAM_MUSIC
    val maxVol = remember { audioManager.getStreamMaxVolume(streamType).coerceAtLeast(1) }
    val minVol = remember { audioManager.getStreamMinVolume(streamType) }

    var currentVol by remember {
        mutableIntStateOf(
            try {
                audioManager.getStreamVolume(streamType)
            } catch (e: Exception) {
                maxVol / 2
            }
        )
    }

    val fraction = ((currentVol - minVol).toFloat() / (maxVol - minVol).toFloat()).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "HyperOSMediaFraction"
    )

    Column(
        modifier = Modifier
            .width(sliderWidth)
            .height(sliderHeight)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val h = size.height
                            val calcFrac = 1f - (offset.y / h).coerceIn(0f, 1f)
                            val target = (minVol + calcFrac * (maxVol - minVol)).roundToInt()
                            if (target != currentVol) {
                                currentVol = target
                                try {
                                    audioManager.setStreamVolume(streamType, target, 0)
                                } catch (e: Exception) {}
                                view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                            }
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val h = size.height
                            val calcFrac = 1f - (change.position.y / h).coerceIn(0f, 1f)
                            val target = (minVol + calcFrac * (maxVol - minVol)).roundToInt()
                            if (target != currentVol) {
                                currentVol = target
                                try {
                                    audioManager.setStreamVolume(streamType, target, 0)
                                } catch (e: Exception) {}
                                view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                            }
                        }
                    )
                }
        ) {
            val totalH = maxHeight

            // Filled level (from bottom)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(totalH * animatedFraction)
                    .background(MaterialTheme.colorScheme.primary)
            )

            // Bottom Speaker Icon badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .size(36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val target = if (currentVol > minVol) minVol else (maxVol * 0.7f).roundToInt()
                        currentVol = target
                        try {
                            audioManager.setStreamVolume(streamType, target, 0)
                        } catch (e: Exception) {}
                    },
                contentAlignment = Alignment.Center
            ) {
                val iconTint = if (animatedFraction > 0.18f) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurface

                Icon(
                    painter = painterResource(id = R.drawable.ic_hyperos_speaker_mid),
                    contentDescription = "Media",
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun HyperOSSingleAppVolumeSlider(
    appVolume: AppVolume,
    audioManager: AudioManager,
    packageManager: PackageManager,
    sliderWidth: androidx.compose.ui.unit.Dp,
    sliderHeight: androidx.compose.ui.unit.Dp,
    view: android.view.View,
) {
    var volumeFraction by remember(appVolume) { mutableFloatStateOf(appVolume.volume.coerceIn(0f, 1f)) }

    val animatedFraction by animateFloatAsState(
        targetValue = volumeFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "HyperOSAppVol_${appVolume.packageName}"
    )

    val appIconBitmap = remember(appVolume.packageName, packageManager) {
        try {
            val drawable = packageManager.getApplicationIcon(appVolume.packageName)
            drawableToBitmap(drawable)
        } catch (e: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .width(sliderWidth)
            .height(sliderHeight)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val h = size.height
                            val calcFrac = (1f - (offset.y / h)).coerceIn(0f, 1f)
                            volumeFraction = calcFrac
                            try {
                                audioManager.setAppVolume(appVolume.packageName, calcFrac)
                            } catch (e: Exception) {}
                            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val h = size.height
                            val calcFrac = (1f - (change.position.y / h)).coerceIn(0f, 1f)
                            volumeFraction = calcFrac
                            try {
                                audioManager.setAppVolume(appVolume.packageName, calcFrac)
                            } catch (e: Exception) {}
                            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                        }
                    )
                }
        ) {
            val totalH = maxHeight

            // Filled level (from bottom)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(totalH * animatedFraction)
                    .background(MaterialTheme.colorScheme.primary)
            )

            // Bottom App Icon badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .size(36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val target = if (volumeFraction > 0.05f) 0f else 1f
                        volumeFraction = target
                        try {
                            audioManager.setAppVolume(appVolume.packageName, target)
                        } catch (e: Exception) {}
                    },
                contentAlignment = Alignment.Center
            ) {
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap.asImageBitmap(),
                        contentDescription = appVolume.packageName,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_hyperos_speaker_mid),
                        contentDescription = appVolume.packageName,
                        tint = if (animatedFraction > 0.18f) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 64
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 64
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
