package com.notesapp.service;

import com.notesapp.dao.TranscriptDao;
import com.notesapp.transcription.TranscriptionService;

import java.nio.file.Path;
import java.sql.SQLException;

/**
 * Orchestrates "transcribe -> persist" for a given recording.
 */
public class TranscriptionRunner {

    private final TranscriptionService service;
    private final TranscriptDao transcriptDao;

    public TranscriptionRunner(TranscriptionService service, TranscriptDao transcriptDao) {
        this.service = service;
        this.transcriptDao = transcriptDao;
    }

    /**
     * Transcribes the audio and upserts the transcript for the given recording id.
     * Returns the text that was saved.
     */
    public String transcribeAndSave(long recordingId, Path audioPath) throws Exception {
        String text = service.transcribeAudio(audioPath);
        // Be tolerant of nulls
        if (text == null) text = "";
        upsertTranscript(recordingId, text);
        return text;
    }

    private void upsertTranscript(long recordingId, String text) throws SQLException {
        // Use the same overloads your tests expect
        transcriptDao.upsertByRecordingId(recordingId, text);
    }
}
