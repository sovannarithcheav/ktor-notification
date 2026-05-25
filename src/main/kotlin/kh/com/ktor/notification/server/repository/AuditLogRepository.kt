package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.AuditLog
import kh.com.ktor.notification.server.entity.AuditLogs
import kh.com.ktor.notification.server.entity.PageRequest
import kh.com.ktor.notification.server.entity.order
import kh.com.ktor.notification.server.entity.toAuditLog
import kh.com.ktor.notification.server.enums.AuditAction
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object AuditLogRepository {

    fun save(
        userId: Long? = null,
        activity: AuditAction,
        function: String,
        module: String,
        description: String? = null,
        status: String = "SUCCESS",
        device: String? = null,
        requestIp: String? = null,
        roleType: String? = null,
        username: String? = null,
        location: String? = null,
    ): AuditLog = transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val id = AuditLogs.insertAndGetId {
            it[AuditLogs.userId]           = userId
            it[AuditLogs.activity]         = activity.name
            it[AuditLogs.function]         = function
            it[AuditLogs.module]           = module
            it[AuditLogs.description]      = description
            it[AuditLogs.status]           = status
            it[AuditLogs.device]           = device
            it[AuditLogs.requestIp]        = requestIp
            it[AuditLogs.roleType]         = roleType
            it[AuditLogs.username]         = username
            it[AuditLogs.location]         = location
            it[AuditLogs.activityDatetime] = now
            it[AuditLogs.createdAt]        = now
        }
        AuditLogs.selectAll().where { AuditLogs.id eq id }.map { it.toAuditLog() }.first()
    }

    fun findAll(
        userId: Long? = null,
        activity: String? = null,
        module: String? = null,
        function: String? = null,
        status: String? = null,
        pageReq: PageRequest = PageRequest.of(sort = "activityDatetime,desc"),
    ): List<AuditLog> = transaction {
        AuditLogs.selectAll()
            .where { buildFilter(userId, activity, module, function, status) }
            .orderBy(AuditLogs.activityDatetime to pageReq.order())
            .limit(pageReq.size, offset = pageReq.offset)
            .map { it.toAuditLog() }
    }

    fun count(
        userId: Long? = null,
        activity: String? = null,
        module: String? = null,
        function: String? = null,
        status: String? = null,
    ): Long = transaction {
        AuditLogs.selectAll().where { buildFilter(userId, activity, module, function, status) }.count()
    }

    private fun buildFilter(
        userId: Long?, activity: String?, module: String?, function: String?, status: String?,
    ): Op<Boolean> {
        val conditions = buildList {
            userId?.let   { add(AuditLogs.userId   eq it) }
            activity?.let { add(AuditLogs.activity.lowerCase() like "%${it.lowercase()}%") }
            module?.let   { add(AuditLogs.module.lowerCase()   like "%${it.lowercase()}%") }
            function?.let { add(AuditLogs.function.lowerCase() like "%${it.lowercase()}%") }
            status?.let   { add(AuditLogs.status.lowerCase()   like "%${it.lowercase()}%") }
        }
        return if (conditions.isEmpty()) Op.TRUE else conditions.reduce { a, b -> a and b }
    }
}
