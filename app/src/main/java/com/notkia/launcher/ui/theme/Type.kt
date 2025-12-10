package com.notkia.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.notkia.launcher.R

val robotoCondensed = FontFamily(
    Font(R.font.robotocondensed_regular, FontWeight.Normal),
    Font(R.font.robotocondensed_bold, FontWeight.Bold),
    Font(R.font.robotocondensed_black, FontWeight.Black),
    Font(R.font.robotocondensed_light, FontWeight.Light)
)

val anton = FontFamily(
    Font(R.font.anton_regular, FontWeight.Normal)
)

val GlobalFontSize = 16.sp

// Set of Material typography styles to start with
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = GlobalFontSize,
    ),
    displayMedium = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = GlobalFontSize,
    ),
    displaySmall = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = GlobalFontSize,
    ),
    headlineLarge = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = GlobalFontSize,
    ),
    headlineMedium = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = GlobalFontSize,
    ),
    headlineSmall = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = GlobalFontSize,
    ),
    titleLarge = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = GlobalFontSize,
    ),
    titleMedium = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Medium,
        fontSize = GlobalFontSize,
    ),
    titleSmall = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Medium,
        fontSize = GlobalFontSize,
    ),
    bodyLarge = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = GlobalFontSize,
    ),
    bodyMedium = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = GlobalFontSize,
    ),
    bodySmall = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = GlobalFontSize,
    ),
    labelLarge = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Medium,
        fontSize = GlobalFontSize,
    ),
    labelMedium = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Medium,
        fontSize = GlobalFontSize,
    ),
    labelSmall = TextStyle(
        fontFamily = robotoCondensed,
        fontWeight = FontWeight.Medium,
        fontSize = GlobalFontSize,
    )
)
