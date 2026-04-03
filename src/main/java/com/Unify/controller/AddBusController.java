package com.Unify.controller;

import com.Unify.dao.TransportDAO;
import com.Unify.model.Bus;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Time;

public class AddBusController {

    @FXML private TextField busNumberField;
    @FXML private TextField routeField;
    @FXML private TextField timeField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField messageField;
    private int currentGroupId;

    private TransportDAO transportDAO;
    private boolean isEditMode = false; // Flag to check what we are doing

    @FXML
    public void initialize() {
        transportDAO = new TransportDAO();
        typeCombo.getItems().addAll("Up Trip", "Down Trip", "Special");
        typeCombo.getSelectionModel().selectFirst();
    }

    public void setGroupId(int groupId) {
        this.currentGroupId = groupId;
    }

    // NEW METHOD: Call this from TransportController when editing
    public void initData(Bus busToEdit) {
        isEditMode = true;

        // Populate the fields with the existing data
        busNumberField.setText(busToEdit.getBusNumber());
        busNumberField.setDisable(true); // Prevent editing the Primary Key!

        routeField.setText(busToEdit.getRouteName());
        timeField.setText(busToEdit.getDepartureTime().toString());
        typeCombo.setValue(busToEdit.getTripType());
        messageField.setText(busToEdit.getMessage() != null ? busToEdit.getMessage() : "");
    }

    @FXML
    private void handleSave() {
        try {
            String busNum = busNumberField.getText().trim();
            String route = routeField.getText().trim();
            String timeStr = timeField.getText().trim();
            String type = typeCombo.getValue();
            String message = messageField.getText().trim();

            if (busNum.isEmpty() || route.isEmpty() || timeStr.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "Please fill in all required fields.");
                return;
            }

            Time departureTime = Time.valueOf(timeStr);
            Bus bus = new Bus(busNum, currentGroupId, route, departureTime, type, message);

            boolean success;
            if (isEditMode) {
                success = transportDAO.updateBus(bus);
            } else {
                success = transportDAO.addBus(bus);
            }

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", isEditMode ? "Bus updated!" : "Bus added!");
                closeWindow();
            } else {
                // If it failed and we are NOT in edit mode, it's almost certainly a duplicate Primary Key
                if (!isEditMode) {
                    showAlert(Alert.AlertType.ERROR, "Duplicate Bus", "A bus with the number '" + busNum + "' already exists! Please use a unique Bus Number.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Database Error", "Operation failed. Check your database connection.");
                }
            }

        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, "Format Error", "Please enter time in exactly HH:MM:SS format (e.g., 14:30:00).");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) busNumberField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}