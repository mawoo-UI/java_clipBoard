package study2;


import com.formdev.flatlaf.*;
import com.formdev.flatlaf.intellijthemes.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UltimateClipboardManager extends JFrame {
    // 일반 복사 히스토리
    private Queue<ClipboardItem> copyHistory;
    private JList<ClipboardItem> copyList;
    private DefaultListModel<ClipboardItem> copyListModel;
    
    // 잘라내기 히스토리
    private Queue<CutItem> cutHistory;
    private JList<CutItem> cutList;
    private DefaultListModel<CutItem> cutListModel;
    
    private Clipboard systemClipboard;
    private Timer clipboardMonitor;
    private String lastClipboardContent = "";
    private boolean isMonitoringCut = false;
    
    // UI 컴포넌트들
    private JTabbedPane mainTabbedPane;
    private JComboBox<ThemeInfo> themeComboBox;
    
    // 테마 정보
    private static final ThemeInfo[] AVAILABLE_THEMES = {
        new ThemeInfo("FlatLaf Light", "com.formdev.flatlaf.FlatLightLaf"),
        new ThemeInfo("FlatLaf Dark", "com.formdev.flatlaf.FlatDarkLaf"),
        new ThemeInfo("FlatLaf IntelliJ", "com.formdev.flatlaf.FlatIntelliJLaf"),
        new ThemeInfo("FlatLaf Darcula", "com.formdev.flatlaf.FlatDarculaLaf"),
        new ThemeInfo("Arc Orange", "com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme"),
        new ThemeInfo("Dracula", "com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme"),
        new ThemeInfo("Nord", "com.formdev.flatlaf.intellijthemes.FlatNordIJTheme"),
        new ThemeInfo("One Dark", "com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme"),
        new ThemeInfo("Solarized Dark", "com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme")
    };
    
    public UltimateClipboardManager() {
        copyHistory = new ConcurrentLinkedQueue<>();
        cutHistory = new ConcurrentLinkedQueue<>();
        copyListModel = new DefaultListModel<>();
        cutListModel = new DefaultListModel<>();
        systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        
        initializeGUI();
        startClipboardMonitoring();
        setupKeyBindings();
        
        setVisible(false);
    }
    
    private void initializeGUI() {
        setTitle("🚀 궁극의 클립보드 매니저");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        
        mainTabbedPane = new JTabbedPane();
        
        // 📋 일반 복사 탭
        JPanel copyPanel = createCopyPanel();
        mainTabbedPane.addTab("📋 복사 (Ctrl+C)", copyPanel);
        
        // ✂️ 잘라내기 탭
        JPanel cutPanel = createCutPanel();
        mainTabbedPane.addTab("✂️ 잘라내기 (Ctrl+X)", cutPanel);
        
        // 🎨 테마 탭
        JPanel themePanel = createThemePanel();
        mainTabbedPane.addTab("🎨 테마", themePanel);
        
        // ⚙️ 설정 탭
        JPanel settingsPanel = createSettingsPanel();
        mainTabbedPane.addTab("⚙️ 설정", settingsPanel);
        
        add(mainTabbedPane);
        setupKeyBindings();
    }
    
    private JPanel createCopyPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 상단 컨트롤
        JPanel controlPanel = new JPanel(new FlowLayout());
        
        JButton pasteSelectedBtn = new JButton("📄 선택 항목 붙여넣기");
        pasteSelectedBtn.addActionListener(e -> pasteSelectedCopyItems());
        
        JButton pasteAllBtn = new JButton("📚 전체 붙여넣기");
        pasteAllBtn.addActionListener(e -> pasteAllCopyItems());
        
        JButton clearBtn = new JButton("🗑️ 복사 히스토리 지우기");
        clearBtn.addActionListener(e -> clearCopyHistory());
        
        controlPanel.add(pasteSelectedBtn);
        controlPanel.add(pasteAllBtn);
        controlPanel.add(clearBtn);
        
        // 복사 히스토리 리스트
        copyList = new JList<>(copyListModel);
        copyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        copyList.setCellRenderer(new ClipboardItemRenderer());
        
        // 더블클릭으로 붙여넣기
        copyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    pasteSelectedCopyItems();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(copyList);
        scrollPane.setBorder(new TitledBorder("📋 복사 히스토리 (다중 선택: Ctrl+클릭)"));
        
        // 하단 상태
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel statusLabel = new JLabel("💡 Ctrl+C로 복사하면 자동으로 히스토리에 추가됩니다");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));
        statusPanel.add(statusLabel);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(statusPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createCutPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 상단 컨트롤
        JPanel controlPanel = new JPanel(new FlowLayout());
        
        JButton pasteSelectedBtn = new JButton("📄 선택 항목 붙여넣기");
        pasteSelectedBtn.addActionListener(e -> pasteSelectedCutItems());
        
        JButton restoreBtn = new JButton("🔄 되돌리기 (복원)");
        restoreBtn.addActionListener(e -> restoreSelectedCutItems());
        restoreBtn.setToolTipText("잘라낸 텍스트를 원래 위치로 되돌립니다");
        
        JButton deleteBtn = new JButton("🗑️ 완전 삭제");
        deleteBtn.addActionListener(e -> permanentlyDeleteCutItems());
        deleteBtn.setForeground(Color.RED);
        
        JButton clearAllBtn = new JButton("🧹 모든 잘라내기 히스토리 지우기");
        clearAllBtn.addActionListener(e -> clearCutHistory());
        
        controlPanel.add(pasteSelectedBtn);
        controlPanel.add(restoreBtn);
        controlPanel.add(deleteBtn);
        controlPanel.add(clearAllBtn);
        
        // 잘라내기 히스토리 리스트
        cutList = new JList<>(cutListModel);
        cutList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        cutList.setCellRenderer(new CutItemRenderer());
        
        // 더블클릭으로 붙여넣기
        cutList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    pasteSelectedCutItems();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(cutList);
        scrollPane.setBorder(new TitledBorder("✂️ 잘라내기 히스토리 (다중 선택: Ctrl+클릭)"));
        
        // 하단 정보
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(new TitledBorder("💡 잘라내기 기능 설명"));
        
        String[] info = {
            "🔄 되돌리기: 잘라낸 텍스트를 다시 클립보드에 복사 (원래 위치 복원)",
            "📄 붙여넣기: 선택한 잘라낸 텍스트를 현재 위치에 붙여넣기",
            "🗑️ 완전 삭제: 잘라낸 텍스트를 영구적으로 삭제",
            "⚠️ 주의: 되돌리기는 텍스트만 가능하며, 원래 애플리케이션에서 직접 붙여넣어야 합니다"
        };
        
        for (String text : info) {
            JLabel label = new JLabel(text);
            label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoPanel.add(label);
        }
        
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(infoPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createThemePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel themeControlPanel = new JPanel(new FlowLayout());
        
        JLabel themeLabel = new JLabel("🎨 테마 선택:");
        themeComboBox = new JComboBox<>(AVAILABLE_THEMES);
        themeComboBox.setRenderer(new ThemeComboBoxRenderer());
        themeComboBox.addActionListener(e -> applySelectedTheme());
        
        JButton applyBtn = new JButton("✨ 적용");
        applyBtn.addActionListener(e -> applySelectedTheme());
        
        JButton resetBtn = new JButton("🔄 기본값");
        resetBtn.addActionListener(e -> resetToDefaultTheme());
        
        themeControlPanel.add(themeLabel);
        themeControlPanel.add(themeComboBox);
        themeControlPanel.add(applyBtn);
        themeControlPanel.add(resetBtn);
        
        JPanel previewPanel = createThemePreviewPanel();
        
        panel.add(themeControlPanel, BorderLayout.NORTH);
        panel.add(previewPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createThemePreviewPanel() {
        JPanel preview = new JPanel();
        preview.setBorder(new TitledBorder("🖼️ 테마 미리보기"));
        preview.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        preview.add(new JLabel("🎨 현재 테마의 모습을 미리 볼 수 있습니다"), gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        preview.add(new JButton("샘플 버튼"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        JTextField sampleField = new JTextField("샘플 텍스트");
        sampleField.setPreferredSize(new Dimension(150, 25));
        preview.add(sampleField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JCheckBox sampleCheck = new JCheckBox("샘플 체크박스", true);
        preview.add(sampleCheck, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JProgressBar sampleProgress = new JProgressBar(0, 100);
        sampleProgress.setValue(75);
        sampleProgress.setStringPainted(true);
        sampleProgress.setString("진행률 75%");
        preview.add(sampleProgress, gbc);
        
        return preview;
    }
    
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JCheckBox alwaysOnTopCheck = new JCheckBox("항상 위에 표시", true);
        alwaysOnTopCheck.addActionListener(e -> setAlwaysOnTop(alwaysOnTopCheck.isSelected()));
        
        JCheckBox autoHideCheck = new JCheckBox("붙여넣기 후 자동 숨김", true);
        
        JPanel maxItemsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        maxItemsPanel.add(new JLabel("최대 히스토리 개수:"));
        JSpinner maxItemsSpinner = new JSpinner(new SpinnerNumberModel(50, 10, 500, 10));
        maxItemsPanel.add(maxItemsSpinner);
        
        panel.add(alwaysOnTopCheck);
        panel.add(Box.createVerticalStrut(10));
        panel.add(autoHideCheck);
        panel.add(Box.createVerticalStrut(10));
        panel.add(maxItemsPanel);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private void setupKeyBindings() {
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "hideWindow");
        getRootPane().getActionMap().put("hideWindow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleVisibility();
            }
        });
    }
    
    private void startClipboardMonitoring() {
        clipboardMonitor = new Timer(300, e -> checkClipboard());
        clipboardMonitor.start();
    }
    
    private void checkClipboard() {
        try {
            if (systemClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String currentContent = (String) systemClipboard.getData(DataFlavor.stringFlavor);
                
                if (currentContent != null && !currentContent.equals(lastClipboardContent)) {
                    // 잘라내기인지 복사인지 구분 (단순화된 방법)
                    // 실제로는 더 복잡한 로직이 필요함
                    if (isMonitoringCut) {
                        addToCutHistory(currentContent);
                        isMonitoringCut = false;
                    } else {
                        addToCopyHistory(currentContent);
                    }
                    lastClipboardContent = currentContent;
                }
            }
        } catch (Exception ex) {
            // 클립보드 접근 오류 무시
        }
    }
    
    private void addToCopyHistory(String content) {
        ClipboardItem item = new ClipboardItem(content, LocalDateTime.now(), ClipboardItem.Type.COPY);
        
        // 중복 제거
        copyHistory.removeIf(existing -> existing.content.equals(content));
        copyHistory.offer(item);
        
        // 최대 50개 유지
        while (copyHistory.size() > 50) {
            copyHistory.poll();
        }
        
        SwingUtilities.invokeLater(() -> {
            copyListModel.clear();
            copyHistory.forEach(copyListModel::addElement);
        });
    }
    
    private void addToCutHistory(String content) {
        CutItem item = new CutItem(content, LocalDateTime.now(), null, false);
        
        cutHistory.offer(item);
        
        // 최대 30개 유지
        while (cutHistory.size() > 30) {
            cutHistory.poll();
        }
        
        SwingUtilities.invokeLater(() -> {
            cutListModel.clear();
            cutHistory.forEach(cutListModel::addElement);
        });
        
        showNotification("✂️ 잘라내기 감지: " + truncateText(content, 30));
    }
    
    private void pasteSelectedCopyItems() {
        List<ClipboardItem> selectedValues = copyList.getSelectedValuesList();
        if (selectedValues.isEmpty()) {
            JOptionPane.showMessageDialog(this, "붙여넣을 항목을 선택해주세요!");
            return;
        }
        
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < selectedValues.size(); i++) {
            combined.append(selectedValues.get(i).content);
            if (i < selectedValues.size() - 1) {
                combined.append("\n");
            }
        }
        
        copyToClipboard(combined.toString());
        showNotification("📋 " + selectedValues.size() + "개 복사 항목 클립보드에 복사됨!");
        toggleVisibility();
    }
    
    private void pasteAllCopyItems() {
        if (copyHistory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "복사 히스토리가 비어있습니다!");
            return;
        }
        
        StringBuilder allText = new StringBuilder();
        copyHistory.forEach(item -> allText.append(item.content).append("\n---\n"));
        
        copyToClipboard(allText.toString());
        showNotification("📚 모든 복사 항목(" + copyHistory.size() + "개) 클립보드에 복사됨!");
        toggleVisibility();
    }
    
    private void pasteSelectedCutItems() {
        List<CutItem> selectedValues = cutList.getSelectedValuesList();
        if (selectedValues.isEmpty()) {
            JOptionPane.showMessageDialog(this, "붙여넣을 잘라낸 항목을 선택해주세요!");
            return;
        }
        
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < selectedValues.size(); i++) {
            combined.append(selectedValues.get(i).content);
            if (i < selectedValues.size() - 1) {
                combined.append("\n");
            }
        }
        
        copyToClipboard(combined.toString());
        
        // 사용됨으로 표시
        selectedValues.forEach(item -> item.isUsed = true);
        SwingUtilities.invokeLater(() -> cutList.repaint());
        
        showNotification("✂️ " + selectedValues.size() + "개 잘라낸 항목 클립보드에 복사됨!");
        toggleVisibility();
    }
    
    private void restoreSelectedCutItems() {
        List<CutItem> selectedValues = cutList.getSelectedValuesList();
        if (selectedValues.isEmpty()) {
            JOptionPane.showMessageDialog(this, "되돌릴 항목을 선택해주세요!");
            return;
        }
        
        if (selectedValues.size() == 1) {
            // 단일 항목 복원
            CutItem item = selectedValues.get(0);
            copyToClipboard(item.content);
            item.isRestored = true;
            
            SwingUtilities.invokeLater(() -> cutList.repaint());
            showNotification("🔄 잘라낸 텍스트가 클립보드에 복원되었습니다!\n이제 Ctrl+V로 붙여넣으세요.");
            
        } else {
            // 다중 항목 복원
            int result = JOptionPane.showConfirmDialog(this,
                selectedValues.size() + "개 항목을 모두 복원하시겠습니까?\n" +
                "항목들이 줄바꿈으로 구분되어 클립보드에 복사됩니다.",
                "다중 복원 확인", JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                StringBuilder combined = new StringBuilder();
                for (int i = 0; i < selectedValues.size(); i++) {
                    combined.append(selectedValues.get(i).content);
                    selectedValues.get(i).isRestored = true;
                    if (i < selectedValues.size() - 1) {
                        combined.append("\n");
                    }
                }
                
                copyToClipboard(combined.toString());
                SwingUtilities.invokeLater(() -> cutList.repaint());
                showNotification("🔄 " + selectedValues.size() + "개 항목이 클립보드에 복원되었습니다!");
            }
        }
    }
    
    private void permanentlyDeleteCutItems() {
        List<CutItem> selectedValues = cutList.getSelectedValuesList();
        if (selectedValues.isEmpty()) {
            JOptionPane.showMessageDialog(this, "삭제할 항목을 선택해주세요!");
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this,
            "선택한 " + selectedValues.size() + "개 항목을 영구적으로 삭제하시겠습니까?\n" +
            "이 작업은 되돌릴 수 없습니다!",
            "영구 삭제 확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            selectedValues.forEach(cutHistory::remove);
            
            SwingUtilities.invokeLater(() -> {
                cutListModel.clear();
                cutHistory.forEach(cutListModel::addElement);
            });
            
            showNotification("🗑️ " + selectedValues.size() + "개 잘라낸 항목이 영구 삭제되었습니다!");
        }
    }
    
    private void clearCopyHistory() {
        int result = JOptionPane.showConfirmDialog(this,
            "모든 복사 히스토리를 삭제하시겠습니까?",
            "확인", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            copyHistory.clear();
            copyListModel.clear();
            showNotification("🗑️ 복사 히스토리가 삭제되었습니다!");
        }
    }
    
    private void clearCutHistory() {
        int result = JOptionPane.showConfirmDialog(this,
            "모든 잘라내기 히스토리를 삭제하시겠습니까?\n" +
            "복원되지 않은 항목들은 영구적으로 사라집니다!",
            "확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            cutHistory.clear();
            cutListModel.clear();
            showNotification("🗑️ 잘라내기 히스토리가 삭제되었습니다!");
        }
    }
    
    private void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        systemClipboard.setContents(selection, null);
        lastClipboardContent = text;
    }
    
    private void applySelectedTheme() {
        ThemeInfo selectedTheme = (ThemeInfo) themeComboBox.getSelectedItem();
        if (selectedTheme != null) {
            try {
                UIManager.setLookAndFeel(selectedTheme.className);
                SwingUtilities.updateComponentTreeUI(this);
                showNotification("🎨 테마가 " + selectedTheme.name + "으로 변경되었습니다!");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "테마 적용 실패: " + e.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void resetToDefaultTheme() {
        try {
            FlatDarkLaf.setup();
            SwingUtilities.updateComponentTreeUI(this);
            themeComboBox.setSelectedIndex(1);
            showNotification("🔄 기본 테마로 복원되었습니다!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void toggleVisibility() {
        setVisible(!isVisible());
        if (isVisible()) {
            toFront();
            requestFocus();
        }
    }
    
    private void showNotification(String message) {
        JLabel notification = new JLabel("<html><center>" + message + "</center></html>");
        notification.setOpaque(true);
        notification.setBackground(new Color(50, 50, 50));
        notification.setForeground(Color.WHITE);
        notification.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        JWindow notificationWindow = new JWindow();
        notificationWindow.add(notification);
        notificationWindow.pack();
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        notificationWindow.setLocation(
            screenSize.width - notificationWindow.getWidth() - 20,
            screenSize.height - notificationWindow.getHeight() - 50
        );
        
        notificationWindow.setVisible(true);
        notificationWindow.setAlwaysOnTop(true);
        
        Timer hideTimer = new Timer(3000, e -> notificationWindow.dispose());
        hideTimer.setRepeats(false);
        hideTimer.start();
    }
    
    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    // 클립보드 아이템 클래스
    private static class ClipboardItem {
        enum Type { COPY, CUT }
        
        final String content;
        final LocalDateTime timestamp;
        final Type type;
        
        ClipboardItem(String content, LocalDateTime timestamp, Type type) {
            this.content = content;
            this.timestamp = timestamp;
            this.type = type;
        }
        
        @Override
        public String toString() {
            String timeStr = timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String preview = content.replaceAll("\\s+", " ").trim();
            if (preview.length() > 60) {
                preview = preview.substring(0, 60) + "...";
            }
            return String.format("[%s] %s", timeStr, preview);
        }
    }
    
    // 잘라내기 아이템 클래스
    private static class CutItem {
        final String content;
        final LocalDateTime timestamp;
        final String originalSource; // 원래 애플리케이션 정보 (가능하다면)
        boolean isUsed;
        boolean isRestored;
        
        CutItem(String content, LocalDateTime timestamp, String originalSource, boolean isUsed) {
            this.content = content;
            this.timestamp = timestamp;
            this.originalSource = originalSource;
            this.isUsed = isUsed;
            this.isRestored = false;
        }
        
        @Override
        public String toString() {
            String timeStr = timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String preview = content.replaceAll("\\s+", " ").trim();
            if (preview.length() > 50) {
                preview = preview.substring(0, 50) + "...";
            }
            
            String status = "";
            if (isRestored) status = "🔄";
            else if (isUsed) status = "✅";
            else status = "✂️";
            
            return String.format("%s [%s] %s", status, timeStr, preview);
        }
    }
    
    // 테마 정보 클래스
    private static class ThemeInfo {
        final String name;
        final String className;
        
        ThemeInfo(String name, String className) {
            this.name = name;
            this.className = className;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    // 클립보드 아이템 렌더러
    private static class ClipboardItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            if (value instanceof ClipboardItem) {
                ClipboardItem item = (ClipboardItem) value;
                String icon = getContentIcon(item.content);
                value = icon + " " + item.toString();
            }
            
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (!isSelected) {
                setBackground(index % 2 == 0 ? list.getBackground() : 
                    new Color(list.getBackground().getRed() + 8, 
                             list.getBackground().getGreen() + 8, 
                             list.getBackground().getBlue() + 8));
            }
            
            return c;
        }
    }
    
    // 잘라내기 아이템 렌더러
    private static class CutItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            if (value instanceof CutItem) {
                CutItem item = (CutItem) value;
                value = item.toString();
                
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                // 상태에 따른 색상 변경
                if (!isSelected) {
                    if (item.isRestored) {
                        setBackground(new Color(200, 255, 200)); // 연한 초록 (복원됨)
                    } else if (item.isUsed) {
                        setBackground(new Color(255, 255, 200)); // 연한 노랑 (사용됨)
                    } else {
                        setBackground(new Color(255, 220, 220)); // 연한 빨강 (잘라냄)
                    }
                }
                
                return c;
            }
            
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }
    
    // 테마 콤보박스 렌더러
    private static class ThemeComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            if (value instanceof ThemeInfo) {
                ThemeInfo theme = (ThemeInfo) value;
                value = "🎨 " + theme.name;
            }
            
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }
    
    private static String getContentIcon(String content) {
        if (content.contains("http://") || content.contains("https://")) {
            return "🔗";
        } else if (content.matches(".*\\d{3}-\\d{3}-\\d{4}.*")) {
            return "📞";
        } else if (content.contains("@") && content.contains(".")) {
            return "📧";
        } else if (content.length() > 100) {
            return "📄";
        } else {
            return "📝";
        }
    }
    
    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            UltimateClipboardManager manager = new UltimateClipboardManager();
            manager.setVisible(true);
            manager.showNotification("🚀 궁극의 클립보드 매니저가 시작되었습니다!\n✂️ 잘라내기 기능이 추가되었어요!");
        });
    }
}
