package pk.vexel.financepassport.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pk.vexel.financepassport.ui.theme.StatusColors

@Composable
fun VexelStatusChip(
    status: String,
    modifier: Modifier = Modifier,
) {
    val darkTheme = isSystemInDarkTheme()
    val (containerColor, contentColor, icon, description) = when (status) {
        "Overdue" -> Quad(
            if (darkTheme) StatusColors.OverdueContainerDark else StatusColors.OverdueContainerLight,
            if (darkTheme) StatusColors.OnOverdueContainerDark else StatusColors.OnOverdueContainerLight,
            Icons.Filled.Error,
            "Status: Overdue",
        )
        "Due soon" -> Quad(
            if (darkTheme) StatusColors.DueSoonContainerDark else StatusColors.DueSoonContainerLight,
            if (darkTheme) StatusColors.OnDueSoonContainerDark else StatusColors.OnDueSoonContainerLight,
            Icons.Filled.Schedule,
            "Status: Due soon",
        )
        "Paid" -> Quad(
            if (darkTheme) StatusColors.PaidContainerDark else StatusColors.PaidContainerLight,
            if (darkTheme) StatusColors.OnPaidContainerDark else StatusColors.OnPaidContainerLight,
            Icons.Filled.Check,
            "Status: Paid",
        )
        "Pending", "Expected" -> Quad(
            if (darkTheme) StatusColors.PendingContainerDark else StatusColors.PendingContainerLight,
            if (darkTheme) StatusColors.OnPendingContainerDark else StatusColors.OnPendingContainerLight,
            Icons.Filled.HourglassEmpty,
            "Status: Pending",
        )
        "Skipped", "Archived" -> Quad(
            if (darkTheme) StatusColors.SkippedContainerDark else StatusColors.SkippedContainerLight,
            if (darkTheme) StatusColors.OnSkippedContainerDark else StatusColors.OnSkippedContainerLight,
            Icons.Filled.SkipNext,
            "Status: Skipped",
        )
        else -> Quad(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Filled.Schedule,
            "Status: $status",
        )
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier.semantics {
            contentDescription = description
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
