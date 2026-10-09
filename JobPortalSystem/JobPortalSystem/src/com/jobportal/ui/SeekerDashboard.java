package com.jobportal.ui;

import com.jobportal.dao.ApplicationDAO;
import com.jobportal.dao.DocumentDAO;
import com.jobportal.dao.JobDAO;
import com.jobportal.dao.ProfileDAO;
import com.jobportal.model.Application;
import com.jobportal.model.JobPosting;
import com.jobportal.model.SeekerProfile;
import com.jobportal.model.User;
import com.jobportal.model.UserDocument;
import com.jobportal.util.Async;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SeekerDashboard extends AppFrame {
    private final JobDAO jobDAO = new JobDAO();
    private final ApplicationDAO appDAO = new ApplicationDAO();
    private final DocumentDAO docDAO = new DocumentDAO();
    private final ProfileDAO profileDAO = new ProfileDAO();

    // home
    private final UIStyle.Stat stApps = new UIStyle.Stat("Applications sent", UIStyle.PRIMARY);
    private final UIStyle.Stat stShort = new UIStyle.Stat("Shortlisted", UIStyle.WARNING);
    private final UIStyle.Stat stAccepted = new UIStyle.Stat("Accepted", UIStyle.SUCCESS);
    private final UIStyle.Stat stSaved = new UIStyle.Stat("Saved jobs", UIStyle.MUTED);
    private final Bar strengthBar = new Bar();
    private final JLabel lblStrength = UIStyle.h2("Profile strength");
    private final JLabel lblTips = UIStyle.muted(" ");

    // jobs
    private final JTextField txtKeyword = UIStyle.field(), txtLocation = UIStyle.field();
    private final JComboBox<String> cbType = UIStyle.combo(typeItems());
    private final JCheckBox chkSaved = new JCheckBox("Saved jobs only");
    private final DefaultTableModel jobModel = UIStyle.model("", "Title", "Company", "Location", "Posted");
    private final JTable jobTable = UIStyle.table(jobModel);
    private List<JobPosting> shown = new ArrayList<>();
    private final JLabel dTitle = UIStyle.h2(" "), dMeta = UIStyle.muted(" ");
    private final JTextArea dBody = new JTextArea();
    private final JButton btnApply = UIStyle.primary("Apply now"), btnSave = UIStyle.outline("Save job");

    // applications
    private final DefaultTableModel appModel = UIStyle.model("Job", "Company", "Applied", "Document", "Status");
    private final JTable appTable = UIStyle.table(appModel);
    private List<Application> myApps = new ArrayList<>();

    public SeekerDashboard(User user) {
        super(user, "Job Seeker");
        SeekerProfilePanel profile = new SeekerProfilePanel(user, this::refreshIdentity);
        DocumentsPanel docs = new DocumentsPanel(user);
        addPage("home", "Dashboard", buildHome(), this::loadHome);
        addPage("jobs", "Find Jobs", buildJobs(), this::loadJobs);
        addPage("apps", "My Applications", buildApplications(), this::loadApplications);
        addPage("profile", "My Profile", profile, profile::load);
        addPage("docs", "My Documents", docs, docs::load);
        addPage("account", "Account Settings", new AccountPanel(user, this::logout), null);
        start("home");
    }

    private static String[] typeItems() {
        String[] a = new String[UIStyle.JOB_TYPES.length + 1];
        a[0] = "All types";
        System.arraycopy(UIStyle.JOB_TYPES, 0, a, 1, UIStyle.JOB_TYPES.length);
        return a;
    }

    @Override protected byte[] fetchPhoto() {
        return profileDAO.getSeeker(user.id).photo;
    }

    // =========================================================== dashboard
    private JComponent buildHome() {
        String first = user.name.trim().split("\\s+")[0];
        JPanel page = UIStyle.page("Welcome back, " + first, "Here is an overview of your job search.");
        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        for (UIStyle.Stat s : new UIStyle.Stat[]{stApps, stShort, stAccepted, stSaved}) stats.add(s);

        UIStyle.Card strength = UIStyle.card(new BorderLayout(0, 12));
        JButton go = UIStyle.primary("Complete my profile");
        go.addActionListener(e -> showPage("profile"));
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(lblStrength, BorderLayout.NORTH);
        top.add(strengthBar, BorderLayout.CENTER);
        strength.add(top, BorderLayout.NORTH);
        strength.add(lblTips, BorderLayout.CENTER);
        strength.add(UIStyle.buttonRow(FlowLayout.LEFT, go), BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);
        center.add(stats, BorderLayout.NORTH);
        center.add(strength, BorderLayout.CENTER);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(center, BorderLayout.NORTH);
        page.add(wrap, BorderLayout.CENTER);
        return page;
    }

    private void loadHome() {
        Async.run(this, () -> new Object[]{appDAO.seekerStats(user.id), profileDAO.getSeeker(user.id), docDAO.hasResume(user.id)}, r -> {
            @SuppressWarnings("unchecked") Map<String, Integer> st = (Map<String, Integer>) r[0];
            SeekerProfile p = (SeekerProfile) r[1];
            boolean resume = (Boolean) r[2];
            stApps.set(st.get("total"));
            stShort.set(st.get("shortlisted"));
            stAccepted.set(st.get("accepted"));
            stSaved.set(st.get("saved"));
            int pct = p.completeness(resume);
            strengthBar.set(pct);
            lblStrength.setText("Profile strength: " + pct + "%");
            List<String> todo = new ArrayList<>();
            if (!resume) todo.add("upload your resume");
            if (p.photo == null) todo.add("add a profile photo");
            if (UIStyle.nz(p.headline).isBlank()) todo.add("write a headline");
            if (UIStyle.nz(p.summary).isBlank()) todo.add("write a short summary");
            if (UIStyle.nz(p.skills).isBlank()) todo.add("list your skills");
            if (UIStyle.nz(p.education).isBlank()) todo.add("add your education");
            if (UIStyle.nz(p.experience).isBlank()) todo.add("add your experience");
            if (UIStyle.nz(p.location).isBlank()) todo.add("add your location");
            lblTips.setText(todo.isEmpty() ? "Great! Your profile is complete."
                    : "<html>To stand out, " + String.join(", ", todo) + ".</html>");
        });
    }

    // =========================================================== find jobs
    private JComponent buildJobs() {
        JPanel page = UIStyle.page("Find Jobs", "Search open positions and apply in a few clicks.");

        txtLocation.setPreferredSize(new Dimension(130, 38));
        cbType.setPreferredSize(new Dimension(130, 38));
        chkSaved.setOpaque(false);
        chkSaved.setFont(UIStyle.F_BODY);
        JButton search = UIStyle.primary("Search");
        JButton clear = UIStyle.ghost("Clear");
        search.addActionListener(e -> loadJobs());
        clear.addActionListener(e -> {
            txtKeyword.setText("");
            txtLocation.setText("");
            cbType.setSelectedIndex(0);
            chkSaved.setSelected(false);
            loadJobs();
        });
        txtKeyword.addActionListener(e -> loadJobs());
        txtLocation.addActionListener(e -> loadJobs());
        UIStyle.Card bar = UIStyle.card(new BorderLayout(10, 0));
        bar.setBorder(new EmptyBorder(14, 16, 14, 16));
        UIStyle.hint(txtKeyword, "Job title, company or keyword");
        UIStyle.hint(txtLocation, "Location");
        bar.add(txtKeyword, BorderLayout.CENTER);
        bar.add(UIStyle.buttonRow(FlowLayout.RIGHT, txtLocation, cbType, chkSaved, search, clear), BorderLayout.EAST);

        UIStyle.widths(jobTable, 28, 190, 130, 100, 110);
        jobTable.getColumnModel().getColumn(0).setMaxWidth(34);
        jobTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showDetail(); });

        // detail card
        dBody.setEditable(false);
        dBody.setLineWrap(true);
        dBody.setWrapStyleWord(true);
        dBody.setFont(UIStyle.F_BODY);
        dBody.setOpaque(false);
        dBody.setBorder(new EmptyBorder(6, 0, 6, 0));
        JScrollPane bodyScroll = new JScrollPane(dBody);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.setOpaque(false);
        bodyScroll.getViewport().setOpaque(false);
        btnApply.addActionListener(e -> startApply());
        btnSave.addActionListener(e -> toggleSave());
        JPanel head = new JPanel(new GridLayout(2, 1, 0, 3));
        head.setOpaque(false);
        head.add(dTitle);
        head.add(dMeta);
        UIStyle.Card detail = UIStyle.card(new BorderLayout(0, 10));
        detail.add(head, BorderLayout.NORTH);
        detail.add(bodyScroll, BorderLayout.CENTER);
        detail.add(UIStyle.buttonRow(FlowLayout.RIGHT, btnSave, btnApply), BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, UIStyle.tablePane(jobTable), detail);
        split.setResizeWeight(0.58);
        split.setDividerSize(14);
        split.setBorder(null);
        split.setOpaque(false);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(bar, BorderLayout.NORTH);
        center.add(split, BorderLayout.CENTER);
        page.add(center, BorderLayout.CENTER);
        showDetail();
        return page;
    }

    private void loadJobs() {
        String type = cbType.getSelectedIndex() == 0 ? null : UIStyle.raw((String) cbType.getSelectedItem());
        String kw = txtKeyword.getText(), loc = txtLocation.getText();
        boolean saved = chkSaved.isSelected();
        Async.run(this, () -> jobDAO.search(user.id, kw, loc, type, saved), list -> {
            shown = list;
            jobModel.setRowCount(0);
            for (JobPosting j : list)
                jobModel.addRow(new Object[]{j.saved ? "★" : "", j.title, j.company, j.location, UIStyle.date(j.postedAt)});
            if (!list.isEmpty()) jobTable.setRowSelectionInterval(0, 0);
            else showDetail();
        });
    }

    private JobPosting selectedJob() {
        int i = UIStyle.selected(jobTable);
        return i < 0 || i >= shown.size() ? null : shown.get(i);
    }

    private void showDetail() {
        JobPosting j = selectedJob();
        btnApply.setEnabled(j != null);
        btnSave.setEnabled(j != null);
        if (j == null) {
            dTitle.setText("No job selected");
            dMeta.setText("Pick a job from the list to see the details.");
            dBody.setText("");
            return;
        }
        dTitle.setText(j.title);
        dMeta.setText(j.company + "  |  " + j.location);
        btnSave.setText(j.saved ? "Remove from saved" : "Save job");
        dBody.setText("Job type:  " + UIStyle.pretty(j.jobType)
                + "\nExperience:  " + (UIStyle.nz(j.experience).isBlank() ? "Not specified" : j.experience)
                + "\nCategory:  " + (UIStyle.nz(j.category).isBlank() ? "Not specified" : j.category)
                + "\nSalary:  " + (UIStyle.nz(j.salary).isBlank() ? "Not disclosed" : j.salary)
                + "\nApply before:  " + (j.deadline == null ? "Open until filled" : UIStyle.date(j.deadline))
                + "\nApplicants so far:  " + j.applicants
                + "\n\n" + j.description);
        dBody.setCaretPosition(0);
    }

    private void toggleSave() {
        JobPosting j = selectedJob();
        if (j == null) return;
        Async.run(this, () -> jobDAO.toggleSaved(user.id, j.id), nowSaved -> {
            j.saved = nowSaved;
            if (chkSaved.isSelected() && !nowSaved) { loadJobs(); return; }
            int row = jobTable.convertRowIndexToModel(jobTable.getSelectedRow());
            jobModel.setValueAt(nowSaved ? "★" : "", row, 0);
            showDetail();
        });
    }

    private void startApply() {
        JobPosting j = selectedJob();
        if (j == null) return;
        Async.run(this, () -> docDAO.list(user.id), docs -> {
            if (docs.isEmpty()) {
                if (UIStyle.confirm(this, "You need to upload a resume before you can apply.\nOpen My Documents now?")) showPage("docs");
                return;
            }
            docs.sort((a, b) -> Boolean.compare(!"RESUME".equals(a.type), !"RESUME".equals(b.type)));
            JComboBox<String> cb = UIStyle.combo();
            for (UserDocument d : docs) cb.addItem(UIStyle.pretty(d.type) + "  -  " + d.fileName);
            JTextArea cover = UIStyle.area(6);
            UIStyle.Form f = new UIStyle.Form(1);
            f.add("Applying for", UIStyle.label(j.title + " at " + j.company));
            f.add("Attach your resume / document", cb);
            f.add("Cover letter (optional)", UIStyle.areaPane(cover));
            f.setPreferredSize(new Dimension(460, 270));
            if (JOptionPane.showConfirmDialog(this, f, "Apply for this job", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            if (cover.getText().length() > 3000) { UIStyle.warn(this, "Cover letter is too long (max 3000 characters)."); return; }
            Integer docId = docs.get(cb.getSelectedIndex()).id;
            String letter = cover.getText().trim();
            Async.run(this, () -> appDAO.apply(j.id, user.id, docId, letter), err -> {
                if (err != null) UIStyle.warn(this, err);
                else UIStyle.info(this, "Your application was sent to " + j.company + ".");
            });
        });
    }

    // =========================================================== applications
    private JComponent buildApplications() {
        JPanel page = UIStyle.page("My Applications", "Track the status of every job you applied for.");
        UIStyle.widths(appTable, 220, 160, 110, 220, 100);
        JButton view = UIStyle.outline("View cover letter");
        JButton withdraw = UIStyle.danger("Withdraw application");
        view.addActionListener(e -> {
            Application a = selectedApp();
            if (a == null) return;
            JTextArea t = UIStyle.area(10);
            t.setText(UIStyle.nz(a.coverLetter).isBlank() ? "(no cover letter)" : a.coverLetter);
            t.setEditable(false);
            JScrollPane sp = UIStyle.areaPane(t);
            sp.setPreferredSize(new Dimension(440, 220));
            JOptionPane.showMessageDialog(this, sp, "Cover letter - " + a.jobTitle, JOptionPane.PLAIN_MESSAGE);
        });
        withdraw.addActionListener(e -> {
            Application a = selectedApp();
            if (a == null) return;
            if (!a.status.equals("PENDING") && !a.status.equals("SHORTLISTED")) {
                UIStyle.warn(this, "Only pending or shortlisted applications can be withdrawn.");
                return;
            }
            if (!UIStyle.confirm(this, "Withdraw your application for \"" + a.jobTitle + "\"?")) return;
            Async.run(this, () -> appDAO.withdraw(a.id, user.id), ok -> loadApplications());
        });
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(UIStyle.tablePane(appTable), BorderLayout.CENTER);
        center.add(UIStyle.buttonRow(FlowLayout.RIGHT, view, withdraw), BorderLayout.SOUTH);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private Application selectedApp() {
        int i = UIStyle.selected(appTable);
        if (i < 0) { UIStyle.warn(this, "Please select an application first."); return null; }
        return myApps.get(i);
    }

    private void loadApplications() {
        Async.run(this, () -> appDAO.bySeeker(user.id), list -> {
            myApps = list;
            appModel.setRowCount(0);
            for (Application a : list)
                appModel.addRow(new Object[]{a.jobTitle, a.company, UIStyle.date(a.appliedAt), UIStyle.nz(a.documentName), a.status});
        });
    }

    // =========================================================== small progress bar
    private static class Bar extends JComponent {
        private int value;

        Bar() { setPreferredSize(new Dimension(100, 12)); }

        void set(int v) { value = v; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UIStyle.BORDER);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.setColor(value >= 80 ? UIStyle.SUCCESS : UIStyle.PRIMARY);
            g2.fillRoundRect(0, 0, Math.max(getHeight(), getWidth() * value / 100), getHeight(), getHeight(), getHeight());
            g2.dispose();
        }
    }
}
