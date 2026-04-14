package com.example.d_linkmobilymanagement.ui.screens.system

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.d_linkmobilymanagement.R

enum class SystemStatus {
    ONLINE, OFFLINE, CHECKING
}

@Composable
fun SystemOverviewCard(
    isLoading: Boolean,
    activeWanCount: Int,
    modifier: Modifier = Modifier
) {
    val status = when {
        isLoading -> SystemStatus.CHECKING
        activeWanCount > 0 -> SystemStatus.ONLINE
        else -> SystemStatus.OFFLINE
    }

    val statusColor by animateColorAsState(
        targetValue = when (status) {
            SystemStatus.ONLINE -> Color(0xFF4CAF50)
            SystemStatus.OFFLINE -> MaterialTheme.colorScheme.error
            SystemStatus.CHECKING -> MaterialTheme.colorScheme.primary
        },
        label = "statusColor"
    )

    val containerColor by animateColorAsState(
        targetValue = when (status) {
            SystemStatus.ONLINE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            SystemStatus.OFFLINE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            SystemStatus.CHECKING -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        label = "containerColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.system_overview_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                StatusBadge(status = status, statusColor = statusColor)
            }

            AnimatedContent(
                targetState = status,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "mainStatus"
            ) { targetStatus ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when (targetStatus) {
                            SystemStatus.ONLINE -> stringResource(R.string.status_online)
                            SystemStatus.OFFLINE -> stringResource(R.string.status_offline)
                            SystemStatus.CHECKING -> stringResource(R.string.status_checking)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = when (targetStatus) {
                            SystemStatus.ONLINE -> stringResource(R.string.active_wan_count, activeWanCount)
                            SystemStatus.OFFLINE -> stringResource(R.string.no_active_connections)
                            SystemStatus.CHECKING -> stringResource(R.string.checking_connections)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: SystemStatus,
    statusColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        color = statusColor.copy(alpha = 0.15f),
        shape = CircleShape,
        modifier = Modifier.heightIn(min = 28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(if (status == SystemStatus.CHECKING) alpha else 1f)
                    .background(statusColor, CircleShape)
            )
            Text(
                text = when (status) {
                    SystemStatus.ONLINE -> stringResource(R.string.badge_connected)
                    SystemStatus.OFFLINE -> stringResource(R.string.badge_disconnected)
                    SystemStatus.CHECKING -> stringResource(R.string.badge_checking)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}
