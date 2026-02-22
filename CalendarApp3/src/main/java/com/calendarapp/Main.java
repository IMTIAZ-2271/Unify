package com.calendarapp;

/**
 * Separate launcher class needed on some JVM/JavaFX combos (especially Mac)
 * so the JAR manifest doesn't need to extend Application directly.
 */
public class Main {
    public static void main(String[] args) {
        App.main(args);
    }
}
