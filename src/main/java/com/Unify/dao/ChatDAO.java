package com.Unify.dao;

import com.Unify.model.ChatMessage;
import com.Unify.util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatDAO {

    // ── The Master SQL Query ────────────────────────────────────────────────
    // This query fetches the message, the sender's username & picture,
    // AND it does a LEFT JOIN to fetch the text and author of the message being replied to!
    private static final String FETCH_SQL =
            "SELECT m.*, u.username, u.profile_picture, " +
                    "p.message AS reply_message, pu.username AS reply_username " +
                    "FROM group_messages m " +
                    "JOIN users u ON m.sender_id = u.id " +
                    "LEFT JOIN group_messages p ON m.reply_to_id = p.id " +
                    "LEFT JOIN users pu ON p.sender_id = pu.id " +
                    "WHERE m.group_id = ?";

    // ── Write Operations ────────────────────────────────────────────────────

    public boolean sendMessage(int groupId, int senderId, String message, Integer replyToId) {
        String sql = "INSERT INTO group_messages (group_id, sender_id, message, reply_to_id) VALUES (?, ?, ?, ?)";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, senderId);
            ps.setString(3, message);

            // Handle the optional reply ID
            if (replyToId != null) {
                ps.setInt(4, replyToId);
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── Read Operations ─────────────────────────────────────────────────────

    // Fetch the entire chat history for a group (used when opening the page)
    public List<ChatMessage> getMessagesForGroup(int groupId) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = FETCH_SQL + " ORDER BY m.created_at ASC";

        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Fetch ONLY new messages (used by the real-time background poller)
    public List<ChatMessage> getModifiedMessages(int groupId, Timestamp since) {
        List<ChatMessage> list = new ArrayList<>();
        // Now we check updated_at instead of created_at!
        String sql = FETCH_SQL + " AND m.updated_at > ? ORDER BY m.created_at ASC";

        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setTimestamp(2, since);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── Helper Mapping Method ───────────────────────────────────────────────

    private ChatMessage mapRow(ResultSet rs) throws SQLException {
        // Safely extract the reply ID (it might be null in the DB)
        Integer replyId = rs.getInt("reply_to_id");
        if (rs.wasNull()) {
            replyId = null;
        }

        // Return a fully constructed ChatMessage object
        return new ChatMessage(
                rs.getInt("id"),
                rs.getInt("group_id"),
                rs.getInt("sender_id"),
                rs.getString("username"),
                rs.getBytes("profile_picture"), // Used for the UI avatars
                rs.getString("message"),
                rs.getTimestamp("created_at"),
                replyId,
                rs.getString("reply_username"),
                rs.getString("reply_message"),
                rs.getBoolean("is_deleted"),
                rs.getTimestamp("updated_at")
        );
    }
    // Fetch only the absolute latest message for the sidebar preview
    public ChatMessage getLastMessage(int groupId) {
        // We reuse your FETCH_SQL but order it backwards and limit it to 1
        String sql = FETCH_SQL + " ORDER BY m.created_at DESC LIMIT 1";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public boolean deleteMessage(int messageId, int currentUserId) {
        String sql = "UPDATE group_messages SET is_deleted = TRUE, message = 'This message was deleted' WHERE id = ? AND sender_id = ?";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, currentUserId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}