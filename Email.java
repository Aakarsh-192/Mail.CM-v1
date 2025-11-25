import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

enum EmailStatus {
    INBOX, ARCHIVED, DELETED, DRAFT, SENT 
}

enum ViewType {
    INBOX, DRAFTS, SENT, ARCHIVE, DELETED, SETTINGS
}

public class Email implements Serializable {
    private static final long serialVersionUID = 1L;
    String messageId;
    String from;
    List<String> to;
    String subject;
    String body;
    List<String> attachmentPaths; 
    long timestamp;
    boolean isRead;
    EmailStatus status; 

    public Email(String from, List<String> to, String subject, String body, List<String> attachmentPaths, EmailStatus status) {
        this.messageId = UUID.randomUUID().toString();
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.attachmentPaths = (attachmentPaths != null) ? attachmentPaths : new ArrayList<>(); 
        this.timestamp = System.currentTimeMillis();
        this.isRead = (status == EmailStatus.DRAFT || status == EmailStatus.SENT);
        this.status = status;
    }
    
    public Email(String messageId, String from, List<String> to, String subject, String body, List<String> attachmentPaths, EmailStatus status) {
        this.messageId = messageId;
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.attachmentPaths = (attachmentPaths != null) ? attachmentPaths : new ArrayList<>(); 
        this.timestamp = System.currentTimeMillis();
        this.isRead = (status == EmailStatus.DRAFT || status == EmailStatus.SENT);
        this.status = status;
    }

    public String getMessageId() { return messageId; }
    public List<String> getTo() { return to; }
    public String getFrom() { return from; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public EmailStatus getStatus() { return status; }
    public List<String> getAttachmentPaths() { return attachmentPaths; } 

    public void setRead(boolean read) { isRead = read; }
    public void setStatus(EmailStatus status) { this.status = status; }

    public boolean isRecipient(String userEmail) {
        return to.stream().anyMatch(addr -> addr.equalsIgnoreCase(userEmail));
    }

}
