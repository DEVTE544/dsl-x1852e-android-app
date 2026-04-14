package com.example.d_linkmobilymanagement.ui.screens.system
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.d_linkmobilymanagement.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemHeader(
    modifier: Modifier = Modifier,
    title: String,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null,
) {
    if (showBackButton && onBackClick != null) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                if (onRefreshClick != null) {
                    IconButton(onClick = onRefreshClick) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.internet_status_refresh)
                        )
                    }
                }
                IconButton(onClick = { onSettingsClick?.invoke() }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            },
            modifier = modifier
        )
    } else {
        TopAppBar(
            title = { Text(title) },
            actions = {
                if (onRefreshClick != null) {
                    IconButton(onClick = onRefreshClick) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.internet_status_refresh)
                        )
                    }
                }
                IconButton(onClick = { onSettingsClick?.invoke() }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            },
            modifier = modifier
        )
    }
}
