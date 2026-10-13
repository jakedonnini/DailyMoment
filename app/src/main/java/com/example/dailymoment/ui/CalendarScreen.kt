package com.example.dailymoment.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dailymoment.data.Entry
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val GreenDay = Color(0xFF2E7D32)
private val RedDay = Color(0xFFC62828)
private val weekdayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy")

private enum class DayStatus { Done, Missed, Neutral }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    entries: List<Entry>,
    streak: Int,
    visibleMonth: String,                       // "2026-10"; hoisted so it survives opening an entry
    onVisibleMonthChange: (String) -> Unit,
    onOpenEntry: (Long) -> Unit,
    onCapture: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val byDate = remember(entries) {
        entries.sortedBy { it.createdAt }
            .groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }
    }
    val today = LocalDate.now()
    val firstDate = byDate.keys.minOrNull()
    val firstMonth = firstDate?.let { YearMonth.from(it) } ?: YearMonth.from(today)
    val currentMonth = YearMonth.from(today)
    val pageCount = (ChronoUnit.MONTHS.between(firstMonth, currentMonth).toInt() + 1).coerceAtLeast(1)

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCapture) { Text("Capture now") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Text("Daily Moment", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (streak > 0) "🔥 $streak day streak" else "Start your streak today",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Re-created only when the earliest month changes (e.g. entries finish loading).
            key(firstMonth) {
                val initialPage = ChronoUnit.MONTHS
                    .between(firstMonth, YearMonth.parse(visibleMonth))
                    .toInt().coerceIn(0, pageCount - 1)
                val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }
                val scope = rememberCoroutineScope()

                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }.collect {
                        onVisibleMonthChange(firstMonth.plusMonths(it.toLong()).toString())
                    }
                }

                val month = firstMonth.plusMonths(pagerState.currentPage.toLong())

                // Month header with back/forward buttons
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        enabled = pagerState.currentPage > 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
                    ) { Text("‹", style = MaterialTheme.typography.headlineSmall) }

                    Text(
                        month.format(monthFormat),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    TextButton(
                        enabled = pagerState.currentPage < pageCount - 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
                    ) { Text("›", style = MaterialTheme.typography.headlineSmall) }
                }

                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    weekdayLabels.forEach {
                        Text(
                            it,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                    MonthGrid(
                        ym = firstMonth.plusMonths(page.toLong()),
                        byDate = byDate,
                        today = today,
                        firstDate = firstDate,
                        onOpenEntry = onOpenEntry
                    )
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    Legend(GreenDay, "Captured")
                    Legend(RedDay, "Missed")
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    ym: YearMonth,
    byDate: Map<LocalDate, List<Entry>>,
    today: LocalDate,
    firstDate: LocalDate?,
    onOpenEntry: (Long) -> Unit
) {
    val offset = ym.atDay(1).dayOfWeek.value % 7   // Sunday = 0
    val daysInMonth = ym.lengthOfMonth()

    // Always 6 rows so every month page has the same height.
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0 until 6) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 7) {
                    val dayNum = row * 7 + col - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (dayNum in 1..daysInMonth) {
                            val date = ym.atDay(dayNum)
                            val dayEntries = byDate[date]
                            val status = when {
                                dayEntries != null -> DayStatus.Done
                                firstDate != null && !date.isBefore(firstDate) && date.isBefore(today) -> DayStatus.Missed
                                else -> DayStatus.Neutral
                            }
                            DayCell(
                                day = dayNum,
                                status = status,
                                isToday = date == today,
                                onClick = dayEntries?.let { { onOpenEntry(it.first().id) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, status: DayStatus, isToday: Boolean, onClick: (() -> Unit)?) {
    val shape = RoundedCornerShape(12.dp)
    val bg = when (status) {
        DayStatus.Done -> GreenDay
        DayStatus.Missed -> RedDay
        DayStatus.Neutral -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (status == DayStatus.Neutral) MaterialTheme.colorScheme.onSurfaceVariant else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(bg)
            .then(if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(day.toString(), color = fg, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = 6.dp).size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
