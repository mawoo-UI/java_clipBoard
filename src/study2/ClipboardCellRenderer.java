package study2;

//import com.formdev.flatlaf.FlatDarkLaf;
//import com.formdev.flatlaf.FlatLightLaf;

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

class ClipboardCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {

        String text = (String) value;
        String displayText = text.replaceAll("\\s+", " ").trim();
        if (displayText.length() > 60) {
            displayText = displayText.substring(0, 60) + "...";
        }

        Component c = super.getListCellRendererComponent(list, displayText,
                index, isSelected, cellHasFocus);

        // 배경색 구분
        if (!isSelected) {
            setBackground(index % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
        }

        return c;
    }

    public static void main(String[] args) {
        // 2️⃣ 이 부분을 FlatLaf로 교체!
        try {
            FlatDarkLaf.setup(); // 다크 테마
            // FlatLightLaf.setup(); // 라이트 테마
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new MultiClipboardManager().setVisible(true);
        });
    }
}