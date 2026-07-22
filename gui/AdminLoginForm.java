package gui;

import database.DBOperations;
import model.User;
import util.AppConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

import static gui.UITheme.*;


public class AdminLoginForm extends JFrame {

    private static final Color ADMIN_ACCENT = new Color(230, 90, 90);

    private StyledTextField     usernameField;
    private StyledPasswordField passwordField;
    private StyledButton        loginButton;
    private StyledButton        backButton;
    private JLabel              statusLabel;

    public AdminLoginForm() {
        initComponents();
        setupLayout();
        setupEventHandlers();
        configureWindow();
    }

    private void initComponents() {
        usernameField = new StyledTextField(20);
        passwordField = new StyledPasswordField(20);
        loginButton   = new StyledButton("Admin Sign In", ADMIN_ACCENT, Color.WHITE);
        backButton    = new StyledButton("\u2190 Back",   BG_ELEVATED,  TEXT_MID);
        statusLabel   = new JLabel(" ");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(DANGER);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void setupLayout() {
        BackgroundPanel bg = new BackgroundPanel();
        bg.setLayout(new GridBagLayout());
        setContentPane(bg);

        RoundedPanel card = new RoundedPanel(R_CARD * 2, BG_SURFACE);
        card.setBorderColor(new Color(100, 40, 40));
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(36, 42, 28, 42));
        card.setPreferredSize(new Dimension(440, 510));

        // Title
        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        titleBlock.setBorder(new EmptyBorder(0, 0, 28, 0));

        JLabel shieldIcon = new JLabel("\u26A0", SwingConstants.CENTER);
        shieldIcon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 38));
        shieldIcon.setForeground(ADMIN_ACCENT);
        shieldIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = makeCenteredLabel("Admin Access", FONT_TITLE, TEXT_HI);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = makeCenteredLabel("Restricted — Authorised Personnel Only", FONT_SMALL, TEXT_LO);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        titleBlock.add(shieldIcon);
        titleBlock.add(Box.createVerticalStrut(8));
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(subtitle);

        // Form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        addRow(form, "ADMIN USERNAME", usernameField);
        form.add(Box.createVerticalStrut(14));
        addRow(form, "PASSWORD",       passwordField);
        form.add(Box.createVerticalStrut(6));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(statusLabel);
        form.add(Box.createVerticalStrut(18));
        addBtn(form, loginButton);
        form.add(Box.createVerticalStrut(10));
        addBtn(form, backButton);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(14, 0, 0, 0));
        JLabel notice = new JLabel("\u26A0  All admin actions are logged.");
        notice.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        notice.setForeground(new Color(200, 140, 60));
        footer.add(notice);

        card.add(titleBlock, BorderLayout.NORTH);
        card.add(form,       BorderLayout.CENTER);
        card.add(footer,     BorderLayout.SOUTH);

        bg.add(card, new GridBagConstraints());
    }

    private void addRow(JPanel p, String lbl, JComponent f) {
        JLabel l = fieldLabel(lbl); l.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(l);
        p.add(Box.createVerticalStrut(5));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42)); p.add(f);
    }

    private void addBtn(JPanel p, JButton b) {
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); p.add(b);
    }

    private void setupEventHandlers() {
        loginButton.addActionListener(e -> performAdminLogin());
        backButton.addActionListener(e -> { dispose(); new LoginForm().setVisible(true); });
        passwordField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) performAdminLogin();
            }
        });
        usernameField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) passwordField.requestFocus();
            }
        });
    }

    private void performAdminLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter both fields.", DANGER); return;
        }
        loginButton.setEnabled(false); backButton.setEnabled(false);
        showStatus("Verifying credentials\u2026", ACCENT);
        SwingWorker<User, Void> w = new SwingWorker<>() {
            @Override protected User doInBackground() { return DBOperations.validateLogin(username, password); }
            @Override protected void done() {
                try {
                    User user = get();
                    if (user == null) {
                        showStatus(DBOperations.isUserBanned(username)
                            ? "This admin account is banned." : "Invalid admin credentials.", DANGER);
                        passwordField.setText(""); loginButton.setEnabled(true); backButton.setEnabled(true); return;
                    }
                    if (!user.isAdmin()) {
                        showStatus("Access denied \u2014 not an admin account.", ADMIN_ACCENT);
                        passwordField.setText(""); loginButton.setEnabled(true); backButton.setEnabled(true); return;
                    }
                    showStatus("Access granted!", SUCCESS);
                    new AdminPanel(user).setVisible(true);
                    dispose();
                } catch (Exception ex) {
                    showStatus("Error: " + ex.getMessage(), DANGER);
                    loginButton.setEnabled(true); backButton.setEnabled(true);
                }
            }
        };
        w.execute();
    }

    private void showStatus(String m, Color c) { statusLabel.setText(m); statusLabel.setForeground(c); }

    private void configureWindow() {
        setTitle("ChatApp \u2014 Admin Login");
        setSize(540, 630);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
    }
}
