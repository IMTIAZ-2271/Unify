package com.Unify.dao;

import com.Unify.model.Bus;
import com.Unify.util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransportDAO {

    // Fetch buses ONLY for the selected group
    public List<Bus> getAllBuses(int groupId) {
        List<Bus> busList = new ArrayList<>();
        String query = "SELECT * FROM buses WHERE group_id = ? ORDER BY departure_time ASC";

        try (Connection conn = DB.conn(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, groupId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Bus bus = new Bus(
                            rs.getString("bus_number"),
                            rs.getInt("group_id"),
                            rs.getString("route_name"),
                            rs.getTime("departure_time"),
                            rs.getString("trip_type"),
                            rs.getString("message")
                    );
                    busList.add(bus);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return busList;
    }

    // Add a new bus with the group_id
    public boolean addBus(Bus bus) {
        String query = "INSERT INTO buses (bus_number, group_id, route_name, departure_time, trip_type, message) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DB.conn(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, bus.getBusNumber());
            pstmt.setInt(2, bus.getGroupId());
            pstmt.setString(3, bus.getRouteName());
            pstmt.setTime(4, bus.getDepartureTime());
            pstmt.setString(5, bus.getTripType());
            pstmt.setString(6, bus.getMessage());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Delete a bus from the database
    public boolean deleteBus(String busNumber) {
        String query = "DELETE FROM buses WHERE bus_number = ?";

        try (Connection conn = DB.conn();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, busNumber);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // Returns true if deletion was successful

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Update an existing bus in the database
    public boolean updateBus(Bus bus) {
        String query = "UPDATE buses SET route_name=?, departure_time=?, trip_type=?, message=? WHERE bus_number=?";

        try (Connection conn = DB.conn();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, bus.getRouteName());
            pstmt.setTime(2, bus.getDepartureTime());
            pstmt.setString(3, bus.getTripType());
            pstmt.setString(4, bus.getMessage());
            pstmt.setString(5, bus.getBusNumber()); // Used for the WHERE clause

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}