package com.jobportal.ui;

import com.jobportal.dao.AdminDAO;
import com.jobportal.dao.JobDAO;
import com.jobportal.dao.UserDAO;
import com.jobportal.model.JobPosting;
import com.jobportal.model.User;
import com.jobportal.util.Async;
import com.jobportal.util.Validator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminDashboard extends AppFrame {
    private final UserDAO userDAO = new UserDAO();
    private final JobDAO jobDAO = new JobDAO();
    private final AdminDAO adminDAO = new AdminDAO();

    private final UIStyle.Stat stSeekers = new UIStyle.Stat("Job seekers", UIStyle.PRIMARY);
    private final UIStyle.Stat stEmployers = new UIStyle.Stat("Employers", UIStyle.WARNING);
    private final UIStyle.Stat stJobs = new UIStyle.Stat("Open jobs", UIStyle.SUCCESS);
    private final UIStyle.Stat stApps = new UIStyle.Stat("Applications", UIStyle.MUTED);

    private final JTextField txtSearch = UIStyle.field();
    private final DefaultTableModel userModel = UIStyle.model("ID", "Name", "Email", "Role", "Status", "Registered");
    private final JTable userTable = UIStyle.table(userModel);
    private List<User> users = new ArrayList<>();

    private final DefaultTableModel jobModel = UIStyle.model("ID", "Title", "Company", "Location", "Status", "Applicants", "Posted");
    private final JTable jobTable = UIStyle.table(jobModel);
    private List<JobPosting> jobs = new ArrayList<>();

    public AdminDashboard(User admin) {
        super(admin, "Administrator");
        addPage("home", "Overview", buildHome(), this::loadHome);
        addPage("users", "Manage Users", buildUsers(), this::loadUsers);
        addPage("jobs", "Manage Jobs", buildJobs(), this::loadJobs);
        addPage("account", "Account Settings", new AccountPanel(admin, this::logout), null);
        start("home");
    }

    // =========================================================== overview
    private JComponent buildHome() {
        JPanel page = UIStyle.page("Administration", "A summary of everything happening on the portal.");
        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        for (UIStyle.Stat s : new UIStyle.Stat[]{stSeekers, stEmployers, stJobs, stApps}) stats.add(s);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(stats, BorderLayout.NORTH);
        page.add(wrap, BorderLayout.CENTER);
        return page;
    }

    private void loadHome() {
        Async.run(this, adminDAO::stats, (Map<String, Integer> s) -> {
            stSeekers.set(s.get("seekers"));
            stEmployers.set(s.get("employers"));
            stJobs.set(s.get("openJobs"));
            stApps.set(s.get("applications"));
        });
    }

    // =========================================================== users
    private JComponent buildUsers() {
        JPanel page = UIStyle.page("Manage Users", "Block accounts, reset passwords or remove users. Administrators are protected.");
        UIStyle.widths(userTable, 50, 190, 240, 110, 90, 110);
        txtSearch.setPreferredSize(new Dimension(280, 38));
        txtSearch.addActionListener(e -> loadUsers());
        JButton search = UIStyle.primary("Search");
        search.addActionListener(e -> loadUsers());
        JButton block = UIStyle.outline("Block / Unblock");
        JButton reset = UIStyle.ghost("Reset password");
        JButton delete = UIStyle.danger("Delete user");
        block.addActionListener(e -> toggleBlock());
        reset.addActionListener(e -> resetPassword());
        delete.addActionListener(e -> deleteUser());

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(UIStyle.buttonRow(FlowLayout.LEFT, txtSearch, search), BorderLayout.WEST);
        bar.add(UIStyle.buttonRow(FlowLayout.RIGHT, block, reset, delete), BorderLayout.EAST);
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(bar, BorderLayout.NORTH);
        center.add(UIStyle.tablePane(userTable), BorderLayout.CENTER);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private void loadUsers() {
        String q = txtSearch.getText();
        Async.run(this, () -> userDAO.search(q), list -> {
            users = list;
            userModel.setRowCount(0);
            for (User u : list)
                userModel.addRow(new Object[]{u.id, u.name, u.email, UIStyle.pretty(u.role), u.active ? "ACTIVE" : "BLOCKED", UIStyle.date(u.createdAt)});
        });
    }

    /** Selected normal (non-admin) user, or null after showing a message. */
    private User selectedUser() {
        int i = UIStyle.selected(userTable);
        if (i < 0) { UIStyle.warn(this, "Please select a user first."); return null; }
        User u = users.get(i);
        if (User.ADMIN.equals(u.role)) { UIStyle.warn(this, "Administrator accounts cannot be changed here."); return null; }
        return u;
    }

    private void toggleBlock() {
        User u = selectedUser();
        if (u == null) return;
        boolean makeActive = !u.active;
        if (!makeActive && !UIStyle.confirm(this, "Block " + u.name + "? They will not be able to sign in.")) return;
        Async.run(this, () -> userDAO.setActive(u.id, makeActive), ok -> loadUsers());
    }

    private void resetPassword() {
        User u = selectedUser();
        if (u == null) return;
        JPasswordField pw = UIStyle.password();
        UIStyle.Form f = new UIStyle.Form(1);
        f.add("New password for " + u.name, pw);
        f.setPreferredSize(new Dimension(340, 70));
        if (JOptionPane.showConfirmDialog(this, f, "Reset password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String np = new String(pw.getPassword());
        String problem = Validator.passwordProblem(np);
        if (problem != null) { UIStyle.warn(this, problem); return; }
        Async.exec(this, () -> userDAO.adminResetPassword(u.id, np), () -> UIStyle.info(this, "Password reset. Tell the user their new password."));
    }

    private void deleteUser() {
        User u = selectedUser();
        if (u == null) return;
        if (!UIStyle.confirm(this, "Permanently delete " + u.name + " (" + u.email + ")?\nTheir profile, documents, jobs and applications will be removed.")) return;
        Async.run(this, () -> userDAO.delete(u.id), ok -> loadUsers());
    }

    // =========================================================== jobs
    private JComponent buildJobs() {
        JPanel page = UIStyle.page("Manage Jobs", "Close or remove postings that break the rules.");
        UIStyle.widths(jobTable, 50, 230, 160, 120, 80, 90, 100);
        JButton toggle = UIStyle.outline("Close / Reopen");
        JButton delete = UIStyle.danger("Delete job");
        toggle.addActionListener(e -> {
            JobPosting j = selectedJob();
            if (j == null) return;
            String next = j.status.equals("OPEN") ? "CLOSED" : "OPEN";
            Async.run(this, () -> jobDAO.setStatus(j.id, 0, next), ok -> loadJobs());
        });
        delete.addActionListener(e -> {
            JobPosting j = selectedJob();
            if (j == null) return;
            if (!UIStyle.confirm(this, "Delete \"" + j.title + "\" and its " + j.applicants + " application(s)?")) return;
            Async.run(this, () -> jobDAO.delete(j.id, 0), ok -> loadJobs());
        });
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(UIStyle.tablePane(jobTable), BorderLayout.CENTER);
        center.add(UIStyle.buttonRow(FlowLayout.RIGHT, toggle, delete), BorderLayout.SOUTH);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private JobPosting selectedJob() {
        int i = UIStyle.selected(jobTable);
        if (i < 0) { UIStyle.warn(this, "Please select a job first."); return null; }
        return jobs.get(i);
    }

    private void loadJobs() {
        Async.run(this, jobDAO::all, list -> {
            jobs = list;
            jobModel.setRowCount(0);
            for (JobPosting j : list)
                jobModel.addRow(new Object[]{j.id, j.title, j.company, j.location, j.status, j.applicants, UIStyle.date(j.postedAt)});
        });
    }
}
