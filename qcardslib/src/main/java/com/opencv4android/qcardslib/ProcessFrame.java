package com.opencv4android.qcardslib;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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

    private List<MatOfPoint> getContoursWithChild(Mat filtered) {
        Mat hierarchy = new Mat();
        List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
        List<MatOfPoint> tempContours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(filtered, mContours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        double[] data = new double[hierarchy.rows() * hierarchy.cols() * hierarchy.channels()];
        for (int i = 0; i < mContours.size(); i++) { /* still not clear how hierarchy worked*/
            double[] hVal = hierarchy.get(0, i); /* 3rd entry in hierarchy is child contour - http://docs.opencv.org/master/d9/d8b/tutorial_py_contours_hierarchy.html*/
            int childId = (int) Math.round(hVal[2]);
            if (childId > -1) { /* Add parent to tempContours list*/
                if (!tempContours.contains(mContours.get(i))) tempContours.add(mContours.get(i)); /* Add child to tempContours list*/
                if (!tempContours.contains(mContours.get(childId)))
                    tempContours.add(mContours.get(childId));
            }
        }
        return tempContours;
    }
}
