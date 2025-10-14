package com.example.personaldiaryapp.data.legacy

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DiaryDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
	override fun onCreate(db: SQLiteDatabase) {
		db.execSQL(
			"CREATE TABLE $TABLE_NOTES (" +
				"$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
				"$COL_TITLE TEXT NOT NULL," +
				"$COL_CONTENT TEXT NOT NULL," +
				"$COL_CREATED_AT INTEGER NOT NULL" +
			")"
		)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		// no-op for v1
	}

	companion object {
		const val DB_NAME = "diary.db"
		const val DB_VERSION = 1
		const val TABLE_NOTES = "notes"
		const val COL_ID = "id"
		const val COL_TITLE = "title"
		const val COL_CONTENT = "content"
		const val COL_CREATED_AT = "createdAt"
	}
}
