package com.example.unify;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class homePageController {

    private Parent loginPage;
    private Scene  loginScene;
    private Stage loginStage;
    @FXML
    private AnchorPane contentPane;

    @FXML
    public void initialize() {
        loadCalendar();
    }

    private void loadCalendar() {
        try {
            BorderPane calendarView = FXMLLoader.load(
                    getClass().getResource("calendar.fxml")
            );

            calendarView.setPrefSize(630, 560);
            AnchorPane.setTopAnchor(calendarView, 100.0);
            //AnchorPane.setBottomAnchor(calendarView, 700.0);
            AnchorPane.setLeftAnchor(calendarView, 200.0);
            //AnchorPane.setRightAnchor(calendarView, 930.0);

            contentPane.getChildren().setAll(calendarView);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logoutButtonClicked(ActionEvent event) throws IOException  {
        try{
            loginPage = FXMLLoader.load(getClass().getResource("studentLogin.fxml"));
            loginStage = (Stage)((Node)event.getSource()).getScene().getWindow();
            loginScene = new Scene(loginPage);
            loginStage.setScene(loginScene);
            loginStage.show();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}
