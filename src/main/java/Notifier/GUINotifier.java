package Notifier;

public class GUINotifier implements Notifier {
    public void sendMessage(String myMessage) {
        System.out.println();
        System.out.println(myMessage);
    }
}
