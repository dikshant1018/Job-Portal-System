package com.jobportal.db;

import com.jobportal.util.PasswordUtil;

import java.sql.*;

/**
 * Creates / upgrades the tables on startup, so the existing Railway database can be kept.
 * It is safe to run many times: it only adds what is missing.
 */
public final class DBInit {
    private DBInit() {}

    public static void run() throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            // --- base tables (same as the old schema, only created if missing) ---
            s.execute("CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, "
                    + "email VARCHAR(100) UNIQUE NOT NULL, password VARCHAR(255) NOT NULL, "
                    + "role ENUM('ADMIN','EMPLOYER','JOB_SEEKER') NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE IF NOT EXISTS jobs (id INT AUTO_INCREMENT PRIMARY KEY, employer_id INT NOT NULL, "
                    + "title VARCHAR(150) NOT NULL, company VARCHAR(100) NOT NULL, location VARCHAR(100) NOT NULL, "
                    + "description TEXT NOT NULL, salary VARCHAR(50), posted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (employer_id) REFERENCES users(id) ON DELETE CASCADE)");
            s.execute("CREATE TABLE IF NOT EXISTS applications (id INT AUTO_INCREMENT PRIMARY KEY, job_id INT NOT NULL, "
                    + "seeker_id INT NOT NULL, resume_note TEXT, status VARCHAR(20) DEFAULT 'PENDING', "
                    + "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (seeker_id) REFERENCES users(id) ON DELETE CASCADE)");

            // --- new tables ---
            s.execute("CREATE TABLE IF NOT EXISTS seeker_profiles (user_id INT PRIMARY KEY, headline VARCHAR(150), "
                    + "location VARCHAR(100), summary TEXT, skills TEXT, education TEXT, experience TEXT, "
                    + "linkedin VARCHAR(200), portfolio VARCHAR(200), expected_salary VARCHAR(60), photo MEDIUMBLOB, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE) DEFAULT CHARSET=utf8mb4");
            s.execute("CREATE TABLE IF NOT EXISTS employer_profiles (user_id INT PRIMARY KEY, company_name VARCHAR(100), "
                    + "industry VARCHAR(80), company_size VARCHAR(30), website VARCHAR(200), location VARCHAR(100), "
                    + "about TEXT, logo MEDIUMBLOB, FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE) "
                    + "DEFAULT CHARSET=utf8mb4");
            s.execute("CREATE TABLE IF NOT EXISTS documents (id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, "
                    + "doc_type VARCHAR(20) NOT NULL, file_name VARCHAR(200) NOT NULL, mime_type VARCHAR(100) NOT NULL, "
                    + "file_size INT NOT NULL, data LONGBLOB NOT NULL, uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE) DEFAULT CHARSET=utf8mb4");
            s.execute("CREATE TABLE IF NOT EXISTS saved_jobs (seeker_id INT NOT NULL, job_id INT NOT NULL, "
                    + "saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (seeker_id, job_id), "
                    + "FOREIGN KEY (seeker_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE)");

            // --- upgrade old tables ---
            addColumn(c, "users", "phone", "VARCHAR(20) NULL");
            addColumn(c, "users", "is_active", "TINYINT(1) NOT NULL DEFAULT 1");
            addColumn(c, "jobs", "job_type", "VARCHAR(20) NOT NULL DEFAULT 'FULL_TIME'");
            addColumn(c, "jobs", "category", "VARCHAR(60) NULL");
            addColumn(c, "jobs", "experience", "VARCHAR(30) NULL");
            addColumn(c, "jobs", "deadline", "DATE NULL");
            addColumn(c, "jobs", "status", "VARCHAR(10) NOT NULL DEFAULT 'OPEN'");
            addColumn(c, "applications", "document_id", "INT NULL");

            // applications.status used to be an ENUM without SHORTLISTED / WITHDRAWN
            if (columnType(c, "applications", "status").toLowerCase().startsWith("enum"))
                s.execute("ALTER TABLE applications MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING'");

            if (!indexExists(c, "applications", "uq_job_seeker")) {
                // remove old duplicate applications, then forbid new ones
                s.execute("DELETE a FROM applications a JOIN applications b "
                        + "ON a.job_id = b.job_id AND a.seeker_id = b.seeker_id AND a.id > b.id");
                s.execute("ALTER TABLE applications ADD UNIQUE KEY uq_job_seeker (job_id, seeker_id)");
            }
            if (!constraintExists(c, "applications", "fk_app_document"))
                s.execute("ALTER TABLE applications ADD CONSTRAINT fk_app_document "
                        + "FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL");

            hashPlainTextPasswords(c);
            ensureAdmin(c);
        }
    }

    /** Old rows stored passwords as plain text. Hash each of them once. */
    private static void hashPlainTextPasswords(Connection c) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, password FROM users WHERE password NOT LIKE 'pbkdf2$%'");
             PreparedStatement up = c.prepareStatement("UPDATE users SET password = ? WHERE id = ?")) {
            while (rs.next()) {
                up.setString(1, PasswordUtil.hash(rs.getString("password")));
                up.setInt(2, rs.getInt("id"));
                up.executeUpdate();
            }
        }
    }

    private static void ensureAdmin(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'")) {
            rs.next();
            if (rs.getInt(1) > 0) return;
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'ADMIN')")) {
            ps.setString(1, "System Admin");
            ps.setString(2, "admin@jobportal.com");
            ps.setString(3, PasswordUtil.hash("admin123"));
            ps.executeUpdate();
        }
    }

    // ---------- small helpers using information_schema ----------
    private static void addColumn(Connection c, String table, String column, String definition) throws SQLException {
        if (columnType(c, table, column) == null)
            try (Statement s = c.createStatement()) {
                s.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
    }

    private static String columnType(Connection c, String table, String column) throws SQLException {
        return query(c, "SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = ? AND COLUMN_NAME = ?", table, column);
    }

    private static boolean indexExists(Connection c, String table, String index) throws SQLException {
        return query(c, "SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = ? AND INDEX_NAME = ? LIMIT 1", table, index) != null;
    }

    private static boolean constraintExists(Connection c, String table, String name) throws SQLException {
        return query(c, "SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = ? AND CONSTRAINT_NAME = ? LIMIT 1", table, name) != null;
    }

    private static String query(Connection c, String sql, String a, String b) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a);
            ps.setString(2, b);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
