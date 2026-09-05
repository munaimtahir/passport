package pk.vexel.financepassport.core.model

/** A deterministic source-backed calendar candidate. The source remains authoritative. */
data class CalendarProjectionSource(
    val sourceType: String,
    val sourceId: String,
    val title: String,
    val dueDateEpochDay: Long,
    val resolved: Boolean = false,
    val reminderMinutesBefore: Long = 0,
) {
    val stableId: String get() = "source-$sourceType-$sourceId"
    val kind: String get() = "SOURCE_$sourceType"
}

fun CalendarProjectionSource.isOverdue(todayEpochDay: Long): Boolean =
    dueDateEpochDay < todayEpochDay && !resolved

fun calendarProjection(
    sources: List<CalendarProjectionSource>,
    todayEpochDay: Long,
): List<CalendarProjectionSource> = sources
    .filter { it.dueDateEpochDay >= todayEpochDay || !it.resolved }
    .distinctBy { it.stableId }
    .sortedWith(compareBy<CalendarProjectionSource> { it.dueDateEpochDay }.thenBy { it.title })
