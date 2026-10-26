package com.mapherdf.maplenilset.data.repository

import com.mapherdf.maplenilset.data.calendar.CalendarItem
import com.mapherdf.maplenilset.data.calendar.UserCalendar

interface CalendarRepository {
    suspend fun getCalendar(): Result<UserCalendar>

    suspend fun saveCalendar(records: List<CalendarItem>): Result<Unit>

    suspend fun deleteCalendar(): Result<Unit>
}
