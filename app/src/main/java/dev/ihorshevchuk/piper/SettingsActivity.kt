package dev.ihorshevchuk.piper

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import dev.ihorshevchuk.piper.tts.FileVoiceStore
import dev.ihorshevchuk.piper.voicedownload.StreamingPairSwitch
import dev.ihorshevchuk.piper.voicedownload.StreamingVoicesStore
import dev.ihorshevchuk.piper.voicedownload.VoiceFormat
import dev.ihorshevchuk.piper.voicedownload.VoiceFormatStore
import java.io.File

/**
 * Settings screen (it replaces the old About screen): the Voice
 * format picker on TOP, then Import Model from Files, then Credits &
 * Legal, engine status, app version and feedback, plus the Voice
 * Licenses entry.
 *
 * The engine status row shows whether Piper is the system's selected
 * TTS engine, with a button to the system TTS settings when it is not.
 */
class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.settings)

        val pad = dp(16)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        addVoiceFormatSection(root)
        addImportSection(root)

        root.addView(TextView(this).apply {
            text = getString(R.string.credits_and_legal)
            setTypeface(typeface, Typeface.BOLD)
            textSize = 18f
            setPadding(0, dp(16), 0, dp(8))
        })
        for ((titleRes, descRes, url) in CREDITS) {
            addCredit(root,
                getString(titleRes), getString(descRes), url)
        }

        root.addView(Button(this).apply {
            text = getString(R.string.voice_licenses)
            setOnClickListener {
                startActivity(Intent(this@SettingsActivity, VoiceLicensesActivity::class.java))
            }
            setPadding(0, dp(16), 0, 0)
        })

        root.addView(TextView(this).apply {
            val active = isPiperDefaultTts()
            text = getString(R.string.tts_engine_status) + ": " +
                getString(if (active) R.string.tts_status_active
                else R.string.tts_status_inactive)
            setPadding(0, dp(16), 0, 0)
        })
        if (!isPiperDefaultTts()) {
            root.addView(Button(this).apply {
                text = getString(R.string.open_tts_settings)
                setOnClickListener { openTtsSettings() }
            })
        }

        root.addView(TextView(this).apply {
            @Suppress("DEPRECATION")
            val appVersion =
                packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
            text = getString(R.string.app_version) + ": " + appVersion
            setPadding(0, dp(16), 0, 0)
        })

        root.addView(Button(this).apply {
            text = getString(R.string.share_feedback)
            setOnClickListener { openUrl(FEEDBACK_URL) }
            setPadding(0, dp(8), 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    /**
     * Voice format section (the first section): a
     * dropdown with Standard (FP16) / Compact (INT8), persisted
     * immediately, and the footer explaining it applies to new
     * downloads only. Voice lists read the format when they bind, so
     * sizes follow the new setting as soon as the user returns to them.
     */
    private fun addVoiceFormatSection(root: LinearLayout) {
        val store = VoiceFormatStore(this)
        root.addView(TextView(this).apply {
            text = getString(R.string.voice_format)
            setTypeface(typeface, Typeface.BOLD)
            textSize = 18f
            setPadding(0, 0, 0, dp(8))
        })
        val formats = listOf(VoiceFormat.STANDARD, VoiceFormat.COMPACT)
        val labels = listOf(
            getString(R.string.voice_format_standard),
            getString(R.string.voice_format_compact)
        )
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity,
                android.R.layout.simple_spinner_item, labels).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(formats.indexOf(store.current))
            contentDescription = getString(R.string.voice_format)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?,
                    position: Int, id: Long
                ) {
                    store.current = formats[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        root.addView(spinner)
        root.addView(TextView(this).apply {
            text = getString(R.string.voice_format_description)
            textSize = 14f
            setPadding(0, dp(4), 0, 0)
        })
        addStreamingSection(root)
    }

    /**
     * Streaming voices switch, directly under the Voice format
     * section: on unless the user opted out. The engine picks split
     * vs single-file synthesis by file presence, so flipping the
     * switch also renames the installed split pairs in or out of the
     * engine's view (see StreamingPairSwitch); the rename runs off
     * the main thread and takes effect on the next voice load.
     */
    private fun addStreamingSection(root: LinearLayout) {
        val store = StreamingVoicesStore(this)
        root.addView(Switch(this).apply {
            text = getString(R.string.streaming_voices)
            textSize = 16f
            isChecked = store.enabled
            setPadding(0, dp(12), 0, 0)
            setOnCheckedChangeListener { _, checked ->
                store.setEnabled(checked)
                val voicesDir = File(filesDir, FileVoiceStore.VOICES_DIR_NAME)
                Thread {
                    StreamingPairSwitch.setPairsEnabled(voicesDir, checked)
                }.apply { isDaemon = true; start() }
            }
        })
        root.addView(TextView(this).apply {
            text = getString(R.string.streaming_voices_description)
            textSize = 14f
            setPadding(0, dp(4), 0, 0)
        })
    }

    /** Import Model from Files lives in Settings. */
    private fun addImportSection(root: LinearLayout) {
        root.addView(Button(this).apply {
            text = getString(R.string.update_model_in_app)
            contentDescription = getString(R.string.update_model_in_app)
            setOnClickListener {
                startActivity(Intent(this@SettingsActivity, ImportVoiceActivity::class.java))
            }
            setPadding(0, dp(16), 0, 0)
        })
    }

    private fun addCredit(root: LinearLayout, title: String, description: String, url: String) {
        root.addView(TextView(this).apply {
            text = title
            setTypeface(typeface, Typeface.BOLD)
            textSize = 16f
            setPadding(0, dp(8), 0, 0)
        })
        root.addView(TextView(this).apply {
            text = description
            setPadding(0, 0, 0, dp(2))
        })
        root.addView(Button(this).apply {
            text = getString(R.string.open_in_browser, title)
            contentDescription = "$title: $url"
            setOnClickListener { openUrl(url) }
        })
    }

    private fun isPiperDefaultTts(): Boolean =
        try {
            Settings.Secure.getString(contentResolver, "tts_default_synth")
                ?.contains(packageName) == true
        } catch (e: Exception) {
            Log.w(TAG, "reading default TTS engine failed", e)
            false
        }

    private fun openTtsSettings() {
        try {
            startActivity(Intent("com.android.settings.TTS_SETTINGS"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.tts_settings_unavailable,
                Toast.LENGTH_LONG).show()
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.open_in_browser, url),
                Toast.LENGTH_LONG).show()
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "PiperSettings"
        private const val FEEDBACK_URL =
            "mailto:piper-feedback@ihor-shevchuk.dev?subject=Piper%20feedback"

        private val CREDITS = listOf(
            Triple(R.string.license_piper_title,
                R.string.license_piper_description,
                "https://github.com/OHF-Voice/piper1-gpl"),
            Triple(R.string.license_espeak_title,
                R.string.license_espeak_description,
                "https://github.com/espeak-ng/espeak-ng-spm"),
            Triple(R.string.license_app_title,
                R.string.license_app_description,
                "https://github.com/piper-tts-apps/piper-app-android"),
        )
    }
}
