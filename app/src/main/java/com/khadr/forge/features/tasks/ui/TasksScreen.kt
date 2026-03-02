package com.khadr.forge.features.tasks.ui

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
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.*
import com.khadr.forge.features.tasks.data.TaskEntity
import com.khadr.forge.features.tasks.data.TaskPriorityEntity
import androidx.compose.ui.res.stringResource
import com.khadr.forge.R
import com.khadr.forge.core.util.LocalForgeFormatter
import com.khadr.forge.ui.theme.*

// ─── Tasks Screen ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val bg        = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ───────────────────────────────────────────────────────
            TopBar(
                pendingCount = uiState.tasks.count { !it.isDone },
                onClearDone  = viewModel::clearCompleted
            )

            // ── Filters ───────────────────────────────────────────────────────
            FilterRow(selected = uiState.filter, onSelect = viewModel::setFilter)

            HorizontalDivider(
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(4.dp))

            // ── List ──────────────────────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(
                            color       = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            modifier    = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                uiState.tasks.isEmpty() -> EmptyState(filter = uiState.filter)
                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(
                            start  = 24.dp,
                            end    = 24.dp,
                            top    = 4.dp,
                            bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(items = uiState.tasks, key = { it.id }) { task ->
                            TaskItem(
                                task     = task,
                                onToggle = { viewModel.toggleTask(task.id, task.isDone) },
                                onEdit   = { viewModel.openEditSheet(task) },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
                        }
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
                contentDescription = "Add task",
                tint               = MaterialTheme.colorScheme.background,
                modifier           = Modifier.size(20.dp)
            )
        }
    }

    // ── Bottom Sheet ──────────────────────────────────────────────────────────
    if (uiState.isAddSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeSheet,
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = MaterialTheme.colorScheme.background,
            dragHandle       = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 6.dp)
                        .width(32.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                )
            }
        ) {
            TaskFormSheet(
                formState        = formState,
                isEditing        = uiState.editingTask != null,
                onTitleChange    = viewModel::onTitleChange,
                onDescChange     = viewModel::onDescChange,
                onCategoryChange = viewModel::onCategoryChange,
                onPriorityChange = viewModel::onPriorityChange,
                onSave           = viewModel::saveTask,
                onDismiss        = viewModel::closeSheet
            )
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────
@Composable
private fun TopBar(pendingCount: Int, onClearDone: () -> Unit) {
    val fmt = LocalForgeFormatter.current
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text  = stringResource(R.string.tasks),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (pendingCount > 0) {
                Text(
                    text  = stringResource(R.string.tasks_remaining, fmt.number(pendingCount)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        // Clear done button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClearDone),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Lucide.Trash2,
                contentDescription = "Clear completed",
                modifier           = Modifier.size(15.dp),
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// ─── Filter Row ───────────────────────────────────────────────────────────────
@Composable
private fun FilterRow(selected: TaskFilter, onSelect: (TaskFilter) -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TaskFilter.entries.forEach { filter ->
            val isSelected = selected == filter
            val label = when (filter) {
                TaskFilter.ALL     -> stringResource(R.string.filter_all)
                TaskFilter.PENDING -> stringResource(R.string.filter_pending)
                TaskFilter.DONE    -> stringResource(R.string.filter_done)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = { onSelect(filter) })
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.background
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ─── Task Item ────────────────────────────────────────────────────────────────
@Composable
private fun TaskItem(
    task     : TaskEntity,
    onToggle : () -> Unit,
    onEdit   : () -> Unit,
    onDelete : () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .border(
                        1.5.dp,
                        if (task.isDone) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        RoundedCornerShape(5.dp)
                    )
                    .background(if (task.isDone) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onToggle),
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
                    color = if (task.isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.onBackground
                )
                if (task.category.isNotBlank() || task.dueDate != null) {
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (task.category.isNotBlank()) MetaTag(text = task.category, icon = Lucide.Tag)
                        if (task.dueDate != null)       MetaTag(text = task.dueDate.toString(), icon = Lucide.Calendar)
                    }
                }
            }

            // Priority dot
            val alpha = when (task.priority) {
                TaskPriorityEntity.HIGH   -> 0.85f
                TaskPriorityEntity.MEDIUM -> 0.4f
                TaskPriorityEntity.LOW    -> 0.18f
            }
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground.copy(alpha = alpha)))
        }

        // Expand actions
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InlineAction(modifier = Modifier.weight(1f), icon = Lucide.Pencil, label = stringResource(R.string.edit),   onClick = onEdit)
                InlineAction(modifier = Modifier.weight(1f), icon = Lucide.Trash2, label = stringResource(R.string.delete), onClick = onDelete)
            }
        }
    }
}

@Composable
private fun MetaTag(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f))
    }
}

@Composable
private fun InlineAction(modifier: Modifier, icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        Spacer(Modifier.width(5.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
    }
}

// ─── Task Form Sheet ──────────────────────────────────────────────────────────
@Composable
private fun TaskFormSheet(
    formState        : TaskFormState,
    isEditing        : Boolean,
    onTitleChange    : (String) -> Unit,
    onDescChange     : (String) -> Unit,
    onCategoryChange : (String) -> Unit,
    onPriorityChange : (TaskPriorityEntity) -> Unit,
    onSave           : () -> Unit,
    onDismiss        : () -> Unit
) {
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
            Text(
                text  = if (isEditing) stringResource(R.string.edit_task) else stringResource(R.string.add_task),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Lucide.X, contentDescription = "Close", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }

        FlatTextField(value = formState.title,       onValueChange = onTitleChange,    placeholder = stringResource(R.string.task_title),              icon = Lucide.Type,      imeAction = ImeAction.Next)
        FlatTextField(value = formState.description, onValueChange = onDescChange,     placeholder = stringResource(R.string.description_optional),  icon = Lucide.AlignLeft, singleLine = false, imeAction = ImeAction.Next)
        FlatTextField(value = formState.category,    onValueChange = onCategoryChange, placeholder = stringResource(R.string.category_hint), icon = Lucide.Tag,    imeAction = ImeAction.Done)

        // Priority
        Text(text = "Priority", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskPriorityEntity.entries.forEach { p ->
                PriorityPill(priority = p, selected = formState.priority == p, onClick = { onPriorityChange(p) })
            }
        }

        Spacer(Modifier.height(2.dp))

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
                text  = if (isEditing) stringResource(R.string.save_changes) else stringResource(R.string.add_task),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.background
            )
        }
    }
}

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
            placeholder   = {
                Text(text = placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f))
            },
            textStyle     = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            singleLine    = singleLine,
            maxLines      = if (singleLine) 1 else 3,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PriorityPill(priority: TaskPriorityEntity, selected: Boolean, onClick: () -> Unit) {
    val label = when (priority) { TaskPriorityEntity.HIGH -> stringResource(R.string.priority_high); TaskPriorityEntity.MEDIUM -> stringResource(R.string.priority_medium); TaskPriorityEntity.LOW -> stringResource(R.string.priority_low) }
    val dotAlpha = when (priority) { TaskPriorityEntity.HIGH -> 0.85f; TaskPriorityEntity.MEDIUM -> 0.45f; TaskPriorityEntity.LOW -> 0.18f }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(6.dp).clip(CircleShape).background(
                (if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground).copy(alpha = dotAlpha)
            )
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────
@Composable
private fun EmptyState(filter: TaskFilter) {
    val (icon, msg) = when (filter) {
        TaskFilter.PENDING -> Lucide.CircleCheck to stringResource(R.string.all_caught_up)
        TaskFilter.DONE    -> Lucide.Circle      to stringResource(R.string.no_completed_tasks)
        TaskFilter.ALL     -> Lucide.ListTodo    to stringResource(R.string.no_tasks_yet)
    }
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            Text(text = msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}