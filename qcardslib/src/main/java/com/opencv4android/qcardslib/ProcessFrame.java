package com.opencv4android.qcardslib;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedHashMap;

/**
 * Created by abhinav on 28/2/16.
 */
public class ProcessFrame {
    private ProcessSettings processSettings;
    private LinkedHashMap<Integer, Integer> questionStatsMap = new LinkedHashMap<Integer, Integer>();
    LinkedHashMap<String, MappedObjects> idMap;
    private int frameCount = 0;

    public ProcessFrame(ProcessSettings processSettings, LinkedHashMap<String, MappedObjects> idMap) {
        this.processSettings = processSettings;
        this.idMap = idMap;
    }

    private Mat filterFrame(Mat gray) {
        Mat grayMat = gray;
        Mat tmpMat1 = grayMat;
        Mat tmpMat2 = grayMat;
        if (processSettings.isAdaptiveBool()) if (frameCount % 3 == 0)
            Imgproc.adaptiveThreshold(tmpMat1, tmpMat1, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 11, 2);
        else {
            Imgproc.threshold(tmpMat2, tmpMat2, 120, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
            Imgproc.threshold(tmpMat2, tmpMat1, 0, 255, Imgproc.THRESH_BINARY_INV);
        }
        else {
            if (frameCount % 3 == 0)
                Imgproc.threshold(tmpMat2, tmpMat2, 120, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
            else if (frameCount % 3 == 1)
                Imgproc.threshold(tmpMat2, tmpMat2, 80, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
            else if (frameCount % 3 == 2)
                Imgproc.threshold(tmpMat2, tmpMat2, 40, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
            Imgproc.threshold(tmpMat2, tmpMat1, 0, 255, Imgproc.THRESH_BINARY_INV);
        }
        if (tmpMat1 != null){
            return tmpMat1;
        }
        else return gray;
    }
}
