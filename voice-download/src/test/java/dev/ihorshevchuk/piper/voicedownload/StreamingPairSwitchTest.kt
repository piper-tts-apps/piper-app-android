package dev.ihorshevchuk.piper.voicedownload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * [StreamingPairSwitch]: disabling renames pair parts out of the
 * engine's view, enabling renames them back, and nothing else in the
 * voices directory is touched.
 */
class StreamingPairSwitchTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun write(name: String) {
        File(temp.root, name).writeText("fake")
    }

    private fun names(): Set<String> =
        temp.root.listFiles()!!.map { it.name }.toSet()

    @Test
    fun disableRenamesBothPartsAndEnableRestoresThem() {
        write("en_US-amy-medium.onnx.json")
        write("en_US-amy-medium.encoder.onnx")
        write("en_US-amy-medium.decoder.onnx")
        write("uk_UA-lada-medium.onnx")

        val renamed = StreamingPairSwitch.setPairsEnabled(temp.root, enabled = false)
        assertEquals(2, renamed)
        assertEquals(
            setOf(
                "en_US-amy-medium.onnx.json",
                "en_US-amy-medium.encoder.onnx.disabled",
                "en_US-amy-medium.decoder.onnx.disabled",
                "uk_UA-lada-medium.onnx"
            ),
            names()
        )

        val restored = StreamingPairSwitch.setPairsEnabled(temp.root, enabled = true)
        assertEquals(2, restored)
        assertTrue(File(temp.root, "en_US-amy-medium.encoder.onnx").isFile)
        assertTrue(File(temp.root, "en_US-amy-medium.decoder.onnx").isFile)
    }

    @Test
    fun switchIsIdempotent() {
        write("en_US-amy-medium.encoder.onnx")
        write("en_US-amy-medium.decoder.onnx")
        assertEquals(
            2, StreamingPairSwitch.setPairsEnabled(temp.root, enabled = false)
        )
        assertEquals(
            0, StreamingPairSwitch.setPairsEnabled(temp.root, enabled = false)
        )
        assertEquals(
            2, StreamingPairSwitch.setPairsEnabled(temp.root, enabled = true)
        )
        assertEquals(
            0, StreamingPairSwitch.setPairsEnabled(temp.root, enabled = true)
        )
        assertFalse(
            File(temp.root, "en_US-amy-medium.encoder.onnx.disabled").exists()
        )
    }
}
