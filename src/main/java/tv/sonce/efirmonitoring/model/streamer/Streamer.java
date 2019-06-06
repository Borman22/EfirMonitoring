package tv.sonce.efirmonitoring.model.streamer;

public interface Streamer {
    String getVideoStreamAddress();

    int getWidth();
    int getHeight();

    int[][] getLogoV2Coordinates(); // row, col трех точек
    int[][] getLogoV2BGRColors(); // max, min среднего арифметического в 3 точках

    int[][] getLogoTraurCoordinates(); // row, col трех точек
    int[][] getLogoTraurBGRColors(); // max, min среднего арифметического в 3 точках

    int getFrozenFrameFlow();
    int getMinimumAverageFlow();
    int getAverageFlow();

}
