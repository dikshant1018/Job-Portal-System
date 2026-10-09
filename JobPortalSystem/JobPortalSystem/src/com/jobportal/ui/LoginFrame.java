package com.jobportal.ui;

import com.jobportal.dao.UserDAO;
import com.jobportal.model.User;
import com.jobportal.util.Async;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final UserDAO userDAO = new UserDAO();
    private final JTextField txtEmail = UIStyle.field();
    private final JPasswordField txtPass = UIStyle.password();
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnLogin = UIStyle.primary("Sign in");

    public LoginFrame() {
        setTitle("JobPortal - Sign in");
        setSize(920, 580);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));
        add(brandPanel());
        add(buildForm());
        getRootPane().setDefaultButton(btnLogin);
    }

    /** Left side with the brand colours, shared with the register window. */
    static JPanel brandPanel() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(15, 23, 42), getWidth(), getHeight(), new Color(30, 64, 175)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(60, 48, 40, 40));
        JLabel logo = new JLabel("JobPortal");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 38));
        logo.setForeground(Color.WHITE);
        JLabel tag = new JLabel("<html>Connect great talent<br>with great opportunities.</html>");
        tag.setFont(new Font("Segoe UI", Font.PLAIN, 19));
        tag.setForeground(new Color(191, 219, 254));
        p.add(logo);
        p.add(Box.createVerticalStrut(14));
        p.add(tag);
        p.add(Box.createVerticalStrut(44));
        for (String s : new String[]{"Build a complete profile with photo and resume",
                "Search and apply to jobs in a few clicks", "Employers review candidates in one place"}) {
            JLabel l = new JLabel("<html>&bull;&nbsp; " + s + "</html>");
            l.setFont(UIStyle.F_BODY);
            l.setForeground(new Color(203, 213, 225));
            l.setBorder(new EmptyBorder(0, 0, 14, 0));
            p.add(l);
        }
        p.add(Box.createVerticalGlue());
        return p;
    }

    static JButton link(String text) {
        JButton b = new JButton(text);
        b.setFont(UIStyle.F_BOLD);
        b.setForeground(UIStyle.PRIMARY);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel buildForm() {
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Color.WHITE);
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setPreferredSize(new Dimension(340, 400));

        JLabel title = new JLabel("Welcome back");
        title.setFont(UIStyle.F_TITLE);
        title.setForeground(UIStyle.TEXT);
        JLabel sub = UIStyle.muted("Sign in to continue to your account");

        UIStyle.Form form = new UIStyle.Form(1);
        form.add("Email", txtEmail);
        form.add("Password", txtPass);

        JCheckBox show = new JCheckBox("Show password");
        show.setOpaque(false);
        show.setFont(UIStyle.F_SMALL);
        char echo = txtPass.getEchoChar();
        show.addActionListener(e -> txtPass.setEchoChar(show.isSelected() ? (char) 0 : echo));

        lblError.setFont(UIStyle.F_SMALL);
        lblError.setForeground(UIStyle.DANGER);
        btnLogin.setAlignmentX(0f);
        btnLogin.addActionListener(e -> doLogin());

        JButton register = link("Create an account");
        register.addActionListener(e -> {
            new RegisterFrame().setVisible(true);
            dispose();
        });
        JPanel reg = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        reg.setOpaque(false);
        reg.add(UIStyle.muted("New to JobPortal?  "));
        reg.add(register);

        for (JComponent c : new JComponent[]{title, sub, form, show, lblError, btnLogin, reg}) c.setAlignmentX(0f);
        col.add(title);
        col.add(Box.createVerticalStrut(4));
        col.add(sub);
        col.add(Box.createVerticalStrut(26));
        col.add(form);
        col.add(show);
        col.add(Box.createVerticalStrut(6));
        col.add(lblError);
        col.add(Box.createVerticalStrut(8));
        col.add(btnLogin);
        col.add(Box.createVerticalStrut(18));
        col.add(reg);
        right.add(col);
        return right;
    }

    private void doLogin() {
        String email = txtEmail.getText().trim();
        String pass = new String(txtPass.getPassword());
        if (email.isEmpty() || pass.isEmpty()) {
            lblError.setText("Please enter your email and password.");
            return;
        }
        lblError.setText(" ");
        btnLogin.setEnabled(false);
        Async.run(this, () -> userDAO.authenticate(email, pass), user -> {
            btnLogin.setEnabled(true);
            if (user == null) {
                lblError.setText("Incorrect email or password.");
            } else if (!user.active) {
                lblError.setText("This account has been blocked. Contact the administrator.");
            } else {
                open(user, pass);
            }
        }, () -> btnLogin.setEnabled(true));
    }

    private void open(User user, String typedPassword) {
        JFrame next = switch (user.role) {
            case User.ADMIN -> new AdminDashboard(user);
            case User.EMPLOYER -> new EmployerDashboard(user);
            default -> new SeekerDashboard(user);
        };
        next.setVisible(true);
        dispose();
        if (user.role.equals(User.ADMIN) && typedPassword.equals("admin123"))
            UIStyle.warn(next, "You are using the default admin password.\nPlease change it in Account Settings.");
    }
}
