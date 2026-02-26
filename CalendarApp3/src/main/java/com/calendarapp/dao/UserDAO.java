package com.calendarapp.dao;

import com.calendarapp.model.User;
import com.calendarapp.util.Crypto;
import com.calendarapp.util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User create(String username, String email, String password, String displayName) throws SQLException {
        String sql = "INSERT INTO users (username,email,password_hash,display_name) VALUES (?,?,?,?)";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, Crypto.hash(password));
            ps.setString(4, displayName);
            ps.executeUpdate();
            ResultSet k = ps.getGeneratedKeys();
            if (k.next()) return findById(k.getInt(1));
        }
        return null;
    }

    public User authenticate(String usernameOrEmail, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username=? OR email=?";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password_hash");
                if (Crypto.check(password, hash)) {
                    User u = map(rs);
                    touchLogin(u.getId());
                    return u;
                }
            }
        }
        return null;
    }

    public User findById(int id) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public List<User> search(String q, int excludeId) throws SQLException {
        String sql = "SELECT * FROM users WHERE (username LIKE ? OR display_name LIKE ?) AND id<>? LIMIT 20";
        List<User> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            String w = "%" + q + "%";
            ps.setString(1, w); ps.setString(2, w); ps.setInt(3, excludeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<User> searchByGroup(String q, int excludeId, int parentGroupID) throws SQLException {
        //String sql = "SELECT * FROM users WHERE (username LIKE ? OR display_name LIKE ?) AND id<>? LIMIT 20";
        String sql="SELECT * " +
        "FROM users u " +
        "JOIN group_members gm ON u.id = gm.user_id " +
        "WHERE gm.group_id = ? " +
        "AND ( u.username LIKE ? OR u.display_name LIKE ?) AND u.id<>??";
        List<User> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            String w = "%" + q + "%";
            ps.setInt(1,parentGroupID);
            ps.setString(2, w);
            ps.setString(3, w);
            ps.setInt(4, excludeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public boolean usernameExists(String username) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE username=?")) {
            ps.setString(1, username);
            return ps.executeQuery().next();
        }
    }

    public boolean emailExists(String email) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE email=?")) {
            ps.setString(1, email);
            return ps.executeQuery().next();
        }
    }

    public void updateProfile(int id, String displayName, String bio, byte[] pic) throws SQLException {
        String sql = pic != null
            ? "UPDATE users SET display_name=?,bio=?,profile_picture=? WHERE id=?"
            : "UPDATE users SET display_name=?,bio=? WHERE id=?";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, displayName);
            ps.setString(2, bio);
            if (pic != null) { ps.setBytes(3, pic); ps.setInt(4, id); }
            else              { ps.setInt(3, id); }
            ps.executeUpdate();
        }
    }

    public void changePassword(int id, String newPassword) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement("UPDATE users SET password_hash=? WHERE id=?")) {
            ps.setString(1, Crypto.hash(newPassword));
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private void touchLogin(int id) {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement("UPDATE users SET last_login=NOW() WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public static User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setDisplayName(rs.getString("display_name"));
        u.setBio(rs.getString("bio"));
        u.setProfilePicture(rs.getBytes("profile_picture"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) u.setCreatedAt(ca.toLocalDateTime());
        Timestamp ll = rs.getTimestamp("last_login");
        if (ll != null) u.setLastLogin(ll.toLocalDateTime());
        return u;
    }
}
