package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.ReminderFires
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object ReminderFireRepository {

    fun firedEmployeeIdsOn(ruleId: Long, firedOn: LocalDate): Set<Long> = transaction {
        ReminderFires.select(ReminderFires.employeeId).where {
            (ReminderFires.ruleId eq ruleId) and (ReminderFires.firedOn eq firedOn)
        }.map { it[ReminderFires.employeeId] }.toSet()
    }

    fun record(ruleId: Long, employeeId: Long, firedOn: LocalDate, offsetUsed: Int, sentCount: Int) = transaction {
        ReminderFires.insert {
            it[ReminderFires.ruleId]     = ruleId
            it[ReminderFires.employeeId] = employeeId
            it[ReminderFires.firedOn]    = firedOn
            it[ReminderFires.offsetUsed] = offsetUsed
            it[ReminderFires.sentCount]  = sentCount
        }
    }
}
