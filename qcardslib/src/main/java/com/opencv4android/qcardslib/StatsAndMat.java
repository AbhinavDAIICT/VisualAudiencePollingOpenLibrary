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

    public StatsAndMat(Mat renderMat, LinkedHashMap<Integer, Integer> questionStatsMap) {
        this.renderMat =  new Mat(renderMat.width(), renderMat.height(),
                CvType.CV_8UC4);
        this.renderMat = renderMat;
        this.questionStatsMap = questionStatsMap;
    }

    public Mat getRenderMat() {
        return renderMat;
    }

    public LinkedHashMap<Integer, Integer> getQuestionStatsMap() {
        return questionStatsMap;
    }
}
