package com.docufind.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.docufind.app.R

/**
 * Official DocuFind brand mark — raster from approved logo reference.
 * Used consistently across splash, onboarding, settings, and in-app branding.
 */
@Composable
fun DocuFindBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    showShadow: Boolean = true
) {
    Image(
        painter = painterResource(R.drawable.ic_docufind_mark),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun DocuFindLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    DocuFindBrandMark(modifier = modifier, size = size, showShadow = false)
}
