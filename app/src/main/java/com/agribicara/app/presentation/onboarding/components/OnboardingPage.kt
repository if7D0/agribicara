package com.agribicara.app.presentation.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agribicara.app.presentation.theme.Dimens

/**
 * Indikator progres bertipe segmented bar.
 *
 * Dipilih dibanding dot indicator karena jauh lebih terbaca bagi pengguna dengan
 * literasi digital rendah dan penglihatan menurun - dot berukuran kecil mudah
 * terlewat saat layar terkena sinar matahari.
 */
@Composable
fun OnboardingProgress(
    totalPages: Int,
    currentPage: Int,
    progressDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = progressDescription },
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSmall),
    ) {
        repeat(totalPages) { index ->
            val isActive = index <= currentPage
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(Dimens.ProgressBarHeight)
                    .background(
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(Dimens.ProgressBarHeight / 2),
                    ),
            )
        }
    }
}

/**
 * Satu slide onboarding: ikon besar, headline, body.
 *
 * Layout mengikuti pola yang konsisten muncul di aplikasi produksi - headline
 * tebal di sepertiga atas, body pendek di bawahnya. CTA sengaja TIDAK ada di
 * dalam slide; CTA di-pin ke bawah layar oleh pemanggil agar posisinya tetap
 * saat slide bergeser.
 */
@Composable
fun OnboardingPage(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(120.dp),
        )
        Spacer(modifier = Modifier.height(Dimens.SpaceExtraLarge))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Dimens.SpaceMedium))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
