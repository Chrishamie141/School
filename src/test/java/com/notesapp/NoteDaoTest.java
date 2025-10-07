package com.notesapp;

import com.notesapp.dao.NoteDao;
import com.notesapp.dao.RecordingDao;
import com.notesapp.dao.TranscriptDao;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class NoteDaoTest {

    @Test
    void upsertAndFetchNote() throws Exception {
        // Use an in-memory DB so tests are isolated and fast
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            // Enable foreign keys
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }

            // Ensure all tables exist (Recording first, then children)
            RecordingDao.createTable(conn);
            NoteDao.createTable(conn);
            // Not required for this test, but harmless:
            TranscriptDao.createTable(conn);

            var rdao = new RecordingDao(conn);
            var ndao = new NoteDao(conn);

            // Insert a parent recording
            long rid = rdao.insert("JUnit Note Test", "/tmp/note.wav", 1_000L, System.currentTimeMillis());
            assertTrue(rid > 0, "Recording id should be generated");

            // First upsert
            int wrote = ndao.upsertByRecordingId(rid, "hello note");
            assertTrue(wrote > 0, "Insert should affect at least 1 row");

            var fetched1 = ndao.findByRecordingId(rid);
            assertTrue(fetched1.isPresent(), "Note should exist after insert");
            assertEquals("hello note", fetched1.get().getContent());

            // Second upsert (exercise overload used in older tests: (int, String, String))
            int wrote2 = ndao.upsertByRecordingId((int) rid, "updated note", "ignored-extra-param");
            assertTrue(wrote2 > 0, "Upsert should update an existing row");

            var fetched2 = ndao.findByRecordingId(rid);
            assertTrue(fetched2.isPresent(), "Note should still exist after update");
            assertEquals("updated note", fetched2.get().getContent());

            // Sanity: delete by parent and ensure cascade removes child
            int deleted = rdao.deleteById(rid);
            assertEquals(1, deleted, "One recording should be deleted");
            assertTrue(ndao.findByRecordingId(rid).isEmpty(), "Note should be gone due to ON DELETE CASCADE");
        }
    }
}
