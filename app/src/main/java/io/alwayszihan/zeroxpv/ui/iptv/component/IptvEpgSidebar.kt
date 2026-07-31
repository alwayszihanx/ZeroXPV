package io.alwayszihan.zeroxpv.ui.iptv.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import io.alwayszihan.zeroxpv.ui.icons.Icon
import io.alwayszihan.zeroxpv.ui.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.alwayszihan.zeroxpv.ui.iptv.parser.EpgProgram
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun IptvEpgSidebar(
    visible: Boolean,
    programs: List<EpgProgram>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf<String?>(null) }
    
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("MM/dd", locale) }
    val availableDates = remember(programs) {
        programs.map { program ->
            dateFormat.format(Date(program.startTime))
        }.distinct().sorted()
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300)
        ) + expandHorizontally(
            expandFrom = Alignment.End,
            animationSpec = tween(300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(300)
        ) + shrinkHorizontally(
            shrinkTowards = Alignment.End,
            animationSpec = tween(300)
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(300.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Program List",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clipToBounds()
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedDate == null,
                                onClick = { selectedDate = null },
                                label = { Text("All") }
                            )
                        }

                        items(availableDates) { date ->
                            FilterChip(
                                selected = selectedDate == date,
                                onClick = { selectedDate = date },
                                label = { Text(date) }
                            )
                        }
                    }
                }

                IptvEpgProgramList(
                    programs = programs,
                    selectedDate = selectedDate,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun IptvEpgProgramList(
    programs: List<EpgProgram>,
    selectedDate: String?,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true
) {
    val currentTime = System.currentTimeMillis()
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("MM/dd", locale) }
    val timeFormat = remember(locale) { SimpleDateFormat("HH:mm", locale) }
    
    val filteredPrograms = remember(programs, selectedDate) {
        val upcomingPrograms = programs.filter { it.stopTime > currentTime }
        if (selectedDate == null) {
            upcomingPrograms.take(20)
        } else {
            upcomingPrograms.filter { program ->
                dateFormat.format(Date(program.startTime)) == selectedDate
            }.take(20)
        }
    }
    
    if (scrollable) {
        LazyColumn(
            modifier = modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(filteredPrograms.size) { index ->
                IptvProgramItem(
                    program = filteredPrograms[index],
                    index = index,
                    programCount = filteredPrograms.size,
                    currentTime = currentTime,
                    dateFormat = dateFormat,
                    timeFormat = timeFormat
                )
            }
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            filteredPrograms.forEachIndexed { index, program ->
                IptvProgramItem(
                    program = program,
                    index = index,
                    programCount = filteredPrograms.size,
                    currentTime = currentTime,
                    dateFormat = dateFormat,
                    timeFormat = timeFormat
                )
            }
        }
    }
}

@Composable
private fun IptvProgramItem(
    program: EpgProgram,
    index: Int,
    programCount: Int,
    currentTime: Long,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat
) {
    val startTime = Date(program.startTime)
    val endTime = Date(program.stopTime)
    val startTimeStr = timeFormat.format(startTime)
    val endTimeStr = timeFormat.format(endTime)
    val dateStr = dateFormat.format(startTime)
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val dotColor = MaterialTheme.colorScheme.primary
    val isCurrentProgram = currentTime in program.startTime..program.stopTime
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(56.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (programCount > 1) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val centerX = size.width / 2
                    val dotY = 12.dp.toPx()
                    
                    if (index > 0) {
                        drawLine(
                            color = lineColor,
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, dotY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    if (index < programCount - 1) {
                        drawLine(
                            color = lineColor,
                            start = Offset(centerX, dotY),
                            end = Offset(centerX, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 9.dp)
                    .size(6.dp)
                    .background(
                        color = if (isCurrentProgram) MaterialTheme.colorScheme.primary else dotColor,
                        shape = CircleShape
                    )
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, top = 4.dp, bottom = 8.dp)
        ) {
            Text(
                text = program.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrentProgram) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isCurrentProgram) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$dateStr $startTimeStr - $endTimeStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
