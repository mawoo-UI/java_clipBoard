package study2;

import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;


public class MultiClipboardManager extends JFrame {
    private List<String> clipboardHistory;
    private JList<String> historyList;
    private DefaultListModel<String> listModel;
    private Clipboard systemClipboard;
    private Timer clipboardMonitor;
    private String lastClipboardContent = "";
    
    
public MultiClipboardManager() {
    clipboardHistory = new ArrayList<>();
    listModel = new DefaultListModel<>();
    systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    
    initializeGUI();
    startClipboardMonitoring();
}

private void initializeGUI() {
    setTitle("멀티 클립보드 매니저");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(500, 400);
    setLocationRelativeTo(null);
    
    // 히스토리 리스트 생성
    historyList = new JList<>(listModel);
    historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    historyList.setCellRenderer(new ClipboardCellRenderer());
    
    JScrollPane scrollPane = new JScrollPane(historyList);
    scrollPane.setBorder(BorderFactory.createTitledBorder("클립보드 히스토리"));
    
    // 버튼 패널
    JPanel buttonPanel = new JPanel(new FlowLayout());
    
    JButton pasteButton = new JButton("선택한 항목 붙여넣기");
    pasteButton.addActionListener(e -> pasteSelectedItem());
    
    JButton pasteAllButton = new JButton("전체 붙여넣기");
    pasteAllButton.addActionListener(e -> pasteAllItems());
    
    JButton clearButton = new JButton("히스토리 지우기");
    clearButton.addActionListener(e -> clearHistory());
    
    JButton minimizeButton = new JButton("시스템 트레이로");
    minimizeButton.addActionListener(e -> minimizeToTray());
    
    buttonPanel.add(pasteButton);
    buttonPanel.add(pasteAllButton);
    buttonPanel.add(clearButton);
    buttonPanel.add(minimizeButton);
    
    // 단축키 설정
    setupKeyBindings();
    
    // 레이아웃
    setLayout(new BorderLayout());
    add(scrollPane, BorderLayout.CENTER);
    add(buttonPanel, BorderLayout.SOUTH);
    
    // 상태 표시
    JLabel statusLabel = new JLabel("Ctrl+Shift+V: 매니저 열기 | 복사하면 자동으로 히스토리에 추가됩니다");
    statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    add(statusLabel, BorderLayout.NORTH);
    
    
}

private void setupKeyBindings() {
    // Ctrl+Shift+V로 창 토글
    KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_V, 
        KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
    
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(keyStroke, "toggleWindow");
    getRootPane().getActionMap().put("toggleWindow", new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            toggleWindow();
        }
    });
}

private void startClipboardMonitoring() {
    // 클립보드 변화 감지 (0.5초마다 체크)
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
        // 클립보드 접근 오류는 무시 (다른 앱이 사용 중일 때)
    }
}

private void addToHistory(String content) {
    // 중복 제거
    clipboardHistory.remove(content);
    
    // 맨 앞에 추가
    clipboardHistory.add(0, content);
    
    // 최대 20개까지만 보관
    if (clipboardHistory.size() > 20) {
        clipboardHistory.remove(clipboardHistory.size() - 1);
    }
    
    // UI 업데이트
    SwingUtilities.invokeLater(() -> {
        listModel.clear();
        for (String item : clipboardHistory) {
            listModel.addElement(item);
        }
    });
}

private void pasteSelectedItem() {
    int selectedIndex = historyList.getSelectedIndex();
    if (selectedIndex >= 0) {
        String selectedText = clipboardHistory.get(selectedIndex);
        copyToClipboard(selectedText);
        
        // 자동으로 붙여넣기 시뮬레이션 (실제 앱에서는 Robot 클래스 사용)
        JOptionPane.showMessageDialog(this, 
            "클립보드에 복사됨: " + truncateText(selectedText, 50) + "\n이제 Ctrl+V로 붙여넣으세요!");
    } else {
        JOptionPane.showMessageDialog(this, "항목을 선택해주세요!");
    }
}

private void pasteAllItems() {
    if (clipboardHistory.isEmpty()) {
        JOptionPane.showMessageDialog(this, "히스토리가 비어있습니다!");
        return;
    }
    
    StringBuilder allText = new StringBuilder();
    for (int i = 0; i < clipboardHistory.size(); i++) {
        allText.append(clipboardHistory.get(i));
        if (i < clipboardHistory.size() - 1) {
            allText.append("\n---\n");
        }
    }
    
    copyToClipboard(allText.toString());
    JOptionPane.showMessageDialog(this, 
        "모든 항목이 클립보드에 복사됨!\n이제 Ctrl+V로 붙여넣으세요!");
}

private void copyToClipboard(String text) {
    StringSelection selection = new StringSelection(text);
    systemClipboard.setContents(selection, null);
    lastClipboardContent = text; // 무한 루프 방지
}

private void clearHistory() {
    clipboardHistory.clear();
    listModel.clear();
    JOptionPane.showMessageDialog(this, "히스토리가 지워졌습니다!");
}

private void minimizeToTray() {
    setVisible(false);
    // 실제로는 SystemTray를 사용해서 트레이 아이콘 구현
}

private void toggleWindow() {
    setVisible(!isVisible());
    if (isVisible()) {
        toFront();
        requestFocus();
    }
}

private String truncateText(String text, int maxLength) {
    if (text.length() <= maxLength) {
        return text;
    }
    return text.substring(0, maxLength) + "...";
	}
}
