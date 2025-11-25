import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    String emailId;
    String passwordHash;

    public User(String name, String emailId, String passwordHash) {
        this.name = name;
        this.emailId = emailId;
        this.passwordHash = passwordHash;
    }

    public String getEmailId() { return emailId; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }

    public void setName(String name) { this.name = name; }
    
    public void setPasswordHash(String passwordHash) { 
        this.passwordHash = passwordHash; 
    }

}
