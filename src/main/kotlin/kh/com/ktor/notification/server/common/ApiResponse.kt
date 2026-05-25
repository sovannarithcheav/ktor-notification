package kh.com.ktor.notification.server.common

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class ApiResponse(val data: JsonElement? = null, val error: String? = null)

suspend inline fun <reified T> ApplicationCall.ok(data: T) =
    respond(HttpStatusCode.OK, ApiResponse(data = Json.encodeToJsonElement(data)))

suspend inline fun <reified T> ApplicationCall.created(data: T) =
    respond(HttpStatusCode.Created, ApiResponse(data = Json.encodeToJsonElement(data)))

suspend inline fun <reified T> ApplicationCall.accepted(data: T) =
    respond(HttpStatusCode.Accepted, ApiResponse(data = Json.encodeToJsonElement(data)))

suspend fun ApplicationCall.noContent() =
    respond(HttpStatusCode.NoContent)

suspend fun ApplicationCall.badRequest(message: String) =
    respond(HttpStatusCode.BadRequest, ApiResponse(error = message))

suspend fun ApplicationCall.notFound(message: String = "Not found") =
    respond(HttpStatusCode.NotFound, ApiResponse(error = message))

suspend fun ApplicationCall.unprocessable(message: String) =
    respond(HttpStatusCode.UnprocessableEntity, ApiResponse(error = message))

suspend fun ApplicationCall.serverError(message: String) =
    respond(HttpStatusCode.InternalServerError, ApiResponse(error = message))

suspend inline fun <reified T> ApplicationCall.respondData(status: HttpStatusCode, data: T) =
    respond(status, ApiResponse(data = Json.encodeToJsonElement(data)))
