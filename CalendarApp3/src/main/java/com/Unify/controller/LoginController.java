package com.Unify.controller;

import com.Unify.App;
import com.Unify.Session;
import com.Unify.dao.UserDAO;
import com.Unify.model.User;
import com.Unify.util.SessionStore;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button loginBtn;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
        passwordField.setOnAction(e -> doLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());
    }

    @FXML
    private void doLogin() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (u.isEmpty() || p.isEmpty()) {
            err("Please fill in all fields.");
            return;
        }
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

    @FXML
    private void goRegister() {
        try {
            Parent root = App.load("/com/Unify/fxml/register.fxml");
            Scene scene = new Scene(root, 900, 620);
            scene.getStylesheets().add(App.class.getResource("/com/Unify/css/styles.css").toExternalForm());
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Unify — Register");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void err(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
