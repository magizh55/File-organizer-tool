import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class FileOrganizerGUI extends JFrame {

    // ── Palette ────────────────────────────────────────────────────────────
    private static final Color BG         = new Color(0x0D0F14);
    private static final Color PANEL      = new Color(0x161A23);
    private static final Color CARD       = new Color(0x1E2433);
    private static final Color ACCENT     = new Color(0x4F8EF7);
    private static final Color ACCENT2    = new Color(0x7C5CFC);
    private static final Color SUCCESS    = new Color(0x3ECFA0);
    private static final Color WARN       = new Color(0xF7C948);
    private static final Color DANGER     = new Color(0xF75F5F);
    private static final Color TEXT       = new Color(0xE8EAF0);
    private static final Color TEXT_DIM   = new Color(0x7A8299);
    private static final Color BORDER     = new Color(0x252C3D);

    // ── Category map ───────────────────────────────────────────────────────
    private static final Map<String, String[]> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("🖼  Images",    new String[]{"jpg","jpeg","png","gif","bmp","svg","webp","ico","tiff","raw","heic"});
        CATEGORIES.put("📄 Documents",  new String[]{"pdf","doc","docx","xls","xlsx","ppt","pptx","txt","odt","rtf","csv","md"});
        CATEGORIES.put("🎵 Audio",      new String[]{"mp3","wav","flac","aac","ogg","wma","m4a","opus","aiff"});
        CATEGORIES.put("🎬 Videos",     new String[]{"mp4","avi","mkv","mov","wmv","flv","webm","m4v","3gp","ts"});
        CATEGORIES.put("📦 Archives",   new String[]{"zip","rar","tar","gz","7z","bz2","xz","cab","iso"});
        CATEGORIES.put("💻 Code",       new String[]{"java","py","js","ts","html","css","cpp","c","cs","go","rb","php","sh","json","xml","yaml","sql"});
        CATEGORIES.put("🔧 Others",     new String[]{});
    }

    // ── UI Components ──────────────────────────────────────────────────────
    private JTextField pathField;
    private JButton browseBtn, organizeBtn, clearBtn;
    private JTextArea logArea;
    private JLabel statusLabel, fileCountLabel, dirLabel;
    private JProgressBar progressBar;
    private StatPanel[] statPanels;
    private int[] statCounts;
    private String selectedPath = "";

    // ══════════════════════════════════════════════════════════════════════
    public FileOrganizerGUI() {
        setTitle("File Organizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 680);
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(null);
        setBackground(BG);
        buildUI();
        setVisible(true);
    }

    // ── UI Builder ─────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(PANEL);
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        h.setPreferredSize(new Dimension(0, 64));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        left.setOpaque(false);

        JLabel icon = new JLabel("⚡");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        icon.setForeground(ACCENT);

        JLabel title = new JLabel("File Organizer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT);

        JLabel sub = new JLabel("— sort your chaos");
        sub.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        sub.setForeground(TEXT_DIM);

        left.add(icon); left.add(title); left.add(sub);
        h.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        right.setOpaque(false);
        dirLabel = new JLabel("No folder selected");
        dirLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dirLabel.setForeground(TEXT_DIM);
        right.add(dirLabel);
        h.add(right, BorderLayout.EAST);

        // vertical-center
        for (Component c : new Component[]{left, right})
            ((JPanel)c).setPreferredSize(new Dimension(((JPanel)c).getPreferredSize().width, 64));

        return h;
    }

    private JPanel buildCenter() {
        JPanel c = new JPanel(new BorderLayout(0, 16));
        c.setBackground(BG);
        c.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));

        c.add(buildPathRow(),   BorderLayout.NORTH);
        c.add(buildMainBody(), BorderLayout.CENTER);
        return c;
    }

    private JPanel buildPathRow() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, 42));

        pathField = new JTextField();
        styleTextField(pathField, "Paste or browse a folder path…");

        browseBtn   = buildBtn("Browse",   ACCENT,  TEXT);
        organizeBtn = buildBtn("Organize ▶", SUCCESS, BG);
        clearBtn    = buildBtn("Clear",    CARD,    TEXT_DIM);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(clearBtn); btns.add(browseBtn); btns.add(organizeBtn);

        p.add(pathField, BorderLayout.CENTER);
        p.add(btns,      BorderLayout.EAST);

        // Listeners
        browseBtn.addActionListener(e -> browse());
        organizeBtn.addActionListener(e -> organize());
        clearBtn.addActionListener(e -> clearAll());
        pathField.addActionListener(e -> organize());

        return p;
    }

    private JPanel buildMainBody() {
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);

        // LEFT — stats panel
        body.add(buildStatsColumn(), BorderLayout.WEST);

        // RIGHT — log
        body.add(buildLogPanel(), BorderLayout.CENTER);

        return body;
    }

    private JPanel buildStatsColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);
        col.setPreferredSize(new Dimension(200, 0));

        JLabel lbl = new JLabel("CATEGORIES");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(TEXT_DIM);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 2, 10, 0));
        col.add(lbl);

        String[] cats = CATEGORIES.keySet().toArray(new String[0]);
        Color[] colors = {ACCENT, new Color(0x5BC4F7), SUCCESS, WARN, DANGER, ACCENT2, TEXT_DIM};
        statPanels = new StatPanel[cats.length];
        statCounts = new int[cats.length];

        for (int i = 0; i < cats.length; i++) {
            statPanels[i] = new StatPanel(cats[i], colors[i % colors.length]);
            statPanels[i].setAlignmentX(LEFT_ALIGNMENT);
            col.add(statPanels[i]);
            col.add(Box.createVerticalStrut(8));
        }

        col.add(Box.createVerticalGlue());

        fileCountLabel = new JLabel("— files found");
        fileCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        fileCountLabel.setForeground(TEXT_DIM);
        fileCountLabel.setAlignmentX(LEFT_ALIGNMENT);
        col.add(fileCountLabel);

        return col;
    }

    private JPanel buildLogPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);

        JLabel lbl = new JLabel("ACTIVITY LOG");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(TEXT_DIM);
        p.add(lbl, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        logArea.setBackground(CARD);
        logArea.setForeground(TEXT);
        logArea.setCaretColor(ACCENT);
        logArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.setBackground(CARD);
        scroll.getViewport().setBackground(CARD);
        scroll.getVerticalScrollBar().setBackground(CARD);
        p.add(scroll, BorderLayout.CENTER);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setBackground(CARD);
        progressBar.setForeground(ACCENT);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 4));
        p.add(progressBar, BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildFooter() {
        JPanel f = new JPanel(new BorderLayout());
        f.setBackground(PANEL);
        f.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        f.setPreferredSize(new Dimension(0, 34));

        statusLabel = new JLabel("  Ready — select a folder to get started");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_DIM);
        f.add(statusLabel, BorderLayout.WEST);

        JLabel credit = new JLabel("File Organizer v2.0  ");
        credit.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        credit.setForeground(new Color(0x3A4055));
        f.add(credit, BorderLayout.EAST);

        return f;
    }

    // ── Actions ────────────────────────────────────────────────────────────
    private void browse() {
        JFileChooser fc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Select Folder to Organize");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedPath = fc.getSelectedFile().getAbsolutePath();
            pathField.setText(selectedPath);
            dirLabel.setText(fc.getSelectedFile().getName());
            setStatus("Folder selected: " + selectedPath, TEXT_DIM);
            previewFiles(fc.getSelectedFile());
        }
    }

    private void previewFiles(File dir) {
        File[] files = dir.listFiles(f -> f.isFile());
        if (files == null) return;
        fileCountLabel.setText(files.length + " file" + (files.length != 1 ? "s" : "") + " found");
        Arrays.fill(statCounts, 0);
        String[] cats = CATEGORIES.keySet().toArray(new String[0]);
        for (File f : files) {
            String ext = getExt(f.getName());
            boolean matched = false;
            for (int i = 0; i < cats.length - 1; i++) {
                for (String e : CATEGORIES.get(cats[i])) {
                    if (e.equalsIgnoreCase(ext)) { statCounts[i]++; matched = true; break; }
                }
                if (matched) break;
            }
            if (!matched) statCounts[cats.length - 1]++;
        }
        for (int i = 0; i < statPanels.length; i++) statPanels[i].setCount(statCounts[i]);
    }

    private void organize() {
        String path = pathField.getText().trim();
        if (path.isEmpty()) { warn("Please enter or browse a folder path."); return; }
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) { warn("Invalid directory path."); return; }

        organizeBtn.setEnabled(false);
        browseBtn.setEnabled(false);
        progressBar.setValue(0);
        logArea.setText("");
        Arrays.fill(statCounts, 0);
        for (StatPanel sp : statPanels) sp.setCount(0);

        new Thread(() -> {
            try {
                File[] files = dir.listFiles(File::isFile);
                if (files == null || files.length == 0) {
                    swingRun(() -> { log("⚠  No files found in the selected directory.", WARN); setStatus("Nothing to organize.", WARN); });
                    return;
                }

                int total = files.length, done = 0, moved = 0, skipped = 0;
                String[] cats = CATEGORIES.keySet().toArray(new String[0]);
                String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());

                swingRun(() -> {
                    log("──────────────────────────────────────────", TEXT_DIM);
                    log("  Started at " + ts + " · " + total + " files", ACCENT);
                    log("──────────────────────────────────────────", TEXT_DIM);
                });

                for (File f : files) {
                    String ext  = getExt(f.getName());
                    String cat  = resolveCategory(ext, cats);
                    String fold = cat.replaceAll("[^a-zA-Z ]", "").trim();

                    File destDir = new File(dir, fold);
                    if (!destDir.exists()) destDir.mkdirs();

                    File dest = new File(destDir, f.getName());
                    if (dest.exists()) dest = new File(destDir, dedup(f.getName(), destDir));

                    final String catFinal = cat;
                    String logLine;
                    Color  logColor;
                    boolean ok = false;
                    try {
                        Files.move(f.toPath(), dest.toPath(), StandardCopyOption.ATOMIC_MOVE);
                        ok = true; moved++;
                        logLine  = "  ✓  " + f.getName() + "  →  " + fold + "/";
                        logColor = SUCCESS;
                        // bump stat
                        for (int i = 0; i < cats.length; i++) if (cats[i].equals(catFinal)) { statCounts[i]++; break; }
                    } catch (IOException ex) {
                        skipped++;
                        logLine  = "  ✗  " + f.getName() + "  (skipped: " + ex.getMessage() + ")";
                        logColor = DANGER;
                    }

                    final String finalLogLine = logLine;
                    final Color finalLogColor = logColor;

                    done++;
                    final int pct = (int)((done / (double)total) * 100);
                    final int doneF = done, movedF = moved;
                    final int[] sc = statCounts.clone();
                    swingRun(() -> {
                        log(finalLogLine, finalLogColor);
                        progressBar.setValue(pct);
                        setStatus("Processing… " + doneF + "/" + total, ACCENT);
                        for (int i = 0; i < statPanels.length; i++) statPanels[i].setCount(sc[i]);
                    });
                    Thread.sleep(18); // smooth animation
                }

                final int mv = moved, sk = skipped;
                swingRun(() -> {
                    log("──────────────────────────────────────────", TEXT_DIM);
                    log("  Done!  Moved: " + mv + "   Skipped: " + sk, SUCCESS);
                    log("──────────────────────────────────────────", TEXT_DIM);
                    progressBar.setValue(100);
                    setStatus("✔  " + mv + " files organized successfully!", SUCCESS);
                    fileCountLabel.setText(mv + " files moved");
                });
            } catch (Exception ex) {
                swingRun(() -> { log("ERROR: " + ex.getMessage(), DANGER); setStatus("Error occurred.", DANGER); });
            } finally {
                swingRun(() -> { organizeBtn.setEnabled(true); browseBtn.setEnabled(true); });
            }
        }).start();
    }

    private void clearAll() {
        logArea.setText("");
        pathField.setText("");
        selectedPath = "";
        progressBar.setValue(0);
        dirLabel.setText("No folder selected");
        fileCountLabel.setText("— files found");
        for (StatPanel sp : statPanels) sp.setCount(0);
        setStatus("Cleared — ready for a new folder.", TEXT_DIM);
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private String resolveCategory(String ext, String[] cats) {
        for (int i = 0; i < cats.length - 1; i++)
            for (String e : CATEGORIES.get(cats[i]))
                if (e.equalsIgnoreCase(ext)) return cats[i];
        return cats[cats.length - 1];
    }

    private String getExt(String name) {
        int i = name.lastIndexOf('.');
        return (i >= 0 && i < name.length() - 1) ? name.substring(i + 1) : "";
    }

    private String dedup(String name, File dir) {
        int dot = name.lastIndexOf('.');
        String base = dot >= 0 ? name.substring(0, dot) : name;
        String ext  = dot >= 0 ? name.substring(dot) : "";
        int n = 1;
        String candidate;
        do { candidate = base + "_" + n++ + ext; } while (new File(dir, candidate).exists());
        return candidate;
    }

    private void log(String msg, Color c) {
        // SimpleAttributeSet won't work with plain JTextArea, use append + color via HTML trick
        // We store as plain text; color hints via prefix characters only.
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void setStatus(String msg, Color c) {
        statusLabel.setText("  " + msg);
        statusLabel.setForeground(c);
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Attention", JOptionPane.WARNING_MESSAGE);
    }

    private void swingRun(Runnable r) { SwingUtilities.invokeLater(r); }

    // ── Style Helpers ──────────────────────────────────────────────────────
    private void styleTextField(JTextField tf, String placeholder) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBackground(CARD);
        tf.setForeground(TEXT);
        tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        tf.setPreferredSize(new Dimension(0, 42));
        tf.setText(placeholder);
        tf.setForeground(TEXT_DIM);
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { if (tf.getText().equals(placeholder)) { tf.setText(""); tf.setForeground(TEXT); } }
            public void focusLost(FocusEvent e)   { if (tf.getText().isEmpty()) { tf.setText(placeholder); tf.setForeground(TEXT_DIM); } }
        });
    }

    private JButton buildBtn(String label, Color bg, Color fg) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(fg);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(label.length() * 9 + 20, 38));
        b.setFocusPainted(false);
        return b;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Inner class: stat row card
    // ══════════════════════════════════════════════════════════════════════
    static class StatPanel extends JPanel {
        private final JLabel nameLbl, cntLbl;
        private final Color dot;

        StatPanel(String name, Color dot) {
            this.dot = dot;
            setLayout(new BorderLayout(8, 0));
            setBackground(CARD);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

            nameLbl = new JLabel(name);
            nameLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            nameLbl.setForeground(TEXT);

            cntLbl = new JLabel("0");
            cntLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            cntLbl.setForeground(dot);

            add(nameLbl, BorderLayout.CENTER);
            add(cntLbl,  BorderLayout.EAST);
        }

        void setCount(int n) {
            cntLbl.setText(String.valueOf(n));
            cntLbl.setForeground(n > 0 ? dot : TEXT_DIM);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(FileOrganizerGUI::new);
    }
}
