package com.notesapp;

import com.notesapp.dao.NoteDao;
import com.notesapp.dao.RecordingDao;
import com.notesapp.dao.TranscriptDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that exercise NoteDao and TranscriptDao against
 * a real in-memory SQLite database with foreign keys enabled.
 */
public class DaoIntegrationTest {

    private Connection conn;
    private RecordingDao rdao;
    private NoteDao ndao;
    private TranscriptDao tdao;

    @BeforeEach
    void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }

        // Create schemas in parent -> child order
        RecordingDao.createTable(conn);
        NoteDao.createTable(conn);
        TranscriptDao.createTable(conn);

        rdao = new RecordingDao(conn);
        ndao = new NoteDao(conn);
        tdao = new TranscriptDao(conn);
    }

    @AfterEach
    void tearDown() throws Exception {
        conn.close();
    }

    @Test
    void note_upsert_read_update_then_cascade_delete() throws Exception {
        long rid = rdao.insert("Note Int Test", "/tmp/note.wav", 1234L, System.currentTimeMillis());
        assertTrue(rid > 0, "recording id should be generated");

        // insert
        int wrote = ndao.upsertByRecordingId(rid, "first note");
        assertTrue(wrote > 0, "insert should affect at least 1 row");

        var row1 = ndao.findByRecordingId(rid);
        assertTrue(row1.isPresent(), "note should exist after insert");
        assertEquals("first note", row1.get().getContent());

        // update via (int, String, String) overload (matches some legacy tests)
        int wrote2 = ndao.upsertByRecordingId((int) rid, "updated note", "ignored");
        assertTrue(wrote2 > 0, "update should affect at least 1 row");

        var row2 = ndao.findByRecordingId(rid);
        assertTrue(row2.isPresent(), "note should still exist after update");
        assertEquals("updated note", row2.get().getContent());

        // cascade delete
        int deleted = rdao.deleteById(rid);
        assertEquals(1, deleted, "parent row should be deleted");
        assertTrue(ndao.findByRecordingId(rid).isEmpty(), "note should be cascade-deleted");
    }

    @Test
    void transcript_upsert_read_update_then_cascade_delete() throws Exception {
        long rid = rdao.insert("Transcript Int Test", "/tmp/tr.wav", 555L, System.currentTimeMillis());
        assertTrue(rid > 0, "recording id should be generated");

        // insert (int, String) overload
        int wrote = tdao.upsertByRecordingId((int) rid, "hello world");
        assertTrue(wrote > 0, "insert should affect at least 1 row");

        var tr1 = tdao.findByRecordingId(rid);
        assertTrue(tr1.isPresent(), "transcript should exist after insert");
        assertEquals("hello world", tr1.get().getText());

        // update via canonical long overload
        int wrote2 = tdao.upsertByRecordingId(rid, "updated transcript");
        assertTrue(wrote2 > 0, "update should affect at least 1 row");

        var tr2 = tdao.findByRecordingId(rid);
        assertTrue(tr2.isPresent(), "transcript should still exist after update");
        assertEquals("updated transcript", tr2.get().getText());

        // cascade delete
        int deleted = rdao.deleteById(rid);
        assertEquals(1, deleted, "parent row should be deleted");
        assertTrue(tdao.findByRecordingId(rid).isEmpty(), "transcript should be cascade-deleted");
    }

    @Test
    void note_and_transcript_can_coexist_for_same_recording() throws Exception {
        long rid = rdao.insert("Both Int Test", "/tmp/both.wav", 42L, System.currentTimeMillis());
        assertTrue(rid > 0);

        ndao.upsertByRecordingId(rid, "note body");
        tdao.upsertByRecordingId(rid, "transcript body");

        var note = ndao.findByRecordingId(rid);
        var tr   = tdao.findByRecordingId(rid);

        assertTrue(note.isPresent(), "note should exist");
        assertTrue(tr.isPresent(),   "transcript should exist");
        assertEquals("note body", note.get().getContent());
        assertEquals("transcript body", tr.get().getText());
    }
}
