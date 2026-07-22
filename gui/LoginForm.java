package gui;

import database.DBOperations;
import model.User;
import util.AppConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

import static gui.UITheme.*;


public class LoginForm extends JFrame {

    private StyledTextField     usernameField;
    private StyledPasswordField passwordField;
    private StyledButton        loginButton;
    private StyledButton        registerButton;
    private JLabel              statusLabel;

    public LoginForm() {
        initComponents();
        setupLayout();
        setupEventHandlers();
        configureWindow();
    }

    private void initComponents() {
        usernameField  = new StyledTextField(20);
        passwordField  = new StyledPasswordField(20);
        loginButton    = new StyledButton("Sign In",        ACCENT,            BG_BASE);
        registerButton = new StyledButton("Create Account", BG_ELEVATED,       TEXT_MID);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(DANGER);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void setupLayout() {
        BackgroundPanel bg = new BackgroundPanel();
        bg.setLayout(new GridBagLayout());
        setContentPane(bg);

        // ── Card ──────────────────────────────────────────────────────────────
        RoundedPanel card = new RoundedPanel(R_CARD * 2, BG_SURFACE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(38, 42, 30, 42));
        card.setPreferredSize(new Dimension(440, 520));

        // Title block
        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        titleBlock.setBorder(new EmptyBorder(0, 0, 30, 0));

        JLabel icon = new JLabel("\u25C8", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 44));
        icon.setForeground(ACCENT);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = makeCenteredLabel("ChatApp", FONT_TITLE, TEXT_HI);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = makeCenteredLabel("Welcome back — sign in to continue", FONT_SMALL, TEXT_LO);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        titleBlock.add(icon);
        titleBlock.add(Box.createVerticalStrut(8));
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(5));
        titleBlock.add(subtitle);

        // Form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        addFormRow(form, "USERNAME", usernameField);
        form.add(Box.createVerticalStrut(14));
        addFormRow(form, "PASSWORD", passwordField);
        form.add(Box.createVerticalStrut(6));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(statusLabel);
        form.add(Box.createVerticalStrut(18));
        addFullWidthButton(form, loginButton);
        form.add(Box.createVerticalStrut(10));
        addFullWidthButton(form, registerButton);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(16, 0, 0, 0));

        JLabel adminLink = new JLabel("<html><u>Admin Login \u2192</u></html>");
        adminLink.setFont(FONT_SMALL);
        adminLink.setForeground(TEXT_LO);
        adminLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        adminLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openAdminLogin(); }
            @Override public void mouseEntered(MouseEvent e) { adminLink.setForeground(ACCENT); }
            @Override public void mouseExited(MouseEvent e)  { adminLink.setForeground(TEXT_LO); }
        });
        footer.add(adminLink, BorderLayout.EAST);

        card.add(titleBlock, BorderLayout.NORTH);
        card.add(form,       BorderLayout.CENTER);
        card.add(footer,     BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        bg.add(card, gbc);
    }

    private void addFormRow(JPanel parent, String labelText, JComponent field) {
        JLabel lbl = fieldLabel(labelText);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(lbl);
        parent.add(Box.createVerticalStrut(5));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        parent.add(field);
    }

    private void addFullWidthButton(JPanel parent, JButton btn) {
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        parent.add(btn);
    }

    private void setupEventHandlers() {
        loginButton.addActionListener(e -> performLogin());
        registerButton.addActionListener(e -> openRegisterForm());
        passwordField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) performLogin();
            }
        });
        usernameField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) passwordField.requestFocus();
            }
        });
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter both username and password.", DANGER); return;
        }
        loginButton.setEnabled(false);
        showStatus("Signing in\u2026", ACCENT);
        SwingWorker<User, Void> w = new SwingWorker<>() {
            @Override protected User doInBackground() {
                return DBOperations.validateLogin(username, password);
            }
            @Override protected void done() {
                try {
                    User user = get();
                    if (user == null) {
                        showStatus(DBOperations.isUserBanned(username)
                            ? "Your account has been banned." : "Invalid username or password.", DANGER);
                        passwordField.setText(""); loginButton.setEnabled(true); return;
                    }
                    if (user.isAdmin()) {
                        showStatus("Admin accounts must use Admin Login.", WARNING);
                        passwordField.setText(""); loginButton.setEnabled(true); return;
                    }
                    showStatus("Welcome back!", SUCCESS);
                    new ChatWindow(user).setVisible(true);
                    dispose();
                } catch (Exception ex) {
                    showStatus("Error: " + ex.getMessage(), DANGER);
                    loginButton.setEnabled(true);
                }
            }
        };
        w.execute();
    }

    private void openRegisterForm() { new RegisterForm(this).setVisible(true); setVisible(false); }
    private void openAdminLogin()   { new AdminLoginForm().setVisible(true); dispose(); }
    private void showStatus(String m, Color c) { statusLabel.setText(m); statusLabel.setForeground(c); }

    private void configureWindow() {
        setTitle(AppConstants.LOGIN_TITLE);
        setSize(540, 640);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new LoginForm().setVisible(true);
        });
    }
}
