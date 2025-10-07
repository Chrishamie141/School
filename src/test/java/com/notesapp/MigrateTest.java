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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class MigrateTest {

    /** Fake transcriber that matches the new interface signature. */
    static class FakeTranscriber implements TranscriptionService {
        @Override
        public String transcribeAudio(Path audioPath) throws Exception {
            assertNotNull(audioPath, "audioPath should not be null");
            return "FAKE TRANSCRIPT for " + audioPath.getFileName();
        }
    }

    @Test
    void migrateAndTranscribeFlow() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }

            // Simulate pre-migration schemas (minimal), then "migrate" by calling createTable()
            // For this project we just ensure the modern tables/columns exist.
            RecordingDao.createTable(conn);
            NoteDao.createTable(conn);
            TranscriptDao.createTable(conn);

            var rdao = new RecordingDao(conn);
            var ndao = new NoteDao(conn);
            var tdao = new TranscriptDao(conn);

            // Insert a recording
            long rid = rdao.insert("Migration Test", "/tmp/migrate.wav", 0L, System.currentTimeMillis());
            assertTrue(rid > 0, "recording id should be > 0");

            // Run the manager with a fake transcriber (no external binary)
            var manager = new TranscriptionManager(new FakeTranscriber(), ndao, tdao);
            String transcript = manager.transcribeAndStore(rid, Path.of("/tmp/migrate.wav"));
            assertTrue(transcript.startsWith("FAKE TRANSCRIPT"), "should be fake text");

            // Verify transcript persisted (supports 'text' column)
            var tr = tdao.findByRecordingId(rid);
            assertTrue(tr.isPresent(), "transcript row should exist");
            assertEquals(transcript, tr.get().getText(), "stored transcript should match");

            // Verify note auto-created
            var note = ndao.findByRecordingId(rid);
            assertTrue(note.isPresent(), "note row should exist");
            assertEquals("Auto-generated from transcription", note.get().getContent());

            // Sanity check via raw SQL: columns exist
            assertTrue(hasColumn(conn, "transcripts", "text"));
            assertTrue(hasColumn(conn, "notes", "content"));

            // Cascade delete still works
            int deleted = rdao.deleteById(rid);
            assertEquals(1, deleted);
            assertTrue(ndao.findByRecordingId(rid).isEmpty());
            assertTrue(tdao.findByRecordingId(rid).isEmpty());
        }
    }

    private static boolean hasColumn(Connection conn, String table, String column) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(" + table + ")")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (column.equalsIgnoreCase(rs.getString("name"))) return true;
                }
            }
        }
        return false;
    }
}
