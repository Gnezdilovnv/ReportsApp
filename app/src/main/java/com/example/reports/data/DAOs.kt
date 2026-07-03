package com.example.reports.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY createdAt DESC")
    suspend fun getAll(): List<Category>
    
    @Query("SELECT * FROM categories ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<Category>>
    
    @Insert
    suspend fun insert(category: Category)
    
    @Update
    suspend fun update(category: Category)
    
    @Delete
    suspend fun delete(category: Category)
}

@Dao
interface FieldDao {
    // ПРАВИЛЬНЫЙ ЗАПРОС С JOIN
    @Transaction
    @Query("""
        SELECT f.* FROM fields f
        JOIN field_category_relation r ON f.id = r.fieldId
        WHERE r.categoryId = :categoryId
        ORDER BY f.createdAt DESC
    """)
    suspend fun getByCategoryId(categoryId: String): List<Field>
    
    @Query("SELECT * FROM fields ORDER BY createdAt DESC")
    suspend fun getAll(): List<Field>
    
    @Insert
    suspend fun insert(field: Field)
    
    @Insert
    suspend fun insertRelation(relation: FieldCategoryRelation)
    
    @Update
    suspend fun update(field: Field)
    
    @Delete
    suspend fun delete(field: Field)
    
    @Query("DELETE FROM field_category_relation WHERE fieldId = :fieldId")
    suspend fun deleteRelations(fieldId: String)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    suspend fun getAll(): List<Report>
    
    @Query("SELECT * FROM reports WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    suspend fun getByCategoryId(categoryId: String): List<Report>
    
    @Insert
    suspend fun insert(report: Report)
    
    @Delete
    suspend fun delete(report: Report)
    
    @Update
    suspend fun update(report: Report)
}
