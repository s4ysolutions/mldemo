package solutions.s4y.mldemo.voice_transcription.service.dependencies

interface IMelVoiceTranscriber {
    /**
     * Transcribe waveforms to text
     * @param waveForms Array of -1..1 values > 1 sec
     * @return List of transcribed texts
     */
    fun getTokens(melSpectrogram: FloatArray): List<Int>
}