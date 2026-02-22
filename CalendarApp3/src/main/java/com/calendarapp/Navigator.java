package com.calendarapp;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Single-window navigation system.
 *
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

    /** Called once from MainController.initialize() */
    public static void init(StackPane content, StackPane overlay) {
        contentArea = content;
        overlayArea = overlay;
        overlayArea.setVisible(false);
        overlayArea.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayArea) closeModal();
        });
    }

    // ── Top-level navigation (sidebar) ───────────────────────────────────

    public static void goTo(String fxml) {
        try {
            Node node = App.loader(fxml).load();
            backStack.clear();
            setContent(node);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Push/pop detail pages ─────────────────────────────────────────────

    public static <T> T push(String fxml) {
        try {
            FXMLLoader loader = App.loader(fxml);
            Node node = loader.load();
            if (!contentArea.getChildren().isEmpty()) {
                backStack.push(contentArea.getChildren().get(0));
            }
            setContent(wrapWithBack(node));
            return loader.getController();
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public static void pop() {
        if (!backStack.isEmpty()) setContent(backStack.pop());
        else backStack.clear();
    }

    public static boolean canPop() { return !backStack.isEmpty(); }

    // ── Modal overlay ─────────────────────────────────────────────────────

    public static <T> T showModal(String fxml) {
        try {
            FXMLLoader loader = App.loader(fxml);
            Node node = loader.load();
            overlayArea.getChildren().setAll(node);
            overlayArea.setVisible(true);
            FadeTransition ft = new FadeTransition(Duration.millis(160), overlayArea);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            return loader.getController();
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    /** Replace modal content without animation (for edit→form transitions). */
    public static <T> T swapModal(String fxml) {
        try {
            FXMLLoader loader = App.loader(fxml);
            Node node = loader.load();
            overlayArea.getChildren().setAll(node);
            return loader.getController();
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public static void closeModal() {
        closeModal(null);
    }

    public static void closeModal(Runnable afterClose) {
        FadeTransition ft = new FadeTransition(Duration.millis(120), overlayArea);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            overlayArea.setVisible(false);
            overlayArea.getChildren().clear();
            if (afterClose != null) afterClose.run();
        });
        ft.play();
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
