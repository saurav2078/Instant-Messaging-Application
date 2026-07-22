package gui;

import database.DBOperations;
import util.AppConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

import static gui.UITheme.*;

/**
 * RegisterForm.java — REDESIGNED
 * Matches the dark-card aesthetic of LoginForm.
 */
public class RegisterForm extends JFrame {

    private StyledTextField     usernameField;
    private StyledPasswordField passwordField;
    private StyledPasswordField confirmPassField;
    private StyledButton        registerButton;
    private StyledButton        cancelButton;
    private JLabel              statusLabel;
    private final LoginForm loginForm;

    public RegisterForm(LoginForm loginForm) {
        this.loginForm = loginForm;
        initComponents();
        setupLayout();
        setupEventHandlers();
        configureWindow();
    }

    private void initComponents() {
        usernameField    = new StyledTextField(20);
        passwordField    = new StyledPasswordField(20);
        confirmPassField = new StyledPasswordField(20);
        registerButton   = new StyledButton("Create Account", ACCENT2,    BG_BASE);
        cancelButton     = new StyledButton("Back to Login",  BG_ELEVATED, TEXT_MID);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(DANGER);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void setupLayout() {
        BackgroundPanel bg = new BackgroundPanel();
        bg.setLayout(new GridBagLayout());
        setContentPane(bg);

        RoundedPanel card = new RoundedPanel(R_CARD * 2, BG_SURFACE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(34, 42, 26, 42));
        card.setPreferredSize(new Dimension(440, 570));

        // Title
        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        titleBlock.setBorder(new EmptyBorder(0, 0, 26, 0));

        JLabel title = makeCenteredLabel("Create Account", FONT_TITLE, TEXT_HI);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = makeCenteredLabel("Join the conversation", FONT_SMALL, TEXT_LO);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(subtitle);

        // Form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        addRow(form, "USERNAME",         usernameField);
        form.add(Box.createVerticalStrut(12));
        addRow(form, "PASSWORD",         passwordField);
        form.add(Box.createVerticalStrut(12));
        addRow(form, "CONFIRM PASSWORD", confirmPassField);
        form.add(Box.createVerticalStrut(6));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(statusLabel);
        form.add(Box.createVerticalStrut(16));
        addBtn(form, registerButton);
        form.add(Box.createVerticalStrut(10));
        addBtn(form, cancelButton);

        // Footer hint
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(12, 0, 0, 0));
        JLabel hint = new JLabel("Password must be at least 4 characters");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(TEXT_LO);
        footer.add(hint);

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
        registerButton.addActionListener(e -> performRegistration());
        cancelButton.addActionListener(e -> goBackToLogin());
        confirmPassField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) performRegistration();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { goBackToLogin(); }
        });
    }

    private void performRegistration() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm  = new String(confirmPassField.getPassword());
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showStatus("All fields are required.", DANGER); return;
        }
        if (username.length() < 3)    { showStatus("Username must be at least 3 characters.", DANGER); return; }
        if (username.contains(" "))   { showStatus("Username cannot contain spaces.", DANGER); return; }
        if (password.length() < 4)    { showStatus("Password must be at least 4 characters.", DANGER); return; }
        if (!password.equals(confirm)){ showStatus("Passwords do not match.", DANGER); confirmPassField.setText(""); return; }

        registerButton.setEnabled(false);
        showStatus("Creating account\u2026", ACCENT);
        SwingWorker<Boolean, Void> w = new SwingWorker<>() {
            @Override protected Boolean doInBackground() { return DBOperations.registerUser(username, password); }
            @Override protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(RegisterForm.this,
                            "Account created! You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        goBackToLogin();
                    } else {
                        showStatus("Username already taken. Choose another.", DANGER);
                        usernameField.setText(""); registerButton.setEnabled(true);
                    }
                } catch (Exception ex) {
                    showStatus("Error: " + ex.getMessage(), DANGER); registerButton.setEnabled(true);
                }
            }
        };
        w.execute();
    }

    private void goBackToLogin() { loginForm.setVisible(true); dispose(); }
    private void showStatus(String m, Color c) { statusLabel.setText(m); statusLabel.setForeground(c); }

    private void configureWindow() {
        setTitle(AppConstants.REGISTER_TITLE);
        setSize(540, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
    }
}
