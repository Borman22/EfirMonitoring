package tv.sonce.efirmonitoring.model;

import tv.sonce.efirmonitoring.model.notifier.Notifier;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FileCatcher implements Runnable{

    private String alarmMessage = "";
    private final Notifier[] notifiers;

    public FileCatcher(Notifier [] notifiers){
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

                if(!alarmMessage.equals("")){
                    for (Notifier notifier : notifiers)
                        notifier.sendMessage(alarmMessage);
                    alarmMessage = "";
                }

                WatchKey key = service.poll(10, TimeUnit.SECONDS);
                if (key == null) continue;

                key.pollEvents().stream().filter((event) -> !(event.kind() == StandardWatchEventKinds.OVERFLOW))
                        .map((event) -> (WatchEvent<Path>) event).forEach((ev) -> {
                    Path filename = ev.context();


                    if (filename.toString().equals("1")) {
                        File file = new File("d:\\Borman\\PL\\PLCheck.bat");
                        Desktop desktop = Desktop.getDesktop();
                        try {
                            desktop.open(file);
                            Thread.sleep(3000);
                            new File("d:\\Borman\\1").delete();
                        } catch (IOException e) {
                            alarmMessage = new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Не удалось удалить файл " + e;
                        } catch (InterruptedException e) {
                            alarmMessage = new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Прервали спящий поток " + e;
                        }
                    }


                    if (filename.toString().equals("2")) {
                        File file = new File("d:\\Borman\\PL\\PLMake.bat");
                        Desktop desktop = Desktop.getDesktop();
                        try {
                            desktop.open(file);
                            Thread.sleep(3000);
                            new File("d:\\Borman\\2").delete();
                        } catch (IOException e) {
                            alarmMessage = new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Не удалось удалить файл " + e;
                        } catch (InterruptedException e) {
                            alarmMessage = new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Прервали спящий поток " + e;
                        }
                    }
                });

                boolean valid = key.reset();
                if (!valid)
                    throw new InterruptedException("Не удалось выполнить Key.reset() в потоке " + Thread.currentThread().getName());
            }

        } catch (IOException | InterruptedException e) {
            alarmMessage = new SimpleDateFormat("HH:mm:ss").format(new Date()) + " FileCatcher has been stopped ";
        }
    }

}


