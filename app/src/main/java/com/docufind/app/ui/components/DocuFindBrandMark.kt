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
 * Single brand source for DocuFind.
 *
 * - [DocuFindBrandMark] / [DocuFindLogo]: transparent D + keyhole (matches launcher mark)
 * - [DocuFindAppIcon]: full white-tile Play icon — About / marketing only
 */
@Composable
fun DocuFindBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    showShadow: Boolean = true
) {
    Image(
        painter = painterResource(R.drawable.ic_docufind_mark_clean),
        contentDescription = "DocuFind",
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

/** Full app tile matching Play Store / home-screen icon look (no tagline). */
@Composable
fun DocuFindAppIcon(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    Image(
        painter = painterResource(R.drawable.ic_docufind_app_icon),
        contentDescription = "DocuFind",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}
