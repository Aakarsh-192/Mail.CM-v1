import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * JDBCDataManager.java
 * Implementation of IDataManager using JDBC (SQL Database).
 * Demonstrates JDBC Connectivity, PreparedStatement, and Exception Handling.
 */
public class JDBCDataManager implements IDataManager {
    private List<User> cachedUsers;
    private List<Email> cachedEmails;
    
    // Using SQLite for portable local database
    // Changed DB name to v2 to ensure fresh table creation with corrected schema
    private static final String DB_URL = "jdbc:sqlite:database/mail_sql_v2.db";
    private static final String ATTACHMENTS_DIR_PATH = "database/attachments/"; 

    public JDBCDataManager() {
        try {
            Files.createDirectories(Paths.get("database"));
            Files.createDirectories(Paths.get(ATTACHMENTS_DIR_PATH));
            
            // Load Driver (Assuming sqlite-jdbc is in classpath)
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                System.err.println("SQLite Driver not found. JDBC mode might fail.");
            }
            
            createTables();
            reloadData(); // Initial load
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void createTables() throws SQLException {
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (\n"
                + " name text NOT NULL,\n"
                + " email text PRIMARY KEY,\n"
                + " password text NOT NULL\n"
                + ");";

        // 'messageId' is no longer PRIMARY KEY to allow multiple copies (Sent/Inbox) of the same message ID.
        // Added 'id' as the actual row Primary Key.
        String sqlEmails = "CREATE TABLE IF NOT EXISTS emails (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " messageId text,\n" 
                + " sender text NOT NULL,\n"
                + " recipients text NOT NULL,\n" // Stored as comma-separated string
                + " subject text,\n"
                + " body text,\n"
                + " attachments text,\n" // Stored as comma-separated string
                + " timestamp integer,\n"
                + " isRead integer,\n"
                + " status text\n"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsers);
            stmt.execute(sqlEmails);
        }
    }

    @Override
    public void reloadData() {
        cachedUsers = new ArrayList<>();
        cachedEmails = new ArrayList<>();

        String selectUsers = "SELECT * FROM users";
        String selectEmails = "SELECT * FROM emails";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            
            // Load Users
            ResultSet rsUsers = stmt.executeQuery(selectUsers);
            while (rsUsers.next()) {
                cachedUsers.add(new User(
                    rsUsers.getString("name"),
                    rsUsers.getString("email"),
                    rsUsers.getString("password")
                ));
            }

            // Load Emails
            ResultSet rsEmails = stmt.executeQuery(selectEmails);
            while (rsEmails.next()) {
                String toStr = rsEmails.getString("recipients");
                List<String> toList = toStr.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(toStr.split(",")));
                
                String attachStr = rsEmails.getString("attachments");
                List<String> attachList = (attachStr == null || attachStr.isEmpty()) ? new ArrayList<>() : new ArrayList<>(Arrays.asList(attachStr.split(",")));

                Email email = new Email(
                    rsEmails.getString("messageId"),
                    rsEmails.getString("sender"),
                    toList,
                    rsEmails.getString("subject"),
                    rsEmails.getString("body"),
                    attachList,
                    EmailStatus.valueOf(rsEmails.getString("status"))
                );
                email.setRead(rsEmails.getInt("isRead") == 1);
                cachedEmails.add(email);
            }
            
        } catch (SQLException e) {
            System.err.println("JDBC Load Error: " + e.getMessage());
        }
    }

    @Override
    public void saveAll() {
        // No-op for JDBC as we usually save incrementally
    }

    @Override
    public List<User> getUsers() { return cachedUsers; }

    @Override
    public List<Email> getEmails() { return cachedEmails; }

    @Override
    public void addUser(User user) {
        String sql = "INSERT INTO users(name, email, password) VALUES(?,?,?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmailId());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.executeUpdate();
            cachedUsers.add(user);
        } catch (SQLException e) {
            System.err.println("Error adding user: " + e.getMessage());
        }
    }
    
    // UPDATED: Implemented the update logic using SQL
    @Override
    public void updateUser(User user) {
        String sql = "UPDATE users SET name = ?, password = ? WHERE email = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getEmailId());
            pstmt.executeUpdate();
            // In-memory 'cachedUsers' is already updated by reference in EmailClient, 
            // so we don't need to manually update the list here.
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
        }
    }

    @Override
    public void deleteUser(User user) {
        String sqlUser = "DELETE FROM users WHERE email = ?";
        String sqlEmails = "DELETE FROM emails WHERE sender = ?"; // Clean up sent mails
        
        try (Connection conn = connect()) {
            // Delete User
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUser)) {
                pstmt.setString(1, user.getEmailId());
                pstmt.executeUpdate();
            }
            
            // Delete Sent Emails (Optional clean up)
            try (PreparedStatement pstmt = conn.prepareStatement(sqlEmails)) {
                pstmt.setString(1, user.getEmailId());
                pstmt.executeUpdate();
            }
            
            // Update Cache
            cachedUsers.removeIf(u -> u.getEmailId().equalsIgnoreCase(user.getEmailId()));
            cachedEmails.removeIf(e -> e.getFrom().equalsIgnoreCase(user.getEmailId()));
            
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
    }

    @Override
    public void addEmail(Email email) {
        String sql = "INSERT INTO emails(messageId, sender, recipients, subject, body, attachments, timestamp, isRead, status) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.getMessageId());
            pstmt.setString(2, email.getFrom());
            pstmt.setString(3, String.join(",", email.getTo()));
            pstmt.setString(4, email.getSubject());
            pstmt.setString(5, email.getBody());
            pstmt.setString(6, String.join(",", email.getAttachmentPaths()));
            pstmt.setLong(7, email.getTimestamp());
            pstmt.setInt(8, email.isRead() ? 1 : 0);
            pstmt.setString(9, email.getStatus().toString());
            pstmt.executeUpdate();
            cachedEmails.add(email);
        } catch (SQLException e) {
             System.err.println("Error adding email: " + e.getMessage());
        }
    }
    
    @Override
    public String saveAttachment(File file) {
        if (file == null || !file.exists()) return null;
        try {
            String uniqueFilename = UUID.randomUUID().toString() + "-" + file.getName();
            File destFile = new File(ATTACHMENTS_DIR_PATH + uniqueFilename);
            Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return uniqueFilename; 
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public File getAttachment(String uniqueFilename) {
        File file = new File(ATTACHMENTS_DIR_PATH + uniqueFilename);
        return (file.exists() && file.isFile()) ? file : null;
    }
}