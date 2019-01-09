package tv.sonce.efirmonitoring.model;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import tv.sonce.efirmonitoring.model.notifier.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EfirMonitor implements Runnable {

    private final int FRAME_WIDTH = 800;
    private final int FRAME_HEIGHT = 600;
    private final long maxFreezeTime = 7*1000; // ms
    private final long timeUntilReboot = 3*60*60*1000; // Раз в 3 часа отключаемся от IP потока и подключаемся снова
    private long timeLastReboot;
    private VideoCapture videoStream;
    private String videoStreamAdr = "http://10.0.4.107:8001/1:0:1:1B08:11:55:320000:0:0:0:";
    private Notifier[] notifiers;


    public EfirMonitor(Notifier[] notifiers){
        timeLastReboot = System.currentTimeMillis();
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_ffmpeg400_64");

        videoStream = new VideoCapture(videoStreamAdr);
//        videoStream = new VideoCapture(0);

        videoStream.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, FRAME_WIDTH);
        videoStream.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, FRAME_HEIGHT);
        this.notifiers = notifiers;
        new Thread(this).start();
    }

    @Override
    public void run() {
        if (!videoStream.isOpened()) {
            System.out.println("Error. I cannot open the video stream");
            videoStream.release();
            return;
        }

        int sensivity = 30;
        double maxArea = 30;
        int index = 0;
        Mat frame = new Mat(FRAME_HEIGHT, FRAME_WIDTH, CvType.CV_8UC3);
        Mat frame_current = new Mat(FRAME_HEIGHT, FRAME_WIDTH, CvType.CV_8UC3);
        Mat frame_previous = new Mat(FRAME_HEIGHT, FRAME_WIDTH, CvType.CV_8UC3);
        Mat frame_result = new Mat(FRAME_HEIGHT, FRAME_WIDTH, CvType.CV_8UC3);
        Mat frame_temp = new Mat(FRAME_HEIGHT, FRAME_WIDTH, CvType.CV_8UC3);
        Size size = new Size(3, 3);
        Mat mat = new Mat();
        Scalar RED = new Scalar(0, 0, 255); //BGR
        Scalar GREEN = new Scalar(0, 255, 0);

//        View view = new View(videoStream, frame_temp); //---------------------------------------------------

        long currentTime = System.currentTimeMillis();
        long previousTime = currentTime;

        System.out.println("The program monitors the broadcast. Allowed freeze frame less than " + maxFreezeTime / 1000 + " seconds ");

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
                if(!isLogoOK(frame))
                    alarmMessage += new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Logo error! ";

                // Раз в 3 часа отключаемся от IP потока и подключаемся снова
                if(System.currentTimeMillis() - timeLastReboot > timeUntilReboot){
                    new GUINotifier().sendMessage("Пересоздадим объект VideoCapture, чтобы пересоздался файл *.tmp ");
                    videoStream.release();
                    videoStream = new VideoCapture(videoStreamAdr);
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

                            Rect r = Imgproc.boundingRect(contours.get(idx));
                            Imgproc.drawContours(frame, contours, idx, RED);
                            Imgproc.rectangle(frame, r.br(), r.tl(), GREEN, 1);
//                            frame.copyTo(frame_temp); //---------------------------------------------------
                        }
                        contour.release();
                    }

                    currentTime = System.currentTimeMillis();
                    if (!foundMovement) {
                        if (currentTime - previousTime > maxFreezeTime) {
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
                alarmMessage += new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Не удалось прочитать кадр с тюнера ";
                videoStream.release();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                videoStream = new VideoCapture(videoStreamAdr);
            }
        }
    }

    private boolean isLogoOK(Mat frame) {
        // Проверка логотипа в трех точках. Точки, в которых будем мерять цвет логотипа
        String alarmMessage;
        double[] point1 = frame.get(92,75);  // point of BGR
        double[] point2 = frame.get(72,61);  // point of BGR
        double[] point3 = frame.get(74,88);  // point of BGR

        // Предельные средние значения: B[0,55] G[187,220] R[232,255]
        int averageB = (int)((point1[0] + point2[0] + point3[0])/3);
        int averageG = (int)((point1[1] + point2[1] + point3[1])/3);
        int averageR = (int)((point1[2] + point2[2] + point3[2])/3);

        return (averageB < 60) && (averageG > 178) && (averageG < 225) && (averageR > 227);
    }
}

