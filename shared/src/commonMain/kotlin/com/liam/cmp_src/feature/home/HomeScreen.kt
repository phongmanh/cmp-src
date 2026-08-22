package com.liam.cmp_src.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.api.user.UserResponse
import com.liam.cmp_src.core.ui.component.StaggeredEntrance
import com.liam.cmp_src.core.ui.modifier.handCursor
import com.liam.cmp_src.core.ui.theme.AppTheme
import com.liam.cmp_src.core.ui.theme.Dimens
import com.liam.cmp_src.core.ui.theme.auroraColors
import com.liam.cmp_src.feature.auth.presentation.asLabel
import com.liam.cmp_src.feature.auth.presentation.component.AnimatedAuthBackground
import com.liam.cmp_src.feature.home.component.HomeBottomBar
import com.liam.cmp_src.feature.home.component.HomeTopBar
import com.liam.cmp_src.feature.home.component.UserAvatar
import com.liam.cmp_src.feature.home.component.displayLabel
import com.liam.cmp_src.feature.home.component.sampleUser
import com.liam.cmp_src.feature.home.component.signedInProvider
import com.liam.cmp_src.getPlatform
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.home_greeting
import cmpsrc.shared.generated.resources.home_notifications_unavailable
import cmpsrc.shared.generated.resources.home_placeholder_body
import cmpsrc.shared.generated.resources.home_running_on
import cmpsrc.shared.generated.resources.home_sign_out
import cmpsrc.shared.generated.resources.home_signed_in_as
import cmpsrc.shared.generated.resources.home_signed_in_with
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val BAR_ENTRANCE_MILLIS = 520
private const val TAB_SWITCH_MILLIS = 260
private const val TAB_SLIDE_DIVISOR = 6

/**
 * Home screen wired to its [HomeViewModel].
 *
 * Same split as `LoginRoute`: the route owns the ViewModel and reports what happened, while the
 * stateless [HomeScreen] below takes a user and a callback and can be previewed without Koin.
 * [onSignedOut] fires once the session has actually been ended, so the app never returns to the
 * login screen while the old tokens are still live.
 */
@Composable
fun HomeRoute(
    user: UserResponse,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.SignedOut -> onSignedOut()
            }
        }
    }

    HomeScreen(
        user = user,
        onSignOut = viewModel::onSignOut,
        modifier = modifier,
    )
}

/**
 * The signed-in app shell: a glass header, a floating navigation pill, and whichever tab's
 * content sits between them.
 *
 * The selected tab is screen state rather than a back-stack entry — see [HomeTab] — so it is
 * held here with `rememberSaveable` and survives rotation and process death.
 */
@Composable
fun HomeScreen(
    user: UserResponse,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    var selectedTab by rememberSaveable(stateSaver = HomeTab.Saver) {
        mutableStateOf(HomeTab.HOME)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notificationsMessage = stringResource(Res.string.home_notifications_unavailable)

    // Drives both bars in from their own edge. A translation rather than an AnimatedVisibility,
    // because the bars are Scaffold slots: changing their measured height mid-animation would
    // shift the content inset underneath them.
    val barOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 1f,
        animationSpec = tween(BAR_ENTRANCE_MILLIS, easing = FastOutSlowInEasing),
        label = "barEntrance",
    )

    Box(modifier.fillMaxSize()) {
        AnimatedAuthBackground(Modifier.matchParentSize())

        Scaffold(
            // Transparent so the aurora shows through; the bars bring their own glass.
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            // Each bar consumes the insets it needs itself, so the Scaffold must not also
            // pad the content for them.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                HomeTopBar(
                    user = user,
                    onNotificationsClick = {
                        scope.launch { snackbarHostState.showSnackbar(notificationsMessage) }
                    },
                    onSignOutClick = onSignOut,
                    modifier = Modifier.graphicsLayer {
                        translationY = -size.height * barOffset
                        alpha = 1f - barOffset
                    },
                )
            },
            bottomBar = {
                HomeBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.graphicsLayer {
                        translationY = size.height * barOffset
                        alpha = 1f - barOffset
                    },
                )
            },
        ) { contentPadding ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { tabTransition() },
                label = "homeTabContent",
            ) { tab ->
                HomeTabContent(
                    tab = tab,
                    user = user,
                    isVisible = isVisible,
                    onSignOut = onSignOut,
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

/** Tabs slide in from the side they sit on in the bar, so the motion matches the bar's order. */
private fun AnimatedContentTransitionScope<HomeTab>.tabTransition(): ContentTransform {
    val forward = targetState.ordinal > initialState.ordinal
    val direction = if (forward) 1 else -1
    val enter = slideInHorizontally(tween(TAB_SWITCH_MILLIS, easing = FastOutSlowInEasing)) {
        direction * it / TAB_SLIDE_DIVISOR
    } + fadeIn(tween(TAB_SWITCH_MILLIS))
    val exit = slideOutHorizontally(tween(TAB_SWITCH_MILLIS, easing = FastOutSlowInEasing)) {
        -direction * it / TAB_SLIDE_DIVISOR
    } + fadeOut(tween(TAB_SWITCH_MILLIS))
    return enter togetherWith exit
}

/** Routes a tab to its content, all of it laid out in the same centred, width-capped column. */
@Composable
private fun HomeTabContent(
    tab: HomeTab,
    user: UserResponse,
    isVisible: Boolean,
    onSignOut: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = Dimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = Dimens.cardMaxWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (tab) {
                HomeTab.HOME -> HomeOverview(user = user, isVisible = isVisible)
                HomeTab.PROFILE -> ProfilePanel(
                    user = user,
                    isVisible = isVisible,
                    onSignOut = onSignOut,
                )

                HomeTab.SEARCH, HomeTab.ACTIVITY -> TabPlaceholder(
                    tab = tab,
                    isVisible = isVisible,
                )
            }
        }
    }
}

/** The landing tab: who signed in, and what this build is running on. */
@Composable
private fun HomeOverview(
    user: UserResponse,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val platformName = remember { getPlatform().name }

    StaggeredEntrance(visible = isVisible, index = 1, modifier = modifier) {
        GlassCard {
            Text(
                text = stringResource(Res.string.home_greeting, user.displayLabel()),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(Dimens.spaceSm))
            Text(
                text = stringResource(Res.string.home_running_on, platformName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The account tab: the signed-in identity, and the way back out of it. */
@Composable
private fun ProfilePanel(
    user: UserResponse,
    isVisible: Boolean,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = auroraColors

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StaggeredEntrance(visible = isVisible, index = 1) {
            GlassCard {
                UserAvatar(
                    user = user,
                    size = Dimens.avatarLg,
                    textStyle = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.size(Dimens.spaceLg))
                Text(
                    text = user.displayLabel(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                user.email?.let { email ->
                    Text(
                        text = stringResource(Res.string.home_signed_in_as, email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                user.signedInProvider()?.let { provider ->
                    Text(
                        text = stringResource(Res.string.home_signed_in_with, provider.asLabel()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.size(Dimens.spaceLg))

        StaggeredEntrance(visible = isVisible, index = 2) {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight)
                    .handCursor(),
                shape = RoundedCornerShape(Dimens.radiusMd),
                // The default outline token is far too dark to read against the
                // aurora backdrop; match the glass treatment used on the login card.
                border = BorderStroke(Dimens.hairline, glass.glassBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = glass.glassFill,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
            ) {
                Text(stringResource(Res.string.home_sign_out))
            }
        }
    }
}

/** Stands in for a tab whose feature does not exist yet, without pretending it has data. */
@Composable
private fun TabPlaceholder(
    tab: HomeTab,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val glass = auroraColors

    StaggeredEntrance(visible = isVisible, index = 1, modifier = modifier) {
        GlassCard {
            Surface(
                modifier = Modifier.size(Dimens.avatarLg),
                shape = CircleShape,
                color = glass.glassFill,
                border = BorderStroke(Dimens.hairline, glass.glassBorder),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(tab.icon),
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconLg),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.size(Dimens.spaceLg))
            Text(
                text = stringResource(tab.label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.home_placeholder_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The one card treatment every tab's content sits in. */
@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = auroraColors

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusXl),
        color = glass.glassFill,
        border = BorderStroke(Dimens.hairline, glass.glassBorder),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.spaceXl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            content = content,
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    AppTheme {
        HomeScreen(
            user = sampleUser(),
            onSignOut = {},
        )
    }
}
