package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioPlaybackState
import com.example.data.audio.TextToSpeechManager
import com.example.data.local.CoachDatabase
import com.example.data.model.CoachHistoryEntity
import com.example.data.model.CoachResponse
import com.example.data.model.LessonStatistics
import com.example.data.model.QuestionBatchItem
import com.example.data.model.QuestionStatus
import com.example.data.repository.CoachRepository
import com.example.data.util.LessonStatisticsCalculator
import com.example.ui.navigation.AppNavigationTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray

data class CoachUiState(
    val selectedTab: AppNavigationTab = AppNavigationTab.SOLVER,
    val isLoading: Boolean = false,
    val currentResponse: CoachResponse? = null,
    val activeHistoryEntity: CoachHistoryEntity? = null,
    val selectedImageUri: Uri? = null,
    val inputPrompt: String = "",
    val errorMessage: String? = null,
    val isAutoPlayEnabled: Boolean = true,
    val isStatisticsDialogOpen: Boolean = false,
    // Multi-Question Batch features
    val batchQuestions: List<QuestionBatchItem> = emptyList(),
    val selectedBatchIndex: Int = 0,
    val isBatchSolving: Boolean = false,
    val batchProgressText: String = "",
    val isContinuousPlaying: Boolean = false
)

class CoachViewModel(application: Application) : AndroidViewModel(application) {

    private val db = CoachDatabase.getInstance(application)
    val ttsManager = TextToSpeechManager(application)
    private val repository = CoachRepository(application, db.coachHistoryDao(), ttsManager)

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    val audioState: StateFlow<AudioPlaybackState> = ttsManager.playbackState

    val historyList: StateFlow<List<CoachHistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val statistics: StateFlow<LessonStatistics> = historyList
        .map { LessonStatisticsCalculator.calculate(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LessonStatistics()
        )

    fun openStatisticsDialog() {
        _uiState.update { it.copy(isStatisticsDialogOpen = true) }
    }

    fun closeStatisticsDialog() {
        _uiState.update { it.copy(isStatisticsDialogOpen = false) }
    }

    fun onPromptChanged(newPrompt: String) {
        _uiState.update { it.copy(inputPrompt = newPrompt, errorMessage = null) }
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri, errorMessage = null) }
    }

    fun clearSelectedImage() {
        _uiState.update { it.copy(selectedImageUri = null) }
    }

    fun applyQuickPrompt(promptText: String) {
        _uiState.update { it.copy(inputPrompt = promptText) }
    }

    fun startAnalysis(autoPlay: Boolean = false) {
        val prompt = _uiState.value.inputPrompt.trim()
        val imageUri = _uiState.value.selectedImageUri

        if (prompt.isBlank() && imageUri == null) {
            _uiState.update { it.copy(errorMessage = "Lütfen bir soru yazın veya görsel yükleyin.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        ttsManager.stop()

        viewModelScope.launch {
            val result = repository.analyze(prompt, imageUri)
            result.onSuccess { (response, historyEntity) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentResponse = response,
                        activeHistoryEntity = historyEntity,
                        errorMessage = null
                    )
                }

                // If user selected "Sesli Dinle" (autoPlay = true), play immediately!
                // If user selected "Çözdür" (autoPlay = false), keep audio ready in player without auto-playing.
                if (autoPlay && response.audioScript.isNotBlank()) {
                    ttsManager.playAudio(historyEntity.audioPath, response.audioScript)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Bir hata oluştu. Lütfen tekrar deneyin."
                    )
                }
            }
        }
    }

    fun loadHistoryItem(item: CoachHistoryEntity) {
        ttsManager.stop()

        val takeaways = mutableListOf<String>()
        try {
            val array = JSONArray(item.keyTakeawaysJson)
            for (i in 0 until array.length()) {
                takeaways.add(array.getString(i))
            }
        } catch (e: Exception) {
            // Ignore parsing fallback
        }

        val response = CoachResponse(
            type = item.type,
            title = item.title,
            displayText = item.displayText,
            audioScript = item.audioScript,
            keyTakeaways = takeaways
        )

        _uiState.update {
            it.copy(
                currentResponse = response,
                activeHistoryEntity = item,
                selectedImageUri = null,
                errorMessage = null
            )
        }

        // Play recorded audio file immediately (zero-latency playback as requested)
        ttsManager.playAudio(item.audioPath, item.audioScript)
    }

    fun resetToNewAnalysis() {
        ttsManager.stop()
        _uiState.update {
            it.copy(
                currentResponse = null,
                activeHistoryEntity = null,
                inputPrompt = "",
                selectedImageUri = null,
                errorMessage = null
            )
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
            if (_uiState.value.activeHistoryEntity?.id == id) {
                _uiState.update { it.copy(activeHistoryEntity = null, currentResponse = null) }
                ttsManager.stop()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            ttsManager.stop()
        }
    }

    fun switchTab(tab: AppNavigationTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // --- Multi-Question Batch Management ---

    fun addBatchQuestion(imageUri: Uri? = null, prompt: String = "") {
        _uiState.update { state ->
            val nextNumber = state.batchQuestions.size + 1
            val newItem = QuestionBatchItem(
                questionNumber = nextNumber,
                imageUri = imageUri,
                prompt = prompt
            )
            val updatedList = state.batchQuestions + newItem
            state.copy(
                batchQuestions = updatedList,
                selectedBatchIndex = updatedList.size - 1
            )
        }
    }

    fun addMultipleBatchQuestions(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _uiState.update { state ->
            var currentNum = state.batchQuestions.size
            val newItems = uris.map { uri ->
                currentNum++
                QuestionBatchItem(
                    questionNumber = currentNum,
                    imageUri = uri
                )
            }
            val updatedList = state.batchQuestions + newItems
            state.copy(
                batchQuestions = updatedList,
                selectedBatchIndex = if (state.batchQuestions.isEmpty()) 0 else state.selectedBatchIndex
            )
        }
    }

    fun updateBatchQuestionPrompt(id: String, newPrompt: String) {
        _uiState.update { state ->
            val updated = state.batchQuestions.map { item ->
                if (item.id == id) item.copy(prompt = newPrompt) else item
            }
            state.copy(batchQuestions = updated)
        }
    }

    fun updateBatchQuestionImage(id: String, newUri: Uri?) {
        _uiState.update { state ->
            val updated = state.batchQuestions.map { item ->
                if (item.id == id) item.copy(imageUri = newUri) else item
            }
            state.copy(batchQuestions = updated)
        }
    }

    fun removeBatchQuestion(id: String) {
        _uiState.update { state ->
            val remaining = state.batchQuestions.filter { it.id != id }
            val renumbered = remaining.mapIndexed { index, item ->
                item.copy(questionNumber = index + 1)
            }
            val newIndex = state.selectedBatchIndex.coerceAtMost((renumbered.size - 1).coerceAtLeast(0))
            state.copy(
                batchQuestions = renumbered,
                selectedBatchIndex = newIndex
            )
        }
    }

    fun clearBatch() {
        ttsManager.stop()
        _uiState.update {
            it.copy(
                batchQuestions = emptyList(),
                selectedBatchIndex = 0,
                isBatchSolving = false,
                batchProgressText = "",
                isContinuousPlaying = false
            )
        }
    }

    fun selectBatchQuestion(index: Int) {
        if (index in _uiState.value.batchQuestions.indices) {
            _uiState.update { it.copy(selectedBatchIndex = index) }
            val item = _uiState.value.batchQuestions[index]
            if (item.status == QuestionStatus.COMPLETED && _uiState.value.isAutoPlayEnabled) {
                ttsManager.playAudio(item.historyEntity?.audioPath, item.response?.audioScript)
            }
        }
    }

    fun nextBatchQuestion() {
        val nextIdx = _uiState.value.selectedBatchIndex + 1
        if (nextIdx < _uiState.value.batchQuestions.size) {
            selectBatchQuestion(nextIdx)
        }
    }

    fun previousBatchQuestion() {
        val prevIdx = _uiState.value.selectedBatchIndex - 1
        if (prevIdx >= 0) {
            selectBatchQuestion(prevIdx)
        }
    }

    fun solveAllBatchQuestions(autoPlayAudio: Boolean = false) {
        val questions = _uiState.value.batchQuestions
        if (questions.isEmpty() || _uiState.value.isBatchSolving) return

        _uiState.update {
            it.copy(
                isBatchSolving = true,
                batchProgressText = "Sorular çözülmeye başlanıyor..."
            )
        }

        viewModelScope.launch {
            val total = questions.size
            for ((index, item) in questions.withIndex()) {
                // If already completed, skip
                if (item.status == QuestionStatus.COMPLETED && item.response != null) {
                    continue
                }

                _uiState.update { state ->
                    state.copy(
                        batchProgressText = "Soru ${index + 1} / $total çözülüyor...",
                        batchQuestions = state.batchQuestions.map {
                            if (it.id == item.id) it.copy(status = QuestionStatus.PROCESSING, errorMessage = null) else it
                        }
                    )
                }

                val prompt = item.prompt.trim()
                val imageUri = item.imageUri

                val result = repository.analyze(prompt, imageUri)
                result.onSuccess { (response, historyEntity) ->
                    _uiState.update { state ->
                        state.copy(
                            batchQuestions = state.batchQuestions.map {
                                if (it.id == item.id) {
                                    it.copy(
                                        status = QuestionStatus.COMPLETED,
                                        response = response,
                                        historyEntity = historyEntity,
                                        errorMessage = null
                                    )
                                } else it
                            }
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            batchQuestions = state.batchQuestions.map {
                                if (it.id == item.id) {
                                    it.copy(
                                        status = QuestionStatus.ERROR,
                                        errorMessage = error.localizedMessage ?: "Çözüm sırasında hata oluştu"
                                    )
                                } else it
                            }
                        )
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isBatchSolving = false,
                    batchProgressText = "Tüm sorular hazır!"
                )
            }

            // Auto-play the currently selected or first completed question if requested
            if (autoPlayAudio) {
                val currentIdx = _uiState.value.selectedBatchIndex
                val itemToPlay = _uiState.value.batchQuestions.getOrNull(currentIdx)
                if (itemToPlay?.status == QuestionStatus.COMPLETED) {
                    ttsManager.playAudio(itemToPlay.historyEntity?.audioPath, itemToPlay.response?.audioScript)
                }
            }
        }
    }

    fun playBatchQuestionAudio(index: Int, continuous: Boolean = false) {
        val state = _uiState.value
        if (index !in state.batchQuestions.indices) return

        val item = state.batchQuestions[index]
        if (item.status != QuestionStatus.COMPLETED) return

        _uiState.update { it.copy(selectedBatchIndex = index, isContinuousPlaying = continuous) }

        ttsManager.playAudio(
            audioPath = item.historyEntity?.audioPath,
            fallbackText = item.response?.audioScript,
            onCompletion = {
                if (_uiState.value.isContinuousPlaying) {
                    val nextIdx = index + 1
                    if (nextIdx < _uiState.value.batchQuestions.size &&
                        _uiState.value.batchQuestions[nextIdx].status == QuestionStatus.COMPLETED
                    ) {
                        playBatchQuestionAudio(nextIdx, continuous = true)
                    } else {
                        _uiState.update { it.copy(isContinuousPlaying = false) }
                    }
                }
            }
        )
    }

    fun toggleContinuousBatchAudio() {
        val state = _uiState.value
        if (state.isContinuousPlaying) {
            ttsManager.stop()
            _uiState.update { it.copy(isContinuousPlaying = false) }
        } else {
            playBatchQuestionAudio(state.selectedBatchIndex, continuous = true)
        }
    }

    fun toggleAutoPlay() {
        _uiState.update { it.copy(isAutoPlayEnabled = !it.isAutoPlayEnabled) }
    }

    fun playActiveAudio() {
        val state = _uiState.value
        val audioPath = state.activeHistoryEntity?.audioPath
        val script = state.currentResponse?.audioScript
        ttsManager.playAudio(audioPath, script)
    }

    fun togglePlayPause() = ttsManager.togglePlayPause()

    fun pauseAudio() = ttsManager.pause()

    fun stopAudio() = ttsManager.stop()

    fun seekAudio(positionMs: Int) = ttsManager.seekTo(positionMs)

    fun setPlaybackSpeed(speed: Float) = ttsManager.setSpeed(speed)

    override fun onCleared() {
        super.onCleared()
        ttsManager.release()
    }
}
