package kh.com.ktor.notification.server.entity

import org.jetbrains.exposed.sql.SortOrder

data class PageRequest(
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "id",
    val sortDir: String = "desc",
) {
    val offset: Long get() = page.toLong() * size

    companion object {
        fun of(
            page: Int = 0,
            size: Int = 20,
            sort: String = "id,desc",
        ): PageRequest {
            val parts = sort.split(",")
            return PageRequest(
                page    = page.coerceAtLeast(0),
                size    = size.coerceIn(1, 100),
                sortBy  = parts.getOrElse(0) { "id" }.trim(),
                sortDir = parts.getOrElse(1) { "desc" }.trim(),
            )
        }
    }
}

internal fun PageRequest.order() =
    if (sortDir.equals("asc", ignoreCase = true)) SortOrder.ASC else SortOrder.DESC
