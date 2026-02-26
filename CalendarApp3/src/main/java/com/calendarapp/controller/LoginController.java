package com.calendarapp.controller;

import com.calendarapp.App;
import com.calendarapp.Session;
import com.calendarapp.dao.UserDAO;
import com.calendarapp.model.User;
import com.calendarapp.util.SessionStore;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField    usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label        errorLabel;
    @FXML private Button       loginBtn;

    private final UserDAO userDAO = new UserDAO();

    @FXML private void initialize() {
        errorLabel.setVisible(false);
        passwordField.setOnAction(e -> doLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());
    }

    @FXML private void doLogin() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (u.isEmpty() || p.isEmpty()) { err("Please fill in all fields."); return; }
        loginBtn.setDisable(true);
        try {
            User user = userDAO.authenticate(u, p);
            if (user != null) {
                Session.login(user);
                SessionStore.save(user.getId());   // remember for 3 days
                App.goToMain();
            } else {
                err("Invalid username or password.");
            }
        } catch (Exception ex) {
            err("DB error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            loginBtn.setDisable(false);
        }
    }

    @FXML private void goRegister() {
        try {
            Parent root = App.load("/com/calendarapp/fxml/register.fxml");
            Scene scene = new Scene(root, 900, 620);
            scene.getStylesheets().add(App.class.getResource("/com/calendarapp/css/styles.css").toExternalForm());
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Unify — Register");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void err(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
}
