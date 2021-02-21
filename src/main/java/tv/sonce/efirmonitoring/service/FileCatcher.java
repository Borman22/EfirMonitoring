package tv.sonce.efirmonitoring.service;

import tv.sonce.efirmonitoring.service.notifier.Notifier;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FileCatcher implements Runnable {

    private final Notifier[] notifiers;
    private String alarmMessage = "";

    public FileCatcher(Notifier[] notifiers) {
        this.notifiers = notifiers;
        new Thread(this).start();
    }

    @Override
    public synchronized void run() {

        try {
            FileSystem fileSystem = FileSystems.getDefault();
            WatchService service = fileSystem.newWatchService();
            Path path = fileSystem.getPath("d:", "Borman");
            path.register(service, StandardWatchEventKinds.ENTRY_CREATE);
            while (true) {
                sendMessages();

                WatchKey key = service.poll(10, TimeUnit.SECONDS);
                if (key == null) continue;

                key.pollEvents()
                        .stream()
                        .filter(event -> event.kind() != StandardWatchEventKinds.OVERFLOW)
                        .map(event -> (WatchEvent<Path>) event)
                        .forEach(
                                ev -> {
                                    String fileName = ev.context().toString();
                                    switch (fileName) {
                                        case "1":
                                            executeScriptAndDeleteFile("d:\\Borman\\PL\\PLCheck.bat", "d:\\Borman\\1");
                                            break;
                                        case "2":
                                            executeScriptAndDeleteFile("d:\\Borman\\PL\\PLMake.bat", "d:\\Borman\\2");
                                    }
                                }
                        );

                boolean valid = key.reset();
                if (!valid)
                    throw new InterruptedException("Не удалось выполнить Key.reset() в потоке " + Thread.currentThread().getName());
            }

        } catch (IOException | InterruptedException e) {
            alarmMessage = getTime() + " FileCatcher has been stopped ";
        }
    }


    private void sendMessages(){
        if (!alarmMessage.equals("")) {
            for (Notifier notifier : notifiers)
                notifier.sendMessage(alarmMessage);
            alarmMessage = "";
        }
    }

    private void executeScriptAndDeleteFile(String scriptUri, String fileUri) {
        File script = new File(scriptUri);
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.open(script);
            Thread.sleep(3000);
            new File(fileUri).delete();
        } catch (IOException e) {
            alarmMessage += getTime() + " Не удалось удалить файл " + e;
        } catch (InterruptedException e) {
            alarmMessage += getTime() + " Прервали спящий поток " + e;
        }
    }

    private String getTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

}


