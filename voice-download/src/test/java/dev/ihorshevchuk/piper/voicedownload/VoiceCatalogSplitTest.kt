package dev.ihorshevchuk.piper.voicedownload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Split (`split` field) parsing + format semantics.
 *
 * Fixture mirrors the real catalog schema (verified 2026-10-24 against
 * IhorShevchuk/piper1-voices-fp16-quantized voices.json on main, where
 * the split pilot lives: 44 voices carry a top-level `split` object
 * with encoder/decoder parts, each with model / size_bytes /
 * md5_digest).
 */
class VoiceCatalogSplitTest {

    private val fixture = """
        {
          "en_US-amy-medium": {
            "key": "en_US-amy-medium",
            "name": "amy",
            "language": {
              "code": "en_US",
              "name_native": "English",
              "name_english": "English",
              "country_english": "United States"
            },
            "quality": "medium",
            "num_speakers": 1,
            "files": {
              "en/en_US/amy/medium/en_US-amy-medium.onnx": {
                "size_bytes": 31951473,
                "md5_digest": "eeb478518a46c9d416eff9721eb5ee20"
              },
              "en/en_US/amy/medium/en_US-amy-medium.onnx.json": {
                "size_bytes": 4882,
                "md5_digest": "7f37dadb26340c90ebc8088e0b252310"
              }
            },
            "split": {
              "encoder": {
                "model": "en/en_US/amy/medium/en_US-amy-medium.encoder.onnx",
                "size_bytes": 28607357,
                "md5_digest": "ef0bd939b74ffa724e7fabead124d33f"
              },
              "decoder": {
                "model": "en/en_US/amy/medium/en_US-amy-medium.decoder.onnx",
                "size_bytes": 3344372,
                "md5_digest": "f8e863ab556237c0a9b3767d201e77c5"
              }
            },
            "variants": {
              "int8": {
                "model": "en/en_US/amy/medium/en_US-amy-medium.int8.onnx",
                "size_bytes": 18681610
              }
            }
          },
          "en_US-lessac-medium": {
            "key": "en_US-lessac-medium",
            "name": "lessac",
            "language": {"code": "en_US", "name_english": "English"},
            "quality": "medium",
            "num_speakers": 1,
            "files": {
              "en/en_US/lessac/medium/en_US-lessac-medium.onnx": {
                "size_bytes": 63201294,
                "md5_digest": "2fc642b535197b6305c7c8f92dc8b24f"
              },
              "en/en_US/lessac/medium/en_US-lessac-medium.onnx.json": {
                "size_bytes": 4885,
                "md5_digest": "c1f2b7bddefe113f3255ff9ef234cfd3"
              }
            },
            "split": {
              "encoder": {
                "model": "en/en_US/lessac/medium/en_US-lessac-medium.encoder.onnx",
                "size_bytes": 100
              }
            }
          }
        }
    """.trimIndent()

    private val source = VoiceCatalogSource(
        catalogUrl = "https://example.invalid/voices.json",
        filesBaseUrl = "https://example.invalid/resolve/main/"
    )

    private fun amy(): VoiceCatalogEntry =
        VoiceCatalogParser.parse(fixture, source)
            .first { it.key == "en_US-amy-medium" }

    @Test
    fun `parses split encoder and decoder with urls sizes and digests`() {
        val split = amy().split
        assertTrue(split != null)
        assertEquals(
            "https://example.invalid/resolve/main/" +
                "en/en_US/amy/medium/en_US-amy-medium.encoder.onnx",
            split!!.encoder.modelUrl
        )
        assertEquals(28607357L, split.encoder.sizeBytes)
        assertEquals("ef0bd939b74ffa724e7fabead124d33f", split.encoder.md5Digest)
        assertEquals(3344372L, split.decoder.sizeBytes)
        assertEquals("f8e863ab556237c0a9b3767d201e77c5", split.decoder.md5Digest)
        assertEquals(28607357L + 3344372L, split.totalSizeBytes)
    }

    @Test
    fun `split applies to standard format only never compact`() {
        val entry = amy()
        assertTrue(entry.streamingSplit(VoiceFormat.STANDARD) != null)
        // The catalog's split describes the standard model; the INT8
        // build has no split form and must stay single-file.
        assertNull(entry.streamingSplit(VoiceFormat.COMPACT))
        assertTrue(entry.isCompactSelected(VoiceFormat.COMPACT))
    }

    @Test
    fun `streaming total size counts parts plus config not the full model`() {
        val entry = amy()
        assertEquals(
            28607357L + 3344372L + 4882L,
            entry.effectiveTotalSizeBytes(VoiceFormat.STANDARD, streaming = true)
        )
        // Streaming off, or Compact: the classic single-file totals.
        assertEquals(
            31951473L + 4882L,
            entry.effectiveTotalSizeBytes(VoiceFormat.STANDARD, streaming = false)
        )
        assertEquals(
            18681610L + 4882L,
            entry.effectiveTotalSizeBytes(VoiceFormat.COMPACT, streaming = true)
        )
    }

    @Test
    fun `incomplete split parses as no split and entry stays usable`() {
        val lessac = VoiceCatalogParser.parse(fixture, source)
            .first { it.key == "en_US-lessac-medium" }
        assertNull(lessac.split)
        assertNull(lessac.streamingSplit(VoiceFormat.STANDARD))
        assertEquals(63201294L, lessac.onnxSizeBytes)
    }

    @Test
    fun `split file names sit next to the model file`() {
        val entry = amy()
        assertEquals("en_US-amy-medium.encoder.onnx", entry.encoderFileName)
        assertEquals("en_US-amy-medium.decoder.onnx", entry.decoderFileName)
    }
}
