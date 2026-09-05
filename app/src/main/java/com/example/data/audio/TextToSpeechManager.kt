package com.example.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isSynthesizing: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val currentAudioPath: String? = null,
    val playbackSpeed: Float = 1.0f,
    val statusMessage: String = "Hazır"
)

class TextToSpeechManager(private val context: Context) {

    private val TAG = "TextToSpeechManager"
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackingJob: Job? = null
    private var onCompletionCallback: (() -> Unit)? = null

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                val trLocale = Locale.forLanguageTag("tr-TR")
                val result = tts?.setLanguage(trLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Turkish not supported, falling back to default locale")
                    tts?.language = Locale.getDefault()
                }
                tts?.setSpeechRate(1.05f)
                tts?.setPitch(1.0f)
            } else {
                Log.e(TAG, "TTS Initialization failed with code: $status")
            }
        }
    }

    /**
     * Synthesizes text to a .wav audio file in the app cache /audio_cache/
     */
    suspend fun synthesizeToFile(text: String, fileNamePrefix: String = "ses"): File? =
        suspendCancellableCoroutine { continuation ->
            val audioDir = File(context.cacheDir, "audio_cache").apply {
                if (!exists()) mkdirs()
            }
            val audioFile = File(audioDir, "${fileNamePrefix}_${System.currentTimeMillis()}.wav")

            if (!isTtsInitialized || tts == null) {
                // Return null if TTS is not yet ready, caller can still use direct fallback
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            _playbackState.update { it.copy(isSynthesizing = true, statusMessage = "Seslendirme hazırlanıyor...") }

            val utteranceId = "synth_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}

                override fun onDone(id: String?) {
                    if (id == utteranceId) {
                        _playbackState.update { it.copy(isSynthesizing = false, statusMessage = "Hazır") }
                        continuation.resume(audioFile)
                    }
                }

                override fun onError(id: String?) {
                    if (id == utteranceId) {
                        _playbackState.update { it.copy(isSynthesizing = false, statusMessage = "Seslendirme hatası") }
                        continuation.resume(null)
                    }
                }
            })

            val result = tts?.synthesizeToFile(text, params, audioFile, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                _playbackState.update { it.copy(isSynthesizing = false, statusMessage = "Seslendirme başlatılamadı") }
                continuation.resume(null)
            }
        }

    /**
     * Plays the audio from file or falls back to live TTS reading
     */
    fun playAudio(audioPath: String?, fallbackText: String? = null, onCompletion: (() -> Unit)? = null) {
        stop()
        onCompletionCallback = onCompletion

        val file = audioPath?.let { File(it) }
        if (file != null && file.exists() && file.length() > 0) {
            playFromFile(file.absolutePath)
        } else if (!fallbackText.isNullOrBlank()) {
            playFromLiveTts(fallbackText)
        }
    }

    private fun playFromFile(path: String) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                val currentSpeed = _playbackState.value.playbackSpeed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = PlaybackParams().setSpeed(currentSpeed)
                }
                val duration = duration
                start()

                _playbackState.update {
                    it.copy(
                        isPlaying = true,
                        isPaused = false,
                        durationMs = duration,
                        currentPositionMs = 0,
                        currentAudioPath = path,
                        statusMessage = "Öğretmen anlatıyor..."
                    )
                }

                setOnCompletionListener {
                    stop()
                    _playbackState.update { it.copy(statusMessage = "Anlatım tamamlandı") }
                    val cb = onCompletionCallback
                    onCompletionCallback = null
                    cb?.invoke()
                }

                setOnErrorListener { _, _, _ ->
                    stop()
                    _playbackState.update { it.copy(statusMessage = "Oynatma hatası") }
                    onCompletionCallback = null
                    true
                }
            }
            startTrackingProgress()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file: $path", e)
            _playbackState.update { it.copy(statusMessage = "Ses dosyası açılamadı") }
            onCompletionCallback = null
        }
    }

    private fun playFromLiveTts(text: String) {
        if (!isTtsInitialized || tts == null) {
            initTts()
        }

        val utteranceId = "live_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val estDuration = ((text.length / 15) * 1000).coerceAtLeast(4000)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                _playbackState.update {
                    it.copy(
                        isPlaying = true,
                        isPaused = false,
                        durationMs = estDuration,
                        currentPositionMs = 0,
                        statusMessage = "Öğretmen seslendiriyor..."
                    )
                }
                startLiveTracking(estDuration)
            }

            override fun onDone(id: String?) {
                stop()
                _playbackState.update { it.copy(statusMessage = "Anlatım tamamlandı") }
                val cb = onCompletionCallback
                onCompletionCallback = null
                cb?.invoke()
            }

            override fun onError(id: String?) {
                stop()
                _playbackState.update { it.copy(statusMessage = "Seslendirme hatası") }
                onCompletionCallback = null
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun togglePlayPause() {
        val state = _playbackState.value
        if (state.isPlaying) {
            pause()
        } else if (state.isPaused) {
            resume()
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            progressTrackingJob?.cancel()
            _playbackState.update { it.copy(isPlaying = false, isPaused = true, statusMessage = "Duraklatıldı") }
        } else if (tts?.isSpeaking == true) {
            tts?.stop()
            progressTrackingJob?.cancel()
            _playbackState.update { it.copy(isPlaying = false, isPaused = true, statusMessage = "Durduruldu") }
        }
    }

    fun resume() {
        if (mediaPlayer != null) {
            mediaPlayer?.start()
            _playbackState.update { it.copy(isPlaying = true, isPaused = false, statusMessage = "Öğretmen anlatıyor...") }
            startTrackingProgress()
        }
    }

    fun stop() {
        progressTrackingJob?.cancel()
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }

        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }

        _playbackState.update {
            it.copy(
                isPlaying = false,
                isPaused = false,
                currentPositionMs = 0,
                statusMessage = "Hazır"
            )
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let { player ->
            try {
                player.seekTo(positionMs)
                _playbackState.update { it.copy(currentPositionMs = positionMs) }
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking", e)
            }
        }
    }

    fun setSpeed(speed: Float) {
        _playbackState.update { it.copy(playbackSpeed = speed) }
        tts?.setSpeechRate(speed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer?.isPlaying == true) {
            try {
                mediaPlayer?.playbackParams = PlaybackParams().setSpeed(speed)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting playback speed", e)
            }
        }
    }

    private fun startTrackingProgress() {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val pos = mediaPlayer?.currentPosition ?: 0
                val dur = mediaPlayer?.duration ?: 1
                _playbackState.update {
                    it.copy(currentPositionMs = pos, durationMs = dur)
                }
                delay(200)
            }
        }
    }

    private fun startLiveTracking(estDuration: Int) {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch {
            var current = 0
            while (isActive && _playbackState.value.isPlaying && current < estDuration) {
                delay(200)
                current += 200
                _playbackState.update {
                    it.copy(currentPositionMs = current, durationMs = estDuration)
                }
            }
        }
    }

    fun release() {
        stop()
        try {
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
