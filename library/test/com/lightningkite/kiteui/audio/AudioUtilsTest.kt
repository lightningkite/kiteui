// by Claude
package com.lightningkite.kiteui.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioUtilsTest {

    @Test
    fun testPcm16ToFloat32() {
        // Test silence (zero)
        val silence = byteArrayOf(0, 0, 0, 0)
        val floatSilence = silence.pcm16ToFloat32()
        assertEquals(2, floatSilence.size)
        assertEquals(0f, floatSilence[0], 0.001f)
        assertEquals(0f, floatSilence[1], 0.001f)

        // Test max positive value (32767 in little-endian: 0xFF, 0x7F)
        val maxPositive = byteArrayOf(0xFF.toByte(), 0x7F.toByte())
        val floatMax = maxPositive.pcm16ToFloat32()
        assertEquals(1, floatMax.size)
        assertTrue(floatMax[0] > 0.99f && floatMax[0] <= 1f)

        // Test max negative value (-32768 in little-endian: 0x00, 0x80)
        val maxNegative = byteArrayOf(0x00.toByte(), 0x80.toByte())
        val floatMin = maxNegative.pcm16ToFloat32()
        assertEquals(1, floatMin.size)
        assertEquals(-1f, floatMin[0], 0.001f)
    }

    @Test
    fun testFloat32ToPcm16() {
        // Test silence
        val silence = floatArrayOf(0f, 0f)
        val pcmSilence = silence.float32ToPcm16()
        assertEquals(4, pcmSilence.size)
        assertEquals(0, pcmSilence[0].toInt())
        assertEquals(0, pcmSilence[1].toInt())

        // Test max positive
        val maxFloat = floatArrayOf(1f)
        val pcmMax = maxFloat.float32ToPcm16()
        assertEquals(2, pcmMax.size)
        // Should be close to 0xFF, 0x7F (32767 in little-endian)
        val reconstructed = (pcmMax[1].toInt() shl 8) or (pcmMax[0].toInt() and 0xFF)
        assertTrue(reconstructed > 32760)

        // Test max negative
        val minFloat = floatArrayOf(-1f)
        val pcmMin = minFloat.float32ToPcm16()
        val reconstructedMin = (pcmMin[1].toInt() shl 8) or (pcmMin[0].toInt() and 0xFF)
        assertTrue(reconstructedMin < -32760)
    }

    @Test
    fun testRoundTrip() {
        // Test that conversion round-trips with minimal loss
        val original = floatArrayOf(0.5f, -0.5f, 0.25f, -0.25f, 0f)
        val pcm = original.float32ToPcm16()
        val restored = pcm.pcm16ToFloat32()

        assertEquals(original.size, restored.size)
        for (i in original.indices) {
            // Allow small error due to quantization
            assertEquals(original[i], restored[i], 0.001f)
        }
    }

    @Test
    fun testCalculatePcm16Level() {
        // Test silence
        val silence = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        assertEquals(0f, silence.calculatePcm16Level(), 0.001f)

        // Test max signal (all samples at max positive)
        val maxSignal = ByteArray(100)
        for (i in 0 until 50) {
            maxSignal[i * 2] = 0xFF.toByte()
            maxSignal[i * 2 + 1] = 0x7F.toByte()
        }
        val level = maxSignal.calculatePcm16Level()
        assertTrue(level > 0.99f)

        // Test empty array
        assertEquals(0f, ByteArray(0).calculatePcm16Level())
        assertEquals(0f, ByteArray(1).calculatePcm16Level())
    }

    @Test
    fun testAudioFormat() {
        val format = AudioFormat(sampleRate = 24000, channels = 1, bitsPerSample = 16)

        assertEquals(2, format.bytesPerSample)
        assertEquals(48000, format.bytesPerSecond)

        // 48000 bytes = 1000ms at 48000 bytes/sec
        assertEquals(1000L, format.bytesToMs(48000))

        // 1000ms = 48000 bytes
        assertEquals(48000, format.msToBytes(1000))
    }

    @Test
    fun testResamplePcm16_sameRate() {
        val original = byteArrayOf(0x00, 0x10, 0x00, 0x20, 0x00, 0x30)
        val resampled = original.resamplePcm16(24000, 24000)
        assertTrue(original.contentEquals(resampled))
    }

    @Test
    fun testResamplePcm16_downsample() {
        // Create 4 samples at 48000Hz
        val original = byteArrayOf(
            0x00, 0x10,  // sample 0
            0x00, 0x20,  // sample 1
            0x00, 0x30,  // sample 2
            0x00, 0x40   // sample 3
        )
        // Downsample to 24000Hz should give approximately 2 samples
        val resampled = original.resamplePcm16(48000, 24000)
        assertEquals(4, resampled.size)  // 2 samples * 2 bytes
    }
}
