package com.Unify.service;

import com.Unify.Session;
import com.Unify.dao.EventDAO;
import com.Unify.dao.NotificationDAO;
import com.Unify.model.Event;
import com.Unify.model.Notification;
import javafx.application.Platform;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationService {

    private static final NotificationService INSTANCE = new NotificationService();

    public static NotificationService get() {
        return INSTANCE;
    }

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final EventDAO eventDAO = new EventDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();
    private final Set<Integer> alerted = new HashSet<>();
    private Consumer<Integer> badgeUpdater;
    private ScheduledExecutorService executor;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private NotificationService() {
    }


public void start() {
    // 1. Check if the executor is null OR if it has already been shut down
    if (executor == null || executor.isShutdown() || executor.isTerminated()) {
        // 2. Create a fresh thread pool!
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    // 3. Schedule the 'tick' method to run
    // I set this back to your original timing: start after 10 seconds, run every 60 seconds.
    executor.scheduleAtFixedRate(this::tick, 10, 60, TimeUnit.SECONDS);
}

    public void stop() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow(); // Kill the thread when logging out
        }
        alerted.clear();
    }



    public void setBadgeUpdater(Consumer<Integer> fn) {
        this.badgeUpdater = fn;
    }

    public void refreshBadge() {
        if (!Session.isLoggedIn() || badgeUpdater == null) return;
        try {
            int n = notifDAO.unreadCount(Session.uid());
            Platform.runLater(() -> badgeUpdater.accept(n));
        } catch (SQLException ignored) {
        }
    }

    private void tick() {
        if (!Session.isLoggedIn()) return;
        try (java.sql.Connection c = com.Unify.util.DB.conn();
             java.sql.PreparedStatement ps = c.prepareStatement("DELETE FROM notifications WHERE created_at < NOW() - INTERVAL 30 DAY")) {
            ps.executeUpdate();
        } catch (SQLException ignored) {}
        try {
            List<Event> soon = eventDAO.upcoming(Session.uid(), 15);
            for (Event e : soon) {
                if (alerted.add(e.getId())) {
                    Notification n = new Notification(Session.uid(),
                            "⏰ Upcoming: " + e.getTitle(),
                            "Starts at " + e.getStartTime().format(FMT)
                                    + (e.getLocation() != null && !e.getLocation().isEmpty() ? " · " + e.getLocation() : ""),
                            "reminder");
                    n.setReferenceId(e.getId());
                    notifDAO.create(n);
                    refreshBadge();
                }
            }
        } catch (SQLException ignored) {
        }
    }
}
