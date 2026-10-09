package com.jobportal.ui;

import com.jobportal.dao.ApplicationDAO;
import com.jobportal.dao.DocumentDAO;
import com.jobportal.dao.ProfileDAO;
import com.jobportal.model.Application;
import com.jobportal.model.SeekerProfile;
import com.jobportal.model.User;
import com.jobportal.model.UserDocument;
import com.jobportal.util.Async;
import com.jobportal.util.FileUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Employer view of one applicant: full profile, cover letter, documents, decision buttons. */
public class ApplicantDialog extends JDialog {
    private final Application app;
    private final User employer;
    private final Runnable onChanged;
    private final ApplicationDAO appDAO = new ApplicationDAO();
    private final DocumentDAO docDAO = new DocumentDAO();
    private final UIStyle.Avatar avatar = new UIStyle.Avatar(72);
    private final JLabel lblName = new JLabel(), lblSub = UIStyle.muted(" "), lblStatus = new JLabel();
    private final JEditorPane details = new JEditorPane("text/html", "<html><body style='font-family:sans-serif'>Loading...</body></html>");
    private final DefaultTableModel docModel = UIStyle.model("Type", "File", "Size", "Note");
    private final JTable docTable = UIStyle.table(docModel);
    private List<UserDocument> docs = new ArrayList<>();

    public ApplicantDialog(Frame parent, Application app, User employer, Runnable onChanged) {
        super(parent, "Applicant - " + app.seekerName, true);
        this.app = app;
        this.employer = employer;
        this.onChanged = onChanged;

        lblName.setFont(UIStyle.F_TITLE);
        lblName.setText(app.seekerName);
        showStatus();
        JPanel names = new JPanel(new GridLayout(3, 1, 0, 2));
        names.setOpaque(false);
        names.add(lblName);
        names.add(lblSub);
        names.add(lblStatus);
        JPanel head = new JPanel(new BorderLayout(16, 0));
        head.setOpaque(false);
        head.add(avatar, BorderLayout.WEST);
        head.add(names, BorderLayout.CENTER);
        avatar.set(app.seekerName, null);

        details.setEditable(false);
        details.setOpaque(false);
        JScrollPane detailScroll = new JScrollPane(details);
        detailScroll.setBorder(new javax.swing.border.LineBorder(UIStyle.BORDER, 1, true));
        detailScroll.setPreferredSize(new Dimension(0, 300));

        UIStyle.widths(docTable, 100, 300, 80, 150);
        JScrollPane docScroll = UIStyle.tablePane(docTable);
        docScroll.setPreferredSize(new Dimension(0, 130));
        JButton open = UIStyle.outline("Open");
        JButton save = UIStyle.ghost("Save as...");
        open.addActionListener(e -> withDoc(d -> DocumentsPanel.open(this, d)));
        save.addActionListener(e -> withDoc(d -> DocumentsPanel.saveAs(this, d)));
        JPanel docBar = new JPanel(new BorderLayout());
        docBar.setOpaque(false);
        docBar.add(UIStyle.h2("Documents shared by the applicant"), BorderLayout.WEST);
        docBar.add(UIStyle.buttonRow(FlowLayout.RIGHT, open, save), BorderLayout.EAST);
        JPanel docsBox = new JPanel(new BorderLayout(0, 8));
        docsBox.setOpaque(false);
        docsBox.add(docBar, BorderLayout.NORTH);
        docsBox.add(docScroll, BorderLayout.CENTER);

        JButton shortlist = UIStyle.outline("Shortlist");
        JButton accept = UIStyle.success("Accept");
        JButton reject = UIStyle.danger("Reject");
        JButton close = UIStyle.ghost("Close");
        shortlist.addActionListener(e -> decide("SHORTLISTED"));
        accept.addActionListener(e -> decide("ACCEPTED"));
        reject.addActionListener(e -> decide("REJECTED"));
        close.addActionListener(e -> dispose());

        JPanel middle = new JPanel(new BorderLayout(0, 14));
        middle.setOpaque(false);
        middle.add(detailScroll, BorderLayout.CENTER);
        middle.add(docsBox, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(22, 26, 18, 26));
        root.add(head, BorderLayout.NORTH);
        root.add(middle, BorderLayout.CENTER);
        root.add(UIStyle.buttonRow(FlowLayout.RIGHT, close, shortlist, reject, accept), BorderLayout.SOUTH);
        setContentPane(root);
        setSize(780, 760);
        setLocationRelativeTo(parent);
        load();
    }

    private void showStatus() {
        lblStatus.setFont(UIStyle.F_BOLD);
        lblStatus.setText("Status: " + UIStyle.pretty(app.status) + "   |   Applied for " + app.jobTitle);
        lblStatus.setForeground(UIStyle.statusColor(app.status));
    }

    private void load() {
        Async.run(this, () -> new Object[]{new ProfileDAO().getSeekerForEmployer(app.seekerId, employer.id),
                docDAO.listForEmployer(app.seekerId, employer.id)}, r -> {
            SeekerProfile p = (SeekerProfile) r[0];
            @SuppressWarnings("unchecked") List<UserDocument> list = (List<UserDocument>) r[1];
            avatar.set(app.seekerName, p.photo);
            lblSub.setText(UIStyle.nz(p.headline).isBlank() ? "No headline" : p.headline);
            details.setText(html(p));
            details.setCaretPosition(0);
            docs = list;
            docModel.setRowCount(0);
            for (UserDocument d : list)
                docModel.addRow(new Object[]{UIStyle.pretty(d.type), d.fileName, FileUtil.humanSize(d.size),
                        app.documentId != null && app.documentId == d.id ? "Sent with application" : ""});
        });
    }

    private String html(SeekerProfile p) {
        StringBuilder h = new StringBuilder("<html><body style='font-family:sans-serif;font-size:11px;margin:8px'>");
        h.append("<b>Email:</b> ").append(esc(app.seekerEmail));
        h.append(" &nbsp;|&nbsp; <b>Phone:</b> ").append(esc(UIStyle.nz(app.seekerPhone).isBlank() ? "-" : app.seekerPhone));
        h.append("<br><b>Location:</b> ").append(esc(or(p.location)));
        h.append(" &nbsp;|&nbsp; <b>Expected salary:</b> ").append(esc(or(p.expectedSalary)));
        h.append("<br><b>LinkedIn:</b> ").append(esc(or(p.linkedin))).append("<br><b>Portfolio:</b> ").append(esc(or(p.portfolio)));
        section(h, "Cover letter", app.coverLetter);
        section(h, "About", p.summary);
        section(h, "Skills", p.skills);
        section(h, "Education", p.education);
        section(h, "Experience", p.experience);
        return h.append("</body></html>").toString();
    }

    private static void section(StringBuilder h, String title, String text) {
        h.append("<p style='margin-top:10px'><b style='color:#2563eb'>").append(title).append("</b><br>")
                .append(esc(UIStyle.nz(text).isBlank() ? "Not provided" : text)).append("</p>");
    }

    private static String or(String s) { return UIStyle.nz(s).isBlank() ? "-" : s; }

    private static String esc(String s) {
        return UIStyle.nz(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    private void withDoc(java.util.function.Consumer<UserDocument> action) {
        int i = UIStyle.selected(docTable);
        if (i < 0) { UIStyle.warn(this, "Please select a document first."); return; }
        int id = docs.get(i).id;
        Async.run(this, () -> docDAO.getForEmployer(id, employer.id), d -> {
            if (d == null) UIStyle.warn(this, "You do not have access to this document.");
            else action.accept(d);
        });
    }

    private void decide(String status) {
        Async.run(this, () -> appDAO.updateStatus(app.id, employer.id, status), ok -> {
            if (!ok) { UIStyle.warn(this, "The status could not be changed (the applicant may have withdrawn)."); return; }
            app.status = status;
            showStatus();
            onChanged.run();
        });
    }
}
