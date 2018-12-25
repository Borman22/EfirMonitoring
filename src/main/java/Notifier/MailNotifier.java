package Notifier;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailNotifier implements Notifier, Runnable {

    private String eMailOfTheRecipient;
    private Properties props;
    private String userName = "borman5433";
    private String password = "borman5433";

    private String myMessage;

    public MailNotifier(String eMailOfTheRecipient){
        this.eMailOfTheRecipient = eMailOfTheRecipient;

        props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
    }

    public void sendMessage(String myMessage) {
        this.myMessage = myMessage;
        new Thread(this).start();
    }

    public void run() {
        Session session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(userName, password);
                    }
                });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("borman5433@gmail.com"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(eMailOfTheRecipient));
            message.setSubject("EfirMonitoringError");

            message.setText(myMessage);

            Transport.send(message);
            System.out.println("\nE-mail sent to " + eMailOfTheRecipient);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}