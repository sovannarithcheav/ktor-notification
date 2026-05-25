package kh.com.ktor.notification.server.security

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

data class UserInfo(
    val userId: Long,
    val username: String = "",
    val roleType: String = "All",
    val ip: String? = null,
    val device: String? = null,
)

fun optionalUser(call: ApplicationCall): UserInfo? {
    val userId = call.request.headers["X-User-Id"]?.toLongOrNull() ?: return null
    return UserInfo(
        userId   = userId,
        username = call.request.headers["X-Username"] ?: "",
        roleType = call.request.headers["X-Role-Type"] ?: "All",
        ip       = call.request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
                   ?: call.request.local.remoteAddress,
        device   = call.request.headers["User-Agent"],
    )
}

suspend fun currentUser(call: ApplicationCall): UserInfo? {
    val userId = call.request.headers["X-User-Id"]?.toLongOrNull()
    if (userId == null) {
        call.respond(HttpStatusCode.Unauthorized, "Missing X-User-Id header")
        return null
    }
    return UserInfo(
        userId   = userId,
        username = call.request.headers["X-Username"] ?: "",
        roleType = call.request.headers["X-Role-Type"] ?: "All",
        ip       = call.request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
                   ?: call.request.local.remoteAddress,
        device   = call.request.headers["User-Agent"],
    )
}
