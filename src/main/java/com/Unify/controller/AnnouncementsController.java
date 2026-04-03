package com.Unify.controller;

import com.Unify.Session;
import com.Unify.dao.AnnouncementDAO;
import com.Unify.dao.GroupDAO;
import com.Unify.dao.NotificationDAO;
import com.Unify.model.Announcement;
import com.Unify.model.Group;
import com.Unify.model.Notification;
import com.Unify.model.User;
import com.Unify.util.AsyncWriter;
import com.Unify.util.Imgs;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AnnouncementsController {

    @FXML private TextField searchGroupField;
    @FXML private ListView<Group> groupListView;

    @FXML private VBox announcementArea;
    @FXML private VBox placeholderArea;

    @FXML private ImageView headerAvatar;
    @FXML private Label headerName;
    @FXML private Label headerSub;
    @FXML private Button newAnnouncementBtn;

    @FXML private VBox postFormBox;
    @FXML private TextField titleField;
    @FXML private TextArea contentArea;

    @FXML private VBox announcementsList;

    private final GroupDAO groupDAO = new GroupDAO();
    private final AnnouncementDAO announcementDAO = new AnnouncementDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();

    private Group currentGroup = null;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private final ObservableList<Group> masterGroupList = FXCollections.observableArrayList();
    private FilteredList<Group> filteredGroups;

    @FXML
    public void initialize() {
        setupGroupList();
        loadGroups();
    }

    private void loadGroups() {
        try {
            List<Group> myGroups = groupDAO.myGroups(Session.uid());
            masterGroupList.setAll(myGroups);

            filteredGroups = new FilteredList<>(masterGroupList, p -> true);
            searchGroupField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredGroups.setPredicate(group -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    return group.getName().toLowerCase().contains(newVal.toLowerCase());
                });
            });

            groupListView.setItems(filteredGroups);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupGroupList() {
        groupListView.setCellFactory(param -> new ListCell<Group>() {
            @Override
            protected void updateItem(Group g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) {
                    setText(null); setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(10));

                    ImageView av = new ImageView();
                    av.setFitWidth(45); av.setFitHeight(45);
                    Image img = Imgs.fromBytes(g.getProfilePicture());
                    if (img != null) av.setImage(img);
                    Imgs.circle(av, 22.5);

                    VBox info = new VBox(3);
                    info.setAlignment(Pos.CENTER_LEFT);
                    Label name = new Label(g.getName());
                    name.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #1E293B;");
                    Label sub = new Label(g.getMemberCount() + " members");
                    sub.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12;");
                    info.getChildren().addAll(name, sub);

                    row.getChildren().addAll(av, info);
                    setGraphic(row);

                    if (isSelected()) {
                        setStyle("-fx-background-color: #E2E8F0; -fx-cursor: hand;");
                    } else {
                        setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                    }
                }
            }
        });

        groupListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) openAnnouncementsForGroup(newVal);
        });
    }

    private void openAnnouncementsForGroup(Group group) {
        this.currentGroup = group;
        placeholderArea.setVisible(false);
        placeholderArea.setManaged(false);
        announcementArea.setVisible(true);
        announcementArea.setManaged(true);
        hidePostForm();

        headerName.setText(group.getName());
        headerSub.setText(group.getMemberCount() + " members");
        Image headerImg = Imgs.fromBytes(group.getProfilePicture());
        if (headerImg != null) headerAvatar.setImage(headerImg);
        Imgs.circle(headerAvatar, 21);

        // Show the "New Announcement" button only if admin/moderator
        boolean isAdminOrMod = "admin".equals(group.getCurrentUserRole()) || "moderator".equals(group.getCurrentUserRole());
        newAnnouncementBtn.setVisible(isAdminOrMod);
        newAnnouncementBtn.setManaged(isAdminOrMod);

        loadAnnouncements();
    }

    private void loadAnnouncements() {
        announcementsList.getChildren().clear();
        try {
            List<Announcement> announcements = announcementDAO.getAnnouncementsForGroup(currentGroup.getId());
            if (announcements.isEmpty()) {
                Label noData = new Label("No announcements yet.");
                noData.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14; -fx-font-style: italic;");
                announcementsList.getChildren().add(noData);
                return;
            }

            for (Announcement a : announcements) {
                VBox card = new VBox(8);
                card.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

                Label title = new Label("📢 " + a.getTitle());
                title.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #1E3A8A;");

                Label meta = new Label("Posted by " + a.getAuthorName() + " • " + a.getCreatedAt().toLocalDateTime().format(FMT));
                meta.setStyle("-fx-font-size: 11; -fx-text-fill: #64748B;");

                Label content = new Label(a.getContent());
                content.setWrapText(true);
                content.setStyle("-fx-font-size: 14; -fx-text-fill: #334155; -fx-padding: 5 0 0 0;");

                card.getChildren().addAll(title, meta, new Separator(), content);
                announcementsList.getChildren().add(card);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void showPostForm() {
        postFormBox.setVisible(true);
        postFormBox.setManaged(true);
        titleField.requestFocus();
    }

    @FXML
    private void hidePostForm() {
        postFormBox.setVisible(false);
        postFormBox.setManaged(false);
        titleField.clear();
        contentArea.clear();
    }

    @FXML
    private void postAnnouncement() {
        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();
        if (title.isEmpty() || content.isEmpty() || currentGroup == null) return;

        AsyncWriter.get().write(() -> {
            announcementDAO.createAnnouncement(currentGroup.getId(), Session.uid(), title, content);

            // Notify everyone else in the group
            try {
                List<User> members = groupDAO.members(currentGroup.getId());
                for (User u : members) {
                    if (u.getId() != Session.uid()) {
                        Notification n = new Notification(u.getId(),
                                "📢 Announcement: " + currentGroup.getName(),
                                title,
                                "announcement");
                        n.setReferenceId(currentGroup.getId());
                        notifDAO.create(n);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }

        }, () -> {
            hidePostForm();
            loadAnnouncements();
        }, Throwable::printStackTrace);
    }
}