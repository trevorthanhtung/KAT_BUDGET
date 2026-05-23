package com.katgr0up.katbudget.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.utils.KatSpringSpec
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class OnboardingPage(
    val titleResId: Int,
    val descResId: Int,
    val icon: ImageVector,
    val glowColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    colors: BudgetColors,
    onFinish: () -> Unit
) {
    val pages = rememberOnboardingPages()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { position ->
                OnboardingPageContent(
                    page = pages[position],
                    pageIndex = position,
                    pagerState = pagerState,
                    colors = colors
                )
            }

            OnboardingFooter(
                pageCount = pages.size,
                pagerState = pagerState,
                colors = colors,
                onNext = {
                    if (pagerState.currentPage == pages.lastIndex) {
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun OnboardingFooter(
    pageCount: Int,
    pagerState: PagerState,
    colors: BudgetColors,
    onNext: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = KatSpringSpec,
        label = "onboardingButtonScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WormIndicator(
            pageCount = pageCount,
            pagerState = pagerState,
            colors = colors,
            modifier = Modifier.padding(bottom = 26.dp)
        )

        Button(
            onClick = onNext,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .scale(buttonScale),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.background
            ),
            shape = RoundedCornerShape(22.dp)
        ) {
            Text(
                text = if (pagerState.currentPage == pageCount - 1) {
                    stringResource(id = R.string.onboarding_finish)
                } else {
                    stringResource(id = R.string.onboarding_next)
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun rememberOnboardingPages(): List<OnboardingPage> {
    return remember {
        listOf(
            OnboardingPage(
                titleResId = R.string.onboarding_page1_title,
                descResId = R.string.onboarding_page1_desc,
                icon = OnboardingIcons.Wallet,
                glowColor = Color(0xFF4ADE80)
            ),
            OnboardingPage(
                titleResId = R.string.onboarding_page2_title,
                descResId = R.string.onboarding_page2_desc,
                icon = OnboardingIcons.Chart,
                glowColor = Color(0xFF16A34A)
            ),
            OnboardingPage(
                titleResId = R.string.onboarding_page3_title,
                descResId = R.string.onboarding_page3_desc,
                icon = OnboardingIcons.GoalStar,
                glowColor = Color(0xFFF59E0B)
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    pageIndex: Int,
    pagerState: PagerState,
    colors: BudgetColors
) {
    val pageOffset by remember(pagerState, pageIndex) {
        derivedStateOf {
            ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).absoluteValue
        }
    }
    val transitionProgress = 1f - pageOffset.coerceIn(0f, 1f)
    val pageScale = 0.92f + 0.08f * transitionProgress
    val pageAlpha = 0.50f + 0.50f * transitionProgress

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = pageAlpha
                scaleX = pageScale
                scaleY = pageScale
                translationY = 30f * (1f - transitionProgress)
            }
            .padding(horizontal = 28.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlowingFeatureOrb(
            icon = page.icon,
            glowColor = page.glowColor,
            colors = colors,
            modifier = Modifier.size(174.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "0${pageIndex + 1}",
            color = colors.accent,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(id = page.titleResId),
            color = colors.text,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 31.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(id = page.descResId),
            color = colors.subText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun GlowingFeatureOrb(
    icon: ImageVector,
    glowColor: Color,
    colors: BudgetColors,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "onboardingOrbMotion")
    val iconFloat by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboardingOrbFloat"
    )
    val iconTilt by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboardingOrbTilt"
    )
    val orbPulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboardingOrbPulse"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = orbPulse
                    scaleY = orbPulse
                }
                .blur(28.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.24f),
                            glowColor.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize(0.80f)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.card.copy(alpha = 0.74f),
                            glowColor.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .border(1.dp, colors.border.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = glowColor.copy(alpha = 0.24f),
                modifier = Modifier
                    .size(96.dp)
                    .blur(9.dp)
            )

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier
                    .size(70.dp)
                    .graphicsLayer {
                        translationY = iconFloat
                        rotationZ = iconTilt
                        scaleX = 1f + (orbPulse - 1f) * 0.35f
                        scaleY = 1f + (orbPulse - 1f) * 0.35f
                    }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WormIndicator(
    pageCount: Int,
    pagerState: PagerState,
    colors: BudgetColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val movingPage by remember(pagerState) {
            derivedStateOf {
                pagerState.currentPage + pagerState.currentPageOffsetFraction
            }
        }

        repeat(pageCount) { index ->
            val distance = (movingPage - index).absoluteValue.coerceIn(0f, 1f)
            val selectedProgress = 1f - distance
            val width by animateDpAsState(
                targetValue = (8f + 22f * selectedProgress).dp,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "wormIndicatorWidth$index"
            )
            val alpha = 0.24f + 0.76f * selectedProgress
            val dotColor = if (selectedProgress > 0.04f) {
                colors.accent.copy(alpha = alpha)
            } else {
                colors.subText.copy(alpha = 0.26f)
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(width)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

private object OnboardingIcons {
    private var wallet: ImageVector? = null
    private var chart: ImageVector? = null
    private var goalStar: ImageVector? = null

    val Wallet: ImageVector
        get() {
            val current = wallet
            if (current != null) return current

            return ImageVector.Builder(
                name = "OnboardingWallet",
                defaultWidth = 64.dp,
                defaultHeight = 64.dp,
                viewportWidth = 64f,
                viewportHeight = 64f
            ).apply {
                path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                    moveTo(13f, 14f)
                    horizontalLineTo(45f)
                    curveTo(48.3f, 14f, 51f, 16.7f, 51f, 20f)
                    verticalLineTo(22f)
                    horizontalLineTo(17f)
                    curveTo(15.3f, 22f, 14f, 23.3f, 14f, 25f)
                    curveTo(14f, 26.7f, 15.3f, 28f, 17f, 28f)
                    horizontalLineTo(52f)
                    curveTo(55.9f, 28f, 59f, 31.1f, 59f, 35f)
                    verticalLineTo(48f)
                    curveTo(59f, 51.9f, 55.9f, 55f, 52f, 55f)
                    horizontalLineTo(14f)
                    curveTo(9f, 55f, 5f, 51f, 5f, 46f)
                    verticalLineTo(22f)
                    curveTo(5f, 17.6f, 8.6f, 14f, 13f, 14f)
                    close()
                }
                path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                    moveTo(47f, 35f)
                    horizontalLineTo(59f)
                    verticalLineTo(47f)
                    horizontalLineTo(47f)
                    curveTo(43.7f, 47f, 41f, 44.3f, 41f, 41f)
                    curveTo(41f, 37.7f, 43.7f, 35f, 47f, 35f)
                    close()
                }
                path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                    moveTo(48f, 38f)
                    curveTo(49.7f, 38f, 51f, 39.3f, 51f, 41f)
                    curveTo(51f, 42.7f, 49.7f, 44f, 48f, 44f)
                    curveTo(46.3f, 44f, 45f, 42.7f, 45f, 41f)
                    curveTo(45f, 39.3f, 46.3f, 38f, 48f, 38f)
                    close()
                }
            }.build().also { wallet = it }
        }

    val Chart: ImageVector
        get() {
            val current = chart
            if (current != null) return current

            return ImageVector.Builder(
                name = "OnboardingChart",
                defaultWidth = 64.dp,
                defaultHeight = 64.dp,
                viewportWidth = 64f,
                viewportHeight = 64f
            ).apply {
                path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                    moveTo(10f, 51f)
                    horizontalLineTo(55f)
                    curveTo(56.7f, 51f, 58f, 52.3f, 58f, 54f)
                    curveTo(58f, 55.7f, 56.7f, 57f, 55f, 57f)
                    horizontalLineTo(9f)
                    curveTo(6.8f, 57f, 5f, 55.2f, 5f, 53f)
                    verticalLineTo(9f)
                    curveTo(5f, 7.3f, 6.3f, 6f, 8f, 6f)
                    curveTo(9.7f, 6f, 11f, 7.3f, 11f, 9f)
                    verticalLineTo(50f)
                    curveTo(11f, 50.6f, 10.6f, 51f, 10f, 51f)
                    close()
                }
                path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                    moveTo(17f, 35f)
                    horizontalLineTo(25f)
                    verticalLineTo(49f)
                    horizontalLineTo(17f)
                    close()
                    moveTo(30f, 25f)
                    horizontalLineTo(38f)
                    verticalLineTo(49f)
                    horizontalLineTo(30f)
                    close()
                    moveTo(43f, 15f)
                    horizontalLineTo(51f)
                    verticalLineTo(49f)
                    horizontalLineTo(43f)
                    close()
                }
                path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                    moveTo(18f, 27f)
                    lineTo(28.5f, 16.5f)
                    curveTo(29.7f, 15.3f, 31.6f, 15.2f, 32.9f, 16.3f)
                    lineTo(38.8f, 21.3f)
                    lineTo(50.1f, 8.9f)
                    curveTo(51.2f, 7.7f, 53.1f, 7.6f, 54.3f, 8.7f)
                    curveTo(55.5f, 9.8f, 55.6f, 11.7f, 54.5f, 12.9f)
                    lineTo(41.3f, 27.5f)
                    curveTo(40.2f, 28.8f, 38.2f, 28.9f, 36.9f, 27.8f)
                    lineTo(30.9f, 22.8f)
                    lineTo(22.2f, 31.2f)
                    curveTo(21f, 32.4f, 19.1f, 32.4f, 17.9f, 31.2f)
                    curveTo(16.8f, 30f, 16.8f, 28.2f, 18f, 27f)
                    close()
                }
            }.build().also { chart = it }
        }

    val GoalStar: ImageVector
        get() {
            val current = goalStar
            if (current != null) return current

            return ImageVector.Builder(
                name = "OnboardingGoalStar",
                defaultWidth = 64.dp,
                defaultHeight = 64.dp,
                viewportWidth = 64f,
                viewportHeight = 64f
            ).apply {
                path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                    moveTo(32f, 4f)
                    curveTo(16.5f, 4f, 4f, 16.5f, 4f, 32f)
                    curveTo(4f, 47.5f, 16.5f, 60f, 32f, 60f)
                    curveTo(47.5f, 60f, 60f, 47.5f, 60f, 32f)
                    curveTo(60f, 16.5f, 47.5f, 4f, 32f, 4f)
                    close()
                    moveTo(32f, 10f)
                    curveTo(44.2f, 10f, 54f, 19.8f, 54f, 32f)
                    curveTo(54f, 44.2f, 44.2f, 54f, 32f, 54f)
                    curveTo(19.8f, 54f, 10f, 44.2f, 10f, 32f)
                    curveTo(10f, 19.8f, 19.8f, 10f, 32f, 10f)
                    close()
                    moveTo(32f, 17f)
                    curveTo(23.7f, 17f, 17f, 23.7f, 17f, 32f)
                    curveTo(17f, 40.3f, 23.7f, 47f, 32f, 47f)
                    curveTo(40.3f, 47f, 47f, 40.3f, 47f, 32f)
                    curveTo(47f, 23.7f, 40.3f, 17f, 32f, 17f)
                    close()
                    moveTo(32f, 23f)
                    curveTo(37f, 23f, 41f, 27f, 41f, 32f)
                    curveTo(41f, 37f, 37f, 41f, 32f, 41f)
                    curveTo(27f, 41f, 23f, 37f, 23f, 32f)
                    curveTo(23f, 27f, 27f, 23f, 32f, 23f)
                    close()
                }
                path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                    moveTo(32f, 18f)
                    lineTo(36.1f, 27.1f)
                    lineTo(46f, 28.2f)
                    lineTo(38.6f, 34.9f)
                    lineTo(40.7f, 44.7f)
                    lineTo(32f, 39.7f)
                    lineTo(23.3f, 44.7f)
                    lineTo(25.4f, 34.9f)
                    lineTo(18f, 28.2f)
                    lineTo(27.9f, 27.1f)
                    close()
                }
            }.build().also { goalStar = it }
        }
}
