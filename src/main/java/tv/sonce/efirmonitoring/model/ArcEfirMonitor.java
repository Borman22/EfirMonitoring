package tv.sonce.efirmonitoring.model;

import tv.sonce.efirmonitoring.model.notifier.Notifier;
import tv.sonce.efirmonitoring.model.streamer.Streamer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ArcEfirMonitor implements Runnable{

    private Streamer streamer;

    private String pass = "\\\\arch-efir\\Air_record_SOLAR\\";   // Путь к папке
    private final int intervalMinutes = 3; // Опрашиваем папку раз в 3 минуты

    private String alarmMessage = "";
    private String consoleMessage = "";

    private File oldFile = null;
    private File newFile = null;

    private long oldFileCreate = 0;
    private long oldFileLength = 0;

    private long newFileCreate = 0;
    private long newFileLength = 0;

    private final Notifier[] notifiers;

    public ArcEfirMonitor(Notifier [] notifiers, Streamer streamer){
        this.notifiers = notifiers;
        this.streamer = streamer;
        // Получаем с pass (ARC-EFIR) файл, который создан последним (самый молодой)
        oldFile = getLastestFile(pass);
        new Thread(this).start();
    }

    @Override
    public synchronized void run() {
        if(oldFile == null) {
            System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Cannot connect to " + pass + " ");
            return;
        } else {
            oldFileCreate = oldFile.lastModified();
            oldFileLength = oldFile.length();
        }

        while (true) {
            if(!alarmMessage.equals("")){
                for (Notifier notifier : notifiers) {
                    notifier.sendMessage(alarmMessage);
                }
                alarmMessage = "";
            }

            if(!consoleMessage.equals("")){
                System.out.print(consoleMessage);
                consoleMessage = "";
            }

            try {
                wait(intervalMinutes*60*1000);
            } catch (InterruptedException e) { }

            if(isRecordWorks())
                consoleMessage = " \\\\Arc-efir OK";
        }
    }

    private File getLastestFile(String path){
        // Находим файл, который создан позже всех и возвращаем его из метода
        File[] folderEntries;
        folderEntries = new File(path).listFiles();
        File lastestFile = null;

        if(folderEntries == null)
            return null;

        long tempTimeFileCreate = 0;
        for (File currentFile:folderEntries) {
            if (tempTimeFileCreate < currentFile.lastModified()) {
                tempTimeFileCreate = currentFile.lastModified();
                lastestFile = currentFile;
            }
        }
        return lastestFile;
    }

    private boolean isRecordWorks() {

        // Получаем самый молодой файл.
        newFile = getLastestFile(pass);

        if (newFile == null) {
            alarmMessage += new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Can not connect to " + pass + " ";
            return false;
        }

        newFileCreate = newFile.lastModified();
        newFileLength = newFile.length();

        if (oldFileCreate == newFileCreate) {
            if (oldFileLength < (newFileLength - streamer.getMinimumAverageFlow() * intervalMinutes)) {
                oldFileLength = newFileLength;
                return true;
            } else {
                if (oldFileLength == newFileLength)  // Если объем файла не меняется, то рекордер завис
                    alarmMessage += new SimpleDateFormat("HH:mm:ss").format(new Date()) + " \\\\Arc-efir: The recorder has hung up . oldFileLength = " + oldFileLength + ". newFileLength = " + newFileLength + " ";
                else
                    alarmMessage += new SimpleDateFormat("HH:mm:ss").format(new Date()) + " \\\\Arc-efir: The tuner shows a static picture. oldFileLength = " + oldFileLength + ". newFileLength = " + newFileLength + " ";  // Если тюнер пишет, но с маленьким потоком, то картинка статичная
                oldFileLength = newFileLength;
                return false;
            }
        }

        if (oldFileCreate > newFileCreate) {
            alarmMessage += new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Last created file is lost or connection is lost ";
            return false;
        }

        oldFile = newFile;
        oldFileLength = newFileLength;
        oldFileCreate = newFileCreate;
        return true;

    }
}
