package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import server.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatClientGUI extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────
    private static final Color C_BG           = new Color(18,  18,  30);
    private static final Color C_HEADER       = new Color(25,  25,  40);
    private static final Color C_BUBBLE_SELF  = new Color(59, 130, 246);
    private static final Color C_BUBBLE_OTHER = new Color(40,  40,  60);
    private static final Color C_INPUT_BG     = new Color(30,  30,  48);
    private static final Color C_INPUT_BORDER = new Color(70,  70, 100);
    private static final Color C_TEXT         = new Color(230, 230, 240);
    private static final Color C_SUBTEXT      = new Color(140, 140, 165);
    private static final Color C_ONLINE       = new Color(52, 211, 153);
    private static final Color C_SEND_BTN     = new Color(59, 130, 246);
    private static final Color C_SEND_HOVER   = new Color(37, 100, 210);

    // Palette de couleurs pour les avatars des autres participants
    private static final Color[] AVATAR_COLORS = {
        new Color(239, 68,  68),  new Color(249, 115, 22),
        new Color(234, 179,  8),  new Color(34, 197,  94),
        new Color(20, 184, 166),  new Color(168, 85, 247)
    };

    private static final Font FONT_UI     = new Font("Segoe UI", Font.PLAIN,  14);
    private static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD,   14);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN,  11);
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,   16);

    private static final String SERVER_URL        = "http://localhost:8080/messages";
    private static final java.lang.reflect.Type   MESSAGE_LIST_TYPE =
            new TypeToken<List<Message>>(){}.getType();

    // ── État ──────────────────────────────────────────────────────────────
    private final String pseudo;
    private final Gson   gson = new Gson();
    private int lastCount = 0;
    private final Map<String, Color> avatarColorMap = new HashMap<>();
    private int colorIndex = 0;

    // ── Composants ────────────────────────────────────────────────────────
    private JPanel    messagesPanel;
    private JScrollPane scrollPane;
    private JTextArea inputField;
    private JLabel    statusDot;
    private JLabel    statusLabel;

    // ─────────────────────────────────────────────────────────────────────

    public ChatClientGUI(String pseudo) {
        this.pseudo = pseudo;
        buildUI();
        startPoller();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Construction de l'interface
    // ══════════════════════════════════════════════════════════════════════

    private void buildUI() {
        setTitle("REST Chat");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 600);
        setMinimumSize(new Dimension(500, 420));
        setLocationRelativeTo(null);
        setBackground(C_BG);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);

        root.add(buildHeader(),    BorderLayout.NORTH);
        root.add(buildMessages(),  BorderLayout.CENTER);
        root.add(buildInputBar(),  BorderLayout.SOUTH);

        setContentPane(root);
        setVisible(true);
    }

    // ── En-tête ───────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(C_HEADER);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // ligne de séparation basse
                g2.setColor(new Color(50, 50, 75));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        // Côté gauche : avatar + infos
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        left.add(buildAvatar(pseudo, C_SEND_BTN, 42));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLabel = new JLabel(pseudo);
        nameLabel.setFont(FONT_BOLD);
        nameLabel.setForeground(C_TEXT);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        statusRow.setOpaque(false);

        statusDot = new JLabel("●");
        statusDot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        statusDot.setForeground(C_ONLINE);

        statusLabel = new JLabel("En ligne");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(C_SUBTEXT);

        statusRow.add(statusDot);
        statusRow.add(statusLabel);

        info.add(nameLabel);
        info.add(statusRow);
        left.add(info);

        // Côté droit : titre de l'app
        JLabel appTitle = new JLabel("REST Chat");
        appTitle.setFont(FONT_TITLE);
        appTitle.setForeground(new Color(100, 160, 255));

        header.add(left,      BorderLayout.WEST);
        header.add(appTitle,  BorderLayout.EAST);
        return header;
    }

    // ── Zone de messages ──────────────────────────────────────────────────

    private JScrollPane buildMessages() {
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(C_BG);
        messagesPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(C_BG);
        scrollPane.getViewport().setBackground(C_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Scrollbar discrète
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scrollPane.getVerticalScrollBar().setBackground(C_BG);

        return scrollPane;
    }

    // ── Barre d'envoi ─────────────────────────────────────────────────────

    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(22, 22, 36));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(50, 50, 75));
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Champ de texte multi-ligne avec scroll
        inputField = new JTextArea(2, 20) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_INPUT_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        inputField.setFont(FONT_UI);
        inputField.setForeground(C_TEXT);
        inputField.setBackground(new Color(0, 0, 0, 0));
        inputField.setCaretColor(C_TEXT);
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);
        inputField.setOpaque(false);
        inputField.setBorder(new EmptyBorder(8, 14, 8, 14));
        inputField.setRows(1);

        // Placeholder
        inputField.setText("Écrire un message…");
        inputField.setForeground(C_SUBTEXT);
        inputField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (inputField.getText().equals("Écrire un message…")) {
                    inputField.setText("");
                    inputField.setForeground(C_TEXT);
                }
            }
            public void focusLost(FocusEvent e) {
                if (inputField.getText().isEmpty()) {
                    inputField.setText("Écrire un message…");
                    inputField.setForeground(C_SUBTEXT);
                }
            }
        });

        // Envoi sur Entrée (Shift+Entrée = saut de ligne)
        inputField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });

        JScrollPane inputScroll = new JScrollPane(inputField);
        inputScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_INPUT_BORDER, 1, true),
            BorderFactory.createEmptyBorder()
        ));
        inputScroll.setBackground(C_INPUT_BG);
        inputScroll.getViewport().setBackground(C_INPUT_BG);
        inputScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.setPreferredSize(new Dimension(0, 48));

        // Bouton Envoyer
        JButton sendBtn = new JButton("Envoyer ➤") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? C_SEND_HOVER : C_SEND_BTN);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 22, 22));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        sendBtn.setFont(FONT_BOLD);
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setContentAreaFilled(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setFocusPainted(false);
        sendBtn.setBorder(new EmptyBorder(10, 20, 10, 20));
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendMessage());

        bar.add(inputScroll, BorderLayout.CENTER);
        bar.add(sendBtn,     BorderLayout.EAST);
        return bar;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Bulles de messages
    // ══════════════════════════════════════════════════════════════════════

    private void addMessageBubble(String from, String text, boolean isSelf) {
        JPanel row = new JPanel(new FlowLayout(
            isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT, 8, 4
        ));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        Color avatarColor = isSelf ? C_SEND_BTN : getAvatarColor(from);

        if (!isSelf) {
            row.add(buildAvatar(from, avatarColor, 36));
        }

        // Bulle
        JPanel bubble = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelf ? C_BUBBLE_SELF : C_BUBBLE_OTHER);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
            }
        };
        bubble.setOpaque(false);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Nom de l'expéditeur (seulement pour les autres)
        if (!isSelf) {
            JLabel nameLabel = new JLabel(from);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            nameLabel.setForeground(avatarColor);
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(nameLabel);
            bubble.add(Box.createVerticalStrut(3));
        }

        // Texte du message (avec retour à la ligne)
        JTextArea msgText = new JTextArea(text) {
            @Override public boolean isFocusable() { return false; }
        };
        msgText.setFont(FONT_UI);
        msgText.setForeground(C_TEXT);
        msgText.setOpaque(false);
        msgText.setEditable(false);
        msgText.setLineWrap(true);
        msgText.setWrapStyleWord(true);
        msgText.setBorder(null);
        msgText.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Largeur max de la bulle = 60% de la fenêtre
        msgText.setMaximumSize(new Dimension((int)(getWidth() * 0.60), Integer.MAX_VALUE));
        msgText.setPreferredSize(null);

        bubble.add(msgText);
        bubble.add(Box.createVerticalStrut(4));

        // Heure
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(FONT_SMALL);
        timeLabel.setForeground(isSelf ? new Color(200, 220, 255) : C_SUBTEXT);
        timeLabel.setAlignmentX(isSelf ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        bubble.add(timeLabel);

        // Contrainte de largeur max
        bubble.setMaximumSize(new Dimension((int)(getWidth() * 0.65), Integer.MAX_VALUE));

        row.add(bubble);

        if (isSelf) {
            row.add(buildAvatar(pseudo, avatarColor, 36));
        }

        messagesPanel.add(row);
        messagesPanel.add(Box.createVerticalStrut(4));
        messagesPanel.revalidate();
        scrollToBottom();
    }

    private void addSystemMessage(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row.setOpaque(false);
        JLabel label = new JLabel(text);
        label.setFont(FONT_SMALL);
        label.setForeground(C_SUBTEXT);
        row.add(label);
        messagesPanel.add(row);
        messagesPanel.revalidate();
        scrollToBottom();
    }

    // ── Avatar avec initiales ─────────────────────────────────────────────

    private JLabel buildAvatar(String name, Color color, int size) {
        String initials = name.isEmpty() ? "?" :
            String.valueOf(name.charAt(0)).toUpperCase();

        JLabel avatar = new JLabel(initials, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setFont(new Font("Segoe UI", Font.BOLD, size / 3));
        avatar.setForeground(Color.WHITE);
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(size, size));
        avatar.setMinimumSize(new Dimension(size, size));
        avatar.setMaximumSize(new Dimension(size, size));
        return avatar;
    }

    // ── Couleur d'avatar par pseudo ───────────────────────────────────────

    private Color getAvatarColor(String name) {
        return avatarColorMap.computeIfAbsent(name, k -> {
            Color c = AVATAR_COLORS[colorIndex % AVATAR_COLORS.length];
            colorIndex++;
            return c;
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Réseau
    // ══════════════════════════════════════════════════════════════════════

    private void sendMessage() {
        String texte = inputField.getText().trim();
        if (texte.isEmpty() || texte.equals("Écrire un message…")) return;

        inputField.setText("");
        inputField.setForeground(C_TEXT);

        new Thread(() -> {
            try {
                byte[] body = gson.toJson(new Message(pseudo, texte)).getBytes("UTF-8");
                URL url = new URL(SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Content-Length", String.valueOf(body.length));
                conn.getOutputStream().write(body);
                conn.getOutputStream().close();
                conn.getInputStream().close();
                conn.disconnect();

                lastCount++;
                SwingUtilities.invokeLater(() -> addMessageBubble(pseudo, texte, true));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    addSystemMessage("⚠ Erreur d'envoi : " + ex.getMessage()));
            }
        }).start();
    }

    private void startPoller() {
        Thread poller = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        URL url = new URL(SERVER_URL);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(3000);
                        conn.setReadTimeout(3000);

                        BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8")
                        );
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();
                        conn.disconnect();

                        List<Message> messages = gson.fromJson(sb.toString(), MESSAGE_LIST_TYPE);
                        if (messages != null && messages.size() > lastCount) {
                            final int from = lastCount;
                            final List<Message> snap = messages;
                            lastCount = messages.size();

                            SwingUtilities.invokeLater(() -> {
                                for (int i = from; i < snap.size(); i++) {
                                    Message m = snap.get(i);
                                    if (!m.getPseudo().equals(pseudo)) {
                                        addMessageBubble(m.getPseudo(), m.getContenu(), false);
                                        // Faire clignoter la barre de titre si fenêtre inactive
                                        if (!isFocused()) {
                                            setTitle("● REST Chat — nouveau message");
                                        }
                                    }
                                }
                            });
                        }

                        // Indicateur de connexion : vert
                        SwingUtilities.invokeLater(() -> {
                            statusDot.setForeground(C_ONLINE);
                            statusLabel.setText("En ligne");
                        });

                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> {
                            statusDot.setForeground(new Color(239, 68, 68));
                            statusLabel.setText("Connexion perdue…");
                        });
                    }

                    try { Thread.sleep(2000); }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
        poller.setDaemon(true);
        poller.start();

        // Réinitialise le titre quand la fenêtre reprend le focus
        addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent e) { setTitle("REST Chat"); }
            public void windowLostFocus(WindowEvent e)   {}
        });
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Point d'entrée
    // ══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        String pseudo = showLoginDialog();
        if (pseudo == null || pseudo.isEmpty()) System.exit(0);

        final String p = pseudo;
        SwingUtilities.invokeLater(() -> new ChatClientGUI(p));
    }

    private static String showLoginDialog() {
        final String[] result = {null};

        JDialog dialog = new JDialog((Frame) null, "REST Chat — Connexion", true);
        dialog.setUndecorated(false);
        dialog.setSize(380, 300);
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);

        // Panneau principal avec fond sombre
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(20, 20, 35));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(30, 36, 24, 36));

        // ── Logo / titre ─────────────────────────────────────────────
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("💬", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLbl = new JLabel("REST Chat");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLbl.setForeground(new Color(100, 160, 255));
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel("Entrez votre pseudo pour rejoindre");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(new Color(130, 130, 160));
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(logo);
        topPanel.add(Box.createVerticalStrut(6));
        topPanel.add(titleLbl);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(subLbl);

        // ── Champ pseudo ─────────────────────────────────────────────
        JTextField pseudoField = new JTextField();
        pseudoField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        pseudoField.setBackground(new Color(35, 35, 55));
        pseudoField.setForeground(Color.WHITE);
        pseudoField.setCaretColor(Color.WHITE);
        pseudoField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 110), 1),
            new EmptyBorder(10, 14, 10, 14)
        ));

        // ── Boutons ───────────────────────────────────────────────────
        JButton joinBtn = new JButton("Rejoindre") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(37, 100, 210) : new Color(59, 130, 246));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        joinBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        joinBtn.setForeground(Color.WHITE);
        joinBtn.setContentAreaFilled(false);
        joinBtn.setBorderPainted(false);
        joinBtn.setFocusPainted(false);
        joinBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        joinBtn.setPreferredSize(new Dimension(0, 42));

        JButton cancelBtn = new JButton("Annuler") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(60, 60, 85) : new Color(45, 45, 65));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelBtn.setForeground(new Color(180, 180, 200));
        cancelBtn.setContentAreaFilled(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.setPreferredSize(new Dimension(0, 42));

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(cancelBtn);
        btnPanel.add(joinBtn);

        // ── Centre ────────────────────────────────────────────────────
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(Box.createVerticalStrut(18));
        pseudoField.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(pseudoField);
        center.add(Box.createVerticalStrut(16));
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(btnPanel);

        root.add(topPanel, BorderLayout.NORTH);
        root.add(center,   BorderLayout.CENTER);
        dialog.setContentPane(root);

        // ── Actions ───────────────────────────────────────────────────
        ActionListener confirm = e -> {
            String txt = pseudoField.getText().trim();
            if (!txt.isEmpty()) {
                result[0] = txt;
                dialog.dispose();
            } else {
                pseudoField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(239, 68, 68), 1),
                    new EmptyBorder(10, 14, 10, 14)
                ));
            }
        };
        joinBtn.addActionListener(confirm);
        pseudoField.addActionListener(confirm);
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
        return result[0];
    }
}
