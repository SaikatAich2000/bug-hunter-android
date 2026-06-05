// Inter delivered at runtime via Compose downloadable Google Fonts provider — no TTF assets shipped.
package com.bughunter.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bughunter.R

internal val BhGoogleFontProvider: GoogleFont.Provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val InterGoogleFont = GoogleFont("Inter")
private val JetBrainsMonoGoogleFont = GoogleFont("JetBrains Mono")

internal val InterFontFamily: FontFamily = FontFamily(
    Font(googleFont = InterGoogleFont, fontProvider = BhGoogleFontProvider, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(googleFont = InterGoogleFont, fontProvider = BhGoogleFontProvider, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(googleFont = InterGoogleFont, fontProvider = BhGoogleFontProvider, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(googleFont = InterGoogleFont, fontProvider = BhGoogleFontProvider, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(googleFont = InterGoogleFont, fontProvider = BhGoogleFontProvider, weight = FontWeight.ExtraBold, style = FontStyle.Normal),
)

internal val BhMonoFontFamily: FontFamily = FontFamily(
    Font(googleFont = JetBrainsMonoGoogleFont, fontProvider = BhGoogleFontProvider, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(googleFont = JetBrainsMonoGoogleFont, fontProvider = BhGoogleFontProvider, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(googleFont = JetBrainsMonoGoogleFont, fontProvider = BhGoogleFontProvider, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(googleFont = JetBrainsMonoGoogleFont, fontProvider = BhGoogleFontProvider, weight = FontWeight.Bold, style = FontStyle.Normal),
)

internal val BhTypography: Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = "tnum",
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.02).em,
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em,
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).em,
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.01).em,
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.08.em,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
)

internal val BhCodeInputStyle = TextStyle(
    fontFamily = BhMonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 20.sp,
    letterSpacing = 0.4.em,
    textAlign = TextAlign.Center,
)
