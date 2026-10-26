package com.mapherdf.maplenilset.data.calendar

import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CalendarItem(
    val weekday: Int,
    val startMin: Int,
    val endMin: Int,
    val title: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val teacher: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val classroom: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val note: String? = null,
)

data class UserCalendar(
    val calendarId: Long,
    val records: List<CalendarItem>,
    val isInitialized: Boolean = true,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class LocalCalendar(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val records: List<CalendarItem> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val isInitialized: Boolean = true,
)
