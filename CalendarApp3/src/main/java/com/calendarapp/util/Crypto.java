package com.calendarapp.util;

import org.mindrot.jbcrypt.BCrypt;

public class Crypto {
    public static String hash(String plain)                    { return BCrypt.hashpw(plain, BCrypt.gensalt(10)); }
    public static boolean check(String plain, String hashed)   { return BCrypt.checkpw(plain, hashed); }
    public static boolean validPassword(String p)              { return p != null && p.length() >= 6; }
    public static boolean validEmail(String e)                 { return e != null && e.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"); }
    public static boolean validUsername(String u)              { return u != null && u.matches("[A-Za-z0-9_]{3,50}"); }
}
