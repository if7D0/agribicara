package com.agribicara.app.presentation.region

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.model.RegionLevel
import com.agribicara.app.presentation.theme.Dimens

/**
 * Pemilihan wilayah bertingkat.
 *
 * Tanpa izin lokasi sama sekali — keputusan sadar: BMKG butuh kode kelurahan
 * yang tidak bisa didapat dari koordinat, dan menghindari izin runtime
 * menghapus satu sumber friksi besar bagi pengguna dengan literasi digital
 * rendah.
 */
@Composable
fun RegionPickerScreen(
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegionPickerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigasi dipicu oleh state, bukan callback yang diserahkan ke ViewModel.
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) onCompleted()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    // Back menaiki satu tingkat dulu; hanya keluar layar bila sudah di provinsi.
    BackHandler(enabled = uiState.breadcrumb.isNotEmpty()) {
        viewModel.onBack()
    }

    Scaffold(
        modifier = modifier.testTag("region_picker_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.ScreenPadding),
        ) {
            Text(
                text = stringResource(titleFor(uiState.level)),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = Dimens.SpaceMedium),
            )

            if (uiState.breadcrumb.isNotEmpty()) {
                Text(
                    text = uiState.breadcrumb.joinToString(" › ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimens.SpaceMedium),
                )
            }

            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.region_search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("region_search"),
            )

            when {
                uiState.isLoading || uiState.isSaving -> CenteredBox {
                    CircularProgressIndicator()
                }

                uiState.visibleItems.isEmpty() -> CenteredBox {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.region_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        if (uiState.query.isBlank()) {
                            TextButton(onClick = viewModel::retry) {
                                Text(stringResource(R.string.region_retry))
                            }
                        }
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("region_list"),
                ) {
                    items(uiState.visibleItems, key = { it.code }) { region ->
                        RegionRow(region = region, onClick = { viewModel.onSelect(region) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionRow(region: Region, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // NFR aksesibilitas: setiap elemen interaktif minimal 48dp.
            .heightIn(min = Dimens.TouchTargetMin)
            .padding(vertical = Dimens.SpaceMedium),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = region.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.SpaceLarge),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun titleFor(level: RegionLevel): Int = when (level) {
    RegionLevel.PROVINCE -> R.string.region_title_province
    RegionLevel.REGENCY -> R.string.region_title_regency
    RegionLevel.DISTRICT -> R.string.region_title_district
    RegionLevel.VILLAGE -> R.string.region_title_village
}
