// by Claude
package com.lightningkite.kiteui.audio

import kotlin.math.sqrt

/**
 * Convert PCM16 (Int16 little-endian) byte array to Float32 normalized array.
 * Each pair of bytes becomes a float value from -1.0 to 1.0.
 */
public fun ByteArray.pcm16ToFloat32(): FloatArray {
    val samples = size / 2
    val result = FloatArray(samples)
    for (i in 0 until samples) {
        val low = this[i * 2].toInt() and 0xFF
        val high = this[i * 2 + 1].toInt()
        val sample = (high shl 8) or low
        result[i] = sample / 32768f
    }
    return result
}

/**
 * Convert Float32 normalized array (-1.0 to 1.0) to PCM16 byte array (little-endian).
 */
public fun FloatArray.float32ToPcm16(): ByteArray {
    val result = ByteArray(size * 2)
    for (i in indices) {
        val sample = (this[i].coerceIn(-1f, 1f) * 32767).toInt()
        result[i * 2] = (sample and 0xFF).toByte()
        result[i * 2 + 1] = (sample shr 8).toByte()
    }
    return result
}

/**
 * Calculate RMS (Root Mean Square) level from PCM16 data.
 * Returns a value from 0.0 to 1.0 representing audio amplitude.
 */
public fun ByteArray.calculatePcm16Level(): Float {
    if (size < 2) return 0f
    val samples = size / 2
    var sumSquares = 0.0
    for (i in 0 until samples) {
        val low = this[i * 2].toInt() and 0xFF
        val high = this[i * 2 + 1].toInt()
        val sample = (high shl 8) or low
        val normalized = sample / 32768.0
        sumSquares += normalized * normalized
    }
    val rms = sqrt(sumSquares / samples)
    return rms.toFloat().coerceIn(0f, 1f)
}

/**
 * Resample PCM16 audio from one sample rate to another using linear interpolation.
 * This is a simple resampling method suitable for voice audio.
 */
public fun ByteArray.resamplePcm16(fromRate: Int, toRate: Int): ByteArray {
    if (fromRate == toRate) return this

    val inputSamples = size / 2
    val outputSamples = (inputSamples.toLong() * toRate / fromRate).toInt()
    val result = ByteArray(outputSamples * 2)

    val ratio = fromRate.toDouble() / toRate

    for (i in 0 until outputSamples) {
        val srcIndex = i * ratio
        val srcIndexInt = srcIndex.toInt()
        val fraction = srcIndex - srcIndexInt

        // Read source samples
        val sample1 = if (srcIndexInt < inputSamples) {
            val low = this[srcIndexInt * 2].toInt() and 0xFF
            val high = this[srcIndexInt * 2 + 1].toInt()
            (high shl 8) or low
        } else 0

        val sample2 = if (srcIndexInt + 1 < inputSamples) {
            val low = this[(srcIndexInt + 1) * 2].toInt() and 0xFF
            val high = this[(srcIndexInt + 1) * 2 + 1].toInt()
            (high shl 8) or low
        } else sample1

        // Linear interpolation
        val interpolated = (sample1 + (sample2 - sample1) * fraction).toInt()

        result[i * 2] = (interpolated and 0xFF).toByte()
        result[i * 2 + 1] = (interpolated shr 8).toByte()
    }

    return result
}
