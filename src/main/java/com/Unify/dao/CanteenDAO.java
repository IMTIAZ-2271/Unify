package com.Unify.dao;

import com.Unify.model.Canteen;
import com.Unify.model.FoodItem;
import com.Unify.model.FoodOrder;
import com.Unify.util.DB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CanteenDAO {

    public CanteenDAO() {
        ensureOrderBatchColumn();
    }

    // ─── Canteens ────────────────────────────────────────────────────────────

    public Canteen addCanteen(int groupId, String name) throws SQLException {
        String sql = "INSERT INTO canteens_tbl (group_id, name) VALUES (?, ?)";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, groupId);
            ps.setString(2, name);
            ps.executeUpdate();
            ResultSet k = ps.getGeneratedKeys();
            if (k.next()) return new Canteen(k.getInt(1), groupId, name);
        }
        return null;
    }

    public List<Canteen> getCanteensByGroup(int groupId) throws SQLException {
        List<Canteen> list = new ArrayList<>();
        String sql = "SELECT * FROM canteens_tbl WHERE group_id = ?";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Canteen(rs.getInt("id"), rs.getInt("group_id"), rs.getString("name")));
            }
        }
        return list;
    }

    // ─── Food Items ──────────────────────────────────────────────────────────

    public void addFood(int canteenId, String name, double price, int qty) throws SQLException {
        String sql = "INSERT INTO canteen_foods_tbl (canteen_id, name, price, available_qty) VALUES (?, ?, ?, ?)";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, canteenId);
            ps.setString(2, name);
            ps.setDouble(3, price);
            ps.setInt(4, qty);
            ps.executeUpdate();
        }
    }

    public void deleteFood(int foodId) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement("DELETE FROM canteen_foods_tbl WHERE id=?")) {
            ps.setInt(1, foodId);
            ps.executeUpdate();
        }
    }

    public void updateFoodQty(int foodId, int newQty) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement("UPDATE canteen_foods_tbl SET available_qty=? WHERE id=?")) {
            ps.setInt(1, newQty);
            ps.setInt(2, foodId);
            ps.executeUpdate();
        }
    }
    public void updateFood(int foodId, String name, double price, int qty) throws SQLException {
        String sql = "UPDATE canteen_foods_tbl SET name=?, price=?, available_qty=? WHERE id=?";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setInt(3, qty);
            ps.setInt(4, foodId);
            ps.executeUpdate();
        }
    }

    // Fetch foods. If canteenId is 0, fetch ALL foods for the group.
    public List<FoodItem> getFoods(int groupId, int canteenId) throws SQLException {
        List<FoodItem> list = new ArrayList<>();
        String sql = "SELECT f.*, c.name AS canteen_name FROM canteen_foods_tbl f " +
                "JOIN canteens_tbl c ON f.canteen_id = c.id " +
                "WHERE c.group_id = ? " + (canteenId > 0 ? "AND c.id = ? " : "") +
                "ORDER BY f.name";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            if (canteenId > 0) ps.setInt(2, canteenId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                FoodItem f = new FoodItem();
                f.setId(rs.getInt("id"));
                f.setCanteenId(rs.getInt("canteen_id"));
                f.setCanteenName(rs.getString("canteen_name"));
                f.setName(rs.getString("name"));
                f.setPrice(rs.getDouble("price"));
                f.setAvailableQty(rs.getInt("available_qty"));
                list.add(f);
            }
        }
        return list;
    }

    // ─── Orders & Cart ───────────────────────────────────────────────────────

    // Places the order and safely decrements the quantity using a transaction
    public boolean placeOrder(int userId, int foodId) throws SQLException {
        return placeOrder(userId, foodId, UUID.randomUUID().toString());
    }

    public boolean placeOrder(int userId, int foodId, String orderBatchId) throws SQLException {
        String checkSql = "SELECT available_qty FROM canteen_foods_tbl WHERE id = ? FOR UPDATE";
        String updateSql = "UPDATE canteen_foods_tbl SET available_qty = available_qty - 1 WHERE id = ?";
        String insertSql = "INSERT INTO canteen_orders_tbl (user_id, food_id, order_batch_id, status) VALUES (?, ?, ?, 'pending')";

        try (Connection c = DB.conn()) {
            c.setAutoCommit(false); // Start transaction
            try (PreparedStatement checkPs = c.prepareStatement(checkSql);
                 PreparedStatement updatePs = c.prepareStatement(updateSql);
                 PreparedStatement insertPs = c.prepareStatement(insertSql)) {

                checkPs.setInt(1, foodId);
                ResultSet rs = checkPs.executeQuery();
                if (rs.next() && rs.getInt("available_qty") > 0) {
                    // Decrease Qty
                    updatePs.setInt(1, foodId);
                    updatePs.executeUpdate();
                    // Insert Order
                    insertPs.setInt(1, userId);
                    insertPs.setInt(2, foodId);
                    insertPs.setString(3, orderBatchId);
                    insertPs.executeUpdate();
                    c.commit();
                    return true;
                } else {
                    c.rollback();
                    return false; // Out of stock
                }
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public void updateOrderStatus(int orderId, String status) throws SQLException {
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement("UPDATE canteen_orders_tbl SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }
    // For Students: Fetch their active orders in the current group
    public List<FoodOrder> getActiveOrdersForUser(int userId, int groupId) throws SQLException {
        List<FoodOrder> list = new ArrayList<>();
        String sql = "SELECT o.*, COALESCE(o.order_batch_id, CONCAT('legacy-', o.id)) AS order_batch_id, f.name AS food_name, c.name AS canteen_name " +
                "FROM canteen_orders_tbl o " +
                "JOIN canteen_foods_tbl f ON o.food_id = f.id " +
                "JOIN canteens_tbl c ON f.canteen_id = c.id " +
                "WHERE o.user_id = ? AND c.group_id = ? AND o.status IN ('pending', 'collected') " +
                "ORDER BY o.order_time DESC";
        try (Connection conn = DB.conn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapOrder(rs, false));
        }
        return list;
    }

    // For Managers/Admins: Fetch active orders for a specific canteen (or all if canteenId is 0)
    public List<FoodOrder> getActiveOrdersForManager(int groupId, int canteenId) throws SQLException {
        List<FoodOrder> list = new ArrayList<>();
        String sql = "SELECT o.*, COALESCE(o.order_batch_id, CONCAT('legacy-', o.id)) AS order_batch_id, f.name AS food_name, c.name AS canteen_name, u.display_name AS user_name " +
                "FROM canteen_orders_tbl o " +
                "JOIN canteen_foods_tbl f ON o.food_id = f.id " +
                "JOIN canteens_tbl c ON f.canteen_id = c.id " +
                "JOIN users u ON o.user_id = u.id " +
                "WHERE c.group_id = ? " + (canteenId > 0 ? "AND c.id = ? " : "") +
                "AND o.status IN ('pending', 'collected') " +
                "ORDER BY o.order_time ASC";
        try (Connection conn = DB.conn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            if (canteenId > 0) ps.setInt(2, canteenId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapOrder(rs, true));
        }
        return list;
    }

    private FoodOrder mapOrder(ResultSet rs, boolean includeUser) throws SQLException {
        FoodOrder o = new FoodOrder();
        o.setId(rs.getInt("id"));
        o.setUserId(rs.getInt("user_id"));
        o.setFoodId(rs.getInt("food_id"));
        o.setOrderBatchId(rs.getString("order_batch_id"));
        o.setStatus(rs.getString("status"));
        o.setFoodName(rs.getString("food_name"));
        o.setCanteenName(rs.getString("canteen_name"));
        if (rs.getTimestamp("order_time") != null) o.setOrderTime(rs.getTimestamp("order_time").toLocalDateTime());
        if (includeUser) o.setUserName(rs.getString("user_name"));
        return o;
    }

    private void ensureOrderBatchColumn() {
        String checkSql = "SELECT 1 FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'canteen_orders_tbl' AND column_name = 'order_batch_id'";
        String alterSql = "ALTER TABLE canteen_orders_tbl ADD COLUMN order_batch_id VARCHAR(64) NULL AFTER food_id";

        try (Connection c = DB.conn();
             PreparedStatement checkPs = c.prepareStatement(checkSql);
             ResultSet rs = checkPs.executeQuery()) {
            if (!rs.next()) {
                try (PreparedStatement alterPs = c.prepareStatement(alterSql)) {
                    alterPs.executeUpdate();
                }
            }
        } catch (SQLException ignored) {
            // Keep startup resilient if the table does not exist yet.
        }
    }

    // ─── 5-Hour Auto-Cancel Logic ────────────────────────────────────────────

    public void cancelOldOrders() throws SQLException {
        // Find all pending orders older than 5 hours
        String findOldSql = "SELECT id, food_id FROM canteen_orders_tbl WHERE status = 'pending' AND order_time < DATE_SUB(NOW(), INTERVAL 5 HOUR)";
        String cancelSql = "UPDATE canteen_orders_tbl SET status = 'cancelled' WHERE id = ?";
        String restoreFoodSql = "UPDATE canteen_foods_tbl SET available_qty = available_qty + 1 WHERE id = ?";

        try (Connection c = DB.conn();
             PreparedStatement findPs = c.prepareStatement(findOldSql);
             PreparedStatement cancelPs = c.prepareStatement(cancelSql);
             PreparedStatement restorePs = c.prepareStatement(restoreFoodSql)) {

            ResultSet rs = findPs.executeQuery();
            while (rs.next()) {
                int orderId = rs.getInt("id");
                int foodId = rs.getInt("food_id");

                // Cancel the order
                cancelPs.setInt(1, orderId);
                cancelPs.executeUpdate();

                // Restore the stock
                restorePs.setInt(1, foodId);
                restorePs.executeUpdate();
            }
        }
    }
}
