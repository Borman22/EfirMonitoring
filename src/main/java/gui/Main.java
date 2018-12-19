package gui;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        int FRAME_WIDTH = 800;
        int FRAME_HEIGHT = 600;
        int maxFreezeTime = 1000; // ms

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_ffmpeg400_64");

        String videoStreamAdr = "http://10.0.4.107:8001/1:0:1:1B08:11:55:320000:0:0:0:";

        VideoCapture videoStream = new VideoCapture(0);
//        VideoCapture videoStream = new VideoCapture(videoStreamAdr);

        videoStream.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, FRAME_WIDTH);
        videoStream.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, FRAME_HEIGHT);

        if (!videoStream.isOpened()) {
            System.out.println("Error");
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
        Scalar scalar1 = new Scalar(0, 0, 255);
        Scalar scalar2 = new Scalar(0, 255, 0);

        View view = new View(videoStream, frame_temp); //-----------------

        long currentTime = System.currentTimeMillis();
        long previousTime = currentTime;

        System.out.println("Program is working");

        int counter = 0;
        int counter2 = 0;
        while (true) {
            if (videoStream.read(frame)) {
                if (++counter == 200) {
                    System.out.println("counter = " + ++counter2);
                    counter = 0;
                }
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

                            view.repaint(); //----------
                        }
                        contour.release();
                    }

                    currentTime = System.currentTimeMillis();
                    if (!foundMovement) {
                        if (currentTime - previousTime > maxFreezeTime) {
//                                MailSender.sendMail("Video frozen", "borman5433@gmail.com");
                            java.awt.Toolkit tk = Toolkit.getDefaultToolkit();
                            tk.beep();
                            System.out.println("Video frozen");
                            previousTime = currentTime = System.currentTimeMillis();
                        }
                    } else {
                        previousTime = currentTime;

                    }
                }

                index++;

                frame_current.copyTo(frame_previous);
                frame.release();
                frame_result.release();
                frame_current.release();

            }
        }
    }
}  