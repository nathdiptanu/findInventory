package com.docufind.app.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.docufind.app.insights.ActivityInsightsTracker
import com.docufind.app.ui.navigation.DocuFindRoutes
import com.docufind.app.ui.navigation.MainScreen
import com.docufind.app.ui.screens.onboarding.OnboardingScreen
import com.docufind.app.ui.screens.onboarding.OnboardingViewModel
import com.docufind.app.ui.screens.profile.ProfileSetupScreen
import com.docufind.app.ui.screens.splash.SplashScreen

@Composable
fun DocuFindApp(
    activityInsightsTracker: ActivityInsightsTracker,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = DocuFindRoutes.SPLASH
    ) {
        composable(DocuFindRoutes.SPLASH) {
            SplashScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(DocuFindRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(DocuFindRoutes.ONBOARDING) {
            val viewModel: OnboardingViewModel = hiltViewModel()
            OnboardingScreen(
                onComplete = {
                    viewModel.completeOnboarding {
                        navController.navigate(DocuFindRoutes.PROFILE_SETUP) {
                            popUpTo(DocuFindRoutes.ONBOARDING) { inclusive = true }
                        }
                    }
                },
                onSkip = {
                    viewModel.completeOnboarding {
                        navController.navigate(DocuFindRoutes.PROFILE_SETUP) {
                            popUpTo(DocuFindRoutes.ONBOARDING) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(DocuFindRoutes.PROFILE_SETUP) {
            ProfileSetupScreen(
                onComplete = {
                    navController.navigate(DocuFindRoutes.MAIN) {
                        popUpTo(DocuFindRoutes.PROFILE_SETUP) { inclusive = true }
                    }
                }
            )
        }
        composable(DocuFindRoutes.MAIN) {
            MainScreen(
                activityInsightsTracker = activityInsightsTracker,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
        }
    }
}
