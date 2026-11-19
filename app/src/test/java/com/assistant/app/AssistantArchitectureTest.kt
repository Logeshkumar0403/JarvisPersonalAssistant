package com.assistant.app

import com.assistant.app.models.AssistantResponse
import com.assistant.app.models.AssistantStatus
import com.assistant.app.models.MessageSender
import com.assistant.app.models.MessageStatus
import com.assistant.app.repository.AssistantRepository
import com.assistant.app.repository.DefaultVoiceRepository
import com.assistant.app.repository.PlaceholderAssistantRepository
import com.assistant.app.repository.VoiceRepository
import com.assistant.app.services.PlaceholderAiService
import com.assistant.app.viewmodels.AssistantViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests validating architecture, MVVM state flows, and seam boundaries.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AssistantArchitectureTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var aiService: PlaceholderAiService
    private lateinit var repository: AssistantRepository
    private lateinit var fakeSpeechService: FakeSpeechRecognitionService
    private lateinit var fakeTtsService: FakeTextToSpeechService
    private lateinit var fakeSettingsRepo: FakeVoiceSettingsRepository
    private lateinit var voiceRepository: VoiceRepository
    private lateinit var viewModel: AssistantViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        aiService = PlaceholderAiService()
        repository = PlaceholderAssistantRepository(aiService = aiService, messageDao = null)
        fakeSpeechService = FakeSpeechRecognitionService()
        fakeTtsService = FakeTextToSpeechService()
        fakeSettingsRepo = FakeVoiceSettingsRepository()

        voiceRepository = DefaultVoiceRepository(
            speechRecognitionService = fakeSpeechService,
            textToSpeechService = fakeTtsService,
            voiceSettingsRepository = fakeSettingsRepo,
            scope = testScope
        )

        viewModel = AssistantViewModel(
            repository = repository,
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
    fun testInitialUiStateIsIdle() {
        val state = viewModel.uiState.value
        assertEquals(AssistantStatus.IDLE, state.status)
        assertEquals("", state.inputText)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isListening)
        assertFalse(state.isProcessing)
        assertFalse(state.isError)
    }

    @Test
    fun testSeamReturnsHonestNotConnectedResponse() = runTest {
        val response = repository.sendUserMessage("What is the weather?")
        assertTrue("Expected AssistantResponse.NotConnected", response is AssistantResponse.NotConnected)

        val notConnected = response as AssistantResponse.NotConnected
        assertTrue(notConnected.reason.contains("not yet connected"))

        val messages = repository.messages.value
        assertEquals(2, messages.size)

        val userMessage = messages[0]
        assertEquals("What is the weather?", userMessage.text)
        assertEquals(MessageSender.USER, userMessage.sender)
        assertEquals(MessageStatus.SENT, userMessage.status)

        val assistantMessage = messages[1]
        assertEquals(MessageSender.ASSISTANT, assistantMessage.sender)
        assertEquals(MessageStatus.NOT_CONNECTED, assistantMessage.status)
    }

    @Test
    fun testListeningStateCanBeExercised() {
        viewModel.triggerTestListeningState()
        assertEquals(AssistantStatus.LISTENING, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.isListening)

        viewModel.resetStatusToIdle()
        assertEquals(AssistantStatus.IDLE, viewModel.uiState.value.status)
    }

    @Test
    fun testErrorStateCanBeExercised() {
        viewModel.triggerTestErrorState("Custom test error")
        assertEquals(AssistantStatus.ERROR, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.isError)
        assertEquals("Custom test error", viewModel.uiState.value.errorMessage)

        viewModel.resetStatusToIdle()
        assertEquals(AssistantStatus.IDLE, viewModel.uiState.value.status)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testRuntimePermissionDeniedShowsRationaleWhenFlagged() {
        viewModel.onPermissionResult(isGranted = false, shouldShowRationale = true)
        assertTrue(viewModel.uiState.value.showPermissionRationale)
        assertFalse(viewModel.uiState.value.showPermanentlyDeniedDialog)
    }

    @Test
    fun testRuntimePermissionPermanentlyDeniedShowsSettingsDialog() {
        viewModel.onPermissionResult(isGranted = false, shouldShowRationale = false)
        assertFalse(viewModel.uiState.value.showPermissionRationale)
        assertTrue(viewModel.uiState.value.showPermanentlyDeniedDialog)
    }
}
