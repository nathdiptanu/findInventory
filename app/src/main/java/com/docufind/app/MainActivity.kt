package com.docufind.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.docufind.app.insights.ActivityInsightsTracker
import com.docufind.app.reminder.NotificationPermissionTracker
import com.docufind.app.reminder.PendingReminderNavigation
import com.docufind.app.reminder.ReminderPendingNavigation
import com.docufind.app.security.session.ScreenTimeoutManager
import com.docufind.app.ui.DocuFindApp
import com.docufind.app.ui.theme.DocuFindTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var reminderPendingNavigation: ReminderPendingNavigation
    @Inject lateinit var screenTimeoutManager: ScreenTimeoutManager
    @Inject lateinit var notificationPermissionTracker: NotificationPermissionTracker
    @Inject lateinit var activityInsightsTracker: ActivityInsightsTracker

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationPermissionTracker.refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleReminderIntent(intent)
        setContent {
            DocuFindTheme {
                DocuFindApp(
                    activityInsightsTracker = activityInsightsTracker,
                    onRequestNotificationPermission = ::requestNotificationPermissionIfNeeded
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activityInsightsTracker.onAppForegrounded()
    }

    override fun onStop() {
        activityInsightsTracker.onAppBackgrounded()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminderIntent(intent)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        screenTimeoutManager.onUserActivity()
        screenTimeoutManager.checkTimeoutAndLock()
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                notificationPermissionTracker.refresh()
            }
        }
    }

    private fun handleReminderIntent(intent: Intent?) {
        val reminderId = intent?.getStringExtra(EXTRA_REMINDER_ID) ?: return
        reminderPendingNavigation.setPending(
            PendingReminderNavigation(
                reminderId = reminderId,
                linkedRecordId = intent.getStringExtra(EXTRA_LINKED_RECORD_ID),
                linkedPetId = intent.getStringExtra(EXTRA_LINKED_PET_ID),
                linkedPetRecordId = intent.getStringExtra(EXTRA_LINKED_PET_RECORD_ID),
                requiresUnlock = intent.getBooleanExtra(EXTRA_REQUIRES_UNLOCK, true)
            )
        )
        intent.removeExtra(EXTRA_REMINDER_ID)
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_LINKED_RECORD_ID = "extra_linked_record_id"
        const val EXTRA_LINKED_PET_ID = "extra_linked_pet_id"
        const val EXTRA_LINKED_PET_RECORD_ID = "extra_linked_pet_record_id"
        const val EXTRA_REQUIRES_UNLOCK = "extra_requires_unlock"
    }
}
