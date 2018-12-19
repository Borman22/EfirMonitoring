package gui;

import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class View extends JComponent {
    VideoCapture videoStream;
    Mat mat;
    JFrame jFrame;

    public View(VideoCapture videoStream, Mat mat) {
        this.videoStream = videoStream;
        this.mat = mat;
        jFrame = new JFrame();
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.add(this);
        jFrame.setSize(800, 600);
        jFrame.setVisible(true);
    }

//    public BufferedImage Mat2BufferedImage(Mat m) {
//
//        int type = BufferedImage.TYPE_BYTE_GRAY;
//        if (m.channels() > 1) {
//            type = BufferedImage.TYPE_3BYTE_BGR;
//        }
//        int bufferSize = m.channels() * m.cols() * m.rows();
//        byte[] b = new byte[bufferSize];
//        m.get(0, 0, b); // get all the pixels
//        BufferedImage img = new BufferedImage(m.cols(), m.rows(), type);
//        final byte[] targetPixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
//        System.arraycopy(b, 0, targetPixels, 0, b.length);
//        return img;
//
//
//    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);



//        BufferedImage image = Mat2BufferedImage(mat);
        BufferedImage image = (BufferedImage) HighGui.toBufferedImage(mat);
        //Mat gray = turnGray(mat);
        //MatOfRect objects = new MatOfRect();
        //CascadeClassifier cas = new CascadeClassifier();
        //cas.detectMultiScale(gray,objects);
        //Mat thresh  = threash( gray);

        //BufferedImage image = Mat2BufferedImage(thresh);
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);

    }
//
//    public Mat turnGray(Mat img){
//        Mat mat1 = new Mat();
//        Imgproc.cvtColor(img, mat1, Imgproc.COLOR_RGB2GRAY);
//        return mat1;
//    }
//
//    public Mat threash(Mat img) {
//        Mat threshed = new Mat();
//        int SENSITIVITY_VALUE = 100;
//        Imgproc.threshold(img, threshed, SENSITIVITY_VALUE, 255, Imgproc.THRESH_BINARY);
//        return threshed;
//    }
}

