import java.io.File;
import java.util.List;

public interface IDataManager {
    void reloadData();
    void saveAll();
    List<User> getUsers();
    void addUser(User user);
    void updateUser(User user);
    void deleteUser(User user);

    List<Email> getEmails();
    void addEmail(Email email);

    String saveAttachment(File file);
    File getAttachment(String uniqueFilename);

}
