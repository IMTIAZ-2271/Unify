package com.example.unify;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.IOException;

public class facultyLoginController {
    @FXML
    private PasswordField facultyPasswordField;

    @FXML
    private TextField facultyUsernameField;

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





    public void facultyLoginButtonClicked(ActionEvent event) throws IOException {
        String password = facultyPasswordField.getText();
        String username = facultyUsernameField.getText();
        if(username.equals("faculty") && password.equals("1234")){
            JOptionPane.showMessageDialog(null, "Login successful!");
            switchToHomePage(event);
        }
        else{
            JOptionPane.showMessageDialog(null, "Invalid username or password. Please try again.");
        }

    }
}
