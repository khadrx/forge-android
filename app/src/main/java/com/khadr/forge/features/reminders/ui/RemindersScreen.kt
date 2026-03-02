package com.khadr.forge.features.reminders.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.*
import com.khadr.forge.R
import com.khadr.forge.core.util.LocalForgeFormatter
import com.khadr.forge.features.reminders.data.ReminderEntity
import com.khadr.forge.features.reminders.data.RepeatInterval
import com.khadr.forge.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ─── Reminders Screen ─────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val fmt       = LocalForgeFormatter.current
    val isRtl     = LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl

    // ── Notification permission (Android 13+) ─────────────────────────────────
    var hasNotifPermission by remember { mutableStateOf(true) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotifPermission = granted }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) {
            val ctx = androidx.compose.ui.platform.LocalContext
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ───────────────────────────────────────────────────────
            Text(
                text     = stringResource(R.string.reminders),
                style    = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color    = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 16.dp)
            )

            // ── Permission Banner ─────────────────────────────────────────────
            if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionBanner(
                    onGrant = {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
            }

            // ── Tab Row ───────────────────────────────────────────────────────
            ReminderTabRow(
                selected = uiState.tab,
                onSelect = viewModel::selectTab,
                upcomingCount = uiState.upcoming.size,
                pastCount     = uiState.past.size
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))

            // ── Content ───────────────────────────────────────────────────────
            val list = if (uiState.tab == ReminderTab.UPCOMING) uiState.upcoming else uiState.past

            when {
                uiState.isLoading -> LoadingBox()
                list.isEmpty()    -> EmptyReminders()
                else -> LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start  = 24.dp,
                        end    = 24.dp,
                        top    = 12.dp,
                        bottom = 100.dp
                    )
                ) {
                    // Group by date
                    val grouped = list.groupBy { it.date }
                    grouped.forEach { (date, items) ->
                        item {
                            DateHeader(date = date, fmt = fmt)
                        }
                        items(items = items, key = { it.id }) { reminder ->
                            ReminderItem(
                                reminder  = reminder,
                                fmt       = fmt,
                                onToggle  = { viewModel.toggleDone(reminder) },
                                onEdit    = { viewModel.openEditSheet(reminder) },
                                onDelete  = { viewModel.deleteReminder(reminder.id) }
                            )
                            HorizontalDivider(
                                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f),
                                modifier = Modifier.padding(start = 44.dp)
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }

        // ── FAB ───────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onBackground)
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = viewModel::openAddSheet
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Lucide.Plus,
                contentDescription = stringResource(R.string.add_reminder),
                tint               = MaterialTheme.colorScheme.background,
                modifier           = Modifier.size(20.dp)
            )
        }
    }

    // ── Bottom Sheet ──────────────────────────────────────────────────────────
    if (uiState.isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeSheet,
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = MaterialTheme.colorScheme.background,
            dragHandle       = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 6.dp)
                        .width(32.dp).height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                )
            }
        ) {
            ReminderFormSheet(
                formState     = formState,
                isEditing     = uiState.editingItem != null,
                onTitleChange = viewModel::onTitleChange,
                onNoteChange  = viewModel::onNoteChange,
                onDateChange  = viewModel::onDateChange,
                onTimeChange  = viewModel::onTimeChange,
                onRepeatChange= viewModel::onRepeatChange,
                onSave        = viewModel::saveReminder,
                onDismiss     = viewModel::closeSheet
            )
        }
    }
}

// ─── Permission Banner ────────────────────────────────────────────────────────
@Composable
private fun PermissionBanner(onGrant: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Lucide.Bell, null, Modifier.size(16.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(
            text     = stringResource(R.string.notification_permission),
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.onBackground)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onGrant)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(stringResource(R.string.grant_permission), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.background)
        }
    }
}

// ─── Tab Row ──────────────────────────────────────────────────────────────────
@Composable
private fun ReminderTabRow(
    selected      : ReminderTab,
    onSelect      : (ReminderTab) -> Unit,
    upcomingCount : Int,
    pastCount     : Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReminderTab.entries.forEach { tab ->
            val isSelected = selected == tab
            val label      = stringResource(if (tab == ReminderTab.UPCOMING) R.string.upcoming else R.string.past)
            val count      = if (tab == ReminderTab.UPCOMING) upcomingCount else pastCount

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSelect(tab) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.background
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.background.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text  = count.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = GeistMonoFamily),
                            color = if (isSelected) MaterialTheme.colorScheme.background
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}

// ─── Date Header ─────────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DateHeader(
    date : LocalDate,
    fmt  : com.khadr.forge.core.util.ForgeFormatter
) {
    val today     = LocalDate.now()
    val tomorrow  = today.plusDays(1)
    val dayLabel  = when (date) {
        today    -> stringResource(R.string.today)
        tomorrow -> stringResource(R.string.tomorrow)
        else     -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    }
    val dateLabel = "${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${fmt.number(date.dayOfMonth)}"

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text  = dayLabel,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text  = dateLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = GeistMonoFamily),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
    }
}

// ─── Reminder Item ────────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ReminderItem(
    reminder : ReminderEntity,
    fmt      : com.khadr.forge.core.util.ForgeFormatter,
    onToggle : () -> Unit,
    onEdit   : () -> Unit,
    onDelete : () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val timeFmt   = DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.getDefault())
    val now       = java.time.LocalDateTime.now(ZoneId.systemDefault())
    val triggerDt = reminder.date.atTime(reminder.time)
    val isOverdue = !reminder.isDone && triggerDt.isBefore(now)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { expanded = !expanded }
            .padding(vertical = 12.dp),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    if (reminder.isDone) MaterialTheme.colorScheme.onBackground
                    else Color.Transparent
                )
                .border(
                    1.5.dp,
                    if (reminder.isDone) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(5.dp)
                )
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (reminder.isDone) {
                Icon(Lucide.Check, null, Modifier.size(11.dp), MaterialTheme.colorScheme.background)
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text  = reminder.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (reminder.isDone) FontWeight.Normal else FontWeight.Medium
                ),
                color = if (reminder.isDone)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                else
                    MaterialTheme.colorScheme.onBackground
            )
            if (reminder.note.isNotBlank()) {
                Text(
                    text     = reminder.note,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    maxLines = 1
                )
            }

            // Time + repeat row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Lucide.Clock, null, Modifier.size(11.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f))
                Text(
                    text  = reminder.time.format(timeFmt),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = GeistMonoFamily),
                    color = if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
                if (isOverdue) {
                    OverduePill()
                }
                if (reminder.repeat != RepeatInterval.NONE) {
                    RepeatPill(reminder.repeat)
                }
            }

            // Expanded actions
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Row(
                    modifier              = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InlineAction(icon = Lucide.Pencil, label = stringResource(R.string.edit),   onClick = onEdit)
                    InlineAction(icon = Lucide.Trash2, label = stringResource(R.string.delete), onClick = onDelete)
                }
            }
        }

        // Chevron
        Icon(
            imageVector        = if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
            contentDescription = null,
            modifier           = Modifier.padding(top = 2.dp).size(14.dp),
            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun OverduePill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text  = stringResource(R.string.overdue),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun RepeatPill(repeat: RepeatInterval) {
    val label = stringResource(
        when (repeat) {
            RepeatInterval.DAILY   -> R.string.repeat_daily
            RepeatInterval.WEEKLY  -> R.string.repeat_weekly
            RepeatInterval.MONTHLY -> R.string.repeat_monthly
            RepeatInterval.NONE    -> R.string.repeat_none
        }
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(Lucide.RefreshCw, null, Modifier.size(9.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
    }
}

// ─── Reminder Form Sheet ──────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderFormSheet(
    formState      : ReminderFormState,
    isEditing      : Boolean,
    onTitleChange  : (String) -> Unit,
    onNoteChange   : (String) -> Unit,
    onDateChange   : (LocalDate) -> Unit,
    onTimeChange   : (LocalTime) -> Unit,
    onRepeatChange : (RepeatInterval) -> Unit,
    onSave         : () -> Unit,
    onDismiss      : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = stringResource(if (isEditing) R.string.edit_reminder else R.string.new_reminder),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            SheetCloseButton(onClick = onDismiss)
        }

        // Title
        FlatTextField(
            value         = formState.title,
            onValueChange = onTitleChange,
            placeholder   = stringResource(R.string.reminder_title),
            icon          = Lucide.Bell
        )

        // Note
        FlatTextField(
            value         = formState.note,
            onValueChange = onNoteChange,
            placeholder   = stringResource(R.string.reminder_note),
            icon          = Lucide.AlignLeft,
            singleLine    = false
        )

        // Date picker row
        DatePickerRow(date = formState.date, onDateChange = onDateChange)

        // Time picker row
        TimePickerRow(time = formState.time, onTimeChange = onTimeChange)

        // Repeat
        Text(
            text  = stringResource(R.string.reminder_repeat),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        RepeatSelector(selected = formState.repeat, onSelect = onRepeatChange)

        Spacer(Modifier.height(4.dp))

        // Save
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onBackground)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onSave),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = stringResource(if (isEditing) R.string.save_changes else R.string.add_reminder),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.background
            )
        }
    }
}

// ─── Date Picker Row ──────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerRow(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d yyyy", Locale.ENGLISH)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { showPicker = true }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Lucide.Calendar, null, Modifier.size(15.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            Text(stringResource(R.string.reminder_date), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Text(date.format(dateFmt), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = GeistMonoFamily, fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onBackground)
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        onDateChange(picked)
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.onBackground) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.background
            )
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ─── Time Picker Row ──────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerRow(time: LocalTime, onTimeChange: (LocalTime) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val timeFmt    = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { showPicker = true }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Lucide.Clock, null, Modifier.size(15.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            Text(stringResource(R.string.reminder_time), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Text(time.format(timeFmt), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = GeistMonoFamily, fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onBackground)
    }

    if (showPicker) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            containerColor   = MaterialTheme.colorScheme.background,
            shape            = RoundedCornerShape(16.dp),
            title            = null,
            text             = {
                Box(Modifier.fillMaxWidth(), Alignment.Center) {
                    TimePicker(
                        state  = state,
                        colors = TimePickerDefaults.colors(
                            clockDialColor                          = MaterialTheme.colorScheme.surfaceVariant,
                            clockDialSelectedContentColor           = MaterialTheme.colorScheme.background,
                            clockDialUnselectedContentColor         = MaterialTheme.colorScheme.onSurface,
                            selectorColor                           = MaterialTheme.colorScheme.onBackground,
                            containerColor                          = MaterialTheme.colorScheme.background,
                            periodSelectorBorderColor               = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            timeSelectorSelectedContainerColor      = MaterialTheme.colorScheme.onBackground,
                            timeSelectorUnselectedContainerColor    = MaterialTheme.colorScheme.surfaceVariant,
                            timeSelectorSelectedContentColor        = MaterialTheme.colorScheme.background,
                            timeSelectorUnselectedContentColor      = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onTimeChange(LocalTime.of(state.hour, state.minute)); showPicker = false }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.onBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        )
    }
}

// ─── Repeat Selector ──────────────────────────────────────────────────────────
@Composable
private fun RepeatSelector(
    selected : RepeatInterval,
    onSelect : (RepeatInterval) -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RepeatInterval.entries.forEach { interval ->
            val label = stringResource(
                when (interval) {
                    RepeatInterval.NONE    -> R.string.repeat_none
                    RepeatInterval.DAILY   -> R.string.repeat_daily
                    RepeatInterval.WEEKLY  -> R.string.repeat_weekly
                    RepeatInterval.MONTHLY -> R.string.repeat_monthly
                }
            )
            val isSelected = selected == interval
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        RoundedCornerShape(9.dp)
                    )
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSelect(interval) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.background
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
@Composable
private fun FlatTextField(
    value         : String,
    onValueChange : (String) -> Unit,
    placeholder   : String,
    icon          : ImageVector,
    singleLine    : Boolean   = true,
    imeAction     : ImeAction = ImeAction.Next
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, Modifier.size(15.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        TextField(
            value           = value,
            onValueChange   = onValueChange,
            placeholder     = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)) },
            textStyle       = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            singleLine      = singleLine,
            maxLines        = if (singleLine) 1 else 3,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            colors          = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun InlineAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, null, Modifier.size(12.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}

@Composable
private fun SheetCloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Lucide.X, stringResource(R.string.close), Modifier.size(15.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
private fun EmptyReminders() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Lucide.Bell, null, Modifier.size(32.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Text(stringResource(R.string.no_reminders), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}