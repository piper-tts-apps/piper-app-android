package dev.ihorshevchuk.piper.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Split voices in [FileVoiceStore]: a complete encoder/decoder pair
 * with no model file is an installed voice; the parts themselves, and
 * incomplete pairs, are not.
 */
class SplitVoiceStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun write(name: String) {
        File(temp.root, name).writeText("fake")
    }

    @Test
    fun pairOnlyVoiceIsListedAsSplitOnly() {
        write("en_US-amy-medium.encoder.onnx")
        write("en_US-amy-medium.decoder.onnx")
        write("en_US-amy-medium.onnx.json")

        val voices = FileVoiceStore(temp.root).listVoices()
        assertEquals(1, voices.size)
        val amy = voices.single()
        assertEquals("en_US-amy-medium", amy.name)
        assertTrue(amy.splitOnly)
        // The model path stays the stem anchor the engine derives the
        // pair names from, even though the file itself is absent.
        assertEquals("en_US-amy-medium.onnx", amy.modelFile.name)
        assertFalse(amy.modelFile.isFile)
        assertEquals(
            "en_US-amy-medium.onnx.json", amy.configFile?.name
        )
    }

    @Test
    fun pairPartsAreNeverVoicesThemselves() {
        write("en_US-amy-medium.onnx")
        write("en_US-amy-medium.encoder.onnx")
        write("en_US-amy-medium.decoder.onnx")

        val voices = FileVoiceStore(temp.root).listVoices()
        assertEquals(listOf("en_US-amy-medium"), voices.map { it.name })
        assertFalse(voices.single().splitOnly)
    }

    @Test
    fun incompletePairIsNotAVoice() {
        write("en_US-amy-medium.encoder.onnx")
        assertTrue(FileVoiceStore(temp.root).listVoices().isEmpty())
        assertNull(FileVoiceStore(temp.root).findVoice("en_US-amy-medium"))
    }

    @Test
    fun disabledPairIsNotAVoice() {
        // The streaming setting parks pairs under a ".disabled" suffix;
        // parked parts must not surface as a playable voice.
        write("en_US-amy-medium.encoder.onnx.disabled")
        write("en_US-amy-medium.decoder.onnx.disabled")
        assertTrue(FileVoiceStore(temp.root).listVoices().isEmpty())
    }

    @Test
    fun completePairCheck() {
        assertFalse(
            FileVoiceStore.hasCompleteSplitPair(temp.root, "en_US-amy-medium")
        )
        write("en_US-amy-medium.encoder.onnx")
        assertFalse(
            FileVoiceStore.hasCompleteSplitPair(temp.root, "en_US-amy-medium")
        )
        write("en_US-amy-medium.decoder.onnx")
        assertTrue(
            FileVoiceStore.hasCompleteSplitPair(temp.root, "en_US-amy-medium")
        )
    }
}
