package Notifier;

public abstract class MessageSender implements Runnable{
    public abstract void sendMessage(String message);
}
