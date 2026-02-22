package com.calendarapp.controller;

import com.calendarapp.Navigator;
import com.calendarapp.Session;
import com.calendarapp.dao.EventDAO;
import com.calendarapp.dao.GroupDAO;
import com.calendarapp.model.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.format.DateTimeFormatter;

public class EventDetailController {

    @FXML private Circle colorDot;
    @FXML private Label  titleLabel;
    @FXML private Label  typeBadge;
    @FXML private Label  startLabel;
    @FXML private Label  endLabel;
    @FXML private Label  locationLabel;
    @FXML private Label  groupLabel;
    @FXML private Label  creatorLabel;
    @FXML private Label  descLabel;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    private Event    event;
    private Runnable onClose;   // called after any action that changes data
    private final EventDAO eventDAO = new EventDAO();
    private final GroupDAO groupDAO = new GroupDAO();
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("EEE, MMM d yyyy 'at' h:mm a");

    /** Called when no refresh callback is needed. */
    public void setEvent(Event e) {
        setEvent(e, null);
    }

    /** Called by calendars that need to refresh after edit/delete. */
    public void setEvent(Event e, Runnable onClose) {
        this.event   = e;
        this.onClose = onClose;

        try { colorDot.setFill(Color.web(e.getColor())); } catch (Exception ignored) {}
        titleLabel.setText(e.getTitle());
        typeBadge.setText(e.isPersonal() ? "Personal" : "Group Event");
        startLabel.setText(e.getStartTime() != null ? e.getStartTime().format(FMT) : "—");
        endLabel.setText(e.getEndTime()     != null ? e.getEndTime().format(FMT)   : "—");
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
            try { canEdit = groupDAO.isAdmin(e.getGroupId(), Session.uid()); }
            catch (Exception ex) { ex.printStackTrace(); }
        }
        editBtn.setVisible(canEdit);
        editBtn.setManaged(canEdit);
        deleteBtn.setVisible(canEdit);
        deleteBtn.setManaged(canEdit);
    }

    @FXML private void doEdit() {
        // Swap this modal for the event-form modal in-place (no new window)
        EventFormController ctrl = Navigator.swapModal("/com/calendarapp/fxml/event_form.fxml");
        if (ctrl != null) {
            ctrl.setEvent(event);
            ctrl.setOnClose(onClose);
        }
    }

    @FXML private void doDelete() {
        new Alert(Alert.AlertType.CONFIRMATION, "Delete this event?", ButtonType.YES, ButtonType.NO)
            .showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    try {
                        eventDAO.delete(event.getId());
                        Navigator.closeModal(onClose);
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
    }

    @FXML private void doClose() { Navigator.closeModal(onClose); }
}
