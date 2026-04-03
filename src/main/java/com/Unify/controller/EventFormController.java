package com.Unify.controller;

import com.Unify.AppData;
import com.Unify.Navigator;
import com.Unify.Session;
import com.Unify.dao.EventDAO;
import com.Unify.model.Event;
import com.Unify.model.Group;
import com.Unify.service.SyncEngine;
import com.Unify.util.ColorUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class EventFormController {

    @FXML
    private TextField titleField;
    @FXML
    private TextArea descArea;
    @FXML
    private TextField locField;
    @FXML
    private DatePicker startDate;
    @FXML
    private Spinner<Integer> startHour, startMin;
    @FXML
    private DatePicker endDate;
    @FXML
    private Spinner<Integer> endHour, endMin;
    @FXML
    private RadioButton personalRadio, groupRadio;
    @FXML
    private ComboBox<Group> groupCombo;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    private CheckBox allDayCheck;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveBtn;
    @FXML
    private Button deleteBtn;

    private Event existing;
    private Runnable onClose;
    private final EventDAO eventDAO = new EventDAO();

    @FXML
    private void initialize() {
        startHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9));
        startMin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        endHour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 10));
        endMin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        startDate.setValue(LocalDate.now());
        endDate.setValue(LocalDate.now());

        ToggleGroup tg = new ToggleGroup();
        personalRadio.setToggleGroup(tg);
        groupRadio.setToggleGroup(tg);
        personalRadio.setSelected(true);
        groupCombo.setDisable(true);

        // When radio changes, update group combo and auto-color
        groupRadio.selectedProperty().addListener((obs, ov, nv) -> {
            groupCombo.setDisable(!nv);
            if (nv) loadGroups();
            updateAutoColor();
        });
        // When group selection changes, update auto-color
        groupCombo.valueProperty().addListener((obs, ov, nv) -> updateAutoColor());

        // Start with personal color
        colorPicker.setValue(Color.web(ColorUtil.defaultPersonal()));
        errorLabel.setVisible(false);
        deleteBtn.setVisible(false);
        deleteBtn.setManaged(false);
        loadGroups();
    }

    /**
     * Update the color picker to reflect auto-assigned color (only for new events).
     */
    private void updateAutoColor() {
        if (existing != null) return; // don't override manually chosen color when editing
        try {
            if (groupRadio.isSelected() && groupCombo.getValue() != null) {
                colorPicker.setValue(Color.web(ColorUtil.forGroup(groupCombo.getValue().getId())));
            } else {
                colorPicker.setValue(Color.web(ColorUtil.defaultPersonal()));
            }
        } catch (Exception ignored) {
        }
    }

    private void loadGroups() {
        try {
            List<Group> groups = AppData.get().getGroups();
            groupCombo.getItems().setAll(groups);
            if (!groups.isEmpty() && groupCombo.getValue() == null) {
                groupCombo.setValue(groups.get(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Populate form for editing an existing event.
     */
    public void setEvent(Event e) {
        this.existing = e;
        titleField.setText(e.getTitle());
        descArea.setText(e.getDescription() != null ? e.getDescription() : "");
        locField.setText(e.getLocation() != null ? e.getLocation() : "");
        startDate.setValue(e.getStartTime().toLocalDate());
        startHour.getValueFactory().setValue(e.getStartTime().getHour());
        startMin.getValueFactory().setValue(e.getStartTime().getMinute());
        endDate.setValue(e.getEndTime().toLocalDate());
        endHour.getValueFactory().setValue(e.getEndTime().getHour());
        endMin.getValueFactory().setValue(e.getEndTime().getMinute());
        if ("group".equals(e.getEventType())) {
            groupRadio.setSelected(true);
            groupCombo.setDisable(false);
            loadGroups();
            groupCombo.getItems().stream()
                    .filter(g -> e.getGroupId() != null && g.getId() == e.getGroupId())
                    .findFirst().ifPresent(groupCombo::setValue);
        } else {
            personalRadio.setSelected(true);
        }
        try {
            colorPicker.setValue(Color.web(e.getColor()));
        } catch (Exception ignored) {
        }
        allDayCheck.setSelected(e.isAllDay());
        deleteBtn.setVisible(true);
        deleteBtn.setManaged(true);
    }

    /**
     * Pre-select a specific group (called from GroupDetailController).
     */
    public void setGroup(Group g) {
        groupRadio.setSelected(true);
        groupCombo.setDisable(false);
        loadGroups();
        groupCombo.getItems().stream()
                .filter(gr -> gr.getId() == g.getId())
                .findFirst().ifPresent(groupCombo::setValue);
        updateAutoColor();
    }

    /**
     * Set default date for new events (called from calendar day-click).
     */
    public void setDefaultDate(LocalDate d) {
        startDate.setValue(d);
        endDate.setValue(d);
    }

    /**
     * Callback to run after save/delete/cancel (e.g. re-render the calendar).
     */
    public void setOnClose(Runnable r) {
        this.onClose = r;
    }

//    @FXML private void doSave() {
//        String title = titleField.getText().trim();
//        if (title.isEmpty()) { err("Title is required."); return; }
//        if (startDate.getValue() == null || endDate.getValue() == null) {
//            err("Select start and end dates."); return;
//        }
//
//        LocalDateTime st = LocalDateTime.of(
//            startDate.getValue(), LocalTime.of(startHour.getValue(), startMin.getValue()));
//        LocalDateTime et = LocalDateTime.of(
//            endDate.getValue(),   LocalTime.of(endHour.getValue(),   endMin.getValue()));
//        if (!et.isAfter(st)) { err("End time must be after start time."); return; }
//
//        Color  c   = colorPicker.getValue();
//        String hex = String.format("#%02X%02X%02X",
//            (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
//
//        Event ev = (existing != null) ? existing : new Event();
//        ev.setTitle(title);
//        ev.setDescription(descArea.getText().trim());
//        ev.setLocation(locField.getText().trim());
//        ev.setStartTime(st);
//        ev.setEndTime(et);
//        ev.setColor(hex);
//        ev.setAllDay(allDayCheck.isSelected());
//        ev.setCreatedBy(Session.uid());
//
//        if (groupRadio.isSelected() && groupCombo.getValue() != null) {
//            ev.setEventType("group");
//            ev.setGroupId(groupCombo.getValue().getId());
//        } else {
//            ev.setEventType("personal");
//            ev.setGroupId(null);
//        }
//
//        try {
//            if (existing != null) {
//                eventDAO.update(ev);
//            } else {
//                Event created = eventDAO.create(ev);
//                if (created != null && created.isGroup() && created.getGroupId() != null) {
//                    // Notify all group members about the new event
//                    notifDAO.createForGroup(
//                        created.getGroupId(),
//                        "New Event: " + created.getTitle(),
//                        "Starts: " + created.getStartTime().format(
//                            DateTimeFormatter.ofPattern("MMM d 'at' h:mm a")),
//                        "event_created",
//                        created.getId(),
//                        Session.uid());
//                }
//            }
//            //Navigator.closeModal(onClose);
//            Navigator.close(titleField,onClose);
//        } catch (Exception e) {
//            err("Error saving event: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    @FXML
    private void doSave() {

        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            err("Title is required.");
            return;
        }
        if (startDate.getValue() == null || endDate.getValue() == null) {
            err("Select start and end dates.");
            return;
        }

        LocalDateTime st = LocalDateTime.of(
                startDate.getValue(), LocalTime.of(startHour.getValue(), startMin.getValue()));
        LocalDateTime et = LocalDateTime.of(
                endDate.getValue(), LocalTime.of(endHour.getValue(), endMin.getValue()));
        if (!et.isAfter(st)) {
            err("End time must be after start time.");
            return;
        }

        Color c = colorPicker.getValue();
        String hex = String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));

        Event ev = (existing != null) ? existing : new Event();
        ev.setTitle(title);
        ev.setDescription(descArea.getText().trim());
        ev.setLocation(locField.getText().trim());
        ev.setStartTime(st);
        ev.setEndTime(et);
        ev.setColor(hex);
        ev.setAllDay(allDayCheck.isSelected());
        ev.setCreatedBy(Session.uid());

        if (groupRadio.isSelected() && groupCombo.getValue() != null) {
            ev.setEventType("group");
            ev.setGroupId(groupCombo.getValue().getId());
        } else {
            ev.setEventType("personal");
            ev.setGroupId(null);
        }

        // ... validate ...
        try {
            if (existing != null) {
                eventDAO.update(ev);              // update in DB immediately (async)
                AppData.get().addOrUpdateEvent(ev); // update in memory instantly
            } else {
                // Optimistic: add to AppData now, send to DB in background
                SyncEngine.get().push(() -> {
                    try {
                        Event created = eventDAO.create(ev);
                        if (created != null) {
                            AppData.get().addOrUpdateEvent(created);
                            // send group notifications...
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            Navigator.close(titleField, onClose);
        } catch (Exception e) {
            err(e.getMessage());
        }
    }

    @FXML
    private void doDelete() {
        if (existing == null) return;
        new Alert(Alert.AlertType.CONFIRMATION, "Delete this event?", ButtonType.YES, ButtonType.NO)
                .showAndWait().ifPresent(b -> {
                    if (b == ButtonType.YES) {
                        try {
                            //eventDAO.delete(existing.getId());
                            //Navigator.closeModal(onClose);
                            AppData.get().removeEvent(existing.getId());
                            Navigator.close(titleField, onClose);
                        } catch (Exception e) {
                            err(e.getMessage());
                        }
                    }
                });
    }

    @FXML
    private void doCancel() {
        //Navigator.closeModal(onClose);
        Navigator.close(titleField, onClose);
    }

    private void err(String m) {
        errorLabel.setText(m);
        errorLabel.setVisible(true);
    }
}
