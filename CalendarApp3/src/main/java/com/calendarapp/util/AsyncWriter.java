package com.calendarapp.util;

import javafx.application.Platform;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * All database writes go through here.
 * A single background thread processes them in order.
 * The UI never waits.
 */
public class AsyncWriter {

    private static final AsyncWriter INSTANCE = new AsyncWriter();
    public static AsyncWriter get() { return INSTANCE; }

    // Single thread so writes happen in the exact order the user made them
    private final ExecutorService thread =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "db-write-thread");
                t.setDaemon(true);
                return t;
            });

    private AsyncWriter() {}

    /**
     * Submit a write task.
     *
     * @param task      the DB operation (runs on background thread)
     * @param onSuccess runs on JavaFX thread if write succeeds
     * @param onError   runs on JavaFX thread if write fails
     */
    public <T> void write(
            Callable<T>       task,
            Consumer<T>       onSuccess,
            Consumer<Exception> onError) {

        thread.submit(() -> {
            try {
                T result = task.call();
                if (onSuccess != null)
                    Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                if (onError != null)
                    Platform.runLater(() -> onError.accept(e));
                else
                    e.printStackTrace();
            }
        });
    }

    /**
     * Simple write with no return value.
     */
    public void write(Runnable task, Runnable onSuccess, Consumer<Exception> onError) {
        thread.submit(() -> {
            try {
                task.run();
                if (onSuccess != null) Platform.runLater(onSuccess);
            } catch (Exception e) {
                if (onError != null) Platform.runLater(() -> onError.accept(e));
                else e.printStackTrace();
            }
        });
    }

    /**
     * Fire and forget — no callbacks needed.
     * Use for low-stakes writes like markRead, last_login, etc.
     */
    public void write(Runnable task) {
        write(task, null, null);
    }

    public void shutdown() {
        thread.shutdown();
    }
}