package gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * UITheme.java — Shared design system for ChatApp
 *
 * Provides:
 *   - Colour palette constants
 *   - Reusable custom Swing components (DarkPanel, StyledButton,
 *     StyledTextField, StyledPasswordField, AvatarLabel)
 *   - Static helpers (makeLabel, separator, applyScrollStyle)
 *
 * All GUI classes import and use this instead of scattering colour
 * literals everywhere.
 */
public final class UITheme {

    private UITheme() {}

    // ── Colour Palette ────────────────────────────────────────────────────────
    public static final Color BG_BASE     = new Color(25,  30,  50);   // deepest bg
    public static final Color BG_SURFACE  = new Color(22,  27,  44);   // cards / panels
    public static final Color BG_ELEVATED = new Color(30,  37,  60);   // inputs / rows
    public static final Color BG_HOVER    = new Color(38,  47,  75);   // hover states

    public static final Color ACCENT      = new Color(100, 180, 255);  // sky-blue primary
    public static final Color ACCENT2     = new Color( 80, 220, 160);  // mint green secondary
    public static final Color DANGER      = new Color(255, 110, 110);  // red
    public static final Color WARNING     = new Color(255, 180,  70);  // amber
    public static final Color SUCCESS     = ACCENT2;

    public static final Color TEXT_HI     = new Color(230, 238, 255);  // primary text
    public static final Color TEXT_MID    = new Color(150, 165, 200);  // secondary text
    public static final Color TEXT_LO     = new Color( 80,  95, 130);  // muted / hints

    public static final Color BORDER      = new Color( 42,  52,  80);  // subtle borders
    public static final Color DIVIDER     = new Color( 35,  43,  68);

    // ── Typography ────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  22);
    public static final Font FONT_H2      = new Font("Segoe UI", Font.BOLD,  15);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_LABEL   = new Font("Segoe UI", Font.BOLD,  10);
    public static final Font FONT_MONO    = new Font("Consolas",  Font.PLAIN, 13);

    // ── Radius constants ──────────────────────────────────────────────────────
    public static final int R_CARD   = 14;
    public static final int R_INPUT  = 8;
    public static final int R_BUTTON = 8;

    // ══════════════════════════════════════════════════════════════════════════
    //  Custom Components
    // ══════════════════════════════════════════════════════════════════════════

    /** A JPanel that paints a rounded rectangle background. */
    public static class RoundedPanel extends JPanel {
        private final int   arc;
        private final Color bg;
        private Color borderColor = BORDER;
        private boolean drawBorder = true;

        public RoundedPanel(int arc, Color bg) {
            this.arc = arc; this.bg = bg;
            setOpaque(false);
        }
        public void setBorderColor(Color c) { this.borderColor = c; }
        public void setDrawBorder(boolean b) { this.drawBorder = b; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            if (drawBorder) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Full-window gradient background panel with a subtle dot-grid overlay. */
    public static class BackgroundPanel extends JPanel {
        public BackgroundPanel() { setOpaque(true); setBackground(BG_BASE); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            // Radial glow top-left
            RadialGradientPaint rg = new RadialGradientPaint(
                new Point2D.Float(getWidth() * 0.2f, getHeight() * 0.1f),
                getWidth() * 0.7f,
                new float[]{0f, 1f},
                new Color[]{new Color(30, 55, 120, 60), new Color(0,0,0,0)});
            g2.setPaint(rg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            // Dot grid
            g2.setColor(new Color(255, 255, 255, 10));
            for (int x = 20; x < getWidth(); x += 28)
                for (int y = 20; y < getHeight(); y += 28)
                    g2.fillOval(x, y, 2, 2);
            g2.dispose();
        }
    }

    /** Styled text field with rounded outline that highlights on focus. */
    public static class StyledTextField extends JTextField {
        private boolean focused = false;
        public StyledTextField() { init(); }
        public StyledTextField(int cols) { super(cols); init(); }
        private void init() {
            setOpaque(false);
            setBackground(BG_ELEVATED);
            setForeground(TEXT_HI);
            setCaretColor(ACCENT);
            setFont(FONT_BODY);
            setBorder(new EmptyBorder(10, 13, 10, 13));
            setPreferredSize(new Dimension(280, 42));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e)   { focused = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BG_ELEVATED);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), R_INPUT*2, R_INPUT*2);
            g2.setColor(focused ? ACCENT : BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.8f : 1f));
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, R_INPUT*2, R_INPUT*2);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Same as StyledTextField but for passwords. */
    public static class StyledPasswordField extends JPasswordField {
        private boolean focused = false;
        public StyledPasswordField() { init(); }
        public StyledPasswordField(int cols) { super(cols); init(); }
        private void init() {
            setOpaque(false);
            setBackground(BG_ELEVATED);
            setForeground(TEXT_HI);
            setCaretColor(ACCENT);
            setFont(FONT_BODY);
            setBorder(new EmptyBorder(10, 13, 10, 13));
            setPreferredSize(new Dimension(280, 42));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e)   { focused = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BG_ELEVATED);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), R_INPUT*2, R_INPUT*2);
            g2.setColor(focused ? ACCENT : BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.8f : 1f));
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, R_INPUT*2, R_INPUT*2);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Rounded, colour-filled button with hover lightening. */
    public static class StyledButton extends JButton {
        private final Color normalBg;
        private final Color normalFg;
        private boolean hovered = false;

        public StyledButton(String text, Color bg, Color fg) {
            super(text);
            this.normalBg = bg; this.normalFg = fg;
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setForeground(fg); setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(140, 40));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color col = !isEnabled()     ? new Color(40, 50, 75)
                      : hovered          ? normalBg.brighter()
                      : normalBg;
            g2.setColor(col);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), R_BUTTON*2, R_BUTTON*2);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Circular avatar showing the first letter of a name. */
    public static class AvatarLabel extends JLabel {
        private final String name;
        private final Color  color;
        private final int    size;

        public AvatarLabel(String name, Color color, int size) {
            this.name = name; this.color = color; this.size = size;
            setPreferredSize(new Dimension(size, size));
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, size, size);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, size / 2));
            String ch = name == null || name.isEmpty() ? "?" : String.valueOf(Character.toUpperCase(name.charAt(0)));
            FontMetrics fm = g2.getFontMetrics();
            int x = (size - fm.stringWidth(ch)) / 2;
            int y = (size - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(ch, x, y);
            g2.dispose();
        }
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    public static JLabel makeLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text); l.setFont(font); l.setForeground(color); return l;
    }

    public static JLabel makeCenteredLabel(String text, Font font, Color color) {
        JLabel l = makeLabel(text, font, color);
        l.setHorizontalAlignment(SwingConstants.CENTER); return l;
    }

    public static JLabel fieldLabel(String text) {
        return makeLabel(text, FONT_LABEL, TEXT_LO);
    }

    public static JSeparator makeSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(DIVIDER); sep.setBackground(DIVIDER); return sep;
    }

    /** Style a JScrollPane to blend with the dark theme. */
    public static void styleScrollPane(JScrollPane sp, Color viewportBg) {
        sp.setBorder(null);
        sp.getViewport().setBackground(viewportBg);
        sp.getVerticalScrollBar().setBackground(viewportBg);
        sp.getVerticalScrollBar().setOpaque(true);
    }

    /** Style a JTextArea as a dark, read-only chat display. */
    public static void styleChatArea(JTextArea ta) {
        ta.setEditable(false);
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setFont(FONT_MONO);
        ta.setBackground(BG_BASE);
        ta.setForeground(TEXT_HI);
        ta.setBorder(new EmptyBorder(12, 14, 12, 14));
        ta.setSelectionColor(new Color(100, 180, 255, 50));
    }
}
