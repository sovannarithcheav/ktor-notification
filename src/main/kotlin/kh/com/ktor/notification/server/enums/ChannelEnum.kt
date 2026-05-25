package kh.com.ktor.notification.server.enums

enum class ChannelEnum(val id: Long) {
    EMAIL(1),
    PUSH(2),
    TELEGRAM(3),
    NONE(4);

    companion object {
        fun withTelegram(): List<Long> = listOf(EMAIL.id, PUSH.id, TELEGRAM.id, NONE.id)
        fun default(): List<Long>      = listOf(EMAIL.id, PUSH.id, NONE.id)
    }
}
