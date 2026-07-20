package com.docufind.app.ui.screens.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docufind.app.R
import com.docufind.app.ui.components.SplashUnlockAnimation
import com.docufind.app.ui.screens.startup.DatabaseMigrationErrorScreen

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val startupState by viewModel.startupState.collectAsStateWithLifecycle()
    val migrationError by viewModel.migrationError.collectAsStateWithLifecycle()

    var animationDone by remember { mutableStateOf(false) }
    var exiting by remember { mutableStateOf(false) }

    LaunchedEffect(destination, animationDone, exiting) {
        if (animationDone && !exiting) {
            destination?.let { route ->
                exiting = true
            }
        }
    }

    LaunchedEffect(exiting, destination) {
        if (exiting) {
            destination?.let { route ->
                kotlinx.coroutines.delay(280)
                onNavigate(route)
                viewModel.onNavigated()
            }
        }
    }

    when (startupState) {
        DatabaseStartupState.FAILED -> {
            DatabaseMigrationErrorScreen(
                message = migrationError?.message
                    ?: stringResource(R.string.migration_error_generic)
            )
        }
        else -> {
            AnimatedVisibility(
                visible = !exiting,
                exit = fadeOut(animationSpec = tween(280))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SplashUnlockAnimation(
                        modifier = Modifier.fillMaxSize(),
                        onAnimationComplete = {
                            animationDone = true
                            viewModel.onAnimationComplete()
                        }
                    )
                }
            }
        }
    }
}
