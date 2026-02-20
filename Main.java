package com.example.unify;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        try{
            Image UnifyLogo = new Image("UnifyLogo.png");
            stage.getIcons().add(UnifyLogo);
            Parent root = FXMLLoader.load(getClass().getResource("HomePage.fxml"));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
