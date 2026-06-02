package kh.com.ktor.notification.server.service

import kh.com.ktor.notification.server.entity.AuditLog
import kh.com.ktor.notification.server.entity.PageRequest
import kh.com.ktor.notification.server.entity.PageResponse
import kh.com.ktor.notification.server.enums.AuditAction
import kh.com.ktor.notification.server.repository.AuditLogRepository
import kh.com.ktor.notification.server.security.UserInfo

object AuditLogService {

    fun log(
        activity: AuditAction,
        function: String,
        module: String,
        user: UserInfo? = null,
        userId: Long? = null,
        description: String? = null,
        status: String = "SUCCESS",
        referenceId: Long? = null,
        requestChangeId: Long? = null,
        username: String? = null,
        roleType: String? = null,
    ): Long = log(
        activity = activity.name.lowercase(),
        function = function, module = module, user = user, userId = userId,
        description = description, status = status,
        referenceId = referenceId, requestChangeId = requestChangeId,
        username = username, roleType = roleType,
    )

    /** Free-string activity (for activities outside the AuditAction enum, e.g. login/switch-role). */
    fun log(
        activity: String,
        function: String,
        module: String,
        user: UserInfo? = null,
        userId: Long? = null,
        description: String? = null,
        status: String = "SUCCESS",
        referenceId: Long? = null,
        requestChangeId: Long? = null,
        username: String? = null,
        roleType: String? = null,
    ): Long = AuditLogRepository.save(
        userId          = user?.userId ?: userId,
        activity        = activity,
        function        = function,
        module          = module,
        description     = description,
        status          = status,
        device          = user?.device,
        requestIp       = user?.ip,
        roleType        = roleType ?: user?.roleType,
        username        = username ?: user?.username,
        referenceId     = referenceId,
        requestChangeId = requestChangeId,
    ).id

    fun findPriorRequestId(module: String, entityId: Long): Long? =
        AuditLogRepository.findPriorId(module, "request-update", entityId)

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
    ): PageResponse<AuditLog> {
        val content = AuditLogRepository.findAll(userId, activity, module, function, status, referenceId, requestChangeId, excludeActivities, pageReq)
        val total   = AuditLogRepository.count(userId, activity, module, function, status, referenceId, requestChangeId, excludeActivities)
        return PageResponse.of(content, pageReq, total)
    }
}
