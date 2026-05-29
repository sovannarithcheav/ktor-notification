package kh.com.ktor.notification.server.entity

import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date

object ReminderFires : Table("reminder_fires") {
    val ruleId      = long("rule_id")
    val employeeId  = long("employee_id")
    val firedOn     = date("fired_on")
    val offsetUsed  = integer("offset_used")
    val sentCount   = integer("sent_count")
    override val primaryKey = PrimaryKey(ruleId, employeeId, firedOn)
}

data class ReminderFire(
    val ruleId: Long,
    val employeeId: Long,
    val firedOn: LocalDate,
    val offsetUsed: Int,
    val sentCount: Int,
)

fun ResultRow.toReminderFire() = ReminderFire(
    ruleId     = this[ReminderFires.ruleId],
    employeeId = this[ReminderFires.employeeId],
    firedOn    = this[ReminderFires.firedOn],
    offsetUsed = this[ReminderFires.offsetUsed],
    sentCount  = this[ReminderFires.sentCount],
)
