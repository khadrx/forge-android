package com.khadr.forge.features.dashboard.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.composables.icons.lucide.*
import com.khadr.forge.R
import com.khadr.forge.core.util.LocalForgeFormatter
import com.khadr.forge.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Dashboard Screen ─────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DashboardScreen(
    onNavigateToBudget    : () -> Unit = {},
    onNavigateToTasks     : () -> Unit = {},
    onNavigateToSchedule  : () -> Unit = {},
    onNavigateToReminders : () -> Unit = {},
    onNavigateToSettings  : () -> Unit = {},
    viewModel             : DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val bg  = MaterialTheme.colorScheme.background
    val fmt = LocalForgeFormatter.current

    LazyColumn(
        modifier       = Modifier
            .fillMaxSize()
            .background(bg),
        contentPadding = PaddingValues(
            start  = 24.dp,
            end    = 24.dp,
            top    = 56.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item { Header(greeting = uiState.greeting, onSettings = {}) }
        item {
            StatsRow(
                tasksRemaining  = uiState.tasksRemaining,
                budgetLeft      = uiState.budgetLeft,
                eventsToday     = uiState.eventsToday,
                onTasksClick    = onNavigateToTasks,
                onBudgetClick   = onNavigateToBudget,
                onScheduleClick = onNavigateToSchedule,
                fmt             = fmt
            )
        }
        item { Divider() }
        item {
            SectionLabel(stringResource(R.string.today_focus))
            Spacer(Modifier.height(12.dp))
            FocusCard(topTask = uiState.topTask)
        }
        item {
            SectionHeader(title = stringResource(R.string.nav_schedule), icon = Lucide.CalendarDays, onSeeAll = onNavigateToSchedule)
            Spacer(Modifier.height(12.dp))
            ScheduleRow(events = uiState.upcomingEvents)
        }
        item {
            SectionHeader(title = stringResource(R.string.tasks), icon = Lucide.CircleCheck, onSeeAll = onNavigateToTasks)
            Spacer(Modifier.height(12.dp))
        }
        items(uiState.recentTasks.take(3)) { task ->
            TaskRow(task = task, onToggle = { viewModel.toggleTask(task.id) })
            if (task != uiState.recentTasks.take(3).last()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f),
                    modifier = Modifier.padding(start = 36.dp)
                )
            }
        }
        item {
            SectionHeader(title = stringResource(R.string.budget), icon = Lucide.Wallet, onSeeAll = onNavigateToBudget)
            Spacer(Modifier.height(12.dp))
            BudgetCard(summary = uiState.budgetSummary, fmt = fmt)
        }
        item {
            SectionHeader(title = stringResource(R.string.reminders), icon = Lucide.Bell, onSeeAll = onNavigateToReminders)
            Spacer(Modifier.height(12.dp))
        }
        items(uiState.upcomingReminders.take(2)) { reminder ->
            ReminderRow(reminder = reminder)
            if (reminder != uiState.upcomingReminders.take(2).last()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun Header(greeting: String, onSettings: () -> Unit) {
    val config    = LocalConfiguration.current
    val sysLocale = remember(config) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            config.locales.get(0) ?: java.util.Locale.getDefault()
        else @Suppress("DEPRECATION") config.locale ?: java.util.Locale.getDefault()
    }
    val today   = LocalDate.now()
    val dateStr = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", sysLocale))
    val greetingText = when (greeting) {
        "greeting_morning"   -> stringResource(R.string.greeting_morning)
        "greeting_afternoon" -> stringResource(R.string.greeting_afternoon)
        "greeting_evening"   -> stringResource(R.string.greeting_evening)
        else                 -> greeting
    }

    Column {
        Text(
            text  = greetingText,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = dateStr,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}

// ─── Stats Row ────────────────────────────────────────────────────────────────
@Composable
private fun StatsRow(
    tasksRemaining  : Int,
    budgetLeft      : Double,
    eventsToday     : Int,
    onTasksClick    : () -> Unit,
    onBudgetClick   : () -> Unit,
    onScheduleClick : () -> Unit,
    fmt             : com.khadr.forge.core.util.ForgeFormatter
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile(
            modifier = Modifier.weight(1f),
            value    = fmt.number(tasksRemaining),
            label    = stringResource(R.string.tasks_left),
            icon     = Lucide.CircleCheck,
            onClick  = onTasksClick
        )
        StatTile(
            modifier = Modifier.weight(1f),
            value    = fmt.currency(budgetLeft),
            label    = stringResource(R.string.budget),
            icon     = Lucide.Wallet,
            onClick  = onBudgetClick
        )
        StatTile(
            modifier = Modifier.weight(1f),
            value    = fmt.number(eventsToday),
            label    = stringResource(R.string.events),
            icon     = Lucide.CalendarDays,
            onClick  = onScheduleClick
        )
    }
}

@Composable
private fun StatTile(
    modifier : Modifier,
    value    : String,
    label    : String,
    icon     : ImageVector,
    onClick  : () -> Unit
) {
    FlatCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
            }
            Text(value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = GeistMonoFamily),
                color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

// ─── Focus Card ───────────────────────────────────────────────────────────────
@Composable
private fun FocusCard(topTask: DashboardTask?) {
    FlatCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Lucide.Crosshair,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp),
                    tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = stringResource(R.string.top_priority),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text     = topTask?.title ?: stringResource(R.string.no_tasks_today),
                    style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color    = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2
                )
                if (topTask != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = topTask.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
            }
            if (topTask != null) {
                Icon(
                    imageVector        = Lucide.ChevronRight,
                    contentDescription = null,
                    modifier           = Modifier.size(16.dp),
                    tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                )
            }
        }
    }
}

// ─── Schedule Row ─────────────────────────────────────────────────────────────
@Composable
private fun ScheduleRow(events: List<DashboardEvent>) {
    if (events.isEmpty()) {
        EmptyHint(stringResource(R.string.nothing_scheduled))
        return
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding        = PaddingValues(horizontal = 2.dp)
    ) {
        items(events) { event ->
            Box(
                modifier = Modifier.width(138.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Text(
                            text  = "%02d:%02d".format(event.startTime.hour, event.startTime.minute),
                            style = ForgeTextStyles.MoneySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    Text(
                        text     = event.title,
                        style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color    = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

// ─── Task Row ─────────────────────────────────────────────────────────────────
@Composable
private fun TaskRow(task: DashboardTask, onToggle: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .border(
                    width = 1.5.dp,
                    color = if (task.isDone)
                        MaterialTheme.colorScheme.onBackground
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(5.dp)
                )
                .background(
                    if (task.isDone) MaterialTheme.colorScheme.onBackground
                    else Color.Transparent
                )
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onToggle
                ),
            contentAlignment = Alignment.Center
        ) {
            if (task.isDone) {
                Icon(
                    imageVector        = Lucide.Check,
                    contentDescription = null,
                    modifier           = Modifier.size(11.dp),
                    tint               = MaterialTheme.colorScheme.background
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = task.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (task.isDone) FontWeight.Normal else FontWeight.Medium
                ),
                color = if (task.isDone)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                else
                    MaterialTheme.colorScheme.onBackground
            )
            if (task.category.isNotBlank()) {
                Text(
                    text  = task.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        // Priority indicator
        val priorityColor = when (task.priority) {
            "HIGH"   -> Color(0xFFEF4444)        // red
            "MEDIUM" -> Color(0xFFF59E0B)        // amber
            else     -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)
        }
        Box(Modifier.size(7.dp).clip(CircleShape).background(priorityColor))
    }
}

// ─── Budget Card ──────────────────────────────────────────────────────────────
@Composable
private fun BudgetCard(summary: BudgetSummary, fmt: com.khadr.forge.core.util.ForgeFormatter) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Balance — outline card (no fill)
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(3.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text  = stringResource(R.string.balance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = fmt.currency(summary.balance),
                        style = ForgeTextStyles.MoneyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Icon(Lucide.TrendingUp, null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
            }
        }

        // Progress bar
        BudgetBar(spent = summary.spent, total = summary.income, fmt = fmt)

        // Income / Expense sub-cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BudgetLeg(Modifier.weight(1f), stringResource(R.string.income), summary.income, Lucide.ArrowDownLeft, ForgeSuccess, fmt)
            BudgetLeg(Modifier.weight(1f), stringResource(R.string.spent),  summary.spent,  Lucide.ArrowUpRight,  ForgeExpense, fmt)
        }
    }
}

@Composable
private fun BudgetBar(spent: Double, total: Double, fmt: com.khadr.forge.core.util.ForgeFormatter) {
    val progress = if (total > 0) (spent / total).coerceIn(0.0, 1.0).toFloat() else 0f
    val animated by animateFloatAsState(targetValue = progress, animationSpec = tween(700, easing = FastOutSlowInEasing), label = "budget")

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text  = stringResource(R.string.spent_pct, fmt.percent(animated)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
            Text(
                text  = stringResource(R.string.left_pct, fmt.percent(1f - animated)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
            )
        }
    }
}

@Composable
private fun BudgetLeg(modifier: Modifier, label: String, amount: Double, icon: ImageVector, iconTint: androidx.compose.ui.graphics.Color, fmt: com.khadr.forge.core.util.ForgeFormatter) {
    Row(
        modifier              = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.13f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier         = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(14.dp), tint = iconTint)
        }
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            Text(text = fmt.currency(amount), style = ForgeTextStyles.MoneySmall, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

// ─── Reminder Row ─────────────────────────────────────────────────────────────
@Composable
private fun ReminderRow(reminder: DashboardReminder) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Lucide.Bell, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = reminder.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onBackground)
            Text(text = reminder.timeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, icon: ImageVector, onSeeAll: () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(
                text  = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Text(
            text     = stringResource(R.string.see_all),
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onSeeAll)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text  = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    )
}

// ─── Divider ──────────────────────────────────────────────────────────────────
@Composable
private fun Divider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
@Composable
private fun EmptyHint(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

// ─── FlatCard ─────────────────────────────────────────────────────────────────
//  Local version: bordered, no shadow, clean fill
@Composable
private fun FlatCard(
    modifier : Modifier        = Modifier,
    onClick  : (() -> Unit)?   = null,
    content  : @Composable () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val base  = modifier
        .clip(shape)
        .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), shape)

    Box(
        modifier = if (onClick != null)
            base.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
        else base
    ) { content() }
}

// ─── FlatIconButton ───────────────────────────────────────────────────────────
@Composable
private fun FlatIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}