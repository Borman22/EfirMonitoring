package tv.sonce.efirmonitoring.model.notifier;

import java.util.Properties;
import javax.mail.*;
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
    private long lastMessageTime = 0;
    private String myMessage = "";

    private String eMailOfTheRecipient;
    private Properties props;
    private String userName = "borman22@ukr.net";
    private String password = "O7Z0iKFTHfruFKIO";

    private Thread thread;

    public MailNotifier(String eMailOfTheRecipient){
        this.eMailOfTheRecipient = eMailOfTheRecipient;

        props = new Properties();
        props.put("mail.smtp.host", "smtp.ukr.net");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465"); // 465, 587

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
                        new Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(userName, password);
                            }
                        });
                try {
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress("borman22@ukr.net")); //techno.solarmedia@gmail.com
                    message.setRecipients(Message.RecipientType.TO,
                            InternetAddress.parse(eMailOfTheRecipient));
                    message.setSubject("EfirMonitoringError");

                    message.setText(tempMyMessage);

                    Transport.send(message);
                    System.out.println("\nE-mail sent to " + eMailOfTheRecipient);
                } catch (MessagingException e) {
                    System.out.println("Не удалось отправить сообщение. \n" + e);
                }
            }).start();
        }
    }
}