package Notifier;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public class PushNotifier implements Notifier, Runnable {

    private String myMessage;
    private String url = "https://pushall.ru/api.php?type=broadcast&id=4697&key=22e4b88cc854ad20c5a6bf42b09a432c&title=EfirMonitoringError&text=";

    public void sendMessage(String myMessage) {
        try {
            this.myMessage = new String (myMessage.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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