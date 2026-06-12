package kh.com.ktor.notification.server.user

import com.rabbitmq.client.ConnectionFactory
import kh.com.ktor.notification.server.repository.UserRepository
import kotlinx.serialization.json.Json

class UserEventConsumer {

    private val EXCHANGE = "user.events"
    private val QUEUE    = "notification.users"

    fun start(host: String, port: Int, username: String, password: String) {
        val factory = ConnectionFactory().apply {
            this.host     = host
            this.port     = port
            this.username = username
            this.password = password
        }
        val conn    = factory.newConnection()
        val channel = conn.createChannel()

        channel.exchangeDeclare(EXCHANGE, "topic", true)
        channel.queueDeclare(QUEUE, true, false, false, null)
        channel.queueBind(QUEUE, EXCHANGE, "user")
        channel.basicQos(1)

        channel.basicConsume(QUEUE, false,
            { _, delivery ->
                try {
                    val msg = Json.decodeFromString<UserSyncMessage>(String(delivery.body, Charsets.UTF_8))
                    // Resolution table is email-keyed and email is NOT NULL: skip (ack) users with no email.
                    if (!msg.email.isNullOrBlank()) {
                        UserRepository.upsert(msg.id, msg.email, msg.fullName)
                    }
                    channel.basicAck(delivery.envelope.deliveryTag, false)
                } catch (_: Exception) {
                    channel.basicNack(delivery.envelope.deliveryTag, false, true)
                }
            },
            { _ -> }
        )
    }
}
