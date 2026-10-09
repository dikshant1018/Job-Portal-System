package com.jobportal.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

/** Design system: colours, fonts and small reusable components used by every screen. */
public final class UIStyle {
    public static final Color PRIMARY = new Color(37, 99, 235);
    public static final Color SUCCESS = new Color(5, 150, 105);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Color WARNING = new Color(217, 119, 6);
    public static final Color SIDEBAR = new Color(15, 23, 42);
    public static final Color SIDEBAR_HOVER = new Color(30, 41, 59);
    public static final Color BG = new Color(241, 245, 249);
    public static final Color BORDER = new Color(226, 232, 240);
    public static final Color TEXT = new Color(15, 23, 42);
    public static final Color MUTED = new Color(100, 116, 139);
    public static final Color SELECTION = new Color(219, 234, 254);

    public static final Font F_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font F_H2 = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font F_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font F_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    public static final String[] JOB_TYPES = {"Full Time", "Part Time", "Contract", "Internship", "Remote"};

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private UIStyle() {}

    // ------------------------------------------------------------ look & feel
    /** Uses FlatLaf if its jar is on the classpath (optional), otherwise Nimbus. */
    public static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            UIManager.put("Component.arc", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
        } catch (Exception flatMissing) {
            try {
                for (UIManager.LookAndFeelInfo i : UIManager.getInstalledLookAndFeels())
                    if ("Nimbus".equals(i.getName())) UIManager.setLookAndFeel(i.getClassName());
            } catch (Exception ignored) { }
        }
        UIManager.put("OptionPane.messageFont", F_BODY);
        UIManager.put("OptionPane.buttonFont", F_BOLD);
        UIManager.put("ToolTip.font", F_SMALL);
    }

    // ------------------------------------------------------------ text helpers
    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_BODY);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_SMALL);
        l.setForeground(MUTED);
        return l;
    }

    public static JLabel h2(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_H2);
        l.setForeground(TEXT);
        return l;
    }

    public static String pretty(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String w : raw.toLowerCase().split("_")) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return sb.toString().trim();
    }

    public static String raw(String pretty) {
        return pretty.trim().toUpperCase().replace(' ', '_');
    }

    public static String date(Timestamp t) { return t == null ? "" : DATE.format(t.toLocalDateTime()); }

    public static String date(Date d) { return d == null ? "" : DATE.format(d.toLocalDate()); }

    public static String nz(String s) { return s == null ? "" : s; }

    public static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] p = name.trim().split("\\s+");
        String s = p[0].substring(0, 1) + (p.length > 1 ? p[p.length - 1].substring(0, 1) : "");
        return s.toUpperCase();
    }

    // ------------------------------------------------------------ dialogs
    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "JobPortal", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void warn(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Please check", JOptionPane.WARNING_MESSAGE);
    }

    public static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "Please confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // ------------------------------------------------------------ buttons
    public static class Btn extends JButton {
        private final Color base;
        private final boolean outline;
        private boolean hover;

        Btn(String text, Color base, boolean outline) {
            super(text);
            this.base = base;
            this.outline = outline;
            setFont(F_BOLD);
            setForeground(outline ? base : Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(new EmptyBorder(9, 18, 9, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = !isEnabled() ? new Color(203, 213, 225) : hover ? base.darker() : base;
            if (outline) {
                g2.setColor(hover && isEnabled() ? new Color(base.getRed(), base.getGreen(), base.getBlue(), 25) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(c);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                setForeground(c);
            } else {
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JButton primary(String t) { return new Btn(t, PRIMARY, false); }
    public static JButton success(String t) { return new Btn(t, SUCCESS, false); }
    public static JButton danger(String t) { return new Btn(t, DANGER, false); }
    public static JButton ghost(String t) { return new Btn(t, new Color(71, 85, 105), true); }
    public static JButton outline(String t) { return new Btn(t, PRIMARY, true); }

    // ------------------------------------------------------------ inputs
    private static void styleField(JTextComponent f) {
        f.setFont(F_BODY);
        f.setForeground(TEXT);
        f.setBackground(Color.WHITE);
        f.setCaretColor(PRIMARY);
        f.setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225), 1, true), new EmptyBorder(7, 10, 7, 10)));
    }

    /** Text field that shows a grey hint while it is empty. */
    public static class HintField extends JTextField {
        private String hint = "";

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !hint.isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(148, 163, 184));
                g2.setFont(getFont());
                Insets in = getInsets();
                g2.drawString(hint, in.left, (getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
                g2.dispose();
            }
        }
    }

    public static JTextField field() {
        HintField f = new HintField();
        styleField(f);
        return f;
    }

    /** Sets the grey hint text of a field created with field(). */
    public static JTextField hint(JTextField f, String text) {
        if (f instanceof HintField h) { h.hint = text; h.repaint(); }
        return f;
    }

    public static JTextField field(String value) {
        JTextField f = field();
        f.setText(nz(value));
        return f;
    }

    public static JPasswordField password() {
        JPasswordField f = new JPasswordField();
        styleField(f);
        return f;
    }

    public static JTextArea area(int rows) {
        JTextArea a = new JTextArea(rows, 20);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        styleField(a);
        return a;
    }

    public static JScrollPane areaPane(JTextArea a) {
        JScrollPane sp = new JScrollPane(a);
        sp.setBorder(BorderFactory.createEmptyBorder());
        return sp;
    }

    public static JComboBox<String> combo(String... items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(F_BODY);
        c.setBackground(Color.WHITE);
        return c;
    }

    // ------------------------------------------------------------ containers
    public static class Card extends JPanel {
        public Card(LayoutManager lm) {
            super(lm);
            setOpaque(false);
            setBorder(new EmptyBorder(18, 20, 18, 20));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static Card card(LayoutManager lm) { return new Card(lm); }

    /** A panel that always has the width of the scroll pane (so long forms only scroll vertically). */
    public static class ScrollPanel extends JPanel implements Scrollable {
        public ScrollPanel(LayoutManager lm) { super(lm); setOpaque(false); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 100; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    public static JScrollPane pageScroll(JComponent content) {
        ScrollPanel host = new ScrollPanel(new BorderLayout());
        host.setOpaque(true);
        host.setBackground(BG);
        host.add(content, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(host);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(BG);
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    /** Stacks cards vertically, each at its own height, with an even gap between them. */
    public static JPanel stack(JComponent... items) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        for (int i = 0; i < items.length; i++) {
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = i;
            gc.weightx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.anchor = GridBagConstraints.NORTH;
            gc.insets = new Insets(0, 0, i < items.length - 1 ? 16 : 0, 0);
            p.add(items[i], gc);
        }
        return p;
    }

    /** Page skeleton: title + subtitle on top, caller adds its content to CENTER. */
    public static JPanel page(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout(0, 18));
        p.setOpaque(false);
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(F_TITLE);
        t.setForeground(TEXT);
        t.setAlignmentX(0f);
        head.add(t);
        if (subtitle != null) {
            JLabel s = new JLabel(subtitle);
            s.setFont(F_BODY);
            s.setForeground(MUTED);
            s.setAlignmentX(0f);
            head.add(Box.createVerticalStrut(4));
            head.add(s);
        }
        p.add(head, BorderLayout.NORTH);
        return p;
    }

    public static JPanel buttonRow(int align, JComponent... buttons) {
        JPanel p = new JPanel(new FlowLayout(align, 10, 0));
        p.setOpaque(false);
        for (JComponent b : buttons) p.add(b);
        return p;
    }

    // ------------------------------------------------------------ form layout
    /** Labelled inputs laid out in a grid of 1..n columns. */
    public static class Form extends JPanel {
        private final int cols;
        private int col = 0, row = 0;

        public Form(int cols) {
            super(new GridBagLayout());
            this.cols = cols;
            setOpaque(false);
        }

        private JPanel cell(String label, JComponent c) {
            JPanel p = new JPanel(new BorderLayout(0, 5));
            p.setOpaque(false);
            JLabel l = new JLabel(label);
            l.setFont(F_BOLD);
            l.setForeground(new Color(51, 65, 85));
            p.add(l, BorderLayout.NORTH);
            p.add(c, BorderLayout.CENTER);
            return p;
        }

        private void place(JPanel cell, int span) {
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = col;
            gc.gridy = row;
            gc.gridwidth = span;
            gc.weightx = span;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.insets = new Insets(0, 0, 14, col + span >= cols ? 0 : 16);
            gc.anchor = GridBagConstraints.NORTH;
            add(cell, gc);
            col += span;
            if (col >= cols) { col = 0; row++; }
        }

        public void add(String label, JComponent c) { place(cell(label, c), 1); }

        public void addWide(String label, JComponent c) {
            if (col != 0) { col = 0; row++; }
            place(cell(label, c), cols);
        }
    }

    // ------------------------------------------------------------ tables
    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    public static Color statusColor(String status) {
        return switch (status == null ? "" : status.toUpperCase()) {
            case "ACCEPTED", "OPEN", "ACTIVE" -> SUCCESS;
            case "REJECTED", "BLOCKED" -> DANGER;
            case "PENDING" -> WARNING;
            case "SHORTLISTED" -> PRIMARY;
            default -> MUTED;
        };
    }

    public static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setFont(F_BODY);
        t.setForeground(TEXT);
        t.setRowHeight(38);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(SELECTION);
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setBackground(Color.WHITE);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 40));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, false, false, r, c);
                setBackground(new Color(248, 250, 252));
                setForeground(MUTED);
                setFont(F_BOLD);
                setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(0, 12, 0, 12)));
                setHorizontalAlignment(LEFT);
                return this;
            }
        });
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, false, r, c);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setFont(F_BODY);
                setForeground(TEXT);
                if (!sel) setBackground(r % 2 == 0 ? Color.WHITE : new Color(250, 251, 253));
                if ("Status".equals(tb.getColumnName(c))) {
                    setFont(F_BOLD);
                    setForeground(statusColor(String.valueOf(v)));
                }
                return this;
            }
        });
        return t;
    }

    /** Sets preferred column widths; a width of 0 keeps the default. */
    public static void widths(JTable t, int... w) {
        for (int i = 0; i < w.length && i < t.getColumnCount(); i++)
            if (w[i] > 0) t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
    }

    public static JScrollPane tablePane(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(BORDER, 1, true));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    /** Row of the underlying list for the selected table row, or -1. */
    public static int selected(JTable t) {
        int r = t.getSelectedRow();
        return r < 0 ? -1 : t.convertRowIndexToModel(r);
    }

    // ------------------------------------------------------------ avatar
    public static class Avatar extends JComponent {
        private final int size;
        private BufferedImage img;
        private String initials = "?";

        public Avatar(int size) {
            this.size = size;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        public void set(String name, byte[] data) {
            initials = initials(name);
            img = null;
            if (data != null) {
                try { img = ImageIO.read(new ByteArrayInputStream(data)); } catch (Exception ignored) { }
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, size, size));
            if (img != null) {
                double s = Math.max((double) size / img.getWidth(), (double) size / img.getHeight());
                int w = (int) (img.getWidth() * s), h = (int) (img.getHeight() * s);
                g2.drawImage(img, (size - w) / 2, (size - h) / 2, w, h, null);
            } else {
                g2.setColor(PRIMARY);
                g2.fillRect(0, 0, size, size);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, size / 3));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials, (size - fm.stringWidth(initials)) / 2, (size + fm.getAscent() - fm.getDescent()) / 2);
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------ stat card
    public static class Stat extends Card {
        private final JLabel value = new JLabel("–");

        public Stat(String label, Color accent) {
            super(new BorderLayout(0, 6));
            setBorder(new EmptyBorder(16, 18, 16, 18));
            value.setFont(new Font("Segoe UI", Font.BOLD, 30));
            value.setForeground(accent);
            add(muted(label), BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }

        public void set(Object v) { value.setText(String.valueOf(v)); }
    }
}
