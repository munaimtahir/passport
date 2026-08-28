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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pk.vexel.financepassport.core.database.MonthlyBillOccurrenceEntity
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class BillRhythmPoint(
    val yearMonth: YearMonth,
    val label: String,
    val status: String,
)

@Composable
fun BillRhythmStrip(
    occurrences: List<MonthlyBillOccurrenceEntity>,
    modifier: Modifier = Modifier,
    monthsToShow: Int = 6,
) {
    val currentMonth = YearMonth.now()
    val points = (monthsToShow - 1 downTo 0).map { offset ->
        val ym = currentMonth.minusMonths(offset.toLong())
        val occ = occurrences.find { it.billingYear == ym.year && it.billingMonth == ym.monthValue }
        val status = occ?.status ?: "Expected"
        BillRhythmPoint(
            yearMonth = ym,
            label = ym.format(DateTimeFormatter.ofPattern("MMM")),
            status = status,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Bill Rhythm",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            points.forEach { point ->
                RhythmItem(point = point)
            }
        }
    }
}

@Composable
private fun RhythmItem(point: BillRhythmPoint) {
    val (bgColor, contentColor, description) = when (point.status) {
        "Paid" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "${point.label}: Paid",
        )
        "Overdue" -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "${point.label}: Overdue",
        )
        "Due soon" -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "${point.label}: Due soon",
        )
        "Skipped" -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "${point.label}: Skipped",
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            "${point.label}: Upcoming",
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics {
            contentDescription = description
        },
    ) {
        Text(
            text = point.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            when (point.status) {
                "Paid" -> Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp),
                )
                "Overdue" -> Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp),
                )
                "Due soon" -> Icon(
                    imageVector = Icons.Filled.HourglassBottom,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(12.dp),
                )
                else -> Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(contentColor),
                )
            }
        }
    }
}
