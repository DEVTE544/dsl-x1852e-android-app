package com.example.d_linkmobilymanagement.ui.screens.system
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.d_linkmobilymanagement.R
import com.example.d_linkmobilymanagement.data.model.SystemMenuItem
import com.example.d_linkmobilymanagement.viewmodel.SafeSystemViewModel
import com.example.d_linkmobilymanagement.viewmodel.SafeSystemUiEvent

@Composable
fun CompleteSystemScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(),
    onNavigateToInternetStatus: () -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: SafeSystemViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Localized filtering logic with Keyword Support
    val filteredMenuItems = remember(uiState.menuItems, uiState.searchQuery, context) {
        val query = uiState.searchQuery.normalizeForSearch()
        uiState.menuItems.filter { item ->
            if (query.isBlank()) return@filter true

            // 1. Check Title
            val title = context.getString(item.titleResId).normalizeForSearch()
            if (title.contains(query)) return@filter true

            // 2. Check Related Keywords (Content Strings)
            val hasMatchingKeyword = item.relatedKeywordsResIds.any { resId ->
                context.getString(resId).normalizeForSearch().contains(query)
            }
            
            hasMatchingKeyword
        }.map { item ->
            item to context.getString(item.titleResId)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // System Header
        SystemHeader(
            title = stringResource(R.string.system_overview),
            showBackButton = false,
            onSettingsClick = onSettingsClick,
            onRefreshClick = { viewModel.refreshSystemData() }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // System Summary Card
            SystemOverviewCard(
                isLoading = uiState.isLoading,
                activeWanCount = uiState.activeWanCount
            )

            // Modern Search Bar
            SystemSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onEvent(SafeSystemUiEvent.OnSearchQueryChanged(it)) },
                onClearClick = { viewModel.onEvent(SafeSystemUiEvent.OnSearchQueryChanged("")) }
            )

            // Features List
            Text(
                text = stringResource(R.string.system_internet_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Box(modifier = Modifier.weight(1f)) {
                if (filteredMenuItems.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 16.dp)
                    ) {
                        items(filteredMenuItems) { (item, localizedTitle) ->
                            SystemMenuButton(
                                item = item,
                                title = localizedTitle,
                                onClick = {
                                    if (item.route == "internet_status") {
                                        onNavigateToInternetStatus()
                                    }
                                }
                            )
                        }
                    }
                } else {
                    EmptySearchState(
                        query = uiState.searchQuery,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                ErrorMessageCard(
                    error = error,
                    onDismiss = { viewModel.onEvent(SafeSystemUiEvent.ClearError) }
                )
            }
        }
    }
}

@Composable
fun SystemMenuButton(
    item: SystemMenuItem,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = item.iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun EmptySearchState(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.system_no_results, query),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.system_no_results_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorMessageCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.error_unknown),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

private fun String.normalizeForSearch(): String {
    return this.trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")
}
