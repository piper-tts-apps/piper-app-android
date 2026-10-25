package dev.ihorshevchuk.piper.voicedownload

import android.content.Context
import dev.ihorshevchuk.piper.tts.FileVoiceStore
import java.io.File

/**
 * The "Streaming voices" setting and the file switch it drives.
 *
 * Storage reality behind the setting: the synthesis engine decides
 * between split and single-file synthesis by file presence - an
 * encoder/decoder pair sitting next to the model path is loaded as a
 * pair, otherwise the model file itself is loaded. There is no runtime
 * flag, so the setting is applied to the files:
 *
 * - Downloads with the setting on store the pair INSTEAD of the model
 *   file (see [VoiceDownloadManager]).
 * - Turning the setting off renames every installed pair out of the
 *   engine's view (a ".disabled" suffix, [StreamingPairSwitch]);
 *   voices that also have a model file keep playing from it, and
 *   pair-only voices become unlisted until the setting is turned back
 *   on, which renames the parts back. No audio data is deleted either
 *   way, and no re-download is needed.
 * - Compact (INT8) voices never use a pair and are unaffected.
 */
class StreamingVoicesStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * True unless the user explicitly turned streaming off: an absent
     * preference means ON, and turning it back on removes the stored
     * value, so only opt-outs are ever persisted.
     */
    val enabled: Boolean
        get() = !prefs.contains(KEY_STREAMING_VOICES) ||
            prefs.getBoolean(KEY_STREAMING_VOICES, true)

    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            prefs.edit().remove(KEY_STREAMING_VOICES).apply()
        } else {
            prefs.edit().putBoolean(KEY_STREAMING_VOICES, false).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "piper_voice_format"
        /** SharedPreferences key for the setting. */
        const val KEY_STREAMING_VOICES = "streaming_voices"
    }
}

/**
 * Renames installed split pairs in and out of the engine's view (see
 * [StreamingVoicesStore]). Plain file operations on a directory, so the
 * switch stays JVM-testable.
 */
object StreamingPairSwitch {

    /** Suffix appended to pair parts while streaming is off. */
    const val DISABLED_SUFFIX = ".disabled"

    /**
     * Applies [enabled] to every split pair in [voicesDir]: disabling
     * renames `<name>.encoder.onnx` / `<name>.decoder.onnx` to the same
     * names plus [DISABLED_SUFFIX]; enabling renames them back.
     * Returns how many files were renamed. Idempotent.
     */
    fun setPairsEnabled(voicesDir: File, enabled: Boolean): Int {
        if (!voicesDir.isDirectory) return 0
        val files = voicesDir.listFiles { f -> f.isFile } ?: return 0
        var renamed = 0
        for (file in files) {
            val isPart = file.name.endsWith(FileVoiceStore.ENCODER_SUFFIX) ||
                file.name.endsWith(FileVoiceStore.DECODER_SUFFIX)
            val isDisabledPart = file.name.endsWith(
                FileVoiceStore.ENCODER_SUFFIX + DISABLED_SUFFIX
            ) || file.name.endsWith(
                FileVoiceStore.DECODER_SUFFIX + DISABLED_SUFFIX
            )
            when {
                !enabled && isPart -> {
                    if (file.renameTo(File(voicesDir, file.name + DISABLED_SUFFIX))) {
                        renamed++
                    }
                }
                enabled && isDisabledPart -> {
                    val restored = file.name.removeSuffix(DISABLED_SUFFIX)
                    if (file.renameTo(File(voicesDir, restored))) {
                        renamed++
                    }
                }
            }
        }
        return renamed
    }
}
