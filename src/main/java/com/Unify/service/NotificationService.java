package com.Unify.service;

import com.Unify.AppData;
import com.Unify.Session;
import com.Unify.dao.NotificationDAO;
import com.Unify.model.Notification;
import com.Unify.util.AsyncWriter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import java.util.function.Consumer;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationService {

    private static final NotificationService INSTANCE = new NotificationService();
    private final NotificationDAO dao = new NotificationDAO();

    private Timeline backgroundPoller;
    private LocalDateTime lastCheckTime;

    private Consumer<Integer> badgeUpdater;

    public static NotificationService get() {
        return INSTANCE;
    }

    private NotificationService() {}

    public void start() {
        if (backgroundPoller != null) {
            backgroundPoller.stop();
        }

        lastCheckTime = LocalDateTime.now();

        // Polls the database every 5 seconds
        backgroundPoller = new Timeline(new KeyFrame(Duration.seconds(5), e -> fetchNewBackgroundNotifications()));
        backgroundPoller.setCycleCount(Animation.INDEFINITE);
        backgroundPoller.play();
    }

    public void stop() {
        if (backgroundPoller != null) {
            backgroundPoller.stop();
        }
    }

    private void fetchNewBackgroundNotifications() {
        if (!Session.isLoggedIn()) return;

        com.Unify.util.AsyncWriter.get().write(
                () -> {
                    List<Notification> newNotifs = dao.newSince(Session.uid(), lastCheckTime);
                    lastCheckTime = LocalDateTime.now();
                    return newNotifs;
                },
                (newNotifs) -> {
                    if (newNotifs != null && !newNotifs.isEmpty()) {
                        // 🛠️ THE FIX: Loop through and use AppData's built-in write method!
                        // We loop backwards so they are inserted at index 0 in chronological order.
                        for (int i = newNotifs.size() - 1; i >= 0; i--) {
                            AppData.get().addOrUpdateNotification(newNotifs.get(i));
                        }
                        refreshBadge();
                    }
                },
                (error) -> System.err.println("Background notification check failed: " + error.getMessage())
        );
    }

    public void setBadgeUpdater(Consumer<Integer> badgeUpdater) {
        this.badgeUpdater = badgeUpdater;
        refreshBadge(); // Trigger an initial update as soon as the controller connects
    }

    public void refreshBadge() {
        if (badgeUpdater != null) {
            // We can now grab the count directly from our lightning-fast AppData cache!
            int unreadCount = AppData.get().getUnreadCount();

            // Ensure the UI update happens on the main JavaFX thread
            javafx.application.Platform.runLater(() -> badgeUpdater.accept(unreadCount));
        }
    }

}