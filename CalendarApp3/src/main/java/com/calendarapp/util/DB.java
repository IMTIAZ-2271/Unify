package com.calendarapp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {

    // ── Change these three lines to match your MySQL setup ──────────────────
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DATABASE = "calendar_app";
    private static final String USER = "root";
    private static final String PASSWORD = "12345678"; // ← put your MySQL password here
    // ────────────────────────────────────────────────────────────────────────

    private static final String URL =
        "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static Connection conn() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
