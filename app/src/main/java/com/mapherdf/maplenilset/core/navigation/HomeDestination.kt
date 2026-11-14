package com.mapherdf.maplenilset.core.navigation

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

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
    val lienzoColorGridColumns: Int = DEFAULT_LIENZO_COLOR_GRID_COLUMNS,
) {
    val visiblePages: List<HomeDestination>
        get() = pageOrder.filterNot { it in hiddenPages }
}

const val MIN_LIENZO_COLOR_GRID_COLUMNS = 2
const val MAX_LIENZO_COLOR_GRID_COLUMNS = 6
const val DEFAULT_LIENZO_COLOR_GRID_COLUMNS = 2

fun targetLienzoColorGridColumns(
    currentColumns: Int,
    zoom: Float,
): Int {
    val current = currentColumns.coerceIn(
        MIN_LIENZO_COLOR_GRID_COLUMNS,
        MAX_LIENZO_COLOR_GRID_COLUMNS,
    )
    val target = if (zoom > 0f) (current / zoom + 0.5f).toInt() else current
    return target.coerceIn(
        MIN_LIENZO_COLOR_GRID_COLUMNS,
        MAX_LIENZO_COLOR_GRID_COLUMNS,
    )
}
