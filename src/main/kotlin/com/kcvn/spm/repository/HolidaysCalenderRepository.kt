package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.references.HOLIDAYS_CALENDAR
import org.jooq.DSLContext
import java.time.OffsetDateTime

class HolidaysCalenderRepository (private val context: DSLContext){
    fun getHolidaysCalender(): List<OffsetDateTime> {
        return context.select(HOLIDAYS_CALENDAR.DAY_OFF)
            .from(HOLIDAYS_CALENDAR)
            .where(HOLIDAYS_CALENDAR.IS_DELETED.eq(false))
            .fetch { record -> record[HOLIDAYS_CALENDAR.DAY_OFF] }
    }
}