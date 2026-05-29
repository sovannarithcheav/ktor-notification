package kh.com.ktor.notification.server.entity

import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Column

@JvmInline
value class IntList(val list: List<Int>) : Comparable<IntList> {
    override fun compareTo(other: IntList): Int = list.size.compareTo(other.list.size)
}

@JvmInline
value class LongList(val list: List<Long>) : Comparable<LongList> {
    override fun compareTo(other: LongList): Int = list.size.compareTo(other.list.size)
}

class IntArrayColumnType : ColumnType<IntList>() {
    override fun sqlType(): String = "INTEGER[]"

    override fun valueFromDB(value: Any): IntList {
        val list = when (value) {
            is java.sql.Array -> {
                @Suppress("UNCHECKED_CAST")
                (value.array as Array<Int>).toList()
            }
            is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                (value as Array<Int>).toList()
            }
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                value as List<Int>
            }
            else -> emptyList()
        }
        return IntList(list)
    }

    override fun notNullValueToDB(value: IntList): Any = value.list.toTypedArray()
}

class LongArrayColumnType : ColumnType<LongList>() {
    override fun sqlType(): String = "BIGINT[]"

    override fun valueFromDB(value: Any): LongList {
        val list = when (value) {
            is java.sql.Array -> {
                @Suppress("UNCHECKED_CAST")
                (value.array as Array<Long>).toList()
            }
            is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                (value as Array<Long>).toList()
            }
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                value as List<Long>
            }
            else -> emptyList()
        }
        return LongList(list)
    }

    override fun notNullValueToDB(value: LongList): Any = value.list.toTypedArray()
}

fun <T : Table> T.intArray(name: String): Column<IntList> = registerColumn(name, IntArrayColumnType())

fun <T : Table> T.longArray(name: String): Column<LongList> = registerColumn(name, LongArrayColumnType())
