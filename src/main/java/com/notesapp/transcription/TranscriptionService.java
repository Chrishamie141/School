package com.notesapp.transcription;

import java.nio.file.Path;

/** Contract for transcription implementations. */
public interface TranscriptionService {
    /**
     * Transcribe an audio file and return the raw text.
     * 
     * @param audioPath path to audio file (e.g. .wav)
     * @return transcript text (or fallback message if unavailable)
     * @throws Exception if process fails unexpectedly
     */
    String transcribeAudio(Path audioPath) throws Exception;
}
