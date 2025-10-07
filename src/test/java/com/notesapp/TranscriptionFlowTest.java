package com.notesapp;

import com.notesapp.dao.NoteDao;
import com.notesapp.dao.RecordingDao;
import com.notesapp.dao.TranscriptDao;
import com.notesapp.transcription.TranscriptionManager;
import com.notesapp.transcription.TranscriptionService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class TranscriptionFlowTest {

    // Fake transcriber so tests don’t call external Whisper binary
    static class FakeTranscriber implements TranscriptionService {
        @Override
        public String transcribeAudio(Path audioFile) {
            assertNotNull(audioFile, "audioFile path should not be null");
            return "FAKE TRANSCRIPT for " + audioFile.getFileName();
        }
    }

    @Test
    void fullTranscriptionFlow() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }

            // Create schemas
            RecordingDao.createTable(conn);
            NoteDao.createTable(conn);
            TranscriptDao.createTable(conn);

            var rdao = new RecordingDao(conn);
            var ndao = new NoteDao(conn);
            var tdao = new TranscriptDao(conn);

            // Insert recording
            long rid = rdao.insert("JUnit Flow", "/tmp/flow.wav", 5000L, System.currentTimeMillis());
            assertTrue(rid > 0);

            // Run through manager with fake transcriber
            var manager = new TranscriptionManager(new FakeTranscriber(), ndao, tdao);
            String result = manager.transcribeAndStore(rid, Path.of("/tmp/flow.wav"));

            // Validate transcript stored
            var tr = tdao.findByRecordingId(rid);
            assertTrue(tr.isPresent(), "Transcript should be persisted");
            assertEquals(result, tr.get().getText());

            // Validate note auto-generated
            var note = ndao.findByRecordingId(rid);
            assertTrue(note.isPresent(), "Note should be auto-created");
            assertEquals("Auto-generated from transcription", note.get().getContent());

            // Cleanup cascade
            rdao.deleteById(rid);
            assertTrue(ndao.findByRecordingId(rid).isEmpty());
            assertTrue(tdao.findByRecordingId(rid).isEmpty());
        }
    }
}
