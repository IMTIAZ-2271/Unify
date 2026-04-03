package com.Unify.controller;

import com.Unify.AppData;
import com.Unify.Navigator;
import com.Unify.Session;
import com.Unify.dao.*;
import com.Unify.model.*;
import com.Unify.util.ColorUtil;
import com.Unify.util.Imgs;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.collections.FXCollections;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.sql.Timestamp;

public class GroupDetailController {

    @FXML
    private Button leaveBtn;
    @FXML
    private Button joinBtn;
    @FXML
    private ImageView groupAvatar;
    @FXML
    private Label groupName, groupCode, memberCount, parentLabel, roleLabel, descLabel;
    @FXML
    private VBox membersList, eventsList, subGroupsList, userSearchResults;
    @FXML
    private HBox adminBar;
    @FXML
    private TextField userSearchField;
    @FXML
    private VBox groupCalendarContainer;

    @FXML private VBox announcementsList;
    @FXML private VBox postAnnouncementBox;
    @FXML private TextField announceTitleField;
    @FXML private TextArea announceContentField;
    @FXML private HBox replyPreviewBox;
    @FXML private Label replyPreviewName;
    @FXML private Label replyPreviewText;
    private ChatMessage replyingToMessage = null; // Tracks the active reply

    private Timeline chatPoller;
    private Timestamp lastMessageTime = new Timestamp(0);
    @FXML private VBox chatList;
    @FXML private ScrollPane chatScroll;
    @FXML private TextField chatInputField;
    private final AnnouncementDAO announcementDAO = new AnnouncementDAO();
    private final ChatDAO chatDAO = new ChatDAO();// for the group calendar tab

    private Group group;
    private YearMonth groupCalYm = YearMonth.now();

    private final GroupDAO groupDAO = new GroupDAO();
    private final EventDAO eventDAO = new EventDAO();
    private final UserDAO userDAO = new UserDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    public void setGroup(Group g) {
        this.group = g;
        fill();
        loadMembers();
        loadEventList();
        loadSubs();
        buildGroupCalendar();

        // Load the initial data
        loadAnnouncements();
        loadChat();

        // Start the real-time chat poller
        startChatPoller();

        // THE AUTO-KILL SWITCH:
        // This listens to see if the chatList is removed from the screen (e.g., user clicked a different sidebar button).
        // If it is removed, we completely stop the background timer so it doesn't leak memory.
        chatList.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null && chatPoller != null) {
                chatPoller.stop();
                System.out.println("Chat poller stopped safely.");
            }
        });
    }

    private void fill() {
        Image img = Imgs.fromBytes(group.getProfilePicture());
        if (img != null) groupAvatar.setImage(img);
        Imgs.circle(groupAvatar, 40);
        groupName.setText(group.getName());
        groupCode.setText("ID: " + group.getGroupCode());
        memberCount.setText(group.getMemberCount() + " members");
        parentLabel.setText(group.getParentGroupName() != null
                ? "Sub-group of: " + group.getParentGroupName() : "Top-level group");
        roleLabel.setText(group.isMember() ? "Your role: " + group.getCurrentUserRole().toUpperCase() : "Not a member");
        descLabel.setText(group.getDescription() != null ? group.getDescription() : "No description.");
        boolean admin = group.isAdmin();
        adminBar.setVisible(admin);
        adminBar.setManaged(admin);
        postAnnouncementBox.setVisible(admin);
        postAnnouncementBox.setManaged(admin);
        boolean member = group.isMember();
        leaveBtn.setVisible(member);
        leaveBtn.setManaged(member);
        if (!member) {
            if (AppData.get().getSentRequestForGroup(group.getId()).getStatus().equals("pending")) {
                joinBtn.setText("Request Pending");
                joinBtn.setDisable(true);
            }
        }
        joinBtn.setVisible(!member);
        joinBtn.setManaged(!member);
    }

    // ── Members tab ───────────────────────────────────────────────────────

    private void loadMembers() {
        membersList.getChildren().clear();
        try {
            for (User u : groupDAO.members(group.getId()))
                membersList.getChildren().add(memberRow(u));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox memberRow(User u) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(8));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color:#F1F5F9;-fx-border-width:0 0 1 0;");

        ImageView av = new ImageView();
        av.setFitWidth(36);
        av.setFitHeight(36);
        Image img = Imgs.fromBytes(u.getProfilePicture());
        if (img != null) av.setImage(img);
        Imgs.circle(av, 18);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(u.getDisplayName());
        name.setStyle("-fx-font-weight:bold;-fx-font-size:13;");

        // Parse role and assigned work from the bio string
        // Parse role, assigned work, and location from the bio string
        String fullRole = u.getBio() != null ? u.getBio() : "member";
        String[] parts = fullRole.split(":");
        String baseRole = parts[0];
        String assignedWork = parts.length > 1 ? parts[1] : null;
        String location = parts.length > 2 ? parts[2] : null;

        StringBuilder displayRole = new StringBuilder(baseRole.toUpperCase());
        if (assignedWork != null) {
            displayRole.append(" (").append(assignedWork.replace("_", " ").toUpperCase());
            if (location != null) {
                displayRole.append(" - ").append(location); // Adds the specific location
            }
            displayRole.append(")");
        }

        Label sub = new Label("@" + u.getUsername() + " • " + displayRole.toString());
        sub.setStyle("-fx-text-fill:#888;-fx-font-size:11;");

        info.getChildren().addAll(name, sub);

        row.getChildren().addAll(av, info);

        if (group.isAdmin() && u.getId() != Session.uid()) {
            boolean isAdmin = "admin".equals(baseRole);
            boolean isModerator = "moderator".equals(baseRole);

            MenuButton manageBtn = new MenuButton("Manage Role");
            manageBtn.getStyleClass().add("btn-primary-small");

            // Admin Toggle
            MenuItem adminItem = new MenuItem(isAdmin ? "Remove Admin" : "Make Admin");
            adminItem.setOnAction(e -> {
                try {
                    String newRole = isAdmin ? "member" : "admin";
                    groupDAO.setRole(group.getId(), u.getId(), newRole);
                    notifDAO.create(new Notification(u.getId(),
                            isAdmin ? "🔻 Admin Role Removed" : "⭐ You're now an Admin",
                            "Role updated in " + group.getName(),
                            isAdmin ? "admin_removed" : "admin_added"));
                    loadMembers();
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            // Moderator Assignment
            MenuItem modItem = new MenuItem(isModerator ? "Change Work Assignment" : "Make Moderator");
            modItem.setOnAction(e -> {
                Dialog<String[]> dialog = new Dialog<>();
                dialog.setTitle("Assign Moderator Work");
                dialog.setHeaderText("Assign role and location to " + u.getDisplayName());

                ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.setPadding(new Insets(20, 150, 10, 10));

                ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList("ticket_manager", "librarian", "canteen_manager"));
                ComboBox<String> locationCombo = new ComboBox<>(); // Replaces the old TextField

                // Function to dynamically update the location dropdown based on the chosen role
                Runnable updateLocations = () -> {
                    locationCombo.getItems().clear();
                    String selectedRole = roleCombo.getValue();

                    // ALWAYS add "None" as the first option
                    locationCombo.getItems().add("None");

                    if ("canteen_manager".equals(selectedRole)) {
                        try {
                            List<Canteen> canteens = new CanteenDAO().getCanteensByGroup(group.getId());
                            // If canteens exist, add them below "None"
                            for (Canteen c : canteens) {
                                locationCombo.getItems().add(c.getName());
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    // Select "None" by default
                    locationCombo.getSelectionModel().selectFirst();
                };

                // Add a listener so whenever the role changes, the location list updates
                roleCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateLocations.run());

                // Set initial state
                roleCombo.setValue("canteen_manager");

                grid.add(new Label("Role:"), 0, 0);
                grid.add(roleCombo, 1, 0);
                grid.add(new Label("Location:"), 0, 1);
                grid.add(locationCombo, 1, 1);

                dialog.getDialogPane().setContent(grid);
                javafx.application.Platform.runLater(roleCombo::requestFocus);

                // Convert the result
                dialog.setResultConverter(dialogButton -> {
                    if (dialogButton == saveButtonType) {
                        String loc = locationCombo.getValue();
                        // If it says "None", we don't want to save the word "None" into the database
                        if ("None".equals(loc)) loc = "";
                        return new String[]{roleCombo.getValue(), loc};
                    }
                    return null;
                });

                // Process and Save
                dialog.showAndWait().ifPresent(result -> {
                    String role = result[0];
                    String loc = result[1];

                    String combinedWork = loc.isEmpty() ? role : role + ":" + loc;

                    try {
                        groupDAO.setModeratorRole(group.getId(), u.getId(), combinedWork);
                        String locText = loc.isEmpty() ? "" : " at " + loc;
                        notifDAO.create(new Notification(u.getId(), "🛠️ New Moderator Assignment",
                                "You are now a " + role.replace("_", " ") + locText + " in " + group.getName(), "mod_added"));
                        loadMembers();
                    } catch (Exception ex) { ex.printStackTrace(); }
                });
            });

            // Remove Member
            MenuItem removeItem = new MenuItem("Remove Member");
            removeItem.setOnAction(e -> {
                new Alert(Alert.AlertType.CONFIRMATION, "Remove " + u.getDisplayName() + "?", ButtonType.YES, ButtonType.NO)
                        .showAndWait().ifPresent(b -> {
                            if (b == ButtonType.YES) {
                                try {
                                    groupDAO.removeMember(group.getId(), u.getId());
                                    loadMembers();
                                } catch (Exception ex) { ex.printStackTrace(); }
                            }
                        });
            });

            manageBtn.getItems().addAll(adminItem, modItem, new SeparatorMenuItem(), removeItem);
            row.getChildren().add(manageBtn);
        }
        return row;
    }

    @FXML
    private void doSearchUser() {
        String q = userSearchField.getText().trim();
        userSearchResults.getChildren().clear();
        if (q.isEmpty()) return;
        try {
            List<User> users;
            if (group.getParentGroupId() == null) {
                users = userDAO.search(q, Session.uid());
            } else {
                users = userDAO.searchByGroup(q, Session.uid(), group.getParentGroupId());
            }
            for (User u : users) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(5));
                Label name = new Label(u.getDisplayName() + " (@" + u.getUsername() + ")");
                HBox.setHgrow(name, Priority.ALWAYS);
                Button invBtn = new Button();
                invBtn.getStyleClass().add("btn-primary-small");
                if (!new GroupDAO().isMember(group.getId(), u.getId())) {
                    invBtn.setText("Invite");
                    invBtn.setOnAction(e ->
                            {
                                sendInvite(u);
                                invBtn.setText("Invited");
                                invBtn.getStyleClass().setAll("btn-outline-small");
                                invBtn.setDisable(true);
                            }
                    );
                } else {
                    invBtn.getStyleClass().setAll("btn-outline-small");
                    invBtn.setText("Member");
                    invBtn.setDisable(true);
                }
                row.getChildren().addAll(name, invBtn);
                userSearchResults.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Issue #4 – Instead of immediately adding the user, send them a group invitation.
     * The user accepts/declines from their Notifications page.
     */
    private void sendInvite(User u) {
        try {
            // Check if already a member
            /*if (groupDAO.isMember(group.getId(), u.getId())) {
                new Alert(Alert.AlertType.INFORMATION, u.getDisplayName() + " is already a member.").showAndWait();
                return;
            }*/
            Notification invite = new Notification(u.getId(),
                    "👥 Group Invitation: " + group.getName(),
                    Session.currentUser().getDisplayName() + " invited you to join '" + group.getName() + "'. Accept or decline in notifications.",
                    "group_invite");
            invite.setReferenceId(group.getId());
            notifDAO.create(invite);
            //new Alert(Alert.AlertType.INFORMATION,
            //    "Invitation sent to " + u.getDisplayName() + "!\nThey will see it in their Notifications.").showAndWait();
            //userSearchResults.getChildren().clear();
            //userSearchField.clear();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
        }
    }

    // ── Events list tab ───────────────────────────────────────────────────

    private void loadEventList() {
        eventsList.getChildren().clear();
        try {
            List<Event> events = eventDAO.forGroup(group.getId());
            if (events.isEmpty()) {
                eventsList.getChildren().add(noData("No events yet."));
                return;
            }
            String color = ColorUtil.forGroup(group.getId());
            for (Event e : events) {
                HBox row = new HBox(10);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color:white;-fx-border-color:" + color
                        + ";-fx-border-width:0 0 0 3;-fx-cursor:hand;");
                VBox info = new VBox(2);
                HBox.setHgrow(info, Priority.ALWAYS);
                Label t = new Label(e.getTitle());
                t.setStyle("-fx-font-weight:bold;-fx-font-size:13;");
                Label d = new Label(e.getStartTime().format(FMT));
                d.setStyle("-fx-text-fill:#888;-fx-font-size:11;");
                info.getChildren().addAll(t, d);
                row.getChildren().add(info);
                row.setOnMouseClicked(ev -> openEventDetail(e));
                eventsList.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Group Calendar tab ────────────────────────────────────────────────

    private void buildGroupCalendar() {
        buildGroupCalendar(groupCalYm);
    }

    private void buildGroupCalendar(YearMonth ym) {
        groupCalYm = ym;
        groupCalendarContainer.getChildren().clear();

        // Nav header
        HBox nav = new HBox(10);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(8, 12, 8, 12));
        nav.setStyle("-fx-border-color:#E2E8F0;-fx-border-width:0 0 1 0;");

        Button prev = new Button("◀");
        prev.getStyleClass().add("nav-btn");
        prev.setOnAction(e -> buildGroupCalendar(groupCalYm.minusMonths(1)));

        Label monthLbl = new Label(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        monthLbl.setStyle("-fx-font-weight:bold;-fx-font-size:14;");

        Button next = new Button("▶");
        next.getStyleClass().add("nav-btn");
        next.setOnAction(e -> buildGroupCalendar(groupCalYm.plusMonths(1)));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        if (group.isAdmin()) {
            Button addBtn = new Button("＋ Add Event");
            addBtn.getStyleClass().add("btn-primary-small");
            addBtn.setOnAction(e -> doAddEvent());
            nav.getChildren().addAll(prev, monthLbl, next, sp, addBtn);
        } else {
            nav.getChildren().addAll(prev, monthLbl, next);
        }
        groupCalendarContainer.getChildren().add(nav);

        // DOW header
        GridPane dow = new GridPane();
        dow.setStyle("-fx-background-color:#F8FAFC;-fx-border-color:#E2E8F0;-fx-border-width:0 0 1 0;");
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label l = new Label(days[i]);
            l.setStyle("-fx-text-fill:#64748B;-fx-font-weight:bold;-fx-font-size:11;-fx-padding:5 0;-fx-alignment:CENTER;");
            l.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(l, Priority.ALWAYS);
            GridPane.setFillWidth(l, true);
            dow.add(l, i, 0);
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            if (i < dow.getColumnConstraints().size()) {
            } else dow.getColumnConstraints().add(cc);
        }
        groupCalendarContainer.getChildren().add(dow);

        // Calendar grid
        GridPane grid = new GridPane();
        grid.setStyle("-fx-background-color:white;");
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        Map<LocalDate, List<Event>> byDay = new HashMap<>();
        try {
            List<Event> events = eventDAO.forGroupInMonth(group.getId(), ym.getYear(), ym.getMonthValue());
            for (Event e : events)
                byDay.computeIfAbsent(e.getStartTime().toLocalDate(), k -> new ArrayList<>()).add(e);
        } catch (Exception e) {
            e.printStackTrace();
        }

        LocalDate today = LocalDate.now();
        String groupColor = ColorUtil.forGroup(group.getId());
        int startCol = ym.atDay(1).getDayOfWeek().getValue() % 7;
        int numDays = ym.lengthOfMonth();
        int col = startCol, row = 0;

        for (int d = 1; d <= numDays; d++) {
            LocalDate date = ym.atDay(d);
            List<Event> dayEvents = byDay.getOrDefault(date, List.of());
            boolean isToday = today.equals(date);

            VBox cell = new VBox(2);
            cell.setPadding(new Insets(4));
            cell.setMinHeight(80);
            cell.setStyle("-fx-border-color:#E2E8F0;-fx-border-width:0 1 1 0;"
                    + (isToday ? "-fx-background-color:#EFF6FF;" : "-fx-background-color:white;"));

            Label num = new Label(String.valueOf(d));
            num.setStyle("-fx-font-weight:bold;-fx-font-size:12;"
                    + (isToday ? "-fx-text-fill:#3B82F6;" : "-fx-text-fill:#475569;"));
            cell.getChildren().add(num);

            int shown = 0;
            for (Event e : dayEvents) {
                if (shown == 2) {
                    cell.getChildren().add(new Label("+" + (dayEvents.size() - 2) + " more"));
                    break;
                }
                Label tag = new Label(e.getTitle());
                tag.setMaxWidth(Double.MAX_VALUE);
                tag.setPadding(new Insets(1, 4, 1, 4));
                tag.setStyle("-fx-background-color:" + groupColor
                        + ";-fx-text-fill:white;-fx-background-radius:3;-fx-font-size:10;-fx-font-weight:bold;");
                tag.setCursor(javafx.scene.Cursor.HAND);
                final Event ev = e;
                tag.setOnMouseClicked(click -> {
                    click.consume();
                    openEventDetail(ev);
                });
                cell.getChildren().add(tag);
                shown++;
            }
            grid.add(cell, col, row);
            if (++col == 7) {
                col = 0;
                row++;
            }
        }

        ScrollPane sp2 = new ScrollPane(grid);
        sp2.setFitToWidth(true);
        sp2.setStyle("-fx-background:white;-fx-background-color:white;");
        VBox.setVgrow(sp2, Priority.ALWAYS);
        groupCalendarContainer.getChildren().add(sp2);
    }

    // ── Sub-groups tab ────────────────────────────────────────────────────

    private void loadSubs() {
        subGroupsList.getChildren().clear();
        try {
            List<Group> subs = groupDAO.subGroups(group.getId(), Session.uid());
            if (subs.isEmpty()) {
                subGroupsList.getChildren().add(noData("No sub-groups."));
                return;
            }
            for (Group sg : subs) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(6));
                Circle dot = new Circle(6, Color.web(ColorUtil.forGroup(sg.getId())));
                Label l = new Label(sg.getName() + " (" + sg.getMemberCount() + " members)");
                l.setStyle("-fx-cursor:hand;-fx-text-fill:#1E293B;-fx-font-weight:bold;");
                l.setOnMouseClicked(e -> openSubGroup(sg));
                row.getChildren().addAll(dot, l);
                subGroupsList.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────

    @FXML
    private void doAddEvent() {
        EventFormController ctrl = Navigator.showWindow("/com/Unify/fxml/event_form.fxml");
        if (ctrl != null) {
            ctrl.setGroup(group);
            ctrl.setOnClose(() -> {
                loadEventList();
                buildGroupCalendar();
            });
        }
    }

    @FXML
    private void doEditGroup() {
        CreateGroupController ctrl = Navigator.showWindow("/com/Unify/fxml/create_group.fxml");
        if (ctrl != null) {
            ctrl.setGroup(group);
            ctrl.setOnClose(() -> {
                try {
                    group = groupDAO.findById(group.getId(), Session.uid());
                    fill();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    @FXML
    private void doLeave() {
        new Alert(Alert.AlertType.CONFIRMATION, "Leave " + group.getName() + "?", ButtonType.YES, ButtonType.NO)
                .showAndWait().ifPresent(b -> {
                    if (b == ButtonType.YES) {
                        try {
                            groupDAO.removeMember(group.getId(), Session.uid());
                            Navigator.pop();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void openEventDetail(Event e) {
        EventDetailController ctrl = Navigator.showWindow("/com/Unify/fxml/event_detail.fxml");
        if (ctrl != null) ctrl.setEvent(e, () -> {
            loadEventList();
            buildGroupCalendar();
        });
    }

    private void openSubGroup(Group g) {
        GroupDetailController ctrl = Navigator.push("/com/Unify/fxml/group_detail.fxml");
        if (ctrl != null) ctrl.setGroup(g);
    }

    private Label noData(String m) {
        Label l = new Label(m);
        l.setStyle("-fx-text-fill:#aaa;-fx-padding:12;");
        return l;
    }

    @FXML
    private void requestJoin() {
        try {
            groupDAO.requestJoin(group.getId(), Session.uid());
            JoinRequest jr = new JoinRequest();
            jr.setGroupId(group.getId());
            jr.setGroupName(group.getName());
            jr.setUserId(Session.uid());
            jr.setUsername(Session.currentUser().getUsername());
            jr.setUserPic(Session.currentUser().getProfilePicture());
            jr.setStatus("pending");
            jr.setRequestedAt(LocalDateTime.now());

            AppData.get().addSentRequest(jr);
            new Alert(Alert.AlertType.INFORMATION, "Request sent to join " + group.getName() + "!\nAn admin will review your request.").showAndWait();
            joinBtn.setText("Request Pending");
            joinBtn.setDisable(true);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
        }
    }
    // ── Announcements Logic ───────────────────────────────────────────────

    private void loadAnnouncements() {
        announcementsList.getChildren().clear();
        // 🔒 SECURITY CHECK: Are they a member?
        if (!group.isMember()) {
            Label lockMsg = new Label("🔒 Announcements are private to group members.");
            lockMsg.setStyle("-fx-text-fill:#64748B; -fx-font-style:italic; -fx-padding:20;");
            announcementsList.getChildren().add(lockMsg);
            return; // Stop loading announcements!
        }
        try {
            List<Announcement> announcements = announcementDAO.getAnnouncementsForGroup(group.getId());
            if (announcements.isEmpty()) {
                announcementsList.getChildren().add(noData("No announcements yet."));
                return;
            }
            for (Announcement a : announcements) {
                VBox card = new VBox(5);
                card.setStyle("-fx-padding:10; -fx-background-color:#EFF6FF; -fx-border-color:#BFDBFE; -fx-border-radius:5; -fx-background-radius:5;");

                Label title = new Label(a.getTitle());
                title.setStyle("-fx-font-weight:bold; -fx-font-size:14; -fx-text-fill:#1E3A8A;");

                Label meta = new Label("Posted by " + a.getAuthorName() + " on " + a.getCreatedAt().toLocalDateTime().format(FMT));
                meta.setStyle("-fx-font-size:10; -fx-text-fill:#64748B;");

                Label content = new Label(a.getContent());
                content.setWrapText(true);

                card.getChildren().addAll(title, meta, content);
                announcementsList.getChildren().add(card);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void postAnnouncement() {
        String title = announceTitleField.getText().trim();
        String content = announceContentField.getText().trim();
        if (title.isEmpty() || content.isEmpty()) return;

        // Use AsyncWriter to do heavy database work in the background
        com.Unify.util.AsyncWriter.get().write(() -> {
            // 1. Save the announcement to the database
            announcementDAO.createAnnouncement(group.getId(), Session.uid(), title, content);

            try {
                // 2. Fetch all members and send them a real-time notification!
                List<User> members = groupDAO.members(group.getId());
                for (User u : members) {
                    if (u.getId() != Session.uid()) { // Don't notify the admin who posted it
                        Notification n = new Notification(u.getId(),
                                "📢 Announcement: " + group.getName(),
                                title,
                                "announcement");
                        n.setReferenceId(group.getId());
                        notifDAO.create(n); // SyncEngine will pick this up automatically!
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }, () -> {
            // 3. Clear the fields and refresh the UI on success
            announceTitleField.clear();
            announceContentField.clear();
            loadAnnouncements();
        }, Throwable::printStackTrace);
    }

    // ── Chat Logic ────────────────────────────────────────────────────────

    private void loadChat() {
        chatList.getChildren().clear();

        chatList.getChildren().clear();

        // 🔒 SECURITY CHECK: Are they a member?
        if (!group.isMember()) {
            Label lockMsg = new Label("🔒 You must join this group to view and participate in the chat.");
            lockMsg.setStyle("-fx-text-fill:#64748B; -fx-font-style:italic; -fx-padding:20;");
            chatList.getChildren().add(lockMsg);
            chatInputField.setDisable(true); // Stop them from typing
            return; // Stop loading messages!
        }
        try {
            List<ChatMessage> messages = chatDAO.getMessagesForGroup(group.getId());
            for (ChatMessage m : messages) {
                appendMessageToUI(m);

                // Track the time of the newest message loaded
                if (m.getCreatedAt().after(lastMessageTime)) {
                    lastMessageTime = m.getCreatedAt();
                }
            }
            javafx.application.Platform.runLater(() -> chatScroll.setVvalue(1.0));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void appendMessageToUI(ChatMessage m) {
        boolean isMe = m.getSenderId() == Session.uid();

        // 1. Avatar
        javafx.scene.image.ImageView avatar = new javafx.scene.image.ImageView();
        avatar.setFitWidth(32); avatar.setFitHeight(32);
        com.Unify.util.Imgs.setAvatar(avatar, m.getSenderPic(), 32);
        HBox.setMargin(avatar, new javafx.geometry.Insets(10, isMe ? 0 : 8, 0, isMe ? 8 : 0));

        // 2. Main Bubble Container
        VBox bubble = new VBox(4); // Slightly more spacing for replies
        String radius = isMe ? "15 15 0 15" : "15 15 15 0";
        String bgColor = isMe ? "#DCF8C6" : "#F3F4F6";

        bubble.setStyle("-fx-padding: 8 12 4 12; -fx-background-radius: " + radius + "; -fx-background-color: " + bgColor + "; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 2, 0, 0, 1);");
        bubble.setMaxWidth(400);

        // --- NEW: Add the Quoted Reply Block inside the bubble ---
        if (m.getReplyToId() != null) {
            VBox quoteBox = new VBox(2);
            quoteBox.setStyle("-fx-background-color: rgba(0,0,0,0.05); -fx-padding: 4 8; -fx-background-radius: 5; -fx-border-color: #00A884; -fx-border-width: 0 0 0 3;");

            Label quoteName = new Label(isMe && m.getReplyToName().equals(Session.currentUser().getUsername()) ? "You" : m.getReplyToName());
            quoteName.setStyle("-fx-font-weight: bold; -fx-font-size: 10; -fx-text-fill: #00A884;");

            Label quoteText = new Label(m.getReplyToMessage());
            quoteText.setStyle("-fx-font-size: 11; -fx-text-fill: #64748B;");
            quoteText.setWrapText(true);
            quoteText.setMaxHeight(30); // Prevent massive quote blocks

            quoteBox.getChildren().addAll(quoteName, quoteText);
            bubble.getChildren().add(quoteBox);
        }
        // ---------------------------------------------------------

        // 3. Sender Name
        if (!isMe) {
            Label sender = new Label(m.getSenderName());
            sender.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #0284C7;");
            bubble.getChildren().add(sender);
        }

        // 4. Message Text
        Label text = new Label(m.getMessage());
        text.setWrapText(true);
        text.setStyle("-fx-font-size: 14; -fx-text-fill: #111827;");

        // 5. Timestamp
        String timeStr = m.getCreatedAt().toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"));
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #9CA3AF;");
        HBox timeBox = new HBox(timeLabel);
        timeBox.setAlignment(javafx.geometry.Pos.BOTTOM_RIGHT);
        timeBox.setPadding(new javafx.geometry.Insets(2, 0, 0, 10));

        bubble.getChildren().addAll(text, timeBox);

        // --- NEW: Right-Click to Reply Context Menu ---
        ContextMenu contextMenu = new ContextMenu();
        MenuItem replyItem = new MenuItem("↩ Reply");
        replyItem.setOnAction(e -> {
            replyingToMessage = m;
            replyPreviewName.setText(isMe ? "Replying to yourself" : "Replying to " + m.getSenderName());
            replyPreviewText.setText(m.getMessage());
            replyPreviewBox.setVisible(true);
            replyPreviewBox.setManaged(true);
            chatInputField.requestFocus(); // Auto-focus the text box
        });
        contextMenu.getItems().add(replyItem);

        // Attach the menu to the bubble
        bubble.setOnContextMenuRequested(e -> contextMenu.show(bubble, e.getScreenX(), e.getScreenY()));
        // ----------------------------------------------

        // 6. Assemble Row
        HBox row = new HBox();
        if (isMe) {
            row.getChildren().addAll(bubble, avatar);
            row.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        } else {
            row.getChildren().addAll(avatar, bubble);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }

        chatList.getChildren().add(row);
    }

    private void startChatPoller() {
        // Runs every 2 seconds
        if (!group.isMember()) return;
        chatPoller = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            // Only ask the database for messages newer than our lastMessageTime
            List<ChatMessage> newMsgs = chatDAO.getNewMessages(group.getId(), lastMessageTime);

            if (!newMsgs.isEmpty()) {
                for (ChatMessage m : newMsgs) {
                    appendMessageToUI(m);

                    if (m.getCreatedAt().after(lastMessageTime)) {
                        lastMessageTime = m.getCreatedAt();
                    }
                }
                // Scroll to bottom when new messages arrive
                javafx.application.Platform.runLater(() -> chatScroll.setVvalue(1.0));
            }
        }));
        chatPoller.setCycleCount(Timeline.INDEFINITE);
        chatPoller.play();
    }

    @FXML
    private void cancelReply() {
        replyingToMessage = null;
        replyPreviewBox.setVisible(false);
        replyPreviewBox.setManaged(false);
    }
    @FXML
    private void sendMessage() {
        String msg = chatInputField.getText().trim();
        if (msg.isEmpty()) return;

        chatInputField.clear();
        Integer replyId = replyingToMessage != null ? replyingToMessage.getId() : null;
        cancelReply(); // Clear the preview UI right away

        com.Unify.util.AsyncWriter.get().write(() -> {
            chatDAO.sendMessage(group.getId(), Session.uid(), msg, replyId);
        }, () -> {
            loadChat();
        }, Throwable::printStackTrace);
    }
}
