package Notifier;

import java.awt.*;

public class SoundNotifier implements Notifier, Runnable{

    // будем воспроизводить звук не чаще, чем раз в 7 сек
    private long lastMessageTime = System.currentTimeMillis();
    private Thread thread;
    private boolean makeSoundFlag = false;
    private long messageInterval = 7*1000;

    public SoundNotifier(){
        thread = new Thread(this);
        thread.start();
    }

    public synchronized void sendMessage(String message) {
        makeSoundFlag = true;
        notifyAll();
    }

    public synchronized void run() {

        while (true) {
            while (!makeSoundFlag){
                try {
                    wait();
                } catch (InterruptedException e) { }
            }

            long deltaTime = System.currentTimeMillis() - lastMessageTime - messageInterval;

            if(deltaTime < 0){
                try {
                    wait(-deltaTime);
                } catch (InterruptedException e) { }
                continue;
            }

            makeSoundFlag = false;
            lastMessageTime = System.currentTimeMillis();

            Toolkit tk = Toolkit.getDefaultToolkit();
            new Thread(() -> {
                for (int i = 0; i < 3; i++) {
                    tk.beep();

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) { }
                }
            }).start();
        }
    }
}



