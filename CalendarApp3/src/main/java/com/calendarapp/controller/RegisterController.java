package com.calendarapp.controller;

import com.calendarapp.App;
import com.calendarapp.Session;
import com.calendarapp.dao.UserDAO;
import com.calendarapp.model.User;
import com.calendarapp.util.Crypto;
import com.calendarapp.util.SessionStore;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField     usernameField;
    @FXML private TextField     emailField;
    @FXML private TextField     displayNameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label         errorLabel;
    @FXML private Button        registerBtn;

    private final UserDAO userDAO = new UserDAO();

    @FXML private void initialize() { errorLabel.setVisible(false); }

    @FXML private void doRegister() {
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String display  = displayNameField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) { err("Fill in all required fields."); return; }
        if (!Crypto.validUsername(username)) { err("Username: 3-50 chars, letters/numbers/underscore only."); return; }
        if (!Crypto.validEmail(email))       { err("Enter a valid email address."); return; }
        if (!Crypto.validPassword(password)) { err("Password must be at least 6 characters."); return; }
        if (!password.equals(confirm))       { err("Passwords do not match."); return; }

        registerBtn.setDisable(true);
        try {
            if (userDAO.usernameExists(username)) { err("Username already taken."); return; }
            if (userDAO.emailExists(email))        { err("Email already registered."); return; }
            User user = userDAO.create(username, email, password, display.isEmpty() ? username : display);
            if (user != null) {
                Session.login(user);
                SessionStore.save(user.getId());
                App.showMain();
            }
        } catch (Exception ex) {
            err("Error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            registerBtn.setDisable(false);
        }
    }

    @FXML private void goLogin() {
        try {
            Parent root = App.load("/com/calendarapp/fxml/login.fxml");
            Scene scene = new Scene(root, 900, 580);
            scene.getStylesheets().add(App.class.getResource("/com/calendarapp/css/styles.css").toExternalForm());
            Stage stage = (Stage) registerBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("CalendarApp — Sign In");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void err(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
}
