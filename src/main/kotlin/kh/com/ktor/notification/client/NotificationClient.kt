package kh.com.ktor.notification.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SendNotificationRequest(
    val userId: Long,
    val eventCode: String,
    val mergeFields: Map<String, String> = emptyMap(),
)

object NotificationClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout)
        expectSuccess = false
    }

    private var baseUrl: String = "http://localhost:8088"

    fun configure(url: String) {
        baseUrl = url.trimEnd('/')
    }

    suspend fun send(
        userId: Long,
        eventCode: String,
        mergeFields: Map<String, String> = emptyMap(),
    ) {
        httpClient.post("$baseUrl/api/v1/notification/send") {
            contentType(ContentType.Application.Json)
            setBody(SendNotificationRequest(userId, eventCode, mergeFields))
        }
    }
}
