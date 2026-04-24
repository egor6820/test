package com.example.tossday.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tossday.ui.main.MainScreen
import com.example.tossday.ui.main.MainViewModel
import com.example.tossday.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(
    // Повертаємо параметр сюди, щоб MainActivity міг його передати!
    navController: NavHostController = rememberNavController()
) {
    // Параметри пружини для ідеального, "важкого" перегортання (як в iOS)
    val springSpec = spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    NavHost(
        navController = navController,
        startDestination = "main",
        // Глобальні анімації переходів для всіх екранів
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = springSpec) + fadeIn()
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = springSpec) + fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = springSpec) + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = springSpec) + fadeOut()
        }
    ) {
        composable("main") {
            MainScreen(
                onDayClick = { /* logic if needed */ },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            // Отримуємо спільну ViewModel, прив'язану до графа
            val viewModel: MainViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}