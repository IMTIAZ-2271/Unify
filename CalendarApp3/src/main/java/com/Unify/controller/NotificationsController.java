package com.Unify.controller;

import com.Unify.AppData;
import com.Unify.Navigator;
import com.Unify.Session;
import com.Unify.dao.EventDAO;
import com.Unify.dao.GroupDAO;
import com.Unify.dao.NotificationDAO;
import com.Unify.model.Event;
import com.Unify.model.Group;
import com.Unify.model.Notification;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationsController {

    @FXML
    private VBox list;
    @FXML
    private Label emptyLabel;

    private final NotificationDAO dao = new NotificationDAO();
    private final GroupDAO groupDAO = new GroupDAO();
    private final EventDAO eventDAO = new EventDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    @FXML
    private void initialize() {
        load();
    }

    @FXML
    private void doMarkAll() {
        try {
            dao.markAllRead(Session.uid());
            load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void doRefresh() {
        load();
    }

    private void load() {
        list.getChildren().clear();
        try {
            //List<Notification> items = dao.forUser(Session.uid());
            List<Notification> items = AppData.get().getNotifications();
            emptyLabel.setVisible(items.isEmpty());
            for (Notification n : items) list.getChildren().add(card(n));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox card(Notification n) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(n.isRead()
                ? "-fx-background-color:white;-fx-border-color:#F1F5F9;-fx-border-width:0 0 1 0;"
                : "-fx-background-color:#EFF6FF;-fx-border-color:#BFDBFE;-fx-border-width:0 0 1 0;");

        Label icon = new Label(emoji(n.getType()));
        icon.setStyle("-fx-font-size:20;");

        VBox body = new VBox(3);
        HBox.setHgrow(body, Priority.ALWAYS);

        Label titleLbl = new Label(n.getTitle());
        titleLbl.setStyle("-fx-font-weight:" + (n.isRead() ? "normal" : "bold") + ";-fx-font-size:13;");
        Label msg = new Label(n.getMessage());
        msg.setStyle("-fx-text-fill:#64748B;-fx-font-size:12;");
        msg.setWrapText(true);
        String ts = n.getCreatedAt() != null ? n.getCreatedAt().format(FMT) : "";
        Label time = new Label(ts);
        time.setStyle("-fx-text-fill:#aaa;-fx-font-size:10;");
        body.getChildren().addAll(titleLbl, msg, time);

        // ── Group invite: show Accept / Decline ──────────────────────────
        if ("group_invite".equals(n.getType()) && n.getReferenceId() != null) {
            if (n.getInviteAccepted() == 0) {
                HBox actions = new HBox(8);
                Button acc = new Button("✅ Accept");
                acc.getStyleClass().add("btn-success-small");
                acc.setOnAction(e -> acceptInvite(n));

                Button dec = new Button("❌ Decline");
                dec.getStyleClass().add("btn-danger-small");
                dec.setOnAction(e -> declineInvite(n));

                actions.getChildren().addAll(acc, dec);
                body.getChildren().add(actions);
            } else if (n.getInviteAccepted() == 1) {
                Label acc = new Label();
                acc.setText("Accepted");
                acc.getStyleClass().add("custom-label");
                body.getChildren().add(acc);
            } else if (n.getInviteAccepted() == -1) {
                Label dec = new Label();
                dec.setText("Declined");
                dec.getStyleClass().add("custom-label");
                body.getChildren().add(dec);
            }
        } else {
            // ── Clickable hyperlink to navigate to source ──────────────
            String linkText = linkLabel(n.getType());
            if (linkText != null && n.getReferenceId() != null) {
                Hyperlink link = new Hyperlink("→ " + linkText);
                link.setStyle("-fx-text-fill:#3B82F6;-fx-font-size:12;-fx-padding:0;");
                link.setOnAction(e -> navigateTo(n));
                body.getChildren().add(link);
            }
        }

        // Mark read button
        Button readBtn = new Button(n.isRead() ? "✓" : "Mark Read");
        readBtn.getStyleClass().add("btn-small");
        readBtn.setOnAction(e -> {
            try {
                AppData.get().markNotificationRead(n.getId());
                load();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button delBtn = new Button("✕");
        delBtn.getStyleClass().add("btn-danger-small");
        delBtn.setOnAction(e -> {
            try {
                AppData.get().removeNotification(n.getId());
                load();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        row.getChildren().addAll(icon, body, readBtn, delBtn);
        return row;
    }

    // ── Invite handling ───────────────────────────────────────────────────

    private void acceptInvite(Notification n) {
        try {
            int groupId = n.getReferenceId();
            groupDAO.addMember(groupId, Session.uid(), "member");
            AppData.get().markNotificationRead(n.getId());
            dao.updateInviteAccepted(n.getUserId(), groupId, 1);
            // Navigate to the group
            load();
            try {
                Group g = groupDAO.findById(groupId, Session.uid());
                if (g != null) {
                    GroupDetailController ctrl = Navigator.push("/com/Unify/fxml/group_detail.fxml");
                    if (ctrl != null) ctrl.setGroup(g);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
        }
    }

    private void declineInvite(Notification n) {
        try {
            dao.markRead(n.getId());
            dao.updateInviteAccepted(n.getUserId(), n.getReferenceId(), -1);
            load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Navigation from notification ──────────────────────────────────────

    private void navigateTo(Notification n) {
        try {
            dao.markRead(n.getId());
            load();
            switch (n.getType() != null ? n.getType() : "") {
                case "accepted", "declined", "admin_added", "admin_removed", "member_added" -> {
                    if (n.getReferenceId() != null) {
                        Group g = groupDAO.findById(n.getReferenceId(), Session.uid());
                        if (g != null) {
                            GroupDetailController ctrl = Navigator.push("/com/Unify/fxml/group_detail.fxml");
                            if (ctrl != null) ctrl.setGroup(g);
                        }
                    }
                }
                case "event_created", "reminder" -> {
                    if (n.getReferenceId() != null) {
                        Event e = eventDAO.findById(n.getReferenceId());
                        if (e != null) {
                            EventDetailController ctrl = Navigator.showWindow("/com/Unify/fxml/event_detail.fxml");
                            if (ctrl != null) ctrl.setEvent(e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String emoji(String type) {
        if (type == null) return "🔔";
        return switch (type) {
            case "reminder" -> "⏰";
            case "accepted" -> "✅";
            case "declined" -> "❌";
            case "event_created" -> "📅";
            case "admin_added" -> "⭐";
            case "admin_removed" -> "🔻";
            case "member_added" -> "👤";
            case "group_invite" -> "📩";
            default -> "🔔";
        };
    }

    private String linkLabel(String type) {
        if (type == null) return null;
        return switch (type) {
            case "accepted", "declined" -> "View Group";
            case "admin_added", "admin_removed", "member_added" -> "View Group";
            case "event_created", "reminder" -> "View Event";
            default -> null;
        };
    }
}
