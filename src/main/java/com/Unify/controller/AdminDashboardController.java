package com.Unify.controller;

import com.Unify.Session;
import com.Unify.dao.UserDAO;
import com.Unify.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminDashboardController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, Boolean> colAdmin;

    private UserDAO userDAO;

    @FXML
    public void initialize() {
        userDAO = new UserDAO();
        loadUsers();
        setupContextMenu();
    }

    private void loadUsers() {
        try {
            List<User> users = userDAO.getAllUsers();
            ObservableList<User> userObservableList = FXCollections.observableArrayList(users);
            userTable.setItems(userObservableList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem toggleAdminItem = new MenuItem("👑 Toggle Admin Status");
        toggleAdminItem.setOnAction(event -> {
            User selectedUser = userTable.getSelectionModel().getSelectedItem();

            if (selectedUser != null) {
                // Prevent the admin from accidentally demoting themselves
                if (selectedUser.getId() == Session.uid()) {
                    showAlert(Alert.AlertType.WARNING, "Action Denied", "You cannot change your own admin status.");
                    return;
                }

                // Determine what the NEW status should be (the opposite of current)
                boolean newStatus = !selectedUser.isAdmin();
                String actionText = newStatus ? "PROMOTE to Admin" : "DEMOTE to Standard User";

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to " + actionText + " " + selectedUser.getUsername() + "?");
                Optional<ButtonType> result = confirm.showAndWait();

                if (result.isPresent() && result.get() == ButtonType.OK) {
                    if (userDAO.setAdminStatus(selectedUser.getId(), newStatus)) {
                        // Refresh the table to show the new status
                        loadUsers();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update user status.");
                    }
                }
            }
        });

        contextMenu.getItems().add(toggleAdminItem);
        userTable.setContextMenu(contextMenu);
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}