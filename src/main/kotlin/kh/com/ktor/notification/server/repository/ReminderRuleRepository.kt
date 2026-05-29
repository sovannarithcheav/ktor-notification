package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.IntList
import kh.com.ktor.notification.server.entity.LongList
import kh.com.ktor.notification.server.entity.ReminderRule
import kh.com.ktor.notification.server.entity.ReminderRuleReq
import kh.com.ktor.notification.server.entity.ReminderRules
import kh.com.ktor.notification.server.entity.toReminderRule
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object ReminderRuleRepository {

    fun findAll(isActive: Boolean? = null, dateKind: String? = null): List<ReminderRule> = transaction {
        ReminderRules.selectAll().where {
            (isActive?.let { ReminderRules.isActive eq it } ?: org.jetbrains.exposed.sql.Op.TRUE) and
            (dateKind?.let { ReminderRules.dateKind eq it } ?: org.jetbrains.exposed.sql.Op.TRUE)
        }.orderBy(ReminderRules.id).map { it.toReminderRule() }
    }

    fun findActive(): List<ReminderRule> = findAll(isActive = true)

    fun findById(id: Long): ReminderRule? = transaction {
        ReminderRules.selectAll().where { ReminderRules.id eq id }.map { it.toReminderRule() }.firstOrNull()
    }

    fun create(req: ReminderRuleReq, createdBy: Long?): ReminderRule = transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val id  = ReminderRules.insertAndGetId {
            it[name]       = req.name
            it[dateKind]   = req.dateKind
            it[offsetDays] = IntList(req.offsetDays)
            it[eventId]    = req.eventId
            it[channelIds] = LongList(req.channelIds)
            it[isActive]   = req.isActive
            it[ReminderRules.createdAt] = now
            it[ReminderRules.updatedAt] = now
            it[ReminderRules.createdBy] = createdBy
            it[ReminderRules.updatedBy] = createdBy
        }
        findById(id.value)!!
    }

    fun update(id: Long, req: ReminderRuleReq, updatedBy: Long?): ReminderRule? = transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val rows = ReminderRules.update({ ReminderRules.id eq id }) {
            it[name]       = req.name
            it[offsetDays] = IntList(req.offsetDays)
            it[channelIds] = LongList(req.channelIds)
            it[isActive]   = req.isActive
            it[ReminderRules.updatedAt] = now
            it[ReminderRules.updatedBy] = updatedBy
        }
        if (rows > 0) findById(id) else null
    }
}
