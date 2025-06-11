package study2;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class SimpleTest {

	public static void main(String[] args) {
		 System.out.println("🔍 테스트 시작...");
	        
	        // 1. 기본 확인
	        System.out.println("현재 디렉토리: " + System.getProperty("user.dir"));
	        
	        // 2. FlatLaf 클래스 찾기
	        try {
	            Class.forName("com.formdev.flatlaf.FlatDarkLaf");
	            System.out.println("✅ FlatLaf 클래스 발견!");
	        } catch (ClassNotFoundException e) {
	            System.out.println("❌ FlatLaf 클래스 못 찾음: " + e.getMessage());
	            return;
	        }
	        
	        // 3. GUI 생성 (EDT에서)
	        SwingUtilities.invokeLater(() -> {
	            try {
	                // FlatLaf 적용 시도
	                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
	                System.out.println("✅ FlatDarkLaf 적용 성공!");
	                
	                // 테스트 창 생성
	                JFrame frame = new JFrame("🎉 FlatLaf 테스트 성공!");
	                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	                frame.setSize(400, 300);
	                frame.setLocationRelativeTo(null);
	                
	                JPanel panel = new JPanel();
	                panel.add(new JLabel("FlatLaf가 성공적으로 로드되었습니다!"));
	                panel.add(new JButton("테스트 버튼"));
	                
	                frame.add(panel);
	                frame.setVisible(true);
	                
	            } catch (Exception e) {
	                System.out.println("❌ FlatLaf 적용 실패: " + e.getMessage());
	                e.printStackTrace();
	            }
	        });
	    }
	}