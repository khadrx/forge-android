package com.khadr.forge.core.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.composables.icons.lucide.*
import com.khadr.forge.R
import com.khadr.forge.features.budget.ui.BudgetScreen
import com.khadr.forge.features.dashboard.ui.DashboardScreen
import com.khadr.forge.features.reminders.ui.RemindersScreen
import com.khadr.forge.features.schedule.ui.ScheduleScreen
import com.khadr.forge.features.settings.ui.SettingsScreen
import com.khadr.forge.features.tasks.ui.TasksScreen

// ─── Routes ───────────────────────────────────────────────────────────────────
sealed class ForgeRoute(val route: String) {
    data object Dashboard  : ForgeRoute("dashboard")
    data object Schedule   : ForgeRoute("schedule")
    data object Tasks      : ForgeRoute("tasks")
    data object Budget     : ForgeRoute("budget")
    data object Reminders  : ForgeRoute("reminders")
    data object Settings   : ForgeRoute("settings")   // NOT in bottom nav
}

// ── Bottom nav items (5 screens — Settings excluded) ──────────────────────────
private data class NavItem(val route: String, val labelRes: Int, val icon: ImageVector)
private val navItems = listOf(
    NavItem(ForgeRoute.Dashboard.route, R.string.nav_home,      Lucide.LayoutDashboard),
    NavItem(ForgeRoute.Schedule.route,  R.string.nav_schedule,  Lucide.CalendarDays),
    NavItem(ForgeRoute.Tasks.route,     R.string.nav_tasks,     Lucide.CheckCheck),
    NavItem(ForgeRoute.Budget.route,    R.string.nav_budget,    Lucide.Wallet),
    NavItem(ForgeRoute.Reminders.route, R.string.nav_reminders, Lucide.Bell),
)

// ── Routes that are part of the bottom nav (for hiding/showing it) ────────────
private val bottomNavRoutes = navItems.map { it.route }.toSet()

// ─── Nav Host ─────────────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ForgeNavHost(
    navController   : NavHostController = rememberNavController(),
    modifier        : Modifier          = Modifier,
    startDestination: String            = ForgeRoute.Dashboard.route
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val forwardEnter = if (isRtl) AnimatedContentTransitionScope.SlideDirection.End   else AnimatedContentTransitionScope.SlideDirection.Start
    val forwardExit  = if (isRtl) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End
    val backEnter    = if (isRtl) AnimatedContentTransitionScope.SlideDirection.Start  else AnimatedContentTransitionScope.SlideDirection.End
    val backExit     = if (isRtl) AnimatedContentTransitionScope.SlideDirection.End   else AnimatedContentTransitionScope.SlideDirection.Start

    NavHost(
        navController       = navController,
        startDestination    = startDestination,
        modifier            = modifier,
        enterTransition     = { fadeIn(tween(180)) + slideIntoContainer(forwardEnter, tween(220)) },
        exitTransition      = { fadeOut(tween(120)) + slideOutOfContainer(forwardExit, tween(180)) },
        popEnterTransition  = { fadeIn(tween(180)) + slideIntoContainer(backEnter, tween(220)) },
        popExitTransition   = { fadeOut(tween(120)) + slideOutOfContainer(backExit, tween(180)) }
    ) {
        composable(ForgeRoute.Dashboard.route) {
            DashboardScreen(
                onNavigateToBudget    = { navController.navigate(ForgeRoute.Budget.route) },
                onNavigateToTasks     = { navController.navigate(ForgeRoute.Tasks.route) },
                onNavigateToSchedule  = { navController.navigate(ForgeRoute.Schedule.route) },
                onNavigateToReminders = { navController.navigate(ForgeRoute.Reminders.route) },
                onNavigateToSettings  = {
                    // Settings pushed ON TOP of current stack — back returns here
                    navController.navigate(ForgeRoute.Settings.route)
                }
            )
        }
        composable(ForgeRoute.Schedule.route)  { ScheduleScreen() }
        composable(ForgeRoute.Tasks.route)     { TasksScreen() }
        composable(ForgeRoute.Budget.route)    { BudgetScreen() }
        composable(ForgeRoute.Reminders.route) { RemindersScreen() }

        // Settings: NOT part of bottom nav — no popUpTo, no saveState
        // Back button / system gesture pops it and returns to wherever user was
        composable(ForgeRoute.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// ─── Bottom Bar ───────────────────────────────────────────────────────────────
@Composable
fun ForgeBottomBar(navController: NavHostController) {
    val backStack    by navController.currentBackStackEntryAsState()
    val currentRoute  = backStack?.destination?.route

    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .height(60.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            navItems.forEach { item ->
                val label    = stringResource(item.labelRes)
                val selected = currentRoute == item.route

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    // Always pop up to Dashboard — keeps only 1 entry per screen
                                    popUpTo(ForgeRoute.Dashboard.route) {
                                        saveState    = true
                                        inclusive    = false
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector        = item.icon,
                        contentDescription = label,
                        modifier           = Modifier.size(21.dp),
                        tint               = if (selected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                    )
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (selected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                    )
                }
            }

            // Settings gear — same row, no popUpTo needed
            val settingsSelected = currentRoute == ForgeRoute.Settings.route
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        if (currentRoute != ForgeRoute.Settings.route) {
                            navController.navigate(ForgeRoute.Settings.route)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector        = Lucide.Settings,
                    contentDescription = stringResource(R.string.settings),
                    modifier           = Modifier.size(21.dp),
                    tint               = if (settingsSelected) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                )
                Text(
                    text  = stringResource(R.string.settings),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (settingsSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (settingsSelected) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                )
            }
        }
    }
}

// ─── Coming Soon ──────────────────────────────────────────────────────────────
@Composable
fun ComingSoonScreen(title: String) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Lucide.Hammer, null, Modifier.size(28.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onBackground)
            Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}