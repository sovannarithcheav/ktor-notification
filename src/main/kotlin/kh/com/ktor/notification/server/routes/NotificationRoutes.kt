package kh.com.ktor.notification.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kh.com.ktor.notification.server.NotificationServerDefaults
import kh.com.ktor.notification.server.common.badRequest
import kh.com.ktor.notification.server.common.created
import kh.com.ktor.notification.server.common.noContent
import kh.com.ktor.notification.server.common.notFound
import kh.com.ktor.notification.server.common.ok
import kh.com.ktor.notification.server.common.respondData
import kh.com.ktor.notification.server.common.serverError
import kh.com.ktor.notification.server.common.unprocessable
import kh.com.ktor.notification.server.dispatch.DispatchRequest
import kh.com.ktor.notification.server.entity.ChannelReq
import kh.com.ktor.notification.server.entity.EventNotificationRequest
import kh.com.ktor.notification.server.entity.NotificationSubscriptionFilterReq
import kh.com.ktor.notification.server.entity.NotificationTemplateUpdateRequest
import kh.com.ktor.notification.server.entity.PageRequest
import kh.com.ktor.notification.server.entity.VariableRequest
import kh.com.ktor.notification.server.security.NotificationException
import kh.com.ktor.notification.server.security.currentUser
import kh.com.ktor.notification.server.security.optionalUser
import kh.com.ktor.notification.server.enums.AuditAction
import kh.com.ktor.notification.server.service.AuditLogService
import kh.com.ktor.notification.server.service.CategoryService
import kh.com.ktor.notification.server.service.EventNotificationService
import kh.com.ktor.notification.server.service.NotificationHistoryService
import kh.com.ktor.notification.server.service.NotificationSendService
import kh.com.ktor.notification.server.service.NotificationSubscriptionService
import kh.com.ktor.notification.server.service.NotificationTemplateService
import kh.com.ktor.notification.server.service.SendNotificationRequest
import kh.com.ktor.notification.server.service.VariableService

fun Routing.installNotificationRoutes(
    basePath: String,
    templateService: NotificationTemplateService,
    templateUpdateHandler: (suspend ApplicationCall.(id: Long) -> Unit)?,
    eventNotificationUpdateHandler: (suspend ApplicationCall.(id: Long) -> Unit)? = null,
) {
    val eventService        = EventNotificationService()
    val variableService     = VariableService()
    val categoryService     = CategoryService()
    val sendService         = NotificationSendService(templateService)
    val subscriptionService = NotificationSubscriptionService()
    val historyService      = NotificationHistoryService()

    route(basePath) {

        // ── Event Notifications ─────────────────────────────────────────────
        route("/event-notifications") {
            get {
                val p       = call.request.queryParameters
                val pageReq = PageRequest.of(
                    page = p["page"]?.toIntOrNull() ?: 0,
                    size = p["size"]?.toIntOrNull() ?: 20,
                    sort = p["sort"] ?: "id,asc",
                )
                call.ok(eventService.getAll(
                    categoryId = p["categoryId"]?.toLongOrNull(),
                    statusId   = p["statusId"]?.toLongOrNull(),
                    search     = p["search"]?.takeIf { it.isNotBlank() },
                    pageReq    = pageReq,
                ))
            }
            get("/options") { call.ok(eventService.getOptions()) }
            get("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.badRequest("Invalid id")
                call.ok(eventService.getById(id) ?: return@get call.notFound())
            }
            post {
                val user    = optionalUser(call)
                val request = call.receive<EventNotificationRequest>()
                val result  = eventService.create(request)
                AuditLogService.log(
                    activity    = AuditAction.CREATE,
                    function    = "create",
                    module      = "EventNotification",
                    user        = user,
                    description = "code=${request.code} name=${request.name}",
                )
                call.created(result)
            }
            put("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@put call.badRequest("Invalid id")
                if (eventNotificationUpdateHandler != null) {
                    eventNotificationUpdateHandler.invoke(call, id)
                } else {
                    val request = call.receive<EventNotificationRequest>()
                    val result  = eventService.update(id, request) ?: return@put call.notFound()
                    AuditLogService.log(
                        activity    = AuditAction.UPDATE,
                        function    = "update",
                        module      = "EventNotification",
                        user        = optionalUser(call),
                        description = "id=$id code=${request.code}",
                    )
                    call.ok(result)
                }
            }
        }

        // ── Templates ───────────────────────────────────────────────────────
        route("/templates") {
            get {
                val p         = call.request.queryParameters
                val pageReq   = PageRequest.of(
                    page = p["page"]?.toIntOrNull() ?: 0,
                    size = p["size"]?.toIntOrNull() ?: 20,
                    sort = p["sort"] ?: "id,asc",
                )
                call.ok(templateService.getAll(p["eventId"]?.toLongOrNull(), p["channelId"]?.toLongOrNull(), p["statusId"]?.toLongOrNull(), pageReq))
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.badRequest("Invalid id")
                call.ok(templateService.getById(id) ?: return@get call.notFound())
            }
            put("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@put call.badRequest("Invalid id")
                if (templateUpdateHandler != null) {
                    templateUpdateHandler.invoke(call, id)
                } else {
                    val request   = call.receive<NotificationTemplateUpdateRequest>()
                    val updatedBy = optionalUser(call)?.userId
                    try {
                        val (_, new) = templateService.applyUpdate(id, request, updatedBy) ?: return@put call.notFound()
                        call.ok(new)
                    } catch (e: NotificationException) {
                        call.unprocessable(e.message ?: e.code)
                    }
                }
            }
        }

        // ── Variables ───────────────────────────────────────────────────────
        route("/variables") {
            get           { call.ok(variableService.getAll()) }
            get("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.badRequest("Invalid id")
                call.ok(variableService.getById(id) ?: return@get call.notFound())
            }
            post {
                call.created(variableService.create(call.receive<VariableRequest>()))
            }
            put("/{id}") {
                val id = call.parameters["id"]?.toLongOrNull() ?: return@put call.badRequest("Invalid id")
                variableService.update(id, call.receive<VariableRequest>()).fold(
                    onSuccess = { v -> if (v == null) call.notFound() else call.ok(v) },
                    onFailure = { call.unprocessable(it.message ?: "Update rejected") },
                )
            }
        }

        route("/events/{eventCode}/variables") {
            get {
                val eventCode = call.parameters["eventCode"] ?: return@get call.badRequest("Missing eventCode")
                val vars = variableService.getByEventCode(eventCode)
                if (vars.isEmpty()) return@get call.notFound("No variables found for event '$eventCode'")
                call.ok(vars)
            }
        }

        // ── Send / Test ─────────────────────────────────────────────────────
        post("/send") {
            val caller    = optionalUser(call)
            val request   = call.receive<SendNotificationRequest>()
            val responses = sendService.send(request, caller)
            if (responses.all { it.success }) call.ok(responses)
            else call.respondData(HttpStatusCode.UnprocessableEntity, responses)
        }

        post("/test-mail") {
            val userId = call.request.queryParameters["userId"]?.toLongOrNull()
                ?: return@post call.badRequest("Missing or invalid query param: userId")
            val result = NotificationServerDefaults.dispatcher?.dispatch(
                DispatchRequest(
                    userId      = userId,
                    title       = "Test Mail",
                    subject     = null,
                    body        = "This is a test email from the notification service.",
                    mergeFields = emptyMap(),
                    channelId   = 1L,
                )
            ) ?: return@post call.serverError("Notification dispatcher not initialized")
            if (result.success) call.ok(mapOf("message" to result.message))
            else call.serverError(result.message)
        }

        // ── Subscriptions ───────────────────────────────────────────────────
        route("/subscriptions") {
            get {
                val user = currentUser(call) ?: return@get
                val p    = call.request.queryParameters
                val filter = NotificationSubscriptionFilterReq(
                    q          = p["q"]?.takeIf { it.isNotBlank() },
                    categoryId = p["categoryId"]?.toLongOrNull(),
                )
                val pageReq = PageRequest.of(
                    page = p["page"]?.toIntOrNull() ?: 0,
                    size = p["size"]?.toIntOrNull() ?: 20,
                    sort = p["sort"] ?: "name,asc",
                )
                call.ok(subscriptionService.findAll(user.userId, filter, pageReq))
            }
            post("/subscribe") {
                val user = currentUser(call) ?: return@post
                try {
                    call.ok(subscriptionService.subscribe(user, call.receive<ChannelReq>()))
                } catch (e: NotificationException) {
                    call.unprocessable(e.message ?: e.code)
                }
            }
        }

        // ── Categories ──────────────────────────────────────────────────────
        route("/categories") {
            get {
                val user = currentUser(call) ?: return@get
                call.ok(categoryService.findAll(user.roleType))
            }
        }

        // ── Notification History ─────────────────────────────────────────────
        route("/history") {
            get {
                val user = currentUser(call) ?: return@get
                val p    = call.request.queryParameters
                val pageReq = PageRequest.of(
                    page = p["page"]?.toIntOrNull() ?: 0,
                    size = p["size"]?.toIntOrNull() ?: 20,
                    sort = p["sort"] ?: "eventDate,desc",
                )
                call.ok(historyService.findAll(user.userId, p["read"]?.toBooleanStrictOrNull(), pageReq))
            }
            get("/unread-count") {
                val user = currentUser(call) ?: return@get
                call.ok(mapOf("count" to historyService.countUnread(user.userId)))
            }
            post("/{id}/read") {
                val user = currentUser(call) ?: return@post
                val id   = call.parameters["id"]?.toLongOrNull() ?: return@post call.badRequest("Invalid id")
                if (historyService.markAsRead(id, user)) call.noContent()
                else call.notFound("Notification not found")
            }
            post("/read-all") {
                val user = currentUser(call) ?: return@post
                call.ok(mapOf("updated" to historyService.markAllAsRead(user)))
            }
        }

        // ── Audit Logs ──────────────────────────────────────────────────────
        route("/audit-logs") {
            get {
                val user = currentUser(call) ?: return@get
                val p    = call.request.queryParameters
                val pageReq = PageRequest.of(
                    page = p["page"]?.toIntOrNull() ?: 0,
                    size = p["size"]?.toIntOrNull() ?: 20,
                    sort = p["sort"] ?: "activityDatetime,desc",
                )
                call.ok(AuditLogService.findAll(
                    userId   = p["userId"]?.toLongOrNull() ?: user.userId,
                    activity = p["activity"]?.takeIf { it.isNotBlank() },
                    module   = p["module"]?.takeIf { it.isNotBlank() },
                    function = p["function"]?.takeIf { it.isNotBlank() },
                    status   = p["status"]?.takeIf { it.isNotBlank() },
                    pageReq  = pageReq,
                ))
            }
        }
    }
}
