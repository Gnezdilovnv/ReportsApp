package com.example.reports.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun fromStringMap(value: String): Map<String, String> {
        return if (value.isEmpty()) emptyMap() else 
            value.split(";").associate {
                val parts = it.split(":")
                if (parts.size == 2) parts[0] to parts[1] else "" to ""
            }
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String {
        return map.map { "${it.key}:${it.value}" }.joinToString(";")
    }
}
