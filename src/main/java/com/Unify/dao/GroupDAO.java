package com.Unify.dao;

import com.Unify.model.Group;
import com.Unify.model.JoinRequest;
import com.Unify.model.User;
import com.Unify.util.DB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupDAO {

    // ── Create ───────────────────────────────────────────────────────────────

    public Group create(String name, String desc, int createdBy, Integer parentId) throws SQLException {
        String code = "GRP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sql = "INSERT INTO groups_tbl (group_code,name,description,created_by,parent_group_id) VALUES (?,?,?,?,?)";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, desc);
            ps.setInt(4, createdBy);
            if (parentId != null) ps.setInt(5, parentId);
            else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
            ResultSet k = ps.getGeneratedKeys();
            if (k.next()) {
                int gid = k.getInt(1);
                addMember(gid, createdBy, "admin");
                return findById(gid, createdBy);
            }
        }
        return null;
    }

    // ── Find ─────────────────────────────────────────────────────────────────

    public Group findById(int id, int uid) throws SQLException {
        String sql = BASE_SELECT + " WHERE g.id=?";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, uid);
            ps.setInt(2, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public List<Group> myGroups(int uid) throws SQLException {
        String sql = BASE_SELECT + " JOIN group_members gm2 ON g.id=gm2.group_id AND gm2.user_id=? ORDER BY g.name";
        // re-use base but add forced join for "my groups"
        String full = "SELECT g.*, "
                + "(SELECT COUNT(*) FROM group_members WHERE group_id=g.id) AS mc, "
                + "(SELECT role FROM group_members WHERE group_id=g.id AND user_id=?) AS ur, "
                + "pg.name AS parent_name "
                + "FROM groups_tbl g "
                + "LEFT JOIN groups_tbl pg ON g.parent_group_id=pg.id "
                + "JOIN group_members gm2 ON g.id=gm2.group_id AND gm2.user_id=? "
                + "ORDER BY g.name";
        List<Group> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(full)) {
            ps.setInt(1, uid);
            ps.setInt(2, uid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

//    public List<Group> search(String q, int uid) throws SQLException {
////        String full = "SELECT g.*, "
////            + "(SELECT COUNT(*) FROM group_members WHERE group_id=g.id) AS mc, "
////            + "(SELECT role FROM group_members WHERE group_id=g.id AND user_id=?) AS ur, "
////            + "pg.name AS parent_name "
////            + "FROM groups_tbl g "
////            + "LEFT JOIN groups_tbl pg ON g.parent_group_id=pg.id "
////            + "WHERE g.name LIKE ? OR g.group_code LIKE ? LIMIT 30";
//
//
//        String full = "SELECT DISTINCT g.* "
//                + "FROM groups_tbl g "
//                + "LEFT JOIN group_members gm "
//                + "ON g.parent_group_id = gm.group_id "
//                //               + "AND gm.user_id = ? "
//                + "WHERE (g.name LIKE ? OR g.group_code LIKE ?)"
//                + "AND (g.parent_group_id IS NULL OR ? )";
//        List<Group> list = new ArrayList<>();
//        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(full)) {
//            String w = "%" + q + "%";
////            ps.setInt(1, Session.uid());
//            ps.setString(1, w);
//            ps.setString(2, w);
//            ps.setInt(3, uid);
//            ResultSet rs = ps.executeQuery();
//            while (rs.next()) list.add(map(rs));
//        }
//        return list;
//    }

public List<Group> search(String q, int uid) throws SQLException {
    // We added the mc, ur, and parent_name columns back into the SELECT statement
    String full = "SELECT DISTINCT g.*, "
            + "(SELECT COUNT(*) FROM group_members WHERE group_id=g.id) AS mc, "
            + "(SELECT role FROM group_members WHERE group_id=g.id AND user_id=?) AS ur, "
            + "pg.name AS parent_name "
            + "FROM groups_tbl g "
            + "LEFT JOIN groups_tbl pg ON g.parent_group_id=pg.id "
            + "LEFT JOIN group_members gm ON g.parent_group_id = gm.group_id "
            + "WHERE (g.name LIKE ? OR g.group_code LIKE ?) "
            + "AND (g.parent_group_id IS NULL OR ? )";

    List<Group> list = new ArrayList<>();
    try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(full)) {
        String w = "%" + q + "%";
        ps.setInt(1, uid);  // 1st ?: For the 'ur' subquery
        ps.setString(2, w); // 2nd ?: For name search
        ps.setString(3, w); // 3rd ?: For group_code search
        ps.setInt(4, uid);  // 4th ?: For the OR condition at the end

        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(map(rs));
    }
    return list;
}

    public List<Group> subGroups(int parentId, int uid) throws SQLException {
        String full = "SELECT g.*, "
                + "(SELECT COUNT(*) FROM group_members WHERE group_id=g.id) AS mc, "
                + "(SELECT role FROM group_members WHERE group_id=g.id AND user_id=?) AS ur, "
                + "pg.name AS parent_name "
                + "FROM groups_tbl g "
                + "LEFT JOIN groups_tbl pg ON g.parent_group_id=pg.id "
                + "WHERE g.parent_group_id=?";
        List<Group> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(full)) {
            ps.setInt(1, uid);
            ps.setInt(2, parentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    // ── Members ──────────────────────────────────────────────────────────────

    public void addMember(int groupId, int userId, String role) throws SQLException {
        String sql = "INSERT INTO group_members (group_id,user_id,role) VALUES (?,?,?) "
                + "ON DUPLICATE KEY UPDATE role=VALUES(role)";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ps.setString(3, role);
            ps.executeUpdate();
        }
    }

    public void removeMember(int groupId, int userId) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM group_members WHERE group_id=? AND user_id=?")) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void setRole(int groupId, int userId, String role) throws SQLException {
        // Clears assigned_work if the role is changed to admin or member
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(
                "UPDATE group_members SET role=?, assigned_work=NULL WHERE group_id=? AND user_id=?")) {
            ps.setString(1, role);
            ps.setInt(2, groupId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public void setModeratorRole(int groupId, int userId, String assignedWork) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(
                "UPDATE group_members SET role='moderator', assigned_work=? WHERE group_id=? AND user_id=?")) {
            ps.setString(1, assignedWork);
            ps.setInt(2, groupId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public boolean isMember(int groupId, int userId) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM group_members WHERE group_id=? AND user_id=?")) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            return ps.executeQuery().next();
        }
    }

    public boolean isAdmin(int groupId, int userId) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM group_members WHERE group_id=? AND user_id=? AND role='admin'")) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            return ps.executeQuery().next();
        }
    }

    public List<User> members(int groupId) throws SQLException {
        // Fetch assigned_work (gwork) alongside the role
        String sql = "SELECT u.*, gm.role AS grole, gm.assigned_work AS gwork FROM users u "
                + "JOIN group_members gm ON u.id=gm.user_id WHERE gm.group_id=? ORDER BY gm.role DESC, u.display_name";
        List<User> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = UserDAO.map(rs);
                String role = rs.getString("grole");
                String work = rs.getString("gwork");

                // Bundle role and work into the bio field so the Controller can read it easily
                if ("moderator".equals(role) && work != null) {
                    u.setBio(role + ":" + work);
                } else {
                    u.setBio(role);
                }
                list.add(u);
            }
        }
        return list;
    }

    // ── Update ───────────────────────────────────────────────────────────────

    public void update(int id, String name, String desc, byte[] pic) throws SQLException {
        String sql = pic != null
                ? "UPDATE groups_tbl SET name=?,description=?,profile_picture=? WHERE id=?"
                : "UPDATE groups_tbl SET name=?,description=? WHERE id=?";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, desc);
            if (pic != null) {
                ps.setBytes(3, pic);
                ps.setInt(4, id);
            } else {
                ps.setInt(3, id);
            }
            ps.executeUpdate();
        }
    }

    // ── Join Requests ────────────────────────────────────────────────────────

    public void requestJoin(int groupId, int userId) throws SQLException {
        String sql = "INSERT INTO join_requests (group_id,user_id) VALUES (?,?) "
                + "ON DUPLICATE KEY UPDATE status='pending', requested_at=NOW()";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public List<JoinRequest> pendingForAdmin(int adminUserId) throws SQLException {
        String sql = "SELECT jr.*, u.username, u.display_name, u.profile_picture, g.name AS gname "
                + "FROM join_requests jr "
                + "JOIN users u ON jr.user_id=u.id "
                + "JOIN groups_tbl g ON jr.group_id=g.id "
                + "JOIN group_members gm ON g.id=gm.group_id AND gm.user_id=? AND gm.role='admin' "
                + "WHERE jr.status='pending' ORDER BY jr.requested_at DESC";
        List<JoinRequest> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, adminUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapJR(rs));
        }
        return list;
    }

    /**
     * Returns all join requests the user has sent
     * that are still pending or were declined.
     */
    public List<JoinRequest> mySentRequests(int userId) throws SQLException {
        String sql =
                "SELECT jr.*, "
                        + "g.name AS gname, g.group_code, g.profile_picture AS gpic, "
                        + "u.display_name AS responded_by "
                        + "FROM join_requests jr "
                        + "JOIN groups_tbl g   ON jr.group_id     = g.id "
                        + "LEFT JOIN users u   ON jr.responded_by = u.id "
                        + "WHERE jr.user_id = ? "
                        + "AND jr.status IN ('pending', 'declined') "
                        + "ORDER BY jr.requested_at DESC";

        List<JoinRequest> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapSent(rs));
        }
        return list;
    }

    private static JoinRequest mapSent(ResultSet rs) throws SQLException {
        JoinRequest r = new JoinRequest();
        r.setId(rs.getInt("id"));
        r.setGroupId(rs.getInt("group_id"));
        r.setGroupName(rs.getString("gname"));
        r.setUserId(rs.getInt("user_id"));
        r.setStatus(rs.getString("status"));

        // Who responded (null if still pending)
        String respondedBy = rs.getString("responded_by");
        if (respondedBy != null) r.setDisplayName("Reviewed by: " + respondedBy);

        Timestamp ra = rs.getTimestamp("requested_at");
        if (ra != null) r.setRequestedAt(ra.toLocalDateTime());

        return r;
    }

    public JoinRequest findRequest(int id) throws SQLException {
        String sql = "SELECT jr.*, u.username, u.display_name, u.profile_picture, g.name AS gname "
                + "FROM join_requests jr JOIN users u ON jr.user_id=u.id "
                + "JOIN groups_tbl g ON jr.group_id=g.id WHERE jr.id=?";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapJR(rs) : null;
        }
    }

    public void respond(int requestId, String status, int respondedBy) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(
                "UPDATE join_requests SET status=?,responded_at=NOW(),responded_by=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, respondedBy);
            ps.setInt(3, requestId);
            ps.executeUpdate();
        }
        if ("accepted".equals(status)) {
            JoinRequest jr = findRequest(requestId);
            if (jr != null) addMember(jr.getGroupId(), jr.getUserId(), "member");
        }
    }

    public int pendingCount(int adminUserId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM join_requests jr "
                + "JOIN group_members gm ON jr.group_id=gm.group_id AND gm.user_id=? AND gm.role='admin' "
                + "WHERE jr.status='pending'";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, adminUserId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private static final String BASE_SELECT =
            "SELECT g.*, "
                    + "(SELECT COUNT(*) FROM group_members WHERE group_id=g.id) AS mc, "
                    + "(SELECT role FROM group_members WHERE group_id=g.id AND user_id=?) AS ur, "
                    + "pg.name AS parent_name "
                    + "FROM groups_tbl g "
                    + "LEFT JOIN groups_tbl pg ON g.parent_group_id=pg.id ";

    private static Group map(ResultSet rs) throws SQLException {
        Group g = new Group();
        g.setId(rs.getInt("id"));
        g.setGroupCode(rs.getString("group_code"));
        g.setName(rs.getString("name"));
        g.setDescription(rs.getString("description"));
        g.setProfilePicture(rs.getBytes("profile_picture"));
        g.setCreatedBy(rs.getInt("created_by"));
        g.setMemberCount(rs.getInt("mc"));
        g.setCurrentUserRole(rs.getString("ur"));
        g.setParentGroupName(rs.getString("parent_name"));
        int pid = rs.getInt("parent_group_id");
        if (!rs.wasNull()) g.setParentGroupId(pid);
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) g.setCreatedAt(ca.toLocalDateTime());
        return g;
    }

    private static JoinRequest mapJR(ResultSet rs) throws SQLException {
        JoinRequest r = new JoinRequest();
        r.setId(rs.getInt("id"));
        r.setGroupId(rs.getInt("group_id"));
        r.setGroupName(rs.getString("gname"));
        r.setUserId(rs.getInt("user_id"));
        r.setUsername(rs.getString("username"));
        r.setDisplayName(rs.getString("display_name"));
        r.setUserPic(rs.getBytes("profile_picture"));
        r.setStatus(rs.getString("status"));
        Timestamp ra = rs.getTimestamp("requested_at");
        if (ra != null) r.setRequestedAt(ra.toLocalDateTime());
        return r;
    }

    /**
     * Returns all groups the user belongs to that have changed since the given time.
     * Covers: group details updated, membership changes, role changes.
     */
    public List<Group> changedSince(int uid, LocalDateTime since) throws SQLException {
        String sql =
                "SELECT g.*, "
                        + "(SELECT COUNT(*) FROM group_members WHERE group_id=g.id) AS mc, "
                        + "(SELECT role FROM group_members WHERE group_id=g.id AND user_id=?) AS ur, "
                        + "pg.name AS parent_name "
                        + "FROM groups_tbl g "
                        + "LEFT JOIN groups_tbl pg ON g.parent_group_id=pg.id "
                        + "WHERE g.id IN ( "
                        + "    SELECT DISTINCT gm.group_id FROM group_members gm "
                        + "    WHERE gm.user_id=? "          // user is a member
                        + ") "
                        + "AND ( "
                        + "    g.last_modified > ? "          // group details changed
                        + "    OR EXISTS ( "                  // membership/roles changed
                        + "        SELECT 1 FROM group_members gm2 "
                        + "        WHERE gm2.group_id=g.id "
                        + "        AND gm2.last_modified > ? "
                        + "    ) "
                        + ")";

        List<Group> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, uid);
            ps.setInt(2, uid);
            ps.setTimestamp(3, Timestamp.valueOf(since));
            ps.setTimestamp(4, Timestamp.valueOf(since));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /**
     * Returns pending join requests for groups the user admins,
     * that arrived or changed since the given time.
     */
    public List<JoinRequest> joinRequestsChangedSince(int adminUid, LocalDateTime since)
            throws SQLException {
        String sql =
                "SELECT jr.*, u.username, u.display_name, u.profile_picture, g.name AS gname "
                        + "FROM join_requests jr "
                        + "JOIN users u ON jr.user_id=u.id "
                        + "JOIN groups_tbl g ON jr.group_id=g.id "
                        + "JOIN group_members gm ON g.id=gm.group_id "
                        + "    AND gm.user_id=? AND gm.role='admin' "
                        + "WHERE jr.last_modified > ? "
                        + "AND jr.status='pending' "
                        + "ORDER BY jr.requested_at DESC";

        List<JoinRequest> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, adminUid);
            ps.setTimestamp(2, Timestamp.valueOf(since));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapJR(rs));
        }
        return list;
    }

    public String getMessagingPermission(int groupId) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement("SELECT messaging_permission FROM groups_tbl WHERE id=?")) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : "ALL";
        }
    }

    public void setMessagingPermission(int groupId, String permission) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement("UPDATE groups_tbl SET messaging_permission=? WHERE id=?")) {
            ps.setString(1, permission);
            ps.setInt(2, groupId);
            ps.executeUpdate();
        }
    }

}
