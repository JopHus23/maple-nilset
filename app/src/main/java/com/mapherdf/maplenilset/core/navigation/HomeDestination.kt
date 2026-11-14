package com.mapherdf.maplenilset.core.navigation

import kotlinx.serialization.Serializable

@Serializable
enum class HomeDestination {
    HOME,
    CALENDAR,
    NOTE,
    TODO,
    SCHEDULE,
    BILI,
    LIENZO,
}

enum class HomeDestinationMove {
    UP,
    DOWN,
}

enum class LienzoColorView {
    LIST,
    GRID,
}

@Serializable
data class UserUiSettings(
    val pageOrder: List<HomeDestination> = HomeDestination.entries,
    val hiddenPages: Set<HomeDestination> = emptySet(),
    val startupPage: HomeDestination = HomeDestination.HOME,
    val lienzoColorView: LienzoColorView = LienzoColorView.LIST,
) {
    val visiblePages: List<HomeDestination>
        get() = pageOrder.filterNot { it in hiddenPages }
}
