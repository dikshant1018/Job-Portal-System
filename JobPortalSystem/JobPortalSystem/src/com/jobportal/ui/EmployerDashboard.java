package com.jobportal.ui;

import com.jobportal.dao.ApplicationDAO;
import com.jobportal.dao.JobDAO;
import com.jobportal.dao.ProfileDAO;
import com.jobportal.model.Application;
import com.jobportal.model.CompanyProfile;
import com.jobportal.model.JobPosting;
import com.jobportal.model.User;
import com.jobportal.util.Async;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmployerDashboard extends AppFrame {
    private final JobDAO jobDAO = new JobDAO();
    private final ApplicationDAO appDAO = new ApplicationDAO();
    private final ProfileDAO profileDAO = new ProfileDAO();

    // home
    private final UIStyle.Stat stOpen = new UIStyle.Stat("Open jobs", UIStyle.PRIMARY);
    private final UIStyle.Stat stApplicants = new UIStyle.Stat("Total applicants", UIStyle.MUTED);
    private final UIStyle.Stat stPending = new UIStyle.Stat("Waiting for review", UIStyle.WARNING);
    private final UIStyle.Stat stShort = new UIStyle.Stat("Shortlisted", UIStyle.SUCCESS);
    private final UIStyle.Card setupCard = UIStyle.card(new BorderLayout(0, 12));

    // jobs
    private final DefaultTableModel jobModel = UIStyle.model("Title", "Type", "Location", "Status", "Applicants", "Posted", "Deadline");
    private final JTable jobTable = UIStyle.table(jobModel);
    private List<JobPosting> myJobs = new ArrayList<>();

    // applicants
    private final DefaultTableModel appModel = UIStyle.model("Applicant", "Job", "Applied", "Document", "Status");
    private final JTable appTable = UIStyle.table(appModel);
    private final JComboBox<String> cbJob = UIStyle.combo("All jobs"), cbStatus = UIStyle.combo("All statuses", "Pending", "Shortlisted", "Accepted", "Rejected");
    private List<Application> allApps = new ArrayList<>(), visibleApps = new ArrayList<>();
    private boolean filling;

    public EmployerDashboard(User user) {
        super(user, "Employer");
        CompanyProfilePanel company = new CompanyProfilePanel(user, this::refreshIdentity);
        addPage("home", "Dashboard", buildHome(), this::loadHome);
        addPage("company", "Company Profile", company, company::load);
        addPage("jobs", "My Jobs", buildJobs(), this::loadJobs);
        addPage("apps", "Applicants", buildApplicants(), this::loadApplicants);
        addPage("account", "Account Settings", new AccountPanel(user, this::logout), null);
        start("home");
    }

    @Override protected byte[] fetchPhoto() {
        return profileDAO.getCompany(user.id).logo;
    }

    // =========================================================== dashboard
    private JComponent buildHome() {
        JPanel page = UIStyle.page("Hello, " + user.name.trim().split("\\s+")[0], "Manage your jobs and review candidates.");
        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        for (UIStyle.Stat s : new UIStyle.Stat[]{stOpen, stApplicants, stPending, stShort}) stats.add(s);

        JButton go = UIStyle.primary("Set up company profile");
        go.addActionListener(e -> showPage("company"));
        setupCard.add(UIStyle.h2("Finish your company profile"), BorderLayout.NORTH);
        setupCard.add(UIStyle.muted("<html>Add your company name, logo and description so candidates trust your job posts.</html>"), BorderLayout.CENTER);
        setupCard.add(UIStyle.buttonRow(FlowLayout.LEFT, go), BorderLayout.SOUTH);
        setupCard.setVisible(false);

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);
        center.add(stats, BorderLayout.NORTH);
        center.add(setupCard, BorderLayout.CENTER);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(center, BorderLayout.NORTH);
        page.add(wrap, BorderLayout.CENTER);
        return page;
    }

    private void loadHome() {
        Async.run(this, () -> new Object[]{jobDAO.employerStats(user.id), profileDAO.getCompany(user.id)}, r -> {
            @SuppressWarnings("unchecked") Map<String, Integer> st = (Map<String, Integer>) r[0];
            CompanyProfile c = (CompanyProfile) r[1];
            stOpen.set(st.get("openJobs"));
            stApplicants.set(st.get("applicants"));
            stPending.set(st.get("pending"));
            stShort.set(st.get("shortlisted"));
            setupCard.setVisible(UIStyle.nz(c.companyName).isBlank() || c.logo == null);
        });
    }

    // =========================================================== jobs
    private JComponent buildJobs() {
        JPanel page = UIStyle.page("My Jobs", "Post new openings and manage the ones you already published.");
        UIStyle.widths(jobTable, 240, 100, 130, 80, 90, 100, 100);
        JButton post = UIStyle.primary("Post new job");
        JButton edit = UIStyle.outline("Edit");
        JButton toggle = UIStyle.ghost("Close / Reopen");
        JButton delete = UIStyle.danger("Delete");
        post.addActionListener(e -> openForm(null));
        edit.addActionListener(e -> { JobPosting j = selectedJob(); if (j != null) openForm(j); });
        toggle.addActionListener(e -> {
            JobPosting j = selectedJob();
            if (j == null) return;
            String next = j.status.equals("OPEN") ? "CLOSED" : "OPEN";
            Async.run(this, () -> jobDAO.setStatus(j.id, user.id, next), ok -> loadJobs());
        });
        delete.addActionListener(e -> {
            JobPosting j = selectedJob();
            if (j == null) return;
            if (!UIStyle.confirm(this, "Delete \"" + j.title + "\"?\nAll " + j.applicants + " application(s) for it will be deleted too.")) return;
            Async.run(this, () -> jobDAO.delete(j.id, user.id), ok -> loadJobs());
        });
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(UIStyle.buttonRow(FlowLayout.LEFT, post), BorderLayout.WEST);
        bar.add(UIStyle.buttonRow(FlowLayout.RIGHT, edit, toggle, delete), BorderLayout.EAST);
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(bar, BorderLayout.NORTH);
        center.add(UIStyle.tablePane(jobTable), BorderLayout.CENTER);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private JobPosting selectedJob() {
        int i = UIStyle.selected(jobTable);
        if (i < 0) { UIStyle.warn(this, "Please select a job first."); return null; }
        return myJobs.get(i);
    }

    private void openForm(JobPosting job) {
        Async.run(this, () -> profileDAO.getCompany(user.id).companyName, name -> {
            String def = UIStyle.nz(name).isBlank() ? user.name : name;
            new JobFormDialog(this, user, job, def, this::loadJobs).setVisible(true);
        });
    }

    private void loadJobs() {
        Async.run(this, () -> jobDAO.byEmployer(user.id), list -> {
            myJobs = list;
            jobModel.setRowCount(0);
            for (JobPosting j : list)
                jobModel.addRow(new Object[]{j.title, UIStyle.pretty(j.jobType), j.location, j.status, j.applicants,
                        UIStyle.date(j.postedAt), j.deadline == null ? "-" : UIStyle.date(j.deadline)});
        });
    }

    // =========================================================== applicants
    private JComponent buildApplicants() {
        JPanel page = UIStyle.page("Applicants", "Review candidates, open their resumes and make decisions.");
        UIStyle.widths(appTable, 190, 230, 110, 200, 100);
        cbJob.setPreferredSize(new Dimension(260, 36));
        cbStatus.setPreferredSize(new Dimension(160, 36));
        cbJob.addActionListener(e -> { if (!filling) applyFilter(); });
        cbStatus.addActionListener(e -> { if (!filling) applyFilter(); });
        JButton view = UIStyle.primary("View profile & resume");
        JButton shortlist = UIStyle.outline("Shortlist");
        JButton accept = UIStyle.success("Accept");
        JButton reject = UIStyle.danger("Reject");
        view.addActionListener(e -> { Application a = selectedApp(); if (a != null) showApplicant(a); });
        shortlist.addActionListener(e -> quick("SHORTLISTED"));
        accept.addActionListener(e -> quick("ACCEPTED"));
        reject.addActionListener(e -> quick("REJECTED"));
        appTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { Application a = selectedAppQuiet(); if (a != null) showApplicant(a); }
            }
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filters.setOpaque(false);
        filters.add(UIStyle.label("Job:"));
        filters.add(cbJob);
        filters.add(UIStyle.label("Status:"));
        filters.add(cbStatus);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(UIStyle.buttonRow(FlowLayout.LEFT, view), BorderLayout.WEST);
        bottom.add(UIStyle.buttonRow(FlowLayout.RIGHT, shortlist, reject, accept), BorderLayout.EAST);
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(filters, BorderLayout.NORTH);
        center.add(UIStyle.tablePane(appTable), BorderLayout.CENTER);
        center.add(bottom, BorderLayout.SOUTH);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private Application selectedAppQuiet() {
        int i = UIStyle.selected(appTable);
        return i < 0 || i >= visibleApps.size() ? null : visibleApps.get(i);
    }

    private Application selectedApp() {
        Application a = selectedAppQuiet();
        if (a == null) UIStyle.warn(this, "Please select an applicant first.");
        return a;
    }

    private void showApplicant(Application a) {
        new ApplicantDialog(this, a, user, this::loadApplicants).setVisible(true);
    }

    private void quick(String status) {
        Application a = selectedApp();
        if (a == null) return;
        Async.run(this, () -> appDAO.updateStatus(a.id, user.id, status), ok -> {
            if (!ok) UIStyle.warn(this, "The status could not be changed.");
            loadApplicants();
        });
    }

    private void loadApplicants() {
        Async.run(this, () -> appDAO.byEmployer(user.id), list -> {
            allApps = list;
            filling = true;
            Object keep = cbJob.getSelectedItem();
            cbJob.removeAllItems();
            cbJob.addItem("All jobs");
            list.stream().map(a -> a.jobTitle).distinct().forEach(cbJob::addItem);
            if (keep != null) cbJob.setSelectedItem(keep);
            filling = false;
            applyFilter();
        });
    }

    private void applyFilter() {
        String job = (String) cbJob.getSelectedItem();
        String status = cbStatus.getSelectedIndex() == 0 ? null : ((String) cbStatus.getSelectedItem()).toUpperCase();
        visibleApps = new ArrayList<>();
        appModel.setRowCount(0);
        for (Application a : allApps) {
            if (job != null && !job.equals("All jobs") && !job.equals(a.jobTitle)) continue;
            if (status != null && !status.equals(a.status)) continue;
            visibleApps.add(a);
            appModel.addRow(new Object[]{a.seekerName, a.jobTitle, UIStyle.date(a.appliedAt), UIStyle.nz(a.documentName), a.status});
        }
    }
}
