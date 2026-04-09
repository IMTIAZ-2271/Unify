package com.Unify.util;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Stores the last logged-in userId on disk so the app can auto-login
 * for up to 3 days without requiring re-authentication.
 * File: ~/.calendarapp/session.properties
 * Cleared on explicit logout.
 */
public class SessionStore {

    private static final long THREE_DAYS_MS = 3*24*60* 60 * 1000;
    private static final Path DIR = Path.of(System.getProperty("user.home"), ".Unify");
    private static final Path FILE = DIR.resolve("session.properties");

    /**
     * Save userId with a 3-day expiry.
     */
    public static void save(int userId) {
        try {
            Files.createDirectories(DIR);
            Properties p = new Properties();
            p.setProperty("userId", String.valueOf(userId));
            p.setProperty("expires", String.valueOf(System.currentTimeMillis() + THREE_DAYS_MS));
            try (Writer w = Files.newBufferedWriter(FILE)) {
                p.store(w, "Unify session");
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Returns stored userId if session is still valid (< 3 days old), else -1.
     */
    public static int loadUserId() {
        if (!Files.exists(FILE)) return -1;
        try {
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(FILE)) {
                p.load(r);
            }
            long expires = Long.parseLong(p.getProperty("expires", "0"));
            if (System.currentTimeMillis() > expires) {
                clear();
                return -1;
            }
            return Integer.parseInt(p.getProperty("userId", "-1"));
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Delete the stored session (called on explicit logout).
     */
    public static void clear() {
        try {
            Files.deleteIfExists(FILE);
        } catch (IOException ignored) {
        }
    }
}
