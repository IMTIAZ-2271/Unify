package com.Unify.dao;

import com.Unify.model.Notification;
import com.Unify.util.DB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public void create(Notification n) throws SQLException {
        String sql = "INSERT INTO notifications (user_id,title,message,type,reference_id) VALUES (?,?,?,?,?)";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, n.getUserId());
            ps.setString(2, n.getTitle());
            ps.setString(3, n.getMessage());
            ps.setString(4, n.getType());
            if (n.getReferenceId() != null) ps.setInt(5, n.getReferenceId());
            else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    public void createForGroup(int groupId, String title, String msg, String type, int refId, int excludeUid) throws SQLException {
        String sql = "INSERT INTO notifications (user_id,title,message,type,reference_id) "
                + "SELECT user_id,?,?,?,? FROM group_members WHERE group_id=? AND user_id<>?";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, msg);
            ps.setString(3, type);
            ps.setInt(4, refId);
            ps.setInt(5, groupId);
            ps.setInt(6, excludeUid);
            ps.executeUpdate();
        }
    }

    public List<Notification> forUser(int uid) throws SQLException {
        String sql = "SELECT * FROM notifications WHERE user_id=? ORDER BY created_at DESC LIMIT 60";
        List<Notification> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, uid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public int unreadCount(int uid) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=FALSE")) {
            ps.setInt(1, uid);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void markRead(int id) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(
                "UPDATE notifications SET is_read=TRUE WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void markAllRead(int uid) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(
                "UPDATE notifications SET is_read=TRUE WHERE user_id=?")) {
            ps.setInt(1, uid);
            ps.executeUpdate();
        }
    }

    public void updateInviteAccepted(int userID, int groupID, int inviteAccepted) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(
                "update notifications set invite_accepted=? where type='group_invite' && user_id=? && reference_id=?;")) {
            ps.setInt(1, inviteAccepted);
            ps.setInt(2, userID);
            ps.setInt(3, groupID);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM notifications WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setRead(rs.getBoolean("is_read"));
        n.setInviteAccepted(rs.getInt("invite_accepted"));
        int rid = rs.getInt("reference_id");
        if (!rs.wasNull()) n.setReferenceId(rid);
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) n.setCreatedAt(ca.toLocalDateTime());
        return n;
    }

    public List<Notification> newSince(int uid, LocalDateTime since) throws SQLException {
        String sql = "SELECT * FROM notifications WHERE user_id=? AND last_modified > ? ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, uid);
            ps.setTimestamp(2, Timestamp.valueOf(since));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }
}
