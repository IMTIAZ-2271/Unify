package com.Unify.util;

import com.Unify.AppData;
import com.Unify.Session;
import com.Unify.dao.*;
import com.Unify.model.*;

import java.util.List;

/**
 * Called once after login. Fetches all user data in parallel
 * and puts it into AppData.
 */
public class DataLoader {

    private final EventDAO eventDAO = new EventDAO();
    private final GroupDAO groupDAO = new GroupDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();

    /**
     * Blocking — run this in a background thread
     */
    public void loadAll() throws Exception {
        int uid = Session.uid();

        // Fetch everything (in parallel for speed)
        List<Event> events = eventDAO.allForUser(uid);
        List<Group> groups = groupDAO.myGroups(uid);
        List<Notification> notifs = notifDAO.forUser(uid);
        List<JoinRequest> requests = groupDAO.pendingForAdmin(uid);
        List<JoinRequest> sentRequests = groupDAO.mySentRequests(uid);

        AppData.get().loadAll(events, groups, notifs, requests, sentRequests);
    }
}