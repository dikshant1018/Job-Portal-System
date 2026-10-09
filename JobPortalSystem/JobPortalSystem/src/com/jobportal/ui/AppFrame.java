package com.jobportal.ui;

import com.jobportal.model.User;
import com.jobportal.util.Async;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Common window for all roles: dark sidebar with navigation + a page area. */
public abstract class AppFrame extends JFrame {
    protected final User user;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel nav = new JPanel();
    private final Map<String, Nav> navItems = new LinkedHashMap<>();
    private final Map<String, Runnable> onShow = new HashMap<>();
    private final UIStyle.Avatar avatar = new UIStyle.Avatar(42);
    private final JLabel nameLabel = new JLabel();

    protected AppFrame(User user, String roleLabel) {
        this.user = user;
        setTitle("JobPortal - " + roleLabel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(1050, 660));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(buildSidebar(roleLabel), BorderLayout.WEST);
        content.setBackground(UIStyle.BG);
        add(content, BorderLayout.CENTER);
    }

    private JPanel buildSidebar(String roleLabel) {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(UIStyle.SIDEBAR);
        side.setPreferredSize(new Dimension(250, 0));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(26, 24, 18, 24));
        JLabel logo = new JLabel("JobPortal");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        logo.setForeground(Color.WHITE);
        JLabel role = new JLabel(roleLabel.toUpperCase());
        role.setFont(new Font("Segoe UI", Font.BOLD, 11));
        role.setForeground(new Color(96, 165, 250));
        brand.add(logo);
        brand.add(Box.createVerticalStrut(2));
        brand.add(role);

        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(8, 14, 8, 14));

        nameLabel.setText(user.name);
        nameLabel.setFont(UIStyle.F_BOLD);
        nameLabel.setForeground(Color.WHITE);
        JLabel mail = new JLabel(user.email);
        mail.setFont(UIStyle.F_SMALL);
        mail.setForeground(new Color(148, 163, 184));
        JPanel who = new JPanel();
        who.setOpaque(false);
        who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));
        who.add(nameLabel);
        who.add(mail);
        JPanel me = new JPanel(new BorderLayout(12, 0));
        me.setOpaque(false);
        me.setBorder(new EmptyBorder(8, 10, 10, 10));
        me.add(avatar, BorderLayout.WEST);
        me.add(who, BorderLayout.CENTER);

        Nav logout = new Nav("Log out");
        logout.setForeground(new Color(252, 165, 165));
        logout.addActionListener(e -> logout());
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(new EmptyBorder(8, 14, 18, 14));
        bottom.add(me);
        bottom.add(logout);

        side.add(brand, BorderLayout.NORTH);
        side.add(nav, BorderLayout.CENTER);
        side.add(bottom, BorderLayout.SOUTH);
        avatar.set(user.name, null);
        return side;
    }

    /** @param refresh optional code that reloads the page data every time the page is opened */
    protected void addPage(String key, String label, JComponent page, Runnable refresh) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(UIStyle.BG);
        wrap.setBorder(new EmptyBorder(26, 30, 22, 30));
        wrap.add(page);
        content.add(wrap, key);
        Nav n = new Nav(label);
        n.addActionListener(e -> showPage(key));
        nav.add(n);
        nav.add(Box.createVerticalStrut(4));
        navItems.put(key, n);
        if (refresh != null) onShow.put(key, refresh);
    }

    protected void showPage(String key) {
        cards.show(content, key);
        navItems.forEach((k, n) -> { n.selected = k.equals(key); n.repaint(); });
        Runnable r = onShow.get(key);
        if (r != null) r.run();
    }

    /** Called by subclasses at the end of their constructor. */
    protected void start(String firstPage) {
        showPage(firstPage);
        Async.run(this, this::fetchPhoto, p -> avatar.set(user.name, p));
    }

    /** Profile photo / logo for the sidebar (runs in the background). */
    protected byte[] fetchPhoto() { return null; }

    /** Call after the user changed their name or photo. */
    public void refreshIdentity(byte[] photo) {
        nameLabel.setText(user.name);
        avatar.set(user.name, photo);
    }

    protected void logout() {
        new LoginFrame().setVisible(true);
        dispose();
    }

    private static class Nav extends JButton {
        boolean selected, hover;

        Nav(String text) {
            super(text);
            setFont(UIStyle.F_BOLD);
            setForeground(new Color(203, 213, 225));
            setHorizontalAlignment(LEFT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(new EmptyBorder(11, 18, 11, 16));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setAlignmentX(0f);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selected || hover) {
                g2.setColor(UIStyle.SIDEBAR_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            if (selected) {
                g2.setColor(UIStyle.PRIMARY);
                g2.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4);
            }
            g2.dispose();
            if (selected) setForeground(Color.WHITE);
            super.paintComponent(g);
        }
    }
}
