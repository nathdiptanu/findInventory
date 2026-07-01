package com.docufind.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.docufind.app.R
import com.docufind.app.ui.theme.DocuFindBrandAccent
import com.docufind.app.ui.theme.DocuFindBrandLight
import com.docufind.app.ui.theme.DocuFindBrandNavy
import com.docufind.app.ui.theme.DocuFindBrandPrimary
import com.docufind.app.ui.theme.DocuFindBrandSoftBg
import com.docufind.app.ui.theme.DocuFindWhite
import com.docufind.app.ui.util.isReducedMotionEnabled
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private const val FULL_DURATION_MS = 5500
private const val REDUCED_DURATION_MS = 550

private val BrandDarkBlue = Color(0xFF1E2A3A)
private val BrandSlate = Color(0xFF334155)
private val BrandSuccess = Color(0xFF10B981)

private fun phase(progress: Float, start: Float, end: Float): Float =
    ((progress - start) / (end - start)).coerceIn(0f, 1f)

private fun fadeBetween(progress: Float, inStart: Float, inEnd: Float, outStart: Float, outEnd: Float): Float {
    val ease = FastOutSlowInEasing
    return ease.transform(phase(progress, inStart, inEnd)) *
        (1f - ease.transform(phase(progress, outStart, outEnd)))
}

@Composable
fun SplashUnlockAnimation(
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val reducedMotion = remember { context.isReducedMotionEnabled() }
    val progress = remember { Animatable(0f) }
    val duration = if (reducedMotion) REDUCED_DURATION_MS else FULL_DURATION_MS
    val taglines = remember {
        listOf(
            "Security you can trust.",
            "Your Life. Securely Organized.",
            "Private by Design.",
            "Everything Important. One Secure Place.",
            "Find. Protect. Remember.",
            "Search Smart. Store Secure.",
            "Your Documents. Always Within Reach."
        )
    }
    val launchTagline = remember {
        taglines[((System.currentTimeMillis() / 1000L) % taglines.size).toInt()]
    }

    LaunchedEffect(reducedMotion) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = duration, easing = LinearEasing))
        delay(if (reducedMotion) 80L else 120L)
        onAnimationComplete()
    }

    val p = progress.value
    val ease = FastOutSlowInEasing
    val bgIn = ease.transform(phase(p, 0f, 0.12f))
    val lensAlpha = if (reducedMotion) 0f else fadeBetween(p, 0.08f, 0.18f, 0.55f, 0.66f)
    val docsAlpha = if (reducedMotion) 0f else fadeBetween(p, 0.18f, 0.34f, 0.50f, 0.62f)
    val scanAlpha = if (reducedMotion) 0f else fadeBetween(p, 0.28f, 0.38f, 0.48f, 0.56f)
    val foundAlpha = if (reducedMotion) 0f else fadeBetween(p, 0.42f, 0.52f, 0.58f, 0.66f)
    val folderAlpha = if (reducedMotion) 0f else fadeBetween(p, 0.52f, 0.64f, 0.68f, 0.76f)
    val shieldAlpha = if (reducedMotion) 0f else fadeBetween(p, 0.64f, 0.76f, 0.78f, 0.86f)
    val brandRevealAlpha = ease.transform(phase(p, if (reducedMotion) 0.20f else 0.76f, if (reducedMotion) 0.75f else 0.88f))
    val taglineAlpha = ease.transform(phase(p, if (reducedMotion) 0.45f else 0.84f, if (reducedMotion) 0.95f else 0.94f))
    val exitScale = 1f - ease.transform(phase(p, 0.94f, 1f)) * 0.035f

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPremiumBackground(bgIn)

            val cx = size.width / 2f
            val cy = size.height * 0.43f
            val scene = size.minDimension.coerceAtMost(size.maxDimension * 0.64f)

            if (docsAlpha > 0.01f) {
                drawDocumentConstellation(cx, cy, scene, docsAlpha, phase(p, 0.18f, 0.34f))
            }
            if (scanAlpha > 0.01f) {
                drawScanningBeam(cx, cy, scene, ease.transform(phase(p, 0.30f, 0.52f)), scanAlpha)
            }
            if (lensAlpha > 0.01f) {
                val enter = ease.transform(phase(p, 0.08f, 0.28f))
                val scan = ease.transform(phase(p, 0.28f, 0.52f))
                drawMagnifyingLens(
                    cx = cx - scene * 0.18f + scene * 0.32f * scan,
                    cy = size.height * (0.62f - 0.19f * enter),
                    scene = scene,
                    progress = p,
                    alpha = lensAlpha
                )
            }
            if (foundAlpha > 0.01f) {
                drawFoundDocument(cx, cy, scene, foundAlpha)
            }
            if (folderAlpha > 0.01f) {
                drawOrganizedFolder(cx, cy + scene * 0.04f, scene, folderAlpha)
            }
            if (shieldAlpha > 0.01f) {
                drawShieldLock(cx, cy, scene, shieldAlpha)
            }
            if (brandRevealAlpha > 0.01f) {
                drawBrandRevealRings(cx, cy, scene, brandRevealAlpha)
            }
        }

        Image(
            painter = painterResource(R.drawable.ic_docufind_mark),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(132.dp)
                .scale((0.92f + brandRevealAlpha * 0.08f) * exitScale)
                .alpha(brandRevealAlpha),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 188.dp)
                .scale(exitScale)
                .alpha(taglineAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DocuFindWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = launchTagline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = DocuFindBrandSoftBg.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

private fun DrawScope.drawPremiumBackground(alpha: Float) {
    drawRect(DocuFindBrandNavy)
    drawRect(
        brush = Brush.verticalGradient(
            listOf(DocuFindBrandNavy, BrandDarkBlue.copy(alpha = 0.86f), DocuFindBrandNavy)
        )
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                DocuFindBrandPrimary.copy(alpha = 0.56f * alpha),
                DocuFindBrandAccent.copy(alpha = 0.24f * alpha),
                Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * 0.82f),
            radius = size.minDimension * 0.62f
        ),
        radius = size.minDimension * 0.62f,
        center = Offset(size.width * 0.5f, size.height * 0.82f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                DocuFindBrandLight.copy(alpha = 0.24f * alpha),
                DocuFindBrandAccent.copy(alpha = 0.12f * alpha),
                Color.Transparent
            ),
            center = Offset(size.width * 0.50f, size.height * 0.86f),
            radius = size.width * 0.56f
        ),
        radius = size.width * 0.56f,
        center = Offset(size.width * 0.50f, size.height * 0.86f)
    )
    drawArc(
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                DocuFindBrandPrimary.copy(alpha = 0.10f * alpha),
                DocuFindBrandLight.copy(alpha = 0.72f * alpha),
                DocuFindBrandAccent.copy(alpha = 0.24f * alpha),
                Color.Transparent
            )
        ),
        startAngle = 206f,
        sweepAngle = 128f,
        useCenter = false,
        topLeft = Offset(-size.width * 0.20f, size.height * 0.715f),
        size = Size(size.width * 1.40f, size.height * 0.50f),
        style = Stroke(width = size.minDimension * 0.026f, cap = StrokeCap.Round)
    )
    drawArc(
        color = DocuFindBrandLight.copy(alpha = 0.42f * alpha),
        startAngle = 205f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(-size.width * 0.18f, size.height * 0.72f),
        size = Size(size.width * 1.36f, size.height * 0.48f),
        style = Stroke(width = size.minDimension * 0.008f, cap = StrokeCap.Round)
    )
    drawArc(
        color = DocuFindWhite.copy(alpha = 0.60f * alpha),
        startAngle = 236f,
        sweepAngle = 54f,
        useCenter = false,
        topLeft = Offset(-size.width * 0.18f, size.height * 0.72f),
        size = Size(size.width * 1.36f, size.height * 0.48f),
        style = Stroke(width = size.minDimension * 0.004f, cap = StrokeCap.Round)
    )
    drawHorizonLights(alpha)
}

private fun DrawScope.drawHorizonLights(alpha: Float) {
    val lights = listOf(
        Offset(0.31f, 0.825f) to 0.010f,
        Offset(0.39f, 0.805f) to 0.006f,
        Offset(0.48f, 0.795f) to 0.012f,
        Offset(0.58f, 0.805f) to 0.007f,
        Offset(0.68f, 0.828f) to 0.009f
    )
    lights.forEach { (offset, radiusFactor) ->
        val center = Offset(size.width * offset.x, size.height * offset.y)
        val radius = size.minDimension * radiusFactor
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    DocuFindWhite.copy(alpha = 0.78f * alpha),
                    DocuFindBrandLight.copy(alpha = 0.32f * alpha),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 4.5f
            ),
            radius = radius * 4.5f,
            center = center
        )
        drawCircle(
            color = DocuFindWhite.copy(alpha = 0.74f * alpha),
            radius = radius,
            center = center
        )
    }
}

private fun DrawScope.drawDocumentConstellation(cx: Float, cy: Float, scene: Float, alpha: Float, spread: Float) {
    val offsets = listOf(
        Offset(-0.34f, -0.24f), Offset(-0.08f, -0.30f), Offset(0.26f, -0.25f),
        Offset(-0.36f, 0.08f), Offset(-0.04f, 0.05f), Offset(0.32f, 0.09f),
        Offset(-0.20f, 0.28f), Offset(0.18f, 0.30f)
    )
    offsets.forEachIndexed { index, offset ->
        val settle = 0.78f + spread * 0.22f
        val x = cx + scene * offset.x * settle
        val y = cy + scene * offset.y * settle
        val iconAlpha = alpha * (0.46f + (index % 3) * 0.12f)
        when (index % 4) {
            0 -> drawDocumentIcon(x, y, scene * 0.105f, iconAlpha)
            1 -> drawFolderIcon(x, y, scene * 0.112f, iconAlpha)
            2 -> drawImageIcon(x, y, scene * 0.104f, iconAlpha)
            else -> drawLockFileIcon(x, y, scene * 0.102f, iconAlpha)
        }
    }
}

private fun DrawScope.drawScanningBeam(cx: Float, cy: Float, scene: Float, scan: Float, alpha: Float) {
    val startX = cx - scene * 0.45f
    val endX = cx + scene * 0.45f
    val beamX = startX + (endX - startX) * scan
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                DocuFindBrandAccent.copy(alpha = 0.16f * alpha),
                DocuFindBrandLight.copy(alpha = 0.86f * alpha),
                DocuFindBrandAccent.copy(alpha = 0.16f * alpha),
                Color.Transparent
            )
        ),
        start = Offset(startX, cy),
        end = Offset(endX, cy),
        strokeWidth = scene * 0.014f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = DocuFindBrandLight.copy(alpha = 0.45f * alpha),
        start = Offset(beamX, cy - scene * 0.18f),
        end = Offset(beamX, cy + scene * 0.18f),
        strokeWidth = scene * 0.005f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawMagnifyingLens(cx: Float, cy: Float, scene: Float, progress: Float, alpha: Float) {
    val r = scene * 0.155f
    repeat(3) { index ->
        val inset = scene * (0.02f + index * 0.018f)
        drawArc(
            color = DocuFindBrandAccent.copy(alpha = (0.42f - index * 0.10f) * alpha),
            startAngle = progress * 720f + index * 118f,
            sweepAngle = 82f,
            useCenter = false,
            topLeft = Offset(cx - r - inset, cy - r - inset),
            size = Size((r + inset) * 2f, (r + inset) * 2f),
            style = Stroke(width = scene * 0.008f, cap = StrokeCap.Round)
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            listOf(DocuFindBrandAccent.copy(alpha = 0.18f * alpha), Color.Transparent),
            center = Offset(cx, cy),
            radius = r * 1.35f
        ),
        radius = r * 1.35f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = DocuFindBrandSoftBg.copy(alpha = 0.86f * alpha),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = scene * 0.016f, cap = StrokeCap.Round)
    )
    drawCircle(
        color = DocuFindBrandLight.copy(alpha = 0.56f * alpha),
        radius = r * 0.82f,
        center = Offset(cx, cy),
        style = Stroke(width = scene * 0.006f, cap = StrokeCap.Round)
    )
    val angle = Math.toRadians(45.0)
    val handleStart = Offset(cx + r * 0.68f, cy + r * 0.68f)
    val handleEnd = Offset(
        handleStart.x + r * 0.92f * cos(angle).toFloat(),
        handleStart.y + r * 0.92f * sin(angle).toFloat()
    )
    drawLine(DocuFindBrandSoftBg.copy(alpha = 0.88f * alpha), handleStart, handleEnd, scene * 0.022f, StrokeCap.Round)
    drawLine(BrandSlate.copy(alpha = 0.95f * alpha), handleStart, handleEnd, scene * 0.012f, StrokeCap.Round)
}

private fun DrawScope.drawFoundDocument(cx: Float, cy: Float, scene: Float, alpha: Float) {
    drawDocumentIcon(cx, cy, scene * 0.18f, alpha)
    val r = scene * 0.045f
    val checkCenter = Offset(cx + scene * 0.075f, cy + scene * 0.075f)
    drawCircle(BrandSuccess.copy(alpha = alpha), r, checkCenter)
    drawLine(DocuFindWhite.copy(alpha = alpha), Offset(checkCenter.x - r * 0.42f, checkCenter.y), Offset(checkCenter.x - r * 0.12f, checkCenter.y + r * 0.30f), scene * 0.010f, StrokeCap.Round)
    drawLine(DocuFindWhite.copy(alpha = alpha), Offset(checkCenter.x - r * 0.12f, checkCenter.y + r * 0.30f), Offset(checkCenter.x + r * 0.48f, checkCenter.y - r * 0.42f), scene * 0.010f, StrokeCap.Round)
}

private fun DrawScope.drawOrganizedFolder(cx: Float, cy: Float, scene: Float, alpha: Float) {
    val w = scene * 0.34f
    val h = scene * 0.22f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(DocuFindBrandAccent.copy(alpha = 0.45f * alpha), Color.Transparent),
            center = Offset(cx, cy),
            radius = w * 0.85f
        ),
        radius = w * 0.85f,
        center = Offset(cx, cy)
    )
    drawRoundRect(
        color = DocuFindBrandLight.copy(alpha = 0.80f * alpha),
        topLeft = Offset(cx - w * 0.36f, cy - h * 0.62f),
        size = Size(w * 0.35f, h * 0.22f),
        cornerRadius = CornerRadius(scene * 0.018f)
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(DocuFindBrandLight.copy(alpha = 0.80f * alpha), DocuFindBrandPrimary.copy(alpha = 0.78f * alpha))
        ),
        topLeft = Offset(cx - w / 2f, cy - h * 0.35f),
        size = Size(w, h),
        cornerRadius = CornerRadius(scene * 0.035f)
    )
    drawRoundRect(
        color = DocuFindBrandLight.copy(alpha = 0.75f * alpha),
        topLeft = Offset(cx - w / 2f, cy - h * 0.35f),
        size = Size(w, h),
        cornerRadius = CornerRadius(scene * 0.035f),
        style = Stroke(width = scene * 0.006f)
    )
}

private fun DrawScope.drawShieldLock(cx: Float, cy: Float, scene: Float, alpha: Float) {
    val shield = Path().apply {
        val w = scene * 0.21f
        val h = scene * 0.27f
        moveTo(cx, cy - h * 0.72f)
        cubicTo(cx + w, cy - h * 0.52f, cx + w * 0.94f, cy + h * 0.18f, cx, cy + h * 0.70f)
        cubicTo(cx - w * 0.94f, cy + h * 0.18f, cx - w, cy - h * 0.52f, cx, cy - h * 0.72f)
        close()
    }
    drawPath(shield, DocuFindBrandAccent.copy(alpha = 0.22f * alpha), style = Stroke(width = scene * 0.038f, cap = StrokeCap.Round))
    drawPath(shield, DocuFindBrandLight.copy(alpha = 0.95f * alpha), style = Stroke(width = scene * 0.014f, cap = StrokeCap.Round))
    drawPath(shield, Brush.verticalGradient(listOf(DocuFindBrandLight.copy(alpha = 0.20f * alpha), DocuFindBrandPrimary.copy(alpha = 0.10f * alpha))))
    val bodyW = scene * 0.082f
    val bodyH = scene * 0.074f
    drawRoundRect(DocuFindWhite.copy(alpha = 0.95f * alpha), Offset(cx - bodyW / 2f, cy - bodyH * 0.08f), Size(bodyW, bodyH), CornerRadius(scene * 0.012f))
    drawArc(DocuFindWhite.copy(alpha = 0.95f * alpha), 180f, 180f, false, Offset(cx - bodyW * 0.45f, cy - bodyH * 0.86f), Size(bodyW * 0.9f, bodyH * 1.1f), style = Stroke(width = scene * 0.011f, cap = StrokeCap.Round))
}

private fun DrawScope.drawBrandRevealRings(cx: Float, cy: Float, scene: Float, alpha: Float) {
    repeat(4) { index ->
        drawCircle(
            color = DocuFindBrandPrimary.copy(alpha = (0.18f - index * 0.032f).coerceAtLeast(0f) * alpha),
            radius = scene * (0.16f + index * 0.055f),
            center = Offset(cx, cy),
            style = Stroke(width = scene * 0.004f, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawDocumentIcon(cx: Float, cy: Float, side: Float, alpha: Float) {
    val w = side * 0.72f
    val h = side
    val left = cx - w / 2f
    val top = cy - h / 2f
    drawRoundRect(DocuFindWhite.copy(alpha = 0.92f * alpha), Offset(left, top), Size(w, h), CornerRadius(side * 0.10f))
    drawPath(Path().apply {
        moveTo(left + w * 0.68f, top)
        lineTo(left + w, top + h * 0.26f)
        lineTo(left + w * 0.68f, top + h * 0.26f)
        close()
    }, DocuFindBrandSoftBg.copy(alpha = 0.92f * alpha))
    repeat(3) { line ->
        val y = top + h * (0.44f + line * 0.15f)
        drawLine(DocuFindBrandAccent.copy(alpha = 0.46f * alpha), Offset(left + w * 0.20f, y), Offset(left + w * 0.80f, y), side * 0.035f, StrokeCap.Round)
    }
}

private fun DrawScope.drawFolderIcon(cx: Float, cy: Float, side: Float, alpha: Float) {
    val w = side
    val h = side * 0.72f
    drawRoundRect(DocuFindBrandLight.copy(alpha = 0.34f * alpha), Offset(cx - w * 0.38f, cy - h * 0.48f), Size(w * 0.42f, h * 0.24f), CornerRadius(side * 0.07f))
    drawRoundRect(DocuFindBrandPrimary.copy(alpha = 0.24f * alpha), Offset(cx - w / 2f, cy - h * 0.28f), Size(w, h), CornerRadius(side * 0.11f), style = Stroke(width = side * 0.045f))
}

private fun DrawScope.drawImageIcon(cx: Float, cy: Float, side: Float, alpha: Float) {
    drawRoundRect(DocuFindBrandLight.copy(alpha = 0.22f * alpha), Offset(cx - side * 0.42f, cy - side * 0.42f), Size(side * 0.84f, side * 0.84f), CornerRadius(side * 0.10f), style = Stroke(width = side * 0.045f))
    drawCircle(DocuFindBrandLight.copy(alpha = 0.52f * alpha), side * 0.065f, Offset(cx - side * 0.18f, cy - side * 0.16f))
    drawPath(Path().apply {
        moveTo(cx - side * 0.30f, cy + side * 0.22f)
        lineTo(cx - side * 0.05f, cy)
        lineTo(cx + side * 0.10f, cy + side * 0.13f)
        lineTo(cx + side * 0.28f, cy - side * 0.08f)
        lineTo(cx + side * 0.34f, cy + side * 0.24f)
        close()
    }, DocuFindBrandAccent.copy(alpha = 0.36f * alpha))
}

private fun DrawScope.drawLockFileIcon(cx: Float, cy: Float, side: Float, alpha: Float) {
    drawRoundRect(DocuFindBrandLight.copy(alpha = 0.20f * alpha), Offset(cx - side * 0.36f, cy - side * 0.16f), Size(side * 0.72f, side * 0.54f), CornerRadius(side * 0.10f), style = Stroke(width = side * 0.044f))
    drawArc(DocuFindBrandLight.copy(alpha = 0.46f * alpha), 180f, 180f, false, Offset(cx - side * 0.23f, cy - side * 0.42f), Size(side * 0.46f, side * 0.44f), style = Stroke(width = side * 0.044f, cap = StrokeCap.Round))
}
