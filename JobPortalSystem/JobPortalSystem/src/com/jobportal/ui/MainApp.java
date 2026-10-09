package com.jobportal.ui;

import com.jobportal.db.DBInit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainApp {
    public static void main(String[] args) {
        UIStyle.installLookAndFeel();
        SwingUtilities.invokeLater(MainApp::start);
    }

    /** Shows a small splash, prepares / upgrades the database, then opens the login window. */
    private static void start() {
        JWindow splash = new JWindow();
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setBackground(UIStyle.SIDEBAR);
        p.setBorder(new EmptyBorder(30, 40, 30, 40));
        JLabel logo = new JLabel("JobPortal", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 30));
        logo.setForeground(Color.WHITE);
        JLabel msg = new JLabel("Connecting to database...", SwingConstants.CENTER);
        msg.setFont(UIStyle.F_BODY);
        msg.setForeground(new Color(148, 163, 184));
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        p.add(logo, BorderLayout.NORTH);
        p.add(msg, BorderLayout.CENTER);
        p.add(bar, BorderLayout.SOUTH);
        splash.setContentPane(p);
        splash.setSize(380, 180);
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                DBInit.run();
                return null;
            }

            @Override protected void done() {
                splash.dispose();
                try {
                    get();
                    new LoginFrame().setVisible(true);
                } catch (Exception e) {
                    Throwable c = e.getCause() != null ? e.getCause() : e;
                    c.printStackTrace();
                    int again = JOptionPane.showConfirmDialog(null,
                            "Could not connect to the database:\n" + c.getMessage() + "\n\nTry again?",
                            "Connection error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                    if (again == JOptionPane.YES_OPTION) start(); else System.exit(1);
                }
            }
        }.execute();
    }
}
