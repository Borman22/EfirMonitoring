package tv.sonce.efirmonitoring;

import tv.sonce.efirmonitoring.model.Streamer;
import tv.sonce.efirmonitoring.model.impl.DreamBox;
import tv.sonce.efirmonitoring.service.ArcEfirMonitor;
import tv.sonce.efirmonitoring.service.EfirMonitor;
import tv.sonce.efirmonitoring.service.FileCatcher;
import tv.sonce.efirmonitoring.service.PLBackupManager;
import tv.sonce.efirmonitoring.service.notifier.*;
import tv.sonce.efirmonitoring.service.notifier.impl.GUINotifier;
import tv.sonce.efirmonitoring.service.notifier.impl.MailNotifier;
import tv.sonce.efirmonitoring.service.notifier.impl.PushNotifier;
import tv.sonce.efirmonitoring.service.notifier.impl.SoundNotifier;

public class MainController {

    public static void main(String[] args) {

        Notifier[] notifiers = new Notifier[]{
                new MailNotifier("borman5433@gmail.com"),
                new PushNotifier(),
                new SoundNotifier(),
                new GUINotifier()
        };

        Streamer streamer = new DreamBox();
//        Streamer streamer = new Zgemma();

        new EfirMonitor(notifiers, streamer);
        new ArcEfirMonitor(notifiers, streamer);
        new PLBackupManager(notifiers);
        new FileCatcher(notifiers);

    }
}
