package com.calendarapp.controller;

import com.calendarapp.Navigator;
import com.calendarapp.Session;
import com.calendarapp.dao.EventDAO;
import com.calendarapp.model.Event;
import com.calendarapp.util.ColorUtil;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DayViewController {

    @FXML private Label       dateLabel;
    @FXML private ScrollPane  scrollPane;
    @FXML private AnchorPane  timePane;

    private LocalDate date;
    private final EventDAO dao = new EventDAO();

    private static final double PX_PER_MIN = 1.4;
    private static final double TIME_COL   = 60.0;
    private static final DateTimeFormatter T = DateTimeFormatter.ofPattern("h:mm a");

    @FXML private void initialize() {
        // setDate() is called externally after push(); do nothing here.
    }

    public void setDate(LocalDate d) {
        this.date = d;
        dateLabel.setText(d.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        build();
        // Scroll to current time if today, else 8am
        double totalPx = 24 * 60 * PX_PER_MIN;
        double nowY;
        if (d.equals(LocalDate.now())) {
            nowY = (LocalTime.now().getHour() * 60 + LocalTime.now().getMinute()) * PX_PER_MIN;
        } else {
            nowY = 8 * 60 * PX_PER_MIN;
        }
        scrollPane.setVvalue(Math.max(0, Math.min(1.0, (nowY - 100) / totalPx)));
    }

    private void build() {
        timePane.getChildren().clear();
        double totalH = 24 * 60 * PX_PER_MIN;
        timePane.setPrefHeight(totalH);

        for (int h = 0; h < 24; h++) {
            double y = h * 60 * PX_PER_MIN;

            Label lbl = new Label(String.format("%02d:00", h));
            lbl.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:11;");
            lbl.setLayoutX(4);
            lbl.setLayoutY(y - 8);
            lbl.setPrefWidth(TIME_COL - 8);

            Rectangle hr = new Rectangle(700, 1);
            hr.setLayoutX(TIME_COL);
            hr.setLayoutY(y);
            hr.setFill(Color.web("#E2E8F0"));

            Rectangle hh = new Rectangle(700, 1);
            hh.setLayoutX(TIME_COL);
            hh.setLayoutY(y + 30 * PX_PER_MIN);
            hh.setFill(Color.web("#F1F5F9"));

            timePane.getChildren().addAll(lbl, hr, hh);
        }

        // Current time indicator
        if (date != null && date.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();
            double ny = (now.getHour() * 60 + now.getMinute()) * PX_PER_MIN;
            Rectangle line = new Rectangle(714, 2);
            line.setLayoutX(TIME_COL - 4);
            line.setLayoutY(ny);
            line.setFill(Color.web("#EF4444"));
            timePane.getChildren().add(line);
        }

        // Events
        try {
            if (date != null) {
                List<Event> events = dao.forDay(Session.uid(), date);
                for (Event e : events) placeEvent(e);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void placeEvent(Event e) {
        LocalTime start = e.getStartTime().toLocalTime();
        LocalTime end   = e.getEndTime().toLocalTime();
        double y    = (start.getHour() * 60 + start.getMinute()) * PX_PER_MIN;
        int    dur  = (int) java.time.Duration.between(start, end).toMinutes();
        if (dur <= 0) dur = 30;
        double h    = Math.max(dur * PX_PER_MIN - 2, 24);

        // Auto-color for group events
        String displayColor = e.isGroup() && e.getGroupId() != null
            ? ColorUtil.forGroup(e.getGroupId())
            : e.getColor();

        VBox box = new VBox(2);
        box.setLayoutX(TIME_COL + 4);
        box.setLayoutY(y + 1);
        box.setPrefWidth(650);
        box.setPrefHeight(h);
        box.setMaxHeight(h);
        box.setPadding(new Insets(3, 6, 3, 8));
        box.setStyle(
            "-fx-background-color:" + displayColor + "33;"
            + "-fx-border-color:" + displayColor + ";"
            + "-fx-border-width:0 0 0 4;"
            + "-fx-background-radius:4;");
        box.setCursor(javafx.scene.Cursor.HAND);

        Label title = new Label(e.getTitle());
        title.setStyle("-fx-font-weight:bold;-fx-font-size:12;-fx-text-fill:#1E293B;");
        Label time  = new Label(start.format(T) + " – " + end.format(T));
        time.setStyle("-fx-font-size:10;-fx-text-fill:#475569;");
        box.getChildren().addAll(title, time);

        if (e.getLocation() != null && !e.getLocation().isEmpty() && h > 45) {
            Label loc = new Label("📍 " + e.getLocation());
            loc.setStyle("-fx-font-size:10;-fx-text-fill:#64748B;");
            box.getChildren().add(loc);
        }

        Tooltip.install(box, new Tooltip(e.getTitle() + "\n" + start.format(T) + " – " + end.format(T)));
        box.setOnMouseClicked(ev -> openDetail(e));
        timePane.getChildren().add(box);
    }

    private void openDetail(Event e) {
        EventDetailController ctrl = Navigator.showModal("/com/calendarapp/fxml/event_detail.fxml");
        if (ctrl != null) ctrl.setEvent(e, this::build);
    }

    @FXML private void newEvent() {
        EventFormController ctrl = Navigator.showModal("/com/calendarapp/fxml/event_form.fxml");
        if (ctrl != null) {
            ctrl.setDefaultDate(date != null ? date : LocalDate.now());
            ctrl.setOnClose(this::build);
        }
    }
}
