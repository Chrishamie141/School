package com.notesapp.dao;
import com.notesapp.db.DatabaseManager;
import java.sql.*;
import java.util.Optional;
public class NoteDao {
    private final Connection conn;
    public NoteDao(Connection conn) throws SQLException {
        this.conn = conn;
        try (Statement s = conn.createStatement()) { s.execute("PRAGMA foreign_keys = ON"); }
        createTable(conn);
    }
    /** Create table if it doesnÃ¢â‚¬â„¢t exist (idempotent). */
    public static void createTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    recording_id INTEGER UNIQUE,
                    title TEXT,
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
                while (rs.next()) if (col.equalsIgnoreCase(rs.getString("name"))) return true;
            }
        }
        return false;
    }
    public static void migrate(Connection conn) throws SQLException {
        createTable(conn);
        if (!hasColumn(conn, "notes", "updated_at")) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE notes ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
            }
        }
    }
    // ---------- Row type for tests ----------
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
    // ---------- Upserts ----------
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
            int updated = ps.executeUpdate();
            DatabaseManager.commit();
            return updated;
        } catch (SQLException e) {
            DatabaseManager.rollback();
            throw e;
        }
    }
    /** Legacy overload for tests */
    public int upsertByRecordingId(int recordingId, String content, String _unused) throws SQLException {
        return upsertByRecordingId((long) recordingId, content);
    }
    // ---------- Title-based access (UI) ----------
    public Optional<String> findByTitle(String title) throws SQLException {
        String sql = "SELECT content FROM notes WHERE title=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.ofNullable(rs.getString("content"));
            }
        }
        return Optional.empty();
    }
    public void save(String title, String content) throws SQLException {
        String sql = """
            INSERT INTO notes(title, content)
            VALUES(?, ?)
            ON CONFLICT(title) DO UPDATE SET content=excluded.content
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.executeUpdate();
            DatabaseManager.commit();
        } catch (SQLException e) {
            DatabaseManager.rollback();
            throw e;
        }
    }
    // ---------- Record-based read/delete ----------
    public Optional<NoteRow> findByRecordingId(long recordingId) throws SQLException {
        String sql = "SELECT * FROM notes WHERE recording_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recordingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
        return Optional.empty();
    }
    public int deleteByRecordingId(long recordingId) throws SQLException {
        String sql = "DELETE FROM notes WHERE recording_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recordingId);
            int deleted = ps.executeUpdate();
            DatabaseManager.commit();
            return deleted;
        } catch (SQLException e) {
            DatabaseManager.rollback();
            throw e;
        }
    }
}


