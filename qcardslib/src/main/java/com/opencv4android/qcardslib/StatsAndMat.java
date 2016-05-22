package com.opencv4android.qcardslib;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.LinkedHashMap;

/**
 * Created by abhinav on 20/3/16.
 */
public class StatsAndMat {
    Mat renderMat;
    private LinkedHashMap<Integer, Integer> questionStatsMap = new LinkedHashMap<Integer, Integer>();

    /**
     *
     * @param renderMat rgba matrix to be rendered on screen
     * @param questionStatsMap studentId-option mapping for each card in frame
     */
    public StatsAndMat(Mat renderMat, LinkedHashMap<Integer, Integer> questionStatsMap) {
        this.renderMat =  new Mat(renderMat.width(), renderMat.height(),
                CvType.CV_8UC4);
        this.renderMat = renderMat;
        this.questionStatsMap = questionStatsMap;
    }

    /**
     * @return rgba matrix to be rendered on screen
     */
    public Mat getRenderMat() {
        return renderMat;
    }

    /**
     *
     * @return studentId-option mapping for each card in frame
     */
    public LinkedHashMap<Integer, Integer> getQuestionStatsMap() {
        return questionStatsMap;
    }
}
