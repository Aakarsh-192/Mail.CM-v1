import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class SignUp extends JPanel {
    private final EmailClient client;
    private final IDataManager dataManager;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    private String tempFirstName;
    private String tempLastName;
    private String tempEmailID; 
    private String tempPassword;

    private Step1_Name step1;
    private Step2_Email step2;
    private Step3_Password step3;

    private static final Color BG_COLOR = new Color(245, 245, 245);
    private static final Color CARD_BG_COLOR = Color.WHITE;
    private static final Color TEXT_COLOR = new Color(30, 30, 30);
    private static final Color SECONDARY_TEXT_COLOR = new Color(100, 100, 100);
    private static final Color LINK_COLOR = new Color(0, 102, 204);
    private static final Color LOGO_COLOR = new Color(219, 68, 55);
    private static final Color BUTTON_COLOR = new Color(26, 115, 232);
    private static final Color SECONDARY_BUTTON_BG = new Color(230, 230, 230);


    public SignUp(EmailClient client) {
        this.client = client;
        this.dataManager = client.getDataManager();
        this.cardLayout = new CardLayout();
        this.cardPanel = new JPanel(cardLayout);
        setLayout(new BorderLayout());

        step1 = new Step1_Name();
        step2 = new Step2_Email();
        step3 = new Step3_Password();

        cardPanel.add(step1, "Name");
        cardPanel.add(step2, "Email");
        cardPanel.add(step3, "Password");

        add(cardPanel, BorderLayout.CENTER);
    }
    
    public void clearFields() {
        tempFirstName = null;
        tempLastName = null;
        tempEmailID = null;
        tempPassword = null;
        
        step1.firstNameField.setText("");
        step1.lastNameField.setText("");
        step2.emailIdField.setText("");
        step2.suggestionArea.setText("");
        step3.passwordField.setText("");
        step3.confirmPasswordField.setText("");
      
        step3.showPasswordCheckbox.setSelected(false);
        char defaultEchoChar = (char) UIManager.get("PasswordField.echoChar");
        step3.passwordField.setEchoChar(defaultEchoChar);
        step3.confirmPasswordField.setEchoChar(defaultEchoChar);
        
        showStep("Name");
    }

    public void showStep(String stepName) {
        cardLayout.show(cardPanel, stepName);
    }

    private JPanel createLogoPanel() {
        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(CARD_BG_COLOR);
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
    
    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        label.setForeground(SECONDARY_TEXT_COLOR);
        return label;
    }
    
    private JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField(25);
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

    abstract class BaseStepPanel extends JPanel {
        public BaseStepPanel() {
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
            gbc.insets = new Insets(10, 0, 10, 0);
            cardPanel.add(createStepContent(), gbc);
            
            add(cardPanel);
        }
        
        abstract JPanel createStepContent();
    }


    class Step1_Name extends BaseStepPanel {
        private JTextField firstNameField;
        private JTextField lastNameField;

        @Override
        JPanel createStepContent() {
            JPanel content = new JPanel(new GridBagLayout());
            content.setBackground(CARD_BG_COLOR);
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            JLabel title = new JLabel("Create your " + EmailClient.APP_NAME + " Account");
            title.setFont(new Font("Arial", Font.BOLD, 24));
            title.setForeground(TEXT_COLOR);
            title.setHorizontalAlignment(JLabel.CENTER);
            content.add(title, gbc);

            gbc.gridy = 1;
            gbc.insets = new Insets(0, 0, 15, 0);
            JLabel subtitle = new JLabel("Enter your name");
            subtitle.setFont(new Font("Arial", Font.PLAIN, 16));
            subtitle.setForeground(SECONDARY_TEXT_COLOR);
            subtitle.setHorizontalAlignment(JLabel.CENTER);
            content.add(subtitle, gbc);

            gbc.gridy = 2;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(10, 0, 0, 0);
            content.add(createFieldLabel("First name"), gbc);

            gbc.gridx = 1;
            content.add(createFieldLabel("Last name (optional)"), gbc);
            
            gbc.gridy = 3;
            gbc.gridx = 0;
            gbc.insets = new Insets(0, 0, 10, 5);
            firstNameField = createFormField();
            content.add(firstNameField, gbc);
            
            gbc.gridx = 1;
            gbc.insets = new Insets(0, 5, 10, 0);
            lastNameField = createFormField();
            content.add(lastNameField, gbc);

            gbc.gridy = 4;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(10, 0, 0, 0);
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;
            JButton signInButton = createLinkButton("Sign in instead");
            content.add(signInButton, gbc);

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
            content.add(buttonPanel, gbc);
            
            nextButton.addActionListener(e -> attemptStep1());
            signInButton.addActionListener(e -> client.showLogin());

            return content;
        }
        
        private void attemptStep1() {
            String first = firstNameField.getText().trim();
            String last = lastNameField.getText().trim();
            
            if (first.isEmpty()) {
                JOptionPane.showMessageDialog(this, "First name is required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            tempFirstName = first;
            tempLastName = last;
            showStep("Email");
        }
    }

    class Step2_Email extends BaseStepPanel {
        private JTextField emailIdField;
        private JTextArea suggestionArea;

        @Override
        JPanel createStepContent() {
            JPanel content = new JPanel(new GridBagLayout());
            content.setBackground(CARD_BG_COLOR);
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridwidth = 2;

            gbc.gridy = 0;
            JLabel title = new JLabel("How you'll sign in");
            title.setFont(new Font("Arial", Font.BOLD, 24));
            title.setForeground(TEXT_COLOR);
            title.setHorizontalAlignment(JLabel.CENTER);
            content.add(title, gbc);

            gbc.gridy = 1;
            gbc.insets = new Insets(0, 0, 15, 0);
            JLabel subtitle = new JLabel("Create a " + EmailClient.APP_NAME + " address");
            subtitle.setFont(new Font("Arial", Font.PLAIN, 16));
            subtitle.setForeground(SECONDARY_TEXT_COLOR);
            subtitle.setHorizontalAlignment(JLabel.CENTER);
            content.add(subtitle, gbc);
            
            gbc.gridy = 2;
            gbc.insets = new Insets(10, 0, 0, 0);
            content.add(createFieldLabel("Username"), gbc);
            
            gbc.gridy = 3;
            gbc.insets = new Insets(0, 0, 5, 0);
            
            JPanel emailContainer = new JPanel(new BorderLayout(5, 0));
            emailContainer.setBackground(CARD_BG_COLOR);
            emailIdField = createFormField();
            emailContainer.add(emailIdField, BorderLayout.CENTER);
            
            JLabel domainLabel = new JLabel(EmailClient.DOMAIN);
            domainLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            domainLabel.setForeground(SECONDARY_TEXT_COLOR);
            emailContainer.add(domainLabel, BorderLayout.EAST);
            content.add(emailContainer, gbc);

            gbc.gridy = 4;
            gbc.insets = new Insets(0, 0, 10, 0);
            JLabel hintLabel = new JLabel("You can use letters, numbers & periods");
            hintLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            hintLabel.setForeground(SECONDARY_TEXT_COLOR);
            content.add(hintLabel, gbc);
            
            gbc.gridy = 5;
            suggestionArea = new JTextArea(3, 20);
            suggestionArea.setEditable(false);
            suggestionArea.setBackground(CARD_BG_COLOR);
            suggestionArea.setForeground(Color.RED);
            suggestionArea.setFont(new Font("Arial", Font.PLAIN, 12));
            suggestionArea.setBorder(BorderFactory.createEmptyBorder());
            content.add(suggestionArea, gbc);

            gbc.gridy = 6;
            gbc.insets = new Insets(10, 0, 0, 0);
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;
            JButton signInButton = createLinkButton("Sign in instead");
            signInButton.addActionListener(e -> client.showLogin());
            content.add(signInButton, gbc);

            gbc.gridy = 7;
            gbc.insets = new Insets(10, 0, 0, 0);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.setBackground(CARD_BG_COLOR);

            JButton backButton = client.createStyledButton("Back", SECONDARY_BUTTON_BG, Color.BLACK);
            backButton.setFont(new Font("Arial", Font.BOLD, 14));
            backButton.setPreferredSize(new Dimension(100, 40));
            backButton.addActionListener(e -> showStep("Name")); 

            JButton nextButton = client.createStyledButton("Next", BUTTON_COLOR, Color.BLACK);
            nextButton.setFont(new Font("Arial", Font.BOLD, 14));
            nextButton.setPreferredSize(new Dimension(100, 40));

            buttonPanel.add(backButton, BorderLayout.WEST);
            buttonPanel.add(nextButton, BorderLayout.EAST);
            content.add(buttonPanel, gbc);
            
            nextButton.addActionListener(e -> attemptStep2());
            emailIdField.addCaretListener(e -> checkEmailAvailability());

            return content;
        }

        private void checkEmailAvailability() {
            String desiredId = emailIdField.getText().trim();
            String fullEmail = desiredId + EmailClient.DOMAIN;
            suggestionArea.setText("");

            if (desiredId.isEmpty()) return;

            if (isEmailTaken(fullEmail)) {
                suggestionArea.setForeground(Color.RED);
                suggestionArea.setText("That username is taken. Try another.\n");
                List<String> suggestions = suggestEmailIDs(desiredId);
                suggestions.forEach(s -> suggestionArea.append("  - " + s.substring(0, s.indexOf(EmailClient.DOMAIN)) + "\n"));
            } else {
                suggestionArea.setForeground(new Color(52, 168, 83));
                suggestionArea.setText("Username is available.");
            }
        }
        
        private boolean isEmailTaken(String fullEmail) {
            return dataManager.getUsers().stream()
                    .anyMatch(u -> u.getEmailId().equalsIgnoreCase(fullEmail));
        }
        
        private List<String> suggestEmailIDs(String baseId) {
            Set<String> suggestions = new HashSet<>();
            Random rand = new Random();
            if (baseId.isEmpty()) baseId = "user"; 

            while (suggestions.size() < 3) {
                String randomPart = String.format("%04d", rand.nextInt(10000));
                String s = baseId + randomPart + EmailClient.DOMAIN;
                if (!isEmailTaken(s)) suggestions.add(s);
            }
            return new ArrayList<>(suggestions);
        }
        
        private void attemptStep2() {
            String desiredId = emailIdField.getText().trim();
            String fullEmail = desiredId + EmailClient.DOMAIN;

            if (desiredId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username is required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (isEmailTaken(fullEmail)) {
                JOptionPane.showMessageDialog(this, "That username is taken. Please choose an available ID.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            tempEmailID = desiredId;
            showStep("Password");
        }
    }

    class Step3_Password extends BaseStepPanel {
        private JPasswordField passwordField;
        private JPasswordField confirmPasswordField;
        private JCheckBox showPasswordCheckbox;

        @Override
        JPanel createStepContent() {
            JPanel content = new JPanel(new GridBagLayout());
            content.setBackground(CARD_BG_COLOR);
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridwidth = 2;

            gbc.gridy = 0;
            JLabel title = new JLabel("Create a strong password");
            title.setFont(new Font("Arial", Font.BOLD, 24));
            title.setForeground(TEXT_COLOR);
            title.setHorizontalAlignment(JLabel.CENTER);
            content.add(title, gbc);

            gbc.gridy = 1;
            gbc.insets = new Insets(0, 0, 15, 0);
            JLabel subtitle = new JLabel("Create a password with a mix of letters, numbers & symbols");
            subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
            subtitle.setForeground(SECONDARY_TEXT_COLOR);
            subtitle.setHorizontalAlignment(JLabel.CENTER);
            content.add(subtitle, gbc);

            gbc.gridy = 2;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(10, 0, 0, 0);
            content.add(createFieldLabel("Password"), gbc);

            gbc.gridx = 1;
            content.add(createFieldLabel("Confirm"), gbc);
            
            gbc.gridy = 3;
            gbc.gridx = 0;
            gbc.insets = new Insets(0, 0, 10, 5);
            passwordField = createPasswordField();
            content.add(passwordField, gbc);
            
            gbc.gridx = 1;
            gbc.insets = new Insets(0, 5, 10, 0);
            confirmPasswordField = createPasswordField();
            content.add(confirmPasswordField, gbc);
            
            gbc.gridy = 4;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(5, 0, 10, 0);
            showPasswordCheckbox = new JCheckBox("Show password");
            showPasswordCheckbox.setBackground(CARD_BG_COLOR);
            showPasswordCheckbox.setForeground(TEXT_COLOR);
            content.add(showPasswordCheckbox, gbc);

            gbc.gridy = 5;
            gbc.insets = new Insets(10, 0, 0, 0);
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;
            JButton signInButton = createLinkButton("Sign in instead");
            signInButton.addActionListener(e -> client.showLogin());
            content.add(signInButton, gbc);

            gbc.gridy = 6;
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
            content.add(buttonPanel, gbc);
            
            nextButton.addActionListener(e -> attemptStep3());
            showPasswordCheckbox.addActionListener(e -> {
                char echoChar = showPasswordCheckbox.isSelected() ? 0 : (char) UIManager.get("PasswordField.echoChar");
                passwordField.setEchoChar(echoChar);
                confirmPasswordField.setEchoChar(echoChar);
            });

            return content;
        }
        
        private void attemptStep3() {
            String password = new String(passwordField.getPassword());
            String confirm = new String(confirmPasswordField.getPassword());

            if (password.length() < 6) {
                JOptionPane.showMessageDialog(this, "Password must be at least 6 characters.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!password.equals(confirm)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String fullEmail = tempEmailID + EmailClient.DOMAIN;
            String fullName = tempFirstName + (tempLastName.isEmpty() ? "" : " " + tempLastName);

            User newUser = new User(fullName, fullEmail, password);
            dataManager.addUser(newUser);

            JOptionPane.showMessageDialog(this,
                "Account created successfully for " + fullEmail + "!",
                "Success", JOptionPane.INFORMATION_MESSAGE);

            client.showLogin(); 
        }
    }

}

