package com.Unify.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {

//    // ── Change these three lines to match your MySQL setup ──────────────────
//    private static final String HOST = "sql12.freesqldatabase.com";
//    private static final String PORT = "3306";
//    private static final String DATABASE = "sql12817926";
//    private static final String USER = "sql12817926";
//    private static final String PASSWORD = "1X8FwV99pY";

    // ── Change these three lines to match your MySQL setup ──────────────────
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DATABASE = "calendar_app";
    private static final String USER = "root";
    private static final String PASSWORD = "12345678";
    // ────────────────────────────────────────────────────────────────────────

    private static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static Connection conn() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
