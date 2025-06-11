package study2;

import com.formdev.flatlaf.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AdvancedMultiClipboardManager extends JFrame {
    private Queue<String> clipboardHistory;
    private JList<String> historyList;
    private DefaultListModel<String> listModel;
    private Clipboard systemClipboard;
    private Timer clipboardMonitor;
    private String lastClipboardContent = "";
    private JTabbedPane mainTabbedPane;
    private boolean isVisible = false;
    
    // 테마 관련
    private JComboBox<ThemeInfo> themeComboBox;
    private static final ThemeInfo[] AVAILABLE_THEMES = {
        new ThemeInfo("FlatLaf Light", "com.formdev.flatlaf.FlatLightLaf"),
        new ThemeInfo("FlatLaf Dark", "com.formdev.flatlaf.FlatDarkLaf"),
        new ThemeInfo("FlatLaf IntelliJ", "com.formdev.flatlaf.FlatIntelliJLaf"),
        new ThemeInfo("FlatLaf Darcula", "com.formdev.flatlaf.FlatDarculaLaf"),
        new ThemeInfo("Arc Orange", "com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme"),
        new ThemeInfo("Carbon", "com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme"),
        new ThemeInfo("Cobalt 2", "com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme"),
        new ThemeInfo("Cyan Light", "com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme"),
        new ThemeInfo("Dark Purple", "com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme"),
        new ThemeInfo("Dracula", "com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme"),
        new ThemeInfo("Gruvbox Dark Hard", "com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme"),
        new ThemeInfo("Material Design Dark", "com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme"),
        new ThemeInfo("Monokai Pro", "com.formdev.flatlaf.intellijthemes.FlatMonokaiProIJTheme"),
        new ThemeInfo("Nord", "com.formdev.flatlaf.intellijthemes.FlatNordIJTheme"),
        new ThemeInfo("One Dark", "com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme"),
        new ThemeInfo("Solarized Dark", "com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme"),
        new ThemeInfo("Solarized Light", "com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme"),
        new ThemeInfo("Spacegray", "com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme"),
        new ThemeInfo("Vuesion", "com.formdev.flatlaf.intellijthemes.FlatVuesionIJTheme")
    };
    
    public AdvancedMultiClipboardManager() {
        clipboardHistory = new ConcurrentLinkedQueue<>();
        listModel = new DefaultListModel<>();
        systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        
        initializeGUI();
        startClipboardMonitoring();
        setupGlobalKeyListener();
        
        // 시작 시 숨김
        setVisible(false);
    }
    
    private void initializeGUI() {
        setTitle("🔥 고급 멀티 클립보드 매니저");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); // 닫기 대신 숨김
        setSize(700, 500);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true); // 항상 위에 표시
        
        // 메인 탭 패널 생성
        mainTabbedPane = new JTabbedPane();
        
        // 클립보드 관리 탭
        JPanel clipboardPanel = createClipboardPanel();
        mainTabbedPane.addTab("📋 클립보드", clipboardPanel);
        
        // 테마 설정 탭
        JPanel themePanel = createThemePanel();
        mainTabbedPane.addTab("🎨 테마", themePanel);
        
        // 설정 탭
        JPanel settingsPanel = createSettingsPanel();
        mainTabbedPane.addTab("⚙️ 설정", settingsPanel);
        
        add(mainTabbedPane);
        
        // ESC 키로 숨기기
        setupKeyBindings();
    }
    
    private JPanel createClipboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 상단 컨트롤 패널
        JPanel controlPanel = new JPanel(new FlowLayout());
        
        JButton pasteSelectedBtn = new JButton("📄 선택 항목 붙여넣기");
        pasteSelectedBtn.addActionListener(e -> pasteSelectedItems());
        
        JButton pasteAllBtn = new JButton("📚 전체 붙여넣기");
        pasteAllBtn.addActionListener(e -> pasteAllItems());
        
        JButton clearBtn = new JButton("🗑️ 히스토리 지우기");
        clearBtn.addActionListener(e -> clearHistory());
        
        JButton hideBtn = new JButton("👁️ 숨기기 (ESC)");
        hideBtn.addActionListener(e -> toggleVisibility());
        
        controlPanel.add(pasteSelectedBtn);
        controlPanel.add(pasteAllBtn);
        controlPanel.add(clearBtn);
        controlPanel.add(hideBtn);
        
        // 클립보드 히스토리 리스트 (다중 선택 가능)
        historyList = new JList<>(listModel);
        historyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // 다중 선택!
        historyList.setCellRenderer(new AdvancedClipboardCellRenderer());
        historyList.setVisibleRowCount(15);
        
        // 더블클릭으로 붙여넣기
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    pasteSelectedItems();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(historyList);
        scrollPane.setBorder(new TitledBorder("클립보드 히스토리 (다중 선택: Ctrl+클릭)"));
        
        // 하단 상태 패널
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel statusLabel = new JLabel("💡 Ctrl+Shift+V: 토글 | ESC: 숨기기 | 더블클릭: 붙여넣기");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));
        statusPanel.add(statusLabel);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(statusPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createThemePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 테마 선택 컨트롤
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
        
        // 테마 미리보기 패널
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
        
        // 미리보기 컴포넌트들
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
        sampleProgress.setValue(60);
        sampleProgress.setStringPainted(true);
        sampleProgress.setString("진행률 60%");
        preview.add(sampleProgress, gbc);
        
        return preview;
    }
    
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 설정 옵션들
        JCheckBox alwaysOnTopCheck = new JCheckBox("항상 위에 표시", true);
        alwaysOnTopCheck.addActionListener(e -> setAlwaysOnTop(alwaysOnTopCheck.isSelected()));
        
        JCheckBox autoHideCheck = new JCheckBox("붙여넣기 후 자동 숨김", true);
        
        JPanel maxItemsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        maxItemsPanel.add(new JLabel("최대 히스토리 개수:"));
        JSpinner maxItemsSpinner = new JSpinner(new SpinnerNumberModel(50, 10, 500, 10));
        maxItemsPanel.add(maxItemsSpinner);
        
        JPanel shortcutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        shortcutPanel.add(new JLabel("전역 단축키: Ctrl+Shift+V (변경 불가)"));
        
        // 시스템 트레이 설정
        JCheckBox trayCheck = new JCheckBox("시스템 트레이에 최소화", true);
        
        panel.add(alwaysOnTopCheck);
        panel.add(Box.createVerticalStrut(10));
        panel.add(autoHideCheck);
        panel.add(Box.createVerticalStrut(10));
        panel.add(maxItemsPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(shortcutPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(trayCheck);
        panel.add(Box.createVerticalGlue());
        
        // 정보 패널
        JPanel infoPanel = new JPanel();
        infoPanel.setBorder(new TitledBorder("💡 사용법"));
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        
        String[] instructions = {
            "• Ctrl+Shift+V: 매니저 표시/숨김",
            "• ESC: 매니저 숨기기",
            "• Ctrl+클릭: 여러 항목 선택",
            "• 더블클릭: 선택 항목 붙여넣기",
            "• 복사할 때마다 자동으로 히스토리에 추가됩니다"
        };
        
        for (String instruction : instructions) {
            JLabel label = new JLabel(instruction);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoPanel.add(label);
        }
        
        panel.add(infoPanel);
        
        return panel;
    }
    
    private void setupKeyBindings() {
        // ESC 키로 숨기기
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "hideWindow");
        getRootPane().getActionMap().put("hideWindow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleVisibility();
            }
        });
    }
    
    private void setupGlobalKeyListener() {
        // 전역 단축키는 JNativeHook 라이브러리가 필요하지만
        // 여기서는 간단한 Timer로 Ctrl+Shift+V 감지 시뮬레이션
        // 실제로는 JNativeHook을 사용해야 함
        
        Timer globalKeyTimer = new Timer(100, e -> {
            // 실제 구현에서는 JNativeHook 사용
            // 여기서는 창이 포커스를 받으면 표시하는 방식으로 대체
        });
        globalKeyTimer.start();
    }
    
    private void startClipboardMonitoring() {
        clipboardMonitor = new Timer(500, e -> checkClipboard());
        clipboardMonitor.start();
    }
    
    private void checkClipboard() {
        try {
            if (systemClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String currentContent = (String) systemClipboard.getData(DataFlavor.stringFlavor);
                
                if (currentContent != null && !currentContent.equals(lastClipboardContent)) {
                    addToHistory(currentContent);
                    lastClipboardContent = currentContent;
                }
            }
        } catch (Exception ex) {
            // 클립보드 접근 오류는 무시
        }
    }
    
    private void addToHistory(String content) {
        // 중복 제거
        clipboardHistory.remove(content);
        
        // 맨 앞에 추가
        clipboardHistory.offer(content);
        
        // 최대 50개까지만 보관
        while (clipboardHistory.size() > 50) {
            clipboardHistory.poll();
        }
        
        // UI 업데이트
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            clipboardHistory.forEach(listModel::addElement);
        });
    }
    
    private void pasteSelectedItems() {
        List<String> selectedValues = historyList.getSelectedValuesList();
        if (selectedValues.isEmpty()) {
            JOptionPane.showMessageDialog(this, "붙여넣을 항목을 선택해주세요!");
            return;
        }
        
        // 선택된 항목들을 하나로 합치기
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < selectedValues.size(); i++) {
            combined.append(selectedValues.get(i));
            if (i < selectedValues.size() - 1) {
                combined.append("\n"); // 항목 사이에 줄바꿈
            }
        }
        
        copyToClipboard(combined.toString());
        
        // 알림 표시
        showNotification("📋 " + selectedValues.size() + "개 항목 클립보드에 복사됨!");
        
        // 창 숨기기
        toggleVisibility();
    }
    
    private void pasteAllItems() {
        if (clipboardHistory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "클립보드 히스토리가 비어있습니다!");
            return;
        }
        
        StringBuilder allText = new StringBuilder();
        clipboardHistory.forEach(item -> allText.append(item).append("\n---\n"));
        
        copyToClipboard(allText.toString());
        showNotification("📚 모든 항목(" + clipboardHistory.size() + "개) 클립보드에 복사됨!");
        toggleVisibility();
    }
    
    private void clearHistory() {
        int result = JOptionPane.showConfirmDialog(this, 
            "정말로 모든 클립보드 히스토리를 삭제하시겠습니까?", 
            "확인", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            clipboardHistory.clear();
            listModel.clear();
            showNotification("🗑️ 클립보드 히스토리가 삭제되었습니다!");
        }
    }
    
    private void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        systemClipboard.setContents(selection, null);
        lastClipboardContent = text; // 무한 루프 방지
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
            themeComboBox.setSelectedIndex(1); // FlatLaf Dark
            showNotification("🔄 기본 테마로 복원되었습니다!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void toggleVisibility() {
        isVisible = !isVisible;
        setVisible(isVisible);
        if (isVisible) {
            toFront();
            requestFocus();
        }
    }
    
    private void showNotification(String message) {
        // 간단한 토스트 메시지 (실제로는 TrayIcon 사용 가능)
        JLabel notification = new JLabel(message);
        notification.setOpaque(true);
        notification.setBackground(Color.BLACK);
        notification.setForeground(Color.WHITE);
        notification.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JWindow notificationWindow = new JWindow();
        notificationWindow.add(notification);
        notificationWindow.pack();
        
        // 화면 오른쪽 아래에 표시
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        notificationWindow.setLocation(
            screenSize.width - notificationWindow.getWidth() - 20,
            screenSize.height - notificationWindow.getHeight() - 50
        );
        
        notificationWindow.setVisible(true);
        
        // 3초 후 사라짐
        Timer hideTimer = new Timer(3000, e -> notificationWindow.dispose());
        hideTimer.setRepeats(false);
        hideTimer.start();
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
    
    // 고급 셀 렌더러
    private static class AdvancedClipboardCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            String text = (String) value;
            String displayText = text.replaceAll("\\s+", " ").trim();
            
            // 텍스트 길이에 따라 잘라내기
            if (displayText.length() > 80) {
                displayText = displayText.substring(0, 80) + "...";
            }
            
            // 아이콘 추가
            String icon = "📄";
            if (text.contains("http://") || text.contains("https://")) {
                icon = "🔗";
            } else if (text.matches(".*\\d{3}-\\d{3}-\\d{4}.*")) {
                icon = "📞";
            } else if (text.contains("@") && text.contains(".")) {
                icon = "📧";
            }
            
            displayText = icon + " " + displayText;
            
            Component c = super.getListCellRendererComponent(list, displayText, 
                index, isSelected, cellHasFocus);
            
            // 교대로 배경색 변경
            if (!isSelected) {
                setBackground(index % 2 == 0 ? list.getBackground() : 
                    new Color(list.getBackground().getRed() + 10, 
                             list.getBackground().getGreen() + 10, 
                             list.getBackground().getBlue() + 10));
            }
            
            return c;
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
    
    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            AdvancedMultiClipboardManager manager = new AdvancedMultiClipboardManager();
            
            // 시스템 트레이 아이콘 생성 (선택사항)
            if (SystemTray.isSupported()) {
                setupSystemTray(manager);
            }
            
            // Ctrl+Shift+V 시뮬레이션을 위한 임시 창
            JFrame tempFrame = new JFrame();
            tempFrame.setSize(1, 1);
            tempFrame.setLocation(-100, -100);
            tempFrame.setVisible(true);
            tempFrame.setFocusable(false);
            
            // 전역 키 리스너 설정
            tempFrame.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.isControlDown() && e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_V) {
                        manager.toggleVisibility();
                    }
                }
            });
            
            manager.setVisible(true);
            manager.showNotification("🚀 고급 멀티 클립보드 매니저가 시작되었습니다!");
        });
    }
    
    private static void setupSystemTray(AdvancedMultiClipboardManager manager) {
        try {
            SystemTray tray = SystemTray.getSystemTray();
            
            // 트레이 아이콘 이미지 생성
            Image image = Toolkit.getDefaultToolkit().createImage(new byte[0]); // 실제로는 아이콘 파일 사용
            
            PopupMenu popup = new PopupMenu();
            
            MenuItem showItem = new MenuItem("매니저 열기");
            showItem.addActionListener(e -> manager.toggleVisibility());
            
            MenuItem exitItem = new MenuItem("종료");
            exitItem.addActionListener(e -> System.exit(0));
            
            popup.add(showItem);
            popup.addSeparator();
            popup.add(exitItem);
            
            TrayIcon trayIcon = new TrayIcon(image, "멀티 클립보드 매니저", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> manager.toggleVisibility());
            
            tray.add(trayIcon);
        } catch (Exception e) {
            System.out.println("시스템 트레이 설정 실패: " + e.getMessage());
        }
    }
}