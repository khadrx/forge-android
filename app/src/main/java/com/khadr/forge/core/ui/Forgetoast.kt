package com.khadr.forge.core.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Types ────────────────────────────────────────────────────────────────────
enum class ToastType { SUCCESS, ERROR, WARNING, INFO }

data class ToastData(
    val message   : String,
    val type      : ToastType = ToastType.INFO,
    val durationMs: Long      = 3000L
)

// ─── State ─────────────────────────────────────────────────────────────────────
class ToastState {
    var current by mutableStateOf<ToastData?>(null)
        private set

    suspend fun show(message: String, type: ToastType = ToastType.INFO, durationMs: Long = 3000L) {
        current = ToastData(message, type, durationMs)
        delay(durationMs)
        current = null
    }

    fun showIn(scope: CoroutineScope, message: String, type: ToastType = ToastType.INFO, durationMs: Long = 3000L) {
        scope.launch { show(message, type, durationMs) }
    }

    fun dismiss() { current = null }
}

// ─── CompositionLocal ─────────────────────────────────────────────────────────
val LocalToastState = compositionLocalOf<ToastState> { error("ForgeToastHost not in tree") }

@Composable
fun rememberToastState() = remember { ToastState() }

// ─── Host ─────────────────────────────────────────────────────────────────────
@Composable
fun ForgeToastHost(modifier: Modifier = Modifier) {
    val state = LocalToastState.current
    val toast  = state.current

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = toast != null,
            enter   = slideInVertically(spring(dampingRatio = 0.65f, stiffness = 480f)) { -it } + fadeIn(tween(100)),
            exit    = slideOutVertically(tween(180, easing = FastOutLinearInEasing)) { -it } + fadeOut(tween(100))
        ) {
            if (toast != null) ToastCard(toast)
        }
    }
}

// ─── Card — #FEFEFE, no border, clean drop shadow ─────────────────────────────
@Composable
private fun ToastCard(toast: ToastData) {
    data class Style(val icon: ImageVector, val tint: Color)
    val style = when (toast.type) {
        ToastType.SUCCESS -> Style(Lucide.CircleCheck, Color(0xFF22C55E))
        ToastType.ERROR   -> Style(Lucide.CircleX,    Color(0xFFEF4444))
        ToastType.WARNING -> Style(Lucide.TriangleAlert, Color(0xFFF59E0B))
        ToastType.INFO    -> Style(Lucide.Info,        MaterialTheme.colorScheme.onBackground)
    }

    Surface(
        modifier      = Modifier.padding(top = 52.dp, start = 20.dp, end = 20.dp),
        shape         = RoundedCornerShape(16.dp),
        color         = Color(0xFFFEFEFE),           // always light — toast is always white
        shadowElevation = 20.dp,                     // clean shadow, no border
        tonalElevation  = 0.dp
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Icon(style.icon, null, Modifier.size(18.dp), tint = style.tint)
            Text(
                text     = toast.message,
                style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color    = Color(0xFF111114),          // always dark text on white bg
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── Validation error consumer ────────────────────────────────────────────────
@Composable
fun ConsumeValidationError(
    error   : String?,
    onClear : () -> Unit,
    onHandle: (String) -> Unit
) {
    LaunchedEffect(error) {
        if (error != null) {
            onHandle(error)
            onClear()
        }
    }
}