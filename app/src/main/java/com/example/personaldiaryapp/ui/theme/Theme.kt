package com.example.personaldiaryapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.example.personaldiaryapp.prefs.ThemeMode

private val DarkColorScheme = darkColorScheme(
	primary = Purple80,
	secondary = PurpleGrey80,
	tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
	primary = Purple40,
	secondary = PurpleGrey40,
	tertiary = Pink40

	/* Other default colors to override
	background = Color(0xFFFFFBFE),
	surface = Color(0xFFFFFBFE),
	onPrimary = Color.White,
	onSecondary = Color.White,
	onTertiary = Color.White,
	onBackground = Color(0xFF1C1B1F),
	onSurface = Color(0xFF1C1B1F),
	*/
)

@Composable
fun PersonalDiaryAppTheme(
	themeMode: ThemeMode = ThemeMode.System,
	fontScale: Float = 1.0f,
	// Dynamic color is available on Android 12+
	dynamicColor: Boolean = true,
	content: @Composable () -> Unit
) {
	val isDark = when (themeMode) {
		ThemeMode.System -> isSystemInDarkTheme()
		ThemeMode.Light -> false
		ThemeMode.Dark -> true
	}

	val colorScheme = when {
		dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
			val context = LocalContext.current
			if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
		}

		isDark -> DarkColorScheme
		else -> LightColorScheme
	}

	val scaledTypography = Typography.copy(
		displayLarge = Typography.displayLarge.scale(fontScale),
		displayMedium = Typography.displayMedium.scale(fontScale),
		displaySmall = Typography.displaySmall.scale(fontScale),
		headlineLarge = Typography.headlineLarge.scale(fontScale),
		headlineMedium = Typography.headlineMedium.scale(fontScale),
		headlineSmall = Typography.headlineSmall.scale(fontScale),
		titleLarge = Typography.titleLarge.scale(fontScale),
		titleMedium = Typography.titleMedium.scale(fontScale),
		titleSmall = Typography.titleSmall.scale(fontScale),
		bodyLarge = Typography.bodyLarge.scale(fontScale),
		bodyMedium = Typography.bodyMedium.scale(fontScale),
		bodySmall = Typography.bodySmall.scale(fontScale),
		labelLarge = Typography.labelLarge.scale(fontScale),
		labelMedium = Typography.labelMedium.scale(fontScale),
		labelSmall = Typography.labelSmall.scale(fontScale),
	)

	MaterialTheme(
		colorScheme = colorScheme,
		typography = scaledTypography,
		content = content
	)
}

private fun TextStyle.scale(factor: Float): TextStyle {
	if (factor == 1.0f) return this
	val sizeSp = this.fontSize.value
	return this.copy(fontSize = (sizeSp * factor).sp)
}