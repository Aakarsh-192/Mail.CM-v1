import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class EmailClient extends JFrame {

    public static final String APP_NAME = "Mail.CM";
    public static final String DOMAIN = "@mail.cm";
    public static final int UNSEND_TIMEOUT_MS = 60000;

    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    
    public final IDataManager dataManager;
    
    private User loggedInUser;

    private Login loginPanel;
    private SignUp signUpPanel;
    private MailboxPanel mailboxPanel;

    public EmailClient() {
        IDataManager tempManager;
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite Driver found. Using JDBCDataManager.");
            tempManager = new JDBCDataManager();
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite Driver NOT found. Falling back to FileDataManager.");
            tempManager = new FileDataManager();
        }
        this.dataManager = tempManager;

        setTitle(APP_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        try {
            ImageIcon icon = new ImageIcon("CMlogo.png");
            setIconImage(icon.getImage());
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        loginPanel = new Login(this);
        signUpPanel = new SignUp(this);

        mainPanel.add(new WelcomePanel(this), "Welcome");
        mainPanel.add(loginPanel, "Login");
        mainPanel.add(signUpPanel, "Register");

        add(mainPanel);
    }

    public IDataManager getDataManager() {
        return dataManager;
    }

    public void showWelcome() {
        setTitle(APP_NAME);
        cardLayout.show(mainPanel, "Welcome");
    }

    public void showLogin() {
        setTitle(APP_NAME + " - Sign In");
        if (loginPanel != null) {
            loginPanel.clearFields();
        }
        cardLayout.show(mainPanel, "Login");
    }

    public void showRegister() {
        setTitle(APP_NAME + " - Create Account");
        if (signUpPanel != null) {
            signUpPanel.clearFields();
        }
        cardLayout.show(mainPanel, "Register");
    }

    public void showMailbox(User user) {
        this.loggedInUser = user;
        setTitle(APP_NAME + " - " + user.getEmailId());

        mailboxPanel = new MailboxPanel(this, dataManager, user);

        if (mainPanel.getComponents().length > 3) {
            Component oldMailbox = mainPanel.getComponent(3);
            mainPanel.remove(oldMailbox);
        }
        mainPanel.add(mailboxPanel, "Mailbox");
        cardLayout.show(mainPanel, "Mailbox");
    }

    public void logout() {
        this.loggedInUser = null;
        showWelcome();
    }


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        SwingUtilities.invokeLater(() -> {
            Splash splash = new Splash();
            splash.setVisible(true);

            EmailClient emailClient = new EmailClient();

            splash.startLoading(emailClient);
        });
    }

    public JButton createStyledButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    class WelcomePanel extends JPanel {
        public WelcomePanel(EmailClient client) {
            setLayout(new GridBagLayout());
            setBackground(new Color(245, 245, 245));

            JLabel title = new JLabel(APP_NAME);
            title.setFont(new Font("Arial", Font.BOLD, 72));
            title.setForeground(new Color(219, 68, 55));

            JButton signInButton = createStyledButton("Sign In", new Color(66, 133, 244), Color.BLACK);
            signInButton.setFont(new Font("Arial", Font.BOLD, 18));
            signInButton.setPreferredSize(new Dimension(200, 50));

            JButton registerButton = createStyledButton("Create Account", new Color(52, 168, 83), Color.BLACK);
            registerButton.setFont(new Font("Arial", Font.BOLD, 18));
            registerButton.setPreferredSize(new Dimension(200, 50));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(20, 0, 20, 0);

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            add(title, gbc);

            gbc.gridy = 1;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(40, 15, 20, 15);
            add(signInButton, gbc);

            gbc.gridx = 1;
            gbc.gridy = 1;
            add(registerButton, gbc);

            signInButton.addActionListener(e -> client.showLogin());
            registerButton.addActionListener(e -> client.showRegister());
        }
    }

    public class MailboxPanel extends JPanel {
        private final EmailClient client;
        private final IDataManager dataManager;
        private final User user;
        private final CardLayout contentCardLayout;
        private final JPanel contentCardPanel;

        private MailListPanel inboxListPanel;
        private MailListPanel draftListPanel;
        private SentListPanel sentListPanel;
        private MailListPanel archiveListPanel;
        private TrashPanel trashListPanel;
        private ComposePanel composePanel;
        private SettingsPanel settingsPanel;

        private JSplitPane mainSplitPane;
        private final int EXPANDED_WIDTH = 240;
        private String currentView = "INBOX";

        public MailboxPanel(EmailClient client, IDataManager dataManager, User user) {
            this.client = client;
            this.dataManager = dataManager;
            this.user = user;
            setLayout(new BorderLayout());

            JPanel headerPanel = createHeaderPanel(client);
            add(headerPanel, BorderLayout.NORTH);

            JPanel sidebar = createSidebar();

            contentCardLayout = new CardLayout();
            contentCardPanel = new JPanel(contentCardLayout);

            inboxListPanel = new MailListPanel(this, client, ViewType.INBOX);
            draftListPanel = new MailListPanel(this, client, ViewType.DRAFTS);
            sentListPanel = new SentListPanel(this, client);
            archiveListPanel = new MailListPanel(this, client, ViewType.ARCHIVE);
            trashListPanel = new TrashPanel(this, client);
            composePanel = new ComposePanel(this, client);
            settingsPanel = new SettingsPanel(this, client);

            contentCardPanel.add(inboxListPanel, "INBOX");
            contentCardPanel.add(draftListPanel, "DRAFTS");
            contentCardPanel.add(sentListPanel, "SENT");
            contentCardPanel.add(archiveListPanel, "ARCHIVE");
            contentCardPanel.add(trashListPanel, "DELETED");
            contentCardPanel.add(composePanel, "COMPOSE");
            contentCardPanel.add(settingsPanel, "SETTINGS");

            mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, contentCardPanel);
            mainSplitPane.setDividerLocation(EXPANDED_WIDTH);
            mainSplitPane.setDividerSize(5);
            mainSplitPane.setBorder(null);

            add(mainSplitPane, BorderLayout.CENTER);

            refreshAllViews();
            showView("INBOX");
        }

        private JPanel createHeaderPanel(EmailClient client) {
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            headerPanel.setBackground(Color.WHITE);

            JLabel logo = new JLabel(APP_NAME);
            logo.setFont(new Font("Arial", Font.BOLD, 24));
            logo.setForeground(new Color(219, 68, 55));

            JLabel userLabel = new JLabel(user.getEmailId());
            userLabel.setFont(new Font("Arial", Font.PLAIN, 14));

            JButton settingsButton = client.createStyledButton("Settings", Color.LIGHT_GRAY, Color.BLACK);
            settingsButton.addActionListener(e -> showView("SETTINGS"));

            JButton logoutButton = client.createStyledButton("Sign Out", new Color(245, 245, 245), Color.BLACK);
            logoutButton.addActionListener(e -> client.logout());

            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            rightPanel.setOpaque(false);
            rightPanel.add(userLabel);
            rightPanel.add(settingsButton);
            rightPanel.add(logoutButton);

            headerPanel.add(logo, BorderLayout.WEST);
            headerPanel.add(rightPanel, BorderLayout.EAST);
            return headerPanel;
        }

        private JPanel createSidebar() {
            JPanel sidebar = new JPanel();
            sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
            sidebar.setBackground(Color.WHITE);
            sidebar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JButton composeBtn = client.createStyledButton("Compose", new Color(200, 220, 255), Color.BLACK);
            composeBtn.setFont(new Font("Arial", Font.BOLD, 14));
            composeBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)));
            composeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            composeBtn.addActionListener(e -> showView("COMPOSE"));

            composeBtn.setMaximumSize(new Dimension(220, 50));
            composeBtn.setMinimumSize(new Dimension(200, 50));

            sidebar.add(composeBtn);
            sidebar.add(Box.createVerticalStrut(10));

            sidebar.add(createNavLink("Inbox", "INBOX"));
            sidebar.add(createNavLink("Drafts", "DRAFTS"));
            sidebar.add(createNavLink("Sent", "SENT"));
            sidebar.add(createNavLink("Archive", "ARCHIVE"));
            sidebar.add(createNavLink("Trash", "DELETED"));

            sidebar.add(Box.createVerticalGlue());

            return sidebar;
        }

        private JButton createNavLink(String text, String viewName) {
            JButton button = new JButton(text);
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setBackground(Color.WHITE);
            button.setForeground(Color.BLACK);
            button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            button.setHorizontalAlignment(SwingConstants.LEFT);

            button.setMaximumSize(new Dimension(220, 35));

            button.addActionListener(e -> showView(viewName));

            if (currentView.equals(viewName)) {
                button.setBackground(new Color(220, 230, 255));
                button.setFont(button.getFont().deriveFont(Font.BOLD));
            } else {
                button.setFont(button.getFont().deriveFont(Font.PLAIN));
            }

            return button;
        }

        private void setSidebarWidth(String viewName) {
            mainSplitPane.setDividerLocation(EXPANDED_WIDTH);
        }

        public void showView(String viewName) {
            this.currentView = viewName;
            setSidebarWidth(viewName);

            if (viewName.equals("COMPOSE")) {
                if (!composePanel.isLoading) {
                    composePanel.clearFields();
                }
                composePanel.isLoading = false;
            }

            if (viewName.equals("INBOX"))
                inboxListPanel.refresh();
            else if (viewName.equals("DRAFTS"))
                draftListPanel.refresh();
            else if (viewName.equals("SENT"))
                sentListPanel.refresh();
            else if (viewName.equals("ARCHIVE"))
                archiveListPanel.refresh();
            else if (viewName.equals("DELETED"))
                trashListPanel.refresh();
            else if (viewName.equals("SETTINGS"))
                settingsPanel.loadUserSettings();

            contentCardLayout.show(contentCardPanel, viewName);

            mainSplitPane.setLeftComponent(createSidebar());
            mainSplitPane.revalidate();
            mainSplitPane.repaint();
        }

        public void refreshAllViews() {
            inboxListPanel.refresh();
            draftListPanel.refresh();
            sentListPanel.refresh();
            archiveListPanel.refresh();
            trashListPanel.refresh();
        }

        public User getLoggedInUser() {
            return user;
        }

        public IDataManager getDataManager() {
            return dataManager;
        }

        public ComposePanel getComposePanel() {
            return composePanel;
        }
    }

    public class MailListPanel extends JPanel {
        protected final MailboxPanel parentPanel;
        protected final EmailClient client;
        protected final ViewType viewType;
        protected final CustomTableModel tableModel;
        protected JTable emailTable;
        protected JTextArea emailView;
        protected JSplitPane splitPane;

        protected JButton refreshButton;
        protected JButton deleteButton;
        protected JButton archiveButton;
        protected JButton unarchiveButton;

        protected JCheckBox masterCheckBox;
        protected JPanel attachmentPanel;
        protected JScrollPane attachmentScrollPane;

        protected Email currentSelectedEmail;
        protected JPanel emailViewActionsPanel;
        protected JButton replyButton;
        protected JButton forwardButton;

        public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, HH:mm");

        public MailListPanel(MailboxPanel parentPanel, EmailClient client, ViewType viewType) {
            this.parentPanel = parentPanel;
            this.client = client;
            this.viewType = viewType;

            setLayout(new BorderLayout());

            String fromToColumnLabel = (viewType == ViewType.SENT || viewType == ViewType.DRAFTS) ? "To" : "From";
            String[] columnNames = { "", "S", fromToColumnLabel, "Subject", "Date" };

            tableModel = new CustomTableModel(columnNames);
            emailTable = new JTable(tableModel);

            emailTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            emailTable.setRowHeight(25);
            emailTable.setAutoCreateRowSorter(true);
            emailTable.setFont(new Font("SansSerif", Font.PLAIN, 12));

            TableColumnModel tcm = emailTable.getColumnModel();
            tcm.getColumn(0).setPreferredWidth(20);
            tcm.getColumn(1).setPreferredWidth(20);
            tcm.getColumn(2).setPreferredWidth(150);
            tcm.getColumn(3).setPreferredWidth(400);
            tcm.getColumn(4).setPreferredWidth(100);

            tcm.removeColumn(tcm.getColumn(5));

            emailTable.setDefaultRenderer(Object.class, new TableCellRenderer() {
                private final DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();

                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                        boolean hasFocus, int row, int column) {
                    Component cell = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                            row, column);

                    if (row >= 0 && row < table.getRowCount()) {
                        int modelRow = table.convertRowIndexToModel(row);

                        if (modelRow >= 0 && modelRow < table.getModel().getRowCount()) {
                            Email email = (Email) table.getModel().getValueAt(modelRow, 5);

                            if (email != null && column > 1) {
                                Font font = email.isRead() ? defaultRenderer.getFont().deriveFont(Font.PLAIN)
                                        : defaultRenderer.getFont().deriveFont(Font.BOLD);
                                cell.setFont(font);
                            }
                        }
                    }

                    if (isSelected) {
                        cell.setBackground(new Color(210, 230, 255));
                    } else {
                        cell.setBackground(Color.WHITE);
                    }

                    return cell;
                }
            });

            tableModel.addTableModelListener(e -> {
                if (e.getColumn() == 0) {
                    updateControlsVisibility();
                    updateMasterCheckboxState();
                }
            });
            emailTable.getModel().addTableModelListener(e -> updateControlsVisibility());
            emailTable.getSelectionModel().addListSelectionListener(e -> displaySelectedEmail());

            JPanel emailViewContainer = new JPanel(new BorderLayout());

            emailViewActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            emailViewActionsPanel.setBackground(Color.WHITE);

            replyButton = client.createStyledButton("Reply", new Color(230, 230, 230), Color.BLACK);
            replyButton.addActionListener(e -> {
                if (currentSelectedEmail != null) {
                    parentPanel.getComposePanel().loadReply(currentSelectedEmail);
                }
            });
            emailViewActionsPanel.add(replyButton);

            forwardButton = client.createStyledButton("Forward", new Color(230, 230, 230), Color.BLACK);
            forwardButton.addActionListener(e -> {
                if (currentSelectedEmail != null) {
                    parentPanel.getComposePanel().loadForward(currentSelectedEmail);
                }
            });
            emailViewActionsPanel.add(forwardButton);

            emailViewActionsPanel.setVisible(false);
            emailViewContainer.add(emailViewActionsPanel, BorderLayout.NORTH);

            emailView = new JTextArea("Select an email to view its content.");
            emailView.setEditable(false);
            emailView.setWrapStyleWord(true);
            emailView.setLineWrap(true);
            emailView.setBorder(new EmptyBorder(10, 10, 10, 10));
            emailViewContainer.add(new JScrollPane(emailView), BorderLayout.CENTER);

            attachmentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            attachmentPanel.setBackground(Color.WHITE);

            attachmentScrollPane = new JScrollPane(attachmentPanel);
            attachmentScrollPane.setBorder(BorderFactory.createTitledBorder("Attachments"));
            attachmentScrollPane.setPreferredSize(new Dimension(100, 120));
            attachmentScrollPane.setVisible(false);

            emailViewContainer.add(attachmentScrollPane, BorderLayout.SOUTH);

            JPanel controlsPanel = createControlsPanel();

            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(emailTable), emailViewContainer);
            splitPane.setDividerLocation(650);
            splitPane.setBorder(null);

            add(controlsPanel, BorderLayout.NORTH);
            add(splitPane, BorderLayout.CENTER);
        }

        protected class CustomTableModel extends DefaultTableModel {
            public CustomTableModel(Object[] columnNames) {
                super(columnNames, 0);
                addColumn("HiddenData");
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0)
                    return Boolean.class;
                return super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }

            @Override
            public Object getValueAt(int row, int column) {
                if (column == 5) {
                    if (row < dataVector.size()) {
                        return ((Vector<?>) dataVector.elementAt(row)).elementAt(column);
                    }
                    return null;
                }
                return super.getValueAt(row, column);
            }
        }

        protected void updateMasterCheckboxState() {
            if (tableModel.getRowCount() == 0) {
                masterCheckBox.setSelected(false);
                masterCheckBox.setEnabled(false);
                return;
            }

            masterCheckBox.setEnabled(true);
            int checkedCount = 0;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if ((Boolean) tableModel.getValueAt(i, 0)) {
                    checkedCount++;
                }
            }
            masterCheckBox.setSelected(checkedCount > 0 && checkedCount == tableModel.getRowCount());
        }

        protected void toggleAllSelection(boolean select) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(select, i, 0);
            }
            updateControlsVisibility();
        }

        protected void updateControlsVisibility() {
            boolean selectedByCheckbox = getSelectedEmails().size() > 0;

            if (deleteButton != null) {
                deleteButton.setVisible(selectedByCheckbox);
                if (viewType == ViewType.DELETED) {
                    deleteButton.setText("Delete Selected Forever");
                } else if (viewType == ViewType.DRAFTS) {
                    deleteButton.setText("Discard Selected Drafts");
                } else if (viewType == ViewType.SENT) {
                    deleteButton.setText("Delete Selected");
                } else {
                    deleteButton.setText("Delete Selected");
                }
            }
            if (archiveButton != null) {
                archiveButton.setVisible(selectedByCheckbox && viewType == ViewType.INBOX);
            }
            if (unarchiveButton != null) {
                unarchiveButton.setVisible(selectedByCheckbox && viewType == ViewType.ARCHIVE);
            }
        }

        protected JPanel createControlsPanel() {
            JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            controlsPanel.setBackground(Color.WHITE);
            controlsPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

            masterCheckBox = new JCheckBox();
            masterCheckBox.setOpaque(false);
            masterCheckBox.addActionListener(e -> toggleAllSelection(masterCheckBox.isSelected()));
            controlsPanel.add(masterCheckBox);

            if (viewType == ViewType.INBOX) {
                refreshButton = client.createStyledButton("Refresh", Color.LIGHT_GRAY, Color.BLACK);
                refreshButton.addActionListener(e -> {
                    refreshButton.setEnabled(false);
                    refreshButton.setText("Refreshing...");
                    
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            parentPanel.getDataManager().reloadData();
                            return null;
                        }

                        @Override
                        protected void done() {
                            parentPanel.refreshAllViews();
                            refreshButton.setEnabled(true);
                            refreshButton.setText("Refresh");
                        }
                    }.execute();
                });
                controlsPanel.add(refreshButton);
            }

            if (viewType == ViewType.INBOX) {
                archiveButton = client.createStyledButton("Archive Selected", new Color(244, 180, 0), Color.BLACK);
                archiveButton.addActionListener(e -> moveSelectedEmails(EmailStatus.ARCHIVED));
                controlsPanel.add(archiveButton);
            }

            if (viewType == ViewType.ARCHIVE) {
                unarchiveButton = client.createStyledButton("Unarchive Selected", new Color(66, 133, 244), Color.BLACK);
                unarchiveButton.addActionListener(e -> unarchiveSelectedEmails());
                controlsPanel.add(unarchiveButton);
            }

            deleteButton = client.createStyledButton("Delete Selected", new Color(219, 68, 55), Color.BLACK);
            deleteButton.addActionListener(e -> {
                if (viewType == ViewType.INBOX || viewType == ViewType.ARCHIVE) {
                    moveSelectedEmails(EmailStatus.DELETED);
                } else {
                    deleteSelectedEmails(true);
                }
            });
            controlsPanel.add(deleteButton);

            updateControlsVisibility();
            updateMasterCheckboxState();

            return controlsPanel;
        }

        public void refresh() {
            String selectedEmailId = null;
            if (currentSelectedEmail != null) {
                selectedEmailId = currentSelectedEmail.getMessageId();
            }

            tableModel.setRowCount(0);

            List<Email> filteredEmails = parentPanel.getDataManager().getEmails().stream()
                    .filter(e -> {
                        String userEmail = parentPanel.getLoggedInUser().getEmailId();
                        switch (viewType) {
                            case INBOX:
                                return e.isRecipient(userEmail) && e.getStatus() == EmailStatus.INBOX;
                            case ARCHIVE:
                                return (e.isRecipient(userEmail) || e.getFrom().equalsIgnoreCase(userEmail))
                                        && e.getStatus() == EmailStatus.ARCHIVED;
                            case DELETED:
                                return (e.isRecipient(userEmail) || e.getFrom().equalsIgnoreCase(userEmail))
                                        && e.getStatus() == EmailStatus.DELETED;
                            case DRAFTS:
                                return e.getFrom().equalsIgnoreCase(userEmail) && e.getStatus() == EmailStatus.DRAFT;
                            default:
                                return false;
                        }
                    })
                    .sorted((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()))
                    .collect(Collectors.toList());

            int newSelectedRow = -1;

            for (int i = 0; i < filteredEmails.size(); i++) {
                Email email = filteredEmails.get(i);
                String fromToText;
                if (viewType == ViewType.SENT || viewType == ViewType.DRAFTS ||
                        (viewType == ViewType.ARCHIVE
                                && email.getFrom().equalsIgnoreCase(parentPanel.getLoggedInUser().getEmailId()))
                        ||
                        (viewType == ViewType.DELETED
                                && email.getFrom().equalsIgnoreCase(parentPanel.getLoggedInUser().getEmailId()))) {
                    fromToText = String.join(", ", email.getTo());
                } else {
                    fromToText = email.getFrom();
                }

                tableModel.addRow(new Object[] {
                        false,
                        email.isRead() ? "" : "N",
                        fromToText.split("@")[0],
                        email.getSubject(),
                        DATE_FORMAT.format(email.getTimestamp()),
                        email
                });

                if (selectedEmailId != null && email.getMessageId().equals(selectedEmailId)) {
                    newSelectedRow = i;
                }
            }
            updateControlsVisibility();
            updateMasterCheckboxState();

            if (newSelectedRow != -1) {
                try {
                    int viewRow = emailTable.convertRowIndexToView(newSelectedRow);
                    emailTable.setRowSelectionInterval(viewRow, viewRow);
                } catch (Exception e) {
                    clearViewPane();
                }
            } else {
                clearViewPane();
            }
        }

        public void clearViewPane() {
            emailView.setText("Select an email to view its content.");
            attachmentScrollPane.setVisible(false);
            attachmentPanel.removeAll();
            emailViewActionsPanel.setVisible(false);
            currentSelectedEmail = null;
            emailTable.clearSelection();
        }

        protected List<Email> getSelectedEmails() {
            List<Email> selected = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if ((Boolean) tableModel.getValueAt(i, 0)) {
                    selected.add((Email) tableModel.getValueAt(i, 5));
                }
            }
            return selected;
        }

        protected void moveSelectedEmails(EmailStatus newStatus) {
            List<Email> selectedEmails = getSelectedEmails();
            if (selectedEmails.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Select one or more emails first.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            for (Email email : selectedEmails) {
                email.setStatus(newStatus);
            }
            parentPanel.getDataManager().saveAll();
            parentPanel.refreshAllViews();
        }

        protected void unarchiveSelectedEmails() {
            List<Email> selectedEmails = getSelectedEmails();
            if (selectedEmails.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Select one or more emails to unarchive.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String myEmail = parentPanel.getLoggedInUser().getEmailId();
            for (Email email : selectedEmails) {
                if (email.getFrom().equalsIgnoreCase(myEmail)) {
                    email.setStatus(EmailStatus.SENT);
                } else {
                    email.setStatus(EmailStatus.INBOX);
                }
            }
            parentPanel.getDataManager().saveAll();
            parentPanel.refreshAllViews();
        }

        protected void deleteSelectedEmails(boolean permanent) {
            List<Email> selectedEmails = getSelectedEmails();
            if (selectedEmails.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Select one or more emails first.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String message = (viewType == ViewType.DELETED) ? "permanently delete the selected items forever?"
                    : "permanently discard the selected items?";
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to " + message, "Confirm Deletion", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                parentPanel.getDataManager().getEmails().removeAll(selectedEmails);
                parentPanel.getDataManager().saveAll();
                parentPanel.refreshAllViews();
            }
        }

        protected void displaySelectedEmail() {
            int selectedRow = emailTable.getSelectedRow();
            attachmentPanel.removeAll();
            attachmentScrollPane.setVisible(false);

            if (selectedRow == -1) {
                emailView.setText("Select an email to view its content.");
                emailViewActionsPanel.setVisible(false);
                currentSelectedEmail = null;
                return;
            }
            int modelRow = emailTable.convertRowIndexToModel(selectedRow);
            if (modelRow < 0 || modelRow >= tableModel.getRowCount())
                return;

            Email email = (Email) tableModel.getValueAt(modelRow, 5);
            if (email == null)
                return;

            currentSelectedEmail = email;

            String myEmail = parentPanel.getLoggedInUser().getEmailId();
            boolean isMyEmail = email.getFrom().equalsIgnoreCase(myEmail);

            boolean showReply = false;
            boolean showForward = false;

            switch (viewType) {
                case INBOX:
                    showReply = true;
                    showForward = true;
                    break;

                case ARCHIVE:
                    if (!isMyEmail) {
                        showReply = true;
                        showForward = true;
                    } else {
                        showReply = false;
                        showForward = true;
                    }
                    break;

                case SENT:
                    showReply = false;
                    showForward = true;
                    break;

                case DRAFTS:
                case DELETED:
                    showReply = false;
                    showForward = false;
                    break;
            }

            replyButton.setVisible(showReply);
            forwardButton.setVisible(showForward);
            emailViewActionsPanel.setVisible(showReply || showForward);

            if (!email.isRead() && email.getStatus() != EmailStatus.DRAFT) {
                email.setRead(true);
                parentPanel.getDataManager().saveAll();
                tableModel.setValueAt("", modelRow, 1);
                emailTable.repaint();
            }

            emailTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && viewType == ViewType.DRAFTS) {
                        int row = emailTable.rowAtPoint(e.getPoint());
                        if (row >= 0) {
                            int draftModelRow = emailTable.convertRowIndexToModel(row);
                            Email selectedDraft = (Email) tableModel.getValueAt(draftModelRow, 5);
                            parentPanel.getComposePanel().loadDraft(selectedDraft);
                        }
                    }
                }
            });

            List<String> attachmentPaths = email.getAttachmentPaths();
            if (attachmentPaths != null && !attachmentPaths.isEmpty()) {
                attachmentScrollPane.setVisible(true);
                attachmentPanel.removeAll();
                for (String uniqueFilename : attachmentPaths) {
                    String originalName = uniqueFilename.substring(uniqueFilename.indexOf("-") + 1);
                    JButton attachmentButton = new JButton(originalName);
                    attachmentButton.setToolTipText("Click to save this file");
                    attachmentButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    attachmentButton.addActionListener(e -> saveAttachment(uniqueFilename, originalName));
                    attachmentPanel.add(attachmentButton);
                }
                attachmentPanel.revalidate();
                attachmentPanel.repaint();
            } else {
                attachmentScrollPane.setVisible(false);
            }

            String content = String.format(
                    "From: %s\n" +
                            "To: %s\n" +
                            "Subject: %s\n" +
                            "Date: %s\n\n" +
                            "----------------------------------\n\n" +
                            "%s",
                    email.getFrom(),
                    String.join(", ", email.getTo()),
                    email.getSubject(),
                    DATE_FORMAT.format(email.getTimestamp()),
                    email.getBody());

            emailView.setText(content);
            emailView.setCaretPosition(0);
        }

        private void saveAttachment(String uniqueFilename, String originalFilename) {
            File attachmentFile = parentPanel.getDataManager().getAttachment(uniqueFilename);
            if (attachmentFile == null) {
                JOptionPane.showMessageDialog(this, "Error: Attachment file not found in database.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Attachment");
            fileChooser.setSelectedFile(new File(originalFilename));

            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                try {
                    Files.copy(attachmentFile.toPath(), fileToSave.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    JOptionPane.showMessageDialog(this, "Attachment saved successfully!", "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Save Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public class SentListPanel extends MailListPanel {

        public SentListPanel(MailboxPanel parentPanel, EmailClient client) {
            super(parentPanel, client, ViewType.SENT);
            removeAll();
            add(createControlsPanel(), BorderLayout.NORTH);
            add(splitPane, BorderLayout.CENTER);
        }

        @Override
        protected JPanel createControlsPanel() {
            JPanel controlsPanel = new JPanel(new BorderLayout());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            buttonPanel.setBackground(Color.WHITE);
            buttonPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

            masterCheckBox = new JCheckBox();
            masterCheckBox.setOpaque(false);
            masterCheckBox.addActionListener(e -> toggleAllSelection(masterCheckBox.isSelected()));
            buttonPanel.add(masterCheckBox);

            deleteButton = client.createStyledButton("Delete Selected", new Color(219, 68, 55), Color.BLACK);
            deleteButton.addActionListener(e -> deleteSelectedEmails(true));
            buttonPanel.add(deleteButton);

            JTextArea unsendInfo = new JTextArea(
                    "Note: Deleting a sent email within 60 seconds of sending will 'unsend' it (delete it for all recipients). After 60 seconds, it only deletes your copy.");
            unsendInfo.setWrapStyleWord(true);
            unsendInfo.setLineWrap(true);
            unsendInfo.setEditable(false);
            unsendInfo.setBackground(new Color(255, 255, 204));
            unsendInfo.setForeground(Color.DARK_GRAY);
            unsendInfo.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            controlsPanel.add(buttonPanel, BorderLayout.NORTH);
            controlsPanel.add(unsendInfo, BorderLayout.CENTER);

            updateControlsVisibility();
            updateMasterCheckboxState();

            return controlsPanel;
        }

        @Override
        protected void deleteSelectedEmails(boolean permanent) {
            List<Email> selectedEmails = getSelectedEmails();
            if (selectedEmails.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Select one or more emails first.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<Email> emailsToUnsend = new ArrayList<>();
            List<Email> emailsToDelete = new ArrayList<>();

            long currentTime = System.currentTimeMillis();

            for (Email email : selectedEmails) {
                long timeElapsed = currentTime - email.getTimestamp();
                if (timeElapsed < UNSEND_TIMEOUT_MS) {
                    emailsToUnsend.add(email);
                } else {
                    emailsToDelete.add(email);
                }
            }

            if (!emailsToDelete.isEmpty()) {
                int confirmDelete = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to permanently delete " + emailsToDelete.size()
                                + " email(s)? This will only remove your copy.",
                        "Confirm Deletion", JOptionPane.YES_NO_OPTION);

                if (confirmDelete == JOptionPane.YES_OPTION) {
                    parentPanel.getDataManager().getEmails().removeAll(emailsToDelete);
                }
            }

            if (!emailsToUnsend.isEmpty()) {
                int confirmUnsend = JOptionPane.showConfirmDialog(this,
                        emailsToUnsend.size() + " email(s) are less than 60 seconds old.\n" +
                                "Are you sure you want to 'unsend' them? This will delete them for ALL recipients.",
                        "Confirm Unsend", JOptionPane.YES_NO_OPTION);

                if (confirmUnsend == JOptionPane.YES_OPTION) {
                    List<Email> allEmails = parentPanel.getDataManager().getEmails();
                    List<Email> emailsToRemove = new ArrayList<>(emailsToUnsend);

                    for (Email toUnsend : emailsToUnsend) {
                        String msgId = toUnsend.getMessageId();
                        if (msgId != null) {
                            allEmails.stream()
                                    .filter(e -> msgId.equals(e.getMessageId()) && e.getStatus() == EmailStatus.INBOX)
                                    .forEach(emailsToRemove::add);
                        }
                    }
                    allEmails.removeAll(emailsToRemove);
                }
            }

            parentPanel.getDataManager().saveAll();
            parentPanel.refreshAllViews();
        }

        @Override
        public void refresh() {
            String selectedEmailId = null;
            if (currentSelectedEmail != null) {
                selectedEmailId = currentSelectedEmail.getMessageId();
            }

            tableModel.setRowCount(0);

            List<Email> filteredEmails = parentPanel.getDataManager().getEmails().stream()
                    .filter(e -> e.getFrom().equalsIgnoreCase(parentPanel.getLoggedInUser().getEmailId()))
                    .filter(e -> e.getStatus() == EmailStatus.SENT)
                    .sorted((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()))
                    .collect(Collectors.toList());

            int newSelectedRow = -1;

            for (int i = 0; i < filteredEmails.size(); i++) {
                Email email = filteredEmails.get(i);
                tableModel.addRow(new Object[] {
                        false,
                        "",
                        String.join(", ", email.getTo()).split("@")[0],
                        email.getSubject(),
                        DATE_FORMAT.format(email.getTimestamp()),
                        email
                });

                if (selectedEmailId != null && email.getMessageId().equals(selectedEmailId)) {
                    newSelectedRow = i;
                }
            }
            updateControlsVisibility();
            updateMasterCheckboxState();

            if (newSelectedRow != -1) {
                try {
                    int viewRow = emailTable.convertRowIndexToView(newSelectedRow);
                    emailTable.setRowSelectionInterval(viewRow, viewRow);
                } catch (Exception e) {
                    clearViewPane();
                }
            } else {
                clearViewPane();
            }
        }
    }

    public class TrashPanel extends MailListPanel {

        private JButton recoverSelectedButton;

        public TrashPanel(MailboxPanel parentPanel, EmailClient client) {
            super(parentPanel, client, ViewType.DELETED);
            removeAll();
            add(createControlsPanel(), BorderLayout.NORTH);
            add(splitPane, BorderLayout.CENTER);
        }

        @Override
        protected JPanel createControlsPanel() {
            JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            controlsPanel.setBackground(Color.WHITE);
            controlsPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

            masterCheckBox = new JCheckBox();
            masterCheckBox.setOpaque(false);
            masterCheckBox.addActionListener(e -> toggleAllSelection(masterCheckBox.isSelected()));
            controlsPanel.add(masterCheckBox);

            recoverSelectedButton = client.createStyledButton("Recover Selected", new Color(66, 133, 244), Color.BLACK);
            recoverSelectedButton.addActionListener(e -> recoverSelected());
            controlsPanel.add(recoverSelectedButton);

            deleteButton = client.createStyledButton("Delete Selected Forever", new Color(219, 68, 55), Color.BLACK);
            deleteButton.addActionListener(e -> deleteSelectedEmails(true));
            controlsPanel.add(deleteButton);

            updateControlsVisibility();
            updateMasterCheckboxState();
            return controlsPanel;
        }

        @Override
        protected void updateControlsVisibility() {
            super.updateControlsVisibility();

            boolean selectedByCheckbox = getSelectedEmails().size() > 0;
            if (recoverSelectedButton != null) {
                recoverSelectedButton.setVisible(selectedByCheckbox);
            }
        }

        private void recoverSelected() {
            List<Email> selectedEmails = getSelectedEmails();
            if (selectedEmails.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Select one or more emails to recover.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            for (Email email : selectedEmails) {
                if (email.getFrom().equalsIgnoreCase(parentPanel.getLoggedInUser().getEmailId())) {
                    email.setStatus(EmailStatus.SENT);
                } else {
                    email.setStatus(EmailStatus.INBOX);
                }
            }
            parentPanel.getDataManager().saveAll();
            parentPanel.refreshAllViews();
        }
    }

    public class ComposePanel extends JPanel {

        private class AttachmentItem {
            String displayName;
            File file;
            String savedPath;

            AttachmentItem(File file) {
                this.file = file;
                this.displayName = file.getName();
                this.savedPath = null;
            }

            AttachmentItem(String savedPath) {
                this.file = null;
                this.savedPath = savedPath;
                this.displayName = savedPath.substring(savedPath.indexOf("-") + 1);
            }

            @Override
            public String toString() {
                return (file == null) ? displayName + " (attached)" : displayName;
            }
        }

        public boolean isLoading = false;
        private Email currentDraft = null;
        private boolean isUpdatingToField = false;

        private final JTextArea toField;
        private final JTextField subjectField;
        private final JTextArea bodyArea;
        private final MailboxPanel parentPanel;
        private final EmailClient client;

        private JPanel attachmentsContainer; 
        private List<AttachmentItem> currentAttachments = new ArrayList<>();

        private List<JCheckBox> contactCheckboxes = new ArrayList<>();

        public ComposePanel(MailboxPanel parentPanel, EmailClient client) {
            this.parentPanel = parentPanel;
            this.client = client;
            setLayout(new BorderLayout());

            JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            controlsPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

            JButton sendButton = client.createStyledButton("Send", new Color(26, 115, 232), Color.BLACK);
            sendButton.setFont(new Font("Arial", Font.BOLD, 14));
            sendButton.addActionListener(e -> handleSend(false));

            JButton saveDraftButton = client.createStyledButton("Save Draft", Color.WHITE, Color.BLACK);
            saveDraftButton.addActionListener(e -> handleSend(true));

            JButton discardButton = client.createStyledButton("Discard", Color.WHITE, Color.BLACK);
            discardButton.addActionListener(e -> {
                clearFields();
                parentPanel.showView("INBOX");
            });

            JButton attachButton = client.createStyledButton("Attach File", Color.WHITE, Color.BLACK);
            attachButton.addActionListener(e -> attachFile());

            controlsPanel.add(sendButton);
            controlsPanel.add(saveDraftButton);
            controlsPanel.add(discardButton);
            controlsPanel.add(new JSeparator(SwingConstants.VERTICAL));
            controlsPanel.add(attachButton);

            add(controlsPanel, BorderLayout.NORTH);

            JPanel inputPanel = new JPanel(new GridBagLayout());
            inputPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridx = 0;

            gbc.gridy = 0;
            inputPanel.add(new JLabel("To:"), gbc);

            gbc.gridy = 1;
            JPanel toContainer = new JPanel(new BorderLayout(5, 0));
            toField = new JTextArea(2, 50);
            toField.setLineWrap(true);
            toField.setWrapStyleWord(true);
            toField.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            toField.setMargin(new Insets(5, 8, 5, 8));
            
            JButton contactsBtn = new JButton("Contacts \u25BC");
            contactsBtn.setPreferredSize(new Dimension(100, 0));
            contactsBtn.addActionListener(e -> showContactsPopup(contactsBtn));

            toContainer.add(toField, BorderLayout.CENTER);
            toContainer.add(contactsBtn, BorderLayout.EAST);
            inputPanel.add(toContainer, gbc);

            gbc.gridy = 2;
            gbc.insets = new Insets(10, 0, 0, 0);
            inputPanel.add(new JLabel("Subject:"), gbc);

            gbc.gridy = 3;
            gbc.insets = new Insets(0, 0, 0, 0);
            subjectField = new JTextField();
            subjectField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
            subjectField.setPreferredSize(new Dimension(0, 35));
            subjectField.setMargin(new Insets(0, 5, 0, 5));
            inputPanel.add(subjectField, gbc);

            gbc.gridy = 4;
            gbc.insets = new Insets(10, 0, 0, 0);
            
            attachmentsContainer = new JPanel();
            attachmentsContainer.setLayout(new BoxLayout(attachmentsContainer, BoxLayout.Y_AXIS));
            attachmentsContainer.setBackground(new Color(245, 245, 245));
            
            JScrollPane attachScroll = new JScrollPane(attachmentsContainer);
            attachScroll.setBorder(null);
            attachScroll.setPreferredSize(new Dimension(0, 80));
            inputPanel.add(attachScroll, gbc);

            bodyArea = new JTextArea();
            bodyArea.setFont(new Font("Arial", Font.PLAIN, 14));
            bodyArea.setLineWrap(true);
            bodyArea.setWrapStyleWord(true);
            JScrollPane bodyScroll = new JScrollPane(bodyArea);
            bodyScroll.setBorder(new EmptyBorder(10, 20, 10, 20));

            JPanel centerWrapper = new JPanel(new BorderLayout());
            centerWrapper.add(inputPanel, BorderLayout.NORTH);
            centerWrapper.add(bodyScroll, BorderLayout.CENTER);
            
            add(centerWrapper, BorderLayout.CENTER);

            toField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { updateCheckboxesFromToField(); }
                public void removeUpdate(DocumentEvent e) { updateCheckboxesFromToField(); }
                public void changedUpdate(DocumentEvent e) { updateCheckboxesFromToField(); }
            });
        }


        private void showContactsPopup(Component invoker) {
            JPopupMenu popup = new JPopupMenu();
            popup.setPreferredSize(new Dimension(300, 200));
            
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setBackground(Color.WHITE);
            
            List<User> allUsers = parentPanel.getDataManager().getUsers();
            String currentUserEmail = parentPanel.getLoggedInUser().getEmailId();
            
            contactCheckboxes.clear();

            Set<String> currentEmails = getEmailsFromField();

            for (User user : allUsers) {
                if (user.getEmailId().equalsIgnoreCase(currentUserEmail)) continue;
                
                String labelText = user.getName() + " <" + user.getEmailId() + ">";
                JCheckBox checkBox = new JCheckBox(labelText);
                checkBox.setBackground(Color.WHITE);
                checkBox.setFocusPainted(false);
                
                if (currentEmails.contains(user.getEmailId().toLowerCase())) {
                    checkBox.setSelected(true);
                }

                checkBox.addActionListener(e -> {
                    if (isUpdatingToField) return;
                    updateToFieldFromCheckbox(user.getEmailId(), checkBox.isSelected());
                });
                
                contactCheckboxes.add(checkBox);
                contentPanel.add(checkBox);
            }
            
            JScrollPane scrollPane = new JScrollPane(contentPanel);
            scrollPane.setBorder(null);
            
            popup.add(scrollPane);
            popup.show(invoker, 0, invoker.getHeight());
        }

        private Set<String> getEmailsFromField() {
            String text = toField.getText().trim();
            if (text.isEmpty()) return new HashSet<>();
            
            return Arrays.stream(text.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
        
        private void updateToFieldFromCheckbox(String email, boolean isSelected) {
            isUpdatingToField = true;
            
            Set<String> currentEmails = getEmailsFromField();
            
            if (isSelected) {
                currentEmails.add(email.toLowerCase());
            } else {
                currentEmails.remove(email.toLowerCase());
            }
            
            String newText = String.join(", ", currentEmails);
            toField.setText(newText);
            
            isUpdatingToField = false;
        }

        private void updateCheckboxesFromToField() {
            if (isUpdatingToField) return; 
        }

        private void attachFile() {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                addAttachment(new AttachmentItem(fileChooser.getSelectedFile()));
            }
        }
        
        private void addAttachment(AttachmentItem item) {
            currentAttachments.add(item);
            refreshAttachmentsPanel();
        }
        
        private void removeAttachment(AttachmentItem item) {
            currentAttachments.remove(item);
            refreshAttachmentsPanel();
        }
        
        private void refreshAttachmentsPanel() {
            attachmentsContainer.removeAll();
            
            for (AttachmentItem item : currentAttachments) {
                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(new Color(230, 230, 230));
                row.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                
                JLabel nameLabel = new JLabel(item.toString());
                nameLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                
                JButton removeBtn = new JButton("x");
                removeBtn.setMargin(new Insets(0, 4, 0, 4));
                removeBtn.setPreferredSize(new Dimension(20, 20));
                removeBtn.setBackground(Color.WHITE);
                removeBtn.setForeground(Color.RED);
                removeBtn.setBorder(BorderFactory.createLineBorder(Color.RED));
                removeBtn.setFocusPainted(false);
                removeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                removeBtn.addActionListener(e -> removeAttachment(item));
                
                row.add(nameLabel, BorderLayout.CENTER);
                row.add(removeBtn, BorderLayout.EAST);
                
                attachmentsContainer.add(row);
                attachmentsContainer.add(Box.createVerticalStrut(2));
            }
            
            attachmentsContainer.revalidate();
            attachmentsContainer.repaint();
        }

        public void loadReply(Email originalEmail) {
            this.isLoading = true;
            parentPanel.showView("COMPOSE");
            clearFields();
    
            toField.setText(originalEmail.getFrom());
    
            String originalSubject = originalEmail.getSubject();
            if (originalSubject != null && originalSubject.startsWith("Re: ")) {
                subjectField.setText(originalSubject);
            } else {
                subjectField.setText("Re: " + (originalSubject != null ? originalSubject : ""));
            }
    
            String quote = createReplyQuote(originalEmail, "wrote");
            bodyArea.setText("\n\n" + quote);
            bodyArea.setCaretPosition(0);
        }

        public void loadForward(Email originalEmail) {
            this.isLoading = true;
            parentPanel.showView("COMPOSE");
            clearFields();
    
            toField.setText("");
    
            String originalSubject = originalEmail.getSubject();
            if (originalSubject != null && originalSubject.startsWith("Fwd: ")) {
                subjectField.setText(originalSubject);
            } else {
                subjectField.setText("Fwd: " + (originalSubject != null ? originalSubject : ""));
            }
    
            if (originalEmail.getAttachmentPaths() != null) {
                for (String uniqueFilename : originalEmail.getAttachmentPaths()) {
                    addAttachment(new AttachmentItem(uniqueFilename));
                }
            }
    
            String quote = createReplyQuote(originalEmail, "Forwarded Message");
            bodyArea.setText("\n\n" + quote);
            bodyArea.setCaretPosition(0);
        }

        private String createReplyQuote(Email email, String action) {
            String date = MailListPanel.DATE_FORMAT.format(email.getTimestamp());
            String sender = email.getFrom();

            String header;
            if (action.equals("Forwarded Message")) {
                header = String.format("---- %s ----\nFrom: %s\nDate: %s\nSubject: %s\nTo: %s\n",
                        action, email.getFrom(), date, email.getSubject(), String.join(", ", email.getTo()));
            } else {
                header = String.format("---- On %s, %s %s: ----", date, sender, action);
            }

            String originalBody = email.getBody();
            if (originalBody == null)
                originalBody = "";

            String quotedBody = Arrays.stream(originalBody.split("\n"))
                    .map(line -> "> " + line)
                    .collect(Collectors.joining("\n"));

            return header + "\n" + quotedBody;
        }

        public void loadDraft(Email draft) {
            this.isLoading = true;
            parentPanel.showView("COMPOSE");
            clearFields();

            this.currentDraft = draft;
            toField.setText(String.join(", ", draft.getTo()));
            subjectField.setText(draft.getSubject());
            bodyArea.setText(draft.getBody());

            if (draft.getAttachmentPaths() != null) {
                for (String uniqueFilename : draft.getAttachmentPaths()) {
                    addAttachment(new AttachmentItem(uniqueFilename));
                }
            }
        }

        private void handleSend(boolean isDraft) {
            String senderEmail = parentPanel.getLoggedInUser().getEmailId();

            List<String> recipients = Arrays.stream(toField.getText().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            String subject = subjectField.getText().trim();
            String body = bodyArea.getText().trim();

            if (!isDraft) {
                if (recipients.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter at least one recipient.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (subject.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Subject is required.", "Send Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (body.isEmpty() && currentAttachments.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please add a message body or an attachment.", "Send Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (recipients.stream().anyMatch(recip -> recip.equalsIgnoreCase(senderEmail))) {
                    JOptionPane.showMessageDialog(this, "You cannot send an email to yourself.", "Send Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                for (String recipient : recipients) {
                    if (!recipient.endsWith(DOMAIN)) {
                        JOptionPane.showMessageDialog(this,
                                "Recipient '" + recipient + "' must use the " + DOMAIN + " domain.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (parentPanel.getDataManager().getUsers().stream()
                            .noneMatch(u -> u.getEmailId().equalsIgnoreCase(recipient))) {
                        JOptionPane.showMessageDialog(this, "Recipient email address '" + recipient + "' not found.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            if (isDraft && recipients.isEmpty() && subject.isEmpty() && body.isEmpty()
                    && currentAttachments.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Cannot save an empty draft.", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            List<String> savedAttachmentNames = new ArrayList<>();
            for (AttachmentItem item : currentAttachments) {
                if (item.savedPath != null) {
                    savedAttachmentNames.add(item.savedPath);
                } else if (item.file != null) {
                    String savedName = parentPanel.getDataManager().saveAttachment(item.file);
                    if (savedName != null) {
                        savedAttachmentNames.add(savedName);
                    }
                }
            }

            if (currentDraft != null) {
                currentDraft.to = recipients;
                currentDraft.subject = subject;
                currentDraft.body = body;
                currentDraft.attachmentPaths = savedAttachmentNames;

                if (!isDraft) {
                    String msgId = currentDraft.getMessageId();
                    Email recipientCopy = new Email(msgId, senderEmail, recipients, subject, body, savedAttachmentNames,
                            EmailStatus.INBOX);
                    parentPanel.getDataManager().addEmail(recipientCopy);

                    Email senderCopy = new Email(msgId, senderEmail, recipients, subject, body, savedAttachmentNames,
                            EmailStatus.SENT);
                    parentPanel.getDataManager().addEmail(senderCopy);

                    parentPanel.getDataManager().getEmails().remove(currentDraft);

                } else {
                    currentDraft.setStatus(EmailStatus.DRAFT);
                    currentDraft.setRead(true);
                }
            } else {
                String msgId = UUID.randomUUID().toString();

                if (!isDraft) {
                    Email recipientCopy = new Email(msgId, senderEmail, recipients, subject, body, savedAttachmentNames,
                            EmailStatus.INBOX);
                    parentPanel.getDataManager().addEmail(recipientCopy);

                    Email senderCopy = new Email(msgId, senderEmail, recipients, subject, body, savedAttachmentNames,
                            EmailStatus.SENT);
                    parentPanel.getDataManager().addEmail(senderCopy);

                } else {
                    Email newDraft = new Email(msgId, senderEmail, recipients, subject, body, savedAttachmentNames,
                            EmailStatus.DRAFT);
                    parentPanel.getDataManager().addEmail(newDraft);
                }
            }

            parentPanel.getDataManager().saveAll();

            if (!isDraft) {
                JOptionPane.showMessageDialog(this, "Email sent successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                clearFields();
                parentPanel.showView("SENT");
            } else {
                JOptionPane.showMessageDialog(this, "Draft saved successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                clearFields();
                parentPanel.showView("DRAFTS");
            }
        }

        public void clearFields() {
            this.currentDraft = null;
            toField.setText("");
            subjectField.setText("");
            bodyArea.setText("");
            clearAttachments();
        }

        private void clearAttachments() {
            currentAttachments.clear();
            refreshAttachmentsPanel();
        }
    }

    public class SettingsPanel extends JPanel {
        private final MailboxPanel parentPanel;
        private final EmailClient client;
        private final JTextField nameField;
        private final JTextField emailField;
        private final JPasswordField newPasswordField;
        private final JPasswordField confirmPasswordField;

        public SettingsPanel(MailboxPanel parentPanel, EmailClient client) {
            this.parentPanel = parentPanel;
            this.client = client;
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(30, 50, 30, 50));

            JLabel title = new JLabel("Account Settings");
            title.setFont(new Font("Arial", Font.BOLD, 30));
            title.setForeground(new Color(66, 133, 244));

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(Color.WHITE);
            formPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
            formPanel.setPreferredSize(new Dimension(600, 550));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 20, 10, 20);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0;
            formPanel.add(new JLabel("Display Name:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            nameField = new JTextField(25);
            formPanel.add(nameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            formPanel.add(new JLabel("Email Address:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            emailField = new JTextField(25);
            emailField.setEditable(false);
            emailField.setBackground(new Color(240, 240, 240));
            formPanel.add(emailField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(20, 0, 20, 0);
            formPanel.add(new JSeparator(), gbc);
            gbc.insets = new Insets(10, 20, 10, 20);

            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            formPanel.add(new JLabel("New Password:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            newPasswordField = new JPasswordField(25);
            formPanel.add(newPasswordField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.weightx = 0;
            formPanel.add(new JLabel("Confirm Password:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            confirmPasswordField = new JPasswordField(25);
            formPanel.add(confirmPasswordField, gbc);

            JButton saveButton = client.createStyledButton("Save Changes", new Color(52, 168, 83), Color.BLACK);
            gbc.gridx = 1;
            gbc.gridy = 5;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(30, 20, 10, 20);
            formPanel.add(saveButton, gbc);

            gbc.gridx = 0;
            gbc.gridy = 6;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(20, 0, 20, 0);
            formPanel.add(new JSeparator(), gbc);
    
            JButton deleteAccountButton = client.createStyledButton("Delete Account", new Color(219, 68, 55), Color.WHITE);
            gbc.gridx = 0;
            gbc.gridy = 7;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.insets = new Insets(10, 20, 20, 20);
            formPanel.add(deleteAccountButton, gbc);

            JPanel centerContainer = new JPanel(new GridBagLayout());
            centerContainer.add(formPanel);

            add(title, BorderLayout.NORTH);
            add(centerContainer, BorderLayout.CENTER);

            saveButton.addActionListener(e -> saveSettings());
            deleteAccountButton.addActionListener(e -> deleteAccount());
        }

        public void loadUserSettings() {
            User user = parentPanel.getLoggedInUser();
            nameField.setText(user.getName());
            emailField.setText(user.getEmailId());
            newPasswordField.setText("");
            confirmPasswordField.setText("");
        }

        private void saveSettings() {
            User user = parentPanel.getLoggedInUser();
            String newName = nameField.getText().trim();
            String newPass = new String(newPasswordField.getPassword());
            String confirmPass = new String(confirmPasswordField.getPassword());

            boolean nameChanged = !user.getName().equals(newName);
            boolean passwordChanged = !newPass.isEmpty();

            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Display Name cannot be empty.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (passwordChanged) {
                if (!newPass.equals(confirmPass)) {
                    JOptionPane.showMessageDialog(this, "New Password and Confirm Password do not match.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (newPass.length() < 6) {
                    JOptionPane.showMessageDialog(this, "Password must be at least 6 characters.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            if (nameChanged) {
                user.setName(newName);
            }
            if (passwordChanged) {
                user.setPasswordHash(newPass);
            }

            if (nameChanged || passwordChanged) {
                parentPanel.getDataManager().saveAll();
                JOptionPane.showMessageDialog(this, "Settings saved successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                parentPanel.showView("INBOX");
            } else {
                JOptionPane.showMessageDialog(this, "No changes detected.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        
        private void deleteAccount() {
            JPasswordField pf = new JPasswordField();
            int ok = JOptionPane.showConfirmDialog(this, 
                new Object[]{"Please enter your password to confirm deletion:", pf}, 
                "Confirm Account Deletion", 
                JOptionPane.OK_CANCEL_OPTION, 
                JOptionPane.WARNING_MESSAGE);

            if (ok == JOptionPane.OK_OPTION) {
                String password = new String(pf.getPassword());
                User currentUser = parentPanel.getLoggedInUser();
                
                if (password.equals(currentUser.getPasswordHash())) {
                    int confirm = JOptionPane.showConfirmDialog(this, 
                        "Are you absolutely sure? This action cannot be undone and will delete your account and data.", 
                        "Last Warning", 
                        JOptionPane.YES_NO_OPTION, 
                        JOptionPane.ERROR_MESSAGE);
                        
                    if (confirm == JOptionPane.YES_OPTION) {
                        parentPanel.getDataManager().deleteUser(currentUser);
                        JOptionPane.showMessageDialog(this, "Account deleted successfully.", "Goodbye", JOptionPane.INFORMATION_MESSAGE);
                        client.logout();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Incorrect password. Account deletion cancelled.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

}
