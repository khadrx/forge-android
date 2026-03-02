package com.khadr.forge.features.budget.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.*
import com.khadr.forge.R
import com.khadr.forge.core.ui.ConsumeValidationError
import com.khadr.forge.core.ui.LocalToastState
import com.khadr.forge.core.ui.ToastType
import com.khadr.forge.core.util.LocalForgeFormatter
import com.khadr.forge.features.budget.data.*
import com.khadr.forge.ui.theme.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// ─── Category icon mapping ────────────────────────────────────────────────────
fun BudgetCategory.icon(): ImageVector = when (this) {
    BudgetCategory.FOOD      -> Lucide.UtensilsCrossed
    BudgetCategory.TRANSPORT -> Lucide.Car
    BudgetCategory.SHOPPING  -> Lucide.ShoppingBag
    BudgetCategory.HEALTH    -> Lucide.HeartPulse
    BudgetCategory.BILLS     -> Lucide.ReceiptText
    BudgetCategory.SALARY    -> Lucide.Banknote
    BudgetCategory.FREELANCE -> Lucide.Laptop
    BudgetCategory.OTHER     -> Lucide.CircleDot
}

// ─── Budget Screen ────────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val uiState   by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val fmt       = LocalForgeFormatter.current
    val isRtl     = LocalLayoutDirection.current == LayoutDirection.Rtl
    val toast     = LocalToastState.current
    val scope     = rememberCoroutineScope()
    val error     by viewModel.validationError.collectAsState()
    val config    = LocalConfiguration.current
    val locale    = remember(config) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) config.locales.get(0) ?: Locale.getDefault()
        else @Suppress("DEPRECATION") config.locale ?: Locale.getDefault()
    }

    // Toast wiring
    ConsumeValidationError(error, viewModel::clearValidationError) { key ->
        val msg = when (key) {
            "title_required"  -> if (isRtl) "العنوان مطلوب" else "Title is required"
            "amount_required" -> if (isRtl) "أدخل مبلغاً" else "Please enter an amount"
            "amount_invalid"  -> if (isRtl) "المبلغ يجب أن يكون أكبر من صفر" else "Amount must be greater than 0"
            else              -> key
        }
        toast.showIn(scope, msg, ToastType.WARNING)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Top Bar ───────────────────────────────────────────────────────
            item {
                BudgetTopBar(
                    month           = uiState.currentMonth,
                    locale          = locale,
                    onPrevious      = viewModel::previousMonth,
                    onNext          = viewModel::nextMonth,
                    onConfigClick   = viewModel::openConfigSheet,
                    showSmartBudget = uiState.showAllocation,
                    onToggleSmart   = viewModel::toggleAllocation,
                    isRtl           = isRtl
                )
            }

            // ── Balance card (outline style, no fill) ─────────────────────────
            item {
                SummarySection(
                    income   = uiState.totalIncome,
                    expenses = uiState.totalExpenses,
                    balance  = uiState.balance,
                    fmt      = fmt,
                    isRtl    = isRtl
                )
            }

            // ── Smart Budget (optional) ───────────────────────────────────────
            if (uiState.showAllocation) {
                uiState.allocation?.let { alloc ->
                    if (alloc.income > 0) {
                        if (alloc.warnings.isNotEmpty()) {
                            item { WarningsBanner(alloc.warnings, isRtl) }
                        }
                        item { AllocationSection(alloc, isRtl, fmt) }
                    } else {
                        item {
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text  = if (isRtl) "أضف دخلاً لتفعيل التوزيع الذكي"
                                    else "Add income to activate smart budget",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Section label ─────────────────────────────────────────────────
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(
                        text  = stringResource(R.string.transactions).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    if (uiState.transactions.isNotEmpty()) {
                        Text(
                            text  = "${uiState.transactions.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // ── Transactions ──────────────────────────────────────────────────
            if (uiState.isLoading) {
                item { LoadingBox() }
            } else if (uiState.transactions.isEmpty()) {
                item { EmptyTransactions(isRtl) }
            } else {
                items(uiState.transactions, key = { it.id }) { tx ->
                    TransactionItem(tx, fmt, locale, isRtl,
                        onEdit   = { viewModel.openEditSheet(tx) },
                        onDelete = { viewModel.deleteTransaction(tx.id) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                }
            }
        }

        // ── FAB ───────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.onBackground)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() },
                    onClick = viewModel::openAddSheet),
            contentAlignment = Alignment.Center
        ) {
            Icon(Lucide.Plus, null, Modifier.size(22.dp), MaterialTheme.colorScheme.background)
        }
    }

    // ── Transaction Sheet ─────────────────────────────────────────────────────
    if (uiState.isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeSheet,
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = MaterialTheme.colorScheme.background,
            // Single handle — no custom dragHandle needed
        ) {
            TransactionFormSheet(
                formState        = formState,
                isEditing        = uiState.editingTx != null,
                fmt              = fmt,
                isRtl            = isRtl,
                onTitleChange    = viewModel::onTitleChange,
                onAmountChange   = viewModel::onAmountChange,
                onTypeChange     = viewModel::onTypeChange,
                onCategoryChange = viewModel::onCategoryChange,
                onNoteChange     = viewModel::onNoteChange,
                onSave           = viewModel::saveTransaction,
                onDismiss        = viewModel::closeSheet
            )
        }
    }

    // ── Allocation Config Sheet ───────────────────────────────────────────────
    if (uiState.isConfigOpen) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeConfigSheet,
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = MaterialTheme.colorScheme.background,
        ) {
            AllocationConfigSheet(
                config    = uiState.allocationConfig,
                isRtl     = isRtl,
                onSave    = { viewModel.setAllocationConfig(it); viewModel.closeConfigSheet() },
                onDismiss = viewModel::closeConfigSheet
            )
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun BudgetTopBar(
    month          : YearMonth,
    locale         : Locale,
    onPrevious     : () -> Unit,
    onNext         : () -> Unit,
    onConfigClick  : () -> Unit,
    showSmartBudget: Boolean,
    onToggleSmart  : () -> Unit,
    isRtl          : Boolean
) {
    val monthLabel = month.month.getDisplayName(TextStyle.FULL, locale) + " ${month.year}"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(stringResource(R.string.budget),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconBtn(if (isRtl) Lucide.ChevronRight else Lucide.ChevronLeft, onPrevious)
                Text(monthLabel, style = MaterialTheme.typography.labelLarge.copy(fontFamily = GeistMonoFamily),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.widthIn(min = 106.dp), textAlign = TextAlign.Center)
                IconBtn(if (isRtl) Lucide.ChevronLeft else Lucide.ChevronRight, onNext)
            }
        }

        // Smart budget toggle chip + config
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Toggle chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (showSmartBudget)
                            Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(8.dp))
                        else
                            Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    )
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onToggleSmart)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Lucide.TrendingUp, null, Modifier.size(13.dp),
                        tint = if (showSmartBudget) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text(
                        text  = if (isRtl) "التوزيع الذكي" else "Smart Budget",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (showSmartBudget) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // Config button (only when smart budget is on)
            if (showSmartBudget) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onConfigClick)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Icon(Lucide.SlidersHorizontal, null, Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun IconBtn(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

// ─── Summary Section (balance outline card) ───────────────────────────────────
@Composable
private fun SummarySection(income: Double, expenses: Double, balance: Double, fmt: com.khadr.forge.core.util.ForgeFormatter, isRtl: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Balance — transparent bg, stroke only
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(3.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.balance), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                Text(fmt.currency(balance),
                    style = ForgeTextStyles.MoneyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground)
                Text(stringResource(R.string.this_month), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SubCard(Modifier.weight(1f), stringResource(R.string.income),
                fmt.currency(income), Lucide.ArrowDownLeft, ForgeSuccess)
            SubCard(Modifier.weight(1f), stringResource(R.string.spent),
                fmt.currency(expenses), Lucide.ArrowUpRight, ForgeExpense)
        }
    }
}

@Composable
private fun SubCard(modifier: Modifier, label: String, value: String, icon: ImageVector, iconTint: Color) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.13f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(15.dp), tint = iconTint)
            }
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                Text(value, style = ForgeTextStyles.MoneySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

// ─── Warnings Banner (no background — just text) ──────────────────────────────
@Composable
private fun WarningsBanner(warnings: List<AllocationWarning>, isRtl: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        warnings.forEach { w ->
            val (iconTint, icon) = when (w.level) {
                AllocationWarningLevel.DANGER  -> Pair(ZoneDanger, Lucide.TriangleAlert)
                AllocationWarningLevel.WARNING -> Pair(ForgeWarning, Lucide.CircleAlert)
                AllocationWarningLevel.INFO    -> Pair(MaterialTheme.colorScheme.primary, Lucide.Info)
            }
            Row(
                modifier              = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.Top
            ) {
                Icon(icon, null, Modifier.size(14.dp).padding(top = 2.dp), tint = iconTint)
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(if (isRtl) w.titleAr else w.titleEn,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = iconTint)
                    Text(if (isRtl) w.bodyAr else w.bodyEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ─── Allocation Section ───────────────────────────────────────────────────────
@Composable
private fun AllocationSection(result: AllocationResult, isRtl: Boolean, fmt: com.khadr.forge.core.util.ForgeFormatter) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val label = if (isRtl) "توزيع الميزانية" else "Budget Split"
        val ratioLabel = result.splits.joinToString(" / ") { "${it.percent}%" }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            Text(ratioLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
        result.splits.forEach { ZoneCard(it, fmt, isRtl) }
        SavingsCard(result.savingsActual, fmt, isRtl)
    }
}

@Composable
private fun ZoneCard(split: ZoneSplit, fmt: com.khadr.forge.core.util.ForgeFormatter, isRtl: Boolean) {
    val zoneColor = when (split.zone) {
        BudgetZone.NEEDS   -> ZoneNeeds
        BudgetZone.WANTS   -> ZoneWants
        BudgetZone.SAVINGS -> ZoneSavings
    }
    val barColor = if (split.isOverBudget) ZoneDanger else zoneColor
    val animated by animateFloatAsState(split.usedPercent.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing), label = "zone")
    val label    = if (isRtl) split.zone.labelAr else split.zone.labelEn

    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .border(
                if (split.isOverBudget) 2.dp else 1.dp,
                if (split.isOverBudget) ZoneDanger.copy(0.45f) else MaterialTheme.colorScheme.outline.copy(0.1f),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(zoneColor))
                    Text("$label  ${split.percent}%",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground)
                    if (split.isOverBudget) {
                        Text(
                            text  = if (isRtl) "تجاوز" else "Over",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ZoneDanger
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(fmt.currency(split.spent),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (split.isOverBudget) ZoneDanger else MaterialTheme.colorScheme.onBackground)
                    Text("/ ${fmt.currency(split.budget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                }
            }
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(animated).clip(RoundedCornerShape(3.dp)).background(barColor))
            }
            Text(
                text  = if (split.remaining >= 0)
                    "${fmt.currency(split.remaining)} ${if (isRtl) "متبقي" else "remaining"}"
                else
                    "${fmt.currency(-split.remaining)} ${if (isRtl) "تجاوز" else "over budget"}",
                style = MaterialTheme.typography.labelSmall,
                color = if (split.remaining >= 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else ZoneDanger
            )
        }
    }
}

@Composable
private fun SavingsCard(actual: Double, fmt: com.khadr.forge.core.util.ForgeFormatter, isRtl: Boolean) {
    val color = if (actual >= 0) ZoneSavings else ZoneDanger
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (actual >= 0) Lucide.PiggyBank else Lucide.TrendingDown,
                null, Modifier.size(15.dp), tint = color)
            Text(if (isRtl) "المدخرات الفعلية" else "Actual Savings",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = color)
        }
        Text(fmt.currency(actual),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

// ─── Allocation Config Sheet ──────────────────────────────────────────────────
@Composable
private fun AllocationConfigSheet(
    config: AllocationConfig, isRtl: Boolean,
    onSave: (AllocationConfig) -> Unit, onDismiss: () -> Unit
) {
    var needs  by remember { mutableIntStateOf(config.needsPercent) }
    var wants  by remember { mutableIntStateOf(config.wantsPercent) }
    val savings = (100 - needs - wants).coerceAtLeast(0)
    val isValid = needs + wants <= 100 && needs >= 0 && wants >= 0

    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {

        Text(if (isRtl) "توزيع الميزانية" else "Budget Split",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground)

        // Reset to default chip
        Box(
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    needs = 50; wants = 30
                }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(if (isRtl) "إعادة للافتراضي (50/30/20)" else "Reset to default (50/30/20)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }

        ZoneSlider(if (isRtl) "الاحتياجات" else "Needs", ZoneNeeds, needs) { needs = it }
        ZoneSlider(if (isRtl) "الرغبات" else "Wants", ZoneWants, wants) { wants = it }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(ZoneSavings))
            Text("${if (isRtl) "المدخرات (تلقائي)" else "Savings (auto)"}: $savings%",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = ZoneSavings)
        }

        if (!isValid) {
            Text(if (isRtl) "المجموع يتجاوز 100%" else "Total exceeds 100%",
                style = MaterialTheme.typography.labelMedium, color = ZoneDanger)
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
                .then(
                    if (isValid) Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(14.dp))
                    else         Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                )
                .clickable(enabled = isValid, indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    onSave(AllocationConfig(needsPercent = needs, wantsPercent = wants, savingsPercent = savings))
                },
            contentAlignment = Alignment.Center
        ) {
            Text(if (isRtl) "حفظ" else "Save",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isValid) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun ZoneSlider(label: String, color: Color, value: Int, onValueChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground)
            }
            Text("$value%", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = color)
        }
        Slider(value = value.toFloat(), onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..80f, steps = 15,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = color.copy(0.2f)))
    }
}

// ─── Transaction Item ─────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TransactionItem(
    tx: TransactionEntity, fmt: com.khadr.forge.core.util.ForgeFormatter,
    locale: Locale, isRtl: Boolean, onEdit: () -> Unit, onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val catColor  = categoryColor(tx.category)
    val dateStr   = tx.date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", locale))
    val catLabel  = categoryLabel(tx.category, isRtl)

    Column(
        modifier = Modifier.fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { expanded = !expanded }
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            // Category icon badge
            Box(
                modifier         = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(catColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tx.category.icon(), null, Modifier.size(18.dp), tint = catColor)
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tx.title.ifBlank { catLabel },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground)
                Text("$catLabel · $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
            }

            Text(
                text  = "${if (tx.type == TransactionType.INCOME) "+" else "-"}${fmt.currency(tx.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (tx.type == TransactionType.INCOME) ForgeSuccess else MaterialTheme.colorScheme.onBackground
            )
        }

        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InlineBtn(Modifier.weight(1f), Lucide.Pencil, if (isRtl) "تعديل" else "Edit", onEdit)
                InlineBtn(Modifier.weight(1f), Lucide.Trash2, if (isRtl) "حذف" else "Delete", onDelete)
            }
        }
    }
}

fun categoryColor(cat: BudgetCategory): Color = when (cat) {
    BudgetCategory.FOOD      -> CatFood
    BudgetCategory.TRANSPORT -> CatTransport
    BudgetCategory.SHOPPING  -> CatShopping
    BudgetCategory.HEALTH    -> CatHealth
    BudgetCategory.BILLS     -> CatBills
    BudgetCategory.SALARY    -> CatSalary
    BudgetCategory.FREELANCE -> CatFreelance
    BudgetCategory.OTHER     -> CatOther
}

fun categoryLabel(cat: BudgetCategory, isRtl: Boolean): String = when (cat) {
    BudgetCategory.FOOD      -> if (isRtl) "طعام"      else "Food"
    BudgetCategory.TRANSPORT -> if (isRtl) "مواصلات"   else "Transport"
    BudgetCategory.SHOPPING  -> if (isRtl) "تسوق"      else "Shopping"
    BudgetCategory.HEALTH    -> if (isRtl) "صحة"       else "Health"
    BudgetCategory.BILLS     -> if (isRtl) "فواتير"    else "Bills"
    BudgetCategory.SALARY    -> if (isRtl) "راتب"      else "Salary"
    BudgetCategory.FREELANCE -> if (isRtl) "عمل حر"    else "Freelance"
    BudgetCategory.OTHER     -> if (isRtl) "أخرى"      else "Other"
}

@Composable
private fun InlineBtn(modifier: Modifier, icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
    }
}

// ─── Transaction Form Sheet ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionFormSheet(
    formState       : TransactionFormState,
    isEditing       : Boolean,
    fmt             : com.khadr.forge.core.util.ForgeFormatter,
    isRtl           : Boolean,
    onTitleChange   : (String) -> Unit,
    onAmountChange  : (String) -> Unit,
    onTypeChange    : (TransactionType) -> Unit,
    onCategoryChange: (BudgetCategory) -> Unit,
    onNoteChange    : (String) -> Unit,
    onSave          : () -> Unit,
    onDismiss       : () -> Unit
) {
    // Separate categories by type
    val incomeCategories  = listOf(BudgetCategory.SALARY, BudgetCategory.FREELANCE, BudgetCategory.OTHER)
    val expenseCategories = listOf(BudgetCategory.FOOD, BudgetCategory.TRANSPORT, BudgetCategory.SHOPPING,
        BudgetCategory.HEALTH, BudgetCategory.BILLS, BudgetCategory.OTHER)
    val visibleCategories = if (formState.type == TransactionType.INCOME) incomeCategories else expenseCategories

    // Auto-select valid category when type changes
    LaunchedEffect(formState.type) {
        if (formState.category !in visibleCategories) {
            onCategoryChange(visibleCategories.first())
        }
    }

    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title only
        Text(
            text  = if (isEditing) stringResource(R.string.edit_transaction) else stringResource(R.string.add_transaction),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Type toggle — transparent + stroke (3px active, 1.5px inactive)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).padding(0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionType.entries.forEach { type ->
                val isSelected = formState.type == type
                val typeLabel  = if (type == TransactionType.INCOME)
                    (if (isRtl) "دخل" else "Income")
                else
                    (if (isRtl) "مصروف" else "Expense")
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .then(
                            if (isSelected)
                                Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(10.dp))
                            else
                                Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        )
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onTypeChange(type) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(typeLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }

        // Amount (prominent)
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("﷼", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f))
            TextField(value = formState.amount, onValueChange = onAmountChange,
                placeholder = { Text("0.00", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)) },
                textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth())
        }

        // Title (optional — hint shows category name)
        FlatField(formState.title, onTitleChange,
            placeholder = if (isRtl) "${categoryLabel(formState.category, true)} (اختياري)"
            else "${categoryLabel(formState.category, false)} (optional)",
            icon = Lucide.Type)

        // Note
        FlatField(formState.note, onNoteChange,
            placeholder = if (isRtl) "ملاحظة (اختياري)" else "Note (optional)",
            icon = Lucide.FileText, singleLine = false)

        // Category grid — filtered by type
        Text(
            text  = (if (isRtl) "التصنيف" else "Category").uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        CategoryGrid(formState.category, visibleCategories, isRtl, onCategoryChange)

        // Save — primary black button
        Box(
            modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.onBackground)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onSave),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = if (isEditing) stringResource(R.string.save_changes) else stringResource(R.string.add_transaction),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.background
            )
        }
    }
}

@Composable
private fun FlatField(value: String, onChange: (String) -> Unit, placeholder: String, icon: ImageVector, singleLine: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        TextField(value = value, onValueChange = onChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            singleLine = singleLine, maxLines = if (singleLine) 1 else 3,
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CategoryGrid(
    selected   : BudgetCategory,
    categories : List<BudgetCategory>,
    isRtl      : Boolean,
    onSelect   : (BudgetCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cat ->
                    val isSelected = selected == cat
                    val catColor   = categoryColor(cat)
                    val catLabel   = categoryLabel(cat, isRtl)
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .then(
                                if (isSelected)
                                    Modifier.border(2.5.dp, catColor, RoundedCornerShape(10.dp))
                                else
                                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            )
                            .background(if (isSelected) catColor.copy(alpha = 0.07f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSelect(cat) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(cat.icon(), null, Modifier.size(18.dp),
                            tint = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Text(catLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun EmptyTransactions(isRtl: Boolean) {
    Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Lucide.ReceiptText, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            Text(if (isRtl) "لا توجد معاملات" else "No transactions yet",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}