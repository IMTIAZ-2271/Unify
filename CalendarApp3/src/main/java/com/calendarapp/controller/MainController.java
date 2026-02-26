package com.calendarapp.controller;

import com.calendarapp.App;
import com.calendarapp.AppData;
import com.calendarapp.Navigator;
import com.calendarapp.Session;
import com.calendarapp.dao.NotificationDAO;
import com.calendarapp.service.NotificationService;
import com.calendarapp.util.Imgs;
import com.calendarapp.util.SessionStore;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class MainController {

    public StackPane overlayArea2;
    public HBox profileNameAndPictureCell;
    @FXML private StackPane contentArea;
    @FXML private StackPane overlayArea;
    @FXML private Label     userNameLabel;
    @FXML private ImageView userAvatar;
    @FXML private Label     notifBadge;

    private final NotificationDAO notifDAO = new NotificationDAO();

    @FXML private void initialize() {
        // Wire single-window navigation
        Navigator.init(contentArea, overlayArea);

        userNameLabel.setText(Session.currentUser().getDisplayName());
        Image img = Imgs.fromBytes(Session.currentUser().getProfilePicture());
        if (img != null)
            userAvatar.setImage(img);
        Imgs.circle(userAvatar, 18);

        NotificationService.get().setBadgeUpdater(count -> {
            notifBadge.setText(count > 0 ? String.valueOf(count) : "");
            notifBadge.setVisible(count > 0);
        });
        refreshBadge();
        showCalendar();
    }

    @FXML public void showCalendar()      { Navigator.goTo("/com/calendarapp/fxml/month_view.fxml"); }
    @FXML public void showGroups()        { Navigator.goTo("/com/calendarapp/fxml/groups.fxml"); }
    @FXML public void showNotifications() { Navigator.goTo("/com/calendarapp/fxml/notifications.fxml"); refreshBadge(); }
    @FXML public void showProfile()       { Navigator.goTo("/com/calendarapp/fxml/profile.fxml"); }

    @FXML public void doLogout() {
        try {
            NotificationService.get().stop();
            SessionStore.clear();
            Session.logout();
            App.showLogin();
            MonthViewController.refreshCheckbox();
            AppData.get().clear();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void refreshBadge() {
        try {
            int n = AppData.get().getUnreadCount();
            Platform.runLater(() -> {
                notifBadge.setText(n > 0 ? String.valueOf(n) : "");
                notifBadge.setVisible(n > 0);
            });
        } catch (Exception ignored) {}
    }
}