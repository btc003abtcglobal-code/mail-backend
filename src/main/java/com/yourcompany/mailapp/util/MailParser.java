package com.yourcompany.mailapp.util;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing email messages
 * Handles MIME message parsing, content extraction, and attachment detection
 */
@Slf4j
@Component
public class MailParser {
    
    /**
     * Extract sender's email address from message
     * @param message email message
     * @return sender email address
     * @throws MessagingException if error occurs
     */
    public String getFromAddress(Message message) throws MessagingException {
        Address[] addresses = message.getFrom();
        if (addresses != null && addresses.length > 0) {
            return ((InternetAddress) addresses[0]).getAddress();
        }
        return null;
    }
    
    /**
     * Extract sender's display name from message
     * @param message email message
     * @return sender display name
     * @throws MessagingException if error occurs
     */
    public String getFromName(Message message) throws MessagingException {
        Address[] addresses = message.getFrom();
        if (addresses != null && addresses.length > 0) {
            String personal = ((InternetAddress) addresses[0]).getPersonal();
            return personal != null ? personal : "";
        }
        return "";
    }
    
    /**
     * Extract "To" recipients as comma-separated string
     * @param message email message
     * @return recipients string
     * @throws MessagingException if error occurs
     */
    public String getToAddresses(Message message) throws MessagingException {
        return getAddressesAsString(message.getRecipients(Message.RecipientType.TO));
    }
    
    /**
     * Extract "CC" recipients as comma-separated string
     * @param message email message
     * @return CC recipients string
     * @throws MessagingException if error occurs
     */
    public String getCcAddresses(Message message) throws MessagingException {
        return getAddressesAsString(message.getRecipients(Message.RecipientType.CC));
    }
    
    /**
     * Extract "BCC" recipients as comma-separated string
     * @param message email message
     * @return BCC recipients string
     * @throws MessagingException if error occurs
     */
    public String getBccAddresses(Message message) throws MessagingException {
        return getAddressesAsString(message.getRecipients(Message.RecipientType.BCC));
    }
    
    /**
     * Convert address array to comma-separated string
     * @param addresses array of email addresses
     * @return comma-separated string
     */
    private String getAddressesAsString(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addresses.length; i++) {
            if (i > 0) sb.append(", ");
            InternetAddress ia = (InternetAddress) addresses[i];
            String personal = ia.getPersonal();
            if (personal != null) {
                sb.append(personal).append(" <").append(ia.getAddress()).append(">");
            } else {
                sb.append(ia.getAddress());
            }
        }
        return sb.toString();
    }
    
    /**
     * Extract subject from message
     * @param message email message
     * @return subject text
     * @throws MessagingException if error occurs
     */
    public String getSubject(Message message) throws MessagingException {
        String subject = message.getSubject();
        if (subject == null || subject.isEmpty()) {
            return "(No Subject)";
        }
        
        // Decode subject if it contains encoded words
        try {
            return MimeUtility.decodeText(subject);
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to decode subject: {}", subject, e);
            return subject;
        }
    }
    
    /**
     * Extract plain text content from message
     * @param part message part
     * @return plain text content
     * @throws MessagingException if error occurs
     * @throws IOException if IO error occurs
     */
    public String getTextContent(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                String text = getTextContent(multipart.getBodyPart(i));
                if (text != null && !text.isEmpty()) {
                    return text;
                }
            }
        } else if (part.isMimeType("message/rfc822")) {
            // Handle nested messages
            Part nestedPart = (Part) part.getContent();
            return getTextContent(nestedPart);
        }
        return "";
    }
    
    /**
     * Extract HTML content from message
     * @param part message part
     * @return HTML content
     * @throws MessagingException if error occurs
     * @throws IOException if IO error occurs
     */
    public String getHtmlContent(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            String html = "";
            
            // First, try to find HTML content
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/html")) {
                    html = (String) bodyPart.getContent();
                    if (!html.isEmpty()) {
                        return html;
                    }
                }
            }
            
            // If no HTML found, recursively search
            for (int i = 0; i < multipart.getCount(); i++) {
                html = getHtmlContent(multipart.getBodyPart(i));
                if (!html.isEmpty()) {
                    return html;
                }
            }
        } else if (part.isMimeType("message/rfc822")) {
            Part nestedPart = (Part) part.getContent();
            return getHtmlContent(nestedPart);
        }
        return "";
    }
    
    /**
     * Extract all attachments from message
     * @param part message part
     * @return list of attachment parts
     * @throws MessagingException if error occurs
     * @throws IOException if IO error occurs
     */
    public List<Part> getAttachments(Part part) throws MessagingException, IOException {
        List<Part> attachments = new ArrayList<>();
        
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                
                // Check if it's an attachment
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                    (bodyPart.getFileName() != null && !bodyPart.getFileName().isEmpty())) {
                    attachments.add(bodyPart);
                } else {
                    // Recursively check nested parts
                    attachments.addAll(getAttachments(bodyPart));
                }
            }
        }
        
        return attachments;
    }
    
    /**
     * Check if message has attachments
     * @param part message part
     * @return true if has attachments
     * @throws MessagingException if error occurs
     * @throws IOException if IO error occurs
     */
    public boolean hasAttachments(Part part) throws MessagingException, IOException {
        return !getAttachments(part).isEmpty();
    }
    
    /**
     * Get attachment filename with proper decoding
     * @param part attachment part
     * @return decoded filename
     * @throws MessagingException if error occurs
     */
    public String getAttachmentFilename(Part part) throws MessagingException {
        String filename = part.getFileName();
        if (filename == null) {
            return "attachment";
        }
        
        try {
            return MimeUtility.decodeText(filename);
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to decode filename: {}", filename, e);
            return filename;
        }
    }
    
    /**
     * Get attachment input stream
     * @param part attachment part
     * @return input stream
     * @throws MessagingException if error occurs
     * @throws IOException if IO error occurs
     */
    public InputStream getAttachmentInputStream(Part part) throws MessagingException, IOException {
        return part.getInputStream();
    }
    
    /**
     * Extract Message-ID header
     * @param message email message
     * @return message ID
     * @throws MessagingException if error occurs
     */
    public String getMessageId(Message message) throws MessagingException {
        if (message instanceof MimeMessage) {
            return ((MimeMessage) message).getMessageID();
        }
        return null;
    }
    
    /**
     * Extract In-Reply-To header
     * @param message email message
     * @return in-reply-to message ID
     * @throws MessagingException if error occurs
     */
    public String getInReplyTo(Message message) throws MessagingException {
        if (message instanceof MimeMessage) {
            String[] headers = ((MimeMessage) message).getHeader("In-Reply-To");
            if (headers != null && headers.length > 0) {
                return headers[0];
            }
        }
        return null;
    }
    
    /**
     * Extract References header
     * @param message email message
     * @return references header value
     * @throws MessagingException if error occurs
     */
    public String getReferences(Message message) throws MessagingException {
        if (message instanceof MimeMessage) {
            String[] headers = ((MimeMessage) message).getHeader("References");
            if (headers != null && headers.length > 0) {
                return headers[0];
            }
        }
        return null;
    }
    
    /**
     * Get message size in bytes
     * @param message email message
     * @return size in bytes
     * @throws MessagingException if error occurs
     */
    public int getMessageSize(Message message) throws MessagingException {
        return message.getSize();
    }
    
    /**
     * Get message priority
     * @param message email message
     * @return priority (1=highest, 3=normal, 5=lowest)
     * @throws MessagingException if error occurs
     */
    public int getMessagePriority(Message message) throws MessagingException {
        if (message instanceof MimeMessage) {
            String[] priority = ((MimeMessage) message).getHeader("X-Priority");
            if (priority != null && priority.length > 0) {
                try {
                    return Integer.parseInt(priority[0]);
                } catch (NumberFormatException e) {
                    return 3; // Normal priority
                }
            }
        }
        return 3; // Default normal priority
    }
    
    /**
     * Check if message is read (SEEN flag)
     * @param message email message
     * @return true if read
     * @throws MessagingException if error occurs
     */
    public boolean isRead(Message message) throws MessagingException {
        return message.isSet(Flags.Flag.SEEN);
    }
    
    /**
     * Check if message is flagged/starred
     * @param message email message
     * @return true if flagged
     * @throws MessagingException if error occurs
     */
    public boolean isFlagged(Message message) throws MessagingException {
        return message.isSet(Flags.Flag.FLAGGED);
    }
    
    /**
     * Check if message is answered/replied
     * @param message email message
     * @return true if answered
     * @throws MessagingException if error occurs
     */
    public boolean isAnswered(Message message) throws MessagingException {
        return message.isSet(Flags.Flag.ANSWERED);
    }
    
    /**
     * Get content type of message
     * @param part message part
     * @return content type
     * @throws MessagingException if error occurs
     */
    public String getContentType(Part part) throws MessagingException {
        return part.getContentType();
    }
    
    /**
     * Extract all email addresses from message (from, to, cc)
     * @param message email message
     * @return list of all email addresses
     * @throws MessagingException if error occurs
     */
    public List<String> getAllEmailAddresses(Message message) throws MessagingException {
        List<String> addresses = new ArrayList<>();
        
        // From
        String from = getFromAddress(message);
        if (from != null && !from.isEmpty()) {
            addresses.add(from);
        }
        
        // To
        Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
        addAddressesToList(toAddresses, addresses);
        
        // CC
        Address[] ccAddresses = message.getRecipients(Message.RecipientType.CC);
        addAddressesToList(ccAddresses, addresses);
        
        return addresses;
    }
    
    /**
     * Helper method to add addresses to list
     */
    private void addAddressesToList(Address[] addresses, List<String> list) {
        if (addresses != null) {
            for (Address address : addresses) {
                if (address instanceof InternetAddress) {
                    list.add(((InternetAddress) address).getAddress());
                }
            }
        }
    }
    
    /**
     * Get message preview (first 200 characters of text content)
     * @param message email message
     * @return preview text
     * @throws MessagingException if error occurs
     * @throws IOException if IO error occurs
     */
    public String getMessagePreview(Message message) throws MessagingException, IOException {
        String text = getTextContent(message);
        if (text == null || text.isEmpty()) {
            text = getHtmlContent(message);
            // Strip HTML tags for preview
            text = text.replaceAll("<[^>]*>", "");
        }
        
        if (text.length() > 200) {
            return text.substring(0, 200) + "...";
        }
        return text;
    }
}