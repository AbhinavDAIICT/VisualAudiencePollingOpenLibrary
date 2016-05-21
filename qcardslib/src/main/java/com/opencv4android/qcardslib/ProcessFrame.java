package com.opencv4android.qcardslib;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
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


    private Point getContourCenter(MatOfPoint wrapper) {
        double moment00 = Imgproc.moments(wrapper).get_m00();
        double moment01 = Imgproc.moments(wrapper).get_m01();
        double moment10 = Imgproc.moments(wrapper).get_m10();
        double centerX = moment10 / moment00;
        double centerY = moment01 / moment00;
        return new Point(centerX, centerY);
    }


    private CenterObjects centerFix(Point p1, Mat tmp) {
        int coordinateX=0, coordinateY=0;
        double check = Math.ceil(p1.x);
        int checkX = (int) (check * 10);
        int checkedX = checkX / 10;
        int tempX = checkedX;
        double check2 = Math.ceil(p1.y);
        int checkY = (int) (check2 * 10);
        int checkedY = checkY / 10;
        int tempY = checkedY;
        int left = 0;
        int right = 0;
        int up = 0;
        int down = 0;
        int checkVal = 0;
        if (tmp.get(checkedY, checkedX)[0] == 0) {
            checkVal = 0;
        } else {
            checkVal = 1;
        }
        while (tmp.get(checkedY, checkedX)[0] == checkVal) {
            left++;
            checkedX -= 1;
        }
        checkedX = tempX;

        while (tmp.get(checkedY, checkedX)[0] == checkVal) {
            right++;
            checkedX += 1;
        }
        if (Math.abs(right - left) > 1) {
            checkedX = tempX;
            int pw = tempX + ((right - left) / 2);
            p1.x = pw;
            coordinateX = pw;
        }

        while (tmp.get(checkedY, checkedX)[0] == checkVal) {
            up++;
            checkedY -= 1;
        }
        checkedY = tempY;

        while (tmp.get(checkedY, checkedX)[0] == checkVal) {
            down++;
            checkedY += 1;
        }
        if (Math.abs(up - down) > 1) {
            checkedY = tempY;
            int pw = tempY + ((down - up) / 2);
            p1.y = pw;
            coordinateY = pw;
        }

        Point p = new Point(p1.x, p1.y);
        CenterObjects centerAndPoints = new CenterObjects(p,coordinateX,coordinateY);
        return centerAndPoints;
    }


}
