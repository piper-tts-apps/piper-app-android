package dev.ihorshevchuk.piper.tts

import java.io.File
import java.util.Locale

/**
 * A Piper voice available to the TTS service: `<name>.onnx` plus its
 * optional `<name>.onnx.json` config (null configPath means the native
 * layer uses modelPath + ".json").
 *
 * A split voice has NO `<name>.onnx`: [modelFile] is then the path the
 * model file would occupy, kept as the stem anchor - the native layer
 * derives `<name>.encoder.onnx` / `<name>.decoder.onnx` from it and
 * loads the pair instead. [splitOnly] marks that storage form.
 */
data class VoiceInfo(
    val name: String,
    val locale: Locale,
    val modelFile: File,
    val configFile: File?,
    val splitOnly: Boolean = false
)

/**
 * Source of installed voices.
 *
 * The download manager + voice catalog UI (a later step) fill the backing
 * directory; the service only reads through this interface, so the catalog
 * can later be swapped for a richer implementation without touching the
 * service.
 */
interface VoiceStore {
    /** All installed voices, sorted by name. Empty when none are installed. */
    fun listVoices(): List<VoiceInfo>

    /** Voice with exactly [name], or null. */
    fun findVoice(name: String): VoiceInfo?
}

/**
 * [VoiceStore] backed by a plain directory of `<name>.onnx` files
 * (e.g. `<files>/voices/`), each optionally accompanied by
 * `<name>.onnx.json`.
 *
 * Split voices are stored as an encoder/decoder pair
 * (`<name>.encoder.onnx` + `<name>.decoder.onnx`) with no model file;
 * the pair parts themselves are never voices. A voice is listed when
 * its model file is present, or when a complete pair is present
 * (an incomplete pair is a partial download, not a voice).
 *
 * Takes a [java.io.File] instead of a Context so it stays JVM-testable.
 */
class FileVoiceStore(private val voicesDir: File) : VoiceStore {

    override fun listVoices(): List<VoiceInfo> {
        if (!voicesDir.isDirectory) return emptyList()
        val files = voicesDir.listFiles { f -> f.isFile } ?: return emptyList()
        val names = sortedSetOf<String>()
        for (f in files) {
            when {
                f.name.endsWith(ENCODER_SUFFIX) ||
                    f.name.endsWith(DECODER_SUFFIX) -> {
                    // A pair part: the voice counts only when the pair
                    // is complete (checked below); never a voice itself.
                    val name = f.name.removeSuffix(ENCODER_SUFFIX)
                        .removeSuffix(DECODER_SUFFIX)
                    if (hasCompleteSplitPair(voicesDir, name)) names.add(name)
                }
                f.name.endsWith(".onnx") ->
                    names.add(f.name.removeSuffix(".onnx"))
            }
        }
        return names.mapNotNull { name ->
            val locale = parseLocale(name) ?: return@mapNotNull null
            val modelFile = File(voicesDir, "$name.onnx")
            val config = File(voicesDir, "$name.onnx.json").takeIf { it.isFile }
            VoiceInfo(
                name, locale, modelFile, config,
                splitOnly = !modelFile.isFile
            )
        }
    }

    override fun findVoice(name: String): VoiceInfo? =
        listVoices().find { it.name == name }

    companion object {
        /**
         * Name of the voices directory under the app's filesDir, shared by
         * the TTS service, the settings UI and the voice download manager.
         * Additive constant - existing call sites keep their literals.
         */
        const val VOICES_DIR_NAME = "voices"

        /** File suffix of a split voice's encoder part. */
        const val ENCODER_SUFFIX = ".encoder.onnx"

        /** File suffix of a split voice's decoder part. */
        const val DECODER_SUFFIX = ".decoder.onnx"

        /** Encoder file name for voice [name], e.g. "en_US-amy-medium.encoder.onnx". */
        fun encoderFileName(name: String): String = "$name$ENCODER_SUFFIX"

        /** Decoder file name for voice [name], e.g. "en_US-amy-medium.decoder.onnx". */
        fun decoderFileName(name: String): String = "$name$DECODER_SUFFIX"

        /** True when both split parts of voice [name] are present in [dir]. */
        fun hasCompleteSplitPair(dir: File, name: String): Boolean =
            File(dir, encoderFileName(name)).isFile &&
                File(dir, decoderFileName(name)).isFile

        /**
         * Parses the locale prefix of a Piper voice name:
         * "en_US-lessac-medium" -> Locale("en", "US").
         * Null when the name has no recognizable `ll_CC-` prefix.
         */
        fun parseLocale(voiceName: String): Locale? {
            val tag = voiceName.substringBefore('-')
            val parts = tag.split('_')
            if (parts.size < 2) return null
            val lang = LanguageNormalizer.normalizeLanguage(parts[0])
            if (lang.length != 2) return null
            val country = LanguageNormalizer.normalizeCountry(parts[1])
            return Locale(lang, country)
        }
    }
}
