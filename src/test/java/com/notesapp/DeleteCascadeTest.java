package com.notesapp;

import com.notesapp.dao.NoteDao;
import com.notesapp.dao.RecordingDao;
import com.notesapp.dao.TranscriptDao;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class DeleteCascadeTest {

    @Test
    void deletingRecordingCascadesToChildren() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            // Enable foreign keys
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }

            // Create all tables
            RecordingDao.createTable(conn);
            NoteDao.createTable(conn);
            TranscriptDao.createTable(conn);

            var rdao = new RecordingDao(conn);
            var ndao = new NoteDao(conn);
            var tdao = new TranscriptDao(conn);

            // Insert parent recording
            long rid = rdao.insert("Cascade Test", "/tmp/cascade.wav", 2000L, System.currentTimeMillis());
            assertTrue(rid > 0);

            // Insert child note + transcript
            ndao.upsertByRecordingId(rid, "cascade note");
            tdao.upsertByRecordingId(rid, "cascade transcript");

            // Confirm both exist
            assertTrue(ndao.findByRecordingId(rid).isPresent(), "Note should exist");
            assertTrue(tdao.findByRecordingId(rid).isPresent(), "Transcript should exist");

            // Delete parent
            int deleted = rdao.deleteById(rid);
            assertEquals(1, deleted, "Parent recording should be deleted");

            // Children should be gone due to cascade
            assertTrue(ndao.findByRecordingId(rid).isEmpty(), "Note should be deleted with parent");
            assertTrue(tdao.findByRecordingId(rid).isEmpty(), "Transcript should be deleted with parent");
        }
    }
}
