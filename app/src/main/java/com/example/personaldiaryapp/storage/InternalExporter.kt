package com.example.personaldiaryapp.storage

import android.content.Context
import java.io.File

object InternalExporter {
	fun saveText(context: Context, fileName: String, text: String): String {
		context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(text.toByteArray()) }
		return fileName
	}

	fun readText(context: Context, fileName: String): String {
		return context.openFileInput(fileName).bufferedReader().use { it.readText() }
	}

	fun listFiles(context: Context): List<File> = context.filesDir.listFiles()?.toList().orEmpty()
}
