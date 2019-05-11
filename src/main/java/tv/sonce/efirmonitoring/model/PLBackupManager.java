package tv.sonce.efirmonitoring.model;

import java.io.*;
import java.util.*;
import tv.sonce.efirmonitoring.model.notifier.GUINotifier;
import tv.sonce.efirmonitoring.model.notifier.MailNotifier;
import tv.sonce.efirmonitoring.model.notifier.Notifier;

public class PLBackupManager implements Runnable {

    private Notifier[] notifiers;
    private GUINotifier guiNotifier;
    private MailNotifier mailNotifier;

    PLBackupManager currentObject;

    private Timer timer;
    private TimerTask timerTask;
    private Date date;

    private Calendar calendar;
    private int currentYear;
    private int currentMonth;
    private int currentDayOfMonth;
    private int currentHourOfDay;

    private Calendar tempCalendar;
    int tempYear;
    int tempMonth;
    int tempDayOfMonth;

    public PLBackupManager(Notifier[] notifiers) {
        this.notifiers = notifiers;
        for(Notifier notifier : notifiers){
            if(notifier instanceof GUINotifier)
                guiNotifier = (GUINotifier)notifier;
            if(notifier instanceof MailNotifier)
                mailNotifier = (MailNotifier)notifier;
        }
        if(mailNotifier == null)
            mailNotifier = new MailNotifier("borman5433@gmail.com");
        if(guiNotifier == null)
            guiNotifier = new GUINotifier();

        timer = new Timer("Timer");
        calendar = new GregorianCalendar();
        tempCalendar = new GregorianCalendar();
        new Thread(this).start();
    }

    @Override
    synchronized public void run() {
        currentObject = this;

        while (true) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (currentObject) {
                        currentObject.notifyAll();
                    }
                }
            };

            updateCurrentDate();

            if (currentHourOfDay <= 7) {
                date = new Date(currentYear - 1900, currentMonth, currentDayOfMonth, 12, 0, 0);
                timer.schedule(timerTask, date);
                guiNotifier.sendMessage("PLBackupManager продолжит работу в " + date);
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
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
                    } catch (InterruptedException e) {
                    }
                }
                continue;
            }


            guiNotifier.sendMessage("Запускается PLBackupManager...\n");


            String asRunRezervPass = "d:\\Borman\\PL\\__PlayListRezerv\\pl_lists\\";
            String asRunLocalPass = "d:\\Borman\\pl_lists\\";
            String doskyRezervPass = "d:\\Borman\\PL\\__PlayListRezerv\\Playlist_Dosky\\";
            String efirPlayListRezervPass = "d:\\Borman\\PL\\__PlayListRezerv\\EfirPlayList\\";

            String asRunStoragePass = "\\\\storage\\Solarmedia\\pl_lists\\";
            String doskyStoragePass = "\\\\storage\\Solarmedia\\Playlist_Dosky\\";
            String efirPlayListStoragePass = "\\\\storage\\Solarmedia\\EfirPlayList\\";

            String asRunInmediaPass = "\\\\inmedia\\AsRunLogs\\";

            // rezerv folders
            FolderKeeper asRunRezerv;
            FolderKeeper asRunLocal;
            FolderKeeper doskyRezerv;
            FolderKeeper efirPlayListRezerv;

            // origin folders
            FolderKeeper asRunStorage;
            FolderKeeper doskyStorage;
            FolderKeeper efirPlayListStorage;
            FolderKeeper asRunInmedia;

            try {
                asRunRezerv = new FolderKeeper(asRunRezervPass);
            } catch (FileNotFoundException e) {
                guiNotifier.sendMessage(e.getLocalizedMessage());
                asRunRezerv = null;
            }

            try{
                asRunLocal = new FolderKeeper(asRunLocalPass);
            } catch (FileNotFoundException e){
                guiNotifier.sendMessage(e.getLocalizedMessage());
                asRunLocal = null;
            }

            try {
                doskyRezerv = new FolderKeeper(doskyRezervPass);
            } catch (FileNotFoundException e) {
                guiNotifier.sendMessage(e.getLocalizedMessage());
                doskyRezerv = null;
            }

            try {
                efirPlayListRezerv = new FolderKeeper(efirPlayListRezervPass);
            } catch (FileNotFoundException e) {
                guiNotifier.sendMessage(e.getLocalizedMessage());
                efirPlayListRezerv = null;
            }

            try {
                asRunStorage = new FolderKeeper(asRunStoragePass);
            } catch (FileNotFoundException e) {
                guiNotifier.sendMessage(e.getLocalizedMessage());
                asRunStorage = null;
            }

            try {
                doskyStorage = new FolderKeeper(doskyStoragePass);
            } catch (FileNotFoundException e) {
                guiNotifier.sendMessage(e.getLocalizedMessage());
                doskyStorage = null;
            }

            try {
                efirPlayListStorage = new FolderKeeper(efirPlayListStoragePass);
            } catch (FileNotFoundException e) {
                guiNotifier.sendMessage(e.getLocalizedMessage());
                efirPlayListStorage = null;
            }

            try {
                asRunInmedia = new FolderKeeper(asRunInmediaPass);
            } catch (FileNotFoundException e) {
                guiNotifier.sendMessage(e.getLocalizedMessage());
                asRunInmedia = null;
            }

            List<String> listOfCopiedFiles = new ArrayList();
            List<String> tempListOfCopiedFiles;
            int countFiles = 0;

            if((doskyRezerv != null) && (doskyStorage != null))
                try {
                    guiNotifier.sendMessage("На данный момент скопированно на локальный компьютер файлов:" + listOfCopiedFiles.size());
                    guiNotifier.sendMessage("Запуск синхронизации " + doskyRezervPass + "\tиз\t" + doskyStoragePass);

                    listOfCopiedFiles.addAll(doskyRezerv.syncronizeFrom(doskyStorage));
                } catch (FileNotFoundException e) {
                    guiNotifier.sendMessage(doskyRezerv + " не синхронизирована");
                    guiNotifier.sendMessage(e.getLocalizedMessage());
                }

            if((efirPlayListRezerv != null) && (efirPlayListStorage != null))
                try {
                    guiNotifier.sendMessage("На данный момент скопированно на локальный компьютер файлов:" + listOfCopiedFiles.size());
                    guiNotifier.sendMessage("Запуск синхронизации " + efirPlayListRezervPass + "\tиз\t" + efirPlayListStoragePass);

                    listOfCopiedFiles.addAll(efirPlayListRezerv.syncronizeFrom(efirPlayListStorage));
                } catch (FileNotFoundException e) {
                    guiNotifier.sendMessage(efirPlayListRezerv + " не синхронизирована");
                    guiNotifier.sendMessage(e.getLocalizedMessage());
                }

            if((asRunRezerv != null) && (asRunStorage != null))
                try {
                    guiNotifier.sendMessage("На данный момент скопированно на локальный компьютер файлов:" + listOfCopiedFiles.size());
                    guiNotifier.sendMessage("Запуск синхронизации " + asRunRezervPass + "\tиз\t" + asRunStoragePass);

                    tempListOfCopiedFiles = asRunRezerv.syncronizeFrom(asRunStorage);
                    listOfCopiedFiles.addAll(tempListOfCopiedFiles);
                    countFiles += tempListOfCopiedFiles.size();
                } catch (FileNotFoundException e) {
                    guiNotifier.sendMessage(asRunRezerv + " не синхронизирована");
                    guiNotifier.sendMessage(e.getLocalizedMessage());
                }

            if((asRunRezerv != null) && (asRunInmedia != null))
                try {
                    guiNotifier.sendMessage("На данный момент скопированно на локальный компьютер файлов:" + listOfCopiedFiles.size());
                    guiNotifier.sendMessage("Запуск синхронизации " + asRunRezervPass + "\tиз\t" + asRunInmediaPass);

                    tempListOfCopiedFiles = asRunRezerv.syncronizeFrom(asRunInmedia);
                    listOfCopiedFiles.addAll(tempListOfCopiedFiles);
                    countFiles += tempListOfCopiedFiles.size();
                } catch (FileNotFoundException e) {
                    guiNotifier.sendMessage(asRunRezerv + " не синхронизирована");
                    guiNotifier.sendMessage(e.getLocalizedMessage());
                }

            if((asRunLocal != null) && (asRunStorage != null))
                try {
                    guiNotifier.sendMessage("На данный момент скопированно на локальный компьютер файлов:" + listOfCopiedFiles.size());
                    guiNotifier.sendMessage("Запуск синхронизации " + asRunLocalPass + "\tиз\t" + asRunStoragePass);

                    tempListOfCopiedFiles = asRunLocal.syncronizeFrom(asRunStorage);
                    listOfCopiedFiles.addAll(tempListOfCopiedFiles);
                    countFiles += tempListOfCopiedFiles.size();
                } catch (FileNotFoundException e) {
                    guiNotifier.sendMessage(asRunLocal + " не синхронизирована");
                    guiNotifier.sendMessage(e.getLocalizedMessage());
                }

            if((asRunLocal != null) && (asRunInmedia != null))
                try {
                    guiNotifier.sendMessage("На данный момент скопированно на локальный компьютер файлов:" + listOfCopiedFiles.size());
                    guiNotifier.sendMessage("Запуск синхронизации " + asRunLocalPass + "\tиз\t" + asRunInmediaPass);

                    tempListOfCopiedFiles = asRunLocal.syncronizeFrom(asRunInmedia);
                    listOfCopiedFiles.addAll(tempListOfCopiedFiles);
                    countFiles += tempListOfCopiedFiles.size();
                } catch (FileNotFoundException e) {
                    guiNotifier.sendMessage(asRunLocal + " не синхронизирована");
                    guiNotifier.sendMessage(e.getLocalizedMessage());
                }

            guiNotifier.sendMessage("На данный момент скопированно на локальный компьютер файлов:" + listOfCopiedFiles.size() + "\n");




            if (countFiles > 0) {
                guiNotifier.sendMessage("Запуск программы для добавления плейлистов в БД");    // Запускаем добавление в БД
                String addToDBOne = "java -jar d:\\Borman\\PL\\PLdbAgent\\PLdbAgent.jar";
                String addToDBTwo = "java -jar d:\\Borman\\PL\\PLdbAgent_str\\PLdbAgent.jar";
                guiNotifier.sendMessage(addToDBOne);
                guiNotifier.sendMessage(addToDBTwo);
                executeExtJAR(addToDBOne);
                executeExtJAR(addToDBTwo);
            }

            String[] pl_listLastFilesArray = new String[10];
            String[] doskiLastFilesArray = new String[5];
            String[] efirPlayListLastFilesArray = new String[10];

            // 2.	Создаем список файлов за последние 10 дней которые должны быть в папке pl_lists (Channel 1_ASRUN201905060600.xml), не включая сегодня
            // str.equals(String.format("Channel 1_ASRUN%d%02d%02d0600.xml", tempYear, tempMonth + 1, tempDayOfMonth));
            for (int i = 0; i < 10; i++) {
                tempCalendar.set(currentYear, currentMonth, currentDayOfMonth - i - 1); // Не включая сегодняшний день
                updateTempCalendar(tempCalendar);
                pl_listLastFilesArray[i] = String.format("Channel 1_ASRUN%d%02d%02d0600.xml", tempYear, tempMonth + 1, tempDayOfMonth);
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
                listOfAbsentFilesInAsRunStorage = asRunStorage.getAbsentFilesList(pl_listLastFilesArray);
            } else {
                String tempMessage = "Нет доступа к " + asRunStoragePass + " поэтому нет возможности определить, сохранены плейлист за предыдущие дни или нет";
                guiNotifier.sendMessage(tempMessage);
                messageForMe.add(tempMessage);
            }


            // Какие файлы отсутствуют в папке  asRunInmediaPass = "\\\\inmedia\\AsRunLogs\\" - сообщение всем: эфир за вчера не сохранен
            if(asRunInmedia != null) {
                listOfAbsentFilesInAsRunInmedia = asRunInmedia.getAbsentFilesList(pl_listLastFilesArray);
            } else {
                String tempMessage = "Нет доступа к " + asRunInmediaPass + " поэтому нет возможности определить, сохранены плейлист за предыдущие дни или нет";
                guiNotifier.sendMessage(tempMessage);
                messageForMe.add(tempMessage);
            }

            if(listOfAbsentFilesInAsRunStorage.size() + listOfAbsentFilesInAsRunInmedia.size() > 0){
                String tempMessage = new String();
                reloadInOneHour = true;
                if(listOfAbsentFilesInAsRunStorage.size() != 0){
                    tempMessage += "В папке " + asRunStoragePass + " не найденные следующие файлы плейлистов: \n";
                    for (int i = 0; i < listOfAbsentFilesInAsRunStorage.size(); i++) {
                        tempMessage += listOfAbsentFilesInAsRunStorage.get(i) + "\n";
                    }
                }

                if(listOfAbsentFilesInAsRunInmedia.size() != 0){
                    tempMessage += "В папке " + asRunInmediaPass + " не найденные следующие файлы плейлистов: /n";
                    for (int i = 0; i < listOfAbsentFilesInAsRunInmedia.size(); i++) {
                        tempMessage += listOfAbsentFilesInAsRunStorage.get(i) + "/n";
                    }
                }
                messageForMe.add(tempMessage);
                guiNotifier.sendMessage(tempMessage);
            }


            // Cообщение только мне на почту: отчет в целом, что где не сохранилось

            // Какие файлы отсутствуют в папке  asRunRezervPass = "d:\\Borman\\PL\\__PlayListRezerv\\pl_lists\\"
            List<String> listOfAbsentFilesInAsRunRezerv = new ArrayList<>();
            if(asRunRezerv != null) {
                listOfAbsentFilesInAsRunRezerv = asRunRezerv.getAbsentFilesList(pl_listLastFilesArray);
                if(listOfAbsentFilesInAsRunRezerv.size() != 0){
                    messageForMe.add("В папке " + asRunRezervPass + " отсутствуют файлы:");
                    messageForMe.addAll(listOfAbsentFilesInAsRunRezerv);
                }
            } else {
                messageForMe.add("Нет доступа к " + asRunRezervPass + " - нет возможности проконтролировать наличие файлов");
            }

            // Какие файлы отсутствуют в папке  asRunLocalPass = "d:\\Borman\\pl_lists\\"
            List<String> listOfAbsentFilesInAsRunLocal = new ArrayList<>();
            if(asRunLocal != null) {
                listOfAbsentFilesInAsRunLocal = asRunLocal.getAbsentFilesList(pl_listLastFilesArray);
                if(listOfAbsentFilesInAsRunLocal.size() != 0){
                    messageForMe.add("В папке " + asRunLocalPass + " отсутствуют файлы:");
                    messageForMe.addAll(listOfAbsentFilesInAsRunLocal);
                }
            } else {
                messageForMe.add("Нет доступа к " + asRunLocalPass + " - нет возможности проконтролировать наличие файлов");
            }

            // Какие файлы отсутствуют в папке  doskyStoragePass = "\\\\storage\\Solarmedia\\Playlist_Dosky\\"
            List<String> listOfAbsentFilesInDoskyStorage = new ArrayList<>();
            if(doskyStorage != null) {
                listOfAbsentFilesInDoskyStorage = doskyStorage.getAbsentFilesList(doskiLastFilesArray);
                if(listOfAbsentFilesInAsRunLocal.size() != 0){
                    messageForMe.add("В папке " + doskyStoragePass + " отсутствуют файлы:");
                    messageForMe.addAll(listOfAbsentFilesInDoskyStorage);
                }
            } else {
                messageForMe.add("Нет доступа к " + doskyStoragePass + " - нет возможности проконтролировать наличие файлов");
            }

            // Какие файлы отсутствуют в папке  doskyRezervPass = "d:\\Borman\\PL\\__PlayListRezerv\\Playlist_Dosky\\"
            List<String> listOfAbsentFilesInDoskyRezerv = new ArrayList<>();
            if(doskyRezerv != null) {
                listOfAbsentFilesInDoskyRezerv = doskyRezerv.getAbsentFilesList(doskiLastFilesArray);
                if(listOfAbsentFilesInAsRunLocal.size() != 0){
                    messageForMe.add("В папке " + doskyRezervPass + " отсутствуют файлы:");
                    messageForMe.addAll(listOfAbsentFilesInDoskyRezerv);
                }
            } else {
                messageForMe.add("Нет доступа к " + doskyRezervPass + " - нет возможности проконтролировать наличие файлов");
            }

            // Какие файлы отсутствуют в папке  efirPlayListStoragePass = "\\\\storage\\Solarmedia\\EfirPlayList\\"
            List<String> listOfAbsentFilesInEfirPlayListStorage = new ArrayList<>();
            if(efirPlayListStorage != null) {
                listOfAbsentFilesInEfirPlayListStorage = efirPlayListStorage.getAbsentFilesList(efirPlayListLastFilesArray);
                if(listOfAbsentFilesInAsRunLocal.size() != 0){
                    messageForMe.add("В папке " + efirPlayListStoragePass + " отсутствуют файлы:");
                    messageForMe.addAll(listOfAbsentFilesInEfirPlayListStorage);
                }
            } else {
                messageForMe.add("Нет доступа к " + efirPlayListStoragePass + " - нет возможности проконтролировать наличие файлов");
            }

            // Какие файлы отсутствуют в папке  efirPlayListRezervPass = "d:\\Borman\\PL\\__PlayListRezerv\\EfirPlayList\\" - сообщение мне на почту
            List<String> listOfAbsentFilesInEfirPlayListRezerv = new ArrayList<>();
            if(efirPlayListRezerv != null) {
                listOfAbsentFilesInEfirPlayListRezerv = efirPlayListRezerv.getAbsentFilesList(efirPlayListLastFilesArray);
                if(listOfAbsentFilesInAsRunLocal.size() != 0){
                    messageForMe.add("В папке " + doskyRezervPass + " отсутствуют файлы:");
                    messageForMe.addAll(listOfAbsentFilesInEfirPlayListRezerv);
                }
            } else {
                messageForMe.add("Нет доступа к " + efirPlayListRezervPass + " - нет возможности проконтролировать наличие файлов");
            }



            if (messageForMe.size() != 0) { // отправляем почту мне
                StringBuffer stringBuffer = new StringBuffer();
                for (String str : messageForMe) {
                    stringBuffer.append(str).append("\n");
                }
                mailNotifier.sendMessage(stringBuffer.toString());
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
            } catch (InterruptedException e) { }
        }
    }

    private void executeExtJAR(String command) {
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            guiNotifier.sendMessage("Не удалось выполнить комманду " + command);
            e.printStackTrace();
            return;
        }
        BufferedReader reader = null;
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
            return;
        }
    }

    void updateCurrentDate() {
        calendar = new GregorianCalendar();
        currentYear = calendar.get(Calendar.YEAR);
        currentMonth = calendar.get(Calendar.MONTH);
        currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        currentHourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
    }

    void updateTempCalendar(Calendar tempCalendar) {
        tempYear = tempCalendar.get(Calendar.YEAR);
        tempMonth = tempCalendar.get(Calendar.MONTH);
        tempDayOfMonth = tempCalendar.get(Calendar.DAY_OF_MONTH);
    }


}
