package com.example.personaldiaryapp.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class DiaryNoteEntity(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	val title: String,
	val content: String,
	val createdAt: Long
)
