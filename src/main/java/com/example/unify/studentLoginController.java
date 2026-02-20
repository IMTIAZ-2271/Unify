package com.example.unify;

import javafx.scene.control.PasswordField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.IOException;

public class studentLoginController {

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField usernameField;

    private Scene scene;
    private Stage stage;
    private Parent root;

    private void switchToHomePage(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("homePage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void studentLoginButtonClicked(ActionEvent event) throws IOException {
        String password = passwordField.getText();
        String username = usernameField.getText();
        if(username.equals("student") && password.equals("password")){
            JOptionPane.showMessageDialog(null, "Login successful!");
            switchToHomePage(event);
        }
        else{
            JOptionPane.showMessageDialog(null, "Invalid username or password. Please try again.");
        }

    }
    public void switchToFacultyLoginPage(ActionEvent event)throws IOException {
        root = FXMLLoader.load(getClass().getResource("facultyLogin.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }


}
