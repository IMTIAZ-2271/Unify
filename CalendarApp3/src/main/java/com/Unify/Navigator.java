package com.Unify;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Single-window navigation system.
 * <p>
 * goTo()       – swap the main content area (sidebar nav, clears back stack)
 * push()       – push a detail page with a Back button; returns controller
 * pop()        – go back
 * showModal()  – show a form/dialog as a centered overlay; returns controller
 * swapModal()  – replace the current modal content; returns controller
 * closeModal() – close the overlay (optionally runs a Runnable after)
 */
public class Navigator {

    private static StackPane contentArea;
    private static StackPane overlayArea;
    private static final Deque<Node> backStack = new ArrayDeque<>();

    /**
     * Called once from MainController.initialize()
     */
    public static void init(StackPane content, StackPane overlay) {
        contentArea = content;
        overlayArea = overlay;
        overlayArea.setVisible(false);
        overlayArea.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayArea) closeWindow();
        });
    }

    // ── Top-level navigation (sidebar) ───────────────────────────────────

    public static void goTo(String fxml) {
        try {
            Node node = App.loader(fxml).load();
            backStack.clear();
            setContent(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Push/pop detail pages ─────────────────────────────────────────────

    public static <T> T push(String fxml) {
        try {
            FXMLLoader loader = App.loader(fxml);
            Node node = loader.load();
            if (!contentArea.getChildren().isEmpty()) {
                backStack.push(contentArea.getChildren().getFirst());
            }
            setContent(wrapWithBack(node));
            return loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void pop() {
        if (!backStack.isEmpty()) setContent(backStack.pop());
        else backStack.clear();
    }

    public static boolean canPop() {
        return !backStack.isEmpty();
    }

    // ── Modal overlay ─────────────────────────────────────────────────────

    public static <T> T showWindow(String fxml) {
        try {
            FXMLLoader loader = App.loader(fxml);
            Parent root = loader.load();
            Stage stage = new Stage();
            Scene scene = new Scene(root, 480, 600);
            scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/com/Unify/css/styles.css")).toExternalForm());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
            //st.showAndWait();
            return loader.getController();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }


    /**
     * Replace modal content without animation (for edit→form transitions).
     */
    public static <T> T swapWindow(String fxml, Stage currentStage) {
        try {
            FXMLLoader loader = App.loader(fxml);
            Parent scene = loader.load();
            currentStage.setScene(new Scene(scene));
            currentStage.show();
            return loader.getController();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void closeWindow() {
        closeWindow(null);
    }

    public static void closeWindow(Runnable afterClose) {
        FadeTransition ft = new FadeTransition(Duration.millis(120), overlayArea);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            overlayArea.setVisible(false);
            overlayArea.getChildren().clear();
            if (afterClose != null) afterClose.run();
        });
        ft.play();
    }

    public static void close(Control control, Runnable afterClose) {
        Stage currentStage = (Stage) control.getScene().getWindow();
        if (afterClose != null) afterClose.run();
        currentStage.close();
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private static void setContent(Node node) {
        contentArea.getChildren().setAll(node);
    }

    private static Node wrapWithBack(Node content) {
        VBox wrapper = new VBox(0);
        wrapper.setStyle("-fx-background-color:white;");
        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> pop());
        VBox.setVgrow(content, Priority.ALWAYS);
        wrapper.getChildren().addAll(backBtn, content);
        return wrapper;
    }
}
