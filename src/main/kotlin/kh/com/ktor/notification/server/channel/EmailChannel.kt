package kh.com.ktor.notification.server.channel

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

data class EmailConfig(
    val host: String,
    val port: Int,
    val from: String,
    val username: String = "",
    val password: String = "",
    val auth: Boolean = true,
    val sslEnable: Boolean = true,
    val starttlsEnable: Boolean = false,
    val debug: Boolean = false,
)

fun interface UserEmailResolver {
    suspend fun resolve(userId: Long): String?
}

class EmailChannel(
    private val config: EmailConfig,
    private val userEmailResolver: UserEmailResolver,
    override val id: Long = CHANNEL_ID,
    override val name: String = "EMAIL",
) : NotificationChannel {

    companion object {
        const val CHANNEL_ID = 1L
    }

    override suspend fun dispatch(
        userId: Long,
        title: String,
        body: String,
        mergeFields: Map<String, String>,
    ): DispatchResult {
        val email = userEmailResolver.resolve(userId)
            ?: return DispatchResult(false, "No email found for user $userId")

        return try {
            sendMail(email, title, body)
            DispatchResult(true, "Email sent to $email")
        } catch (e: Exception) {
            DispatchResult(false, "Email failed: ${e.message}")
        }
    }

    private fun sendMail(to: String, subject: String, body: String) {
        val props = Properties().apply {
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port.toString())
            put("mail.debug", config.debug.toString())
            put("mail.smtp.auth", config.auth.toString())
            put("mail.smtp.ssl.enable", config.sslEnable.toString())
            put("mail.smtp.starttls.enable", config.starttlsEnable.toString())
        }
        val authenticator = if (config.auth) {
            object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(config.username, config.password)
            }
        } else null

        val session = Session.getInstance(props, authenticator)
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(config.from))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            setSubject(subject)
            setText(body)
        }
        Transport.send(message)
    }
}
