package tv.sonce.efirmonitoring.service.notifier.impl;

import tv.sonce.efirmonitoring.service.notifier.Notifier;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PushNotifier implements Notifier, Runnable {

    // Первые 5 сообщений отправляем с интервалом в 10 сек, остальные в 1 мин. Если ничего не отправлялось в течении 3х минут, все счетчики обнуляем
    // и следующее 5 сообщений опять отправляем с интервалом 10 сек

    private static final long smallInterval = 10 * 1000L; // ms
    private static final long largeInterval = 60 * 1000L; // ms
    private static final long resetInterval = 3 * 60 * 1000L; // ms
    private static final int friquentlySentMessages = 5;

    private int messageNumber = 0;
    private long lastMessageTime = 0;
    private String myMessage = "";
    private String url = "https://pushall.ru/api.php?type=broadcast&id=4697&key=22e4b88cc854ad20c5a6bf42b09a432c&title=EfirMonitoringError&text=";
    //    private String url = "https://pushall.ru/api.php?type=self&id=80900&key=cdbe8b1d9698396460991f7518f85a2a&text=";
    private Thread thread;

    public PushNotifier() {
        thread = new Thread(this);
        thread.start();
    }

    public synchronized void sendMessage(String myMessage) {

        this.myMessage += new String(("[" + myMessage + "] ").getBytes(StandardCharsets.UTF_8));
        notifyAll();

    }

    public synchronized void run() {
        while (true) {
            while (myMessage.equals("")) {
                try {
                    wait();
                } catch (InterruptedException e) {}
            }

            if ((System.currentTimeMillis() - lastMessageTime) > resetInterval) {
                messageNumber = 0;
            }

            long deltaTime = (messageNumber < friquentlySentMessages) ? (System.currentTimeMillis() - lastMessageTime - smallInterval) : (System.currentTimeMillis() - lastMessageTime - largeInterval);

            if (deltaTime < 0) {
                try {
                    wait(-deltaTime);
                } catch (InterruptedException e) {}
                continue;
            }

            messageNumber++;
            lastMessageTime = System.currentTimeMillis();
            String tempMyMessage = myMessage;
            myMessage = "";

            new Thread(() -> {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(url + tempMyMessage).openConnection();
                    connection.getResponseCode();  // Пока мы не обращаемся к объекту connection он реально не создается
                    connection.disconnect();
                } catch (IOException e) {
                    System.out.println("Не удалось создать объект URL с аргументом " + url + " \n" + e);
                    myMessage = "[Could not make URL] ";
                }
            }).start();
        }
    }
}