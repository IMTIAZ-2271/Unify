package com.Unify.controller;

import com.Unify.App;
import com.Unify.AppData;
import com.Unify.Navigator;
import com.Unify.Session;
import com.Unify.dao.NotificationDAO;
import com.Unify.dao.UserDAO;
import com.Unify.model.User;
import com.Unify.service.NotificationService;
import com.Unify.util.Imgs;
import com.Unify.util.SessionStore;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.sql.SQLException;

public class MainController {

    public StackPane overlayArea2;
    public HBox profileNameAndPictureCell;
    @FXML
    private StackPane contentArea;
    @FXML
    private StackPane overlayArea;
    @FXML
    private Label userNameLabel;
    @FXML
    private ImageView userAvatar;
    @FXML
    private Label notifBadge;
    @FXML
    private Button adminPanelButton;

    private final NotificationDAO notifDAO = new NotificationDAO();


    @FXML
    private void initialize() {
        // Wire single-window navigation
        Navigator.init(contentArea, overlayArea);

        // 1. Initial load of the avatar and name
        updateSidebarProfile();

        // 2. Listen for user profile changes and update the UI dynamically
        AppData.get().addUserListener(() -> {
            // Ensure UI updates happen on the JavaFX Application Thread
            Platform.runLater(this::updateSidebarProfile);
        });

        com.Unify.service.NotificationService.get().setBadgeUpdater(count -> {
            notifBadge.setText(count > 0 ? String.valueOf(count) : "");
            notifBadge.setVisible(count > 0);
        });
        refreshBadge();
        showCalendar();
    }

    /**
     * Helper method to refresh the sidebar profile picture and display name.
     */
    private void updateSidebarProfile() {
        User user = Session.currentUser();
        if (user != null) {
            userNameLabel.setText(user.getDisplayName());
            Image img = Imgs.fromBytes(user.getProfilePicture());
            if (img != null) {
                userAvatar.setImage(img);
            }
            Imgs.circle(userAvatar, 18);
        }
    }

    @FXML
    public void showCalendar() {
        Navigator.goTo("/com/Unify/fxml/month_view.fxml");
    }

    @FXML
    public void showGroups() {
        Navigator.goTo("/com/Unify/fxml/groups.fxml");
    }

    @FXML
    public void showNotifications() {
        Navigator.goTo("/com/Unify/fxml/notifications.fxml");
        refreshBadge();
    }

    @FXML
    public void showUtilities() {
        Navigator.goTo("/com/Unify/fxml/utility.fxml");
    }

    @FXML
    public void showProfile() {
        Navigator.goTo("/com/Unify/fxml/profile.fxml");
    }

    @FXML
    public void doLogout() {
        try {
            NotificationService.get().stop();
            SessionStore.clear();
            Session.logout();
            App.showLogin();
            MonthViewController.refreshCheckbox(); // Assuming this is valid in your broader project
            AppData.get().clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshBadge() {
        try {
            int n = AppData.get().getUnreadCount();
            Platform.runLater(() -> {
                notifBadge.setText(n > 0 ? String.valueOf(n) : "");
                notifBadge.setVisible(n > 0);
            });
        } catch (Exception ignored) {
        }
    }

    @FXML
    public void showChat() {
        Navigator.goTo("/com/Unify/fxml/chat_view.fxml");
    }

    @FXML
    public void showAnnouncements() {
        Navigator.goTo("/com/Unify/fxml/announcements_view.fxml");
    }
}