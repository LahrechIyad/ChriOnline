package server.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Handles sending emails via Gmail SMTP using Jakarta Mail.
 */
public class EmailService {
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    
    private String senderEmail;
    private String senderAppPassword;

    private Session session;

    public EmailService() {
        // Load secrets from .env file
        Dotenv dotenv = Dotenv.load();
        this.senderEmail = dotenv.get("GMAIL_EMAIL");
        this.senderAppPassword = dotenv.get("GMAIL_APP_PASSWORD");

        if (this.senderEmail == null || this.senderAppPassword == null) {
            System.err.println("WARNING: GMAIL_EMAIL or GMAIL_APP_PASSWORD not set in .env file!");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderAppPassword);
            }
        });
    }

    public boolean sendRegistrationVerification(String recipientEmail, String verificationCode) {
        String subject = "Verify your ChriOnline account";
        String content = "Welcome to ChriOnline!\n\nPlease verify your account by entering the following code: " + verificationCode + "\n\nThank you!";
        return sendEmail(recipientEmail, subject, content);
    }
    
    public boolean sendOrderSummary(String recipientEmail, int orderId, double amount, String method) {
        String subject = "ChriOnline - Order Confirmation #" + orderId;
        String content = "Thank you for your order!\n\nOrder ID: " + orderId + "\nTotal Amount: $" + amount 
                         + "\nPayment Method: " + method + "\nStatus: VALIDATED\n\nWe are processing your order.";
        return sendEmail(recipientEmail, subject, content);
    }

    private boolean sendEmail(String recipientEmail, String subject, String content) {
        if (senderEmail == null || senderAppPassword == null) {
            System.err.println("Email ignored - Missing credentials configured in .env");
            return false;
        }
        
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setText(content);

            Transport.send(message);
            System.out.println("Email sent successfully to " + recipientEmail);
            return true;
        } catch (MessagingException e) {
            System.err.println("Failed to send email to " + recipientEmail + ". Error: " + e.getMessage());
            return false;
        }
    }
}
