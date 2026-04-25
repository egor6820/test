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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tossday.ui.day.DayDetailScreen
import com.example.tossday.ui.main.MainScreen
import com.example.tossday.ui.main.MainViewModel
import com.example.tossday.ui.settings.SettingsScreen
import java.time.LocalDate

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    // ГОЛОВНИЙ ФІКС: Створюємо ViewModel тут один раз.
    // Тепер всі екрани ділять між собою один стан (isEditMode, selectedDate тощо).
    val sharedViewModel: MainViewModel = hiltViewModel()

    // Налаштування пружини для преміального перегортання
    val springSpec = spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    NavHost(
        navController = navController,
        startDestination = "main",
        // Глобальні преміальні анімації переходів
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
                onDayClick = { date ->
                    navController.navigate("day_detail/$date")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                viewModel = sharedViewModel // Використовуємо спільну VM
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = sharedViewModel, // Використовуємо спільну VM
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "day_detail/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val dateString = backStackEntry.arguments?.getString("date")
            val date = LocalDate.parse(dateString)
            DayDetailScreen(
                date = date,
                onBack = { navController.popBackStack() },
                viewModel = sharedViewModel // Використовуємо спільну VM
            )
        }
    }
}