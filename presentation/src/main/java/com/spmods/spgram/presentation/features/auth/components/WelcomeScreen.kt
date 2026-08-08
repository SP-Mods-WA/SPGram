package com.spmods.spgram.presentation.features.auth.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.presentation.R
import com.spmods.spgram.presentation.core.ui.ExpressiveDefaults
import kotlinx.coroutines.launch

/**
 * The very first screen shown on a fresh install, before "Your Phone".
 *
 * Illustration-style onboarding: a horizontally swipeable set of feature
 * pages (fast messaging, privacy, groups/communities) followed by a final
 * welcome page with the primary call-to-action. Each page has its own
 * lightweight vector illustration (no external image assets needed) and a
 * soft animated gradient backdrop, in the calm M3 Expressive mood of the
 * rest of the auth flow.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.Bolt,
            titleRes = R.string.onboarding_page1_title,
            descRes = R.string.onboarding_page1_desc,
            accentIndex = 0
        ),
        OnboardingPage(
            icon = Icons.Filled.Lock,
            titleRes = R.string.onboarding_page2_title,
            descRes = R.string.onboarding_page2_desc,
            accentIndex = 1
        ),
        OnboardingPage(
            icon = Icons.Filled.Groups,
            titleRes = R.string.onboarding_page3_title,
            descRes = R.string.onboarding_page3_desc,
            accentIndex = 2
        ),
        OnboardingPage(
            icon = null,
            titleRes = R.string.onboarding_page4_title,
            descRes = R.string.onboarding_page4_desc,
            accentIndex = 0,
            isFinal = true
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScopeCompat()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedOnboardingBackdrop(pagerState = pagerState, pageCount = pages.size)

        Column(modifier = Modifier.fillMaxSize()) {
            // Top row: skip button, hidden on the final page.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < pages.lastIndex) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pages.lastIndex) }
                    }) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(page = pages[pageIndex])
            }

            // Dot indicators.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                pages.indices.forEach { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (selected) 24.dp else 8.dp,
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
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onContinue()
                        }
                    },
                    shapes = ExpressiveDefaults.extraLargeButtonShapes(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.lastIndex) {
                            stringResource(R.string.onboarding_next)
                        } else {
                            stringResource(R.string.welcome_start_messaging)
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.welcome_terms_notice),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private data class OnboardingPage(
    val icon: ImageVector?,
    val titleRes: Int,
    val descRes: Int,
    val accentIndex: Int,
    val isFinal: Boolean = false
)

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (page.isFinal) {
            AppLogoBadge()
        } else {
            FeatureIllustration(icon = page.icon, accentIndex = page.accentIndex)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(page.descRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
    }
}

/**
 * A soft circular badge with a gently pulsing ring behind a feature icon —
 * a lightweight vector "illustration" that avoids needing external image
 * assets for each onboarding page.
 */
@Composable
private fun FeatureIllustration(icon: ImageVector?, accentIndex: Int) {
    val pulse = rememberInfiniteTransition(label = "iconPulse")
    val scale by pulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    val (containerColor, accentColor) = accentColorsFor(accentIndex)

    Box(
        modifier = Modifier
            .size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((180 * scale).dp)
                .clip(CircleShape)
                .background(containerColor.copy(alpha = 0.35f))
        )
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(containerColor, accentColor)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
    }
}

@Composable
private fun AppLogoBadge() {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_app_logo),
            contentDescription = null,
            modifier = Modifier.size(104.dp)
        )
    }
}

@Composable
private fun accentColorsFor(index: Int): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (index % 3) {
        0 -> scheme.primary to scheme.tertiary
        1 -> scheme.tertiary to scheme.primary
        else -> scheme.secondary to scheme.primary
    }
}

/**
 * Full-bleed backdrop whose gradient tint softly crossfades based on which
 * onboarding page is currently focused, giving each page a distinct but
 * cohesive mood without needing separate background images.
 */
@Composable
private fun AnimatedOnboardingBackdrop(pagerState: PagerState, pageCount: Int) {
    val scheme = MaterialTheme.colorScheme
    val progress = pagerState.currentPage + pagerState.currentPageOffsetFraction

    val topColor by animateColorAsState(
        targetValue = when {
            progress < 1f -> lerpColor(scheme.primaryContainer, scheme.tertiaryContainer, progress.coerceIn(0f, 1f))
            progress < 2f -> lerpColor(scheme.tertiaryContainer, scheme.secondaryContainer, (progress - 1f).coerceIn(0f, 1f))
            else -> lerpColor(scheme.secondaryContainer, scheme.primaryContainer, (progress - 2f).coerceIn(0f, 1f))
        },
        animationSpec = tween(durationMillis = 300),
        label = "backdropTop"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        topColor.copy(alpha = 0.45f),
                        scheme.background
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(0f, 900f)
                )
            )
    )
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomeScreenPreview() {
    MaterialTheme {
        WelcomeScreen(onContinue = {})
    }
}
