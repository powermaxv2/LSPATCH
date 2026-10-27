package com.powermax.patchstudio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.powermax.patchstudio.ui.app.AppSettingsScreen
import com.powermax.patchstudio.ui.guide.GuideScreen
import com.powermax.patchstudio.ui.home.HomeScreen
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

    BackHandler(enabled = screen != Screen.Home) { route = Screen.Home.toRoute() }

    Box(Modifier.fillMaxSize().background(PsColors.Background)) {
        AnimatedContent(
            targetState = screen,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val forward = targetState.depth() > initialState.depth()
                val ease = FastOutSlowInEasing
                if (forward) {
                    if (targetState == Screen.Guide) {
                        (slideInVertically(tween(520, easing = ease)) { it / 3 } +
                            fadeIn(tween(420)) +
                            scaleIn(tween(520, easing = ease), initialScale = 0.9f)) togetherWith
                            (fadeOut(tween(260)) +
                                scaleOut(tween(420, easing = ease), targetScale = 0.94f))
                    } else {
                        (slideInHorizontally(tween(520, easing = ease)) { it / 3 } +
                            fadeIn(tween(420)) +
                            scaleIn(tween(520, easing = ease), initialScale = 0.92f)) togetherWith
                            (slideOutHorizontally(tween(420, easing = ease)) { -it / 4 } +
                                fadeOut(tween(260)) +
                                scaleOut(tween(420, easing = ease), targetScale = 0.92f))
                    }
                } else {
                    val outgoing = if (initialState == Screen.Guide) {
                        slideOutVertically(tween(420, easing = ease)) { it / 3 } +
                            fadeOut(tween(300)) +
                            scaleOut(tween(420, easing = ease), targetScale = 0.9f)
                    } else {
                        slideOutHorizontally(tween(420, easing = ease)) { it / 3 } +
                            fadeOut(tween(300)) +
                            scaleOut(tween(420, easing = ease), targetScale = 0.92f)
                    }
                    (fadeIn(tween(420)) +
                        scaleIn(tween(520, easing = ease), initialScale = 1.08f)) togetherWith outgoing
                }
            },
            label = "screens",
        ) { s ->
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
