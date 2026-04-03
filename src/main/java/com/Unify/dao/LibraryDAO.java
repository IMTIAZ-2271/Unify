package com.Unify.dao;

import com.Unify.model.Book;
import com.Unify.model.BookIssue;
import com.Unify.util.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibraryDAO {

    // ─── Add a new book (For Admins) ─────────────────────────────────────────
    public void addBook(int groupId, String title, String author, String description, int totalCopies) throws SQLException {
        String sql = "INSERT INTO books (group_id, title, author, description, total_copies, available_copies) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setString(2, title);
            ps.setString(3, author);
            ps.setString(4, description);
            ps.setInt(5, totalCopies);
            ps.setInt(6, totalCopies); // Initially, available copies = total copies
            ps.executeUpdate();
        }
    }

    // ─── Fetch all books for a specific group ────────────────────────────────
    public List<Book> getBooksByGroup(int groupId) throws SQLException {
        String sql = "SELECT * FROM books WHERE group_id = ? ORDER BY title ASC";
        List<Book> books = new ArrayList<>();

        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Book b = new Book();
                b.setId(rs.getInt("id"));
                b.setGroupId(rs.getInt("group_id"));
                b.setTitle(rs.getString("title"));
                b.setAuthor(rs.getString("author"));
                b.setDescription(rs.getString("description"));
                b.setTotalCopies(rs.getInt("total_copies"));
                b.setAvailableCopies(rs.getInt("available_copies"));

                Timestamp ca = rs.getTimestamp("created_at");
                if (ca != null) b.setCreatedAt(ca.toLocalDateTime());

                books.add(b);
            }
        }
        return books;
    }

    // ─── Request a book (Returns the new Issue ID) ───────────────────────────
    public int requestBook(int bookId, int userId) throws SQLException {
        String sql = "INSERT INTO book_issues (book_id, user_id, status) VALUES (?, ?, 'pending')";
        // Notice we added Statement.RETURN_GENERATED_KEYS
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, bookId);
            ps.setInt(2, userId);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1); // Return the ID so we can put it in the notification!
        }
        return -1;
    }


    // ─── Fetch Pending Requests (For Admins) ─────────────────────────────────
    public List<BookIssue> getPendingRequests(int groupId) throws SQLException {
        String sql = "SELECT bi.*, b.title, u.display_name " +
                "FROM book_issues bi " +
                "JOIN books b ON bi.book_id = b.id " +
                "JOIN users u ON bi.user_id = u.id " +
                "WHERE b.group_id = ? AND bi.status = 'pending'";

        List<BookIssue> requests = new ArrayList<>();
        try (Connection c = DB.conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                BookIssue issue = new BookIssue();
                issue.setId(rs.getInt("id"));
                issue.setBookId(rs.getInt("book_id"));
                issue.setUserId(rs.getInt("user_id"));
                issue.setStatus(rs.getString("status"));
                // The two new fields we just added!
                issue.setBookTitle(rs.getString("title"));
                issue.setRequesterName(rs.getString("display_name"));
                requests.add(issue);
            }
        }
        return requests;
    }

    // ─── Process Request from Notification Pane ──────────────────────────────
    public void processRequestFromNotification(int issueId, boolean approve) throws SQLException {
        Connection c = null;
        try {
            c = DB.conn();
            c.setAutoCommit(false);

            int bookId = -1;
            int studentId = -1;
            String bookTitle = "";

            // 1. Find the book and student info
            String getSql = "SELECT bi.book_id, bi.user_id, b.title FROM book_issues bi JOIN books b ON bi.book_id = b.id WHERE bi.id = ?";
            try (PreparedStatement ps = c.prepareStatement(getSql)) {
                ps.setInt(1, issueId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    bookId = rs.getInt("book_id");
                    studentId = rs.getInt("user_id");
                    bookTitle = rs.getString("title");
                }
            }

            // 2. Update the library tables ATOMICALLY
            if (bookId != -1) {
                if (approve) {
                    // CONCURRENCY FIX: Check if copies are available at the exact moment of update
                    try (PreparedStatement ps2 = c.prepareStatement("UPDATE books SET available_copies = available_copies - 1 WHERE id = ? AND available_copies > 0")) {
                        ps2.setInt(1, bookId);
                        int rowsAffected = ps2.executeUpdate();
                        if (rowsAffected == 0) {
                            // If 0 rows updated, it means available_copies was already 0!
                            throw new SQLException("OUT_OF_STOCK");
                        }
                    }
                    try (PreparedStatement ps1 = c.prepareStatement("UPDATE book_issues SET status = 'issued', issued_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                        ps1.setInt(1, issueId);
                        ps1.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = c.prepareStatement("UPDATE book_issues SET status = 'rejected' WHERE id = ?")) {
                        ps.setInt(1, issueId);
                        ps.executeUpdate();
                    }
                }
            }
            c.commit();

            // 3. Notify the student
            if (studentId != -1) {
                NotificationDAO ndao = new NotificationDAO();
                String msg = approve ? "Your request for '" + bookTitle + "' was approved! You can now collect it."
                        : "Sorry, your request for '" + bookTitle + "' was rejected.";
                ndao.create(new com.Unify.model.Notification(studentId, approve ? "✅ Book Approved" : "❌ Book Rejected", msg, "info"));
            }
        } catch (SQLException e) {
            if (c != null) c.rollback();
            throw e;
        } finally {
            if (c != null) c.setAutoCommit(true);
            if (c != null) c.close();
        }
    }
}