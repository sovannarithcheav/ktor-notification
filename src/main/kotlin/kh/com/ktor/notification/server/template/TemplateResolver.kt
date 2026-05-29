package kh.com.ktor.notification.server.template

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class ResolvedTemplate(
    val subject: String?,
    val body: String,
)

object TemplateResolver {

    fun resolve(
        subject: String?,
        body: String,
        mergeFields: Map<String, String>,
        metadata: JsonObject? = null,
    ): ResolvedTemplate {
        val effective = HashMap<String, String>(mergeFields)
        metadata?.forEach { (k, v) ->
            effective["meta.$k"] = (v as? JsonPrimitive)?.contentOrNull ?: v.toString()
        }
        fun String.apply() = effective.entries.fold(this) { acc, (key, value) ->
            acc.replace("\${$key}", value)
        }
        return ResolvedTemplate(subject?.apply(), body.apply())
    }
}
