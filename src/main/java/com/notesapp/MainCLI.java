package com.notesapp;

import com.notesapp.dao.NoteDao;
import com.notesapp.dao.RecordingDao;
import com.notesapp.dao.TranscriptDao;
import com.notesapp.export.PdfExporter;
import com.notesapp.transcription.TranscriptionManager;
import com.notesapp.transcription.TranscriptionService;
import com.notesapp.transcription.WhisperCppTranscriber;

import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainCLI {

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                printUsage();
                return;
            }

            Files.createDirectories(Paths.get("data"));
            String cmd = args[0].toLowerCase();

            switch (cmd) {

                /* ---------- DB setup ---------- */
                case "initdb", "migrate" -> {
                    try (Connection conn = open()) { bootstrap(conn); }
                    System.out.println("Database initialized or migrated.");
                }

                /* ---------- List recordings ---------- */
                case "list" -> {
                    try (Connection conn = open();
                         PreparedStatement ps = conn.prepareStatement(
                                 "SELECT id, title, duration_ms, audio_path, tag FROM recordings ORDER BY id"
                         );
                         ResultSet rs = ps.executeQuery()) {

                        System.out.printf("%-4s | %-25s | %-10s | %-30s | %-15s%n",
                                "ID", "Title", "Duration", "Audio Path", "Tag");
                        System.out.println("=".repeat(90));
                        while (rs.next()) {
                            System.out.printf("%-4d | %-25s | %-10d | %-30s | %-15s%n",
                                    rs.getLong("id"),
                                    nullToEmpty(rs.getString("title")),
                                    rs.getLong("duration_ms"),
                                    nullToEmpty(rs.getString("audio_path")),
                                    nullToEmpty(rs.getString("tag")));
                        }
                    }
                }

                /* ---------- Show single recording ---------- */
                case "show" -> {
                    if (args.length < 2) {
                        System.out.println("Usage: show <recordingId>");
                        return;
                    }
                    long rid = Long.parseLong(args[1]);
                    try (Connection conn = open()) {
                        displayRecording(conn, rid);
                    }
                }

                /* ---------- Delete recording ---------- */
                case "delete" -> {
                    if (args.length < 2) {
                        System.out.println("Usage: delete <recordingId>");
                        return;
                    }
                    long rid = Long.parseLong(args[1]);
                    try (Connection conn = open()) {
                        var rdao = new RecordingDao(conn);
                        int n = rdao.deleteById(rid);
                        System.out.println(n > 0 ? "Deleted." : "Nothing deleted.");
                    }
                }

                /* ---------- Export PDF ---------- */
                case "export" -> {
                    if (args.length < 3) {
                        System.out.println("Usage: export <recordingId> <out.pdf>");
                        return;
                    }
                    long rid = Long.parseLong(args[1]);
                    Path out = Paths.get(args[2]);

                    try (Connection conn = open()) {
                        bootstrap(conn);

                        String title = getString(conn,
                                "SELECT title FROM recordings WHERE id=?", rid);
                        String tag = getString(conn,
                                "SELECT tag FROM recordings WHERE id=?", rid);
                        String note = getString(conn,
                                "SELECT content FROM notes WHERE recording_id=?", rid);
                        String transcript = getString(conn,
                                "SELECT " + resolveTranscriptBodyColumn(conn) +
                                        " FROM transcripts WHERE recording_id=?", rid);

                        PdfExporter.export(title, note, transcript, tag, out);
                        System.out.println("Exported to " + out.toAbsolutePath());
                    }
                }

                /* ---------- Update note / transcript ---------- */
                case "set-note" -> {
                    if (args.length < 3) {
                        System.out.println("Usage: set-note <recordingId> \"<content>\"");
                        return;
                    }
                    long rid = Long.parseLong(args[1]);
                    String content = args[2];
                    try (Connection conn = open()) {
                        bootstrap(conn);
                        new NoteDao(conn).upsertByRecordingId(rid, content);
                        System.out.println("Note saved.");
                    }
                }

                case "set-transcript" -> {
                    if (args.length < 3) {
                        System.out.println("Usage: set-transcript <recordingId> \"<text>\"");
                        return;
                    }
                    long rid = Long.parseLong(args[1]);
                    String text = args[2];
                    try (Connection conn = open()) {
                        bootstrap(conn);
                        new TranscriptDao(conn).upsertByRecordingId(rid, text);
                        System.out.println("Transcript saved.");
                    }
                }

                /* ---------- Add or update tag ---------- */
                case "set-tag" -> {
                    if (args.length < 3) {
                        System.out.println("Usage: set-tag <recordingId> \"<tag>\"");
                        return;
                    }
                    long rid = Long.parseLong(args[1]);
                    String tag = args[2];
                    try (Connection conn = open()) {
                        bootstrap(conn);
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE recordings SET tag=? WHERE id=?")) {
                            ps.setString(1, tag);
                            ps.setLong(2, rid);
                            int n = ps.executeUpdate();
                            System.out.println(n > 0 ? "Tag updated." : "Recording not found.");
                        }
                    }
                }

                /* ---------- Search by keyword/tag ---------- */
                case "search" -> {
                    if (args.length < 2) {
                        System.out.println("Usage: search <keyword>");
                        return;
                    }
                    String keyword = args[1];
                    try (Connection conn = open();
                         PreparedStatement ps = conn.prepareStatement(
                                 "SELECT id, title, tag FROM recordings " +
                                         "WHERE title LIKE ? OR tag LIKE ? ORDER BY id")) {
                        ps.setString(1, "%" + keyword + "%");
                        ps.setString(2, "%" + keyword + "%");
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                System.out.printf("[%d] %s  (tag: %s)%n",
                                        rs.getLong("id"),
                                        rs.getString("title"),
                                        nullToEmpty(rs.getString("tag")));
                            }
                        }
                    }
                }

                /* ---------- Transcribe ---------- */
                case "transcribe" -> {
                    if (args.length < 3) {
                        System.out.println("Usage: transcribe <audioPath> \"<title>\"");
                        return;
                    }
                    Path audio = Paths.get(args[1]);
                    String title = args[2];

                    try (Connection conn = open()) {
                        bootstrap(conn);

                        var rdao = new RecordingDao(conn);
                        var ndao = new NoteDao(conn);
                        var tdao = new TranscriptDao(conn);

                        long rid = rdao.insert(title, audio.toString(), 0L, System.currentTimeMillis());
                        System.out.println("Recording created id=" + rid);

                        Path bin = pathEnv("WHISPER_CPP_BIN");
                        Path model = pathEnv("WHISPER_CPP_MODEL");

                        TranscriptionService transcriber = new WhisperCppTranscriber(bin, model);
                        var manager = new TranscriptionManager(transcriber, ndao, tdao);

                        String txt;
                        try {
                            txt = manager.transcribeAndStore(rid, audio);
                        } catch (Exception ex) {
                            txt = "(transcription unavailable: missing whisper.cpp binary or model)";
                            tdao.upsertByRecordingId(rid, txt);
                        }
                        System.out.println("Transcript saved:\n" + txt);
                        ndao.upsertByRecordingId(rid, "Auto-generated from transcription");
                    }
                }

                default -> printUsage();
            }
        } catch (Exception e) {
            System.err.println("CLI failed:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /* -------------------- helpers -------------------- */

    private static Connection open() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:data/app.db");
        try (Statement st = conn.createStatement()) { st.execute("PRAGMA foreign_keys = ON"); }
        return conn;
    }

    private static void bootstrap(Connection conn) throws SQLException {
        RecordingDao.createTable(conn);
        NoteDao.createTable(conn);
        TranscriptDao.createTable(conn);
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE recordings ADD COLUMN tag TEXT DEFAULT ''");
        } catch (SQLException ignored) {}
    }

    private static void displayRecording(Connection conn, long rid) throws SQLException {
        String title = getString(conn, "SELECT title FROM recordings WHERE id=?", rid);
        String tag = getString(conn, "SELECT tag FROM recordings WHERE id=?", rid);
        String note = getString(conn, "SELECT content FROM notes WHERE recording_id=?", rid);
        String transcript = getString(conn,
                "SELECT " + resolveTranscriptBodyColumn(conn) +
                        " FROM transcripts WHERE recording_id=?", rid);

        System.out.printf("Title: %s%nTag: %s%n%nNote:%n%s%n%nTranscript:%n%s%n",
                title, tag, note, transcript);
    }

    private static String getString(Connection conn, String sql, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "";
            }
        }
    }

    private static Path pathEnv(String var) {
        String val = System.getenv(var);
        return (val == null || val.isBlank()) ? null : Paths.get(val);
    }

    private static String resolveTranscriptBodyColumn(Connection conn) throws SQLException {
        if (tableHasColumn(conn, "transcripts", "text")) return "text";
        if (tableHasColumn(conn, "transcripts", "content")) return "content";
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE transcripts ADD COLUMN text TEXT");
        }
        return "text";
    }

    private static boolean tableHasColumn(Connection conn, String table, String col) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(" + table + ")")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    if (col.equalsIgnoreCase(rs.getString("name"))) return true;
            }
        }
        return false;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static void printUsage() {
        System.out.println("""
                Usage:
                  initdb
                  migrate
                  list
                  show <recordingId>
                  delete <recordingId>
                  export <recordingId> <out.pdf>
                  set-note <recordingId> "<content>"
                  set-transcript <recordingId> "<text>"
                  set-tag <recordingId> "<tag>"
                  search <keyword>
                  transcribe <audioPath> "<title>"
                """);
    }
}
