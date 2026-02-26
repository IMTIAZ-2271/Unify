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
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private NotificationService() {
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::tick, 10, 60, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
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
