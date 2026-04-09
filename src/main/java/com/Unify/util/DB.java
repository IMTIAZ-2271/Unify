package com.Unify.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {

//    // ── Change these three lines to match your MySQL setup ──────────────────
//    private static final String HOST = "sql12.freesqldatabase.com";
//    private static final String PORT = "3306";
//    private static final String DATABASE = "sql12820176";
//    private static final String USER = "sql12820176";
//    private static final String PASSWORD = "37aRSSjxEj";

    // ── Change these three lines to match your MySQL setup ──────────────────
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DATABASE = "Unify";
    private static final String USER = "root";
    private static final String PASSWORD = "Imti@z3point14159";
    // ────────────────────────────────────────────────────────────────────────

    private static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static Connection conn() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
