package com.docufind.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.docufind.app.R

@Composable
fun SplashUnlockAnimation(
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        onAnimationComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF08152B),
                    0.62f to Color(0xFF061126),
                    1f to Color(0xFF020814)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_docufind_mark_raster),
            contentDescription = "DocuFind",
            modifier = Modifier.size(140.dp),
            contentScale = ContentScale.Fit
        )
    }
}
