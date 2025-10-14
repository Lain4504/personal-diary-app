package com.example.personaldiaryapp.data.legacy

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.personaldiaryapp.data.model.DiaryNote

class DiaryNoteDaoLegacy(context: Context) {
	private val dbHelper = DiaryDbHelper(context)

	fun getAll(): List<DiaryNote> {
		val db = dbHelper.readableDatabase
		db.query(
			DiaryDbHelper.TABLE_NOTES,
			arrayOf(
				DiaryDbHelper.COL_ID,
				DiaryDbHelper.COL_TITLE,
				DiaryDbHelper.COL_CONTENT,
				DiaryDbHelper.COL_CREATED_AT
			),
			null, null, null, null,
			"${DiaryDbHelper.COL_CREATED_AT} DESC"
		).use { cursor ->
			val result = ArrayList<DiaryNote>()
			while (cursor.moveToNext()) {
				result.add(cursor.toNote())
			}
			return result
		}
	}

	fun getById(id: Long): DiaryNote? {
		val db = dbHelper.readableDatabase
		db.query(
			DiaryDbHelper.TABLE_NOTES,
			arrayOf(
				DiaryDbHelper.COL_ID,
				DiaryDbHelper.COL_TITLE,
				DiaryDbHelper.COL_CONTENT,
				DiaryDbHelper.COL_CREATED_AT
			),
			"${DiaryDbHelper.COL_ID} = ?",
			arrayOf(id.toString()),
			null, null, null
		).use { cursor ->
			return if (cursor.moveToFirst()) cursor.toNote() else null
		}
	}

	fun insert(note: DiaryNote): Long {
		val db = dbHelper.writableDatabase
		val values = ContentValues().apply {
			put(DiaryDbHelper.COL_TITLE, note.title)
			put(DiaryDbHelper.COL_CONTENT, note.content)
			put(DiaryDbHelper.COL_CREATED_AT, note.createdAt)
		}
		return db.insertOrThrow(DiaryDbHelper.TABLE_NOTES, null, values)
	}

	fun update(note: DiaryNote): Int {
		requireNotNull(note.id)
		val db = dbHelper.writableDatabase
		val values = ContentValues().apply {
			put(DiaryDbHelper.COL_TITLE, note.title)
			put(DiaryDbHelper.COL_CONTENT, note.content)
		}
		return db.update(
			DiaryDbHelper.TABLE_NOTES,
			values,
			"${DiaryDbHelper.COL_ID} = ?",
			arrayOf(note.id.toString())
		)
	}

	fun delete(id: Long): Int {
		val db = dbHelper.writableDatabase
		return db.delete(
			DiaryDbHelper.TABLE_NOTES,
			"${DiaryDbHelper.COL_ID} = ?",
			arrayOf(id.toString())
		)
	}

	private fun Cursor.toNote(): DiaryNote {
		val id = getLong(getColumnIndexOrThrow(DiaryDbHelper.COL_ID))
		val title = getString(getColumnIndexOrThrow(DiaryDbHelper.COL_TITLE))
		val content = getString(getColumnIndexOrThrow(DiaryDbHelper.COL_CONTENT))
		val createdAt = getLong(getColumnIndexOrThrow(DiaryDbHelper.COL_CREATED_AT))
		return DiaryNote(id = id, title = title, content = content, createdAt = createdAt)
	}
}
