package pk.vexel.financepassport.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
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
import pk.vexel.financepassport.core.database.MonthlyBillOccurrenceEntity
import pk.vexel.financepassport.core.database.UtilityBillProfileEntity
import pk.vexel.financepassport.core.model.PkrMoneyInput
import pk.vexel.financepassport.core.model.UtilityCategory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun LivingBillCard(
    profile: UtilityBillProfileEntity,
    occurrences: List<MonthlyBillOccurrenceEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMasked: Boolean = false,
) {
    val profileOccurrences = occurrences.filter { it.profileId == profile.id }
    val today = LocalDate.now()
    val currentMonthOcc = profileOccurrences.find { it.billingYear == today.year && it.billingMonth == today.monthValue }
        ?: profileOccurrences.maxByOrNull { it.billingYear * 12 + it.billingMonth }

    val status = currentMonthOcc?.status ?: "Expected"
    val amountMinor = currentMonthOcc?.amountMinor
    val dueDateStr = currentMonthOcc?.expectedDueDateEpochDay?.let {
        LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    } ?: "-"

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("profile-card-${profile.id}"),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                "Overdue" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                "Due soon" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryAvatar(category = profile.category)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${profile.provider ?: "Utility"} · ${profile.locationLabel ?: "Home"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Ref: ${maskRef(profile.referenceNumber)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    VexelStatusChip(status = status)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isMasked) "PKR ••••••"
                        else if (amountMinor != null) PkrMoneyInput.formatMinorUnits(amountMinor)
                        else "Amount TBD",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Due $dueDateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View bill detail",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (profileOccurrences.isNotEmpty()) {
                BillRhythmStrip(occurrences = profileOccurrences, monthsToShow = 6)
            }
        }
    }
}

@Composable
private fun CategoryAvatar(category: String) {
    val icon = when (UtilityCategory.fromStored(category)) {
        UtilityCategory.ELECTRICITY -> Icons.Filled.FlashOn
        UtilityCategory.MOBILE_TELEPHONE -> Icons.Filled.Phone
        UtilityCategory.GAS -> Icons.Filled.Star
        else -> Icons.Filled.Description
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = MaterialTheme.shapes.small,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            modifier = Modifier
                .padding(8.dp)
                .size(24.dp),
        )
    }
}

private fun maskRef(ref: String): String {
    if (ref.length <= 4) return ref
    return "••••" + ref.takeLast(4)
}
