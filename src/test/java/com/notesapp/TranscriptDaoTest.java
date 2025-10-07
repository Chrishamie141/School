package com.notesapp;

import com.notesapp.dao.RecordingDao;
import com.notesapp.dao.TranscriptDao;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class TranscriptDaoTest {

    @Test
    void upsertFetchUpdateAndCascadeDelete() throws Exception {
        // In-memory DB for isolation
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            // Enable foreign keys
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }

            // Create necessary tables
            RecordingDao.createTable(conn);
            TranscriptDao.createTable(conn);

            var rdao = new RecordingDao(conn);
            var tdao = new TranscriptDao(conn);

            // Insert a parent recording
            long rid = rdao.insert("JUnit Transcript Test", "/tmp/t.wav", 2_000L, System.currentTimeMillis());
            assertTrue(rid > 0, "Recording id should be generated");

            // First upsert
            int wrote = tdao.upsertByRecordingId(rid, "first transcript");
            assertTrue(wrote > 0, "Insert should affect at least 1 row");

            var fetched1 = tdao.findByRecordingId(rid);
            assertTrue(fetched1.isPresent(), "Transcript should exist after insert");
            assertEquals("first transcript", fetched1.get().getText(), "Persisted text should match");

            // Second upsert using legacy overload (int, String, String) to match older tests
            int wrote2 = tdao.upsertByRecordingId((int) rid, "updated transcript", "ignored");
            assertTrue(wrote2 > 0, "Upsert should update an existing row");

            var fetched2 = tdao.findByRecordingId(rid);
            assertTrue(fetched2.isPresent(), "Transcript should still exist after update");
            assertEquals("updated transcript", fetched2.get().getText(), "Updated text should match");
            assertNotNull(fetched2.get().getCreatedAt(), "created_at should be populated");

            // Delete parent and verify cascade removed transcript
            int deleted = rdao.deleteById(rid);
            assertEquals(1, deleted, "One recording should be deleted");
            assertTrue(tdao.findByRecordingId(rid).isEmpty(), "Transcript should be gone due to ON DELETE CASCADE");
        }
    }
}
