package pk.vexel.financepassport.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pk.vexel.financepassport.core.model.PkrMoneyInput

data class FinancialAttentionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val status: String,
    val amountMinor: Long?,
    val isCritical: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun FinancialAttentionCard(
    item: FinancialAttentionItem,
    modifier: Modifier = Modifier,
    isMasked: Boolean = false,
) {
    Card(
        onClick = item.onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("pending-card-${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCritical) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = if (item.isCritical) Icons.Filled.Warning else Icons.Filled.NotificationsActive,
                contentDescription = if (item.isCritical) "Critical attention" else "Attention required",
                tint = if (item.isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                VexelStatusChip(status = item.status)
                if (item.amountMinor != null) {
                    Text(
                        text = if (isMasked) "PKR ••••••" else PkrMoneyInput.formatMinorUnits(item.amountMinor),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Action detail",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
