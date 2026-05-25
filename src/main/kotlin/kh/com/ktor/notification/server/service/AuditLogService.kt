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
    ) {
        AuditLogRepository.save(
            userId      = user?.userId ?: userId,
            activity    = activity,
            function    = function,
            module      = module,
            description = description,
            status      = status,
            device      = user?.device,
            requestIp   = user?.ip,
            roleType    = user?.roleType,
            username    = user?.username,
        )
    }

    fun findAll(
        userId: Long? = null,
        activity: String? = null,
        module: String? = null,
        function: String? = null,
        status: String? = null,
        pageReq: PageRequest = PageRequest.of(sort = "activityDatetime,desc"),
    ): PageResponse<AuditLog> {
        val content = AuditLogRepository.findAll(userId, activity, module, function, status, pageReq)
        val total   = AuditLogRepository.count(userId, activity, module, function, status)
        return PageResponse.of(content, pageReq, total)
    }
}
