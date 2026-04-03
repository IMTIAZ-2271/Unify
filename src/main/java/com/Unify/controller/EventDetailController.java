package com.Unify.controller;

import com.Unify.AppData;
import com.Unify.Navigator;
import com.Unify.Session;
import com.Unify.dao.EventDAO;
import com.Unify.model.Event;
import com.Unify.service.SyncEngine;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class EventDetailController {

    @FXML
    private Circle colorDot;
    @FXML
    private Label titleLabel;
    @FXML
    private Label typeBadge;
    @FXML
    private Label startLabel;
    @FXML
    private Label endLabel;
    @FXML
    private Label locationLabel;
    @FXML
    private Label groupLabel;
    @FXML
    private Label creatorLabel;
    @FXML
    private Label descLabel;
    @FXML
    private Button editBtn;
    @FXML
    private Button deleteBtn;

    private Event event;
    private Runnable onClose;   // called after any action that changes data
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d yyyy 'at' h:mm a");
    private EventDAO eventDAO = new EventDAO();

    /**
     * Called when no refresh callback is needed.
     */
    public void setEvent(Event e) {
        setEvent(e, null);
    }

    /**
     * Called by calendars that need to refresh after edit/delete.
     */
    public void setEvent(Event e, Runnable onClose) {
        this.event = e;
        this.onClose = onClose;
        try {
            colorDot.setFill(Color.web(e.getColor()));
        } catch (Exception ignored) {
        }
        titleLabel.setText(e.getTitle());
        typeBadge.setText(e.isPersonal() ? "Personal" : "Group Event");
        startLabel.setText(e.getStartTime() != null ? e.getStartTime().format(FMT) : "—");
        endLabel.setText(e.getEndTime() != null ? e.getEndTime().format(FMT) : "—");
        locationLabel.setText(
                e.getLocation() != null && !e.getLocation().isEmpty() ? e.getLocation() : "—");
        groupLabel.setText(e.getGroupName() != null ? e.getGroupName() : "—");
        creatorLabel.setText(
                e.getCreatedByUsername() != null ? "@" + e.getCreatedByUsername() : "—");
        descLabel.setText(
                e.getDescription() != null && !e.getDescription().isEmpty()
                        ? e.getDescription() : "No description.");

        // Show edit/delete for creator OR group admin
        boolean canEdit = (e.getCreatedBy() == Session.uid());
        if (!canEdit && e.isGroup() && e.getGroupId() != null) {
            try {
                canEdit = AppData.get().isAdmin(e.getGroupId());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        editBtn.setVisible(canEdit);
        editBtn.setManaged(canEdit);
        deleteBtn.setVisible(canEdit);
        deleteBtn.setManaged(canEdit);
    }

    @FXML
    private void doEdit() {
        // Swap this modal for the event-form modal in-place (no new window)
        EventFormController ctrl = Navigator.swapWindow("/com/Unify/fxml/event_form.fxml", (Stage) titleLabel.getScene().getWindow());
        if (ctrl != null) {
            ctrl.setEvent(event);
            ctrl.setOnClose(onClose);
        }
    }

    @FXML
    private void doDelete() {
        new Alert(Alert.AlertType.CONFIRMATION, "Delete this event?", ButtonType.YES, ButtonType.NO)
                .showAndWait().ifPresent(b -> {
                    if (b == ButtonType.YES) {
                        try {
                            AppData.get().removeEvent(event.getId());
                            SyncEngine.get().push(() -> {
                                try {
                                    eventDAO.delete(event.getId());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                            Navigator.close(titleLabel, onClose);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Navigator.close(titleLabel, onClose);
                        }
                    }
                });
    }

    @FXML
    private void doClose() {
        //Navigator.closeModal(onClose);
        Navigator.close(titleLabel, onClose);
    }

}
