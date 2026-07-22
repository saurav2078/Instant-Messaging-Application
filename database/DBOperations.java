package database;

import model.User;
import model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class DBOperations {

    // ── User Operations ──────────────────────────────────────────────────────

    public static User validateLogin(String username, String password) {
        String sql = "SELECT id, username, role, status FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                if ("banned".equalsIgnoreCase(status)) {
                    System.out.println("[DB] Login blocked: " + username + " is banned.");
                    return null;
                }
                return new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("role"),
                    status
                );
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] validateLogin: " + e.getMessage());
        }
        return null;
    }

    public static boolean registerUser(String username, String password) {
        if (usernameExists(username)) {
            System.out.println("[DB] Registration failed: '" + username + "' already taken.");
            return false;
        }
        String sql = "INSERT INTO users (username, password, role, status) VALUES (?, ?, 'user', 'active')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] registerUser: " + e.getMessage());
            return false;
        }
    }

    public static boolean usernameExists(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] usernameExists: " + e.getMessage());
        }
        return false;
    }

    public static boolean isUserBanned(String username) {
        String sql = "SELECT status FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return "banned".equalsIgnoreCase(rs.getString("status"));
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] isUserBanned: " + e.getMessage());
        }
        return false;
    }

    // ── Admin – User Management ──────────────────────────────────────────────

    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        // Exclude admin accounts — they are protected and don't belong in the
        // user management list. Sorting is done in Java.
        String sql = "SELECT id, username, role, status FROM users WHERE role != 'admin'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getAllUsers: " + e.getMessage());
        }
        // Sort alphabetically by username
        users.sort((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()));
        return users;
    }

    public static boolean banUser(String username) {
        String sql = "UPDATE users SET status = 'banned' WHERE username = ? AND role != 'admin'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] banUser: " + e.getMessage());
            return false;
        }
    }

    public static boolean unbanUser(String username) {
        String sql = "UPDATE users SET status = 'active' WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] unbanUser: " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username = ? AND role != 'admin'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] deleteUser: " + e.getMessage());
            return false;
        }
    }

    // ── Admin – Message Management ───────────────────────────────────────────

    public static List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>();
        // Simple query — no subquery alias needed.
        String sql = "SELECT id, sender, receiver, message, timestamp " +
                     "FROM messages ORDER BY timestamp DESC LIMIT 200";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                messages.add(new Message(
                    rs.getInt("id"),
                    rs.getString("sender"),
                    rs.getString("receiver"),
                    rs.getString("message"),
                    rs.getTimestamp("timestamp")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getAllMessages: " + e.getMessage());
        }
        return messages;
    }

    // ── Admin Stats ──────────────────────────────────────────────────────────

    public static int getUserCount() {
        String sql = "SELECT COUNT(*) FROM users WHERE role != 'admin'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getUserCount: " + e.getMessage());
        }
        return 0;
    }

    public static int getMessageCount() {
        String sql = "SELECT COUNT(*) FROM messages";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getMessageCount: " + e.getMessage());
        }
        return 0;
    }

    // ── Message Operations ───────────────────────────────────────────────────

    public static boolean saveMessage(String sender, String receiver, String message) {
        String sql = "INSERT INTO messages (sender, receiver, message, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, message);
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] saveMessage: " + e.getMessage());
            return false;
        }
    }

    public static List<Message> getPrivateHistory(String userA, String userB) {
        List<Message> messages = new ArrayList<>();
        // FIX: removed subquery alias FROM (...) AS recent which breaks old mysql.jar.
        // Fetch the last 50 messages descending, then reverse in Java for display order.
        String sql = "SELECT id, sender, receiver, message, timestamp FROM messages " +
                     "WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) " +
                     "ORDER BY timestamp DESC LIMIT 50";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userA); pstmt.setString(2, userB);
            pstmt.setString(3, userB); pstmt.setString(4, userA);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(new Message(
                    rs.getInt("id"), rs.getString("sender"), rs.getString("receiver"),
                    rs.getString("message"), rs.getTimestamp("timestamp")));
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getPrivateHistory: " + e.getMessage());
        }
        // Reverse so oldest message is first (ascending display order)
        java.util.Collections.reverse(messages);
        return messages;
    }

    public static List<Message> getChatHistory() {
        List<Message> messages = new ArrayList<>();
        // FIX: removed subquery alias FROM (...) AS recent which breaks old mysql.jar.
        String sql = "SELECT id, sender, receiver, message, timestamp FROM messages " +
                     "WHERE receiver = 'ALL' ORDER BY timestamp DESC LIMIT 50";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                messages.add(new Message(
                    rs.getInt("id"), rs.getString("sender"), rs.getString("receiver"),
                    rs.getString("message"), rs.getTimestamp("timestamp")));
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getChatHistory: " + e.getMessage());
        }
        // Reverse for ascending display order
        java.util.Collections.reverse(messages);
        return messages;
    }
}
