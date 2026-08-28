package pk.vexel.financepassport.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pk.vexel.financepassport.core.database.FinancialEventEntity
import pk.vexel.financepassport.core.model.PkrMoneyInput
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun FinancialDayGroupHeader(
    date: LocalDate,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val dateLabel = when (date) {
        today -> "TODAY"
        today.minusDays(1) -> "YESTERDAY"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")).uppercase()
    }

    Text(
        text = dateLabel,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 8.dp, horizontal = 4.dp),
    )
}

@Composable
fun FinancialTimelineEventRow(
    event: FinancialEventEntity,
    accountName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMasked: Boolean = false,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("history-item-${event.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val (icon, bgContainer) = when (event.eventType) {
                "INCOME" -> Icons.Filled.ArrowUpward to MaterialTheme.colorScheme.primaryContainer
                "EXPENSE" -> Icons.Filled.ArrowDownward to MaterialTheme.colorScheme.secondaryContainer
                "TRANSFER" -> Icons.Filled.SwapHoriz to MaterialTheme.colorScheme.surfaceVariant
                else -> Icons.Filled.Description to MaterialTheme.colorScheme.surfaceVariant
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bgContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = event.eventType,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.description.ifBlank { event.category ?: "Financial Event" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = listOfNotNull(
                        event.eventType.lowercase().replaceFirstChar { it.uppercase() },
                        event.category,
                        accountName,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val prefix = when (event.eventType) {
                    "INCOME" -> "+ "
                    "EXPENSE" -> "- "
                    else -> ""
                }
                Text(
                    text = if (isMasked) "PKR ••••••" else "$prefix${PkrMoneyInput.formatMinorUnits(event.amountMinor)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = when (event.eventType) {
                        "INCOME" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}
