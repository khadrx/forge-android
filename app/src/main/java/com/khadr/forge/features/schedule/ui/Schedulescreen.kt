package com.khadr.forge.features.schedule.ui

import android.os.Build
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.*
import com.khadr.forge.core.util.LocalForgeFormatter
import com.khadr.forge.features.schedule.data.EventEntity
import androidx.compose.ui.res.stringResource
import com.khadr.forge.R
import com.khadr.forge.core.util.LocalForgeFormatter
import com.khadr.forge.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// ─── Schedule Screen ──────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val fmt       = LocalForgeFormatter.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ───────────────────────────────────────────────────────
            ScheduleTopBar(selectedDate = uiState.selectedDate, fmt = fmt)

            // ── Week Strip ────────────────────────────────────────────────────
            WeekStrip(
                selectedDate    = uiState.selectedDate,
                datesWithEvents = uiState.datesWithEvents,
                onSelectDate    = viewModel::selectDate
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))

            // ── Events List ───────────────────────────────────────────────────
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.eventsToday.isEmpty() -> EmptyDay(date = uiState.selectedDate)
                else -> EventsList(
                    events    = uiState.eventsToday,
                    fmt       = fmt,
                    onEdit    = viewModel::openEditSheet,
                    onDelete  = viewModel::deleteEvent
                )
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
                contentDescription = stringResource(R.string.add_event),
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
            EventFormSheet(
                formState     = formState,
                selectedDate  = uiState.selectedDate,
                isEditing     = uiState.editingEvent != null,
                onTitleChange = viewModel::onTitleChange,
                onNoteChange  = viewModel::onNoteChange,
                onStartTimeChange = viewModel::onStartTimeChange,
                onEndTimeChange   = viewModel::onEndTimeChange,
                onToggleEndTime   = viewModel::onToggleEndTime,
                onSave        = viewModel::saveEvent,
                onDismiss     = viewModel::closeSheet
            )
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ScheduleTopBar(selectedDate: LocalDate, fmt: com.khadr.forge.core.util.ForgeFormatter) {
    val monthYear = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text  = stringResource(R.string.schedule),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text  = monthYear,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        // Today button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(9.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text  = stringResource(R.string.today),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// ─── Week Strip ───────────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun WeekStrip(
    selectedDate    : LocalDate,
    datesWithEvents : Set<LocalDate>,
    onSelectDate    : (LocalDate) -> Unit
) {
    // Always show 3 weeks centred on today
    val today     = LocalDate.now()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
    val days      = (0 until 21).map { weekStart.plusDays(it.toLong()) }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 7) // start at current week

    LazyRow(
        state         = listState,
        modifier      = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(days) { day ->
            DayCell(
                day             = day,
                isSelected      = day == selectedDate,
                isToday         = day == today,
                hasEvents       = day in datesWithEvents,
                onSelect        = { onSelectDate(day) }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DayCell(
    day        : LocalDate,
    isSelected : Boolean,
    isToday    : Boolean,
    hasEvents  : Boolean,
    onSelect   : () -> Unit
) {
    val dayName   = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    val dayNumber = day.dayOfMonth.toString()

    Column(
        modifier = Modifier
            .width(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.onBackground
                    isToday    -> MaterialTheme.colorScheme.surfaceVariant
                    else       -> Color.Transparent
                }
            )
            .border(
                width = 1.dp,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onBackground
                    isToday    -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    else       -> Color.Transparent
                },
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onSelect)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text  = dayName.take(1).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = when {
                isSelected -> MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                else       -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            }
        )
        Text(
            text      = dayNumber,
            style     = MaterialTheme.typography.titleSmall.copy(
                fontFamily = GeistMonoFamily,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
            ),
            color     = when {
                isSelected -> MaterialTheme.colorScheme.background
                isToday    -> MaterialTheme.colorScheme.onBackground
                else       -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            },
            textAlign = TextAlign.Center
        )
        // Event dot
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(
                    if (hasEvents)
                        (if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground).copy(alpha = 0.5f)
                    else Color.Transparent
                )
        )
    }
}

// ─── Events List ──────────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun EventsList(
    events   : List<EventEntity>,
    fmt      : com.khadr.forge.core.util.ForgeFormatter,
    onEdit   : (EventEntity) -> Unit,
    onDelete : (Long) -> Unit
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(items = events, key = { it.id }) { event ->
            EventItem(event = event, onEdit = { onEdit(event) }, onDelete = { onDelete(event.id) })
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun EventItem(
    event    : EventEntity,
    onEdit   : () -> Unit,
    onDelete : () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val timeFmt  = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { expanded = !expanded }
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Time column
            Column(
                modifier = Modifier.width(52.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text  = event.startTime.format(timeFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
                if (event.endTime != null) {
                    Text(
                        text  = event.endTime.format(timeFmt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    )
                }
            }

            // Vertical line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            )

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = event.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (event.note.isNotBlank()) {
                    Text(
                        text     = event.note,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        maxLines = 1
                    )
                }
            }

            Icon(
                imageVector        = if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                contentDescription = null,
                modifier           = Modifier.size(14.dp),
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            )
        }

        // Expanded actions
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(start = 68.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InlineAction(icon = Lucide.Pencil, label = stringResource(R.string.edit),   onClick = onEdit)
                InlineAction(icon = Lucide.Trash2, label = stringResource(R.string.delete), onClick = onDelete)
            }
        }
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
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}

// ─── Empty Day ────────────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun EmptyDay(date: LocalDate) {
    val isToday = date == LocalDate.now()
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = Lucide.CalendarDays, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Text(
                text  = if (isToday) stringResource(R.string.nothing_scheduled) else stringResource(R.string.no_events_day),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

// ─── Loading ──────────────────────────────────────────────────────────────────
@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator(
            color       = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier    = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
    }
}

// ─── Event Form Sheet ─────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun EventFormSheet(
    formState         : EventFormState,
    selectedDate      : LocalDate,
    isEditing         : Boolean,
    onTitleChange     : (String) -> Unit,
    onNoteChange      : (String) -> Unit,
    onStartTimeChange : (LocalTime) -> Unit,
    onEndTimeChange   : (LocalTime?) -> Unit,
    onToggleEndTime   : (Boolean) -> Unit,
    onSave            : () -> Unit,
    onDismiss         : () -> Unit
) {
    val dateFmt = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
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
            Column {
                Text(
                    text  = stringResource(if (isEditing) R.string.edit_event else R.string.new_event),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text  = selectedDate.format(dateFmt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            SheetCloseButton(onClick = onDismiss)
        }

        FlatTextField(value = formState.title, onValueChange = onTitleChange, placeholder = stringResource(R.string.event_title), icon = Lucide.Type)
        FlatTextField(value = formState.note,  onValueChange = onNoteChange,  placeholder = stringResource(R.string.note_optional), icon = Lucide.AlignLeft, singleLine = false)

        // Time pickers
        Text(text = stringResource(R.string.time), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))

        TimePickerRow(
            label    = stringResource(R.string.start),
            time     = formState.startTime,
            onChange = onStartTimeChange
        )

        // End time toggle
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "End time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Switch(
                checked         = formState.hasEndTime,
                onCheckedChange = onToggleEndTime,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor   = MaterialTheme.colorScheme.background,
                    checkedTrackColor   = MaterialTheme.colorScheme.onBackground,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        AnimatedVisibility(visible = formState.hasEndTime) {
            TimePickerRow(
                label    = stringResource(R.string.end),
                time     = formState.endTime ?: formState.startTime.plusHours(1),
                onChange = { onEndTimeChange(it) }
            )
        }

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
                text  = stringResource(if (isEditing) R.string.save_changes else R.string.add_event),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.background
            )
        }
    }
}

// ─── Time Picker Row (inline HH:MM picker) ────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TimePickerRow(
    label    : String,
    time     : LocalTime,
    onChange : (LocalTime) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val timeFmt    = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { showPicker = true }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(
            text  = time.format(timeFmt),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = GeistMonoFamily,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    if (showPicker) {
        TimePickerDialog(
            initialTime = time,
            onConfirm   = { onChange(it); showPicker = false },
            onDismiss   = { showPicker = false }
        )
    }
}

// ─── Time Picker Dialog ───────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime : LocalTime,
    onConfirm   : (LocalTime) -> Unit,
    onDismiss   : () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour   = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour      = false
    )

    AlertDialog(
        onDismissRequest   = onDismiss,
        containerColor     = MaterialTheme.colorScheme.background,
        shape              = RoundedCornerShape(16.dp),
        title              = null,
        text               = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(
                    state  = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor          = MaterialTheme.colorScheme.surfaceVariant,
                        clockDialSelectedContentColor   = MaterialTheme.colorScheme.background,
                        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        selectorColor           = MaterialTheme.colorScheme.onBackground,
                        containerColor          = MaterialTheme.colorScheme.background,
                        periodSelectorBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        timeSelectorSelectedContainerColor   = MaterialTheme.colorScheme.onBackground,
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        timeSelectorSelectedContentColor     = MaterialTheme.colorScheme.background,
                        timeSelectorUnselectedContentColor   = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.onBackground)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    )
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
@Composable
private fun FlatTextField(
    value         : String,
    onValueChange : (String) -> Unit,
    placeholder   : String,
    icon          : ImageVector,
    singleLine    : Boolean = true,
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
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        TextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)) },
            textStyle     = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            singleLine    = singleLine,
            maxLines      = if (singleLine) 1 else 3,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            colors        = TextFieldDefaults.colors(
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
private fun SheetCloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = Lucide.X, contentDescription = stringResource(R.string.close), modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}