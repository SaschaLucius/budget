package com.lukulent.finanzapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lukulent.finanzapp.ui.entry.EntryScreen
import com.lukulent.finanzapp.ui.statistics.StatisticsScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "entry") {
        composable(
            route = "entry?transactionId={transactionId}",
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: -1L
            EntryScreen(
                transactionId = if (transactionId == -1L) null else transactionId,
                onNavigateToStatistics = { navController.navigate("statistics") },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("statistics") {
            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditTransaction = { id ->
                    navController.navigate("entry?transactionId=$id")
                }
            )
        }
    }
}
