package Notifier;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class PushNotifier implements Notifier, Runnable {

    private String myMessage;
    private String url = "https://pushall.ru/api.php?type=self&id=80900&key=cdbe8b1d9698396460991f7518f85a2a&text=";

    public void sendMessage(String myMessage) {
        this.myMessage = myMessage;
        new Thread(this).start();
    }

    public void run() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url + myMessage).openConnection();
            connection.getResponseCode();  // Пока мы не обращаемся к объекту connection он реально не создается
            connection.disconnect();
        } catch (IOException e) {
            System.out.println("Не удалось создать объект URL с аргументом " + url + " \n" + e);
        }
    }
}