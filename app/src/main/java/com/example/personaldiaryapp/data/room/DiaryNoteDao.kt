package com.example.personaldiaryapp.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DiaryNoteDao {
	@Query("SELECT * FROM notes ORDER BY createdAt DESC")
	suspend fun getAll(): List<DiaryNoteEntity>

	@Query("SELECT * FROM notes WHERE id = :id")
	suspend fun getById(id: Long): DiaryNoteEntity?

	@Insert
	suspend fun insert(entity: DiaryNoteEntity): Long

	@Update
	suspend fun update(entity: DiaryNoteEntity): Int

	@Query("DELETE FROM notes WHERE id = :id")
	suspend fun deleteById(id: Long): Int
}
