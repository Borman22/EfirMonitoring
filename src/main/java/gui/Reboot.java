package gui;

public class Reboot implements Runnable{

    String rebootName;
    Reboot(String rebootName){
        this.rebootName = rebootName;
        new Thread(this, rebootName).start();

    }

    public void run() {
        new Main().main(null);
    }
}
