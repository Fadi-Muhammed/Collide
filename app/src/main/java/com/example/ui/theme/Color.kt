package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Chrome (achromatic). The entire interface lives here.
val Ink = Color(0xFF12161A)       // Primary text, active controls
val Ink2 = Color(0xFF5A646E)      // Secondary text, labels
val Ink3 = Color(0xFF8D97A1)      // Disabled, placeholder
val Rule = Color(0xFFD2D7DC)      // Hairlines, 1px only
val Paper = Color(0xFFFBFCFD)     // Card and panel surfaces
val Plate = Color(0xFFEDEFF2)     // App background

// Load ramp (chromatic). Reserved. Encodes pressure and nothing else.
val Calm = Color(0xFF3F7E92)      // Below normal (< 0.60)
val Steady = Color(0xFF6E9270)    // Normal week (0.60 – 0.89)
val Busy = Color(0xFFC4A02C)      // Full, no slack (0.90 – 1.09)
val Collision = Color(0xFFC0472C) // Over capacity (1.10 – 1.39)
val Critical = Color(0xFF7C2030)  // Not survivable as planned (≥ 1.40)

/**
 * Maps a numerical pressure ratio to its designated stepped chromatic token.
 */
fun pressureToColor(pressure: Double): Color = when {
    pressure < 0.60 -> Calm
    pressure < 0.90 -> Steady
    pressure < 1.10 -> Busy
    pressure < 1.40 -> Collision
    else -> Critical
}

/**
 * Returns the formal name of the pressure band for scannability and accessibility.
 */
fun pressureToBandName(pressure: Double): String = when {
    pressure < 0.60 -> "calm"
    pressure < 0.90 -> "steady"
    pressure < 1.10 -> "busy"
    pressure < 1.40 -> "collision"
    else -> "critical"
}
