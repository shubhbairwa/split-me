package com.shubh.splitme.ui.onboarding

import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shubh.splitme.R
import com.shubh.splitme.ui.theme.AccentOrange
import com.shubh.splitme.ui.theme.AccentOrangeDeep
import com.shubh.splitme.ui.theme.PureWhite
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.TextSecondary
import com.shubh.splitme.ui.theme.glassShadow
import kotlinx.coroutines.launch

private data class OnboardingPage(@DrawableRes val image: Int, @StringRes val title: Int, @StringRes val body: Int)

private val Pages = listOf(
    OnboardingPage(
        image = R.drawable.onboarding_track_expenses,
        title = R.string.onboarding_track_title,
        body = R.string.onboarding_track_body
    ),
    OnboardingPage(
        image = R.drawable.onboarding_split_settle,
        title = R.string.onboarding_split_title,
        body = R.string.onboarding_split_body
    )
)

private val ImageShape = RoundedCornerShape(32.dp)

/** Two intro slides shown once, before sign-in. [onFinish] runs on "Get Started" or "Skip". */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { Pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == Pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            if (!isLast) {
                TextButton(onClick = onFinish, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Text(stringResource(R.string.onboarding_skip), color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { index ->
            val page = Pages[index]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // The illustrations have white backgrounds, so they sit on a white rounded card
                // that looks the same in light and dark mode.
                Image(
                    painter = painterResource(page.image),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .glassShadow(ImageShape)
                        .clip(ImageShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.height(36.dp))
                Text(
                    stringResource(page.title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(page.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        PageDots(count = Pages.size, current = pagerState.currentPage, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Ignore taps while a slide is still moving: otherwise a quick second tap on
                // "Next" lands as "Get Started" and skips the last slide.
                if (!pagerState.isScrollInProgress) {
                    if (pagerState.currentPage == Pages.lastIndex) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(AccentOrange, AccentOrangeDeep))),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = PureWhite)
        ) {
            Text(if (isLast) stringResource(R.string.onboarding_get_started) else stringResource(R.string.onboarding_next), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 28.dp else 8.dp, label = "dotWidth")
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(if (i == current) AccentOrange else MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}
