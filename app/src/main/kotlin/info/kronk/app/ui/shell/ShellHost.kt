package info.kronk.app.ui.shell

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import info.kronk.app.ui.webshell.KronkWebView
import info.kronk.core.designsystem.primitive.BottomTabBar
import info.kronk.core.designsystem.primitive.BottomTabItem
import kotlinx.coroutines.launch

// Top-level app scaffold. Five WebViews (one per pillar) mounted in a
// HorizontalPager with `beyondBoundsPageCount = 4` so every tab is
// composed and kept warm — tab-swap is instant, no reload, no lost
// scroll position, no lost form state. The pager itself is
// non-swipeable (Tal 2026-08-13: no side-scroll on phone); tabs move
// only through the BottomTabBar.
//
// Each pillar loads its web path (Me → /@me, Home → /home,
// AWAWB → /awawb, Hub → /hub, Nudges → /nudges) inside the WebView.
// The web app is the single source of truth for content; the native
// shell owns only the bottom nav + auth token storage (kept for future
// push-notification registration).

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShellHost(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val pillars = PillarKey.values()
    val pagerState = rememberPagerState(initialPage = PillarKey.Home.ordinal) { pillars.size }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val items = pillars.map { pillar ->
                BottomTabItem(
                    icon = painterResource(pillar.iconRes),
                    contentDescription = stringResource(pillar.labelRes),
                    selected = pagerState.currentPage == pillar.ordinal,
                    onSelect = {
                        if (pagerState.currentPage != pillar.ordinal) {
                            scope.launch { pagerState.scrollToPage(pillar.ordinal) }
                        }
                    },
                )
            }
            BottomTabBar(items = items)
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            beyondBoundsPageCount = pillars.size - 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { page ->
            KronkWebView(path = pillars[page].webPath)
        }
    }
}
