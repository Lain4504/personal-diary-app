package com.example.personaldiaryapp.data

import android.content.Context
import com.example.personaldiaryapp.data.model.DiaryNote
import com.example.personaldiaryapp.data.room.DiaryDatabase
import com.example.personaldiaryapp.data.room.DiaryNoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class DiaryRepository(context: Context) {
	private val roomDb = DiaryDatabase.getInstance(context)
	private val dao = roomDb.noteDao()

	fun getAll(): List<DiaryNote> = runBlocking(Dispatchers.IO) {
		dao.getAll().map { it.toModel() }
	}

	fun getById(id: Long): DiaryNote? = runBlocking(Dispatchers.IO) {
		dao.getById(id)?.toModel()
	}

	fun insert(note: DiaryNote): Long = runBlocking(Dispatchers.IO) {
		require(note.title.isNotBlank() && note.content.isNotBlank())
		dao.insert(note.toEntity(withId = false))
	}

	fun update(note: DiaryNote): Int = runBlocking(Dispatchers.IO) {
		val id = requireNotNull(note.id)
		require(note.title.isNotBlank() && note.content.isNotBlank())
		dao.update(note.toEntity(withId = true))
		1
	}

	fun delete(id: Long): Int = runBlocking(Dispatchers.IO) {
		dao.deleteById(id)
	}
}

private fun DiaryNoteEntity.toModel(): DiaryNote =
	DiaryNote(id = id, title = title, content = content, createdAt = createdAt)

private fun DiaryNote.toEntity(withId: Boolean): DiaryNoteEntity =
	if (withId) DiaryNoteEntity(id = requireNotNull(id), title = title, content = content, createdAt = createdAt)
	else DiaryNoteEntity(title = title, content = content, createdAt = createdAt)
