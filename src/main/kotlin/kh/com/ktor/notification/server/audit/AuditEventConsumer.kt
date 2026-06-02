package kh.com.ktor.notification.server.audit

import com.rabbitmq.client.ConnectionFactory
import kh.com.ktor.notification.server.repository.AuditLogRepository
import kotlinx.serialization.json.Json

class AuditEventConsumer {

    private val EXCHANGE = "audit.events"
    private val QUEUE    = "notification.audit"

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
        channel.queueBind(QUEUE, EXCHANGE, "audit")
        channel.basicQos(1)

        channel.basicConsume(QUEUE, false,
            { _, delivery ->
                try {
                    val msg = Json.decodeFromString<AuditEventMessage>(String(delivery.body, Charsets.UTF_8))
                    // Chain non-root workflow rows (approve/reject/cancel) back to their
                    // request-* root via request_change_id, unless an explicit referenceId was given.
                    val referenceId = msg.referenceId ?: msg.requestChangeId
                        ?.takeIf { !msg.activity.startsWith("request-") }
                        ?.let { AuditLogRepository.findRootByRequestChangeId(it) }
                    AuditLogRepository.save(
                        activity        = msg.activity,
                        function        = msg.function,
                        module          = msg.module,
                        userId          = msg.userId,
                        username        = msg.username,
                        roleType        = msg.roleType,
                        description     = msg.description,
                        referenceId     = referenceId,
                        requestChangeId = msg.requestChangeId,
                    )
                    channel.basicAck(delivery.envelope.deliveryTag, false)
                } catch (_: Exception) {
                    channel.basicNack(delivery.envelope.deliveryTag, false, true)
                }
            },
            { _ -> }
        )
    }
}
