package com.calendarapp.dao;

import com.calendarapp.model.Event;
import com.calendarapp.util.DB;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    private static final String SELECT =
        "SELECT e.*, u.username AS cname, g.name AS gname "
      + "FROM events e "
      + "LEFT JOIN users u ON e.created_by=u.id "
      + "LEFT JOIN groups_tbl g ON e.group_id=g.id ";

    private static final String VISIBLE =
        "AND (  (e.event_type='personal' AND e.created_by=?) "
      + "    OR (e.event_type='group'    AND e.group_id IN "
      + "        (SELECT group_id FROM group_members WHERE user_id=?)) )";

    public Event create(Event e) throws SQLException {
        String sql = "INSERT INTO events (title,description,location,start_time,end_time,event_type,group_id,created_by,color,is_all_day) "
                   + "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getTitle());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getLocation());
            ps.setTimestamp(4, Timestamp.valueOf(e.getStartTime()));
            ps.setTimestamp(5, Timestamp.valueOf(e.getEndTime()));
            ps.setString(6, e.getEventType());
            if (e.getGroupId() != null) ps.setInt(7, e.getGroupId()); else ps.setNull(7, Types.INTEGER);
            ps.setInt(8, e.getCreatedBy());
            ps.setString(9, e.getColor());
            ps.setBoolean(10, e.isAllDay());
            ps.executeUpdate();
            ResultSet k = ps.getGeneratedKeys();
            if (k.next()) return findById(k.getInt(1));
        }
        return null;
    }

    public Event findById(int id) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(SELECT + "WHERE e.id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public List<Event> forMonth(int uid, int year, int month) throws SQLException {
        LocalDateTime from = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime to   = from.plusMonths(1).minusSeconds(1);
        return inRange(uid, from, to);
    }

    public List<Event> forDay(int uid, LocalDate d) throws SQLException {
        return inRange(uid, d.atStartOfDay(), d.atTime(23, 59, 59));
    }

    public List<Event> inRange(int uid, LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = SELECT + "WHERE e.start_time BETWEEN ? AND ? " + VISIBLE + " ORDER BY e.start_time";
        List<Event> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            ps.setInt(3, uid); ps.setInt(4, uid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Event> upcoming(int uid, int minutesAhead) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        return inRange(uid, now, now.plusMinutes(minutesAhead));
    }

    /** Get all events for a specific group (for group event list). */
    public List<Event> forGroup(int groupId) throws SQLException {
        String sql = SELECT + "WHERE e.group_id=? ORDER BY e.start_time DESC";
        List<Event> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Get group events for a specific month (for group calendar view). */
    public List<Event> forGroupInMonth(int groupId, int year, int month) throws SQLException {
        LocalDateTime from = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime to   = from.plusMonths(1).minusSeconds(1);
        String sql = SELECT + "WHERE e.group_id=? AND e.start_time BETWEEN ? AND ? ORDER BY e.start_time";
        List<Event> list = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setTimestamp(2, Timestamp.valueOf(from));
            ps.setTimestamp(3, Timestamp.valueOf(to));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void update(Event e) throws SQLException {
        String sql = "UPDATE events SET title=?,description=?,location=?,start_time=?,end_time=?,color=?,is_all_day=? WHERE id=?";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getTitle());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getLocation());
            ps.setTimestamp(4, Timestamp.valueOf(e.getStartTime()));
            ps.setTimestamp(5, Timestamp.valueOf(e.getEndTime()));
            ps.setString(6, e.getColor());
            ps.setBoolean(7, e.isAllDay());
            ps.setInt(8, e.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement("DELETE FROM events WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    private static Event map(ResultSet rs) throws SQLException {
        Event e = new Event();
        e.setId(rs.getInt("id"));
        e.setTitle(rs.getString("title"));
        e.setDescription(rs.getString("description"));
        e.setLocation(rs.getString("location"));
        Timestamp st = rs.getTimestamp("start_time");
        if (st != null) e.setStartTime(st.toLocalDateTime());
        Timestamp et = rs.getTimestamp("end_time");
        if (et != null) e.setEndTime(et.toLocalDateTime());
        e.setEventType(rs.getString("event_type"));
        int gid = rs.getInt("group_id");
        if (!rs.wasNull()) e.setGroupId(gid);
        e.setGroupName(rs.getString("gname"));
        e.setCreatedBy(rs.getInt("created_by"));
        e.setCreatedByUsername(rs.getString("cname"));
        e.setColor(rs.getString("color"));
        e.setAllDay(rs.getBoolean("is_all_day"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) e.setCreatedAt(ca.toLocalDateTime());
        return e;
    }
}
