package tv.sonce.efirmonitoring.service.notifier.impl;

import tv.sonce.efirmonitoring.service.notifier.Notifier;

public class GUINotifier implements Notifier {
    public void sendMessage(String myMessage) {
        System.out.println();
        System.out.println(myMessage);
    }
}
