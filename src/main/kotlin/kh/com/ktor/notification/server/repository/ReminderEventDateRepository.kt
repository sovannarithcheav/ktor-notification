package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.ReminderEventDate
import kh.com.ktor.notification.server.entity.ReminderEventDates
import kh.com.ktor.notification.server.entity.toReminderEventDate
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object ReminderEventDateRepository {

    fun findMatchesForOffsets(dateKind: String, dates: List<LocalDate>): List<ReminderEventDate> = transaction {
        if (dates.isEmpty()) return@transaction emptyList()
        ReminderEventDates.selectAll().where {
            (ReminderEventDates.dateKind eq dateKind) and (ReminderEventDates.targetDate inList dates)
        }.map { it.toReminderEventDate() }
    }

    fun findByEmployeeKind(employeeId: Long, dateKind: String): ReminderEventDate? = transaction {
        ReminderEventDates.selectAll().where {
            (ReminderEventDates.employeeId eq employeeId) and (ReminderEventDates.dateKind eq dateKind)
        }.map { it.toReminderEventDate() }.firstOrNull()
    }

    fun upsert(
        employeeId: Long,
        employeeName: String,
        dateKind: String,
        targetDate: LocalDate,
        metadata: JsonObject?,
    ): ReminderEventDate = transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val existing = findByEmployeeKind(employeeId, dateKind)
        if (existing == null) {
            ReminderEventDates.insertAndGetId {
                it[ReminderEventDates.employeeId]   = employeeId
                it[ReminderEventDates.employeeName] = employeeName
                it[ReminderEventDates.dateKind]     = dateKind
                it[ReminderEventDates.targetDate]   = targetDate
                it[ReminderEventDates.metadata]     = metadata
                it[ReminderEventDates.createdAt]    = now
                it[ReminderEventDates.updatedAt]    = now
            }
        } else {
            ReminderEventDates.update({
                (ReminderEventDates.employeeId eq employeeId) and (ReminderEventDates.dateKind eq dateKind)
            }) {
                it[ReminderEventDates.employeeName] = employeeName
                it[ReminderEventDates.targetDate]   = targetDate
                it[ReminderEventDates.metadata]     = metadata
                it[ReminderEventDates.updatedAt]    = now
            }
        }
        findByEmployeeKind(employeeId, dateKind)!!
    }

    fun delete(employeeId: Long, dateKind: String): Int = transaction {
        ReminderEventDates.deleteWhere {
            (ReminderEventDates.employeeId eq employeeId) and (ReminderEventDates.dateKind eq dateKind)
        }
    }
}
