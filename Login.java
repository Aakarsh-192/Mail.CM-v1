import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Login.java
 * Handles the multi-step login (Sign In) process with a custom light UI.
 */
public class Login extends JPanel {
    private final EmailClient client;
    private final IDataManager dataManager;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    private String enteredEmail;
    private Optional<User> targetUser = Optional.empty();
    private String userFirstName; 

    // References to the step panels to clear them
    private Step1_Email step1;
    private Step2_Password step2;

    // UI Colors
    private static final Color BG_COLOR = new Color(245, 245, 245);
    private static final Color CARD_BG_COLOR = Color.WHITE;
    private static final Color TEXT_COLOR = new Color(30, 30, 30);
    private static final Color SECONDARY_TEXT_COLOR = new Color(100, 100, 100);
    private static final Color LINK_COLOR = new Color(0, 102, 204);
    private static final Color LOGO_COLOR = new Color(219, 68, 55);
    private static final Color BUTTON_COLOR = new Color(26, 115, 232);
    private static final Color SECONDARY_BUTTON_BG = new Color(230, 230, 230);


    public Login(EmailClient client) {
        this.client = client;
        this.dataManager = client.getDataManager();
        this.cardLayout = new CardLayout();
        this.cardPanel = new JPanel(cardLayout);
        setLayout(new BorderLayout());

        step1 = new Step1_Email();
        step2 = new Step2_Password();

        cardPanel.add(step1, "Email");
        cardPanel.add(step2, "Password");

        add(cardPanel, BorderLayout.CENTER);
    }

    /**
     * Clears all fields and resets the panel to step 1.
     */
    public void clearFields() {
        // Clear temporary data
        enteredEmail = null;
        targetUser = Optional.empty();
        userFirstName = null;
        
        // Clear text fields
        step1.emailField.setText("");
        step2.passwordField.setText("");
        
        // Reset password field to hide characters
        step2.passwordField.setEchoChar((char) UIManager.get("PasswordField.echoChar"));
        
        // Reset title
        step2.titleLabel.setText("Welcome"); 
        
        // Reset to step 1
        showStep("Email");
    }

    public void showStep(String stepName) {
        cardLayout.show(cardPanel, stepName);
    }

    private JPanel createLogoPanel() {
        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(CARD_BG_COLOR);
        // UPDATED: Case sensitive
        JLabel logoLabel = new JLabel("Mail.CM");
        logoLabel.setFont(new Font("Arial", Font.BOLD, 32));
        logoLabel.setForeground(LOGO_COLOR);
        logoPanel.add(logoLabel);
        return logoPanel;
    }

    private JTextField createFormField() {
        JTextField field = new JTextField(25);
        field.setFont(new Font("Arial", Font.PLAIN, 16));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(200, 200, 200)),
            new EmptyBorder(10, 10, 10, 10)
        ));
        return field;
    }

    private JButton createLinkButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(LINK_COLOR);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        return button;
    }

    class Step1_Email extends JPanel {
        private final JTextField emailField;

        public Step1_Email() {
            setBackground(BG_COLOR);
            setLayout(new GridBagLayout()); 
            
            JPanel cardPanel = new JPanel(new GridBagLayout());
            cardPanel.setBackground(CARD_BG_COLOR);
            cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(40, 50, 40, 50)
            ));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 0, 10, 0);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;

            gbc.gridy = 0;
            cardPanel.add(createLogoPanel(), gbc);

            gbc.gridy = 1;
            gbc.insets = new Insets(15, 0, 5, 0);
            JLabel title = new JLabel("Sign in");
            title.setFont(new Font("Arial", Font.BOLD, 28));
            title.setForeground(TEXT_COLOR);
            title.setHorizontalAlignment(JLabel.CENTER);
            cardPanel.add(title, gbc);

            gbc.gridy = 2;
            gbc.insets = new Insets(0, 0, 15, 0);
            JLabel subtitle = new JLabel("to continue to " + EmailClient.APP_NAME);
            subtitle.setFont(new Font("Arial", Font.PLAIN, 16));
            subtitle.setForeground(SECONDARY_TEXT_COLOR);
            subtitle.setHorizontalAlignment(JLabel.CENTER);
            cardPanel.add(subtitle, gbc);

            gbc.gridy = 3;
            gbc.insets = new Insets(15, 0, 10, 0);
            emailField = createFormField();
            cardPanel.add(emailField, gbc);

            gbc.gridy = 4;
            gbc.insets = new Insets(10, 0, 0, 0);
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;
            JButton createAccountButton = createLinkButton("Create account");
            cardPanel.add(createAccountButton, gbc);

            gbc.gridy = 5;
            gbc.insets = new Insets(20, 0, 0, 0);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.setBackground(CARD_BG_COLOR);

            JButton nextButton = client.createStyledButton("Next", BUTTON_COLOR, Color.BLACK);
            nextButton.setFont(new Font("Arial", Font.BOLD, 14));
            nextButton.setPreferredSize(new Dimension(100, 40));

            buttonPanel.add(new JPanel(){{ setBackground(CARD_BG_COLOR); }}, BorderLayout.WEST);
            buttonPanel.add(nextButton, BorderLayout.EAST);
            cardPanel.add(buttonPanel, gbc);
            
            add(cardPanel); 

            nextButton.addActionListener(e -> attemptStep1());
            createAccountButton.addActionListener(e -> client.showRegister());
        }

        private void attemptStep1() {
            String email = emailField.getText().trim();
            
            if (email.isEmpty() || !email.endsWith(EmailClient.DOMAIN)) {
                JOptionPane.showMessageDialog(this, "Please enter a valid " + EmailClient.DOMAIN + " email address.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            targetUser = dataManager.getUsers().stream()
                .filter(u -> u.getEmailId().equalsIgnoreCase(email))
                .findFirst();

            if (targetUser.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Could not find your " + EmailClient.APP_NAME + " Account.", "User Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            enteredEmail = email;
            userFirstName = targetUser.get().getName().split(" ")[0]; 
            
            showStep("Password");
        }
    }

    class Step2_Password extends JPanel {
        private final JPasswordField passwordField;
        private final JLabel emailDisplayLabel;
        private final JLabel titleLabel; 

        public Step2_Password() {
            setBackground(BG_COLOR);
            setLayout(new GridBagLayout()); 

            JPanel cardPanel = new JPanel(new GridBagLayout());
            cardPanel.setBackground(CARD_BG_COLOR);
            cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(40, 50, 40, 50)
            ));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 0, 10, 0);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;

            gbc.gridy = 0;
            cardPanel.add(createLogoPanel(), gbc);

            gbc.gridy = 1;
            gbc.insets = new Insets(15, 0, 5, 0);
            titleLabel = new JLabel("Welcome"); 
            titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
            titleLabel.setForeground(TEXT_COLOR);
            titleLabel.setHorizontalAlignment(JLabel.CENTER);
            cardPanel.add(titleLabel, gbc);

            gbc.gridy = 2;
            gbc.insets = new Insets(0, 0, 15, 0);
            
            emailDisplayLabel = new JLabel(""); 
            emailDisplayLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            emailDisplayLabel.setForeground(SECONDARY_TEXT_COLOR);
            emailDisplayLabel.setHorizontalAlignment(JLabel.CENTER);
            emailDisplayLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            emailDisplayLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showStep("Email"); 
                }
            });
            cardPanel.add(emailDisplayLabel, gbc);

            gbc.gridy = 3;
            gbc.insets = new Insets(15, 0, 10, 0);
            passwordField = new JPasswordField(25);
            passwordField.setFont(new Font("Arial", Font.PLAIN, 16));
            passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(200, 200, 200)),
                new EmptyBorder(10, 10, 10, 10)
            ));
            cardPanel.add(passwordField, gbc);

            gbc.gridy = 4;
            gbc.insets = new Insets(10, 0, 0, 0);
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;
            JButton forgotPasswordButton = createLinkButton("Forgot password?");
            forgotPasswordButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Forgot Password functionality is currently non-functional.", "Info", JOptionPane.INFORMATION_MESSAGE));
            cardPanel.add(forgotPasswordButton, gbc);

            gbc.gridy = 5;
            gbc.insets = new Insets(20, 0, 0, 0);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.setBackground(CARD_BG_COLOR);

            JButton backButton = client.createStyledButton("Back", SECONDARY_BUTTON_BG, Color.BLACK);
            backButton.setFont(new Font("Arial", Font.BOLD, 14));
            backButton.setPreferredSize(new Dimension(100, 40));
            backButton.addActionListener(e -> showStep("Email")); 

            JButton nextButton = client.createStyledButton("Next", BUTTON_COLOR, Color.BLACK);
            nextButton.setFont(new Font("Arial", Font.BOLD, 14));
            nextButton.setPreferredSize(new Dimension(100, 40));

            buttonPanel.add(backButton, BorderLayout.WEST);
            buttonPanel.add(nextButton, BorderLayout.EAST);
            cardPanel.add(buttonPanel, gbc);
            
            add(cardPanel); 

            nextButton.addActionListener(e -> attemptStep2());
        }

        @Override
        public void addNotify() {
            super.addNotify();
            if (enteredEmail != null) {
                emailDisplayLabel.setText(enteredEmail);
            }
            if (userFirstName != null && !userFirstName.isEmpty()) {
                titleLabel.setText("Welcome, " + userFirstName);
            } else {
                titleLabel.setText("Welcome"); // Fallback
            }
        }

        private void attemptStep2() {
            String password = new String(passwordField.getPassword());
            
            if (targetUser.isPresent() && targetUser.get().getPasswordHash().equals(password)) {
                client.showMailbox(targetUser.get());
            } else {
                JOptionPane.showMessageDialog(this, "Incorrect password. Please try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
            }
        }
    }
}