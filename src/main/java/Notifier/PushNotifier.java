package Notifier;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public class PushNotifier implements Notifier, Runnable {

    // Первые 5 сообщений отправляем с интервалом в 10 сек, остальные в 1 мин. Если ничего не отправлялось в течении 3х минут, все счетчики обнуляем
    // и следующее 5 сообщений опять отправляем с интервалом 10 сек

    private long smallInterval = 10*1000; // ms
    private long largeInterval = 60*1000; // ms
    private long resetInterval = 3*60*1000; // ms
    private int friquentlySentMessages = 5;

    private int messageNumber = 0;
    private long lastMessageTime = System.currentTimeMillis();
    private String myMessage = "";
    private String url = "https://pushall.ru/api.php?type=broadcast&id=4697&key=22e4b88cc854ad20c5a6bf42b09a432c&title=EfirMonitoringError&text=";
//    private String url = "https://pushall.ru/api.php?type=self&id=80900&key=cdbe8b1d9698396460991f7518f85a2a&text=";
    private Thread thread;

    public PushNotifier(){
        thread = new Thread(this);
        thread.start();
    }

    synchronized public void sendMessage(String myMessage) {

        try {
            this.myMessage += new String (("[" + myMessage + "] ").getBytes("UTF-8"));
            notifyAll();
        } catch (UnsupportedEncodingException e) {
            System.out.println("Не получилось перекодировать сообщение в UTF-8 в классе PushNotifier. Message = " + myMessage);
            this.myMessage = "[Could not convert message to UTF-8] ";
        }

    }

     synchronized public void run() {
        while (true) {
            while (myMessage.equals("")) {
                try {
                    wait();
                } catch (InterruptedException e) { }
            }

            if ((System.currentTimeMillis() - lastMessageTime) > resetInterval) {
                messageNumber = 0;
            }

            long deltaTime = (messageNumber < friquentlySentMessages) ? (System.currentTimeMillis() - lastMessageTime - smallInterval) : (System.currentTimeMillis() - lastMessageTime - largeInterval);

            if(deltaTime < 0){
                try {
                    wait(-deltaTime);
                } catch (InterruptedException e) { }
                continue;
            }

            messageNumber++;
            lastMessageTime = System.currentTimeMillis();
            String tempMyMessage = new String(myMessage);
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