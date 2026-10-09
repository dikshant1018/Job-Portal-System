package com.jobportal.dao;

import com.jobportal.model.UserDocument;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DocumentDAO extends BaseDAO {
    public static final int MAX_DOCUMENTS = 10;
    private static final String META = "id, user_id, doc_type, file_name, mime_type, file_size, uploaded_at";

    public int add(UserDocument d) {
        try (Connection c = conn()) {
            try (PreparedStatement cnt = c.prepareStatement("SELECT COUNT(*) FROM documents WHERE user_id = ?")) {
                cnt.setInt(1, d.userId);
                try (ResultSet rs = cnt.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) >= MAX_DOCUMENTS)
                        throw new DaoException("You can store at most " + MAX_DOCUMENTS + " documents. Delete one first.", null);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO documents (user_id, doc_type, file_name, mime_type, file_size, data) VALUES (?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, d.userId, d.type, d.fileName, d.mimeType, d.data.length, d.data);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    return k.next() ? k.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    /** List without file contents (fast). */
    public List<UserDocument> list(int userId) {
        return query("SELECT " + META + " FROM documents WHERE user_id = ? ORDER BY uploaded_at DESC", userId);
    }

    /** Employer sees documents only of people who applied to one of his jobs. */
    public List<UserDocument> listForEmployer(int seekerId, int employerId) {
        return query("SELECT " + META + " FROM documents d WHERE d.user_id = ? AND "
                + String.format(APPLIED_TO_EMPLOYER, "d.user_id") + " ORDER BY d.uploaded_at DESC", seekerId, employerId);
    }

    public boolean hasResume(int userId) {
        return query("SELECT " + META + " FROM documents WHERE user_id = ? AND doc_type = 'RESUME' LIMIT 1", userId).size() > 0;
    }

    /** Full document including data; only for its owner. */
    public UserDocument get(int id, int ownerId) {
        List<UserDocument> l = query("SELECT " + META + ", data FROM documents WHERE id = ? AND user_id = ?", id, ownerId);
        return l.isEmpty() ? null : l.get(0);
    }

    public UserDocument getForEmployer(int id, int employerId) {
        List<UserDocument> l = query("SELECT " + META + ", data FROM documents d WHERE d.id = ? AND "
                + String.format(APPLIED_TO_EMPLOYER, "d.user_id"), id, employerId);
        return l.isEmpty() ? null : l.get(0);
    }

    public boolean delete(int id, int ownerId) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement("DELETE FROM documents WHERE id = ? AND user_id = ?")) {
            bind(ps, id, ownerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    private List<UserDocument> query(String sql, Object... params) {
        List<UserDocument> out = new ArrayList<>();
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                boolean withData = rs.getMetaData().getColumnCount() > 7;
                while (rs.next()) {
                    UserDocument d = new UserDocument();
                    d.id = rs.getInt("id");
                    d.userId = rs.getInt("user_id");
                    d.type = rs.getString("doc_type");
                    d.fileName = rs.getString("file_name");
                    d.mimeType = rs.getString("mime_type");
                    d.size = rs.getLong("file_size");
                    d.uploadedAt = rs.getTimestamp("uploaded_at");
                    if (withData) d.data = rs.getBytes("data");
                    out.add(d);
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return out;
    }
}
