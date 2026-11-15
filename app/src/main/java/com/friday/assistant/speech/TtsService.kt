package com.friday.assistant.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thin wrapper around Android's on-device TextToSpeech engine.
 *
 * - speak() is safe to call before the engine finishes initializing; early
 *   requests are queued and flushed once the engine is ready.
 * - Each speak() call uses QUEUE_FLUSH so a new reply replaces the previous
 *   spoken text instead of stacking up.
 */
class TtsService(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private val ready = AtomicBoolean(false)
    private val pendingQueue = ArrayDeque<String>()

    /** Called with true when speech starts and false when it finishes, errors, or is stopped. */
    var onSpeakingChanged: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.w("FRIDAY", "TTS: init failed (status=$status)")
                return@TextToSpeech
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.i("FRIDAY", "TTS: utterance started")
                    onSpeakingChanged?.invoke(true)
                }

                override fun onDone(utteranceId: String?) {
                    Log.i("FRIDAY", "TTS: utterance done")
                    onSpeakingChanged?.invoke(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.w("FRIDAY", "TTS: utterance error")
                    onSpeakingChanged?.invoke(false)
                }
            })
            val result = tts?.setLanguage(Locale.getDefault())
            val usable = result == TextToSpeech.LANG_AVAILABLE ||
                result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            if (usable) {
                ready.set(true)
                Log.i("FRIDAY", "TTS: ready (lang=${Locale.getDefault()})")
                drainPending()
                return@TextToSpeech
            }
            Log.w("FRIDAY", "TTS: device language unavailable (code=$result), trying US fallback")
            val fallback = tts?.setLanguage(Locale.US)
            if (fallback != null && fallback >= 0) {
                ready.set(true)
                Log.i("FRIDAY", "TTS: ready (lang=en-US fallback)")
                drainPending()
            } else {
                Log.w("FRIDAY", "TTS: no usable voice data installed, staying silent")
            }
        }
    }

    fun isReady(): Boolean = ready.get()

    fun isSpeaking(): Boolean = ready.get() && runCatching { tts?.isSpeaking() }.getOrNull() == true

    fun speak(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        if (!ready.get()) {
            synchronized(pendingQueue) {
                if (pendingQueue.size < 4) pendingQueue.addLast(clean.take(500))
            }
            Log.i("FRIDAY", "TTS: queued ${clean.length} chars while engine initializes")
            return
        }
        speakNow(clean)
    }

    fun stop() {
        synchronized(pendingQueue) { pendingQueue.clear() }
        if (!ready.get()) return
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        synchronized(pendingQueue) { pendingQueue.clear() }
        runCatching { tts?.shutdown() }
        tts = null
        ready.set(false)
        Log.i("FRIDAY", "TTS: shutdown")
    }

    private fun drainPending() {
        val queued = synchronized(pendingQueue) {
            val copy = pendingQueue.toList()
            pendingQueue.clear()
            copy
        }
        queued.forEach { speakNow(it) }
    }

    private fun speakNow(text: String) {
        val engine = tts ?: return
        val result = runCatching {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "friday-${System.currentTimeMillis()}")
        }
        Log.i("FRIDAY", "TTS: speak ${text.length} chars queued=${result.isSuccess}")
    }
}
