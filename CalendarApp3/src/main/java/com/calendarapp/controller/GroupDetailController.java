package com.calendarapp.controller;

import com.calendarapp.Navigator;
import com.calendarapp.Session;
import com.calendarapp.dao.EventDAO;
import com.calendarapp.dao.GroupDAO;
import com.calendarapp.dao.NotificationDAO;
import com.calendarapp.dao.UserDAO;
import com.calendarapp.model.Event;
import com.calendarapp.model.Group;
import com.calendarapp.model.Notification;
import com.calendarapp.model.User;
import com.calendarapp.util.ColorUtil;
import com.calendarapp.util.Imgs;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GroupDetailController {

    @FXML private ImageView groupAvatar;
    @FXML private Label     groupName, groupCode, memberCount, parentLabel, roleLabel, descLabel;
    @FXML private VBox      membersList, eventsList, subGroupsList, userSearchResults;
    @FXML private HBox      adminBar;
    @FXML private TextField userSearchField;
    @FXML private VBox      groupCalendarContainer;  // for the group calendar tab

    private Group  group;
    private YearMonth groupCalYm = YearMonth.now();

    private final GroupDAO        groupDAO = new GroupDAO();
    private final EventDAO        eventDAO = new EventDAO();
    private final UserDAO         userDAO  = new UserDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    public void setGroup(Group g) {
        this.group = g;
        fill();
        loadMembers();
        loadEventList();
        loadSubs();
        buildGroupCalendar();
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
    }

    // ── Members tab ───────────────────────────────────────────────────────

    private void loadMembers() {
        membersList.getChildren().clear();
        try {
            for (User u : groupDAO.members(group.getId()))
                membersList.getChildren().add(memberRow(u));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private HBox memberRow(User u) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(8));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color:#F1F5F9;-fx-border-width:0 0 1 0;");

        ImageView av = new ImageView();
        av.setFitWidth(36); av.setFitHeight(36);
        Image img = Imgs.fromBytes(u.getProfilePicture());
        if (img != null) av.setImage(img);
        Imgs.circle(av, 18);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(u.getDisplayName());
        name.setStyle("-fx-font-weight:bold;-fx-font-size:13;");
        String role = u.getBio(); // role stored in bio field
        Label sub = new Label("@" + u.getUsername() + " • " + (role != null ? role.toUpperCase() : "MEMBER"));
        sub.setStyle("-fx-text-fill:#888;-fx-font-size:11;");
        info.getChildren().addAll(name, sub);

        row.getChildren().addAll(av, info);

        if (group.isAdmin() && u.getId() != Session.uid()) {
            boolean isAdmin = "admin".equals(role);
            Button roleBtn = new Button(isAdmin ? "Remove Admin" : "Make Admin");
            roleBtn.getStyleClass().add(isAdmin ? "btn-warning-small" : "btn-primary-small");
            roleBtn.setOnAction(e -> {
                try {
                    String newRole = isAdmin ? "member" : "admin";
                    groupDAO.setRole(group.getId(), u.getId(), newRole);
                    Notification n = new Notification(u.getId(),
                        "admin".equals(newRole) ? "⭐ You're now an Admin" : "🔻 Admin Role Removed",
                        "admin".equals(newRole)
                            ? "You've been made admin of " + group.getName()
                            : "Your admin role in " + group.getName() + " was removed.",
                        "admin".equals(newRole) ? "admin_added" : "admin_removed");
                    n.setReferenceId(group.getId());
                    notifDAO.create(n);
                    loadMembers();
                } catch (Exception ex) { ex.printStackTrace(); }
            });
            Button remBtn = new Button("Remove");
            remBtn.getStyleClass().add("btn-danger-small");
            remBtn.setOnAction(e -> {
                new Alert(Alert.AlertType.CONFIRMATION, "Remove " + u.getDisplayName() + "?", ButtonType.YES, ButtonType.NO)
                    .showAndWait().ifPresent(b -> {
                        if (b == ButtonType.YES) {
                            try { groupDAO.removeMember(group.getId(), u.getId()); loadMembers(); }
                            catch (Exception ex) { ex.printStackTrace(); }
                        }
                    });
            });
            row.getChildren().addAll(roleBtn, remBtn);
        }
        return row;
    }

    @FXML private void doSearchUser() {
        String q = userSearchField.getText().trim();
        userSearchResults.getChildren().clear();
        if (q.isEmpty()) return;
        try {
            List<User> users = userDAO.search(q, Session.uid());
            for (User u : users) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(5));
                Label name = new Label(u.getDisplayName() + " (@" + u.getUsername() + ")");
                HBox.setHgrow(name, Priority.ALWAYS);

                Button invBtn = new Button("Send Invite");
                invBtn.getStyleClass().add("btn-primary-small");
                invBtn.setOnAction(e -> sendInvite(u));

                row.getChildren().addAll(name, invBtn);
                userSearchResults.getChildren().add(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Issue #4 – Instead of immediately adding the user, send them a group invitation.
     * The user accepts/declines from their Notifications page.
     */
    private void sendInvite(User u) {
        try {
            // Check if already a member
            if (groupDAO.isMember(group.getId(), u.getId())) {
                new Alert(Alert.AlertType.INFORMATION, u.getDisplayName() + " is already a member.").showAndWait();
                return;
            }
            Notification invite = new Notification(u.getId(),
                "👥 Group Invitation: " + group.getName(),
                Session.currentUser().getDisplayName() + " invited you to join '" + group.getName() + "'. Accept or decline in notifications.",
                "group_invite");
            invite.setReferenceId(group.getId());
            notifDAO.create(invite);
            new Alert(Alert.AlertType.INFORMATION,
                "Invitation sent to " + u.getDisplayName() + "!\nThey will see it in their Notifications.").showAndWait();
            userSearchResults.getChildren().clear();
            userSearchField.clear();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
        }
    }

    // ── Events list tab ───────────────────────────────────────────────────

    private void loadEventList() {
        eventsList.getChildren().clear();
        try {
            List<Event> events = eventDAO.forGroup(group.getId());
            if (events.isEmpty()) { eventsList.getChildren().add(noData("No events yet.")); return; }
            String color = ColorUtil.forGroup(group.getId());
            for (Event e : events) {
                HBox row = new HBox(10);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color:white;-fx-border-color:" + color
                    + ";-fx-border-width:0 0 0 3;-fx-cursor:hand;");
                VBox info = new VBox(2);
                HBox.setHgrow(info, Priority.ALWAYS);
                Label t = new Label(e.getTitle()); t.setStyle("-fx-font-weight:bold;-fx-font-size:13;");
                Label d = new Label(e.getStartTime().format(FMT)); d.setStyle("-fx-text-fill:#888;-fx-font-size:11;");
                info.getChildren().addAll(t, d);
                row.getChildren().add(info);
                row.setOnMouseClicked(ev -> openEventDetail(e));
                eventsList.getChildren().add(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
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

        Button prev = new Button("◀"); prev.getStyleClass().add("nav-btn");
        prev.setOnAction(e -> buildGroupCalendar(groupCalYm.minusMonths(1)));

        Label monthLbl = new Label(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        monthLbl.setStyle("-fx-font-weight:bold;-fx-font-size:14;");

        Button next = new Button("▶"); next.getStyleClass().add("nav-btn");
        next.setOnAction(e -> buildGroupCalendar(groupCalYm.plusMonths(1)));

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

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
        String[] days = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        for (int i = 0; i < 7; i++) {
            Label l = new Label(days[i]);
            l.setStyle("-fx-text-fill:#64748B;-fx-font-weight:bold;-fx-font-size:11;-fx-padding:5 0;-fx-alignment:CENTER;");
            l.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(l, Priority.ALWAYS);
            GridPane.setFillWidth(l, true);
            dow.add(l, i, 0);
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0/7);
            if (i < dow.getColumnConstraints().size()) {} else dow.getColumnConstraints().add(cc);
        }
        groupCalendarContainer.getChildren().add(dow);

        // Calendar grid
        GridPane grid = new GridPane();
        grid.setStyle("-fx-background-color:white;");
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0/7);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        Map<LocalDate, List<Event>> byDay = new HashMap<>();
        try {
            List<Event> events = eventDAO.forGroupInMonth(group.getId(), ym.getYear(), ym.getMonthValue());
            for (Event e : events)
                byDay.computeIfAbsent(e.getStartTime().toLocalDate(), k -> new ArrayList<>()).add(e);
        } catch (Exception e) { e.printStackTrace(); }

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
                    cell.getChildren().add(new Label("+" + (dayEvents.size()-2) + " more"));
                    break;
                }
                Label tag = new Label(e.getTitle());
                tag.setMaxWidth(Double.MAX_VALUE);
                tag.setPadding(new Insets(1,4,1,4));
                tag.setStyle("-fx-background-color:" + groupColor
                    + ";-fx-text-fill:white;-fx-background-radius:3;-fx-font-size:10;-fx-font-weight:bold;");
                tag.setCursor(javafx.scene.Cursor.HAND);
                final Event ev = e;
                tag.setOnMouseClicked(click -> { click.consume(); openEventDetail(ev); });
                cell.getChildren().add(tag);
                shown++;
            }
            grid.add(cell, col, row);
            if (++col == 7) { col = 0; row++; }
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
            if (subs.isEmpty()) { subGroupsList.getChildren().add(noData("No sub-groups.")); return; }
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
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Actions ───────────────────────────────────────────────────────────

    @FXML private void doAddEvent() {
        EventFormController ctrl = Navigator.showModal("/com/calendarapp/fxml/event_form.fxml");
        if (ctrl != null) {
            ctrl.setGroup(group);
            ctrl.setOnClose(() -> { loadEventList(); buildGroupCalendar(); });
        }
    }

    @FXML private void doEditGroup() {
        CreateGroupController ctrl = Navigator.showModal("/com/calendarapp/fxml/create_group.fxml");
        if (ctrl != null) {
            ctrl.setGroup(group);
            ctrl.setOnClose(() -> {
                try { group = groupDAO.findById(group.getId(), Session.uid()); fill(); }
                catch (Exception ex) { ex.printStackTrace(); }
            });
        }
    }

    @FXML private void doLeave() {
        new Alert(Alert.AlertType.CONFIRMATION, "Leave " + group.getName() + "?", ButtonType.YES, ButtonType.NO)
            .showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    try { groupDAO.removeMember(group.getId(), Session.uid()); Navigator.pop(); }
                    catch (Exception e) { e.printStackTrace(); }
                }
            });
    }

    private void openEventDetail(Event e) {
        EventDetailController ctrl = Navigator.showModal("/com/calendarapp/fxml/event_detail.fxml");
        if (ctrl != null) ctrl.setEvent(e, () -> { loadEventList(); buildGroupCalendar(); });
    }

    private void openSubGroup(Group g) {
        GroupDetailController ctrl = Navigator.push("/com/calendarapp/fxml/group_detail.fxml");
        if (ctrl != null) ctrl.setGroup(g);
    }

    private Label noData(String m) {
        Label l = new Label(m); l.setStyle("-fx-text-fill:#aaa;-fx-padding:12;"); return l;
    }
}
