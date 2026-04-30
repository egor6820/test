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
    val sharedViewModel: MainViewModel = hiltViewModel()

    // ФІКС НАВІГАЦІЇ: Жорстка та чуйна пружина, яка миттєво реагує на переривання (клік "Назад")
    val snappySpring = spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = 0.85f, // Ледь помітна віддача, відчувається природно
        stiffness = 400f // Висока жорсткість = висока швидкість екрана
    )

    NavHost(
        navController = navController,
        startDestination = "main",
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = snappySpring) + fadeIn()
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = snappySpring) + fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = snappySpring) + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = snappySpring) + fadeOut()
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
                viewModel = sharedViewModel
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = sharedViewModel,
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
                viewModel = sharedViewModel
            )
        }
    }
}