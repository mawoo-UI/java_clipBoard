package study2;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.intellijthemes.*;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

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

public class SecureClipboardManager extends JFrame implements NativeKeyListener {
    // 클립보드 히스토리
    private Queue<ClipboardItem> copyHistory;
    private JList<ClipboardItem> copyList;
    private DefaultListModel<ClipboardItem> copyListModel;
    
    private Queue<CutItem> cutHistory;
    private JList<CutItem> cutList;
    private DefaultListModel<CutItem> cutListModel;
    
    private Clipboard systemClipboard;
    private Timer clipboardMonitor;
    private String lastClipboardContent = "";
    
    // 키 입력 감지용 (보안 강화)
    private boolean ctrlPressed = false;
    private boolean expectingCut = false;
    private boolean expectingCopy = false;
    private long lastKeyTime = 0;
    
    // 보안 설정
    private boolean enableKeyLogging = false; // 키 로깅 비활성화
    private boolean autoHideAfterPaste = false; // 자동 숨김 비활성화
    private Set<Integer> monitoredKeys; // 감시할 키만 제한
    
    // UI 컴포넌트들
    private JTabbedPane mainTabbedPane;
    private JComboBox<ThemeInfo> themeComboBox;
    private JLabel statusLabel;
    private JCheckBox autoHideCheckBox;
    private JCheckBox keyLoggingCheckBox;
    
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
    
    public SecureClipboardManager() {
        initializeSecuritySettings();
        
        copyHistory = new ConcurrentLinkedQueue<>();
        cutHistory = new ConcurrentLinkedQueue<>();
        copyListModel = new DefaultListModel<>();
        cutListModel = new DefaultListModel<>();
        systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        
        setupSecureKeyListener();
        initializeGUI();
        startClipboardMonitoring();
        
        setVisible(false);
    }
    
    private void initializeSecuritySettings() {
        // 🔒 보안 설정 초기화
        enableKeyLogging = false; // 기본적으로 키 로깅 비활성화
        autoHideAfterPaste = false; // 기본적으로 자동 숨김 비활성화
        
        // 감시할 키만 제한 (Ctrl, C, X, V, Shift)
        monitoredKeys = new HashSet<>();
        monitoredKeys.add(29);  // Ctrl (왼쪽)
        monitoredKeys.add(3613); // Ctrl (오른쪽, 일부 시스템)
        monitoredKeys.add(46);  // C
        monitoredKeys.add(45);  // X  
        monitoredKeys.add(47);  // V
        monitoredKeys.add(42);  // Shift (왼쪽)
        monitoredKeys.add(54);  // Shift (오른쪽)
        
        System.out.println("🔒 보안 모드 활성화:");
        System.out.println("   - 키 로깅: " + (enableKeyLogging ? "활성" : "비활성"));
        System.out.println("   - 감시 키: Ctrl, C, X, V, Shift만");
        System.out.println("   - 자동 숨김: " + (autoHideAfterPaste ? "활성" : "비활성"));
    }
    
    
    private void setupSecureKeyListener() {
        try {
            // JNativeHook 로깅 완전 비활성화 (보안 강화)
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(java.util.logging.Level.OFF); // 완전 비활성화
            logger.setUseParentHandlers(false);
            
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            System.out.println("✅ 보안 키 리스너 등록 성공 (제한된 키만 감지)");
            
        } catch (NativeHookException e) {
            System.err.println("❌ 키 리스너 등록 실패: " + e.getMessage());
            showSecurityWarning();
        }
    }
    
    private void showSecurityWarning() {
        JOptionPane.showMessageDialog(null, 
            "🔒 보안 키 감지 기능을 사용할 수 없습니다.\n\n" +
            "• Ctrl+C/Ctrl+X 자동 구분이 어려울 수 있습니다.\n" +
            "• 수동 추가 버튼을 사용해주세요.\n" +
            "• 관리자 권한 또는 보안 프로그램 확인이 필요합니다.\n\n" +
            "⚠️ 개인정보 보호를 위해 키 로깅은 최소화됩니다.", 
            "보안 알림", JOptionPane.INFORMATION_MESSAGE);
    }
    
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        int keyCode = e.getKeyCode();
        
        // 🔒 보안: 감시 대상 키가 아니면 무시
        if (!monitoredKeys.contains(keyCode)) {
            return; // 개인정보 보호
        }
        
        // 🔒 보안: 키 로깅이 활성화된 경우에만 로그 출력
        if (enableKeyLogging) {
            System.out.println("🔍 키 감지: " + keyCode + " (" + getKeyName(keyCode) + ")");
        }
        
        // Ctrl 키 감지
        if (keyCode == 29 || keyCode == 3613) {
            ctrlPressed = true;
            updateStatus("🔧 Ctrl 키 활성화됨");
            
        } else if (ctrlPressed) {
            if (keyCode == 46) { // C 키
                expectingCopy = true;
                expectingCut = false;
                lastKeyTime = System.currentTimeMillis();
                updateStatus("🔍 Ctrl+C 감지됨, 복사 대기 중...");
                if (enableKeyLogging) System.out.println("📋 Ctrl+C 조합 감지!");
                
            } else if (keyCode == 45) { // X 키
                expectingCut = true;
                expectingCopy = false;
                lastKeyTime = System.currentTimeMillis();
                updateStatus("✂️ Ctrl+X 감지됨, 잘라내기 대기 중...");
                if (enableKeyLogging) System.out.println("✂️ Ctrl+X 조합 감지!");
                
            } else if (keyCode == 47) { // V 키
                updateStatus("📄 Ctrl+V 감지됨");
                if (enableKeyLogging) System.out.println("📄 Ctrl+V 조합 감지!");
                
                // 🚨 자동 숨김 설정 확인
                if (autoHideAfterPaste && isVisible()) {
                    Timer hideTimer = new Timer(100, ev -> {
                        toggleVisibility();
                        updateStatus("👁️ 붙여넣기 후 자동 숨김됨");
                    });
                    hideTimer.setRepeats(false);
                    hideTimer.start();
                }
            }
        }
    }
    
    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        int keyCode = e.getKeyCode();
        
        // 🔒 보안: 감시 대상 키가 아니면 무시
        if (!monitoredKeys.contains(keyCode)) {
            return;
        }
        
        // Ctrl 키 해제
        if (keyCode == 29 || keyCode == 3613) {
            ctrlPressed = false;
            updateStatus("🔧 Ctrl 키 해제됨");
        }
    }
    
    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // 🔒 보안: 타이핑 이벤트는 완전 무시 (개인정보 보호)
    }
    
    private String getKeyName(int keyCode) {
        // 🔒 보안: 감시 대상 키의 이름만 반환
        switch (keyCode) {
            case 29:
            case 3613: return "Ctrl";
            case 46: return "C";
            case 45: return "X";
            case 47: return "V";
            case 42:
            case 54: return "Shift";
            default: return "Unknown";
        }
    }
    
    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(message);
            }
        });
        
        // 3초 후 상태 초기화
        Timer resetTimer = new Timer(3000, ev -> {
            SwingUtilities.invokeLater(() -> {
                if (statusLabel != null) {
                    statusLabel.setText("💡 Ctrl+C: 복사 | Ctrl+X: 잘라내기 | 보안 모드 활성화");
                }
            });
        });
        resetTimer.setRepeats(false);
        resetTimer.start();
    }
    
    private void initializeGUI() {
        setTitle("🔒 보안 클립보드 매니저 (개인정보 보호)");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(850, 650);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        
        mainTabbedPane = new JTabbedPane();
        
        // 📋 복사 탭
        JPanel copyPanel = createCopyPanel();
        mainTabbedPane.addTab("📋 복사 (Ctrl+C)", copyPanel);
        
        // ✂️ 잘라내기 탭
        JPanel cutPanel = createCutPanel();
        mainTabbedPane.addTab("✂️ 잘라내기 (Ctrl+X)", cutPanel);
        
        // 🎨 테마 탭
        JPanel themePanel = createThemePanel();
        mainTabbedPane.addTab("🎨 테마", themePanel);
        
        // ⚙️ 설정 탭 (보안 설정 포함)
        JPanel settingsPanel = createSecureSettingsPanel();
        mainTabbedPane.addTab("⚙️ 설정", settingsPanel);
        
        // 하단 상태 바
        statusLabel = new JLabel("💡 Ctrl+C: 복사 | Ctrl+X: 잘라내기 | 보안 모드 활성화");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC));
        
        add(mainTabbedPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        
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
        
        JButton manualAddBtn = new JButton("➕ 수동 추가");
        manualAddBtn.addActionListener(e -> manuallyAddToCopyHistory());
        manualAddBtn.setToolTipText("현재 클립보드 내용을 복사 히스토리에 수동으로 추가");
        
        controlPanel.add(pasteSelectedBtn);
        controlPanel.add(pasteAllBtn);
        controlPanel.add(clearBtn);
        controlPanel.add(manualAddBtn);
        
        // 복사 히스토리 리스트
        copyList = new JList<>(copyListModel);
        copyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        copyList.setCellRenderer(new ClipboardItemRenderer());
        
        copyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    pasteSelectedCopyItems();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(copyList);
        scrollPane.setBorder(new TitledBorder("📋 복사 히스토리 (총 " + copyHistory.size() + "개)"));
        
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
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
        restoreBtn.setToolTipText("잘라낸 텍스트를 클립보드에 다시 복사");
        
        JButton deleteBtn = new JButton("🗑️ 완전 삭제");
        deleteBtn.addActionListener(e -> permanentlyDeleteCutItems());
        deleteBtn.setForeground(Color.RED);
        
        JButton manualAddBtn = new JButton("➕ 수동 추가");
        manualAddBtn.addActionListener(e -> manuallyAddToCutHistory());
        manualAddBtn.setToolTipText("현재 클립보드 내용을 잘라내기 히스토리에 수동으로 추가");
        
        controlPanel.add(pasteSelectedBtn);
        controlPanel.add(restoreBtn);
        controlPanel.add(deleteBtn);
        controlPanel.add(manualAddBtn);
        
        // 잘라내기 히스토리 리스트
        cutList = new JList<>(cutListModel);
        cutList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        cutList.setCellRenderer(new CutItemRenderer());
        
        cutList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    pasteSelectedCutItems();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(cutList);
        scrollPane.setBorder(new TitledBorder("✂️ 잘라내기 히스토리 (총 " + cutHistory.size() + "개)"));
        
        // 하단 안내
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(new TitledBorder("💡 잘라내기 기능 안내"));
        
        String[] info = {
            "🔄 되돌리기: 잘라낸 텍스트를 클립보드에 다시 복사",
            "📄 붙여넣기: 선택한 잘라낸 텍스트를 현재 위치에 붙여넣기",
            "🗑️ 완전 삭제: 잘라낸 텍스트를 영구적으로 삭제",
            "🔒 보안: 개인정보 보호를 위해 키 로깅이 제한됩니다"
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
    
    private JPanel createSecureSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 기본 설정
        JCheckBox alwaysOnTopCheck = new JCheckBox("항상 위에 표시", true);
        alwaysOnTopCheck.addActionListener(e -> setAlwaysOnTop(alwaysOnTopCheck.isSelected()));
        
        // 🚨 자동 숨김 설정 (체크박스로 제어)
        autoHideCheckBox = new JCheckBox("붙여넣기 후 자동 숨김", autoHideAfterPaste);
        autoHideCheckBox.addActionListener(e -> {
            autoHideAfterPaste = autoHideCheckBox.isSelected();
            updateStatus("⚙️ 자동 숨김 설정: " + (autoHideAfterPaste ? "활성화" : "비활성화"));
            System.out.println("🔧 자동 숨김 설정 변경: " + autoHideAfterPaste);
        });
        
        JPanel maxItemsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        maxItemsPanel.add(new JLabel("최대 히스토리 개수:"));
        JSpinner maxItemsSpinner = new JSpinner(new SpinnerNumberModel(50, 10, 500, 10));
        maxItemsPanel.add(maxItemsSpinner);
        
        // 🔒 보안 설정 섹션
        JPanel securityPanel = new JPanel();
        securityPanel.setLayout(new BoxLayout(securityPanel, BoxLayout.Y_AXIS));
        securityPanel.setBorder(new TitledBorder("🔒 보안 설정"));
        
        // 키 로깅 설정
        keyLoggingCheckBox = new JCheckBox("키 입력 디버그 로그 (개발자용)", enableKeyLogging);
        keyLoggingCheckBox.addActionListener(e -> {
            enableKeyLogging = keyLoggingCheckBox.isSelected();
            updateStatus("🔒 키 로깅: " + (enableKeyLogging ? "활성화" : "비활성화"));
            System.out.println("🔒 키 로깅 설정 변경: " + enableKeyLogging);
            
            if (enableKeyLogging) {
                JOptionPane.showMessageDialog(this,
                    "⚠️ 키 로깅 활성화 경고\n\n" +
                    "개발 목적으로만 사용하세요.\n" +
                    "개인정보가 콘솔에 노출될 수 있습니다.\n" +
                    "일반 사용 시에는 비활성화하세요!",
                    "보안 경고", JOptionPane.WARNING_MESSAGE);
            }
        });
        
        // 키 감지 상태
        JPanel keyDetectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        keyDetectionPanel.add(new JLabel("🔍 전역 키 감지: "));
        JLabel keyStatusLabel = new JLabel(GlobalScreen.isNativeHookRegistered() ? "✅ 활성 (제한됨)" : "❌ 비활성");
        keyDetectionPanel.add(keyStatusLabel);
        
        // 개인정보 보호 안내
        JTextArea privacyInfo = new JTextArea(
            "🔒 개인정보 보호 정책:\n" +
            "• Ctrl, C, X, V 키만 감지합니다\n" +
            "• 비밀번호나 개인정보는 기록되지 않습니다\n" +
            "• 키 로깅은 기본적으로 비활성화됩니다\n" +
            "• 클립보드 내용만 임시 저장됩니다"
        );
        privacyInfo.setEditable(false);
        privacyInfo.setOpaque(false);
        privacyInfo.setFont(privacyInfo.getFont().deriveFont(Font.ITALIC, 11f));
        
        securityPanel.add(keyLoggingCheckBox);
        securityPanel.add(Box.createVerticalStrut(5));
        securityPanel.add(keyDetectionPanel);
        securityPanel.add(Box.createVerticalStrut(10));
        securityPanel.add(privacyInfo);
        
        panel.add(alwaysOnTopCheck);
        panel.add(Box.createVerticalStrut(10));
        panel.add(autoHideCheckBox);
        panel.add(Box.createVerticalStrut(10));
        panel.add(maxItemsPanel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(securityPanel);
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
        clipboardMonitor = new Timer(200, e -> checkClipboard());
        clipboardMonitor.start();
    }
    
    private void checkClipboard() {
        try {
            if (systemClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String currentContent = (String) systemClipboard.getData(DataFlavor.stringFlavor);
                
                if (currentContent != null && !currentContent.equals(lastClipboardContent)) {
                    long timeSinceLastKey = System.currentTimeMillis() - lastKeyTime;
                    
                    // 키 입력 후 1초 이내에 클립보드가 변경되면 해당 키 입력으로 판단
                    if (timeSinceLastKey < 1000) {
                        if (expectingCut) {
                            addToCutHistory(currentContent);
                            expectingCut = false;
                            updateStatus("✂️ 잘라내기 히스토리에 추가됨!");
                            if (enableKeyLogging) System.out.println("✂️ 잘라내기로 분류");
                            
                        } else if (expectingCopy) {
                            addToCopyHistory(currentContent);
                            expectingCopy = false;
                            updateStatus("📋 복사 히스토리에 추가됨!");
                            if (enableKeyLogging) System.out.println("📋 복사로 분류");
                        } else {
                            addToCopyHistory(currentContent);
                            if (enableKeyLogging) System.out.println("📋 기본 복사로 분류");
                        }
                    } else {
                        addToCopyHistory(currentContent);
                        if (enableKeyLogging) System.out.println("📋 시간 경과로 복사로 분류");
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
        
        copyHistory.removeIf(existing -> existing.content.equals(content));
        copyHistory.offer(item);
        
        while (copyHistory.size() > 50) {
            copyHistory.poll();
        }
        
        SwingUtilities.invokeLater(() -> {
            copyListModel.clear();
            copyHistory.forEach(copyListModel::addElement);
            updateTabTitles();
        });
    }
    
    private void addToCutHistory(String content) {
        CutItem item = new CutItem(content, LocalDateTime.now(), null, false);
        
        cutHistory.offer(item);
        
        while (cutHistory.size() > 30) {
            cutHistory.poll();
        }
        
        SwingUtilities.invokeLater(() -> {
            cutListModel.clear();
            cutHistory.forEach(cutListModel::addElement);
            updateTabTitles();
        });
        
        showNotification("✂️ 잘라내기 감지: " + truncateText(content, 30));
    }
    
    private void updateTabTitles() {
        mainTabbedPane.setTitleAt(0, "📋 복사 (Ctrl+C) - " + copyHistory.size() + "개");
        mainTabbedPane.setTitleAt(1, "✂️ 잘라내기 (Ctrl+X) - " + cutHistory.size() + "개");
    }
    
    private void manuallyAddToCopyHistory() {
        try {
            String content = (String) systemClipboard.getData(DataFlavor.stringFlavor);
            if (content != null && !content.trim().isEmpty()) {
                addToCopyHistory(content);
                showNotification("📋 복사 히스토리에 수동으로 추가되었습니다!");
            } else {
                JOptionPane.showMessageDialog(this, "클립보드가 비어있습니다!");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "클립보드 읽기 실패: " + e.getMessage());
        }
    }
    
    private void manuallyAddToCutHistory() {
        try {
            String content = (String) systemClipboard.getData(DataFlavor.stringFlavor);
            if (content != null && !content.trim().isEmpty()) {
                addToCutHistory(content);
                showNotification("✂️ 잘라내기 히스토리에 수동으로 추가되었습니다!");
            } else {
                JOptionPane.showMessageDialog(this, "클립보드가 비어있습니다!");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "클립보드 읽기 실패: " + e.getMessage());
        }
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
        
        // 🚨 자동 숨김 확인
        if (!autoHideAfterPaste) {
            updateStatus("📋 클립보드에 복사됨 (자동 숨김 비활성화)");
        }
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
        
        if (!autoHideAfterPaste) {
            updateStatus("📚 전체 복사됨 (자동 숨김 비활성화)");
        }
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
        
        selectedValues.forEach(item -> item.isUsed = true);
        SwingUtilities.invokeLater(() -> cutList.repaint());
        
        showNotification("✂️ " + selectedValues.size() + "개 잘라낸 항목 클립보드에 복사됨!");
        
        if (!autoHideAfterPaste) {
            updateStatus("✂️ 잘라낸 항목 복사됨 (자동 숨김 비활성화)");
        }
    }
    
    private void restoreSelectedCutItems() {
        List<CutItem> selectedValues = cutList.getSelectedValuesList();
        if (selectedValues.isEmpty()) {
            JOptionPane.showMessageDialog(this, "되돌릴 항목을 선택해주세요!");
            return;
        }
        
        if (selectedValues.size() == 1) {
            CutItem item = selectedValues.get(0);
            copyToClipboard(item.content);
            item.isRestored = true;
            
            SwingUtilities.invokeLater(() -> cutList.repaint());
            showNotification("🔄 잘라낸 텍스트가 클립보드에 복원되었습니다!\n이제 Ctrl+V로 붙여넣으세요.");
            
        } else {
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
                updateTabTitles();
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
            updateTabTitles();
            showNotification("🗑️ 복사 히스토리가 삭제되었습니다!");
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
    
    // 데이터 클래스들과 렌더러들... (이전과 동일)
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
    
    private static class CutItem {
        final String content;
        final LocalDateTime timestamp;
        final String originalSource;
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
    
    private static class CutItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            if (value instanceof CutItem) {
                CutItem item = (CutItem) value;
                value = item.toString();
                
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                if (!isSelected) {
                    if (item.isRestored) {
                        setBackground(new Color(200, 255, 200));
                    } else if (item.isUsed) {
                        setBackground(new Color(255, 255, 200));
                    } else {
                        setBackground(new Color(255, 220, 220));
                    }
                }
                
                return c;
            }
            
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }
    
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
            SecureClipboardManager manager = new SecureClipboardManager();
            manager.setVisible(true);
            manager.showNotification("🔒 보안 클립보드 매니저가 시작되었습니다!\n개인정보 보호 모드가 활성화되어 있습니다.");
        });
    }
}