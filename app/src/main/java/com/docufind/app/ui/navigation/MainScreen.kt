package com.docufind.app.ui.navigation



import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Scaffold

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.docufind.app.ui.components.UnlockOverlay
import com.docufind.app.ui.util.rememberFragmentActivity

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.navigation.NavGraph.Companion.findStartDestination

import androidx.navigation.NavType

import androidx.navigation.compose.NavHost

import androidx.navigation.compose.composable

import androidx.navigation.compose.currentBackStackEntryAsState

import androidx.navigation.compose.rememberNavController

import androidx.navigation.navArgument

import com.docufind.app.R

import com.docufind.app.domain.model.QuickAccessItem
import com.docufind.app.domain.model.module.DocuFindModule
import com.docufind.app.insights.ActivityInsightsTracker

import com.docufind.app.ui.components.DocuFindBottomBar

import com.docufind.app.ui.screens.add.AddScreen

import com.docufind.app.ui.screens.emergency.EmergencyListScreen

import com.docufind.app.ui.screens.family.FamilyDetailScreen

import com.docufind.app.ui.screens.family.FamilyListScreen

import com.docufind.app.ui.screens.home.HomeScreen

import com.docufind.app.ui.screens.module.ModuleDetailScreen

import com.docufind.app.ui.screens.module.ModuleHubScreen

import com.docufind.app.ui.screens.module.ModuleListScreen

import com.docufind.app.ui.screens.pets.PetDetailScreen

import com.docufind.app.ui.screens.pets.PetListScreen

import com.docufind.app.ui.screens.placeholder.CategoryPlaceholderScreen

import com.docufind.app.ui.screens.backup.BackupScreen
import com.docufind.app.ui.screens.privacy.PrivacyScreen
import com.docufind.app.ui.screens.storage.StorageScreen
import com.docufind.app.ui.screens.support.AboutScreen
import com.docufind.app.ui.screens.support.HelpSupportScreen
import com.docufind.app.ui.screens.support.ReportBugScreen
import com.docufind.app.ui.screens.support.SendFeedbackScreen

import com.docufind.app.domain.model.reminder.ReminderListItem

import com.docufind.app.ui.screens.reminders.RemindersScreen

import com.docufind.app.ui.screens.preview.FilePreviewScreen
import com.docufind.app.ui.screens.search.SearchScreen

import com.docufind.app.ui.screens.settings.HowToUseScreen
import com.docufind.app.ui.screens.settings.ActivityInsightsScreen
import com.docufind.app.ui.screens.settings.SecuritySettingsScreen
import com.docufind.app.ui.screens.settings.SettingsScreen

import com.docufind.app.ui.screens.scan.QrScannerScreen
import com.docufind.app.ui.screens.vault.VaultTabScreen



private val bottomNavRoutes = setOf(

    DocuFindRoutes.HOME,

    DocuFindRoutes.VAULT,

    DocuFindRoutes.ADD,

    DocuFindRoutes.REMINDERS,

    DocuFindRoutes.SETTINGS

)



@Composable

fun MainScreen(
    activityInsightsTracker: ActivityInsightsTracker,
    onRequestNotificationPermission: () -> Unit = {},
    navigationViewModel: MainNavigationViewModel = hiltViewModel()
) {

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavRoutes

    val activity = rememberFragmentActivity()

    val unlockPrompt by navigationViewModel.unlockPrompt.collectAsStateWithLifecycle()
    val unlockAuthError by navigationViewModel.unlockAuthError.collectAsStateWithLifecycle()
    val isUnlockAuthenticating by navigationViewModel.isUnlockAuthenticating.collectAsStateWithLifecycle()
    val unlockUiState by navigationViewModel.unlockUiState.collectAsStateWithLifecycle()

    val navigateToRecord by navigationViewModel.navigateToRecord.collectAsStateWithLifecycle()
    val navigateToPet by navigationViewModel.navigateToPet.collectAsStateWithLifecycle()
    val navigateToVault by navigationViewModel.navigateToVault.collectAsStateWithLifecycle()

    val navigateToSearch by navigationViewModel.navigateToSearch.collectAsStateWithLifecycle()
    val pendingSearchQuery by navigationViewModel.pendingSearchQuery.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        navigationViewModel.consumePendingNavigation()
    }

    LaunchedEffect(currentRoute) {
        activityInsightsTracker.trackScreenView(currentRoute)
    }

    LaunchedEffect(navigateToRecord) {
        navigateToRecord?.let { recordId ->
            navController.navigate(DocuFindRoutes.recordDetail(recordId))
            navigationViewModel.onRecordNavigationHandled()
        }
    }

    LaunchedEffect(navigateToPet) {
        navigateToPet?.let { petId ->
            navController.navigate(DocuFindRoutes.petDetail(petId))
            navigationViewModel.onPetNavigationHandled()
        }
    }

    fun navigateToTab(route: String) {

        navController.navigate(route) {

            popUpTo(navController.graph.findStartDestination().id) {

                saveState = true

            }

            launchSingleTop = true

            restoreState = true

        }

    }

    LaunchedEffect(navigateToVault) {
        if (navigateToVault) {
            navigateToTab(DocuFindRoutes.VAULT)
            navigationViewModel.onVaultNavigationHandled()
        }
    }

    UnlockOverlay(
        visible = unlockPrompt.visible,
        authError = unlockAuthError,
        biometricEnabled = unlockUiState.first,
        biometricAvailable = navigationViewModel.biometricAvailable,
        isAuthenticating = isUnlockAuthenticating,
        onDismiss = navigationViewModel::dismissUnlockPrompt,
        onPinComplete = { pin ->
            activity?.let { navigationViewModel.unlockWithPin(it, pin) }
        },
        onBiometric = {
            activity?.let { navigationViewModel.unlockWithBiometric(it) }
        },
        onForgotPinBiometricReset = {
            activity?.let { navigationViewModel.resetPinWithBiometric(it) }
        },
        onForgotPinConfirmReset = navigationViewModel::resetPinWithoutBiometric
    )

    LaunchedEffect(navigateToSearch) {
        if (navigateToSearch) {
            navController.navigate(DocuFindRoutes.SEARCH) {
                launchSingleTop = true
            }
            navigationViewModel.onSearchNavigationHandled()
        }
    }



    Scaffold(

        bottomBar = {

            if (showBottomBar) {

                DocuFindBottomBar(

                    currentRoute = currentRoute,

                    onNavigate = ::navigateToTab

                )

            }

        }

    ) { padding ->

        NavHost(

            navController = navController,

            startDestination = DocuFindRoutes.HOME,

            modifier = Modifier.padding(padding)

        ) {

            composable(DocuFindRoutes.HOME) {

                HomeScreen(

                    onQuickAccessClick = { item ->
                        when (item) {
                            QuickAccessItem.REMINDERS -> navigateToTab(DocuFindRoutes.REMINDERS)
                            QuickAccessItem.EMERGENCY ->
                                navController.navigate(DocuFindRoutes.EMERGENCY_CONTACTS)
                            QuickAccessItem.FAMILY ->
                                navController.navigate(DocuFindRoutes.category("family"))
                            QuickAccessItem.PETS ->
                                navController.navigate(DocuFindRoutes.category("pets"))
                            QuickAccessItem.MORE ->
                                navController.navigate(DocuFindRoutes.category("more"))
                            else ->
                                navController.navigate(DocuFindRoutes.category(item.id))
                        }
                    },

                    onRecentItemClick = { record ->
                        navigationViewModel.openRecord(record.id)
                    },
                    onExpiringItemClick = { recordId ->
                        navigationViewModel.openRecord(recordId)
                    },
                    onNavigateReminders = { navigateToTab(DocuFindRoutes.REMINDERS) },

                    onNavigateScanQr = {
                        navController.navigate(DocuFindRoutes.SCAN_QR) { launchSingleTop = true }
                    },

                    onNavigateSearch = { query -> navigationViewModel.openSearch(query) }

                )

            }

            composable(DocuFindRoutes.SCAN_QR) {
                QrScannerScreen(onBack = { navController.popBackStack() })
            }

            composable(DocuFindRoutes.VAULT) {

                VaultTabScreen(
                    onRecordClick = { recordId ->
                        navigationViewModel.openRecord(recordId)
                    }
                )

            }

            composable(DocuFindRoutes.SEARCH) {

                SearchScreen(
                    initialQuery = pendingSearchQuery,
                    onBack = { navController.popBackStack() },
                    onRecordClick = { recordId ->
                        if (recordId.isNotBlank()) {
                            navigationViewModel.openRecord(recordId)
                        }
                    }
                )

            }

            composable(DocuFindRoutes.ADD) {

                AddScreen(
                    onDocumentSaved = { recordId ->
                        navigationViewModel.openRecord(recordId)
                    }
                )

            }

            composable(DocuFindRoutes.REMINDERS) {

                RemindersScreen(
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onReminderClick = { reminder: ReminderListItem ->
                        when {
                            reminder.linkedRecordId != null ->
                                navigationViewModel.openRecord(reminder.linkedRecordId)
                            reminder.linkedPetId != null ->
                                navigationViewModel.openPet(reminder.linkedPetId)
                        }
                    }
                )

            }

            composable(DocuFindRoutes.SETTINGS) {

                SettingsScreen(
                    onNavigatePrivacy = {
                        navController.navigate(DocuFindRoutes.PRIVACY) { launchSingleTop = true }
                    },
                    onNavigateBackup = {
                        navController.navigate(DocuFindRoutes.BACKUP) { launchSingleTop = true }
                    },
                    onNavigateStorage = {
                        navController.navigate(DocuFindRoutes.STORAGE) { launchSingleTop = true }
                    },
                    onNavigateSecurity = {
                        navController.navigate(DocuFindRoutes.SECURITY) { launchSingleTop = true }
                    },
                    onNavigateTrash = {
                        navController.navigate(DocuFindRoutes.TRASH) { launchSingleTop = true }
                    },
                    onNavigateActivityInsights = {
                        navController.navigate(DocuFindRoutes.ACTIVITY_INSIGHTS) { launchSingleTop = true }
                    },
                    onNavigateFamily = {
                        navController.navigate(DocuFindRoutes.category("family")) { launchSingleTop = true }
                    },
                    onNavigateEmergency = {
                        navController.navigate(DocuFindRoutes.EMERGENCY_CONTACTS) { launchSingleTop = true }
                    },
                    onNavigateHowToUse = {
                        navController.navigate(DocuFindRoutes.HOW_TO_USE) { launchSingleTop = true }
                    },
                    onNavigateHelpSupport = {
                        navController.navigate(DocuFindRoutes.HELP_SUPPORT) { launchSingleTop = true }
                    },
                    onNavigateAbout = {
                        navController.navigate(DocuFindRoutes.ABOUT) { launchSingleTop = true }
                    }
                )

            }

            composable(DocuFindRoutes.HOW_TO_USE) {
                HowToUseScreen(onBack = { navController.popBackStack() })
            }

            composable(DocuFindRoutes.SECURITY) {

                SecuritySettingsScreen(onBack = { navController.popBackStack() })

            }

            composable(DocuFindRoutes.TRASH) {
                com.docufind.app.ui.screens.trash.TrashScreen(onBack = { navController.popBackStack() })
            }

            composable(DocuFindRoutes.ACTIVITY_INSIGHTS) {

                ActivityInsightsScreen(onBack = { navController.popBackStack() })

            }

            composable(DocuFindRoutes.HELP_SUPPORT) {

                HelpSupportScreen(
                    onBack = { navController.popBackStack() },
                    onReportBug = {
                        navController.navigate(DocuFindRoutes.REPORT_BUG) { launchSingleTop = true }
                    },
                    onSendFeedback = {
                        navController.navigate(DocuFindRoutes.SEND_FEEDBACK) { launchSingleTop = true }
                    },
                    onPrivacy = {
                        navController.navigate(DocuFindRoutes.PRIVACY) { launchSingleTop = true }
                    }
                )

            }

            composable(DocuFindRoutes.REPORT_BUG) {

                ReportBugScreen(onBack = { navController.popBackStack() })

            }

            composable(DocuFindRoutes.SEND_FEEDBACK) {

                SendFeedbackScreen(onBack = { navController.popBackStack() })

            }

            composable(DocuFindRoutes.ABOUT) {

                AboutScreen(onBack = { navController.popBackStack() })

            }

            composable(DocuFindRoutes.BACKUP) {

                BackupScreen(onBack = { navController.popBackStack() })

            }

            composable(DocuFindRoutes.STORAGE) {

                StorageScreen(onBack = { navController.popBackStack() })

            }

            composable(DocuFindRoutes.PRIVACY) {

                PrivacyScreen(onBack = { navController.popBackStack() })

            }

            composable(

                route = DocuFindRoutes.CATEGORY,

                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })

            ) { backStackEntry ->

                val categoryId = backStackEntry.arguments?.getString("categoryId").orEmpty()

                when {

                    categoryId == "more" -> {

                        ModuleHubScreen(

                            onBack = { navController.popBackStack() },

                            onModuleClick = { moduleId ->

                                navController.navigate(DocuFindRoutes.category(moduleId))

                            }

                        )

                    }

                    DocuFindModule.isSupported(categoryId) -> {

                        ModuleListScreen(

                            onBack = { navController.popBackStack() },

                            onRecordClick = { recordId ->
                                if (recordId.isNotBlank()) {
                                    navigationViewModel.openRecord(recordId)
                                }
                            },

                            onAddClick = { navigateToTab(DocuFindRoutes.ADD) }

                        )

                    }

                    categoryId == "family" -> {

                        FamilyListScreen(

                            onBack = { navController.popBackStack() },

                            onMemberClick = { memberId ->
                                if (memberId.isNotBlank()) {
                                    navController.navigate(DocuFindRoutes.familyDetail(memberId))
                                }
                            }

                        )

                    }

                    categoryId == "pets" -> {

                        PetListScreen(

                            onBack = { navController.popBackStack() },

                            onPetClick = { petId ->
                                if (petId.isNotBlank()) {
                                    navController.navigate(DocuFindRoutes.petDetail(petId))
                                }
                            }

                        )

                    }

                    else -> {

                        CategoryPlaceholderScreen(

                            categoryId = categoryId,

                            onBack = { navController.popBackStack() }

                        )

                    }

                }

            }

            composable(

                route = DocuFindRoutes.RECORD_DETAIL,

                arguments = listOf(navArgument("recordId") { type = NavType.StringType })

            ) {

                ModuleDetailScreen(

                    onBack = { navController.popBackStack() },

                    onDeleted = { navController.popBackStack() },

                    onPreviewFile = { fileId ->
                        navController.navigate(DocuFindRoutes.filePreview(fileId))
                    },

                    onRequiresUnlock = { recordId ->
                        navController.popBackStack()
                        navigationViewModel.openRecord(recordId)
                    }

                )

            }

            composable(

                route = DocuFindRoutes.FILE_PREVIEW,

                arguments = listOf(navArgument("fileId") { type = NavType.StringType })

            ) {

                FilePreviewScreen(onBack = { navController.popBackStack() })

            }

            composable(

                route = DocuFindRoutes.FAMILY_DETAIL,

                arguments = listOf(navArgument("memberId") { type = NavType.StringType })

            ) {

                FamilyDetailScreen(

                    onBack = { navController.popBackStack() },

                    onDeleted = { navController.popBackStack() },

                    onOpenDocument = { recordId -> navigationViewModel.openRecord(recordId) }

                )

            }

            composable(DocuFindRoutes.EMERGENCY_CONTACTS) {

                EmergencyListScreen(onBack = { navController.popBackStack() })

            }

            composable(

                route = DocuFindRoutes.PET_DETAIL,

                arguments = listOf(navArgument("petId") { type = NavType.StringType })

            ) {

                PetDetailScreen(

                    onBack = { navController.popBackStack() },

                    onDeleted = { navController.popBackStack() }

                )

            }

        }

    }

}


