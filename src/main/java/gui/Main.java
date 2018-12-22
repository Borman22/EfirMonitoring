package gui;

import Notifier.MailSender;
import Notifier.MessageSender;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/* TODO
 1. Сделать контроль наличия логотипа
 2. Дать возможность выбора, на какую почту отправлять сообщения (или Push уведомления)
 3. Дать возможность запускать в фоновом или полном режиме
 4. Сделать выбор за чем следить: обычный логотип, новогодний логотип, траурный (траур + новый год), свечка.

  1000. Контролировать знак 16+
  2000. Подумать, как программа может сама включать логотип.

  */
public class Main {

    public static void main(String[] args) {
        int FRAME_WIDTH = 800;
        int FRAME_HEIGHT = 600;
        int maxFreezeTime = 7000; // ms

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_ffmpeg400_64");

        String videoStreamAdr = "http://10.0.4.107:8001/1:0:1:1B08:11:55:320000:0:0:0:";

//        VideoCapture videoStream = new VideoCapture(0);
        VideoCapture videoStream = new VideoCapture(videoStreamAdr);

        videoStream.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, FRAME_WIDTH);
        videoStream.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, FRAME_HEIGHT);

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
        Scalar scalar1 = new Scalar(0, 0, 255); //BGR
        Scalar scalar2 = new Scalar(0, 255, 0);

//        View view = new View(videoStream, frame_temp); //-----------------

        long currentTime = System.currentTimeMillis();
        long previousTime = currentTime;

        System.out.println("The program monitors the broadcast. Allowed freeze frame less than " + maxFreezeTime / 1000 + " seconds ");

        int counter = 0;
        int frameCounter = 0;

        MessageSender messageSender = new MailSender("borman5433@gmail.com");

        while (true) {
            if (videoStream.read(frame)) {

//*
                if(++frameCounter != 25)    // анализируем каждый двадцать пятый кадр
                    continue;
                frameCounter = 0;

                System.out.print('.'); // Рисуем точки на экране, чтобы было видно, что программа работает
                if (++counter == 180) {
                    System.out.print(new SimpleDateFormat("\nHH:mm:ss").format(new Date())); // Перенос строки
                    counter = 0;
                }
//*/
                frame.copyTo(frame_current);

                Imgproc.GaussianBlur(frame_current, frame_current, size, 0);

                if (index > 1) {
                    Core.subtract(frame_previous, frame_current, frame_result);
                    Imgproc.cvtColor(frame_result, frame_result, Imgproc.COLOR_RGB2GRAY);
                    Imgproc.threshold(frame_result, frame_result, sensivity, 255, Imgproc.THRESH_BINARY);

                    List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                    Imgproc.findContours(frame_result, contours, mat, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                    mat.release();

                    boolean foundMovement = false;
                    for (int idx = 0; idx < contours.size(); idx++) {
                        Mat contour = contours.get(idx);
                        double contourarea = Imgproc.contourArea(contour);
                        if (contourarea > maxArea) {
                            foundMovement = true;

                            Rect r = Imgproc.boundingRect(contours.get(idx));
                            Imgproc.drawContours(frame, contours, idx, scalar1);
                            Imgproc.rectangle(frame, r.br(), r.tl(), scalar2, 1);
                            frame.copyTo(frame_temp);

                        }
                        contour.release();
                    }

                    currentTime = System.currentTimeMillis();
                    if (!foundMovement) {
                        if (currentTime - previousTime > maxFreezeTime) {
                            java.awt.Toolkit tk = Toolkit.getDefaultToolkit();
                            tk.beep();
                            messageSender.sendMessage("Warning! Video frozen");
                            System.out.println(new SimpleDateFormat("\ndd.MM.yyyy HH:mm:ss").format(new Date()) + "   -   Warning! Video frozen");
                            counter = 0;
                            previousTime = currentTime = System.currentTimeMillis();
                        }
                    } else {
                        previousTime = currentTime;
                    }
                }
//                        view.repaint(); //----------

                index++;

                frame_current.copyTo(frame_previous);
                frame.release();
                frame_result.release();
                frame_current.release();

            } else {
                messageSender.sendMessage("Кадр не прочитан - скорее всего завис ffmpeg. Перезапускаем main() в другом потоке");
                System.out.println("Кадр не прочитан - скорее всего завис ffmpeg");
                System.out.println("Перезапустим main() другом потоке, а этот поток " + Thread.currentThread().getName() + " завершим");
                new Reboot(Thread.currentThread().getName() + "_1");
                Thread.currentThread().stop();
            }
        }
    }
}
