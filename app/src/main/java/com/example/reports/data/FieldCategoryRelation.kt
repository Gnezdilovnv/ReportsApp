package com.example.reports.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "field_category_relation")
data class FieldCategoryRelation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fieldId: String,
    val categoryId: String
)
