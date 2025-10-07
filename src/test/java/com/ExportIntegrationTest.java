package com.notesapp;

import com.notesapp.dao.NoteDao;
import com.notesapp.dao.RecordingDao;
import com.notesapp.dao.TranscriptDao;
import com.notesapp.export.PdfExporter;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class ExportIntegrationTest {

    @Test
    void exportPdf_containsSomething() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            RecordingDao.createTable(conn);
            NoteDao.createTable(conn);
            TranscriptDao.createTable(conn);

            var rdao = new RecordingDao(conn);
            var ndao = new NoteDao(conn);
            var tdao = new TranscriptDao(conn);

            long rid = rdao.insert("Export Title", "/tmp/a.wav", 0L, System.currentTimeMillis());
            ndao.upsertByRecordingId(rid, "Note body");
            tdao.upsertByRecordingId(rid, "Transcript body");

            // generate a temp file
            Path out = Files.createTempFile("notesapp-export-", ".pdf");
            out.toFile().deleteOnExit();

            // fetch content & export
            var note = ndao.findByRecordingId(rid).map(NoteDao.NoteRow::getContent).orElse("");
            var tr   = tdao.findByRecordingId(rid).map(TranscriptDao.TranscriptRow::getText).orElse("");
            PdfExporter.export("Export Title", note, tr, out);

            assertTrue(Files.exists(out), "PDF should be created");
            assertTrue(Files.size(out) > 100, "PDF should be non-empty");
        }
    }
}
