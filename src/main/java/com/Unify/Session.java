package com.Unify;

import com.Unify.model.User;
import com.Unify.service.NotificationService;

public class Session {
    private static User user;

    public static void login(User u) {
        user = u;
    }

    public static void logout() {
        user = null;
        // STOP THE REAL-TIME POLLER HERE
        NotificationService.get().stop();
    }

    public static User currentUser() {
        return user;
    }

    public static boolean isLoggedIn() {
        return user != null;
    }

    public static int uid() {
        return user != null ? user.getId() : -1;
    }
}