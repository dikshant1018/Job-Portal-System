package com.jobportal.ui;

import com.jobportal.dao.DocumentDAO;
import com.jobportal.model.User;
import com.jobportal.model.UserDocument;
import com.jobportal.util.Async;
import com.jobportal.util.FileUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Upload, open, download and delete resume / certificates / pictures. */
public class DocumentsPanel extends JPanel {
    private final User user;
    private final DocumentDAO dao = new DocumentDAO();
    private final DefaultTableModel model = UIStyle.model("Type", "File name", "Size", "Uploaded");
    private final JTable table = UIStyle.table(model);
    private final JComboBox<String> cbType = UIStyle.combo("Resume", "Certificate", "Portfolio", "Other");
    private List<UserDocument> docs = new ArrayList<>();

    public DocumentsPanel(User user) {
        this.user = user;
        setOpaque(false);
        setLayout(new BorderLayout());
        JPanel page = UIStyle.page("My Documents", "Upload your resume, certificates and pictures. Employers can open them when you apply.");

        UIStyle.Card upload = UIStyle.card(new BorderLayout(20, 0));
        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        text.add(UIStyle.h2("Upload a new document"), BorderLayout.NORTH);
        text.add(UIStyle.muted("<html>PDF, DOC, DOCX, TXT, PNG or JPG<br>Max 5 MB per file, up to "
                + DocumentDAO.MAX_DOCUMENTS + " files</html>"), BorderLayout.CENTER);
        JButton choose = UIStyle.primary("Choose file and upload");
        choose.addActionListener(e -> upload());
        cbType.setPreferredSize(new Dimension(150, 38));
        JPanel actions = UIStyle.buttonRow(FlowLayout.RIGHT, UIStyle.label("Document type:"), cbType, choose);
        upload.add(text, BorderLayout.CENTER);
        upload.add(actions, BorderLayout.EAST);

        JButton open = UIStyle.outline("Open");
        JButton save = UIStyle.ghost("Save as...");
        JButton delete = UIStyle.danger("Delete");
        open.addActionListener(e -> withSelected(d -> open(this, d)));
        save.addActionListener(e -> withSelected(d -> saveAs(this, d)));
        delete.addActionListener(e -> delete());
        UIStyle.widths(table, 110, 380, 90, 120);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(upload, BorderLayout.NORTH);
        center.add(UIStyle.tablePane(table), BorderLayout.CENTER);
        center.add(UIStyle.buttonRow(FlowLayout.RIGHT, open, save, delete), BorderLayout.SOUTH);
        page.add(center, BorderLayout.CENTER);
        add(page);
    }

    public void load() {
        Async.run(this, () -> dao.list(user.id), list -> {
            docs = list;
            model.setRowCount(0);
            for (UserDocument d : list)
                model.addRow(new Object[]{UIStyle.pretty(d.type), d.fileName, FileUtil.humanSize(d.size), UIStyle.date(d.uploadedAt)});
        });
    }

    private void upload() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose a document or picture");
        fc.setFileFilter(new FileNameExtensionFilter("Documents and pictures (PDF, DOC, DOCX, TXT, PNG, JPG)",
                "pdf", "doc", "docx", "txt", "png", "jpg", "jpeg"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        UserDocument d = new UserDocument();
        try {
            d.data = FileUtil.readChecked(f, FileUtil.DOC_EXT, FileUtil.MAX_DOC_BYTES);
        } catch (IllegalArgumentException ex) {
            UIStyle.warn(this, ex.getMessage());
            return;
        } catch (IOException ex) {
            UIStyle.error(this, "Could not read the file: " + ex.getMessage());
            return;
        }
        d.userId = user.id;
        d.type = UIStyle.raw((String) cbType.getSelectedItem());
        d.fileName = f.getName().length() > 200 ? f.getName().substring(0, 200) : f.getName();
        d.mimeType = FileUtil.mime(f.getName());
        Async.exec(this, () -> dao.add(d), () -> {
            UIStyle.info(this, "\"" + d.fileName + "\" was uploaded.");
            load();
        });
    }

    private void withSelected(java.util.function.Consumer<UserDocument> action) {
        int i = UIStyle.selected(table);
        if (i < 0) { UIStyle.warn(this, "Please select a document first."); return; }
        int id = docs.get(i).id;
        Async.run(this, () -> dao.get(id, user.id), d -> {
            if (d == null) UIStyle.warn(this, "This document no longer exists.");
            else action.accept(d);
        });
    }

    private void delete() {
        int i = UIStyle.selected(table);
        if (i < 0) { UIStyle.warn(this, "Please select a document first."); return; }
        UserDocument d = docs.get(i);
        if (!UIStyle.confirm(this, "Delete \"" + d.fileName + "\"?\nApplications that used it will lose the attachment.")) return;
        Async.exec(this, () -> dao.delete(d.id, user.id), this::load);
    }

    // ---- shared helpers (also used by the employer's applicant window) ----
    public static void open(Component parent, UserDocument d) {
        try {
            File tmp = File.createTempFile("jobportal_", "_" + d.fileName.replaceAll("[^A-Za-z0-9._-]", "_"));
            tmp.deleteOnExit();
            Files.write(tmp.toPath(), d.data);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) Desktop.getDesktop().open(tmp);
            else saveAs(parent, d);
        } catch (Exception e) {
            UIStyle.error(parent, "Could not open the file: " + e.getMessage());
        }
    }

    public static void saveAs(Component parent, UserDocument d) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save document");
        fc.setSelectedFile(new File(d.fileName));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File target = fc.getSelectedFile();
        if (target.exists() && !UIStyle.confirm(parent, target.getName() + " already exists. Replace it?")) return;
        try {
            Files.write(target.toPath(), d.data);
            UIStyle.info(parent, "Saved to " + target.getAbsolutePath());
        } catch (IOException e) {
            UIStyle.error(parent, "Could not save the file: " + e.getMessage());
        }
    }
}
