package tv.sonce.efirmonitoring.model.impl;

import tv.sonce.efirmonitoring.model.Streamer;

public class DreamBox implements Streamer {

    private String videoStreamAddres = "http://10.0.4.107:8001/1:0:1:1B08:11:55:300000:0:0:0:";

    private int width = 704;
    private int height = 576;

    // Точки, в которых будем мерять цвет логотипа
    private int[][] logoV2Coordinates = {{92, 75}, {72, 61}, {74, 88}};
    // Предельные средние значения: B[0,55] G[187,220] R[232,255]
    private int[][] logoV2BGRColors = {{0, 55}, {187, 220}, {232, 255}};

    private int[][] logoTraurCoordinates = {{92, 75}, {72, 61}, {74, 88}};
    // Предельные средние значения: B[152,255] G[152,255] R[151,255]
    private int[][] logoTraurBGRColors = {{152, 255}, {152, 255}, {151, 255}};

    private int frozenFrameFlow = 742000; // байт в минуту
    private int minimumAverageFlow = 2100000;
    private int averageFlow = 3000000;

    @Override
    public String getVideoStreamAddress() {
        return videoStreamAddres;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int[][] getLogoV2Coordinates() {
        return logoV2Coordinates;
    }

    @Override
    public int[][] getLogoV2BGRColors() {
        return logoV2BGRColors;
    }

    @Override
    public int[][] getLogoTraurCoordinates() {
        return logoTraurCoordinates;
    }

    @Override
    public int[][] getLogoTraurBGRColors() {
        return logoTraurBGRColors;
    }


    // считаем, что минимальный поток 2.6 МБ/с (стоп-кадр 0.742 МБ/с, движение около 3 МБ/с)
    @Override
    public int getFrozenFrameFlow() {
        return frozenFrameFlow;
    }

    @Override
    public int getMinimumAverageFlow() {
        return minimumAverageFlow;
    }

    @Override
    public int getAverageFlow() {
        return averageFlow;
    }
}
