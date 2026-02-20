package com.example.unify.calendar.view;

import com.example.unify.calendar.controller.CalendarEvent;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.YearMonth;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseButton;

public class MonthlyEventView {


    private Label monthLabel;
    private Button prevMonthBtn;
    private Button nextMonthBtn;
    private GridPane calendarGrid;

    private YearMonth currentMonth;
    private LocalDate selectedDate;

    // Stores events where the key is a specific Date
    private Map<LocalDate, List<CalendarEvent>> eventMap = new HashMap<>();

    // Constructor to receive UI elements from the Controller
    public MonthlyEventView(Label monthLabel, Button prevMonthBtn, Button nextMonthBtn, GridPane calendarGrid) {
        this.monthLabel = monthLabel;
        this.prevMonthBtn = prevMonthBtn;
        this.nextMonthBtn = nextMonthBtn;
        this.calendarGrid = calendarGrid;
    }

    public void initialize() {
        // Set initial state to the current real-world month and day
        currentMonth = YearMonth.now();
        selectedDate = LocalDate.now();

        renderCalendar();

        // Actions to navigate between months
        prevMonthBtn.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            renderCalendar();
        });

        nextMonthBtn.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            renderCalendar();
        });
    }

    private void renderCalendar() {
        calendarGrid.getChildren().clear();
        monthLabel.setText(currentMonth.getMonth().toString() + " " + currentMonth.getYear());

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int daysInMonth = currentMonth.lengthOfMonth();

        // Calculate empty slots before the 1st of the month
        int leadingDays = firstOfMonth.getDayOfWeek().getValue() - 1;

        int row = 0;
        int col = 0;

        // 1. Fill trailing days of the previous month
        LocalDate prevMonthDate = firstOfMonth.minusDays(leadingDays);
        for (int i = 0; i < leadingDays; i++) {
            calendarGrid.add(createDayCell(prevMonthDate, false), col++, row);
            prevMonthDate = prevMonthDate.plusDays(1);
        }

        // 2. Fill actual days of the current month
        for (int day = 1; day <= daysInMonth; day++) {
            calendarGrid.add(createDayCell(currentMonth.atDay(day), true), col, row);
            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }

        // 3. Fill leading days of the next month to complete the grid
        LocalDate nextMonthDate = currentMonth.atEndOfMonth().plusDays(1);
        while (row < 6) {
            calendarGrid.add(createDayCell(nextMonthDate, false), col++, row);
            if (col == 7) {
                col = 0;
                row++;
            }
            nextMonthDate = nextMonthDate.plusDays(1);
        }
    }

    private StackPane createDayCell(LocalDate date, boolean isCurrentMonth) {
        StackPane cell = new StackPane();
        cell.setPrefSize(90, 80);
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setAlignment(Pos.TOP_RIGHT);
        cell.setPadding(new Insets(5));

        String baseStyle = "-fx-border-width: 0.5; ";

        // Style the cell based on its state
        if (!isCurrentMonth) {
            cell.setStyle(baseStyle + "-fx-border-color: #eeeeee; -fx-background-color: #fafafa;");
        } else {
            if (date.equals(selectedDate)) {
                cell.setStyle(baseStyle + "-fx-border-color: #4285F4; -fx-background-color: #bddaf0;");
            } else if (date.equals(LocalDate.now())) {
                cell.setStyle(baseStyle + "-fx-border-color: green; -fx-background-color: #e8f5e9;");
            } else {
                cell.setStyle(baseStyle + "-fx-border-color: #eeeeee; -fx-background-color: white;");
            }
        }

        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        if (!isCurrentMonth) dayLabel.setStyle("-fx-text-fill: lightgray;");

        // Container to stack multiple events vertically in the cell
        VBox eventBox = new VBox(2);
        eventBox.setAlignment(Pos.BOTTOM_LEFT);
        eventBox.setPadding(new Insets(20, 0, 0, 2));

        // Display events for this date
        if (eventMap.containsKey(date)) {
            for (CalendarEvent event : eventMap.get(date)) {
                Label eventLabel = new Label(event.getStartTime() + " " + event.getDescription());
                eventLabel.setStyle("-fx-background-color: #4285F4; -fx-text-fill: white; -fx-font-size: 5px; -fx-background-radius: 3; -fx-padding: 1;");
                eventLabel.setMaxWidth(40);
                eventBox.getChildren().add(eventLabel);
            }
        }

        cell.getChildren().addAll(eventBox, dayLabel);

        // Handle Mouse Clicks
        cell.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                selectedDate = date;
                renderCalendar();
            } else if (e.getButton() == MouseButton.SECONDARY && isCurrentMonth) {
                new EventCreationPopup().show(
                        calendarGrid.getScene().getWindow(),
                        date,
                        e.getScreenX(),
                        e.getScreenY(),
                        (newEvent) -> {
                            eventMap.computeIfAbsent(date, k -> new ArrayList<>()).add(newEvent);
                            renderCalendar();
                        }
                );
            }
        });

        return cell;
    }
}