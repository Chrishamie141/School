package com.notesapp.dao;

import com.notesapp.model.Recording;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecordingDao {
    private final Connection conn;

    public RecordingDao(Connection conn) throws SQLException {
        this.conn = conn;
        try (Statement s = conn.createStatement()) { s.execute("PRAGMA foreign_keys = ON"); }
        RecordingDao.createTable(conn);
    }

    /** Create table if missing; ensure at least one variant of each logical field exists. */
    public static void createTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS recordings(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  title TEXT NOT NULL
                )
            """);
        }
        // path column variants (prefer audio_path for CLI compatibility)
        ensureAny(conn, "recordings", new String[]{"audio_path", "file_path", "path"},
                "ALTER TABLE recordings ADD COLUMN audio_path TEXT");
        // duration column variants
        ensureAny(conn, "recordings", new String[]{"duration_ms", "duration"},
                "ALTER TABLE recordings ADD COLUMN duration_ms INTEGER NOT NULL DEFAULT 0");
        // created/recorded time variants
        ensureAny(conn, "recordings", new String[]{"created_at", "created_at_ms", "timestamp", "recorded_at"},
                "ALTER TABLE recordings ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0");
        // optional class name
        ensureAny(conn, "recordings", new String[]{"class_name"},
                "ALTER TABLE recordings ADD COLUMN class_name TEXT");
        // ✅ Week 6: tag column
        ensureAny(conn, "recordings", new String[]{"tag"},
                "ALTER TABLE recordings ADD COLUMN tag TEXT DEFAULT ''");
    }

    private static void ensureAny(Connection c, String table, String[] cols, String ddlIfMissing) throws SQLException {
        boolean any = false;
        for (String col : cols) if (hasColumn(c, table, col)) { any = true; break; }
        if (!any) try (Statement st = c.createStatement()) { st.execute(ddlIfMissing); }
    }

    private static boolean hasColumn(Connection c, String table, String col) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("PRAGMA table_info(" + table + ")")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) if (col.equalsIgnoreCase(rs.getString("name"))) return true;
            }
        }
        return false;
    }

    /** Return first existing column among the given names, or throw if none exist. */
    private String existingColOrThrow(String... names) throws SQLException {
        for (String n : names) if (hasColumn(conn, "recordings", n)) return n;
        throw new SQLException("None of the expected columns exist: " + String.join(",", names));
    }

    private String pathCol() throws SQLException {
        if (hasColumn(conn, "recordings", "audio_path")) return "audio_path";
        if (hasColumn(conn, "recordings", "file_path")) return "file_path";
        if (hasColumn(conn, "recordings", "path")) return "path";
        try (Statement st = conn.createStatement()) { st.execute("ALTER TABLE recordings ADD COLUMN audio_path TEXT"); }
        return "audio_path";
    }

    private String durationCol() throws SQLException {
        if (hasColumn(conn, "recordings", "duration_ms")) return "duration_ms";
        if (hasColumn(conn, "recordings", "duration")) return "duration";
        try (Statement st = conn.createStatement()) { st.execute("ALTER TABLE recordings ADD COLUMN duration_ms INTEGER NOT NULL DEFAULT 0"); }
        return "duration_ms";
    }

    /** Return list of existing created-time columns we should populate. */
    private List<String> createdCols() throws SQLException {
        List<String> cols = new ArrayList<>();
        if (hasColumn(conn, "recordings", "created_at")) cols.add("created_at");
        if (hasColumn(conn, "recordings", "created_at_ms")) cols.add("created_at_ms");
        if (hasColumn(conn, "recordings", "timestamp")) cols.add("timestamp");
        if (hasColumn(conn, "recordings", "recorded_at")) cols.add("recorded_at");
        if (cols.isEmpty()) {
            try (Statement st = conn.createStatement()) { st.execute("ALTER TABLE recordings ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0"); }
            cols.add("created_at");
        }
        return cols;
    }

    /* ---------------- App API (explicit args) ---------------- */
    public long insert(String title, String filePath, long durationMs, long createdAtEpochMs) throws SQLException {
        String pcol = pathCol();
        String dcol = durationCol();
        List<String> ccols = createdCols();

        StringBuilder sbCols = new StringBuilder("title, ").append(pcol).append(", ").append(dcol);
        StringBuilder sbVals = new StringBuilder("?,?,?");
        for (int i = 0; i < ccols.size(); i++) {
            sbCols.append(", ").append(ccols.get(i));
            sbVals.append(", ?");
        }
        sbCols.append(", class_name, tag");
        sbVals.append(", NULL, ''");

        String sql = "INSERT INTO recordings(" + sbCols + ") VALUES (" + sbVals + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            ps.setString(idx++, nz(title));
            ps.setString(idx++, nz(filePath));
            ps.setLong(idx++, durationMs);
            for (int i = 0; i < ccols.size(); i++) ps.setLong(idx++, createdAtEpochMs);
            ps.executeUpdate();

            Long id = readGeneratedKeyOrFallback(ps, conn);
            if (id != null) return id;
        }
        throw new SQLException("Failed to insert recording (no id returned)");
    }

    /* ---------------- Test-compat API (model in/out) ---------------- */
    public Recording insert(Recording r) throws SQLException {
        String title     = pickString(r, "getTitle", "title", "getName", "name");
        String filePath  = pickString(r, "getFilePath", "getPath", "getAudioPath", "path", "filePath", "file");
        long durationMs  = pickLong(r, 0L, "getDurationMs", "getDurationMillis", "getDuration", "durationMs", "duration");
        long createdAtMs = pickLong(r, System.currentTimeMillis(),
                "getCreatedAtEpochMs", "getCreatedAtMs", "getCreatedAt", "getTimestamp", "createdAtMs", "createdAt", "getRecordedAt", "recordedAt");
        String className = pickStringOrNull(r, "getClassName", "className", "getCourse", "course", "getCourseName", "courseName");

        String pcol = pathCol();
        String dcol = durationCol();
        List<String> ccols = createdCols();

        StringBuilder sbCols = new StringBuilder("title, ").append(pcol).append(", ").append(dcol);
        StringBuilder sbVals = new StringBuilder("?,?,?");
        for (int i = 0; i < ccols.size(); i++) { sbCols.append(", ").append(ccols.get(i)); sbVals.append(", ?"); }
        sbCols.append(", class_name, tag");
        sbVals.append(", ?, ''");

        String sql = "INSERT INTO recordings(" + sbCols + ") VALUES (" + sbVals + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            ps.setString(idx++, nz(title));
            ps.setString(idx++, nz(filePath));
            ps.setLong(idx++, durationMs);
            for (int i = 0; i < ccols.size(); i++) ps.setLong(idx++, createdAtMs);
            if (className == null || className.isBlank()) ps.setNull(idx, Types.VARCHAR); else ps.setString(idx, className);
            ps.executeUpdate();

            Long id = readGeneratedKeyOrFallback(ps, conn);
            if (id != null) {
                setIdOnModel(r, id);
                setString(r, "setClassName", className);
                return r;
            }
        }
        throw new SQLException("Failed to insert recording (no id returned)");
    }

    /** Robust id retrieval for SQLite */
    private static Long readGeneratedKeyOrFallback(PreparedStatement ps, Connection c) {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs != null && rs.next()) return rs.getLong(1);
        } catch (SQLException ignored) {}
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT last_insert_rowid()")) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException ignored) {}
        return null;
    }

    /* ---------------- Model-based reads ---------------- */
    public Optional<Recording> findById(long id) throws SQLException {
        String pcol = existingColOrThrow("audio_path", "file_path", "path");
        String dcol = existingColOrThrow("duration_ms", "duration");
        String ccol = existingColOrThrow("created_at", "created_at_ms", "timestamp", "recorded_at");

        String sql = "SELECT id, title, " + pcol + " AS _path, " + dcol + " AS _duration_ms, "
                   + ccol + " AS _created_at, class_name, tag FROM recordings WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapToModel(rs));
            }
        }
    }

    public List<Recording> findAllModels() throws SQLException {
        String pcol = existingColOrThrow("audio_path", "file_path", "path");
        String dcol = existingColOrThrow("duration_ms", "duration");
        String ccol = existingColOrThrow("created_at", "created_at_ms", "timestamp", "recorded_at");

        String sql = "SELECT id, title, " + pcol + " AS _path, " + dcol + " AS _duration_ms, "
                   + ccol + " AS _created_at, class_name, tag FROM recordings ORDER BY _created_at DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Recording> out = new ArrayList<>();
            while (rs.next()) out.add(mapToModel(rs));
            return out;
        }
    }

    /* ---------------- Simple helpers for Week 6 UI ---------------- */
    public List<String> getAllRecordingNames() throws SQLException {
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT title FROM recordings ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("title"));
        }
        return list;
    }

    public List<String> searchByKeyword(String keyword) throws SQLException {
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT title FROM recordings WHERE title LIKE ? OR tag LIKE ? ORDER BY id")) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("title"));
            }
        }
        return list;
    }

    public String getTagForRecording(String title) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT tag FROM recordings WHERE title=?")) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("tag") : "";
            }
        }
    }

    public void setTagForRecording(String title, String tag) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE recordings SET tag=? WHERE title=?")) {
            ps.setString(1, tag);
            ps.setString(2, title);
            ps.executeUpdate();
        }
    }

    /* ---------------- Reflection-based mapping helpers ---------------- */
    private Recording mapToModel(ResultSet rs) throws SQLException {
        long id         = rs.getLong("id");
        String title    = rs.getString("title");
        String filePath = rs.getString("_path");
        long durationMs = rs.getLong("_duration_ms");
        long createdAt  = rs.getLong("_created_at");
        String classNm  = rs.getString("class_name");
        String tag      = hasColumn(conn, "recordings", "tag") ? rs.getString("tag") : "";

        Recording r = newInstance(Recording.class);
        setLong(r, "setId", id);
        setString(r, "setTitle", title);
        setString(r, "setFilePath", filePath);
        setString(r, "setPath", filePath);
        setString(r, "setAudioPath", filePath);
        setLong(r, "setDurationMs", durationMs);
        setLong(r, "setDuration", durationMs);
        setLong(r, "setCreatedAt", createdAt);
        setString(r, "setClassName", classNm);
        setString(r, "setTag", tag);
        setFieldIfPresent(r, "tag", tag);
        return r;
    }

    private static <T> T newInstance(Class<T> c) {
        try { return c.getDeclaredConstructor().newInstance(); }
        catch (Exception e) { throw new RuntimeException("Model needs a no-arg ctor: " + c.getName(), e); }
    }

    private static void setIdOnModel(Object obj, long id) {
        setLong(obj, "setId", id);
        setInt(obj, "setId", (int) id);
        setFieldIfPresent(obj, "id", id);
    }

    private static String nz(String s) { return s == null ? "" : s; }
    private static void setString(Object o, String setter, String v) {
        try { Method m = o.getClass().getMethod(setter, String.class); m.invoke(o, v); } catch (Exception ignored) {}
    }
    private static void setLong(Object o, String setter, long v) {
        try { Method m = o.getClass().getMethod(setter, long.class); m.invoke(o, v); } catch (Exception ignored) {}
        try { Method m = o.getClass().getMethod(setter, Long.class); m.invoke(o, v); } catch (Exception ignored) {}
    }
    private static void setInt(Object o, String setter, int v) {
        try { Method m = o.getClass().getMethod(setter, int.class); m.invoke(o, v); } catch (Exception ignored) {}
        try { Method m = o.getClass().getMethod(setter, Integer.class); m.invoke(o, v); } catch (Exception ignored) {}
    }
    private static void setFieldIfPresent(Object o, String name, Object v) {
        try { Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); f.set(o, v); } catch (Exception ignored) {}
    }

    private static String pickString(Object obj, String... names) {
        for (String n : names) {
            try { Method m = obj.getClass().getMethod(n); Object v = m.invoke(obj); if (v != null) return String.valueOf(v); }
            catch (Exception ignored) {}
        }
        return "";
    }
    private static String pickStringOrNull(Object obj, String... names) {
        for (String n : names) {
            try { Method m = obj.getClass().getMethod(n); Object v = m.invoke(obj);
                  if (v != null && !String.valueOf(v).isBlank()) return String.valueOf(v); }
            catch (Exception ignored) {}
        }
        return null;
    }
    private static long pickLong(Object obj, long def, String... names) {
        for (String n : names) {
            try {
                Method m = obj.getClass().getMethod(n);
                Object v = m.invoke(obj);
                if (v == null) continue;
                if (v instanceof Number num) return num.longValue();
                return Long.parseLong(String.valueOf(v));
            } catch (Exception ignored) {}
        }
        return def;
    }
}
