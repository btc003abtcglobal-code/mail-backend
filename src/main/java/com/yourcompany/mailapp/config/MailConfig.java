package com.yourcompany.mailapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuration class for email sending functionality
 * Configures JavaMailSender beans for SMTP operations
 */
@Configuration
public class MailConfig {
    
    @Value("${spring.mail.host}")
    private String mailHost;
    
    @Value("${spring.mail.port}")
    private Integer mailPort;
    
    @Value("${spring.mail.username}")
    private String mailUsername;
    
    @Value("${spring.mail.password}")
    private String mailPassword;
    
    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private Boolean smtpAuth;
    
    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private Boolean starttlsEnable;
    
    @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
    private Boolean starttlsRequired;
    
    @Value("${spring.mail.properties.mail.debug:false}")
    private Boolean mailDebug;
    
    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:5000}")
    private Integer connectionTimeout;
    
    @Value("${spring.mail.properties.mail.smtp.timeout:5000}")
    private Integer timeout;
    
    @Value("${spring.mail.properties.mail.smtp.writetimeout:5000}")
    private Integer writeTimeout;
    
    /**
     * Default JavaMailSender bean for system emails
     * Uses configuration from application.yml
     * 
     * @return configured JavaMailSender
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Basic configuration
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);
        
        // Mail properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", smtpAuth);
        props.put("mail.smtp.starttls.enable", starttlsEnable);
        props.put("mail.smtp.starttls.required", starttlsRequired);
        props.put("mail.debug", mailDebug);
        
        // Timeout settings
        props.put("mail.smtp.connectiontimeout", connectionTimeout);
        props.put("mail.smtp.timeout", timeout);
        props.put("mail.smtp.writetimeout", writeTimeout);
        
        // Security settings
        props.put("mail.smtp.ssl.trust", mailHost);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        
        return mailSender;
    }
    
    /**
     * Create a custom JavaMailSender for specific mail account
     * This method allows creating mail senders with different configurations
     * for each user's mail account
     * 
     * @param host SMTP host
     * @param port SMTP port
     * @param username SMTP username
     * @param password SMTP password
     * @param useTls whether to use TLS
     * @return configured JavaMailSender
     */
    public JavaMailSender createMailSender(String host, Integer port, String username, 
                                          String password, Boolean useTls) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Basic configuration
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        
        // Mail properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", useTls.toString());
        props.put("mail.smtp.starttls.required", useTls.toString());
        props.put("mail.debug", "false");
        
        // Timeout settings
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        
        // Security settings
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        
        return mailSender;
    }
    
    /**
     * Create JavaMailSender with SSL/TLS configuration
     * 
     * @param host SMTP host
     * @param port SMTP port (typically 465 for SSL, 587 for TLS)
     * @param username SMTP username
     * @param password SMTP password
     * @param useSsl whether to use SSL (true) or TLS (false)
     * @return configured JavaMailSender
     */
    public JavaMailSender createMailSenderWithSsl(String host, Integer port, String username, 
                                                  String password, Boolean useSsl) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        
        if (useSsl) {
            // SSL Configuration (port 465)
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", host);
            props.put("mail.smtp.socketFactory.port", port.toString());
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        } else {
            // TLS Configuration (port 587)
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.trust", host);
        }
        
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.debug", "false");
        
        // Timeout settings
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        
        return mailSender;
    }
    
    /**
     * Create JavaMailSender for Gmail
     * Pre-configured with Gmail SMTP settings
     * 
     * @param username Gmail email address
     * @param password Gmail app password (not regular password)
     * @return configured JavaMailSender for Gmail
     */
    public JavaMailSender createGmailSender(String username, String password) {
        return createMailSenderWithSsl("smtp.gmail.com", 587, username, password, false);
    }
    
    /**
     * Create JavaMailSender for Outlook/Hotmail
     * Pre-configured with Outlook SMTP settings
     * 
     * @param username Outlook email address
     * @param password Outlook password
     * @return configured JavaMailSender for Outlook
     */
    public JavaMailSender createOutlookSender(String username, String password) {
        return createMailSenderWithSsl("smtp-mail.outlook.com", 587, username, password, false);
    }
    
    /**
     * Create JavaMailSender for Yahoo
     * Pre-configured with Yahoo SMTP settings
     * 
     * @param username Yahoo email address
     * @param password Yahoo app password
     * @return configured JavaMailSender for Yahoo
     */
    public JavaMailSender createYahooSender(String username, String password) {
        return createMailSenderWithSsl("smtp.mail.yahoo.com", 587, username, password, false);
    }
    
    /**
     * Create JavaMailSender for Office 365
     * Pre-configured with Office 365 SMTP settings
     * 
     * @param username Office 365 email address
     * @param password Office 365 password
     * @return configured JavaMailSender for Office 365
     */
    public JavaMailSender createOffice365Sender(String username, String password) {
        return createMailSenderWithSsl("smtp.office365.com", 587, username, password, false);
    }
    
    /**
     * Test mail server connection
     * 
     * @param mailSender JavaMailSender to test
     * @return true if connection successful
     */
    public boolean testConnection(JavaMailSender mailSender) {
        try {
            JavaMailSenderImpl sender = (JavaMailSenderImpl) mailSender;
            sender.testConnection();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get mail properties for debugging
     * 
     * @param mailSender JavaMailSender
     * @return Properties object
     */
    public Properties getMailProperties(JavaMailSender mailSender) {
        if (mailSender instanceof JavaMailSenderImpl) {
            return ((JavaMailSenderImpl) mailSender).getJavaMailProperties();
        }
        return new Properties();
    }
    
    /**
     * Create JavaMailSender with custom properties
     * 
     * @param host SMTP host
     * @param port SMTP port
     * @param username SMTP username
     * @param password SMTP password
     * @param customProperties additional custom properties
     * @return configured JavaMailSender
     */
    public JavaMailSender createMailSenderWithCustomProperties(String host, Integer port, 
                                                               String username, String password,
                                                               Properties customProperties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        
        // Start with default properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        
        // Add custom properties
        if (customProperties != null) {
            props.putAll(customProperties);
        }
        
        return mailSender;
    }
    
    /**
     * Get default SMTP properties
     * Useful for creating consistent configurations
     * 
     * @return default Properties
     */
    public Properties getDefaultSmtpProperties() {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.debug", "false");
        return props;
    }
    
    /**
     * Create JavaMailSender for custom mail server
     * With full configuration options
     * 
     * @param host SMTP host
     * @param port SMTP port
     * @param username SMTP username
     * @param password SMTP password
     * @param protocol Protocol (smtp/smtps)
     * @param auth Enable authentication
     * @param starttls Enable STARTTLS
     * @param sslEnable Enable SSL
     * @param debug Enable debug mode
     * @return configured JavaMailSender
     */
    public JavaMailSender createFullyConfiguredMailSender(String host, Integer port, 
                                                          String username, String password,
                                                          String protocol, Boolean auth,
                                                          Boolean starttls, Boolean sslEnable,
                                                          Boolean debug) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setProtocol(protocol);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", protocol);
        props.put("mail.smtp.auth", auth.toString());
        props.put("mail.smtp.starttls.enable", starttls.toString());
        props.put("mail.smtp.starttls.required", starttls.toString());
        props.put("mail.smtp.ssl.enable", sslEnable.toString());
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.debug", debug.toString());
        
        // Timeout settings
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        
        return mailSender;
    }
    
    // Getters for configuration values (useful for debugging)
    
    public String getMailHost() {
        return mailHost;
    }
    
    public Integer getMailPort() {
        return mailPort;
    }
    
    public String getMailUsername() {
        return mailUsername;
    }
    
    public Boolean isSmtpAuthEnabled() {
        return smtpAuth;
    }
    
    public Boolean isStarttlsEnabled() {
        return starttlsEnable;
    }
    
    public Boolean isMailDebugEnabled() {
        return mailDebug;
    }
}
