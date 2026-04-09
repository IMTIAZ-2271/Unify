package com.Unify.util;

import com.Unify.AppData;
import com.Unify.Session;
import com.Unify.dao.*;
import com.Unify.model.*;
import com.Unify.service.NotificationService;
import javafx.application.Platform;

import java.util.List;

public class DataLoader {

    private final EventDAO eventDAO = new EventDAO();
    private final GroupDAO groupDAO = new GroupDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();

    public void loadAll() throws Exception {
        int uid = Session.uid();

        List<Event> events = eventDAO.allForUser(uid);
        List<Group> groups = groupDAO.myGroups(uid);
        List<Notification> notifs = notifDAO.forUser(uid);
        List<JoinRequest> requests = groupDAO.pendingForAdmin(uid);
        List<JoinRequest> sentRequests = groupDAO.mySentRequests(uid);

        AppData.get().loadAll(events, groups, notifs, requests, sentRequests);

        // START THE REAL-TIME POLLER HERE
        Platform.runLater(() -> NotificationService.get().start());
    }
}