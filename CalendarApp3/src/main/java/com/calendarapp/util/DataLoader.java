package com.calendarapp.util;

import com.calendarapp.AppData;
import com.calendarapp.Session;
import com.calendarapp.dao.*;
import com.calendarapp.model.*;

import java.util.List;

/**
 * Called once after login. Fetches all user data in parallel
 * and puts it into AppData.
 */
public class DataLoader {

    private final EventDAO        eventDAO  = new EventDAO();
    private final GroupDAO        groupDAO  = new GroupDAO();
    private final NotificationDAO notifDAO  = new NotificationDAO();

    /** Blocking — run this in a background thread */
    public void loadAll() throws Exception {
        int uid = Session.uid();

        // Fetch everything (in parallel for speed)
        List<Event>        events    = eventDAO.allForUser(uid);
        List<Group>        groups    = groupDAO.myGroups(uid);
        List<Notification> notifs    = notifDAO.forUser(uid);
        List<JoinRequest>  requests  = groupDAO.pendingForAdmin(uid);

        AppData.get().loadAll(events, groups, notifs, requests);
    }
}