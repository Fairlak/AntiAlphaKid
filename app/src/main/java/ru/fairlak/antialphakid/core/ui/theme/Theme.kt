package ru.fairlak.antialphakid.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MatrixGreen,
    secondary = DarkGreen,
    tertiary = MatrixGreen,
    background = TerminalBlack,
    surface = TerminalGray,
    onPrimary = Color.Black,
    onBackground = MatrixGreen,
    onSurface = MatrixGreen
)

@Composable
fun AntiAlphaKidTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}