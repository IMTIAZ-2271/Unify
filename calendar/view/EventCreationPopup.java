package com.example.unify.calendar.view;

import com.example.unify.calendar.controller.CalendarEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

public class EventCreationPopup {

    // We use a "Consumer" callback to send the created event back to the Controller
    public void show(Window owner, LocalDate date, double x, double y, Consumer<CalendarEvent> onSave) {
        Popup popup = new Popup();
        popup.setAutoHide(true);


        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: white; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0); " +
                "-fx-background-radius: 8; " +
                "-fx-border-color: #e0e0e0; " +
                "-fx-border-radius: 8;");
        root.setPrefWidth(280);


        Label header = new Label("Add Event: " + date.toString());
        header.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

        TextField titleField = new TextField();
        titleField.setPromptText("Event Title");
        titleField.setStyle("-fx-background-color: transparent; -fx-border-width: 0 0 1 0; -fx-border-color: #ccc;");

        TextField startTimeField = new TextField();
        startTimeField.setPromptText("Start (09:00)");

        TextField endTimeField = new TextField();
        endTimeField.setPromptText("End (10:00)");

        Button saveBtn = new Button("Save Event");
        saveBtn.setStyle("-fx-background-color: #4285F4; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        // --- Save Action ---
        saveBtn.setOnAction(e -> {
            try {
                String title = titleField.getText().trim();
                if (title.isEmpty()) {
                    titleField.setStyle("-fx-border-color: red; -fx-border-width: 0 0 1 0;"); // Error visual
                    return;
                }

                // Parse Times
                LocalTime start = LocalTime.parse(startTimeField.getText());
                LocalTime end = LocalTime.parse(endTimeField.getText());

                // Create the event object
                CalendarEvent newEvent = new CalendarEvent(date, start, end, title);

                // Send it back to the controller!
                onSave.accept(newEvent);

                popup.hide();

            } catch (DateTimeParseException ex) {
                startTimeField.setStyle("-fx-border-color: red;"); // Simple error feedback
                System.out.println("Invalid time format.");
            }
        });

        root.getChildren().addAll(header, titleField, startTimeField, endTimeField, saveBtn);
        popup.getContent().add(root);
        popup.show(owner, x, y);
        titleField.requestFocus();
    }
}