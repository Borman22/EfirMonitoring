package tv.sonce.efirmonitoring.view;

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

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage image = (BufferedImage) HighGui.toBufferedImage(mat);
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);

    }
}

