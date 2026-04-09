package com.Unify;

import com.Unify.dao.UserDAO;
import com.Unify.model.User;
import com.Unify.service.NotificationService;
import com.Unify.service.SyncEngine;
import com.Unify.util.AsyncWriter;
import com.Unify.util.DataLoader;
import com.Unify.util.SessionStore;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

import static javafx.geometry.Pos.CENTER;


public class App extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        int savedUid = SessionStore.loadUserId();
        if (savedUid > 0) {
            User user = new UserDAO().findById(savedUid);
            if (user != null) {
                Session.login(user);
                SessionStore.save(user.getId());
                goToMain();
                primaryStage.show();
            }
        } else {
            showLogin();
            primaryStage.show();
        }
    }

    private static void showLoadingScreen() {
        ProgressIndicator spinner = new ProgressIndicator();
        Label msg = new Label("Loading your data...");
        VBox box = new VBox(16, spinner, msg);
        box.setAlignment(CENTER);
        box.setStyle("-fx-background-color:white;");
        Scene scene = new Scene(box, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static void loadDataThenShowMain() {
        try {
            showMain(); // Show the app interface immediately

            AsyncWriter.get().write(
                    () -> {
                        new DataLoader().loadAll(); // Fetch everything
                        return null;
                    },
                    (success) -> {
                        try {
                            SyncEngine.get().start(); // Start background polling after data loads
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },
                    (error) -> {
                        error.printStackTrace();
                        try {
                            showLogin();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showLogin() throws IOException {
        Parent root = load("/com/Unify/fxml/login.fxml");
        Scene scene = new Scene(root, 900, 620);
        attachCss(scene);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setTitle("Unify — Sign In");
    }

    private static void showMain() throws IOException {
        Parent root = load("/com/Unify/fxml/main.fxml");
        Scene scene = new Scene(root, 1200, 780);
        attachCss(scene);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setTitle("Unify — " + Session.currentUser().getDisplayName());
        NotificationService.get().start();
    }

    public static void goToMain() {
        showLoadingScreen(); // This will just be a fast flash now
        loadDataThenShowMain();
    }

    public static Parent load(String fxml) throws IOException {
        URL url = App.class.getResource(fxml);
        if (url == null) throw new IOException("FXML not found: " + fxml);
        return FXMLLoader.load(url);
    }

    public static FXMLLoader loader(String fxml) {
        URL url = App.class.getResource(fxml);
        return new FXMLLoader(url);
    }

    private static void attachCss(Scene scene) {
        URL css = App.class.getResource("/com/Unify/css/styles.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
    }

    @Override
    public void stop() {
        NotificationService.get().stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
