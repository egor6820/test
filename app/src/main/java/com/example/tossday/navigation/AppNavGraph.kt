package com.example.tossday.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.tossday.ui.day.DayDetailScreen
import com.example.tossday.ui.main.MainScreen
import com.example.tossday.ui.main.MainViewModel
import com.example.tossday.ui.settings.SettingsScreen
import java.time.LocalDate

// Material Shared Axis Z — standard 300ms, symmetric, smooth ease-in-out.
// No shortening: premium feel comes from letting the motion breathe.
private const val Z_MS    = 300
private const val PUSH_MS = 340

@Composable
fun AppNavGraph(navController: NavHostController) {
    val viewModel: MainViewModel = hiltViewModel()

    // Surface paints the theme background behind every destination so transitions
    // never reveal the XML windowBackground underneath.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = "main",
            enterTransition = { fadeIn(tween(180)) },
            exitTransition  = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition  = { fadeOut(tween(180)) }
        ) {

            // ── Main ──────────────────────────────────────────────────────────
            composable(
                route = "main",
                exitTransition = {
                    when (targetState.destination.route) {
                        "settings" ->
                            // Main recedes a hair behind the rising settings sheet. No fade:
                            // settings paints on top so the user feels depth, not a curtain.
                            scaleOut(
                                targetScale = 0.97f,
                                animationSpec = tween(Z_MS, easing = FastOutSlowInEasing)
                            )
                        else ->
                            slideOutHorizontally(
                                targetOffsetX = { -(it / 3) },
                                animationSpec = tween(PUSH_MS, easing = FastOutSlowInEasing)
                            )
                    }
                },
                popEnterTransition = {
                    when (initialState.destination.route) {
                        "settings" ->
                            scaleIn(
                                initialScale = 0.97f,
                                animationSpec = tween(Z_MS, easing = FastOutSlowInEasing)
                            )
                        else ->
                            slideInHorizontally(
                                initialOffsetX = { -(it / 3) },
                                animationSpec = tween(PUSH_MS, easing = FastOutSlowInEasing)
                            )
                    }
                }
            ) {
                MainScreen(
                    viewModel = viewModel,
                    onDayClick = { date -> navController.navigate("day/$date") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }

            // ── Day detail ────────────────────────────────────────────────────
            composable(
                route = "day/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType }),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(PUSH_MS, easing = FastOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(PUSH_MS - 30, easing = FastOutSlowInEasing)
                    )
                }
            ) { backStack ->
                val date = LocalDate.parse(backStack.arguments!!.getString("date")!!)
                DayDetailScreen(
                    date = date,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Settings (Material Shared Axis Z) ─────────────────────────────
            composable(
                route = "settings",
                // Forward: grows in from 88% + fades in. Classic Z-axis "coming toward camera".
                enterTransition = {
                    scaleIn(
                        initialScale = 0.88f,
                        animationSpec = tween(Z_MS, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(Z_MS, easing = FastOutSlowInEasing))
                },
                // Back: shrinks to 88% + fades out. Mirror of enter.
                popExitTransition = {
                    scaleOut(
                        targetScale = 0.88f,
                        animationSpec = tween(Z_MS, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(Z_MS, easing = FastOutSlowInEasing))
                }
            ) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
