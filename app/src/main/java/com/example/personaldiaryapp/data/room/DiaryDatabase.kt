package com.example.personaldiaryapp.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DiaryNoteEntity::class], version = 1)
abstract class DiaryDatabase : RoomDatabase() {
	abstract fun noteDao(): DiaryNoteDao

	companion object {
		@Volatile private var INSTANCE: DiaryDatabase? = null
		fun getInstance(context: Context): DiaryDatabase = INSTANCE ?: synchronized(this) {
			INSTANCE ?: Room.databaseBuilder(context.applicationContext, DiaryDatabase::class.java, "diary.db").build().also { INSTANCE = it }
		}

		fun closeAndClear() {
			try { INSTANCE?.close() } catch (_: Throwable) {}
			INSTANCE = null
		}
	}
}
