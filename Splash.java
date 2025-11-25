import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class Splash extends JWindow {

    private JProgressBar progressBar;
    private Timer timer;
    private int progressValue = 0;
    private static final int LOADING_TIME_MS = 3000;
    private static final int TIMER_DELAY = 30;

    public Splash() {
        setSize(500, 300);
        setLocationRelativeTo(null);
        

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new LineBorder(new Color(219, 68, 55), 3));
        add(mainPanel);


        JLabel logoLabel = new JLabel();
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        try {
            File imgFile = new File("CMlogo.png");
            if (imgFile.exists()) {
                ImageIcon originalIcon = new ImageIcon("CMlogo.png");
     
                Image img = originalIcon.getImage();
                Image scaledImg = img.getScaledInstance(-1, 200, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaledImg));
            } else {

                logoLabel.setText("Mail.CM");
                logoLabel.setFont(new Font("Arial", Font.BOLD, 72));
                logoLabel.setForeground(new Color(219, 68, 55));
            }
        } catch (Exception e) {
            System.err.println("Error loading splash image: " + e.getMessage());
            logoLabel.setText("Mail.CM");
        }
        
        mainPanel.add(logoLabel, BorderLayout.CENTER);
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("Loading...");
        progressBar.setForeground(new Color(52, 168, 83)); 
        progressBar.setPreferredSize(new Dimension(100, 30));
        mainPanel.add(progressBar, BorderLayout.SOUTH);
    }

    public void startLoading(EmailClient mainApp) {
        timer = new Timer(TIMER_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                progressValue++;
                progressBar.setValue(progressValue);
                
                if (progressValue >= 100) {
                    timer.stop();
                    setVisible(false);
                    dispose();
       
                    mainApp.setVisible(true);
                }
            }
        });
        
        timer.start();
    }

}
