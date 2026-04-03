package com.Unify.dao;

import com.Unify.model.Announcement;
import com.Unify.util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDAO {

    public boolean createAnnouncement(int groupId, int authorId, String title, String content) {
        String sql = "INSERT INTO announcements (group_id, author_id, title, content) VALUES (?, ?, ?, ?)";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, authorId);
            ps.setString(3, title);
            ps.setString(4, content);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Announcement> getAnnouncementsForGroup(int groupId) {
        List<Announcement> list = new ArrayList<>();
        // JOIN users table to get the author's username
        String sql = "SELECT a.*, u.username FROM announcements a JOIN users u ON a.author_id = u.id WHERE a.group_id = ? ORDER BY a.created_at DESC";

        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Announcement(
                        rs.getInt("id"), rs.getInt("group_id"), rs.getInt("author_id"),
                        rs.getString("username"), rs.getString("title"),
                        rs.getString("content"), rs.getTimestamp("created_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}