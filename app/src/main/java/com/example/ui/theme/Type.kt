package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.R

// Font Provider
private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Three typefaces, three jobs, no overlap.
// 1. Interface: Bricolage Grotesque
val BricolageFont = GoogleFont("Bricolage Grotesque")
val BricolageGrotesqueFamily = FontFamily(
    Font(googleFont = BricolageFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = BricolageFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = BricolageFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = BricolageFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

// 2. Prose: Newsreader (Chat replies and quoted syllabus text only. Never for UI)
val NewsreaderFont = GoogleFont("Newsreader")
val NewsreaderFamily = FontFamily(
    Font(googleFont = NewsreaderFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = NewsreaderFont, fontProvider = fontProvider, weight = FontWeight.Medium)
)

// 3. Measured: Martian Mono (Dates, weights, hours, week numbers, percentages, counts. Always.)
val MartianMonoFont = GoogleFont("Martian Mono")
val MartianMonoFamily = FontFamily(
    Font(googleFont = MartianMonoFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = MartianMonoFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = MartianMonoFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

/**
 * Strict Scale: 11 / 13 / 15 / 18 / 24 / 34 / 56. Nothing between.
 * Line height 1.15 for display, 1.55 for prose.
 * Tight tracking at large sizes for Interface.
 */
object CollideType {
    // Display / Headings (Interface)
    val display56 = TextStyle(
        fontFamily = BricolageGrotesqueFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = (56 * 1.15).sp,
        letterSpacing = (-0.03).em,
        color = Ink
    )

    val headline34 = TextStyle(
        fontFamily = BricolageGrotesqueFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = (34 * 1.15).sp,
        letterSpacing = (-0.02).em,
        color = Ink
    )

    val title24 = TextStyle(
        fontFamily = BricolageGrotesqueFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = (24 * 1.15).sp,
        letterSpacing = (-0.01).em,
        color = Ink
    )

    val interface18 = TextStyle(
        fontFamily = BricolageGrotesqueFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = (18 * 1.2).sp,
        color = Ink
    )

    val interface15 = TextStyle(
        fontFamily = BricolageGrotesqueFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = (15 * 1.3).sp,
        color = Ink
    )

    val interface13 = TextStyle(
        fontFamily = BricolageGrotesqueFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = (13 * 1.3).sp,
        color = Ink2
    )

    val interface11 = TextStyle(
        fontFamily = BricolageGrotesqueFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = (11 * 1.3).sp,
        color = Ink2
    )

    // Prose (Newsreader) - 1.55 line height
    val prose18 = TextStyle(
        fontFamily = NewsreaderFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = (18 * 1.55).sp,
        color = Ink
    )

    val prose15 = TextStyle(
        fontFamily = NewsreaderFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = (15 * 1.55).sp,
        color = Ink
    )

    val prose13 = TextStyle(
        fontFamily = NewsreaderFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = (13 * 1.55).sp,
        color = Ink2
    )

    // Measured (Martian Mono) - Dates, weights, hours, week numbers, percentages
    val measured24 = TextStyle(
        fontFamily = MartianMonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = (24 * 1.15).sp,
        color = Ink
    )

    val measured18 = TextStyle(
        fontFamily = MartianMonoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = (18 * 1.2).sp,
        color = Ink
    )

    val measured15 = TextStyle(
        fontFamily = MartianMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = (15 * 1.2).sp,
        color = Ink
    )

    val measured13 = TextStyle(
        fontFamily = MartianMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = (13 * 1.2).sp,
        color = Ink
    )

    val measured11 = TextStyle(
        fontFamily = MartianMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = (11 * 1.2).sp,
        color = Ink2
    )
}

val Typography = Typography(
    displayLarge = CollideType.display56,
    headlineLarge = CollideType.headline34,
    headlineMedium = CollideType.title24,
    titleMedium = CollideType.interface18,
    bodyLarge = CollideType.interface15,
    bodyMedium = CollideType.interface13,
    bodySmall = CollideType.interface11,
    labelLarge = CollideType.measured15,
    labelMedium = CollideType.measured13,
    labelSmall = CollideType.measured11
)
