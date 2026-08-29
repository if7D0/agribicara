package com.agribicara.app.presentation.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agribicara.app.R
import com.agribicara.app.presentation.onboarding.components.OnboardingPage
import com.agribicara.app.presentation.onboarding.components.OnboardingProgress
import com.agribicara.app.presentation.theme.Dimens
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
)

private val onboardingSlides = listOf(
    OnboardingSlide(
        icon = Icons.Filled.WbSunny,
        titleRes = R.string.onboarding_title_1,
        bodyRes = R.string.onboarding_body_1,
    ),
    OnboardingSlide(
        icon = Icons.Filled.Place,
        titleRes = R.string.onboarding_title_2,
        bodyRes = R.string.onboarding_body_2,
    ),
    OnboardingSlide(
        icon = Icons.Filled.Mic,
        titleRes = R.string.onboarding_title_3,
        bodyRes = R.string.onboarding_body_3,
    ),
)

/**
 * Onboarding tiga slide.
 *
 * PENTING: slide 2 dan 3 hanya MENJELASKAN kebutuhan izin lokasi dan mikrofon.
 * Permintaan izin runtime sengaja TIDAK dilakukan di sini - meminta izin sebelum
 * pengguna memahami manfaatnya adalah penyebab utama penolakan permanen.
 * Izin lokasi diminta pada Fase 2, izin mikrofon pada Fase 3.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { onboardingSlides.size })
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLastPage = pagerState.currentPage == onboardingSlides.lastIndex

    // Navigasi dipicu oleh state, bukan callback yang dipegang ViewModel.
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) onFinished()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        modifier = modifier.testTag("onboarding_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // targetSdk 37 memaksa edge-to-edge: tanpa inset ini, CTA di bawah
                // tertutup navigation bar sistem.
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = Dimens.SpaceLarge),
        ) {
            OnboardingProgress(
                totalPages = onboardingSlides.size,
                currentPage = pagerState.currentPage,
                progressDescription = stringResource(
                    R.string.cd_onboarding_progress,
                    pagerState.currentPage + 1,
                    onboardingSlides.size,
                ),
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
            )

            Box(modifier = Modifier.weight(1f)) {
                HorizontalPager(state = pagerState) { page ->
                    val slide = onboardingSlides[page]
                    OnboardingPage(
                        icon = slide.icon,
                        title = stringResource(slide.titleRes),
                        body = stringResource(slide.bodyRes),
                    )
                }
            }

            Button(
                // Penjaga tap ganda; ViewModel juga menjaga di sisinya.
                enabled = !uiState.isSaving,
                onClick = {
                    if (isLastPage) {
                        viewModel.completeOnboarding()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding)
                    .height(Dimens.ButtonHeight)
                    .testTag("onboarding_cta"),
            ) {
                Text(
                    text = if (isLastPage) {
                        stringResource(R.string.onboarding_start)
                    } else {
                        stringResource(R.string.onboarding_next)
                    },
                )
            }
        }
    }
}
