package com.notesapp.dao;

import java.sql.*;
import java.util.Optional;

public class NoteDao {
    private final Connection conn;

    public NoteDao(Connection conn) throws SQLException {
        this.conn = conn;
        try (Statement s = conn.createStatement()) { s.execute("PRAGMA foreign_keys = ON"); }
        createTable(conn);
    }

    /** Create table if it doesn’t exist (idempotent). */
    public static void createTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    recording_id INTEGER NOT NULL UNIQUE,
                    content TEXT NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (recording_id) REFERENCES recordings(id) ON DELETE CASCADE
                )
            """);
        }
    }

    // ---------- MIGRATION HELPERS ----------
    private static boolean hasColumn(Connection c, String table, String col) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("PRAGMA table_info(" + table + ")")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (col.equalsIgnoreCase(rs.getString("name"))) return true;
                }
            }
        }
        return false;
    }

    /** Add missing created_at / updated_at columns if an older DB exists. Idempotent. */
    public static void migrate(Connection conn) throws SQLException {
        createTable(conn); // safe even if exists
        if (!hasColumn(conn, "notes", "created_at")) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE notes ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
            }
        }
        if (!hasColumn(conn, "notes", "updated_at")) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE notes ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
            }
        }
    }

    // ---------- Row type ----------
    public static class NoteRow {
        private final long id;
        private final long recordingId;
        private final String content;
        private final String createdAt;
        private final String updatedAt;

        public NoteRow(long id, long recordingId, String content, String createdAt, String updatedAt) {
            this.id = id;
            this.recordingId = recordingId;
            this.content = content;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public long getId() { return id; }
        public long getRecordingId() { return recordingId; }
        public String getContent() { return content; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
    }

    // ---------- Upserts (overloads to match earlier tests) ----------
    public int upsertByRecordingId(int recordingId, String content, String _unused) throws SQLException {
        return upsertByRecordingId((long) recordingId, content);
    }

    public int upsertByRecordingId(int recordingId, String content) throws SQLException {
        return upsertByRecordingId((long) recordingId, content);
    }

    public int upsertByRecordingId(long recordingId, String content) throws SQLException {
        final String sql = """
            INSERT INTO notes (recording_id, content, created_at, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT(recording_id) DO UPDATE SET
              content = excluded.content,
              updated_at = CURRENT_TIMESTAMP
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recordingId);
            ps.setString(2, content == null ? "" : content);
            return ps.executeUpdate();
        }
    }

    // ---------- Reads ----------
    public Optional<NoteRow> findByRecordingId(int recordingId) throws SQLException {
        return findByRecordingId((long) recordingId);
    }

    public Optional<NoteRow> findByRecordingId(long recordingId) throws SQLException {
        final String sql = """
            SELECT id, recording_id, content, created_at, updated_at
            FROM notes
            WHERE recording_id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recordingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new NoteRow(
                    rs.getLong("id"),
                    rs.getLong("recording_id"),
                    rs.getString("content"),
                    rs.getString("created_at"),
                    rs.getString("updated_at")
                ));
            }
        }
    }

    public int deleteByRecordingId(long recordingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM notes WHERE recording_id = ?")) {
            ps.setLong(1, recordingId);
            return ps.executeUpdate();
        }
    }
}
