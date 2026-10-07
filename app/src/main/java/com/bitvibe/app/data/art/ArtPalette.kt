package com.bitvibe.app.data.art

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Picks an accent colour from album art, the way Poweramp tints its player to the cover.
 * Saturated, bright pixels count most; a mostly grey cover returns null (keep the app accent).
 */
object ArtPalette {
    fun accentFrom(bitmap: Bitmap): Int? {
        val small = Bitmap.createScaledBitmap(bitmap, SAMPLE, SAMPLE, true)
        val hsv = FloatArray(3)
        var x = 0.0
        var y = 0.0
        var weightSum = 0.0
        for (px in 0 until SAMPLE) {
            for (py in 0 until SAMPLE) {
                Color.colorToHSV(small.getPixel(px, py), hsv)
                val weight = (hsv[1] * hsv[2]).toDouble()
                if (weight < 0.05) continue
                // Average hue on the colour wheel so reds near 0°/360° don't cancel out.
                val angle = Math.toRadians(hsv[0].toDouble())
                x += cos(angle) * weight
                y += sin(angle) * weight
                weightSum += weight
            }
        }
        if (small != bitmap) small.recycle()
        if (weightSum < SAMPLE * SAMPLE * 0.08) return null
        val hue = ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
        // Fixed saturation/brightness keeps text readable on any cover.
        return Color.HSVToColor(floatArrayOf(hue, 0.65f, 0.95f))
    }

    private const val SAMPLE = 24
}
