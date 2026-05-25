package kh.com.ktor.notification.server.channel

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object WebSocketSessionManager {

    private val sessions = ConcurrentHashMap<Long, MutableSet<DefaultWebSocketSession>>()

    fun add(userId: Long, session: DefaultWebSocketSession) {
        sessions.getOrPut(userId) { Collections.newSetFromMap(ConcurrentHashMap()) }.add(session)
    }

    fun remove(userId: Long, session: DefaultWebSocketSession) {
        sessions[userId]?.remove(session)
        if (sessions[userId]?.isEmpty() == true) sessions.remove(userId)
    }

    suspend fun send(userId: Long, message: String) {
        sessions[userId]?.toList()?.forEach { session ->
            runCatching { session.send(Frame.Text(message)) }
                .onFailure { remove(userId, session) }
        }
    }

    fun isConnected(userId: Long): Boolean =
        sessions[userId]?.isNotEmpty() == true
}

class WebSocketChannel(
    override val id: Long = CHANNEL_ID,
    override val name: String = "PUSH",
) : NotificationChannel {

    companion object {
        const val CHANNEL_ID = 2L
    }

    override suspend fun dispatch(
        userId: Long,
        title: String,
        body: String,
        mergeFields: Map<String, String>,
    ): DispatchResult {
        WebSocketSessionManager.send(userId, body)
        val connected = WebSocketSessionManager.isConnected(userId)
        return DispatchResult(
            success = true,
            message = if (connected) "Push sent to user $userId"
                      else "User $userId is not connected via WebSocket",
        )
    }
}
