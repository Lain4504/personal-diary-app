package com.example.personaldiaryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.personaldiaryapp.data.DiaryRepository
import com.example.personaldiaryapp.data.model.DiaryNote
import com.example.personaldiaryapp.data.room.DiaryDatabase
import com.example.personaldiaryapp.prefs.ThemeMode
import com.example.personaldiaryapp.prefs.UserPreferences
import com.example.personaldiaryapp.storage.InternalExporter
import com.example.personaldiaryapp.ui.theme.PersonalDiaryAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			val prefs = remember { UserPreferences(applicationContext) }
			val themeMode by prefs.themeMode.collectAsState()
			val fontScale by prefs.fontScale.collectAsState()
			PersonalDiaryAppTheme(themeMode = themeMode, fontScale = fontScale) {
				val navController = rememberNavController()
				Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
					NavHost(
						navController = navController,
						startDestination = "list",
						modifier = Modifier.padding(innerPadding)
					) {
						composable("list") {
							NotesListScreen(
								onAdd = { navController.navigate("edit") },
								onEdit = { id -> navController.navigate("edit/$id") },
								onSettings = { navController.navigate("settings") },
								onViewFile = { filename -> navController.navigate("fileView/$filename") }
							)
						}
						composable(
							route = "edit/{id}",
							arguments = listOf(navArgument("id") { type = NavType.LongType })
						) { backStack ->
							NoteEditorScreen(
								noteId = backStack.arguments?.getLong("id"),
								onDone = { navController.popBackStack() },
								onExportInternal = { fileName -> navController.navigate("fileView/$fileName") }
							)
						}
						composable("edit") {
							NoteEditorScreen(noteId = null, onDone = { navController.popBackStack() }, onExportInternal = { file -> navController.navigate("fileView/$file") })
						}
						composable("settings") {
							val prefsLocal = remember { prefs }
							SettingsScreen(
								currentTheme = themeMode,
								currentFontScale = fontScale,
								onThemeChange = prefsLocal::setThemeMode,
								onFontScaleChange = prefsLocal::setFontScale,
								onBack = { navController.popBackStack() }
							)
						}
						composable(
							route = "fileView/{filename}",
							arguments = listOf(navArgument("filename") { type = NavType.StringType })
						) { backStack ->
							FileViewerScreen(
								fileName = backStack.arguments?.getString("filename").orEmpty(),
								onBack = { navController.popBackStack() }
							)
						}
					}
				}
			}
		}
	}
}

@Composable
fun NotesListScreen(
	onAdd: () -> Unit,
	onEdit: (Long) -> Unit,
	onSettings: () -> Unit,
	onViewFile: (String) -> Unit
) {
	val context = LocalContext.current
	var notes by remember { mutableStateOf(emptyList<DiaryNote>()) }
	var refreshKey by remember { mutableStateOf(0) }
	var query by remember { mutableStateOf("") }
	val scope = rememberCoroutineScope()
	var loadError by remember { mutableStateOf<String?>(null) }

	LaunchedEffect(refreshKey) {
		loadError = null
		try {
			val repo = DiaryRepository(context)
			notes = repo.getAll()
		} catch (t: Throwable) {
			loadError = t.message
			notes = emptyList()
		}
	}

	val createDocLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
		ActivityResultContracts.CreateDocument("text/plain")
	) { uri ->
		if (uri != null) {
			val filtered = notes.filter { it.title.contains(query, ignoreCase = true) }
			val text = buildString {
				filtered.forEach { n ->
					appendLine(n.title)
					appendLine(n.content)
					appendLine("---")
				}
			}
			scope.launch(Dispatchers.IO) {
				context.contentResolver.openOutputStream(uri)?.use { out ->
					out.write(text.toByteArray())
				}
			}
		}
	}

	val backupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
		ActivityResultContracts.CreateDocument("application/octet-stream")
	) { uri ->
		if (uri != null) {
			scope.launch(Dispatchers.IO) {
				// Ensure DB state is flushed
				DiaryDatabase.closeAndClear()
				val dbFile = context.getDatabasePath("diary.db")
				context.contentResolver.openOutputStream(uri)?.use { out ->
					dbFile.inputStream().use { input -> input.copyTo(out) }
				}
			}
		}
	}

	val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
		ActivityResultContracts.OpenDocument()
	) { uri ->
		if (uri != null) {
			scope.launch(Dispatchers.IO) {
				// Close Room and remove old files
				DiaryDatabase.closeAndClear()
				val dbFile = context.getDatabasePath("diary.db")
				val wal = File(dbFile.path + "-wal")
				val shm = File(dbFile.path + "-shm")
				try { if (wal.exists()) wal.delete() } catch (_: Throwable) {}
				try { if (shm.exists()) shm.delete() } catch (_: Throwable) {}
				try { if (dbFile.exists()) dbFile.delete() } catch (_: Throwable) {}
				// Copy in new DB
				context.contentResolver.openInputStream(uri)?.use { input ->
					dbFile.outputStream().use { out -> input.copyTo(out) }
				}
				// Trigger reload on main thread
				launch(Dispatchers.Main) { refreshKey++ }
			}
		}
	}

	val visible = notes.filter { it.title.contains(query, ignoreCase = true) }

	Column(modifier = Modifier.padding(16.dp)) {
		Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
			Button(onClick = onAdd) { Text("Add") }
			Button(onClick = onSettings) { Text("Settings") }
		}
		Spacer(Modifier.height(12.dp))
		OutlinedTextField(
			value = query,
			onValueChange = { query = it },
			label = { Text("Search title") },
			modifier = Modifier.fillMaxWidth()
		)
		if (loadError != null) {
			Spacer(Modifier.height(8.dp))
			Text("Load error: ${'$'}loadError")
		}
		Spacer(Modifier.height(12.dp))
		Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
			Button(onClick = { createDocLauncher.launch("diary_export.txt") }) { Text("Export All (SAF)") }
			Button(onClick = { backupLauncher.launch("diary_backup.db") }) { Text("Backup DB") }
			Button(onClick = { restoreLauncher.launch(arrayOf("*/*")) }) { Text("Restore DB") }
		}
		Spacer(Modifier.height(12.dp))
		LazyColumn {
			items(visible) { n ->
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.clickable { n.id?.let { onEdit(it) } }
						.padding(vertical = 8.dp),
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Column(modifier = Modifier.weight(1f)) {
						Text(n.title)
						Spacer(Modifier.height(4.dp))
						Text(n.content.take(60))
					}
					Row {
						TextButton(onClick = { n.id?.let {
							DiaryRepository(context).delete(it)
							refreshKey++
						} }) { Text("Delete") }
					}
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
fun NotesListPreview() {
	PersonalDiaryAppTheme() {
		NotesListScreen({}, {}, {}, {})
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(noteId: Long?, onDone: () -> Unit, onExportInternal: (String) -> Unit) {
	val context = LocalContext.current
	val repo = remember { DiaryRepository(context) }
	var title by remember { mutableStateOf("") }
	var content by remember { mutableStateOf("") }
	var createdAt by remember { mutableStateOf<Long?>(null) }

	LaunchedEffect(noteId) {
		if (noteId != null) {
			repo.getById(noteId)?.let { note ->
				title = note.title
				content = note.content
				createdAt = note.createdAt
			}
		}
	}

	Column(modifier = Modifier.padding(16.dp)) {
		val titleError = title.isBlank()
		OutlinedTextField(
			value = title,
			onValueChange = { title = it },
			label = { Text("Title") },
			isError = titleError,
			supportingText = { if (titleError) Text("Title is required") },
			modifier = Modifier.fillMaxWidth()
		)
		Spacer(Modifier.height(12.dp))
		val contentError = content.isBlank()
		OutlinedTextField(
			value = content,
			onValueChange = { content = it },
			label = { Text("Content") },
			isError = contentError,
			supportingText = { if (contentError) Text("Content is required") },
			modifier = Modifier.fillMaxWidth()
		)
		Spacer(Modifier.height(16.dp))
		Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
			Button(onClick = {
				if (title.isBlank() || content.isBlank()) return@Button
				val trimmedTitle = title.trim()
				val trimmedContent = content.trim()
				val id = if (noteId == null) {
					repo.insert(
						DiaryNote(
							title = trimmedTitle,
							content = trimmedContent,
							createdAt = System.currentTimeMillis()
						)
					)
				} else {
					repo.update(
						DiaryNote(
							id = noteId,
							title = trimmedTitle,
							content = trimmedContent,
							createdAt = createdAt ?: System.currentTimeMillis()
						)
					)
					noteId
				}
				onDone()
			}, enabled = !titleError && !contentError) { Text("Save") }
			Button(onClick = {
				// Export this note to internal storage as text
				val exportTitle = if (title.isBlank()) "note" else title
				val safe = exportTitle.replace('[', '(').replace(']', ')').replace('/', '-')
				val fileName = "$safe.txt"
				InternalExporter.saveText(context, fileName, content)
				onExportInternal(fileName)
			}) { Text("Export Internal") }
			if (noteId != null) {
				Button(onClick = { repo.delete(noteId); onDone() }) { Text("Delete") }
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	currentTheme: ThemeMode,
	currentFontScale: Float,
	onThemeChange: (ThemeMode) -> Unit,
	onFontScaleChange: (Float) -> Unit,
	onBack: () -> Unit,
) {
	Column(modifier = Modifier.padding(16.dp)) {
		Text("Settings")
		Spacer(Modifier.height(16.dp))
		Text("Theme")
		Spacer(Modifier.height(8.dp))
		RowThemeSelector(currentTheme = currentTheme, onThemeChange = onThemeChange)
		Spacer(Modifier.height(16.dp))
		Text("Font size")
		Spacer(Modifier.height(8.dp))
		RowFontScaleSelector(current = currentFontScale, onChange = onFontScaleChange)
		Spacer(Modifier.height(24.dp))
		Button(onClick = onBack) { Text("Back") }
	}
}

@Composable
private fun RowThemeSelector(currentTheme: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
	Column {
		TextButton(onClick = { onThemeChange(ThemeMode.System) }) { Text(label(ThemeMode.System, currentTheme)) }
		TextButton(onClick = { onThemeChange(ThemeMode.Light) }) { Text(label(ThemeMode.Light, currentTheme)) }
		TextButton(onClick = { onThemeChange(ThemeMode.Dark) }) { Text(label(ThemeMode.Dark, currentTheme)) }
	}
}

private fun label(mode: ThemeMode, current: ThemeMode): String =
	(if (mode == current) "• " else "") + mode.name

@Composable
private fun RowFontScaleSelector(current: Float, onChange: (Float) -> Unit) {
	Column {
		listOf(0.85f, 1.0f, 1.15f).forEach { scale ->
			TextButton(onClick = { onChange(scale) }) { Text((if (scale == current) "• " else "") + "${scale}x") }
		}
	}
}

@Composable
fun FileViewerScreen(fileName: String, onBack: () -> Unit) {
	val context = LocalContext.current
	var content by remember(fileName) { mutableStateOf("") }
	LaunchedEffect(fileName) {
		content = runCatching { InternalExporter.readText(context, fileName) }.getOrElse { it.message ?: "" }
	}
	Column(modifier = Modifier.padding(16.dp)) {
		Text("File: $fileName")
		Spacer(Modifier.height(12.dp))
		Text(content)
		Spacer(Modifier.height(24.dp))
		Button(onClick = onBack) { Text("Back") }
	}
}