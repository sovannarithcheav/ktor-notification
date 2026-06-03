package kh.com.ktor.notification.server.security

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kh.com.ktor.security.userContext

data class UserInfo(
    val userId: Long,
    val username: String = "",
    val roleType: String = "All",
    val ip: String? = null,
    val device: String? = null,
)

// ip / device are request metadata (not identity) — kept from the request, not the token.
private fun ApplicationCall.clientIp(): String? =
    request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
        ?: request.local.remoteAddress

private fun ApplicationCall.device(): String? = request.headers["User-Agent"]

/** Identity from the validated JWT ([userContext]) — null if absent. No X-* headers. */
fun optionalUser(call: ApplicationCall): UserInfo? {
    val ctx = call.userContext() ?: return null
    return UserInfo(
        userId   = ctx.userId,
        username = ctx.username ?: "",
        roleType = ctx.roleType ?: "All",
        ip       = call.clientIp(),
        device   = call.device(),
    )
}

/** Identity from the validated JWT, or respond 401. */
suspend fun currentUser(call: ApplicationCall): UserInfo? {
    val ctx = call.userContext()
    if (ctx == null) {
        call.respond(HttpStatusCode.Unauthorized, "Missing or invalid bearer token")
        return null
    }
    return UserInfo(
        userId   = ctx.userId,
        username = ctx.username ?: "",
        roleType = ctx.roleType ?: "All",
        ip       = call.clientIp(),
        device   = call.device(),
    )
}
