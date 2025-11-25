import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FileDataManager.java
 * Implementation of IDataManager using Serialized Files.
 * Demonstrates Inheritance (Object -> FileDataManager) and Polymorphism.
 */
public class FileDataManager implements IDataManager {
    private List<User> users;
    private List<Email> emails;
    
    private static final String DB_DIR = "database";
    private static final String USERS_FILE_PATH = DB_DIR + "/users.db";
    private static final String EMAILS_FILE_PATH = DB_DIR + "/emails.db";
    private static final String ATTACHMENTS_DIR_PATH = DB_DIR + "/attachments/"; 

    public FileDataManager() {
        try {
            Files.createDirectories(Paths.get(DB_DIR));
            Files.createDirectories(Paths.get(ATTACHMENTS_DIR_PATH)); 
        } catch (IOException e) {
            System.err.println("Error creating database directory: " + e.getMessage());
        }
        reloadData();
    }

    private <T> List<T> loadData(String filename) {
        // Synchronization for Thread Safety
        synchronized (this) {
            File file = new File(filename);
            if (!file.exists()) return new ArrayList<>();
            
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                @SuppressWarnings("unchecked")
                List<T> data = (List<T>) ois.readObject();
                return data;
            } catch (EOFException e) {
                return new ArrayList<>();
            } catch (IOException | ClassNotFoundException e) {
                 System.err.println("Error loading data from " + filename + ": " + e.getMessage());
                return new ArrayList<>(); 
            }
        }
    }

    private <T> void saveData(String filename, List<T> data) {
        synchronized (this) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeObject(data);
            } catch (IOException e) {
                System.err.println("Error saving data to " + filename + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void reloadData() {
        users = loadData(USERS_FILE_PATH);
        emails = loadData(EMAILS_FILE_PATH);
        if (users == null) users = new ArrayList<>();
        if (emails == null) emails = new ArrayList<>();
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
            System.err.println("Error saving attachment: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public File getAttachment(String uniqueFilename) {
        File file = new File(ATTACHMENTS_DIR_PATH + uniqueFilename);
        return (file.exists() && file.isFile()) ? file : null;
    }

    @Override
    public List<User> getUsers() { return users; }
    @Override
    public List<Email> getEmails() { return emails; }

    @Override
    public void addUser(User user) {
        users.add(user);
        saveData(USERS_FILE_PATH, users);
    }
    
    // UPDATED: Implemented update logic (just resaves the file)
    @Override
    public void updateUser(User user) {
        // Since 'user' is a reference to an object already in the 'users' list,
        // we just need to save the list to disk.
        saveAll();
    }

    @Override
    public void deleteUser(User user) {
        // Remove the user
        users.removeIf(u -> u.getEmailId().equalsIgnoreCase(user.getEmailId()));
        
        // Ideally, we should also remove emails sent by this user or where they are the sole recipient.
        // For now, removing the user prevents login.
        emails.removeIf(e -> e.getFrom().equalsIgnoreCase(user.getEmailId()));
        
        saveAll();
    }

    @Override
    public void addEmail(Email email) {
        emails.add(email);
        saveData(EMAILS_FILE_PATH, emails);
    }

    @Override
    public void saveAll() {
        saveData(EMAILS_FILE_PATH, emails);
        saveData(USERS_FILE_PATH, users);
    }
}