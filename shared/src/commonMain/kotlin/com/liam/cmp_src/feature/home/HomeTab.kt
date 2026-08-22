package com.liam.cmp_src.feature.home

import androidx.compose.runtime.saveable.Saver
import cmpsrc.shared.generated.resources.Res
import cmpsrc.shared.generated.resources.home_tab_activity
import cmpsrc.shared.generated.resources.home_tab_home
import cmpsrc.shared.generated.resources.home_tab_profile
import cmpsrc.shared.generated.resources.home_tab_search
import cmpsrc.shared.generated.resources.ic_bolt
import cmpsrc.shared.generated.resources.ic_home
import cmpsrc.shared.generated.resources.ic_person
import cmpsrc.shared.generated.resources.ic_search
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/**
 * The sections of the signed-in app, in the order they appear in the bottom navigation bar.
 *
 * Each constant carries its own label and icon so the bar can be built by iterating [entries] —
 * adding a section is one constant here plus a branch in `HomeTabContent`, with no bar changes.
 *
 * These are not [com.liam.cmp_src.core.navigation.AppRoute] destinations on purpose: switching
 * tabs is a change of content *within* the home screen, not a push onto the app's back stack,
 * which the back stack models as handovers that clear what came before.
 */
enum class HomeTab(
    val label: StringResource,
    val icon: DrawableResource,
) {
    HOME(label = Res.string.home_tab_home, icon = Res.drawable.ic_home),
    SEARCH(label = Res.string.home_tab_search, icon = Res.drawable.ic_search),
    ACTIVITY(label = Res.string.home_tab_activity, icon = Res.drawable.ic_bolt),
    PROFILE(label = Res.string.home_tab_profile, icon = Res.drawable.ic_person),
    ;

    companion object {
        /**
         * Saves the selection by name rather than ordinal, so reordering the constants can't
         * silently restore a different tab. An unknown name restores as `null`, which
         * `rememberSaveable` treats as "no saved value" and falls back to the initial tab.
         */
        val Saver: Saver<HomeTab, String> = Saver(
            save = { it.name },
            restore = { name -> entries.firstOrNull { it.name == name } },
        )
    }
}
