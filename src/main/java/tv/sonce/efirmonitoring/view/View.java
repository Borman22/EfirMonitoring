package tv.sonce.efirmonitoring.view;

import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static javax.swing.JFrame.EXIT_ON_CLOSE;

public class View extends JComponent {
    transient VideoCapture videoStream;
    transient Mat mat;
    JFrame jFrame;

    public View(VideoCapture videoStream, Mat mat) {
        this.videoStream = videoStream;
        this.mat = mat;
        jFrame = new JFrame();
        jFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        jFrame.add(this);
        jFrame.setSize(mat.width() + 50, mat.height() + 50);
        jFrame.setVisible(true);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage image = (BufferedImage) HighGui.toBufferedImage(mat);
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);

    }
}

