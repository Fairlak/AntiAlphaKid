package ru.fairlak.antialphakid.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MatrixFont = FontFamily.Monospace
val Typography = Typography(
    // Основной текст (списки, описание)
    bodyLarge = TextStyle(
        fontFamily = MatrixFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // Заголовки (названия экранов)
    headlineSmall = TextStyle(
        fontFamily = MatrixFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    // Текст в карточках
    bodyMedium = TextStyle(
        fontFamily = MatrixFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    // Подписи (пакеты приложений)
    labelSmall = TextStyle(
        fontFamily = MatrixFont,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)