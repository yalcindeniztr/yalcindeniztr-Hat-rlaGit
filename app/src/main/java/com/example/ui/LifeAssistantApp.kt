package com.example.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.tabs.AddReminderScreen
import com.example.ui.tabs.CategoriesScreen
import com.example.ui.tabs.SubCategoriesScreen

@Composable
fun LifeAssistantApp() {
    val navController = rememberNavController()
    val viewModel: LifeAssistantViewModel = viewModel()
    
    val legalAccepted by viewModel.legalAccepted.collectAsStateWithLifecycle()
    val userNick by viewModel.userNick.collectAsStateWithLifecycle()
    val fontScaleLevel by viewModel.fontScaleLevel.collectAsStateWithLifecycle()

    val currentDensity = LocalDensity.current
    val fontMultiplier = when (fontScaleLevel) {
        "LARGE" -> 1.22f
        "EXTRA_LARGE" -> 1.42f
        else -> 1.0f
    }
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * fontMultiplier
    )

    val startDestination = when {
        !legalAccepted -> "legal"
        userNick == null -> "profile_setup"
        else -> "main"
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable("legal") {
                LegalScreen(
                    onAccept = {
                        viewModel.acceptLegalTerms()
                        navController.navigate("profile_setup") {
                            popUpTo("legal") { inclusive = true }
                        }
                    }
                )
            }
            
            composable("profile_setup") {
                ProfileSetupScreen(
                    onProfileSaved = { nick, pin, avatar ->
                        viewModel.saveProfile(nick, pin, avatar)
                        navController.navigate("main") {
                            popUpTo("profile_setup") { inclusive = true }
                        }
                    }
                )
            }

            composable("main") {
                MainScreen(viewModel, navController)
            }
            
            composable("categories") {
                CategoriesScreen(
                    viewModel = viewModel,
                    onCategorySelected = { categoryKey ->
                        navController.navigate("subcategories/$categoryKey")
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "subcategories/{categoryKey}",
                arguments = listOf(navArgument("categoryKey") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryKey = backStackEntry.arguments?.getString("categoryKey") ?: "HEALTH"
                SubCategoriesScreen(
                    categoryKey = categoryKey,
                    viewModel = viewModel,
                    onSubCategorySelected = { key, subName, desc, times, interval ->
                        val encodedSubName = Uri.encode(subName)
                        val encodedDesc = Uri.encode(desc)
                        val timesStr = times.joinToString(",")
                        navController.navigate("add_reminder/$key?subName=$encodedSubName&desc=$encodedDesc&times=$timesStr&interval=$interval")
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(
                route = "add_reminder/{categoryName}?subName={subName}&desc={desc}&times={times}&interval={interval}",
                arguments = listOf(
                    navArgument("categoryName") { type = NavType.StringType; defaultValue = "GENERAL" },
                    navArgument("subName") { type = NavType.StringType; defaultValue = "" },
                    navArgument("desc") { type = NavType.StringType; defaultValue = "" },
                    navArgument("times") { type = NavType.StringType; defaultValue = "" },
                    navArgument("interval") { type = NavType.StringType; defaultValue = "DAILY" }
                )
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("categoryName") ?: "GENERAL"
                val subName = backStackEntry.arguments?.getString("subName") ?: ""
                val desc = backStackEntry.arguments?.getString("desc") ?: ""
                val timesParam = backStackEntry.arguments?.getString("times") ?: ""
                val interval = backStackEntry.arguments?.getString("interval") ?: "DAILY"

                val timesList = if (timesParam.isNotBlank()) timesParam.split(",").filter { it.isNotBlank() } else emptyList()

                AddReminderScreen(
                    categoryKey = categoryName,
                    initialSubCategoryName = subName,
                    initialDescription = desc,
                    initialTimes = timesList,
                    initialInterval = interval,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack("main", false) }
                )
            }
        }
    }
}
