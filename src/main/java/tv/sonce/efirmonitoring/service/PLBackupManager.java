package tv.sonce.efirmonitoring.service;

import java.io.*;
import java.util.*;

import tv.sonce.efirmonitoring.service.notifier.impl.GUINotifier;
import tv.sonce.efirmonitoring.service.notifier.impl.MailNotifier;
import tv.sonce.efirmonitoring.service.notifier.Notifier;
import tv.sonce.efirmonitoring.service.notifier.impl.SoundNotifier;

public class PLBackupManager implements Runnable {

    private static final String AS_RUN_REZERV_PASS = "d:\\Borman\\PL\\__PlayListRezerv\\pl_lists\\";
    private static final String AS_RUN_LOCAL_PASS = "d:\\Borman\\pl_lists\\";
    private static final String DOSKY_REZERV_PASS = "d:\\Borman\\PL\\__PlayListRezerv\\Playlist_Dosky\\";
    private static final String EFIR_PLAY_LIST_REZERV_PASS = "d:\\Borman\\PL\\__PlayListRezerv\\EfirPlayList\\";

    private static final String AS_RUN_STORAGE_PASS = "\\\\storage\\Solarmedia\\pl_lists\\";
    private static final String DOSKY_STORAGE_PASS = "\\\\storage\\Solarmedia\\Playlist_Dosky\\";
    private static final String EFIR_PLAY_LIST_STORAGE_PASS = "\\\\storage\\Solarmedia\\EfirPlayList\\";
    private static final String AS_RUN_INMEDIA_PATH = "\\\\inmedia\\AsRunLogs\\";

    private GUINotifier guiNotifier;
    private MailNotifier mailNotifier;
    private SoundNotifier soundNotifier;

    private final Timer timer;

    private Calendar calendar;
    private int currentYear;
    private int currentMonth;
    private int currentDayOfMonth;
    private int currentHourOfDay;

    private Calendar tempCalendar;
    private int tempYear;
    private int tempMonth;
    private int tempDayOfMonth;

    public PLBackupManager(Notifier[] notifiers) {
        for (Notifier notifier : notifiers) {
            if (notifier instanceof GUINotifier)
                guiNotifier = (GUINotifier) notifier;
            if (notifier instanceof MailNotifier)
                mailNotifier = (MailNotifier) notifier;
            if (notifier instanceof SoundNotifier)
                soundNotifier = (SoundNotifier) notifier;
        }
        if (mailNotifier == null)
            mailNotifier = new MailNotifier("borman5433@gmail.com");
        if (guiNotifier == null)
            guiNotifier = new GUINotifier();
        if (soundNotifier == null)
            soundNotifier = new SoundNotifier();

        timer = new Timer("Timer");
        calendar = new GregorianCalendar();
        tempCalendar = new GregorianCalendar();
        new Thread(this).start();
    }

    @Override
    public synchronized void run() {
        while (true) {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (this) {
                        this.notifyAll();
                    }
                }
            };

            updateCurrentDate();

            Date date;
            if (currentHourOfDay <= 7) {
                date = new Date(currentYear - 1900, currentMonth, currentDayOfMonth, 12, 0, 0);
                timer.schedule(timerTask, date);
                guiNotifier.sendMessage("PLBackupManager продолжит работу в " + date);
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {}
                }
                continue;
            }

            if (currentHourOfDay >= 22) {
                date = new Date(currentYear - 1900, currentMonth, currentDayOfMonth + 1, 12, 0, 0);
                timer.schedule(timerTask, date);
                guiNotifier.sendMessage("PLBackupManager продолжит работу в " + date);
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {}
                }
                continue;
            }

            guiNotifier.sendMessage("Запускается PLBackupManager...\n");

            // rezerv folders
            FolderKeeper asRunRezerv = getFolderKeeper(AS_RUN_REZERV_PASS);
            FolderKeeper asRunLocal = getFolderKeeper(AS_RUN_LOCAL_PASS);
            FolderKeeper doskyRezerv = getFolderKeeper(DOSKY_REZERV_PASS);
            FolderKeeper efirPlayListRezerv = getFolderKeeper(EFIR_PLAY_LIST_REZERV_PASS);

            // origin folders
            FolderKeeper asRunStorage = getFolderKeeper(AS_RUN_STORAGE_PASS);
            FolderKeeper doskyStorage = getFolderKeeper(DOSKY_STORAGE_PASS);
            FolderKeeper efirPlayListStorage = getFolderKeeper(EFIR_PLAY_LIST_STORAGE_PASS);
            FolderKeeper asRunInmedia = getFolderKeeper(AS_RUN_INMEDIA_PATH);

            int numOfCopiedFiles = 0;
            int countFilesForDb = 0;

            if ((doskyRezerv != null) && (doskyStorage != null))
                numOfCopiedFiles += syncFolders(doskyRezerv, doskyStorage, numOfCopiedFiles);
            if ((efirPlayListRezerv != null) && (efirPlayListStorage != null))
                numOfCopiedFiles += syncFolders(efirPlayListRezerv, efirPlayListStorage, numOfCopiedFiles);
            if ((asRunRezerv != null) && (asRunStorage != null)) {
                int temp = syncFolders(asRunRezerv, asRunStorage, numOfCopiedFiles);
                numOfCopiedFiles += temp;
                countFilesForDb += temp;
            }
            if ((asRunRezerv != null) && (asRunInmedia != null)) {
                int temp = syncFolders(asRunRezerv, asRunInmedia, numOfCopiedFiles);
                numOfCopiedFiles += temp;
                countFilesForDb += temp;
            }
            if ((asRunLocal != null) && (asRunStorage != null)) {
                int temp = syncFolders(asRunLocal, asRunStorage, numOfCopiedFiles);
                numOfCopiedFiles += temp;
                countFilesForDb += temp;
            }
            if ((asRunLocal != null) && (asRunInmedia != null)) {
                int temp = syncFolders(asRunLocal, asRunInmedia, numOfCopiedFiles);
                numOfCopiedFiles += temp;
                countFilesForDb += temp;
            }

            guiNotifier.sendMessage("На данный момент скопированно на локальный компьютер файлов: " + numOfCopiedFiles);

            if (countFilesForDb > 0) {
                startSavingToDb();
            }

            String[] plListLastFilesArray = new String[10];
            String[] doskiLastFilesArray = new String[5];
            String[] efirPlayListLastFilesArray = new String[10];

            // 2.	Создаем список файлов за последние 10 дней которые должны быть в папке pl_lists (Channel 1_ASRUN201905060600.xml), не включая сегодня
            // str.equals(String.format("Channel 1_ASRUN%d%02d%02d0600.xml", tempYear, tempMonth + 1, tempDayOfMonth));
            for (int i = 0; i < 10; i++) {
                tempCalendar.set(currentYear, currentMonth, currentDayOfMonth - i - 1); // Не включая сегодняшний день
                updateTempCalendar(tempCalendar);
                plListLastFilesArray[i] = String.format("Channel 1_ASRUN%d%02d%02d0600.xml", tempYear, tempMonth + 1, tempDayOfMonth);
            }

            // 4.	Создаем список всех файлов за последние 5 дней которые должны быть в папке Playlist_Dosky (08.05.2019, среда.xls), включая сегодня
            // str.startsWith(String.format("%02d.%02d.%d", tempDayOfMonth, tempMonth + 1, tempYear));
            for (int i = 0; i < 5; i++) {
                tempCalendar.set(currentYear, currentMonth, currentDayOfMonth - i);
                updateTempCalendar(tempCalendar);
                doskiLastFilesArray[i] = String.format("%02d.%02d.%d", tempDayOfMonth, tempMonth + 1, tempYear);
            }

            // 5.	Создаем список всех файлов за последние 10 дней которые должны быть в папке EfirPlayList (190508_Wednesday.xml), включая сегодня
            // str.startsWith(String.format("%02d%02d%02d_", tempYear - 2000, tempMonth + 1, tempDayOfMonth));
            for (int i = 0; i < 10; i++) {
                tempCalendar.set(currentYear, currentMonth, currentDayOfMonth - i);
                updateTempCalendar(tempCalendar);
                efirPlayListLastFilesArray[i] = String.format("%02d%02d%02d_", tempYear - 2000, tempMonth + 1, tempDayOfMonth);
            }

            List<String> listOfAbsentFilesInAsRunStorage = new ArrayList<>();
            List<String> listOfAbsentFilesInAsRunInmedia = new ArrayList<>();

            List<String> messageForMe = new ArrayList<>();
            boolean reloadInOneHour = false;

            // Какие файлы отсутствуют в папке  asRunStoragePass = "\\\\storage\\Solarmedia\\pl_lists\\"
            if(asRunStorage != null) {
                listOfAbsentFilesInAsRunStorage = asRunStorage.getAbsentFilesList(plListLastFilesArray);
            } else {
                String tempMessage = "Нет доступа к " + AS_RUN_STORAGE_PASS + " поэтому нет возможности определить, сохранены плейлисты за предыдущие дни или нет";
                guiNotifier.sendMessage(tempMessage);
                messageForMe.add(tempMessage);
            }


            // Какие файлы отсутствуют в папке  AS_RUN_INMEDIA_PATH = "\\\\inmedia\\AsRunLogs\\" - сообщение всем: эфир за вчера не сохранен
            if (asRunInmedia != null) {
                listOfAbsentFilesInAsRunInmedia = asRunInmedia.getAbsentFilesList(plListLastFilesArray);
            } else {
                String tempMessage = "Нет доступа к " + AS_RUN_INMEDIA_PATH + " поэтому нет возможности определить, сохранены плейлист за предыдущие дни или нет";
                guiNotifier.sendMessage(tempMessage);
                messageForMe.add(tempMessage);
            }

            if(!listOfAbsentFilesInAsRunStorage.isEmpty() || !listOfAbsentFilesInAsRunInmedia.isEmpty()){
                String tempMessage = "";
                reloadInOneHour = true;
                if(!listOfAbsentFilesInAsRunStorage.isEmpty()){
                    tempMessage += "В папке " + AS_RUN_STORAGE_PASS + " не найденные следующие файлы плейлистов: \n";
                    tempMessage += String.join("\n", listOfAbsentFilesInAsRunStorage);
                    tempMessage += "\n";
                }

                if(!listOfAbsentFilesInAsRunInmedia.isEmpty()){
                    tempMessage += "В папке " + AS_RUN_INMEDIA_PATH + " не найденные следующие файлы плейлистов: \n";
                    tempMessage += String.join("\n", listOfAbsentFilesInAsRunInmedia);
                    tempMessage += "\n";
                }
                messageForMe.add(tempMessage);
                guiNotifier.sendMessage(tempMessage);
                soundNotifier.sendMessage("");
            }

            // Cообщение только мне на почту: отчет в целом, что где не сохранилось

            // Какие файлы отсутствуют в папке  AS_RUN_REZERV_PASS = "d:\\Borman\\PL\\__PlayListRezerv\\pl_lists\\"
            if(asRunRezerv != null)
                messageForMe.addAll(absentFilesMessage(asRunRezerv, plListLastFilesArray));

            // Какие файлы отсутствуют в папке  AS_RUN_LOCAL_PASS = "d:\\Borman\\pl_lists\\"
            if (asRunLocal != null)
                messageForMe.addAll(absentFilesMessage(asRunLocal, plListLastFilesArray));

            // Какие файлы отсутствуют в папке  DOSKY_STORAGE_PASS = "\\\\storage\\Solarmedia\\Playlist_Dosky\\"
            if (doskyStorage != null)
                messageForMe.addAll(absentFilesMessage(doskyStorage, doskiLastFilesArray));

            // Какие файлы отсутствуют в папке  DOSKY_REZERV_PASS = "d:\\Borman\\PL\\__PlayListRezerv\\Playlist_Dosky\\"
            if (doskyRezerv != null)
                messageForMe.addAll(absentFilesMessage(doskyRezerv, doskiLastFilesArray));

            // Какие файлы отсутствуют в папке  efirPlayListStoragePass = "\\\\storage\\Solarmedia\\EfirPlayList\\"
            if (efirPlayListStorage != null)
                messageForMe.addAll(absentFilesMessage(efirPlayListStorage, efirPlayListLastFilesArray));

            // Какие файлы отсутствуют в папке  efirPlayListRezervPass = "d:\\Borman\\PL\\__PlayListRezerv\\EfirPlayList\\"
            if (efirPlayListRezerv != null)
                messageForMe.addAll(absentFilesMessage(efirPlayListRezerv, efirPlayListLastFilesArray));

            if (!messageForMe.isEmpty()) { // отправляем почту мне
                mailNotifier.sendMessage(String.join("\n", messageForMe));
            }

            if (reloadInOneHour) { // перезагрузить программу через час
                date = new Date(currentYear - 1900, currentMonth, currentDayOfMonth, currentHourOfDay + 1, 0, 0);
            } else {    // перезагрузить программу в 12:00 следующего дня
                date = new Date(currentYear - 1900, currentMonth, currentDayOfMonth + 1, 12, 0, 0);
            }
            timer.schedule(timerTask, date);
            guiNotifier.sendMessage("PLBackupManager продолжит работу в " + date);

            try {
                wait();
            } catch (InterruptedException e) {}
        }
    }

    private void startSavingToDb() {
        guiNotifier.sendMessage("Запуск программы для добавления плейлистов в БД");    // Запускаем добавление в БД
        String addToDBOne = "java -jar d:\\Borman\\PL\\PLdbAgent\\PLdbAgent.jar";
        String addToDBTwo = "java -jar d:\\Borman\\PL\\PLdbAgent_str\\PLdbAgent.jar";
        guiNotifier.sendMessage(addToDBOne);
        guiNotifier.sendMessage(addToDBTwo);
        executeExtJAR(addToDBOne);
        executeExtJAR(addToDBTwo);
    }

    private FolderKeeper getFolderKeeper(String path) {
        FolderKeeper result = null;
        try {
            result = new FolderKeeper(path);
        } catch (FileNotFoundException e) {
            guiNotifier.sendMessage(e.getLocalizedMessage());
        }
        return result;
    }

    private int syncFolders(FolderKeeper syncTo, FolderKeeper syncFrom, int numOfCopiedFiles){
        try {
            guiNotifier.sendMessage("На данный момент скопированно на локальный компьютер файлов: " + numOfCopiedFiles);
            guiNotifier.sendMessage("Запуск синхронизации " + syncTo + "  из  " + syncFrom);
            return syncTo.syncronizeFrom(syncFrom).size();
        } catch (FileNotFoundException e) {
            guiNotifier.sendMessage(syncTo + " не синхронизирована");
            guiNotifier.sendMessage(e.getLocalizedMessage());
        }
        return 0;
    }

    private List<String> absentFilesMessage(FolderKeeper folderKeeper, String [] lastFiles) {
        List<String> absentFiles = folderKeeper.getAbsentFilesList(lastFiles);
        List<String> messageForMe = new ArrayList<>();
        if (absentFiles.size() != 0) {
            messageForMe.add("В папке " + folderKeeper.toString() + " отсутствуют файлы:");
            messageForMe.addAll(absentFiles);
        } else {
            messageForMe.add("Нет доступа к " + folderKeeper.toString() + " - нет возможности проконтролировать наличие файлов");
        }
        return messageForMe;
    }

    private void executeExtJAR(String command) {
        Process proc;
        try {
            proc = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            guiNotifier.sendMessage("Не удалось выполнить комманду " + command);
            e.printStackTrace();
            return;
        }
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), "CP1251"));
        } catch (UnsupportedEncodingException e) {
            guiNotifier.sendMessage("Не удалось подключиться ко входному потоку при выполнении комманды " + command);
            e.printStackTrace();
            return;
        }
        String temp;
        try {
            while ((temp = reader.readLine()) != null) {
                guiNotifier.sendMessage(temp);
            }
        } catch (IOException e) {
            guiNotifier.sendMessage("Не удалось прочитать данные из входного потока при выполнении команды " + command);
            e.printStackTrace();
        }
    }

    private void updateCurrentDate() {
        calendar = new GregorianCalendar();
        currentYear = calendar.get(Calendar.YEAR);
        currentMonth = calendar.get(Calendar.MONTH);
        currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        currentHourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
    }

    private void updateTempCalendar(Calendar tempCalendar) {
        tempYear = tempCalendar.get(Calendar.YEAR);
        tempMonth = tempCalendar.get(Calendar.MONTH);
        tempDayOfMonth = tempCalendar.get(Calendar.DAY_OF_MONTH);
    }
}
