package kh.com.ktor.notification.server.user

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object UserEventPublisher {

    private const val EXCHANGE    = "user.events"
    private const val ROUTING_KEY = "user"

    private var connection: Connection? = null

    fun configure(host: String, port: Int, username: String, password: String) {
        val factory = ConnectionFactory().apply {
            this.host     = host
            this.port     = port
            this.username = username
            this.password = password
        }
        connection = factory.newConnection()
        connection!!.createChannel().use { ch ->
            ch.exchangeDeclare(EXCHANGE, "topic", true)
        }
    }

    fun publish(event: UserSyncMessage) {
        val conn = connection ?: return
        try {
            val ch    = conn.createChannel()
            val body  = Json.encodeToString(event).toByteArray(Charsets.UTF_8)
            val props = AMQP.BasicProperties.Builder().deliveryMode(2).build()
            ch.basicPublish(EXCHANGE, ROUTING_KEY, props, body)
            ch.close()
        } catch (_: Exception) {
            // best-effort; never block a user write
        }
    }
}
