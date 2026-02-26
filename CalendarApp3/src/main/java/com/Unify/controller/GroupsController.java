package com.Unify.controller;

import com.Unify.Navigator;
import com.Unify.Session;
import com.Unify.dao.GroupDAO;
import com.Unify.dao.NotificationDAO;
import com.Unify.model.Group;
import com.Unify.model.JoinRequest;
import com.Unify.model.Notification;
import com.Unify.util.ColorUtil;
import com.Unify.util.Imgs;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;

public class GroupsController {

    @FXML
    private VBox myGroupsList;
    @FXML
    private VBox searchResultsList;
    @FXML
    private VBox requestsList;
    @FXML
    private TextField searchField;

    private final GroupDAO groupDAO = new GroupDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();

    @FXML
    private void initialize() {
        loadMyGroups();
        loadRequests();
    }

    @FXML
    private void loadMyGroups() {
        myGroupsList.getChildren().clear();
        try {
            List<Group> groups = groupDAO.myGroups(Session.uid());
            if (groups.isEmpty()) {
                myGroupsList.getChildren().add(noData("No groups yet."));
                return;
            }
            for (Group g : groups) myGroupsList.getChildren().add(groupCard(g));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void doSearch() {
        String q = searchField.getText().trim();
        searchResultsList.getChildren().clear();
        if (q.isEmpty()) return;
        try {
            List<Group> groups = groupDAO.search(q, Session.uid());
            if (groups.isEmpty()) {
                searchResultsList.getChildren().add(noData("No groups found."));
                return;
            }
            for (Group g : groups) searchResultsList.getChildren().add(groupCard(g));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRequests() {
        requestsList.getChildren().clear();
        try {
            List<JoinRequest> reqs = groupDAO.pendingForAdmin(Session.uid());
            if (reqs.isEmpty()) {
                requestsList.getChildren().add(noData("No pending requests."));
                return;
            }
            for (JoinRequest r : reqs) requestsList.getChildren().add(requestCard(r));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox groupCard(Group g) {
        HBox card = new HBox(12);
        card.getStyleClass().add("group-card");
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER_LEFT);

        // Colored group dot
        Circle colorDot = new Circle(8, Color.web(ColorUtil.forGroup(g.getId())));

        ImageView av = new ImageView();
        av.setFitWidth(48);
        av.setFitHeight(48);
        Image img = Imgs.fromBytes(g.getProfilePicture());
        if (img != null) av.setImage(img);
        Imgs.circle(av, 24);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(g.getName());
        name.setStyle("-fx-font-weight:bold;-fx-font-size:14;");
        Label sub = new Label("ID: " + g.getGroupCode() + "  •  " + g.getMemberCount() + " members"
                + (g.isMember() ? "  •  [" + g.getCurrentUserRole().toUpperCase() + "]" : ""));
        sub.setStyle("-fx-text-fill:#000000;-fx-font-size:11;");
        info.getChildren().addAll(name, sub);
        if (g.getParentGroupName() != null) {
            Label p = new Label("Sub-group of: " + g.getParentGroupName());
            p.setStyle("-fx-text-fill:#000000;-fx-font-size:10;");
            info.getChildren().add(p);
        }

        Button viewBtn = new Button("View");
        viewBtn.getStyleClass().add("btn-primary-small");
        viewBtn.setOnAction(e -> openDetail(g));

        HBox btns = new HBox(6, viewBtn);
        btns.setAlignment(Pos.CENTER);
        if (!g.isMember()) {
            Button joinBtn = new Button("Request Join");
            joinBtn.getStyleClass().add("btn-outline-small");
            joinBtn.setOnAction(e -> requestJoin(g));
            btns.getChildren().add(joinBtn);
        }

        card.getChildren().addAll(colorDot, av, info, btns);
        return card;
    }

    private HBox requestCard(JoinRequest r) {
        HBox card = new HBox(12);
        card.getStyleClass().add("request-card");
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.CENTER_LEFT);

        ImageView av = new ImageView();
        av.setFitWidth(40);
        av.setFitHeight(40);
        Image img = Imgs.fromBytes(r.getUserPic());
        if (img != null) av.setImage(img);
        Imgs.circle(av, 20);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label n = new Label(r.getDisplayName());
        n.setStyle("-fx-font-weight:bold;");
        Label g = new Label("Wants to join: " + r.getGroupName());
        g.setStyle("-fx-text-fill:#666;-fx-font-size:11;");
        info.getChildren().addAll(n, g);

        Button acc = new Button("Accept");
        acc.getStyleClass().add("btn-success-small");
        acc.setOnAction(e -> respond(r, "accepted"));

        Button dec = new Button("Decline");
        dec.getStyleClass().add("btn-danger-small");
        dec.setOnAction(e -> respond(r, "declined"));

        card.getChildren().addAll(av, info, acc, dec);
        return card;
    }

    private void respond(JoinRequest r, String status) {
        try {
            groupDAO.respond(r.getId(), status, Session.uid());
            notifDAO.updateInviteAccepted(r.getUserId(), r.getGroupId(), "accepted".equals(status) ? 1 : -1);
            Notification n = new Notification(r.getUserId(),
                    "accepted".equals(status) ? "✅ Join Request Accepted" : "❌ Join Request Declined",
                    "Your request to join " + r.getGroupName() + " was " + status + ".",
                    status);
            n.setReferenceId(r.getGroupId());
            notifDAO.create(n);
            loadRequests();
            loadMyGroups();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestJoin(Group g) {
        try {
            groupDAO.requestJoin(g.getId(), Session.uid());
            new Alert(Alert.AlertType.INFORMATION, "Request sent to join " + g.getName() + "!\nAn admin will review your request.").showAndWait();
            doSearch();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
        }
    }

    private void openDetail(Group g) {
        GroupDetailController ctrl = Navigator.push("/com/Unify/fxml/group_detail.fxml");
        if (ctrl != null) ctrl.setGroup(g);
    }

    @FXML
    private void openCreateGroup() {
        CreateGroupController ctrl = Navigator.showWindow("/com/Unify/fxml/create_group.fxml");
        if (ctrl != null) ctrl.setOnClose(this::loadMyGroups);
    }

    private Label noData(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-text-fill:#aaa;-fx-font-size:13;-fx-padding:20;");
        return l;
    }
}
