package tv.sonce.efirmonitoring.controller;

import tv.sonce.efirmonitoring.model.ArcEfirMonitor;
import tv.sonce.efirmonitoring.model.EfirMonitor;
import tv.sonce.efirmonitoring.model.FileCatcher;
import tv.sonce.efirmonitoring.model.PLBackupManager;
import tv.sonce.efirmonitoring.model.notifier.*;
import tv.sonce.efirmonitoring.model.streamer.DreamBox;
import tv.sonce.efirmonitoring.model.streamer.Streamer;
import tv.sonce.efirmonitoring.model.streamer.Zgemma;

/* TODO
 1.
 2. Дать возможность выбора, на какую почту отправлять сообщения (или Push уведомления)
 3.
 4.
 5.
 6.

  1000. Контролировать знак 16+
  1001. Контролировать свечку + серый логотип

  */
public class MainController {

    public static void main(String[] args) {

        Notifier[] notifiers = new Notifier[]{
                new MailNotifier("borman5433@gmail.com"),
                new PushNotifier(),
                new SoundNotifier(),
                new GUINotifier()
        };

//        Streamer streamer = new DreamBox();
        Streamer streamer = new Zgemma();

        new EfirMonitor(notifiers, streamer);
        new ArcEfirMonitor(notifiers, streamer);
        new PLBackupManager(notifiers);
        new FileCatcher(notifiers);

    }
}
