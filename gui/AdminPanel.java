package gui;

import database.DBOperations;
import model.Message;
import model.User;
import util.AppConstants;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;


public class AdminPanel extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color C_HEADER_BG  = UITheme.BG_SURFACE;
    private static final Color C_NAV_BG     = UITheme.BG_ELEVATED;
    private static final Color C_BODY_BG    = UITheme.BG_BASE;
    private static final Color C_CARD_BG    = Color.WHITE;
    private static final Color C_BORDER     = new Color(220, 225, 232);
    private static final Color C_TEXT_PRI   = new Color(30,  40,  55);
    private static final Color C_TEXT_SEC   = new Color(100, 115, 135);
    private static final Color C_TEXT_HINT  = new Color(160, 170, 185);
    private static final Color C_BLUE       = new Color(37,  99,  235);
    private static final Color C_BLUE_BG    = new Color(219, 234, 254);
    private static final Color C_GREEN      = new Color(22,  163,  74);
    private static final Color C_GREEN_BG   = new Color(209, 250, 229);
    private static final Color C_AMBER      = new Color(180,  83,   9);
    private static final Color C_AMBER_BG   = new Color(254, 243, 199);
    private static final Color C_RED        = new Color(185,  28,  28);
    private static final Color C_RED_BG     = new Color(254, 226, 226);
    private static final Color C_PURPLE_BG  = new Color(243, 232, 255);
    private static final Color C_ROW_ALT    = new Color(250, 251, 253);
    private static final Color C_ROW_BANNED = new Color(255, 242, 242);

    // ── State ─────────────────────────────────────────────────────────────────
    private final User adminUser;

    // ── Dashboard components ──────────────────────────────────────────────────
    private JLabel statUsers, statMessages, statBanned;
    private JTextArea activityLog;

    // ── Users tab ─────────────────────────────────────────────────────────────
    private DefaultTableModel userTableModel;
    private JTable  userTable;
    private JButton banButton, unbanButton, deleteButton, kickButton;

    // ── Messages tab ──────────────────────────────────────────────────────────
    private DefaultTableModel messageTableModel;
    private JTable messageTable;

    // ── Status bar ────────────────────────────────────────────────────────────
    private JLabel statusBar;

    // ── Constructor ───────────────────────────────────────────────────────────
    public AdminPanel(User adminUser) {
        this.adminUser = adminUser;
        initComponents();
        configureWindow();
        loadAllData();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(buildHeader(),      BorderLayout.NORTH);
        add(buildTabbedArea(),  BorderLayout.CENTER);
        add(buildStatusBar(),   BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(C_HEADER_BG);
        h.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        // Left: title block
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        JLabel title = new JLabel("ChatApp Admin Console");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(new Color(226, 232, 240));
        JLabel sub = new JLabel("BSc CSIT Advanced Java");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        sub.setForeground(new Color(100, 116, 139));
        titleBlock.add(title); titleBlock.add(sub);
        left.add(titleBlock);

        // Right: user pill + logout
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JLabel pill = new JLabel("  " + adminUser.getUsername() + "  [ADMIN]  ");
        pill.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pill.setForeground(new Color(148, 163, 184));
        pill.setOpaque(true);
        pill.setBackground(new Color(255, 255, 255, 18));
        pill.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 40)),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        JButton logout = styledButton("Logout", C_RED, C_RED_BG);
        logout.addActionListener(e -> confirmLogout());
        right.add(pill); right.add(logout);

        h.add(left,  BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    // ── Tabbed content ────────────────────────────────────────────────────────
    private JPanel buildTabbedArea() {
        UIManager.put("TabbedPane.tabAreaBackground",  C_NAV_BG);
        UIManager.put("TabbedPane.background",         C_NAV_BG);
        UIManager.put("TabbedPane.selected",           C_BODY_BG);
        UIManager.put("TabbedPane.foreground",         new Color(100, 116, 139));
        UIManager.put("TabbedPane.selectedForeground", new Color(96, 165, 250));
        UIManager.put("TabbedPane.contentAreaColor",   C_BODY_BG);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.addTab("  Dashboard  ", buildDashboardPanel());
        tabs.addTab("  Users      ", buildUsersPanel());
        tabs.addTab("  Messages   ", buildMessagesPanel());

        JPanel w = new JPanel(new BorderLayout());
        w.setBackground(C_BODY_BG);
        w.add(tabs, BorderLayout.CENTER);
        return w;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────
    private JPanel buildDashboardPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(C_BODY_BG);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Stat cards
        JPanel row = new JPanel(new GridLayout(1, 3, 12, 0));
        row.setOpaque(false);
        statUsers    = bigLabel("...", C_BLUE);
        statMessages = bigLabel("...", C_GREEN);
        statBanned   = bigLabel("...", C_RED);
        row.add(buildStatCard("Total Users",    statUsers,    C_BLUE,  C_BLUE_BG));
        row.add(buildStatCard("Messages Sent",  statMessages, C_GREEN, C_GREEN_BG));
        row.add(buildStatCard("Banned Users",   statBanned,   C_RED,   C_RED_BG));

        // Activity log
        JPanel actCard = new JPanel(new BorderLayout(0, 8));
        actCard.setBackground(C_CARD_BG);
        actCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel actTitle = new JLabel("Recent Activity");
        actTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        actTitle.setForeground(C_TEXT_SEC);
        activityLog = new JTextArea();
        activityLog.setEditable(false);
        activityLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        activityLog.setForeground(C_TEXT_PRI);
        activityLog.setBackground(C_CARD_BG);
        activityLog.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        activityLog.setText(
            "[server] Waiting for events...\n" +
            "[tip]    Refresh to update stats.\n" +
            "[tip]    Banned rows are highlighted red in the Users tab."
        );
        actCard.add(actTitle,                            BorderLayout.NORTH);
        actCard.add(new JScrollPane(activityLog),        BorderLayout.CENTER);

        JLabel info = new JLabel(
            "<html><center>Use <b>Users</b> to manage accounts &nbsp;·&nbsp; " +
            "Use <b>Messages</b> to view chat history.<br>" +
            "<font color='#b91c1c'>Banned users are highlighted red.</font></center></html>",
            SwingConstants.CENTER);
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setForeground(C_TEXT_SEC);

        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setOpaque(false);
        south.add(actCard, BorderLayout.CENTER);
        south.add(info,    BorderLayout.SOUTH);

        p.add(row,   BorderLayout.NORTH);
        p.add(south, BorderLayout.CENTER);
        return p;
    }

    private JLabel bigLabel(String text, Color color) {
        JLabel l = new JLabel(text, SwingConstants.LEFT);
        l.setFont(new Font("Segoe UI", Font.BOLD, 28));
        l.setForeground(color);
        return l;
    }

    private JPanel buildStatCard(String label, JLabel valueLabel, Color accent, Color bgTint) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(C_CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16))));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(C_TEXT_SEC);

        card.add(lbl,        BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(0, 100));
        return card;
    }

    // ── Users Panel ───────────────────────────────────────────────────────────
    private JPanel buildUsersPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(C_BODY_BG);
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        // Toolbar
        JPanel tb = new JPanel(new BorderLayout());
        tb.setOpaque(false);
        tb.add(sectionLabel("User Management"), BorderLayout.WEST);
        JButton ref = styledButton("Refresh", C_BLUE, C_BLUE_BG);
        ref.addActionListener(e -> loadAllData());
        tb.add(ref, BorderLayout.EAST);

        // Table
        userTableModel = new DefaultTableModel(new String[]{"ID","Username","Role","Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = buildStyledTable(userTableModel);
        userTable.setAutoCreateRowSorter(true);

        // Badge renderer for role/status columns
        userTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                int model = t.convertRowIndexToModel(row);
                String status = (String) userTableModel.getValueAt(model, 3);
                boolean banned = "banned".equalsIgnoreCase(status);

                if (col == 2 || col == 3) {
                    String text = val != null ? val.toString() : "";
                    JLabel badge = new JLabel(text, SwingConstants.CENTER);
                    badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    badge.setOpaque(true);
                    badge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                    if (col == 2) {
                        badge.setBackground(C_BLUE_BG); badge.setForeground(new Color(30, 64, 175));
                    } else if (banned) {
                        badge.setBackground(C_RED_BG); badge.setForeground(C_RED);
                    } else {
                        badge.setBackground(C_GREEN_BG); badge.setForeground(new Color(6, 95, 70));
                    }
                    JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
                    cell.setOpaque(true);
                    cell.setBackground(sel ? t.getSelectionBackground()
                                          : (banned ? C_ROW_BANNED : (row%2==0?Color.WHITE:C_ROW_ALT)));
                    cell.add(badge);
                    return cell;
                }

                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    setBackground(banned ? C_ROW_BANNED : (row%2==0?Color.WHITE:C_ROW_ALT));
                    setForeground(banned ? C_RED : C_TEXT_PRI);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return this;
            }
        });

        userTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(90);

        // Action bar
        banButton    = styledButton("Ban User",    C_AMBER, C_AMBER_BG);
        unbanButton  = styledButton("Unban",       C_GREEN, C_GREEN_BG);
        kickButton   = styledButton("Kick (live)", C_BLUE,  C_BLUE_BG);
        deleteButton = styledButton("Delete",      C_RED,   C_RED_BG);
        banButton.addActionListener(e    -> performBan());
        unbanButton.addActionListener(e  -> performUnban());
        kickButton.addActionListener(e   -> performKick());
        deleteButton.addActionListener(e -> performDelete());

        JLabel hint = new JLabel("Click a row to select  ·  Admin accounts are protected");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        hint.setForeground(C_TEXT_HINT);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btns.setOpaque(false);
        btns.add(banButton); btns.add(unbanButton); btns.add(kickButton); btns.add(deleteButton);

        JPanel actionBar = new JPanel(new BorderLayout(8,0));
        actionBar.setBackground(new Color(248, 250, 252));
        actionBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0,C_BORDER),
            BorderFactory.createEmptyBorder(8,10,8,10)));
        actionBar.add(btns, BorderLayout.WEST);
        actionBar.add(hint, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(userTable);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(C_BORDER));
        card.add(scroll,    BorderLayout.CENTER);
        card.add(actionBar, BorderLayout.SOUTH);

        p.add(tb,   BorderLayout.NORTH);
        p.add(card, BorderLayout.CENTER);
        return p;
    }

    // ── Messages Panel ────────────────────────────────────────────────────────
    private JPanel buildMessagesPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(C_BODY_BG);
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel tb = new JPanel(new BorderLayout());
        tb.setOpaque(false);
        tb.add(sectionLabel("Message Log"), BorderLayout.WEST);
        JButton ref = styledButton("Refresh", C_BLUE, C_BLUE_BG);
        ref.addActionListener(e -> loadMessages());
        tb.add(ref, BorderLayout.EAST);

        messageTableModel = new DefaultTableModel(
                new String[]{"ID","From","To","Message","Timestamp"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        messageTable = buildStyledTable(messageTableModel);
        messageTable.setAutoCreateRowSorter(true);

        // PM row highlight
        messageTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    int model = t.convertRowIndexToModel(row);
                    Object rcv = messageTableModel.getValueAt(model, 2);
                    boolean ispm = rcv != null && !"ALL".equals(rcv.toString());
                    setBackground(ispm ? C_PURPLE_BG : (row%2==0?Color.WHITE:C_ROW_ALT));
                    setForeground(C_TEXT_PRI);
                }
                setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
                return this;
            }
        });

        messageTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        messageTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        messageTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        messageTable.getColumnModel().getColumn(3).setPreferredWidth(360);
        messageTable.getColumnModel().getColumn(4).setPreferredWidth(150);

        JScrollPane scroll = new JScrollPane(messageTable);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JLabel note = new JLabel("  Showing up to 200 most recent messages  ·  Purple rows = private messages");
        note.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        note.setForeground(C_TEXT_HINT);
        note.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(C_BORDER));
        card.add(scroll, BorderLayout.CENTER);
        card.add(note,   BorderLayout.SOUTH);

        p.add(tb,   BorderLayout.NORTH);
        p.add(card, BorderLayout.CENTER);
        return p;
    }

    // ── Status Bar ────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(1,0,0,0,C_BORDER));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        left.setOpaque(false);

        // Painted green dot
        JLabel dot = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_GREEN); g2.fillOval(0, 3, 8, 8);
            }
        };
        dot.setPreferredSize(new Dimension(10, 14));

        statusBar = new JLabel("Server running · localhost:" + AppConstants.SERVER_PORT);
        statusBar.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusBar.setForeground(C_TEXT_SEC);
        left.add(dot); left.add(statusBar);

        JLabel right = new JLabel("ChatApp Admin Console  |  BSc CSIT Advanced Java  ");
        right.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        right.setForeground(C_TEXT_HINT);

        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Shared table factory ──────────────────────────────────────────────────
    private JTable buildStyledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(28);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        t.getTableHeader().setBackground(new Color(243, 246, 250));
        t.getTableHeader().setForeground(C_TEXT_SEC);
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setSelectionBackground(new Color(219, 234, 254));
        t.setSelectionForeground(C_TEXT_PRI);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return t;
    }

    // ── Data Loading ──────────────────────────────────────────────────────────
    private void loadAllData() {
        setStatus("Refreshing...");
        loadDashboard(); loadUsers(); loadMessages();
    }

    private void loadDashboard() {
        new SwingWorker<int[], Void>() {
            @Override protected int[] doInBackground() {
                int users  = DBOperations.getUserCount();
                int msgs   = DBOperations.getMessageCount();
                int banned = 0;
                for (User u : DBOperations.getAllUsers()) if (u.isBanned()) banned++;
                return new int[]{users, msgs, banned};
            }
            @Override protected void done() {
                try {
                    int[] c = get();
                    statUsers.setText(String.valueOf(c[0]));
                    statMessages.setText(String.valueOf(c[1]));
                    statBanned.setText(String.valueOf(c[2]));
                    setStatus("Data refreshed.");
                    if (activityLog != null) {
                        activityLog.setText("[" + AppConstants.getCurrentTime() + "]  Dashboard refreshed — " +
                            c[0] + " users, " + c[1] + " messages, " + c[2] + " banned.");
                    }
                } catch (Exception e) { showError("Dashboard load failed: " + e.getCause()); }
            }
        }.execute();
    }

    private void loadUsers() {
        new SwingWorker<List<User>, Void>() {
            @Override protected List<User> doInBackground() { return DBOperations.getAllUsers(); }
            @Override protected void done() {
                try {
                    userTableModel.setRowCount(0);
                    for (User u : get())
                        userTableModel.addRow(new Object[]{u.getId(), u.getUsername(), u.getRole(), u.getStatus()});
                } catch (Exception e) { showError("User load failed: " + e.getCause()); }
            }
        }.execute();
    }

    private void loadMessages() {
        new SwingWorker<List<Message>, Void>() {
            @Override protected List<Message> doInBackground() { return DBOperations.getAllMessages(); }
            @Override protected void done() {
                try {
                    messageTableModel.setRowCount(0);
                    for (Message m : get())
                        messageTableModel.addRow(new Object[]{
                            m.getId(), m.getSender(), m.getReceiver(), m.getMessage(), m.getTimestamp()});
                } catch (Exception e) { showError("Message load failed: " + e.getCause()); }
            }
        }.execute();
    }

    // ── User Actions ──────────────────────────────────────────────────────────
    private String getSelectedUsername() {
        int view = userTable.getSelectedRow();
        if (view < 0) { showInfo("Please select a user first."); return null; }
        int model = userTable.convertRowIndexToModel(view);
        return (String) userTableModel.getValueAt(model, 1);
    }

    private String getSelectedRole() {
        int view = userTable.getSelectedRow();
        if (view < 0) return null;
        int model = userTable.convertRowIndexToModel(view);
        return (String) userTableModel.getValueAt(model, 2);
    }

    private void performBan() {
        String u = getSelectedUsername(); if (u == null) return;
        if ("admin".equalsIgnoreCase(getSelectedRole())) { showInfo("Cannot ban an admin account."); return; }
        if (JOptionPane.showConfirmDialog(this,
            "Ban '" + u + "'?\nThey will be kicked if currently online.",
            "Confirm Ban", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        new SwingWorker<Boolean,Void>() {
            @Override protected Boolean doInBackground() {
                boolean ok = DBOperations.banUser(u);
                if (ok) sendKickCommand(u);
                return ok;
            }
            @Override protected void done() {
                try { setStatus(get() ? u + " banned." : "Ban failed."); loadAllData(); }
                catch (Exception e) { showError("Ban error: " + e.getCause()); }
            }
        }.execute();
    }

    private void performUnban() {
        String u = getSelectedUsername(); if (u == null) return;
        new SwingWorker<Boolean,Void>() {
            @Override protected Boolean doInBackground() { return DBOperations.unbanUser(u); }
            @Override protected void done() {
                try { setStatus(get() ? u + " unbanned." : "Unban failed."); loadAllData(); }
                catch (Exception e) { showError("Unban error: " + e.getCause()); }
            }
        }.execute();
    }

    private void performDelete() {
        String u = getSelectedUsername(); if (u == null) return;
        if ("admin".equalsIgnoreCase(getSelectedRole())) { showInfo("Cannot delete an admin account."); return; }
        if (JOptionPane.showConfirmDialog(this,
            "PERMANENTLY delete '" + u + "' and all their messages?\nCannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.YES_OPTION) return;
        new SwingWorker<Boolean,Void>() {
            @Override protected Boolean doInBackground() { sendKickCommand(u); return DBOperations.deleteUser(u); }
            @Override protected void done() {
                try { setStatus(get() ? u + " deleted." : "Delete failed."); loadAllData(); }
                catch (Exception e) { showError("Delete error: " + e.getCause()); }
            }
        }.execute();
    }

    private void performKick() {
        String u = getSelectedUsername(); if (u == null) return;
        sendKickCommand(u);
        setStatus("Kick command sent for: " + u);
    }

    /** One-shot kick — no chat session opened on the server. */
    private void sendKickCommand(String target) {
        new Thread(() -> {
            try {
                java.net.Socket s = new java.net.Socket(AppConstants.SERVER_HOST, AppConstants.SERVER_PORT);
                java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.OutputStreamWriter(s.getOutputStream()), true);
                pw.println(AppConstants.CMD_ADMIN_KICK_DIRECT + adminUser.getUsername() + ":" + target);
                Thread.sleep(200);
                s.close();
            } catch (Exception e) {
                System.err.println("[Admin] Could not send kick command: " + e.getMessage());
            }
        }).start();
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    private void confirmLogout() {
        if (JOptionPane.showConfirmDialog(this,
            "Logout and return to Admin Login?", "Confirm Logout",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            dispose();
            new AdminLoginForm().setVisible(true);
        }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────
    private void setStatus(String msg) {
        if (statusBar != null) statusBar.setText(
            "Server running · localhost:" + AppConstants.SERVER_PORT + "  ·  " + msg);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Admin Panel", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Admin Panel – Error", JOptionPane.ERROR_MESSAGE);
        setStatus("Error: " + msg);
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(C_TEXT_SEC);
        return l;
    }

    /** Flat button with tinted background; darkens on hover. */
    private JButton styledButton(String text, Color fg, Color bg) {
        JButton btn = new JButton(text);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(bg.darker()); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    // ── Window ────────────────────────────────────────────────────────────────
    private void configureWindow() {
        setTitle(AppConstants.ADMIN_TITLE);
        setSize(960, 660);
        setMinimumSize(new Dimension(760, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { confirmLogout(); }
        });
    }
}
