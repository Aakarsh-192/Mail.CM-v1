import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Splash.java
 * A borderless splash screen that shows the CMlogo and a progress bar.
 */
public class Splash extends JWindow {

    private JProgressBar progressBar;
    private Timer timer;
    private int progressValue = 0;
    private static final int LOADING_TIME_MS = 3000; // 3 seconds
    private static final int TIMER_DELAY = 30; // 30ms timer (3000 / 100 steps)

    public Splash() {
        setSize(500, 300);
        setLocationRelativeTo(null); // Center the screen
        
        // Main panel with a border
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new LineBorder(new Color(219, 68, 55), 3)); // Mail.cm red border
        add(mainPanel);

        // Logo Image - UPDATED: Load CMlogo.png instead of text
        JLabel logoLabel = new JLabel();
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        try {
            File imgFile = new File("CMlogo.png");
            if (imgFile.exists()) {
                ImageIcon originalIcon = new ImageIcon("CMlogo.png");
                // Scale the image to fit reasonably within the splash screen (max height 200)
                Image img = originalIcon.getImage();
                Image scaledImg = img.getScaledInstance(-1, 200, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaledImg));
            } else {
                // Fallback if image is missing
                logoLabel.setText("Mail.CM");
                logoLabel.setFont(new Font("Arial", Font.BOLD, 72));
                logoLabel.setForeground(new Color(219, 68, 55));
            }
        } catch (Exception e) {
            System.err.println("Error loading splash image: " + e.getMessage());
            logoLabel.setText("Mail.CM");
        }
        
        mainPanel.add(logoLabel, BorderLayout.CENTER);

        // Progress Bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("Loading...");
        progressBar.setForeground(new Color(52, 168, 83)); // Mail.cm green
        progressBar.setPreferredSize(new Dimension(100, 30));
        mainPanel.add(progressBar, BorderLayout.SOUTH);
    }

    /**
     * Starts the loading timer. When finished, it disposes this splash screen
     * and makes the main EmailClient app visible.
     */
    public void startLoading(EmailClient mainApp) {
        // This timer will fire 100 times over 3 seconds
        timer = new Timer(TIMER_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                progressValue++;
                progressBar.setValue(progressValue);
                
                if (progressValue >= 100) {
                    timer.stop();
                    setVisible(false);
                    dispose();
                    
                    // Show the main app
                    mainApp.setVisible(true);
                }
            }
        });
        
        timer.start();
    }
}