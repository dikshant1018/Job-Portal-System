package com.jobportal.dao;

import com.jobportal.model.User;
import com.jobportal.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserDAO extends BaseDAO {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MS = 60_000;
    /** email -> {failed attempts, locked-until millis} */
    private static final Map<String, long[]> ATTEMPTS = new ConcurrentHashMap<>();

    private static User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.id = rs.getInt("id");
        u.name = rs.getString("name");
        u.email = rs.getString("email");
        u.phone = rs.getString("phone");
        u.role = rs.getString("role");
        u.active = rs.getBoolean("is_active");
        u.createdAt = rs.getTimestamp("created_at");
        return u;
    }

    /** @return false when the email is already registered. */
    public boolean register(User u, String password) {
        String sql = "INSERT INTO users (name, email, phone, password, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, u.name.trim(), u.email.trim().toLowerCase(), u.phone == null || u.phone.isBlank() ? null : u.phone.trim(),
                    PasswordUtil.hash(password), u.role);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (isDuplicate(e)) return false;
            throw fail(e);
        }
    }

    /** @return the user, or null when email/password are wrong. Locks the email for a minute after 5 failures. */
    public User authenticate(String email, String password) {
        String key = email.trim().toLowerCase();
        long[] a = ATTEMPTS.get(key);
        if (a != null && a[1] > System.currentTimeMillis())
            throw new DaoException("Too many failed attempts. Try again in " + (a[1] - System.currentTimeMillis()) / 1000 + " seconds.", null);
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE email = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordUtil.verify(password, rs.getString("password"))) {
                    ATTEMPTS.remove(key);
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        long[] f = ATTEMPTS.computeIfAbsent(key, k -> new long[2]);
        if (++f[0] >= MAX_ATTEMPTS) { f[0] = 0; f[1] = System.currentTimeMillis() + LOCK_MS; }
        return null;
    }

    public void updateBasic(int id, String name, String phone) {
        update("UPDATE users SET name = ?, phone = ? WHERE id = ?", name.trim(), phone == null || phone.isBlank() ? null : phone.trim(), id);
    }

    /** @return error message, or null on success. */
    public String changeEmail(int id, String newEmail, String currentPassword) {
        if (!checkPassword(id, currentPassword)) return "Current password is incorrect.";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement("UPDATE users SET email = ? WHERE id = ?")) {
            bind(ps, newEmail.trim().toLowerCase(), id);
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            if (isDuplicate(e)) return "That email is already used by another account.";
            throw fail(e);
        }
    }

    /** @return error message, or null on success. */
    public String changePassword(int id, String oldPw, String newPw) {
        if (!checkPassword(id, oldPw)) return "Current password is incorrect.";
        update("UPDATE users SET password = ? WHERE id = ?", PasswordUtil.hash(newPw), id);
        return null;
    }

    public void adminResetPassword(int id, String newPw) {
        update("UPDATE users SET password = ? WHERE id = ? AND role <> 'ADMIN'", PasswordUtil.hash(newPw), id);
    }

    public boolean checkPassword(int id, String password) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement("SELECT password FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && PasswordUtil.verify(password, rs.getString(1));
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    public List<User> search(String text) {
        List<User> list = new ArrayList<>();
        String like = "%" + (text == null ? "" : text.trim().replace("%", "\\%").replace("_", "\\_")) + "%";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM users WHERE name LIKE ? OR email LIKE ? ORDER BY id DESC")) {
            bind(ps, like, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return list;
    }

    /** Admin accounts can never be blocked or deleted from the UI. */
    public boolean setActive(int id, boolean active) {
        return update("UPDATE users SET is_active = ? WHERE id = ? AND role <> 'ADMIN'", active, id) > 0;
    }

    public boolean delete(int id) {
        return update("DELETE FROM users WHERE id = ? AND role <> 'ADMIN'", id) > 0;
    }

    /** User deletes their own account after confirming the password. */
    public boolean deleteOwnAccount(int id, String password) {
        return checkPassword(id, password) && delete(id);
    }

    private int update(String sql, Object... params) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }
}
