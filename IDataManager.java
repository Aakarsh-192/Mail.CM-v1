import java.io.File;
import java.util.List;

/**
 * IDataManager.java
 * Interface defining the contract for Data Access Objects (DAO).
 * Demonstrates Abstraction and allows for Polymorphism between File and JDBC implementations.
 */
public interface IDataManager {
    // Core Data Operations
    void reloadData();
    void saveAll(); // Useful for file-based, maybe no-op for JDBC (auto-commit)
    
    // User Operations
    List<User> getUsers();
    void addUser(User user);
    void updateUser(User user); // UPDATED: Added method to handle updates
    void deleteUser(User user); // Method to delete a user
    
    // Email Operations
    List<Email> getEmails();
    void addEmail(Email email);
    
    // Attachment Operations
    String saveAttachment(File file);
    File getAttachment(String uniqueFilename);
}