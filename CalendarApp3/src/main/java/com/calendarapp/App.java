package com.calendarapp;

import com.calendarapp.dao.UserDAO;
import com.calendarapp.model.User;
import com.calendarapp.service.NotificationService;
import com.calendarapp.util.SessionStore;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Unify");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);

        // Try auto-login (remembers user for 3 days)
        int savedUid = SessionStore.loadUserId();
        if (savedUid > 0) {
            try {
                User user = new UserDAO().findById(savedUid);
                if (user != null) {
                    Session.login(user);
                    SessionStore.save(user.getId()); // refresh 3-day window
                    showMain();
                    primaryStage.show();
                    return;
                }
            } catch (Exception ignored) {}
        }

        showLogin();
        primaryStage.show();
    }

    public static void showLogin() throws IOException {
        Parent root = load("/com/calendarapp/fxml/login.fxml");
        Scene scene = new Scene(root, 900, 580);
        attachCss(scene);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle("CalendarApp — Sign In");
    }

    public static void showMain() throws IOException {
        Parent root = load("/com/calendarapp/fxml/main.fxml");
        Scene scene = new Scene(root, 1200, 780);
        attachCss(scene);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setTitle("CalendarApp — " + Session.currentUser().getDisplayName());
        NotificationService.get().start();
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
