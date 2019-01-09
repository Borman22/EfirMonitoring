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

    // Первые 5 сообщений отправляем с интервалом в 10 сек, остальные в 1 мин. Если ничего не отправлялось в течении 3х минут, все счетчики обнуляем
    // и следующее 5 сообщений опять отправляем с интервалом 10 сек

    private long smallInterval = 10*1000; // ms
    private long largeInterval = 60*1000; // ms
    private long resetInterval = 3*60*1000; // ms
    private int friquentlySentMessages = 5;

    private int messageNumber = 0;
    private long lastMessageTime = System.currentTimeMillis();
    private String myMessage = "";

    private String eMailOfTheRecipient;
    private Properties props;
    private String userName = "borman5433";
    private String password = "borman5433";

    private Thread thread;

    public MailNotifier(String eMailOfTheRecipient){
        this.eMailOfTheRecipient = eMailOfTheRecipient;

        props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        thread = new Thread(this);
        thread.start();
    }

    public synchronized void sendMessage(String myMessage) {
        this.myMessage += "[" + myMessage + "] ";
        notifyAll();
    }

    public synchronized void run() {
        while (true) {
            while (myMessage.equals("")) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }

            if ((System.currentTimeMillis() - lastMessageTime) > resetInterval) {
                messageNumber = 0;
            }

            long deltaTime = (messageNumber < friquentlySentMessages) ? (System.currentTimeMillis() - lastMessageTime - smallInterval) : (System.currentTimeMillis() - lastMessageTime - largeInterval);

            if (deltaTime < 0) {
                try {
                    wait(-deltaTime);
                } catch (InterruptedException e) {
                }
                continue;
            }

            messageNumber++;
            lastMessageTime = System.currentTimeMillis();
            String tempMyMessage = new String(myMessage);
            myMessage = "";

            new Thread(() -> {
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

                    message.setText(tempMyMessage);

                    Transport.send(message);
                    System.out.println("\nE-mail sent to " + eMailOfTheRecipient);
                } catch (MessagingException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }
}