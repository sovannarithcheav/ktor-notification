package kh.com.ktor.notification.server.template

data class ResolvedTemplate(
    val subject: String?,
    val body: String,
)

object TemplateResolver {

    fun resolve(
        subject: String?,
        body: String,
        mergeFields: Map<String, String>,
    ): ResolvedTemplate {
        fun String.apply() = mergeFields.entries.fold(this) { acc, (key, value) ->
            acc.replace("\${$key}", value)
        }
        return ResolvedTemplate(subject?.apply(), body.apply())
    }
}
