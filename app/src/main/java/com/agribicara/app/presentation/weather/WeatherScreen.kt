package com.agribicara.app.presentation.weather

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agribicara.app.R
import com.agribicara.app.presentation.theme.Dimens
import com.agribicara.app.presentation.weather.components.DailyForecastRow
import com.agribicara.app.presentation.weather.components.OfflineBanner

/**
 * Prakiraan 7 hari.
 *
 * Hari 1-3 berasal dari BMKG (hiperlokal, level kelurahan) dan hari 4-7 dari
 * Open-Meteo yang ditandai sebagai estimasi — BMKG memang hanya menyediakan
 * sekitar tiga hari.
 */
@Composable
fun WeatherScreen(
    onChooseRegion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeatherViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        modifier = modifier.testTag("weather_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.ScreenPadding),
        ) {
            Text(
                text = uiState.regionName
                    ?.let { stringResource(R.string.weather_title_with_region, it) }
                    ?: stringResource(R.string.weather_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = Dimens.SpaceMedium),
            )

            // Satu-satunya jalan mengganti wilayah setelah terpilih. Tanpa ini
            // pengguna yang salah tap saat setup terkunci selamanya di desa
            // yang keliru — risiko nyata bagi target pengguna aplikasi ini.
            if (!uiState.needsRegion) {
                TextButton(
                    onClick = onChooseRegion,
                    modifier = Modifier
                        .heightIn(min = Dimens.TouchTargetMin)
                        .testTag("change_region"),
                ) {
                    Text(stringResource(R.string.weather_change_region))
                }
            }

            if (uiState.isOffline) {
                OfflineBanner(modifier = Modifier.testTag("offline_banner"))
                Spacer(modifier = Modifier.height(Dimens.SpaceMedium))
            }

            when {
                uiState.isLoading -> Centered {
                    CircularProgressIndicator()
                }

                uiState.needsRegion -> Centered {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.weather_no_region),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpaceMedium))
                        Button(onClick = onChooseRegion) {
                            Text(stringResource(R.string.weather_choose_region))
                        }
                    }
                }

                uiState.days.isEmpty() -> Centered {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.error_weather_unavailable),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpaceMedium))
                        Button(onClick = viewModel::refresh) {
                            Text(stringResource(R.string.weather_retry))
                        }
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("weather_list"),
                ) {
                    items(uiState.days, key = { it.date.toString() }) { day ->
                        DailyForecastRow(day = day)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .padding(Dimens.SpaceLarge),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
