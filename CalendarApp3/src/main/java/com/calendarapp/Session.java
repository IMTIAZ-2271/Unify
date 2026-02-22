package com.calendarapp;

import com.calendarapp.model.User;

public class Session {
    private static User user;

    public static void login(User u)  { user = u; }
    public static void logout()       { user = null; }
    public static User currentUser()  { return user; }
    public static boolean isLoggedIn(){ return user != null; }
    public static int uid()           { return user != null ? user.getId() : -1; }
}
