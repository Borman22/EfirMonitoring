package Notifier;

import java.awt.*;

public class SoundNotifier implements Notifier, Runnable{

    public void sendMessage(String message) {
        new Thread(this).start();
    }

    public void run() {
        for (int i = 0; i < 3; i++) {
            Toolkit tk = Toolkit.getDefaultToolkit();
            tk.beep();
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {}
        }
    }
}

