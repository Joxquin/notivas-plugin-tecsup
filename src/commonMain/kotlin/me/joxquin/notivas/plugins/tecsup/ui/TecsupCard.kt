package me.joxquin.notivas.plugins.tecsup.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.joxquin.notivas.plugins.tecsup.session.TecsupSessionState
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class PluginThemeMode {
    MATERIAL,
    MIUIX
}

@Composable
fun TecsupCard(
    themeMode: PluginThemeMode,
    sessionState: TecsupSessionState,
    todayClassesCount: Int = 0,
    attendanceAlertsCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (themeMode == PluginThemeMode.MIUIX) {
        TecsupMiuixCard(
            sessionState = sessionState,
            todayClassesCount = todayClassesCount,
            attendanceAlertsCount = attendanceAlertsCount,
            onClick = onClick,
            modifier = modifier
        )
    } else {
        TecsupMaterialCard(
            sessionState = sessionState,
            todayClassesCount = todayClassesCount,
            attendanceAlertsCount = attendanceAlertsCount,
            onClick = onClick,
            modifier = modifier
        )
    }
}

@Composable
private fun TecsupMiuixCard(
    sessionState: TecsupSessionState,
    todayClassesCount: Int,
    attendanceAlertsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MiuixCard(
        modifier = modifier.fillMaxWidth(),
        colors = MiuixCardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
        insideMargin = PaddingValues(16.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick
    ) {
        TecsupCardContent(
            sessionState = sessionState,
            todayClassesCount = todayClassesCount,
            attendanceAlertsCount = attendanceAlertsCount,
            primaryColor = MiuixTheme.colorScheme.primary,
            onSurfaceColor = MiuixTheme.colorScheme.onSurface,
            onSurfaceVariantColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            errorColor = MiuixTheme.colorScheme.error
        )
    }
}

@Composable
private fun TecsupMaterialCard(
    sessionState: TecsupSessionState,
    todayClassesCount: Int,
    attendanceAlertsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TecsupCardContent(
                sessionState = sessionState,
                todayClassesCount = todayClassesCount,
                attendanceAlertsCount = attendanceAlertsCount,
                primaryColor = MaterialTheme.colorScheme.primary,
                onSurfaceColor = MaterialTheme.colorScheme.onSurface,
                onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TecsupCardContent(
    sessionState: TecsupSessionState,
    todayClassesCount: Int,
    attendanceAlertsCount: Int,
    primaryColor: androidx.compose.ui.graphics.Color,
    onSurfaceColor: androidx.compose.ui.graphics.Color,
    onSurfaceVariantColor: androidx.compose.ui.graphics.Color,
    errorColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Tecsup Integración",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurfaceColor
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = onSurfaceVariantColor,
            modifier = Modifier.size(18.dp)
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    when (sessionState) {
        is TecsupSessionState.Connected -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primaryColor.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Clases hoy",
                            fontSize = 12.sp,
                            color = onSurfaceVariantColor
                        )
                        Text(
                            text = "$todayClassesCount",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (attendanceAlertsCount > 0) errorColor.copy(alpha = 0.12f) else onSurfaceVariantColor.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (attendanceAlertsCount > 0) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = errorColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "Alertas faltas",
                                fontSize = 12.sp,
                                color = if (attendanceAlertsCount > 0) errorColor else onSurfaceVariantColor
                            )
                        }
                        Text(
                            text = "$attendanceAlertsCount",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (attendanceAlertsCount > 0) errorColor else onSurfaceColor
                        )
                    }
                }
            }
        }
        is TecsupSessionState.Expired -> {
            Text(
                text = "Sesión expirada. Toca para reanudar.",
                fontSize = 13.sp,
                color = errorColor
            )
        }
        else -> {
            Text(
                text = "Toca para conectar tu cuenta de Tecsup.",
                fontSize = 13.sp,
                color = onSurfaceVariantColor
            )
        }
    }
}
