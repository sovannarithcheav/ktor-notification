package kh.com.ktor.notification.server.entity

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object AuditLogs : LongIdTable("audit_log") {
    val activity         = varchar("activity", 255).nullable()
    val userId           = long("user_id").nullable()
    val description      = text("description").nullable()
    val function         = varchar("function", 255).nullable()
    val module           = varchar("module", 255).nullable()
    val device           = varchar("device", 500).nullable()
    val requestIp        = varchar("request_ip", 100).nullable()
    val location         = varchar("location", 255).nullable()
    val status           = varchar("status", 255).nullable()
    val activityDatetime = datetime("activity_datetime").nullable()
    val createdAt        = datetime("created_at").nullable()
    val roleType         = varchar("role_type", 100).nullable()
    val username         = varchar("username", 255).nullable()
}

@Serializable
data class AuditLog(
    val id: Long = 0,
    val activity: String? = null,
    val userId: Long? = null,
    val description: String? = null,
    val function: String? = null,
    val module: String? = null,
    val device: String? = null,
    val requestIp: String? = null,
    val location: String? = null,
    val status: String? = null,
    val activityDatetime: LocalDateTime? = null,
    val createdAt: LocalDateTime? = null,
    val roleType: String? = null,
    val username: String? = null,
)

fun ResultRow.toAuditLog() = AuditLog(
    id               = this[AuditLogs.id].value,
    activity         = this[AuditLogs.activity],
    userId           = this[AuditLogs.userId],
    description      = this[AuditLogs.description],
    function         = this[AuditLogs.function],
    module           = this[AuditLogs.module],
    device           = this[AuditLogs.device],
    requestIp        = this[AuditLogs.requestIp],
    location         = this[AuditLogs.location],
    status           = this[AuditLogs.status],
    activityDatetime = this[AuditLogs.activityDatetime],
    createdAt        = this[AuditLogs.createdAt],
    roleType         = this[AuditLogs.roleType],
    username         = this[AuditLogs.username],
)
