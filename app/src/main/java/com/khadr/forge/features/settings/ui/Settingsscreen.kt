package com.khadr.forge.features.settings.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.*
import com.khadr.forge.R
import com.khadr.forge.core.preferences.NotifMode
import com.khadr.forge.core.preferences.TextSizeOption
import com.khadr.forge.ui.theme.*

// ─── Settings Screen ──────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack   : () -> Unit        = {}
) {
    val uiState = viewModel.uiState.collectAsState().value
    val config  = LocalConfiguration.current
    val isAr    = remember(config) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            config.locales.get(0)?.language == "ar"
        else config.locale?.language == "ar"
    } ?: false
    val context = LocalContext.current

    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.toString() ?: ""
            viewModel.setRingtoneUri(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header with back ──────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 24.dp, top = 56.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Lucide.ChevronLeft, null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text  = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ── Flat list of all settings ─────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // Dark mode toggle
            ToggleRow(
                icon     = Lucide.Moon,
                title    = stringResource(R.string.dark_mode),
                desc     = stringResource(R.string.dark_mode_desc),
                checked  = uiState.darkMode ?: false,
                onToggle = viewModel::setDarkMode,
                isFirst  = true
            )

            Divider()

            // Font picker — expandable inline
            var fontExpanded by remember { mutableStateOf(false) }
            ExpandableRow(
                icon      = Lucide.Type,
                title     = if (isAr) "الخط" else "Font",
                current   = if (isAr) uiState.fontChoice.displayNameAr else uiState.fontChoice.displayNameEn,
                expanded  = fontExpanded,
                onToggle  = { fontExpanded = !fontExpanded }
            )
            if (fontExpanded) {
                Column(Modifier.padding(start = 52.dp, end = 0.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppFont.entries.forEach { font ->
                        ChipOption(
                            label      = if (isAr) font.displayNameAr else font.displayNameEn,
                            sublabel   = "Aa أبجد",
                            isSelected = uiState.fontChoice == font,
                            onClick    = { viewModel.setFontChoice(font) }
                        )
                    }
                }
            }

            Divider()

            // Text size
            var sizeExpanded by remember { mutableStateOf(false) }
            ExpandableRow(
                icon      = Lucide.ALargeSmall,
                title     = if (isAr) "حجم النص" else "Text Size",
                current   = if (isAr) uiState.textSize.labelAr else uiState.textSize.labelEn,
                expanded  = sizeExpanded,
                onToggle  = { sizeExpanded = !sizeExpanded }
            )
            if (sizeExpanded) {
                Row(
                    modifier = Modifier.padding(start = 52.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextSizeOption.entries.forEach { opt ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (uiState.textSize == opt)
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(8.dp))
                                    else
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                )
                                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setTextSize(opt) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text  = if (isAr) opt.labelAr else opt.labelEn,
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = (11 * opt.scale).sp),
                                color = if (uiState.textSize == opt) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Divider()

            // Notification type
            var notifExpanded by remember { mutableStateOf(false) }
            val notifLabel = when (uiState.notifMode) {
                NotifMode.SILENT           -> if (isAr) "صامت"       else "Silent"
                NotifMode.NOTIFICATION_ONLY-> if (isAr) "إشعار فقط"  else "Notification"
                NotifMode.SOUND            -> if (isAr) "صوت"        else "Sound"
                NotifMode.FULL_ALARM       -> if (isAr) "منبه كامل"  else "Full Alarm"
            }
            ExpandableRow(
                icon      = Lucide.Bell,
                title     = if (isAr) "نوع التنبيه" else "Alert Type",
                current   = notifLabel,
                expanded  = notifExpanded,
                onToggle  = { notifExpanded = !notifExpanded }
            )
            if (notifExpanded) {
                data class ModeInfo(val mode: NotifMode, val icon: ImageVector, val en: String, val ar: String)
                val modes = listOf(
                    ModeInfo(NotifMode.SILENT,           Lucide.BellOff,    "Silent",       "صامت"),
                    ModeInfo(NotifMode.NOTIFICATION_ONLY,Lucide.Bell,       "Notification", "إشعار فقط"),
                    ModeInfo(NotifMode.SOUND,            Lucide.Volume2,    "Sound",        "صوت"),
                    ModeInfo(NotifMode.FULL_ALARM,       Lucide.AlarmClock, "Full Alarm",   "منبه كامل"),
                )
                Column(
                    modifier = Modifier.padding(start = 52.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    modes.forEach { info ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (uiState.notifMode == info.mode)
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(8.dp))
                                    else
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                                )
                                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setNotifMode(info.mode) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(info.icon, null, Modifier.size(15.dp),
                                tint = if (uiState.notifMode == info.mode) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                            Text(if (isAr) info.ar else info.en,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = if (uiState.notifMode == info.mode) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f))
                            if (uiState.notifMode == info.mode) {
                                Icon(Lucide.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
            }

            // Ringtone — only when sound enabled
            if (uiState.notifMode == NotifMode.SOUND || uiState.notifMode == NotifMode.FULL_ALARM) {
                Divider()
                val ringtoneName = remember(uiState.ringtoneUri) {
                    if (uiState.ringtoneUri.isBlank()) null
                    else runCatching {
                        RingtoneManager.getRingtone(context, Uri.parse(uiState.ringtoneUri))?.getTitle(context)
                    }.getOrNull()
                }
                ActionRow(
                    icon    = Lucide.Music,
                    title   = if (isAr) "نغمة المنبه" else "Ringtone",
                    current = ringtoneName ?: (if (isAr) "النغمة الافتراضية" else "System Default"),
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            if (uiState.ringtoneUri.isNotBlank())
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(uiState.ringtoneUri))
                        }
                        ringtoneLauncher.launch(intent)
                    }
                )
            }

            Divider()

            // About
            ActionRow(
                icon    = Lucide.Info,
                title   = stringResource(R.string.version),
                current = "1.0.0",
                isLast  = true
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

// ─── Row components ───────────────────────────────────────────────────────────
@Composable
private fun ToggleRow(
    icon: ImageVector, title: String, desc: String,
    checked: Boolean, onToggle: (Boolean) -> Unit,
    isFirst: Boolean = false
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RowIcon(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onBackground)
            Text(desc,  style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        }
        Switch(
            checked = checked, onCheckedChange = onToggle,
            colors  = SwitchDefaults.colors(
                checkedThumbColor    = MaterialTheme.colorScheme.background,
                checkedTrackColor    = MaterialTheme.colorScheme.onBackground,
                uncheckedThumbColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                uncheckedTrackColor  = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun ExpandableRow(icon: ImageVector, title: String, current: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onToggle)
            .padding(vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RowIcon(icon)
        Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Text(current, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Icon(
            if (expanded) Lucide.ChevronUp else Lucide.ChevronDown, null,
            Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, current: String, isLast: Boolean = false, onClick: (() -> Unit)? = null) {
    val m = if (onClick != null)
        Modifier.fillMaxWidth().clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick).padding(vertical = 14.dp)
    else
        Modifier.fillMaxWidth().padding(vertical = 14.dp)
    Row(m, Arrangement.spacedBy(14.dp), Alignment.CenterVertically) {
        RowIcon(icon)
        Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Text(current, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        if (onClick != null)
            Icon(Lucide.ChevronRight, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
    }
}

@Composable
private fun RowIcon(icon: ImageVector) {
    Box(
        modifier         = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f),
        modifier = Modifier.padding(start = 50.dp)
    )
}

@Composable
private fun ChipOption(label: String, sublabel: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected)
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(8.dp))
                else
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            )
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground)
            Text(sublabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
        }
        if (isSelected)
            Icon(Lucide.Check, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onBackground)
    }
}