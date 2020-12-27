package tv.sonce.efirmonitoring.model;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import tv.sonce.efirmonitoring.model.notifier.*;
import tv.sonce.efirmonitoring.model.streamer.Streamer;
import tv.sonce.efirmonitoring.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EfirMonitor implements Runnable {

    private final long MAX_FREEZE_TIME = 7*1000; // ms
    private final long TIME_UNTIL_REBOOT = 4*60*60*1000; // Раз в 4 часа отключаемся от IP потока и подключаемся снова
    private long timeLastReboot;
    private VideoCapture videoStream;
    private Streamer streamer;
    private Notifier[] notifiers;
    
    private final int MAX_COUNT_OF_DROPPED_FRAMES = 5;    // сколько потерянных кадров
    private int countOfDroppedFrames = 0;
    private final long MAX_TIME_DROPPED_FRAMES = 5*1000; // за сколько времени мерять потерянные кадры
    private long lastTimeDroppedFrame = 0;
    private final long TIME_FOR_SLEEPING_WHEN_FRAMES_DROPPED = 5*1000;



    public EfirMonitor(Notifier[] notifiers, Streamer streamer){
        timeLastReboot = System.currentTimeMillis();
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_ffmpeg400_64");

        this.streamer = streamer;
        videoStream = new VideoCapture(streamer.getVideoStreamAddress());
//        videoStream = new VideoCapture(0);

        videoStream.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, streamer.getWidth());
        videoStream.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, streamer.getHeight());
        this.notifiers = notifiers;
        new Thread(this).start();
    }

    @Override
    public void run() {
        if (!videoStream.isOpened()) {
            System.out.println("Error. I cannot open the video stream. Module 'EfirMonitor' is stopped.");
            videoStream.release();
            return;
        }

        int sensivity = 30;
        double maxArea = 30;
        int index = 0;
        Mat frame = new Mat(streamer.getHeight(), streamer.getWidth(), CvType.CV_8UC3);
        Mat frame_current = new Mat(streamer.getHeight(), streamer.getWidth(), CvType.CV_8UC3);
        Mat frame_previous = new Mat(streamer.getHeight(), streamer.getWidth(), CvType.CV_8UC3);
        Mat frame_result = new Mat(streamer.getHeight(), streamer.getWidth(), CvType.CV_8UC3);
        Mat frame_temp = new Mat(streamer.getHeight(), streamer.getWidth(), CvType.CV_8UC3);
        Size size = new Size(3, 3);
        Mat mat = new Mat();
        Scalar RED = new Scalar(0, 0, 255); //BGR
        Scalar GREEN = new Scalar(0, 255, 0);

//        View view = new View(videoStream, frame_temp); //---------------------------------------------------

        long currentTime = System.currentTimeMillis();
        long previousTime = currentTime;

        System.out.println("The program monitors the broadcast. Allowed freeze frame less than " + MAX_FREEZE_TIME / 1000 + " seconds ");

        int counter = 0;
        int frameCounter = 0;

        String alarmMessage = "";

        while (true) {
            if(!alarmMessage.equals("")){
                for (Notifier notifier : notifiers) {
                    notifier.sendMessage(alarmMessage);
                }
                alarmMessage = "";
            }

            if (videoStream.read(frame)) {

                if(++frameCounter != 25)    // анализируем каждый двадцать пятый кадр
                    continue;
                frameCounter = 0;

                System.out.print('.'); // Рисуем точки на экране (max 180 шт), чтобы было видно, что программа работает
                if (++counter == 180) {
                    System.out.print(new SimpleDateFormat("\nHH:mm:ss").format(new Date())); // Перенос строки
                    counter = 0;
                }

                // Проверка наличия логотипа
                boolean logoV2Present = isLogoV2Present(frame);
                boolean logoTraurPresent = isLogoTraurPresent(frame);
//                boolean candlePresent = isCandlePresent(frame);
//
                if(!(logoV2Present || logoTraurPresent))
                    alarmMessage += new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Logo is missing! ";


                // Раз в 4 часа отключаемся от IP потока и подключаемся снова
                if(System.currentTimeMillis() - timeLastReboot > TIME_UNTIL_REBOOT){
                    new GUINotifier().sendMessage("Переподключимся к тюнеру, чтобы пересоздался файл *.tmp ");
                    videoStream.release();
                    videoStream = new VideoCapture(streamer.getVideoStreamAddress());
                    timeLastReboot = System.currentTimeMillis();
                    continue;
                }

                frame.copyTo(frame_current);
                Imgproc.GaussianBlur(frame_current, frame_current, size, 0);

                if (index > 1) {
                    Core.subtract(frame_previous, frame_current, frame_result);
                    Imgproc.cvtColor(frame_result, frame_result, Imgproc.COLOR_RGB2GRAY);
                    Imgproc.threshold(frame_result, frame_result, sensivity, 255, Imgproc.THRESH_BINARY);

                    List<MatOfPoint> contours = new ArrayList<>();
                    Imgproc.findContours(frame_result, contours, mat, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                    mat.release();

                    boolean foundMovement = false;
                    for (int idx = 0; idx < contours.size(); idx++) {
                        Mat contour = contours.get(idx);
                        double contourArea = Imgproc.contourArea(contour);
                        if (contourArea > maxArea) {
                            foundMovement = true;

//                            Rect r = Imgproc.boundingRect(contours.get(idx));
//                            Imgproc.drawContours(frame, contours, idx, RED);
//                            Imgproc.rectangle(frame, r.br(), r.tl(), GREEN, 1);
//                            frame.copyTo(frame_temp); //---------------------------------------------------
                        }
                        contour.release();
                    }

                    currentTime = System.currentTimeMillis();
                    if (!foundMovement) {
                        if (currentTime - previousTime > MAX_FREEZE_TIME) {
                            alarmMessage += new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Video frozen ";
                            previousTime = currentTime;
                        }
                    } else {
                        previousTime = currentTime;
                    }
                }
//                        view.repaint(); //---------------------------------------------------

                index++;

                frame_current.copyTo(frame_previous);
                frame.release();
                frame_result.release();
                frame_current.release();

            } else {
                // Будем реагировать не на каждый потерянный кадр, а только если потерялись M кадров за N сек
                countOfDroppedFrames++;
                if(lastTimeDroppedFrame == 0){
                    lastTimeDroppedFrame = System.currentTimeMillis();
                    continue;
                }

                if(System.currentTimeMillis() - lastTimeDroppedFrame > MAX_TIME_DROPPED_FRAMES){
                    countOfDroppedFrames = 1;
                    lastTimeDroppedFrame = System.currentTimeMillis();
                    continue;
                }

                if(countOfDroppedFrames >= MAX_COUNT_OF_DROPPED_FRAMES) {
                    alarmMessage += new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Не удалось прочитать кадр с тюнера ";
                    for (Notifier notifier : notifiers) {
                        notifier.sendMessage(alarmMessage);
                    }
                    alarmMessage = "";
                    videoStream.release();
                    try {
                        Thread.sleep(TIME_FOR_SLEEPING_WHEN_FRAMES_DROPPED);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    videoStream = new VideoCapture(streamer.getVideoStreamAddress());

                    countOfDroppedFrames = 0;
                    lastTimeDroppedFrame = 0;
                }
            }
        }
    }

    private boolean isLogoV2Present(Mat frame) {
        // Проверка логотипа в трех точках. Точки, в которых будем мерять цвет логотипа
        double[] point1 = frame.get(streamer.getLogoV2Coordinates()[0][0],streamer.getLogoV2Coordinates()[0][1]);  // point of BGR
        double[] point2 = frame.get(streamer.getLogoV2Coordinates()[1][0],streamer.getLogoV2Coordinates()[1][1]);  // point of BGR
        double[] point3 = frame.get(streamer.getLogoV2Coordinates()[2][0],streamer.getLogoV2Coordinates()[2][1]);  // point of BGR

        int averageB = (int)((point1[0] + point2[0] + point3[0])/3);
        int averageG = (int)((point1[1] + point2[1] + point3[1])/3);
        int averageR = (int)((point1[2] + point2[2] + point3[2])/3);

//        return (averageB < 60) && (averageG > 178) && (averageG < 225) && (averageR > 227);
        return (averageB > streamer.getLogoV2BGRColors()[0][0] - 5) && (averageB < streamer.getLogoV2BGRColors()[0][1] + 5)
                && (averageG > streamer.getLogoV2BGRColors()[1][0] - 5) && (averageG < streamer.getLogoV2BGRColors()[1][1] + 5)
                && (averageR > streamer.getLogoV2BGRColors()[2][0] - 5) && (averageR < streamer.getLogoV2BGRColors()[2][1] + 5);
    }

    private boolean isLogoTraurPresent(Mat frame) {
        // Проверка траурного логотипа в трех точках. Точки, в которых будем мерять цвет логотипа
        double[] point1 = frame.get(streamer.getLogoV2Coordinates()[0][0],streamer.getLogoV2Coordinates()[0][1]);  // point of BGR
        double[] point2 = frame.get(streamer.getLogoV2Coordinates()[1][0],streamer.getLogoV2Coordinates()[1][1]);  // point of BGR
        double[] point3 = frame.get(streamer.getLogoV2Coordinates()[2][0],streamer.getLogoV2Coordinates()[2][1]);  // point of BGR

        int averageB = (int)((point1[0] + point2[0] + point3[0])/3);
        int averageG = (int)((point1[1] + point2[1] + point3[1])/3);
        int averageR = (int)((point1[2] + point2[2] + point3[2])/3);
//        return (averageB > 140) && (averageG > 140) && (averageR > 140);

        return (averageB > streamer.getLogoTraurBGRColors()[0][0] - 5) && (averageB < streamer.getLogoTraurBGRColors()[0][1] + 5)
                && (averageG > streamer.getLogoTraurBGRColors()[1][0] - 5) && (averageG < streamer.getLogoTraurBGRColors()[1][1] + 5)
                && (averageR > streamer.getLogoTraurBGRColors()[2][0] - 5) && (averageR < streamer.getLogoTraurBGRColors()[2][1] + 5);
    }

}

