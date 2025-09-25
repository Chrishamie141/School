package com.notesapp.db;

import java.nio.file.*;
import java.sql.*;

public class Db {
    private static final String DB_PATH = "data/notes.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    public static void init() {
        try {
            Files.createDirectories(Paths.get("data"));
            try (Connection conn = DriverManager.getConnection(JDBC_URL);
                 Statement st = conn.createStatement()) {
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS notes(
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      title TEXT NOT NULL,
                      body  TEXT NOT NULL,
                      created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    );
                """);
            }
            System.out.println("SQLite ready at " + DB_PATH);
        } catch (Exception e) {
            throw new RuntimeException("DB init failed", e);
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }
}