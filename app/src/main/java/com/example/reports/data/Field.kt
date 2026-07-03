package com.example.reports.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "fields")
data class Field(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: FieldType,
    val isRequired: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class FieldType {
    TEXT, DATE_TIME, LOCATION, SWITCH_YES_NO, NUMBER, PHOTO
}
