package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.AuditLog
import kh.com.ktor.notification.server.entity.AuditLogs
import kh.com.ktor.notification.server.entity.PageRequest
import kh.com.ktor.notification.server.entity.order
import kh.com.ktor.notification.server.entity.toAuditLog
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object AuditLogRepository {

    fun save(
        userId: Long? = null,
        activity: String,
        function: String,
        module: String,
        description: String? = null,
        status: String = "SUCCESS",
        device: String? = null,
        requestIp: String? = null,
        roleType: String? = null,
        username: String? = null,
        location: String? = null,
        referenceId: Long? = null,
        requestChangeId: Long? = null,
    ): AuditLog = transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val id = AuditLogs.insertAndGetId {
            it[AuditLogs.userId]           = userId
            it[AuditLogs.activity]         = activity
            it[AuditLogs.function]         = function
            it[AuditLogs.module]           = module
            it[AuditLogs.description]      = description
            it[AuditLogs.status]           = status
            it[AuditLogs.device]           = device
            it[AuditLogs.requestIp]        = requestIp
            it[AuditLogs.roleType]         = roleType ?: "SYSTEM"
            it[AuditLogs.username]         = username ?: "system"
            it[AuditLogs.location]         = location
            it[AuditLogs.referenceId]      = referenceId
            it[AuditLogs.requestChangeId]  = requestChangeId
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
        referenceId: Long? = null,
        requestChangeId: Long? = null,
        excludeActivities: List<String>? = null,
        pageReq: PageRequest = PageRequest.of(sort = "activityDatetime,desc"),
    ): List<AuditLog> = transaction {
        AuditLogs.selectAll()
            .where { buildFilter(userId, activity, module, function, status, referenceId, requestChangeId, excludeActivities) }
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
        referenceId: Long? = null,
        requestChangeId: Long? = null,
        excludeActivities: List<String>? = null,
    ): Long = transaction {
        AuditLogs.selectAll().where { buildFilter(userId, activity, module, function, status, referenceId, requestChangeId, excludeActivities) }.count()
    }

    fun findById(id: Long): AuditLog? = transaction {
        AuditLogs.selectAll().where { AuditLogs.id eq id }.map { it.toAuditLog() }.firstOrNull()
    }

    fun findPriorId(module: String, function: String, entityId: Long): Long? = transaction {
        AuditLogs.selectAll()
            .where {
                (AuditLogs.module   eq module)   and
                (AuditLogs.function eq function) and
                (AuditLogs.description like "id=$entityId %")
            }
            .orderBy(AuditLogs.id to SortOrder.DESC)
            .limit(1)
            .map { it[AuditLogs.id].value }
            .firstOrNull()
    }

    fun findDistinctFunctions(): List<String> = transaction {
        AuditLogs.select(AuditLogs.function)
            .where { AuditLogs.function.isNotNull() }
            .withDistinct()
            .orderBy(AuditLogs.function to SortOrder.ASC)
            .mapNotNull { it[AuditLogs.function] }
    }

    /** The chain root for a requestChangeId: the first audit record saved for it. */
    fun findRootByRequestChangeId(requestChangeId: Long): Long? = transaction {
        AuditLogs.selectAll()
            .where { AuditLogs.requestChangeId eq requestChangeId }
            .orderBy(AuditLogs.id to SortOrder.ASC)
            .limit(1)
            .map { it[AuditLogs.id].value }
            .firstOrNull()
    }

    private fun buildFilter(
        userId: Long?, activity: String?, module: String?, function: String?, status: String?,
        referenceId: Long? = null, requestChangeId: Long? = null, excludeActivities: List<String>? = null,
    ): Op<Boolean> {
        val conditions = buildList {
            userId?.let          { add(AuditLogs.userId          eq it) }
            referenceId?.let     { add(AuditLogs.referenceId     eq it) }
            requestChangeId?.let { add(AuditLogs.requestChangeId eq it) }
            excludeActivities?.takeIf { it.isNotEmpty() }?.let { ex ->
                add(AuditLogs.activity.lowerCase() notInList ex.map { it.lowercase() })
            }
            activity?.let { add(AuditLogs.activity.lowerCase() like "%${it.lowercase()}%") }
            module?.let   { add(AuditLogs.module.lowerCase()   like "%${it.lowercase()}%") }
            function?.let { add(AuditLogs.function.lowerCase() like "%${it.lowercase()}%") }
            status?.let   { add(AuditLogs.status.lowerCase()   like "%${it.lowercase()}%") }
        }
        return if (conditions.isEmpty()) Op.TRUE else conditions.reduce { a, b -> a and b }
    }
}
