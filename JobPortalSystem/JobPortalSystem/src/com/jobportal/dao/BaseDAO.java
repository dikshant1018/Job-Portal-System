package com.jobportal.dao;

import com.jobportal.db.DBConnection;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class BaseDAO {
    /** SQL fragment: seeker (column given by %s) has an active application to a job of employer "?". */
    static final String APPLIED_TO_EMPLOYER =
            "EXISTS (SELECT 1 FROM applications ap JOIN jobs jb ON ap.job_id = jb.id "
                    + "WHERE ap.seeker_id = %s AND jb.employer_id = ? AND ap.status <> 'WITHDRAWN')";

    protected Connection conn() {
        try {
            return DBConnection.getConnection();
        } catch (SQLException e) {
            throw new DaoException("Cannot connect to the database. Please check your internet connection.", e);
        }
    }

    protected DaoException fail(SQLException e) {
        e.printStackTrace();
        return new DaoException("Database error: " + e.getMessage(), e);
    }

    static boolean isDuplicate(SQLException e) {
        return e.getErrorCode() == 1062;
    }

    protected static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
    }

    /** Runs a single-row query and returns {columnLabel -> int value}. */
    protected Map<String, Integer> counts(String sql, Object... params) {
        Map<String, Integer> out = new LinkedHashMap<>();
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                if (rs.next()) for (int i = 1; i <= md.getColumnCount(); i++) out.put(md.getColumnLabel(i), rs.getInt(i));
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return out;
    }
}
