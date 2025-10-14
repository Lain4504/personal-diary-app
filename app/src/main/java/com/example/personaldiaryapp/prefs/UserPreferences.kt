package com.example.personaldiaryapp.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { System, Light, Dark }

class UserPreferences(context: Context) {
	private val prefs: SharedPreferences =
		context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

	private val _themeMode = MutableStateFlow(getThemeMode())
	val themeMode: StateFlow<ThemeMode> = _themeMode

	private val _fontScale = MutableStateFlow(getFontScale())
	val fontScale: StateFlow<Float> = _fontScale

	fun setThemeMode(mode: ThemeMode) {
		prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
		_themeMode.value = mode
	}

	fun setFontScale(scale: Float) {
		prefs.edit().putFloat(KEY_FONT_SCALE, scale).apply()
		_fontScale.value = scale
	}

	private fun getThemeMode(): ThemeMode {
		return runCatching {
			ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.System.name)!!)
		}.getOrDefault(ThemeMode.System)
	}

	private fun getFontScale(): Float {
		return prefs.getFloat(KEY_FONT_SCALE, 1.0f)
	}

	companion object {
		private const val KEY_THEME_MODE = "theme_mode"
		private const val KEY_FONT_SCALE = "font_scale"
	}
}
