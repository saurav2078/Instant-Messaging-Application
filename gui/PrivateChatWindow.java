package gui;

import client.Client;
import database.DBOperations;
import model.Message;
import util.AppConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import static gui.UITheme.*;


public class PrivateChatWindow extends JFrame {

    private JTextArea  chatArea;
    private StyledTextField messageField;
    private StyledButton    sendButton;
    private JLabel          statusBar;

    private final String   myUsername;
    private final String   partnerUsername;
    private final Client   client;
    private final Runnable onCloseCallback;

    // BUG 1 FIX: buffering while history loads
    private boolean historyLoaded = false;
    private final java.util.Queue<Runnable> pendingMessages = new java.util.ArrayDeque<>();

    private static final Color[] AVATAR_COLORS = {
        new Color(67, 97, 238), new Color(72, 149, 239), new Color(76, 201, 240),
        new Color(114, 9, 183), new Color(247, 37, 133)
    };

    public PrivateChatWindow(String myUsername, String partnerUsername,
                              Client client, Runnable onCloseCallback) {
        this.myUsername      = myUsername;
        this.partnerUsername = partnerUsername;
        this.client          = client;
        this.onCloseCallback = onCloseCallback;
        initComponents(); setupLayout(); setupEventHandlers(); configureWindow(); loadHistory();
    }

    private void initComponents() {
        chatArea = new JTextArea();
        styleChatArea(chatArea);

        messageField = new StyledTextField();
        messageField.setFont(FONT_BODY);
        messageField.setBackground(BG_ELEVATED);
        messageField.setForeground(TEXT_HI);
        messageField.setCaretColor(ACCENT);
        messageField.setBorder(new EmptyBorder(0, 13, 0, 13));
        messageField.setToolTipText("Message " + partnerUsername + "\u2026");

        sendButton = new StyledButton("Send \u2192", ACCENT, BG_BASE);
        sendButton.setPreferredSize(new Dimension(90, 40));

        statusBar = makeLabel("  Private chat with " + partnerUsername, FONT_SMALL, TEXT_LO);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_BASE);

        // ── Header ─────────────────────────────────────────────────────────
        Color avColor = AVATAR_COLORS[Math.abs(partnerUsername.hashCode()) % AVATAR_COLORS.length];
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
            new EmptyBorder(10, 16, 10, 16)));

        JPanel nameArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        nameArea.setOpaque(false);
        nameArea.add(new AvatarLabel(partnerUsername, avColor, 34));

        JPanel nameInfo = new JPanel();
        nameInfo.setLayout(new BoxLayout(nameInfo, BoxLayout.Y_AXIS));
        nameInfo.setOpaque(false);
        JLabel nameLabel = makeLabel(partnerUsername, FONT_H2, TEXT_HI);
        JLabel subLabel  = makeLabel("Private Message", FONT_SMALL, TEXT_LO);
        nameInfo.add(nameLabel); nameInfo.add(subLabel);
        nameArea.add(nameInfo);
        header.add(nameArea, BorderLayout.WEST);

        JLabel lock = makeLabel("\uD83D\uDD12 End-to-end", FONT_SMALL, ACCENT2);
        header.add(lock, BorderLayout.EAST);

        // ── Chat ───────────────────────────────────────────────────────────
        JScrollPane scroll = new JScrollPane(chatArea);
        styleScrollPane(scroll, BG_BASE);

        // ── Footer ─────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG_SURFACE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER));

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setBackground(BG_SURFACE);
        inputRow.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel fieldWrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_ELEVATED);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), R_INPUT*2, R_INPUT*2);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, R_INPUT*2, R_INPUT*2);
                g2.dispose();
            }
        };
        fieldWrap.setOpaque(false);
        fieldWrap.setPreferredSize(new Dimension(0, 40));
        fieldWrap.add(messageField, BorderLayout.CENTER);

        inputRow.add(fieldWrap,  BorderLayout.CENTER);
        inputRow.add(sendButton, BorderLayout.EAST);

        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setBackground(BG_SURFACE);
        statusRow.setBorder(new EmptyBorder(0, 14, 6, 14));
        statusRow.add(statusBar, BorderLayout.WEST);

        footer.add(inputRow,  BorderLayout.CENTER);
        footer.add(statusRow, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);
        add(footer,  BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        sendButton.addActionListener(e -> sendMessage());
        messageField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) sendMessage();
            }
        });
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;
        if (client != null && client.isConnected()) {
            client.sendMessage(AppConstants.CMD_PM + partnerUsername + ":" + text);
            messageField.setText("");
        } else appendSystem("Not connected to server.");
    }

    public void appendIncoming(String text) {
        if (!historyLoaded) { pendingMessages.add(() -> doIncoming(text)); return; }
        doIncoming(text);
    }
    public void appendOutgoing(String text) {
        if (!historyLoaded) { pendingMessages.add(() -> doOutgoing(text)); return; }
        doOutgoing(text);
    }

    private void doIncoming(String text) {
        chatArea.append("[" + AppConstants.getCurrentTime() + "] " + partnerUsername + ": " + text + "\n");
        scrollDown(); toFront(); requestFocus();
    }
    private void doOutgoing(String text) {
        chatArea.append("[" + AppConstants.getCurrentTime() + "] You: " + text + "\n");
        scrollDown();
    }

    private void loadHistory() {
        appendSystem("Loading conversation history\u2026");
        new SwingWorker<List<Message>, Void>() {
            @Override protected List<Message> doInBackground() {
                return DBOperations.getPrivateHistory(myUsername, partnerUsername);
            }
            @Override protected void done() {
                try {
                    List<Message> history = get();
                    chatArea.setText("");
                    if (history.isEmpty()) appendSystem("No previous messages. Say hello!");
                    else {
                        appendSystem("--- Conversation History ---");
                        for (Message m : history)
                            if (m.getSender().equals(myUsername)) doOutgoing(m.getMessage());
                            else doIncoming(m.getMessage());
                        appendSystem("--- End of History ---");
                    }
                    historyLoaded = true;
                    while (!pendingMessages.isEmpty()) pendingMessages.poll().run();
                    messageField.requestFocus();
                } catch (Exception ex) {
                    historyLoaded = true;
                    while (!pendingMessages.isEmpty()) pendingMessages.poll().run();
                    appendSystem("Could not load history: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void appendSystem(String m) { chatArea.append("  \u2014 " + m + "\n"); scrollDown(); }
    private void scrollDown() { chatArea.setCaretPosition(chatArea.getDocument().getLength()); }

    private void configureWindow() {
        setTitle("Private Chat \u2014 " + partnerUsername);
        setSize(620, 560); setMinimumSize(new Dimension(480, 400));
        setLocationRelativeTo(null);
        int off = (int)(Math.random() * 60) - 30;
        setLocation(getX() + off + 200, getY() + off);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (onCloseCallback != null) onCloseCallback.run();
                dispose();
            }
        });
    }
}
