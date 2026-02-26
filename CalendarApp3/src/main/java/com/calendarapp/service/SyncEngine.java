package com.calendarapp.service;

import com.calendarapp.AppData;
import com.calendarapp.Session;
import com.calendarapp.dao.*;
import com.calendarapp.model.*;
import com.calendarapp.util.DB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

/**
 * Polls the server every 5 seconds for changes since last sync.
 * When the user makes a change, it's written to the DB immediately.
 */
public class SyncEngine {

    private static final SyncEngine INSTANCE = new SyncEngine();
    public static SyncEngine get() { return INSTANCE; }

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sync-engine");
                t.setDaemon(true);
                return t;
            });

    private final ExecutorService writer =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "sync-writer");
                t.setDaemon(true);
                return t;
            });

    private volatile LocalDateTime lastSync = LocalDateTime.now().minusSeconds(10);

    private final EventDAO        eventDAO = new EventDAO();
    private final GroupDAO        groupDAO = new GroupDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();

    private SyncEngine() {}

    public void start() {
        // Poll every 5 seconds for incoming changes
        scheduler.scheduleAtFixedRate(this::pollChanges, 5, 5, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
        writer.shutdownNow();
    }

    // ── Incoming: server → AppData ────────────────────────────────────────

    private void pollChanges() {
        if (!Session.isLoggedIn()) return;
        try {
            LocalDateTime since = lastSync;
            lastSync = LocalDateTime.now();
            int uid = Session.uid();

            // 1. Updated/new events
            List<Event> changedEvents = eventDAO.changedSince(uid, since);
            changedEvents.forEach(e -> AppData.get().addOrUpdateEvent(e));

            // 2. Updated/new groups
            List<Group> changedGroups = groupDAO.changedSince(uid, since);
            changedGroups.forEach(g -> AppData.get().addOrUpdateGroup(g));

            // 3. New notifications
            List<Notification> newNotifs = notifDAO.newSince(uid, since);
            newNotifs.forEach(n -> AppData.get().addOrUpdateNotification(n));

            // 4. Deletions
            fetchDeletions(since);

        } catch (Exception e) {
            // Silently ignore — will retry in 5 seconds
            System.err.println("Sync poll failed: " + e.getMessage());
        }
    }

    private void fetchDeletions(LocalDateTime since) throws SQLException {
        String sql = "SELECT table_name, record_id FROM deleted_records WHERE deleted_at > ?";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(since));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String table = rs.getString("table_name");
                int id = rs.getInt("record_id");
                switch (table) {
                    case "events"     -> AppData.get().removeEvent(id);
                    case "groups_tbl" -> AppData.get().removeGroup(id);
                    case "notifications" -> AppData.get().removeNotification(id);
                }
            }
        }
    }

    // ── Outgoing: user action → DB immediately (async) ───────────────────

    /**
     * Call this after any user-initiated change.
     * Writes to DB in background — non-blocking.
     */
    public void push(Runnable dbWrite) {
        writer.submit(() -> {
            try {
                dbWrite.run();
            } catch (Exception e) {
                // TODO: could queue for retry here
                e.printStackTrace();
            }
        });
    }

    public void stop(Runnable dbWrite) {
        writer.submit(dbWrite);
    }
}