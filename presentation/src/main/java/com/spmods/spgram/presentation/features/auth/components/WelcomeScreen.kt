package com.spmods.spgram.presentation.features.auth.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.presentation.R
import kotlinx.coroutines.launch

/**
 * The very first screen shown on a fresh install, before "Your Phone".
 *
 * Card-on-color onboarding, matching the layout of the reference design:
 * a big curved brand-color backdrop, a white rounded card floating on top
 * holding a flat vector illustration, title, description, dot indicators,
 * a pill "Get Started" button, and a "Sign in" footer link.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onSignIn: () -> Unit = {}
) {
    val pages = listOf(
        OnboardingPage(
            illustration = IllustrationStyle.LOUNGE,
            titleRes = R.string.onboarding_page1_title,
            descRes = R.string.onboarding_page1_desc
        ),
        OnboardingPage(
            illustration = IllustrationStyle.FLYING,
            titleRes = R.string.onboarding_page2_title,
            descRes = R.string.onboarding_page2_desc
        ),
        OnboardingPage(
            illustration = IllustrationStyle.READING,
            titleRes = R.string.onboarding_page3_title,
            descRes = R.string.onboarding_page3_desc
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        // Soft curved accent blob peeking from the bottom-left, echoing the
        // reference design's orange/blue two-tone backdrop.
        BackdropBlob()

        Column(modifier = Modifier.fillMaxSize()) {
            // Close (X) button, top-left — matches the reference header.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onContinue) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Text(
                    text = "${pagerState.currentPage + 1} of ${pages.size}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) { pageIndex ->
                    OnboardingCard(
                        page = pages[pageIndex],
                        pagerState = pagerState,
                        pageIndex = pageIndex,
                        pageCount = pages.size,
                        isLastPage = pageIndex == pages.lastIndex,
                        onPrimaryClick = {
                            if (pageIndex < pages.lastIndex) {
                                scope.launch { pagerState.animateScrollToPage(pageIndex + 1) }
                            } else {
                                onContinue()
                            }
                        },
                        onSignIn = onSignIn
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private enum class IllustrationStyle { LOUNGE, FLYING, READING }

private data class OnboardingPage(
    val illustration: IllustrationStyle,
    val titleRes: Int,
    val descRes: Int
)

/**
 * The floating white card that holds one onboarding page: status-style
 * illustration area, title, description, dot indicators, CTA button and
 * sign-in footer — mirroring the reference screenshot's card structure.
 */
@Composable
private fun OnboardingCard(
    page: OnboardingPage,
    pagerState: PagerState,
    pageIndex: Int,
    pageCount: Int,
    isLastPage: Boolean,
    onPrimaryClick: () -> Unit,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Illustration area.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f),
            contentAlignment = Alignment.Center
        ) {
            FlatIllustration(style = page.illustration)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(page.descRes),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dot indicators.
        Row(horizontalArrangement = Arrangement.Center) {
            repeat(pageCount) { index ->
                val selected = pageIndex == index
                val width by animateDpAsState(
                    targetValue = if (selected) 18.dp else 6.dp,
                    label = "dotWidth"
                )
                val color by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    label = "dotColor"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.TextButton(
                onClick = onPrimaryClick,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(26.dp),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isLastPage) {
                        stringResource(R.string.welcome_start_messaging)
                    } else {
                        stringResource(R.string.onboarding_get_started)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row {
            Text(
                text = stringResource(R.string.onboarding_already_have_account),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.onboarding_sign_in),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onSignIn)
            )
        }
    }
}

/**
 * Soft two-tone curved backdrop behind the card — a simplified nod to the
 * reference design's blue/orange curved background, done with plain shapes
 * so no image assets are required.
 */
@Composable
private fun BackdropBlob() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomStart)
                .padding(bottom = 0.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Flat-style vector illustration drawn with Canvas: a simplified seated /
 * reclining figure plus supporting props (plant, paper airplane, speech
 * bubble), echoing the mood of the reference screenshots without
 * reproducing any copyrighted artwork.
 */
@Composable
private fun FlatIllustration(style: IllustrationStyle) {
    val primary = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.tertiary
    val skin = Color(0xFFF2C2A0)
    val hair = Color(0xFF2B2B3A)

    Canvas(modifier = Modifier.fillMaxSize()) {
        when (style) {
            IllustrationStyle.LOUNGE -> drawLounging(primary, accent, skin, hair)
            IllustrationStyle.FLYING -> drawFlying(primary, accent, skin, hair)
            IllustrationStyle.READING -> drawReading(primary, accent, skin, hair)
        }
    }
}

private fun DrawScope.drawFloorShadowPlane(accent: Color) {
    val w = size.width
    val h = size.height
    // Paper-plane-like diagonal shape, echoing the reference illustrations.
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(w * 0.05f, h * 0.62f)
        lineTo(w * 0.95f, h * 0.40f)
        lineTo(w * 0.55f, h * 0.55f)
        lineTo(w * 0.70f, h * 0.90f)
        close()
    }
    drawPath(path, color = accent.copy(alpha = 0.18f))
}

private fun DrawScope.drawPersonBase(
    skin: Color,
    hair: Color,
    topColor: Color,
    bottomColor: Color,
    centerX: Float,
    centerY: Float,
    scaleFactor: Float
) {
    val s = scaleFactor
    // Torso.
    drawRoundRect(
        color = topColor,
        topLeft = Offset(centerX - 30f * s, centerY - 40f * s),
        size = androidx.compose.ui.geometry.Size(60f * s, 70f * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f * s, 18f * s)
    )
    // Legs.
    drawRoundRect(
        color = bottomColor,
        topLeft = Offset(centerX - 26f * s, centerY + 20f * s),
        size = androidx.compose.ui.geometry.Size(52f * s, 60f * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f * s, 16f * s)
    )
    // Head.
    drawCircle(
        color = skin,
        radius = 22f * s,
        center = Offset(centerX, centerY - 62f * s)
    )
    // Hair.
    drawArc(
        color = hair,
        startAngle = 180f,
        sweepAngle = 200f,
        useCenter = true,
        topLeft = Offset(centerX - 24f * s, centerY - 84f * s),
        size = androidx.compose.ui.geometry.Size(48f * s, 44f * s)
    )
}

private fun DrawScope.drawSpeechBubble(cx: Float, cy: Float, s: Float, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - 26f * s, cy - 16f * s),
        size = androidx.compose.ui.geometry.Size(52f * s, 32f * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * s, 10f * s)
    )
}

private fun DrawScope.drawLounging(primary: Color, accent: Color, skin: Color, hair: Color) {
    drawFloorShadowPlane(accent)
    val cx = size.width * 0.42f
    val cy = size.height * 0.5f
    val s = size.minDimension / 260f

    // Crescent "chair".
    drawArc(
        color = primary.copy(alpha = 0.85f),
        startAngle = 90f,
        sweepAngle = 200f,
        useCenter = false,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 20f * s),
        topLeft = Offset(cx - 70f * s, cy - 70f * s),
        size = androidx.compose.ui.geometry.Size(140f * s, 140f * s)
    )

    drawPersonBase(skin, hair, accent, primary, cx, cy, s)
    drawSpeechBubble(cx + 46f * s, cy - 70f * s, s, primary.copy(alpha = 0.25f))

    // Plant pot, bottom-left.
    val potX = size.width * 0.12f
    val potY = size.height * 0.82f
    drawRoundRect(
        color = Color(0xFFB0BEC5),
        topLeft = Offset(potX - 16f * s, potY),
        size = androidx.compose.ui.geometry.Size(32f * s, 24f * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * s, 4f * s)
    )
    drawOval(
        color = Color(0xFF4CAF7D),
        topLeft = Offset(potX - 22f * s, potY - 44f * s),
        size = androidx.compose.ui.geometry.Size(24f * s, 48f * s)
    )
    drawOval(
        color = Color(0xFF4CAF7D),
        topLeft = Offset(potX + 2f * s, potY - 36f * s),
        size = androidx.compose.ui.geometry.Size(22f * s, 40f * s)
    )
}

private fun DrawScope.drawFlying(primary: Color, accent: Color, skin: Color, hair: Color) {
    drawFloorShadowPlane(primary)
    val cx = size.width * 0.5f
    val cy = size.height * 0.48f
    val s = size.minDimension / 260f

    drawPersonBase(skin, hair, primary, accent, cx, cy, s)
    drawSpeechBubble(cx - 10f * s, cy - 96f * s, s * 0.8f, accent.copy(alpha = 0.3f))
    drawSpeechBubble(cx + 40f * s, cy - 118f * s, s * 0.6f, primary.copy(alpha = 0.3f))
}

private fun DrawScope.drawReading(primary: Color, accent: Color, skin: Color, hair: Color) {
    drawFloorShadowPlane(accent)
    val cx = size.width * 0.5f
    val cy = size.height * 0.55f
    val s = size.minDimension / 260f

    // Reclining torso (wider, rotated feel via wide rounded rect).
    drawRoundRect(
        color = accent,
        topLeft = Offset(cx - 55f * s, cy - 10f * s),
        size = androidx.compose.ui.geometry.Size(110f * s, 44f * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f * s, 20f * s)
    )
    drawRoundRect(
        color = primary,
        topLeft = Offset(cx + 10f * s, cy + 6f * s),
        size = androidx.compose.ui.geometry.Size(70f * s, 30f * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f * s, 14f * s)
    )
    drawCircle(
        color = skin,
        radius = 20f * s,
        center = Offset(cx - 62f * s, cy + 6f * s)
    )
    drawArc(
        color = hair,
        startAngle = 160f,
        sweepAngle = 200f,
        useCenter = true,
        topLeft = Offset(cx - 84f * s, cy - 14f * s),
        size = androidx.compose.ui.geometry.Size(44f * s, 40f * s)
    )
    // Small tablet/book in front.
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(cx - 24f * s, cy + 14f * s),
        size = androidx.compose.ui.geometry.Size(40f * s, 26f * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * s, 4f * s)
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomeScreenPreview() {
    MaterialTheme {
        WelcomeScreen(onContinue = {})
    }
}
