package com.agribicara.app.presentation.voice.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.agribicara.app.presentation.theme.Dimens

/**
 * Tombol mikrofon dengan lingkaran pulsa mengikuti volume suara.
 *
 * Pulsa ini bukan hiasan: tanpa umpan balik yang terlihat, orang tidak tahu
 * apakah suaranya tertangkap dan cenderung menekan tombol berulang kali -
 * yang justru membatalkan sesi yang sedang berjalan.
 */
@Composable
fun MicIndicator(
    isListening: Boolean,
    soundLevel: Float,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Skala dianimasikan, bukan dipakai mentah: nilai RMS berubah sangat
    // sering dan meloncat-loncat, sehingga tanpa animasi lingkarannya bergetar.
    val pulseScale by animateFloatAsState(
        targetValue = if (isListening) 1f + (soundLevel * PULSE_RANGE) else 1f,
        label = "pulse",
    )

    Box(
        modifier = modifier.size(Dimens.MicButtonSize + PULSE_HEADROOM),
        contentAlignment = Alignment.Center,
    ) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(Dimens.MicButtonSize)
                    .scale(pulseScale)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = PULSE_ALPHA),
                        shape = CircleShape,
                    ),
            )
        }

        FilledIconButton(
            onClick = onClick,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .size(Dimens.MicButtonSize)
                // Deskripsi dipasang di sini, bukan pada Icon, supaya pembaca
                // layar mengumumkan status "sedang mendengarkan" pada elemen
                // yang benar-benar bisa difokuskan.
                .semantics { this.contentDescription = contentDescription }
                .testTag("voice_mic_button"),
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                modifier = Modifier.size(Dimens.MicIconSize),
            )
        }
    }
}

/** Pembesaran maksimum lingkaran pulsa pada volume penuh. */
private const val PULSE_RANGE = 0.35f

private const val PULSE_ALPHA = 0.25f

/** Ruang ekstra agar pulsa terbesar tidak terpotong. */
private val PULSE_HEADROOM = 48.dp
