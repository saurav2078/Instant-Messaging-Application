package gui;

import client.Client;
import model.User;
import util.AppConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static gui.UITheme.*;


public class ChatWindow extends JFrame implements Client.MessageListener {

    // ── Components ────────────────────────────────────────────────────────────
    private JTextArea                chatArea;
    private StyledTextField          messageField;
    private StyledButton             sendButton;
    private StyledButton             logoutButton;
    private DefaultListModel<String> userListModel;
    private JList<String>            onlineUsersList;
    private JLabel                   statusBar;
    private JLabel                   typingLabel;
    private JLabel                   memberCountLabel;

    // ── State ─────────────────────────────────────────────────────────────────
    private Client client;
    private final User currentUser;
    private final Map<String, PrivateChatWindow> pmWindows = new HashMap<>();
    private Timer   typingTimer;
    private boolean isCurrentlyTyping = false;

    public ChatWindow(User user) {
        this.currentUser = user;
        initComponents();
        setupLayout();
        setupEventHandlers();
        configureWindow();
        connectToServer();
    }

    // ── Component initialisation ──────────────────────────────────────────────
    private void initComponents() {
        chatArea = new JTextArea();
        styleChatArea(chatArea);

        messageField = new StyledTextField();
        messageField.setFont(FONT_BODY);
        messageField.setBackground(BG_ELEVATED);
        messageField.setForeground(TEXT_HI);
        messageField.setCaretColor(ACCENT);
        messageField.setBorder(new EmptyBorder(0, 13, 0, 13));
        messageField.setToolTipText("Type a message and press Enter");

        sendButton   = new StyledButton("Send \u2192", ACCENT,       BG_BASE);
        logoutButton = new StyledButton("Sign Out",  BG_ELEVATED,  TEXT_MID);
        sendButton.setPreferredSize(new Dimension(90, 40));
        logoutButton.setPreferredSize(new Dimension(84, 30));
        // Danger-colour on hover for logout
        logoutButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { logoutButton.setForeground(DANGER); repaint(); }
            @Override public void mouseExited(MouseEvent e)  { logoutButton.setForeground(TEXT_MID); repaint(); }
        });

        userListModel   = new DefaultListModel<>();
        onlineUsersList = new JList<>(userListModel);
        onlineUsersList.setFont(FONT_BODY);
        onlineUsersList.setFixedCellHeight(42);
        onlineUsersList.setBackground(BG_SURFACE);
        onlineUsersList.setForeground(TEXT_HI);
        onlineUsersList.setBorder(new EmptyBorder(4, 6, 4, 6));
        onlineUsersList.setSelectionBackground(BG_HOVER);
        onlineUsersList.setSelectionForeground(ACCENT);
        onlineUsersList.setCellRenderer(new UserCellRenderer());

        memberCountLabel = makeLabel("0 online", FONT_LABEL, TEXT_LO);

        statusBar  = makeLabel("  Connecting\u2026", FONT_SMALL, TEXT_LO);
        typingLabel = makeLabel(" ", new Font("Segoe UI", Font.ITALIC, 11), ACCENT);
        typingLabel.setBorder(new EmptyBorder(0, 14, 0, 0));

        typingTimer = new Timer(2000, e -> stopTyping());
        typingTimer.setRepeats(false);
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    private void setupLayout() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_BASE);

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
            new EmptyBorder(10, 18, 10, 18)));

        JPanel leftHead = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftHead.setOpaque(false);
        JLabel dot = makeLabel("\u25CF", new Font("Segoe UI Symbol", Font.PLAIN, 13), ACCENT2);
        JLabel appTitle = makeLabel("ChatApp", FONT_H2, TEXT_HI);
        JLabel roomTag  = makeLabel("/ General", FONT_BODY, TEXT_MID);
        leftHead.add(dot); leftHead.add(appTitle); leftHead.add(roomTag);

        JPanel rightHead = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHead.setOpaque(false);
        JLabel userBadge = makeLabel("\u2022 " + currentUser.getUsername(), FONT_SMALL, TEXT_MID);
        rightHead.add(userBadge);
        rightHead.add(logoutButton);

        header.add(leftHead,  BorderLayout.WEST);
        header.add(rightHead, BorderLayout.EAST);

        // ── Sidebar ───────────────────────────────────────────────────────────
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(BG_SURFACE);
        sidebar.setPreferredSize(new Dimension(180, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, DIVIDER));

        JPanel sideHead = new JPanel(new BorderLayout());
        sideHead.setBackground(BG_SURFACE);
        sideHead.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
            new EmptyBorder(10, 13, 10, 13)));
        sideHead.add(makeLabel("MEMBERS", FONT_LABEL, TEXT_LO), BorderLayout.WEST);
        sideHead.add(memberCountLabel, BorderLayout.EAST);

        JScrollPane usersScroll = new JScrollPane(onlineUsersList);
        styleScrollPane(usersScroll, BG_SURFACE);

        JLabel pmHint = makeCenteredLabel(
            "<html><center>Double-click to<br>open private chat</center></html>",
            new Font("Segoe UI", Font.ITALIC, 10), TEXT_LO);
        pmHint.setBorder(new EmptyBorder(6, 6, 8, 6));

        sidebar.add(sideHead,    BorderLayout.NORTH);
        sidebar.add(usersScroll, BorderLayout.CENTER);
        sidebar.add(pmHint,      BorderLayout.SOUTH);

        // ── Chat scroll ───────────────────────────────────────────────────────
        JScrollPane chatScroll = new JScrollPane(chatArea);
        styleScrollPane(chatScroll, BG_BASE);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScroll, sidebar);
        split.setResizeWeight(0.82);
        split.setDividerSize(1);
        split.setBorder(null);
        split.setBackground(DIVIDER);

        // ── Footer ────────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG_SURFACE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER));

        // Input row
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setBackground(BG_SURFACE);
        inputRow.setBorder(new EmptyBorder(10, 14, 10, 14));

        // Wrap text field in a rounded container
        JPanel fieldWrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_ELEVATED);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), R_INPUT * 2, R_INPUT * 2);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, R_INPUT * 2, R_INPUT * 2);
                g2.dispose();
            }
        };
        fieldWrap.setOpaque(false);
        fieldWrap.setPreferredSize(new Dimension(0, 40));
        fieldWrap.add(messageField, BorderLayout.CENTER);

        inputRow.add(fieldWrap,  BorderLayout.CENTER);
        inputRow.add(sendButton, BorderLayout.EAST);

        // Status row
        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setBackground(BG_SURFACE);
        statusRow.setBorder(new EmptyBorder(0, 14, 6, 14));
        statusRow.add(typingLabel, BorderLayout.WEST);
        statusRow.add(statusBar,   BorderLayout.EAST);

        footer.add(inputRow,  BorderLayout.CENTER);
        footer.add(statusRow, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(split,  BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    // ── Event handlers ────────────────────────────────────────────────────────
    private void setupEventHandlers() {
        sendButton.addActionListener(e -> sendMessage());
        messageField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) sendMessage();
                else notifyTyping();
            }
        });
        logoutButton.addActionListener(e -> performLogout());
        onlineUsersList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                String sel = onlineUsersList.getSelectedValue();
                if (sel == null) return;
                String partner = sel.startsWith("(me) ") ? sel.substring(5).trim() : sel.trim();
                if (partner.equals(currentUser.getUsername())) {
                    JOptionPane.showMessageDialog(ChatWindow.this,
                        "You cannot send a private message to yourself.",
                        "Private Chat", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                openPrivateChatWith(partner);
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(ChatWindow.this,
                    "Exit ChatApp?", "Exit", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    performExit();
            }
        });
    }

    // ── Typing indicator ──────────────────────────────────────────────────────
    private void notifyTyping() {
        if (!isCurrentlyTyping && client != null && client.isConnected()) {
            isCurrentlyTyping = true;
            client.sendMessage(AppConstants.CMD_TYPING + "ALL");
        }
        typingTimer.restart();
    }
    private void stopTyping() {
        if (isCurrentlyTyping && client != null && client.isConnected()) {
            isCurrentlyTyping = false;
            client.sendMessage(AppConstants.CMD_TYPING_STOP + "ALL");
        }
    }

    // ── Networking ────────────────────────────────────────────────────────────
    private void connectToServer() {
        client = new Client(currentUser.getUsername());
        appendSystem("Connecting to server\u2026");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { client.connect(ChatWindow.this); return null; }
            @Override protected void done() {
                try {
                    get();
                    appendSystem("Connected! Welcome, " + currentUser.getUsername() + "!");
                    sendButton.setEnabled(true); messageField.setEnabled(true); messageField.requestFocus();
                    statusBar.setText("  \u25CF " + currentUser.getUsername()); statusBar.setForeground(ACCENT2);
                } catch (Exception ex) {
                    appendSystem("Could not connect: " + ex.getMessage());
                    sendButton.setEnabled(false);
                    statusBar.setText("  Disconnected"); statusBar.setForeground(DANGER);
                }
            }
        }.execute();
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;
        if (client != null && client.isConnected()) {
            stopTyping(); typingTimer.stop();
            client.sendMessage(text); messageField.setText("");
        } else {
            appendSystem("Not connected to server.");
        }
    }

    private void performLogout() {
        if (!isDisplayable()) return;
        stopTyping();
        new LoginForm().setVisible(true); setVisible(false);
        new ArrayList<>(pmWindows.values()).forEach(PrivateChatWindow::dispose);
        pmWindows.clear();
        if (client != null) client.disconnect();
        dispose();
    }

    private void performExit() {
        stopTyping();
        new ArrayList<>(pmWindows.values()).forEach(PrivateChatWindow::dispose);
        pmWindows.clear();
        if (client != null) client.disconnect();
        dispose(); System.exit(0);
    }

    // ── MessageListener ───────────────────────────────────────────────────────
    @Override public void onMessageReceived(String message) {
        if (message.startsWith(AppConstants.CMD_TYPING_NOTIFY)) {
            typingLabel.setText("  " + message.substring(AppConstants.CMD_TYPING_NOTIFY.length()) + " is typing\u2026");
            return;
        }
        if (message.startsWith(AppConstants.CMD_TYPING_STOP_NOTIFY)) { typingLabel.setText(" "); return; }
        if (message.equals(AppConstants.CMD_BANNED)) {
            JOptionPane.showMessageDialog(this,
                "Your account has been banned by an administrator.\nYou will be disconnected.",
                "Account Banned", JOptionPane.ERROR_MESSAGE);
            performLogout(); return;
        }
        if      (message.startsWith(AppConstants.CMD_USER_LIST)) updateOnlineUsers(message.substring(AppConstants.CMD_USER_LIST.length()));
        else if (message.startsWith(AppConstants.CMD_HISTORY))   appendHistory(message.substring(AppConstants.CMD_HISTORY.length()));
        else if (message.startsWith(AppConstants.CMD_PM_IN)) {
            String pay = message.substring(AppConstants.CMD_PM_IN.length()); int c = pay.indexOf(':');
            if (c > 0) {
                PrivateChatWindow w = getOrCreatePM(pay.substring(0, c));
                w.setVisible(true); w.appendIncoming(pay.substring(c + 1));
                if (!w.isFocused()) w.toFront();
            }
        } else if (message.startsWith(AppConstants.CMD_PM_ECHO)) {
            String pay = message.substring(AppConstants.CMD_PM_ECHO.length()); int c = pay.indexOf(':');
            if (c > 0) { PrivateChatWindow w = getOrCreatePM(pay.substring(0, c)); w.setVisible(true); w.appendOutgoing(pay.substring(c + 1)); }
        } else if (message.startsWith(AppConstants.CMD_PM_OFFLINE)) {
            appendSystem("[!] " + message.substring(AppConstants.CMD_PM_OFFLINE.length()) + " is offline. Message not delivered.");
        } else if (message.contains("***")) appendSystem(message);
        else chatArea.append(message + "\n"); scrollDown();
    }

    @Override public void onConnectionLost() {
        appendSystem("Connection to server lost.");
        sendButton.setEnabled(false);
        statusBar.setText("  Disconnected"); statusBar.setForeground(DANGER);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void appendSystem(String m) { chatArea.append("  \u2014 " + m + "\n"); scrollDown(); }
    private void appendHistory(String m) { chatArea.append(m + "\n"); scrollDown(); }
    private void scrollDown() { chatArea.setCaretPosition(chatArea.getDocument().getLength()); }

    private void openPrivateChatWith(String p) { PrivateChatWindow w = getOrCreatePM(p); w.setVisible(true); w.toFront(); }
    private PrivateChatWindow getOrCreatePM(String p) {
        PrivateChatWindow ex = pmWindows.get(p);
        if (ex != null && ex.isDisplayable()) return ex;
        PrivateChatWindow w = new PrivateChatWindow(currentUser.getUsername(), p, client, () -> pmWindows.remove(p));
        pmWindows.put(p, w); return w;
    }

    private void updateOnlineUsers(String csv) {
        userListModel.clear(); int n = 0;
        if (!csv.isBlank()) {
            for (String u : csv.split(",")) {
                String name = u.trim();
                if (!name.isEmpty()) { userListModel.addElement(name.equals(currentUser.getUsername()) ? "(me) " + name : name); n++; }
            }
        }
        memberCountLabel.setText(n + " online");
    }

    private void configureWindow() {
        setTitle(AppConstants.APP_TITLE + " \u2014 " + currentUser.getUsername());
        setSize(1020, 680); setMinimumSize(new Dimension(780, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        sendButton.setEnabled(false); messageField.setEnabled(false);
    }

    // ── Avatar user list cell renderer ────────────────────────────────────────
    private static class UserCellRenderer extends DefaultListCellRenderer {
        private static final Color[] AVATAR_COLORS = {
            new Color(67, 97, 238), new Color(72, 149, 239), new Color(76, 201, 240),
            new Color(114, 9, 183), new Color(247, 37, 133)
        };
        @Override public Component getListCellRendererComponent(
                JList<?> list, Object value, int idx, boolean sel, boolean focus) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 4));
            row.setOpaque(true);
            String raw     = value == null ? "" : value.toString();
            boolean isMe   = raw.startsWith("(me) ");
            String name    = isMe ? raw.substring(5) : raw;
            Color aBg      = AVATAR_COLORS[Math.abs(name.hashCode()) % AVATAR_COLORS.length];
            Color nameCol  = isMe ? ACCENT : TEXT_HI;
            AvatarLabel av = new AvatarLabel(name, aBg, 28);
            JLabel nl      = makeLabel(name + (isMe ? " (you)" : ""), FONT_BODY, nameCol);
            row.add(av); row.add(nl);
            row.setBackground(sel ? BG_HOVER : BG_SURFACE);
            return row;
        }
    }
}
