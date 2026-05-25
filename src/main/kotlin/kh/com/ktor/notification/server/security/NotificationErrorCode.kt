package kh.com.ktor.notification.server.security

class NotificationException(val code: String, override val message: String) : RuntimeException(message)

enum class NotificationErrorCode(private val template: String) {
    NOTI006("Event type does not match your role"),
    NOTI007("Cannot unsubscribe from required event [%s]"),
    NOTI010("Event not found: %s"),
    NOTI011("Invalid channel selection"),
    NOTI013("No template configured for event [%s] on channel(s): %s"),
    NOTI014("Role [%s] is not allowed to subscribe to channel [%s]"),
    NOTI015("Placeholder mismatch — declared but not in body: [%s] | in body but not declared: [%s]");

    fun issue(args: Array<Any> = emptyArray()): Nothing =
        throw NotificationException(name, if (args.isEmpty()) template else template.format(*args))
}
