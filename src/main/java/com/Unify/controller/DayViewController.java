package com.Unify.controller;

import com.Unify.AppData;
import com.Unify.Navigator;
import com.Unify.model.Event;
import com.Unify.util.ColorUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


public class DayViewController {

    @FXML
    private Label dateLabel;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private AnchorPane timePane;

    private LocalDate date;

    private static final double PX_PER_MIN = 1.4;
    private static final double TIME_COL = 60.0;
    private static final DateTimeFormatter T = DateTimeFormatter.ofPattern("h:mm a");

    @FXML
    private void initialize() {
        // setDate() is called externally after push(); do nothing here.
        AppData.get().addEventsListener(this::build);
    }

    public LocalDate getDate() {
        return date;
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
                List<Event> allEvents = AppData.get().getEventsForDay(date);
                List<Event> visibleEvents = new java.util.ArrayList<>();

                // Filter visible events
                for (Event e : allEvents) {
                    boolean showGroup = !e.isPersonal() && MonthViewController.groupFilter.getOrDefault(e.getGroupId(), true);
                    boolean showPers = e.isPersonal() && MonthViewController.showPersonal;
                    if (showGroup || showPers) {
                        visibleEvents.add(e);
                    }
                }

                // Sort by Start Time, then End Time
                visibleEvents.sort(java.util.Comparator
                        .comparing((Event e) -> e.getStartTime().toLocalTime())
                        .thenComparing(e -> e.getEndTime().toLocalTime()));

                // Send to algorithm
                layoutEvents(visibleEvents);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private void layoutEvents(List<Event> events) {
        if (events.isEmpty()) return;

        List<Event> currentCluster = new java.util.ArrayList<>();
        long maxClusterEnd = getEndMin(events.get(0));

        for (Event e : events) {
            long start = getStartMin(e);
            long end = getEndMin(e);

            if (start < maxClusterEnd) {
                currentCluster.add(e);
                if (end > maxClusterEnd) {
                    maxClusterEnd = end;
                }
            } else {
                renderCluster(currentCluster);
                currentCluster.clear();
                currentCluster.add(e);
                maxClusterEnd = end;
            }
        }
        if (!currentCluster.isEmpty()) {
            renderCluster(currentCluster);
        }
    }

    private void placeEventNode(Event e, double x, double width) {
        long startMin = getStartMin(e);
        long endMin = getEndMin(e);

        double y = startMin * PX_PER_MIN;
        long dur = endMin - startMin;
        if (dur <= 0) dur = 30;
        double h = Math.max(dur * PX_PER_MIN - 2, 24);

        String displayColor = e.isGroup() && e.getGroupId() != null
                ? ColorUtil.forGroup(e.getGroupId())
                : e.getColor();

        VBox box = new VBox(2);
        box.setLayoutX(x);
        box.setLayoutY(y + 1);
        box.setPrefWidth(width);
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

        // Show dates in the label if it spans multiple days
        String timeStr;
        if (e.getStartTime().toLocalDate().equals(e.getEndTime().toLocalDate())) {
            timeStr = e.getStartTime().format(T) + " – " + e.getEndTime().format(T);
        } else {
            DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("MMM d, h:mm a");
            timeStr = e.getStartTime().format(dtFmt) + " – " + e.getEndTime().format(dtFmt);
        }

        Label time = new Label(timeStr);
        time.setStyle("-fx-font-size:10;-fx-text-fill:#475569;");
        box.getChildren().addAll(title, time);

        if (e.getLocation() != null && !e.getLocation().isEmpty() && h > 45) {
            Label loc = new Label("📍 " + e.getLocation());
            loc.setStyle("-fx-font-size:10;-fx-text-fill:#64748B;");
            box.getChildren().add(loc);
        }

        Tooltip.install(box, new Tooltip(e.getTitle() + "\n" + timeStr));
        box.setOnMouseClicked(ev -> openDetail(e));
        timePane.getChildren().add(box);
    }

    private void renderCluster(List<Event> cluster) {
        List<List<Event>> columns = new java.util.ArrayList<>();

        for (Event e : cluster) {
            boolean placed = false;
            long start = getStartMin(e);

            for (List<Event> col : columns) {
                Event lastEventInCol = col.get(col.size() - 1);
                long colEnd = getEndMin(lastEventInCol);

                if (start >= colEnd) {
                    col.add(e);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<Event> newCol = new java.util.ArrayList<>();
                newCol.add(e);
                columns.add(newCol);
            }
        }

        double totalAvailWidth = 730 - TIME_COL - 10;
        int numCols = columns.size();
        double colWidth = totalAvailWidth / numCols;

        for (int colIdx = 0; colIdx < numCols; colIdx++) {
            List<Event> col = columns.get(colIdx);
            for (Event e : col) {
                double x = TIME_COL + 4 + (colIdx * colWidth);
                placeEventNode(e, x, colWidth - 2);
            }
        }
    }

    private void openDetail(Event e) {
        EventDetailController ctrl = Navigator.showWindow("/com/Unify/fxml/event_detail.fxml");
        if (ctrl != null) {
            ctrl.setEvent(e, this::build);
        }
    }

    @FXML
    private void newEvent() {
        EventFormController ctrl = Navigator.showWindow("/com/Unify/fxml/event_form.fxml");
        if (ctrl != null) {
            ctrl.setDefaultDate(date != null ? date : LocalDate.now());
            ctrl.setOnClose(this::build);
        }
    }

    @FXML
    private void gotoMonthView(ActionEvent event) {
        Navigator.goTo("/com/Unify/fxml/month_view.fxml");
    }

    @FXML
    private void gotoPreviousDay(ActionEvent event) {
        setDate(date.minusDays(1));
    }

    @FXML
    private void gotoNextDay(ActionEvent event) {
        setDate(date.plusDays(1));
    }
    private long getStartMin(Event e) {
        LocalDateTime startOfDay = date.atStartOfDay();
        if (e.getStartTime().isBefore(startOfDay)) return 0; // Started on a previous day
        return java.time.Duration.between(startOfDay, e.getStartTime()).toMinutes();
    }

    private long getEndMin(Event e) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        if (!e.getEndTime().isBefore(endOfDay)) return 1440; // Ends on a future day
        return java.time.Duration.between(startOfDay, e.getEndTime()).toMinutes();
    }
}
