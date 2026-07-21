package com.docufind.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.ui.components.DocuFindCard
import com.docufind.app.ui.components.SettingsListItem

private data class SettingsEntry(
    val titleRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigatePrivacy: () -> Unit = {},
    onNavigateBackup: () -> Unit = {},
    onNavigateStorage: () -> Unit = {},
    onNavigateSecurity: () -> Unit = {},
    onNavigateTrash: () -> Unit = {},
    onNavigateActivityInsights: () -> Unit = {},
    onNavigateFamily: () -> Unit = {},
    onNavigateEmergency: () -> Unit = {},
    onNavigateHowToUse: () -> Unit = {},
    onNavigateHelpSupport: () -> Unit = {},
    onNavigateAbout: () -> Unit = {}
) {
    val items = listOf(
        SettingsEntry(R.string.settings_security, Icons.Default.Security, onNavigateSecurity),
        SettingsEntry(R.string.settings_trash, Icons.Default.DeleteSweep, onNavigateTrash),
        SettingsEntry(R.string.settings_activity_insights, Icons.Default.Analytics, onNavigateActivityInsights),
        SettingsEntry(R.string.settings_backup, Icons.Default.Backup, onNavigateBackup),
        SettingsEntry(R.string.settings_storage, Icons.Default.Storage, onNavigateStorage),
        SettingsEntry(R.string.settings_family, Icons.Default.FamilyRestroom, onNavigateFamily),
        SettingsEntry(R.string.settings_emergency, Icons.Default.ContactEmergency, onNavigateEmergency),
        SettingsEntry(R.string.settings_how_to_use, Icons.AutoMirrored.Filled.MenuBook, onNavigateHowToUse),
        SettingsEntry(R.string.settings_help, Icons.AutoMirrored.Filled.Help, onNavigateHelpSupport),
        SettingsEntry(R.string.settings_privacy_policy, Icons.Default.Policy, onNavigatePrivacy),
        SettingsEntry(R.string.settings_about_docufind, Icons.Default.Info, onNavigateAbout)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            com.docufind.app.ui.components.DocuFindTopBar(
                title = stringResource(R.string.nav_settings)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                DocuFindCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        items.forEach { item ->
                            SettingsListItem(
                                title = stringResource(item.titleRes),
                                icon = item.icon,
                                onClick = item.onClick
                            )
                        }
                    }
                }
            }
        }
    }
}
