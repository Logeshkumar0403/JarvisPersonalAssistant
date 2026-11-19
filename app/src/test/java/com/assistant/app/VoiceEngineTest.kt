package com.assistant.app

import com.assistant.app.models.AssistantStatus
import com.assistant.app.models.VoiceError
import com.assistant.app.models.VoiceLanguage
import com.assistant.app.models.VoiceSettings
import com.assistant.app.repository.AssistantRepository
import com.assistant.app.repository.DefaultVoiceRepository
import com.assistant.app.repository.PlaceholderAssistantRepository
import com.assistant.app.repository.VoiceRepository
import com.assistant.app.repository.VoiceSettingsRepository
import com.assistant.app.services.PlaceholderAiService
import com.assistant.app.services.SpeechRecognitionService
import com.assistant.app.services.TextToSpeechService
import com.assistant.app.viewmodels.AssistantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests validating Phase 2 Voice Engine:
 * - Voice state transitions (Idle, Listening, Processing, Speaking, Error)
 * - Recognition result handling & insertion into chat input
 * - Recognition cancellation
 * - TTS initialization, speak, and stop controls
 * - Mutual exclusion (starting recognition stops TTS; starting TTS stops recognition)
 * - Voice settings persistence and modifications
 * - Runtime permission denial handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeSpeechService: FakeSpeechRecognitionService
    private lateinit var fakeTtsService: FakeTextToSpeechService
    private lateinit var fakeSettingsRepo: FakeVoiceSettingsRepository
    private lateinit var voiceRepository: VoiceRepository
    private lateinit var assistantRepository: AssistantRepository
    private lateinit var viewModel: AssistantViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeSpeechService = FakeSpeechRecognitionService()
        fakeTtsService = FakeTextToSpeechService()
        fakeSettingsRepo = FakeVoiceSettingsRepository()

        voiceRepository = DefaultVoiceRepository(
            speechRecognitionService = fakeSpeechService,
            textToSpeechService = fakeTtsService,
            voiceSettingsRepository = fakeSettingsRepo,
            scope = testScope
        )

        assistantRepository = PlaceholderAssistantRepository(
            aiService = PlaceholderAiService(),
            messageDao = null
        )

        viewModel = AssistantViewModel(
            repository = assistantRepository,
            voiceRepository = voiceRepository,
            voiceSettingsRepository = fakeSettingsRepo
        )

        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialVoiceStateIsIdle() {
        assertEquals(AssistantStatus.IDLE, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isListening)
        assertFalse(viewModel.uiState.value.isSpeaking)
        assertNull(viewModel.uiState.value.currentlySpeakingMessageId)
    }

    @Test
    fun testStartListeningTransitionsToListeningState() {
        viewModel.onPermissionAlreadyGranted()
        viewModel.onMicClicked()

        assertEquals(AssistantStatus.LISTENING, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.isListening)
        assertTrue(fakeSpeechService.isListening.value)
    }

    @Test
    fun testStopListeningTransitionsToIdle() {
        viewModel.onPermissionAlreadyGranted()
        viewModel.onMicClicked() // starts
        assertTrue(viewModel.uiState.value.isListening)

        viewModel.onMicClicked() // stops
        assertEquals(AssistantStatus.IDLE, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isListening)
    }

    @Test
    fun testRecognitionResultInsertedIntoInputWithoutAutoSend() {
        viewModel.onPermissionAlreadyGranted()
        viewModel.onMicClicked()

        fakeSpeechService.emitResult("Hello in Tamil வணக்கம்")

        // Text must be inserted into input text for user review and editing
        assertEquals("Hello in Tamil வணக்கம்", viewModel.uiState.value.inputText)
        // Must return to IDLE after recognition
        assertEquals(AssistantStatus.IDLE, viewModel.uiState.value.status)
        // Message must NOT be auto-sent
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun testRecognitionAppendsToExistingInputText() {
        viewModel.onInputTextChanged("Existing query")
        viewModel.onPermissionAlreadyGranted()
        viewModel.onMicClicked()

        fakeSpeechService.emitResult("with voice addition")

        assertEquals("Existing query with voice addition", viewModel.uiState.value.inputText)
    }

    @Test
    fun testRecognitionCancellation() {
        viewModel.onPermissionAlreadyGranted()
        viewModel.onMicClicked()
        assertTrue(viewModel.uiState.value.isListening)

        voiceRepository.cancelListening()
        assertTrue(fakeSpeechService.cancelCalled)
        assertEquals(AssistantStatus.IDLE, voiceRepository.voiceStatus.value)
    }

    @Test
    fun testTtsSpeakAndStop() {
        viewModel.speakMessage("msg_1", "Hello from assistant")

        assertEquals(AssistantStatus.SPEAKING, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.isSpeaking)
        assertEquals("msg_1", viewModel.uiState.value.currentlySpeakingMessageId)

        // Calling stop
        viewModel.stopPlayback()
        assertEquals(AssistantStatus.IDLE, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isSpeaking)
        assertNull(viewModel.uiState.value.currentlySpeakingMessageId)
    }

    @Test
    fun testTtsToggleOnSameMessageStopsPlayback() {
        viewModel.speakMessage("msg_1", "Hello from assistant")
        assertTrue(viewModel.uiState.value.isSpeaking)

        // Tapping the same message speaker button again toggles playback off
        viewModel.speakMessage("msg_1", "Hello from assistant")
        assertEquals(AssistantStatus.IDLE, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isSpeaking)
    }

    @Test
    fun testMutualExclusion_StartingMicStopsTts() {
        viewModel.speakMessage("msg_1", "Assistant talking")
        assertTrue(viewModel.uiState.value.isSpeaking)

        viewModel.onPermissionAlreadyGranted()
        viewModel.onMicClicked()

        // TTS should be stopped and listening started
        assertFalse(fakeTtsService.isSpeaking.value)
        assertTrue(fakeSpeechService.isListening.value)
        assertEquals(AssistantStatus.LISTENING, viewModel.uiState.value.status)
    }

    @Test
    fun testMutualExclusion_SpeakingStopsListening() {
        viewModel.onPermissionAlreadyGranted()
        viewModel.onMicClicked()
        assertTrue(fakeSpeechService.isListening.value)

        viewModel.speakMessage("msg_2", "Speaking message")

        // Listening should be cancelled and speaking started
        assertTrue(fakeSpeechService.cancelCalled)
        assertFalse(fakeSpeechService.isListening.value)
        assertTrue(fakeTtsService.isSpeaking.value)
        assertEquals(AssistantStatus.SPEAKING, viewModel.uiState.value.status)
    }

    @Test
    fun testAutoVoiceResponseSpeaksAssistantResponseOnly() = runTest {
        viewModel.setAutoVoiceResponse(true)
        assertTrue(viewModel.uiState.value.voiceSettings.autoVoiceResponse)

        viewModel.onInputTextChanged("Hello assistant")
        viewModel.sendMessage()

        // Advance simulated network / processing delay (700ms)
        testScheduler.advanceTimeBy(1000)

        // TTS should have been called for assistant response
        assertTrue(fakeTtsService.speakCalled)
        assertTrue(fakeTtsService.lastSpokenText.contains("not yet connected"))
    }

    @Test
    fun testVoiceSettingsPersistence() {
        viewModel.setAutoVoiceResponse(true)
        assertEquals(true, viewModel.uiState.value.voiceSettings.autoVoiceResponse)

        viewModel.setSpeechLanguage(VoiceLanguage.TAMIL)
        assertEquals(VoiceLanguage.TAMIL, viewModel.uiState.value.voiceSettings.language)

        viewModel.setSpeechRate(1.5f)
        assertEquals(1.5f, viewModel.uiState.value.voiceSettings.speechRate)

        viewModel.setSpeechPitch(0.8f)
        assertEquals(0.8f, viewModel.uiState.value.voiceSettings.pitch)
    }

    @Test
    fun testAppBackgroundingPausesVoiceWork() {
        viewModel.onPermissionAlreadyGranted()
        viewModel.onMicClicked()
        assertTrue(viewModel.uiState.value.isListening)

        viewModel.onAppBackgrounded()

        assertTrue(fakeSpeechService.cancelCalled)
        assertEquals(AssistantStatus.IDLE, viewModel.uiState.value.status)
    }

    @Test
    fun testSpeechRecognitionErrorHandling() {
        viewModel.onPermissionAlreadyGranted()
        viewModel.onMicClicked()

        fakeSpeechService.emitError(VoiceError.NoSpeechDetected)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AssistantStatus.ERROR, viewModel.uiState.value.status)
        assertEquals(VoiceError.NoSpeechDetected.message, viewModel.uiState.value.errorMessage)

        viewModel.resetStatusToIdle()
        assertEquals(AssistantStatus.IDLE, viewModel.uiState.value.status)
    }
}

// --- Test Fakes ---

class FakeSpeechRecognitionService : SpeechRecognitionService {
    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    var cancelCalled = false
    var stopCalled = false
    var lastLanguage: VoiceLanguage? = null
    private var resultCallback: ((String) -> Unit)? = null
    private var errorCallback: ((VoiceError) -> Unit)? = null

    override fun isAvailable(): Boolean = true

    override fun startListening(
        language: VoiceLanguage,
        onResult: (String) -> Unit,
        onPartialResult: ((String) -> Unit)?,
        onError: (VoiceError) -> Unit
    ) {
        lastLanguage = language
        resultCallback = onResult
        errorCallback = onError
        cancelCalled = false
        stopCalled = false
        _isListening.value = true
    }

    override fun stopListening() {
        stopCalled = true
        _isListening.value = false
    }

    override fun cancelListening() {
        cancelCalled = true
        _isListening.value = false
    }

    override fun destroy() {
        _isListening.value = false
    }

    fun emitResult(text: String) {
        _isListening.value = false
        resultCallback?.invoke(text)
    }

    fun emitError(error: VoiceError) {
        _isListening.value = false
        errorCallback?.invoke(error)
    }
}

class FakeTextToSpeechService : TextToSpeechService {
    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentlySpeakingMessageId = MutableStateFlow<String?>(null)
    override val currentlySpeakingMessageId: StateFlow<String?> = _currentlySpeakingMessageId.asStateFlow()

    var speakCalled = false
    var lastSpokenText: String = ""
    var lastMessageId: String = ""

    override fun isAvailable(): Boolean = true

    override fun speak(
        text: String,
        messageId: String,
        language: VoiceLanguage,
        pitch: Float,
        speechRate: Float,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (VoiceError) -> Unit
    ) {
        speakCalled = true
        lastSpokenText = text
        lastMessageId = messageId
        _isSpeaking.value = true
        _currentlySpeakingMessageId.value = messageId
        onStart()
    }

    override fun stop() {
        _isSpeaking.value = false
        _currentlySpeakingMessageId.value = null
    }

    override fun shutdown() {
        _isSpeaking.value = false
        _currentlySpeakingMessageId.value = null
    }
}

class FakeVoiceSettingsRepository : VoiceSettingsRepository {
    private val _settings = MutableStateFlow(VoiceSettings())
    override val settings: StateFlow<VoiceSettings> = _settings.asStateFlow()

    override fun setAutoVoiceResponse(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoVoiceResponse = enabled)
    }

    override fun setLanguage(language: VoiceLanguage) {
        _settings.value = _settings.value.copy(language = language)
    }

    override fun setSpeechRate(rate: Float) {
        _settings.value = _settings.value.copy(speechRate = rate)
    }

    override fun setPitch(pitch: Float) {
        _settings.value = _settings.value.copy(pitch = pitch)
    }
}
