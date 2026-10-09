package com.jobportal.dao;

import com.jobportal.model.CompanyProfile;
import com.jobportal.model.SeekerProfile;

import java.sql.*;

public class ProfileDAO extends BaseDAO {

    // ---------------- job seeker ----------------
    public SeekerProfile getSeeker(int userId) {
        return readSeeker("SELECT * FROM seeker_profiles WHERE user_id = ?", userId, userId);
    }

    /** Employers may only read profiles of people who applied to their jobs. */
    public SeekerProfile getSeekerForEmployer(int seekerId, int employerId) {
        return readSeeker("SELECT * FROM seeker_profiles p WHERE p.user_id = ? AND "
                + String.format(APPLIED_TO_EMPLOYER, "p.user_id"), seekerId, seekerId, employerId);
    }

    private SeekerProfile readSeeker(String sql, int userId, Object... params) {
        SeekerProfile p = new SeekerProfile();
        p.userId = userId;
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p.headline = rs.getString("headline");
                    p.location = rs.getString("location");
                    p.summary = rs.getString("summary");
                    p.skills = rs.getString("skills");
                    p.education = rs.getString("education");
                    p.experience = rs.getString("experience");
                    p.linkedin = rs.getString("linkedin");
                    p.portfolio = rs.getString("portfolio");
                    p.expectedSalary = rs.getString("expected_salary");
                    p.photo = rs.getBytes("photo");
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return p;
    }

    public void saveSeeker(SeekerProfile p) {
        ensureRow("seeker_profiles", p.userId);
        run("UPDATE seeker_profiles SET headline=?, location=?, summary=?, skills=?, education=?, experience=?, "
                        + "linkedin=?, portfolio=?, expected_salary=? WHERE user_id=?",
                p.headline, p.location, p.summary, p.skills, p.education, p.experience, p.linkedin, p.portfolio, p.expectedSalary, p.userId);
    }

    public void savePhoto(int userId, byte[] jpeg) {
        ensureRow("seeker_profiles", userId);
        run("UPDATE seeker_profiles SET photo = ? WHERE user_id = ?", jpeg, userId);
    }

    // ---------------- employer / company ----------------
    public CompanyProfile getCompany(int userId) {
        CompanyProfile p = new CompanyProfile();
        p.userId = userId;
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM employer_profiles WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p.companyName = rs.getString("company_name");
                    p.industry = rs.getString("industry");
                    p.size = rs.getString("company_size");
                    p.website = rs.getString("website");
                    p.location = rs.getString("location");
                    p.about = rs.getString("about");
                    p.logo = rs.getBytes("logo");
                }
            }
        } catch (SQLException e) {
            throw fail(e);
        }
        return p;
    }

    public void saveCompany(CompanyProfile p) {
        ensureRow("employer_profiles", p.userId);
        run("UPDATE employer_profiles SET company_name=?, industry=?, company_size=?, website=?, location=?, about=? WHERE user_id=?",
                p.companyName, p.industry, p.size, p.website, p.location, p.about, p.userId);
    }

    public void saveLogo(int userId, byte[] jpeg) {
        ensureRow("employer_profiles", userId);
        run("UPDATE employer_profiles SET logo = ? WHERE user_id = ?", jpeg, userId);
    }

    // ---------------- helpers ----------------
    private void ensureRow(String table, int userId) {
        run("INSERT IGNORE INTO " + table + " (user_id) VALUES (?)", userId);
    }

    private void run(String sql, Object... params) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw fail(e);
        }
    }
}
