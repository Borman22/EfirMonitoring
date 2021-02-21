package tv.sonce.efirmonitoring.service;

import tv.sonce.efirmonitoring.service.notifier.Notifier;
import tv.sonce.efirmonitoring.model.Streamer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ArcEfirMonitor implements Runnable {

    private static final String PASS = "\\\\arch-efir\\Air_record_SOLAR\\";   // Путь к папке
    private static final int INTERVAL_MINUTES = 3; // Опрашиваем папку раз в 3 минуты
    private final Notifier[] notifiers;
    private Streamer streamer;
    private String alarmMessage = "";
    private String consoleMessage = "";
    private File oldFile;
    private long oldFileCreate = 0;
    private long oldFileLength = 0;

    public ArcEfirMonitor(Notifier[] notifiers, Streamer streamer) {
        this.notifiers = notifiers;
        this.streamer = streamer;
        // Получаем с pass (ARC-EFIR) файл, который создан последним (самый молодой)
        oldFile = getLatestFile(PASS);
        new Thread(this).start();
    }

    @Override
    public synchronized void run() {
        if (oldFile == null) {
            System.out.println(getTime() + " Cannot connect to " + PASS + " ");
            return;
        } else {
            oldFileCreate = oldFile.lastModified();
            oldFileLength = oldFile.length();
        }

        while (true) {
            if (!alarmMessage.equals("")) {
                for (Notifier notifier : notifiers) {
                    notifier.sendMessage(alarmMessage);
                }
                alarmMessage = "";
            }

            if (!consoleMessage.equals("")) {
                System.out.print(consoleMessage);
                consoleMessage = "";
            }

            try {
                wait(INTERVAL_MINUTES * 60L * 1000);
            } catch (InterruptedException ignored) {
            }

            if (isRecordWorks())
                consoleMessage = " \\\\Arc-efir OK";
        }
    }

    private String getTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    private File getLatestFile(String path) {
        // Находим файл, который создан позже всех и возвращаем его из метода
        File[] folderEntries;
        folderEntries = new File(path).listFiles();
        File lastestFile = null;

        if (folderEntries == null)
            return null;

        long tempTimeFileCreate = 0;
        for (File currentFile : folderEntries) {
            if (tempTimeFileCreate < currentFile.lastModified() && !currentFile.isDirectory()) {
                tempTimeFileCreate = currentFile.lastModified();
                lastestFile = currentFile;
            }
        }
        return lastestFile;
    }

    private boolean isRecordWorks() {

        // Получаем самый молодой файл.
        File newFile = getLatestFile(PASS);

        if (newFile == null) {
            alarmMessage += getTime() + " Can not connect to " + PASS + " ";
            return false;
        }

        long newFileCreate = newFile.lastModified();
        long newFileLength = newFile.length();

        if (oldFileCreate == newFileCreate) {
            if (oldFileLength < (newFileLength - (long) streamer.getMinimumAverageFlow() * INTERVAL_MINUTES)) {
                oldFileLength = newFileLength;
                return true;
            } else {
                if (oldFileLength == newFileLength)  // Если объем файла не меняется, то рекордер завис
                    alarmMessage += getTime() + " \\\\Arc-efir: Рекордер завис. Размер старого файла = (" + oldFile.getAbsolutePath() + ") = " + oldFileLength + ". newFileLength = (" + newFile.getAbsolutePath() + ") = " + newFileLength + " ";
                else
                    alarmMessage += getTime() + " \\\\Arc-efir: Тюнер показывает статичную картинку. Размер старого файла = (" + oldFile.getAbsolutePath() + ") = " + oldFileLength + ". newFileLength = (" + newFile.getAbsolutePath() + ") = " + newFileLength + " ";  // Если тюнер пишет, но с маленьким потоком, то картинка статичная
                oldFileLength = newFileLength;
                return false;
            }
        }

        if (oldFileCreate > newFileCreate) {
            alarmMessage += getTime() + " Last created file is lost or connection is lost ";
            return false;
        }

        oldFile = newFile;
        oldFileLength = newFileLength;
        oldFileCreate = newFileCreate;
        return true;

    }
}
