package com.agribicara.app.domain.usecase

import com.agribicara.app.core.common.Constants
import com.agribicara.app.domain.model.SpeechEvent
import com.agribicara.app.domain.model.TtsStatus
import com.agribicara.app.domain.repository.SpeechRecognizerRepository
import com.agribicara.app.domain.repository.TextToSpeechRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case suara, sengaja tipis - sama seperti [GetForecastUseCase].
 *
 * Nilainya ada pada batasnya: Fase 5 nanti memakai ulang [SpeakTextUseCase]
 * untuk membacakan jawaban Gemini tanpa perlu tahu apa pun soal TextToSpeech.
 */
class ListenForSpeechUseCase @Inject constructor(
    private val speechRecognizer: SpeechRecognizerRepository,
) {
    /**
     * Bahasa di-default ke Indonesia di sini, bukan di pemanggil, supaya tidak
     * ada layar yang tidak sengaja mendengarkan dalam bahasa lain.
     */
    operator fun invoke(
        languageTag: String = Constants.SPEECH_LANGUAGE_TAG,
    ): Flow<SpeechEvent> = speechRecognizer.listen(languageTag)
}

class SpeakTextUseCase @Inject constructor(
    private val textToSpeech: TextToSpeechRepository,
) {
    suspend operator fun invoke(text: String) = textToSpeech.speak(text)
}

class PrepareTextToSpeechUseCase @Inject constructor(
    private val textToSpeech: TextToSpeechRepository,
) {
    suspend operator fun invoke(): TtsStatus = textToSpeech.prepare()
}

class StopSpeakingUseCase @Inject constructor(
    private val textToSpeech: TextToSpeechRepository,
) {
    operator fun invoke() = textToSpeech.stop()
}

class IsSpeechRecognitionAvailableUseCase @Inject constructor(
    private val speechRecognizer: SpeechRecognizerRepository,
) {
    operator fun invoke(): Boolean = speechRecognizer.isAvailable()
}
