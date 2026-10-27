package com.powermax.patchstudio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.app.AppSettingsScreen
import com.powermax.patchstudio.ui.guide.GuideScreen
import com.powermax.patchstudio.ui.home.HomeScreen
import com.powermax.patchstudio.ui.motion.PsMotion
import com.powermax.patchstudio.ui.theme.PsColors

sealed interface Screen {
    data object Home : Screen
    data class AppSettings(val appId: String) : Screen
    data object Guide : Screen
}

private fun Screen.toRoute(): String = when (this) {
    Screen.Home -> "home"
    Screen.Guide -> "guide"
    is Screen.AppSettings -> "app:$appId"
}

private fun routeToScreen(route: String): Screen = when {
    route == "guide" -> Screen.Guide
    route.startsWith("app:") -> Screen.AppSettings(route.removePrefix("app:"))
    else -> Screen.Home
}

private fun Screen.depth(): Int = if (this == Screen.Home) 0 else 1

@Composable
fun AppRoot() {
    var route by rememberSaveable { mutableStateOf("home") }
    val screen = routeToScreen(route)
    val stateHolder = rememberSaveableStateHolder()
    val offsetPx = with(LocalDensity.current) { 30.dp.roundToPx() }

    BackHandler(enabled = screen != Screen.Home) { route = Screen.Home.toRoute() }

    Box(
        Modifier
            .fillMaxSize()
            .background(PsColors.Background),
    ) {
        AnimatedContent(
            targetState = screen,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val forward = targetState.depth() >= initialState.depth()
                PsMotion.sharedAxisX(forward = forward, offsetPx = offsetPx)
            },
            label = "screens",
        ) { s ->
            stateHolder.SaveableStateProvider(s.toRoute()) {
                when (s) {
                    Screen.Home -> HomeScreen(
                        onOpenApp = { route = Screen.AppSettings(it).toRoute() },
                        onOpenGuide = { route = Screen.Guide.toRoute() },
                    )
                    is Screen.AppSettings -> AppSettingsScreen(
                        appId = s.appId,
                        onBack = { route = Screen.Home.toRoute() },
                    )
                    Screen.Guide -> GuideScreen(onBack = { route = Screen.Home.toRoute() })
                }
            }
        }
    }
}
