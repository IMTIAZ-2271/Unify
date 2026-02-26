package com.calendarapp;

import com.calendarapp.dao.UserDAO;
import com.calendarapp.model.User;
import com.calendarapp.service.NotificationService;
import com.calendarapp.service.SyncEngine;
import com.calendarapp.util.DataLoader;
import com.calendarapp.util.SessionStore;
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
        // Simple spinner while data loads
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
        new Thread(() -> {
            try {
                new DataLoader().loadAll();            // fetch everything
                Platform.runLater(() -> {
                    try {
                        showMain();                    // now show the app
                        SyncEngine.get().start();      // start polling
                    } catch (Exception e) { e.printStackTrace(); }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    try { showLogin(); }
                    catch (Exception ex) { ex.printStackTrace(); }
                });
            }
        }, "data-loader").start();
    }

    public static void showLogin() throws IOException {
        Parent root = load("/com/calendarapp/fxml/login.fxml");
        Scene scene = new Scene(root, 900, 620);
        attachCss(scene);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Unify — Sign In");
    }

    private static void showMain() throws IOException {
        Parent root = load("/com/calendarapp/fxml/main.fxml");
        Scene scene = new Scene(root, 1200, 780);
        attachCss(scene);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Unify — " + Session.currentUser().getDisplayName());
        NotificationService.get().start();
    }

    public static void goToMain(){
        showLoadingScreen();
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
        URL css = App.class.getResource("/com/calendarapp/css/styles.css");
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
