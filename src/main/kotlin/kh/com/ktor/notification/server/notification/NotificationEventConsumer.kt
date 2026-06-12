package kh.com.ktor.notification.server.notification

import com.rabbitmq.client.ConnectionFactory
import kh.com.ktor.notification.server.service.NotificationSendService
import kh.com.ktor.notification.server.service.SendNotificationRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class NotificationEventConsumer(private val service: NotificationSendService = NotificationSendService()) {

    private val EXCHANGE = "notification.events"
    private val QUEUE    = "notification.messages"

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
        channel.queueBind(QUEUE, EXCHANGE, "notification")
        channel.basicQos(1)

        channel.basicConsume(QUEUE, false,
            { _, delivery ->
                try {
                    val msg = Json.decodeFromString<NotificationEventMessage>(String(delivery.body, Charsets.UTF_8))
                    runBlocking {
                        service.send(SendNotificationRequest(msg.userId, msg.eventCode, msg.mergeFields, msg.forceChannels))
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
