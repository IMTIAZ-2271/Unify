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

    @FXML private VBox announcementsList;
    @FXML private VBox postFormBox;
    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private Button submitPostBtn; // Add this line!

    private Integer editingAnnouncementId = null; // Tracks which announcement is being edited

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

        // Move to the content area when pressing Enter in the title field
        titleField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                contentArea.requestFocus();
            }
        });

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

            // Check if the current user is an admin or moderator
            boolean isAdminOrMod = "admin".equals(currentGroup.getCurrentUserRole()) || "moderator".equals(currentGroup.getCurrentUserRole());

            for (Announcement a : announcements) {
                VBox card = new VBox(8);
                card.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

                // --- HEADER ROW ---
                HBox headerBox = new HBox(10);
                headerBox.setAlignment(Pos.CENTER_LEFT);

                Label title = new Label("📢 " + a.getTitle());
                title.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #1E3A8A;");
                HBox.setHgrow(title, Priority.ALWAYS); // Pushes the menu to the far right
                title.setMaxWidth(Double.MAX_VALUE);

                headerBox.getChildren().add(title);

                // --- 3-DOT MENU & HIDDEN BUTTONS ---
                if (isAdminOrMod) {
                    // 1. The container for Edit/Delete (Hidden by default)
                    HBox actionBox = new HBox(15);
                    actionBox.setAlignment(Pos.CENTER);
                    actionBox.setVisible(false); // Hidden initially
                    actionBox.setManaged(false); // Doesn't take up space when hidden
                    actionBox.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 2 10; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10;");

                    // 2. Minimalist Edit Label
                    Label editLbl = new Label("Edit");
                    editLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #64748B; -fx-cursor: hand;");
                    editLbl.setOnMouseEntered(e -> editLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #3B82F6; -fx-cursor: hand; -fx-underline: true;"));
                    editLbl.setOnMouseExited(e -> editLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #64748B; -fx-cursor: hand; -fx-underline: false;"));
                    editLbl.setOnMouseClicked(e -> startEditAnnouncement(a));

                    // 3. Minimalist Delete Label
                    Label delLbl = new Label("Delete");
                    delLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #64748B; -fx-cursor: hand;");
                    delLbl.setOnMouseEntered(e -> delLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #EF4444; -fx-cursor: hand; -fx-underline: true;"));
                    delLbl.setOnMouseExited(e -> delLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #64748B; -fx-cursor: hand; -fx-underline: false;"));
                    delLbl.setOnMouseClicked(e -> deleteAnnouncement(a));

                    actionBox.getChildren().addAll(editLbl, delLbl);

                    // 4. The 3-Dot Trigger
                    Label optionsBtn = new Label("⋮");
                    optionsBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #94A3B8; -fx-cursor: hand; -fx-padding: 0 5;");
                    optionsBtn.setOnMouseEntered(e -> optionsBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #334155; -fx-cursor: hand; -fx-padding: 0 5;"));
                    optionsBtn.setOnMouseExited(e -> optionsBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #94A3B8; -fx-cursor: hand; -fx-padding: 0 5;"));

                    // Toggle visibility when clicking the 3 dots
                    optionsBtn.setOnMouseClicked(e -> {
                        boolean isVisible = actionBox.isVisible();
                        actionBox.setVisible(!isVisible);
                        actionBox.setManaged(!isVisible);
                    });

                    headerBox.getChildren().addAll(actionBox, optionsBtn);
                }
                // -----------------------------------

                Label meta = new Label("Posted by " + a.getAuthorName() + " • " + a.getCreatedAt().toLocalDateTime().format(FMT));
                meta.setStyle("-fx-font-size: 11; -fx-text-fill: #64748B;");

                Label content = new Label(a.getContent());
                content.setWrapText(true);
                content.setStyle("-fx-font-size: 14; -fx-text-fill: #334155; -fx-padding: 5 0 0 0;");

                card.getChildren().addAll(headerBox, meta, new Separator(), content);
                announcementsList.getChildren().add(card);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    private void startEditAnnouncement(Announcement a) {
        editingAnnouncementId = a.getId(); // Save the ID
        titleField.setText(a.getTitle());
        contentArea.setText(a.getContent());
        submitPostBtn.setText("Update"); // Change button text to "Update"
        showPostForm();
    }

    private void deleteAnnouncement(Announcement a) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete this announcement?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                AsyncWriter.get().write(() -> {
                    announcementDAO.deleteAnnouncement(a.getId());
                }, this::loadAnnouncements, Throwable::printStackTrace);
            }
        });
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

        // Reset the editing state and button text
        editingAnnouncementId = null;
        if (submitPostBtn != null) submitPostBtn.setText("Post");
    }

    @FXML
    private void postAnnouncement() {
        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();
        if (title.isEmpty() || content.isEmpty() || currentGroup == null) return;

        AsyncWriter.get().write(() -> {
            if (editingAnnouncementId != null) {
                // We are EDITING: Update the existing record in the database
                announcementDAO.updateAnnouncement(editingAnnouncementId, title, content);
            } else {
                // We are CREATING: Insert a new record
                announcementDAO.createAnnouncement(currentGroup.getId(), Session.uid(), title, content);

                // Only send notifications for NEW announcements
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
            }
        }, () -> {
            hidePostForm();
            loadAnnouncements();
        }, Throwable::printStackTrace);
    }
}