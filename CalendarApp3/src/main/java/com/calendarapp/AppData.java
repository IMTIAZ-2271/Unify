package com.calendarapp;

import com.calendarapp.model.*;
import javafx.application.Platform;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Single source of truth for all user data.
 * Controllers READ from here. SyncEngine WRITES here.
 * Thread-safe: listeners always fire on the JavaFX thread.
 */
public class AppData {

    private static final AppData INSTANCE = new AppData();
    public static AppData get() { return INSTANCE; }

    // ── Data ─────────────────────────────────────────────────────────────

    private final List<Event>        events        = new CopyOnWriteArrayList<>();
    private final List<Group>        groups        = new CopyOnWriteArrayList<>();
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();
    private final List<JoinRequest>  joinRequests  = new CopyOnWriteArrayList<>();

    // ── Change listeners ─────────────────────────────────────────────────
    // Controllers subscribe here; fired whenever data changes

    private final List<Runnable> onEventsChanged        = new ArrayList<>();
    private final List<Runnable> onGroupsChanged        = new ArrayList<>();
    private final List<Runnable> onNotificationsChanged = new ArrayList<>();

    private AppData() {}

    // ── Initial load (called once on login) ──────────────────────────────

    public void loadAll(List<Event> e, List<Group> g,
                        List<Notification> n, List<JoinRequest> jr) {
        events.clear();        events.addAll(e);
        groups.clear();        groups.addAll(g);
        notifications.clear(); notifications.addAll(n);
        joinRequests.clear();  joinRequests.addAll(jr);
        fireAll();
    }

    // ── Getters (return unmodifiable snapshots) ───────────────────────────

    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public List<Event> getEventsForMonth(int year, int month) {
        return events.stream()
                .filter(e -> e.getStartTime().getYear() == year
                        && e.getStartTime().getMonthValue() == month)
                .collect(Collectors.toList());
    }

    public List<Event> getEventsForDay(java.time.LocalDate d) {
        return events.stream()
                .filter(e -> e.getStartTime().toLocalDate().equals(d))
                .collect(Collectors.toList());
    }

    public List<Group> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public List<Group> getGroupsIAmAdminOf() {
        return groups.stream()
                .filter(Group::isAdmin)
                .collect(Collectors.toList());
    }

    public boolean isAdmin(int groupId){
        for(Group g : groups){
            if(g.getId()==groupId && g.isAdmin())
                return true;
        }
        return false;
    }

    public List<Notification> getNotifications() {
        return Collections.unmodifiableList(notifications);
    }

    public int getUnreadCount() {
        return (int) notifications.stream().filter(n -> !n.isRead()).count();
    }

    public List<JoinRequest> getJoinRequests() {
        return Collections.unmodifiableList(joinRequests);
    }

    // ── Writes — called immediately on user action ────────────────────────

    public void addOrUpdateEvent(Event e) {
        events.removeIf(x -> x.getId() == e.getId());
        events.add(e);
        events.sort(Comparator.comparing(Event::getStartTime));
        fire(onEventsChanged);
    }

    public void removeEvent(int id) {
        events.removeIf(e -> e.getId() == id);
        fire(onEventsChanged);
    }

    public void addOrUpdateGroup(Group g) {
        groups.removeIf(x -> x.getId() == g.getId());
        groups.add(g);
        groups.sort(Comparator.comparing(Group::getName));
        fire(onGroupsChanged);
    }

    public void removeGroup(int id) {
        groups.removeIf(g -> g.getId() == id);
        fire(onGroupsChanged);
    }

    public void addOrUpdateNotification(Notification n) {
        notifications.removeIf(x -> x.getId() == n.getId());
        notifications.add(0, n); // newest first
        fire(onNotificationsChanged);
    }

    public void markNotificationRead(int id) {
        notifications.stream()
                .filter(n -> n.getId() == id)
                .findFirst()
                .ifPresent(n -> n.setRead(true));
        fire(onNotificationsChanged);
    }

    public void markAllNotificationsRead() {
        notifications.forEach(n -> n.setRead(true));
        fire(onNotificationsChanged);
    }

    public void removeNotification(int id) {
        notifications.removeIf(n -> n.getId() == id);
        fire(onNotificationsChanged);
    }

    // ── Listener registration ─────────────────────────────────────────────

    public void addEventsListener(Runnable r)        { onEventsChanged.add(r); }
    public void removeEventsListener(Runnable r)     { onEventsChanged.remove(r); }

    public void addGroupsListener(Runnable r)        { onGroupsChanged.add(r); }
    public void removeGroupsListener(Runnable r)     { onGroupsChanged.remove(r); }

    public void addNotifListener(Runnable r)         { onNotificationsChanged.add(r); }
    public void removeNotifListener(Runnable r)      { onNotificationsChanged.remove(r); }

    // ── Clear on logout ───────────────────────────────────────────────────

    public void clear() {
        events.clear();
        groups.clear();
        notifications.clear();
        joinRequests.clear();
        onEventsChanged.clear();
        onGroupsChanged.clear();
        onNotificationsChanged.clear();
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private void fire(List<Runnable> listeners) {
        // Always deliver on the JavaFX thread so controllers can update UI directly
        Runnable task = () -> new ArrayList<>(listeners).forEach(Runnable::run);
        if (Platform.isFxApplicationThread()) task.run();
        else Platform.runLater(task);
    }

    private void fireAll() {
        fire(onEventsChanged);
        fire(onGroupsChanged);
        fire(onNotificationsChanged);
    }
}