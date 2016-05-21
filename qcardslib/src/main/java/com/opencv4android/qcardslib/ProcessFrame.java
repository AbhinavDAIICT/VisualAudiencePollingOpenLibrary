package com.opencv4android.qcardslib;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
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
    private List<GoodSquare> populateUsefulContourList(List<MatOfPoint> tempContours, Mat filtered, Mat colorMat) {
        List<GoodSquare> goodSquareList = new ArrayList<GoodSquare>();
        Iterator<MatOfPoint> each1 = tempContours.iterator();
        while (each1.hasNext()) {
            MatOfPoint wrapper = each1.next();
            MatOfPoint2f rotatedRectangleMat = new MatOfPoint2f();
            MatOfPoint wrapRotate = wrapper;
            wrapRotate.convertTo(rotatedRectangleMat, CvType.CV_32FC2);
            RotatedRect rotated = Imgproc.minAreaRect(rotatedRectangleMat); /* Calculate important parameters for contours size contains width and height of the rotated rectangle*/
            Size sizeRect = rotated.size;
            if ((sizeRect.height / sizeRect.width) > 0.7 && (sizeRect.height / sizeRect.width) < 1.3) {
                if ((sizeRect.height * sizeRect.width) < 1.4 * Imgproc.contourArea(wrapper)) {
                    Point detectedCenter = getContourCenter(wrapper);
                    CenterObjects centerAndPoints = centerFix(detectedCenter, filtered);
                    Point fixedCenter = centerAndPoints.getCenter();
                    int fixedCenterX = centerAndPoints.getCenterX();
                    int fixedCenterY = centerAndPoints.getCenterY();
                    if (filtered.get(fixedCenterY, fixedCenterX)[0] == 0) {
                        double perimeter = Imgproc.arcLength(rotatedRectangleMat, true);
                        double area = Imgproc.contourArea(wrapper);
                        GoodSquare goodSquare = new GoodSquare();
                        goodSquare.area = area;
                        goodSquare.perimeter = perimeter;
                        goodSquare.detectedCenter = detectedCenter;
                        goodSquare.fixedCenter = fixedCenter;
                        goodSquare.fixedCenterX = fixedCenterX;
                        goodSquare.fixedCenterY = fixedCenterY;
                        goodSquareList.add(goodSquare);
                    }
                }
            }
        }
        return goodSquareList;
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
    private Point getCentroid(Point p1, Point p2, Point p3) {
        double cx = (p1.x + p2.x + p3.x) / 3;
        double cy = (p1.y + p2.y + p3.y) / 3;
        return new Point(cx, cy);
    }
    private Point[] rearrangePoints(Point p1, Point p2, Point p3, Point hole) {
        Point pivot = new Point();
        if ((hole.x - 2) < (p1.x + p3.x) / 2
                && (p1.x + p3.x) / 2 < (hole.x + 2)
                && (hole.y - 2) < (p1.y + p3.y) / 2
                && (p1.y + p3.y) / 2 < (hole.y + 2)) {
            pivot = p2;
            hole.x = (p1.x + p3.x) / 2;
            hole.y = (p1.y + p3.y) / 2;

        }
        if ((hole.x - 2) < (p2.x + p3.x) / 2
                && (p2.x + p3.x) / 2 < (hole.x + 2)
                && (hole.y - 2) < (p2.y + p3.y) / 2
                && (p2.y + p3.y) / 2 < (hole.y + 2)) {
            pivot = p1;
            hole.x = (p2.x + p3.x) / 2;
            hole.y = (p2.y + p3.y) / 2;
            p1 = p2;
            p2 = pivot;
        }
        if ((hole.x - 2) < (p2.x + p1.x) / 2
                && (p2.x + p1.x) / 2 < (hole.x + 2)
                && (hole.y - 2) < (p2.y + p1.y) / 2
                && (p2.y + p1.y) / 2 < (hole.y + 2)) {
            pivot = p3;
            hole.x = (p2.x + p1.x) / 2;
            hole.y = (p2.y + p1.y) / 2;
            p3 = p2;
            p2 = pivot;
        }

        Point[] arrangedPointArray = {pivot, p1, p3, hole};
        return arrangedPointArray;
    }
    private int decodeId(Point p1, Point pivot, Point p3, Point hole, Mat colorMat) {
        Mat cardMat = colorMat;
        Point idCenter = getIdCenter(hole, pivot);
        //p1-pivot midpoint
        Point m1p = new Point();
        //p3-pivot midpoint
        Point m3p = new Point();
        int tempCol = 0;
        ArrayList<Integer> tempId = new ArrayList<Integer>();
        m1p.x = (pivot.x + p1.x) / 2;
        m1p.y = (pivot.y + p1.y) / 2;
        m3p.x = (pivot.x + p3.x) / 2;
        m3p.y = (pivot.y + p3.y) / 2;

        Point id1 = new Point((2 * hole.x - m1p.x),
                (2 * hole.y - m1p.y));
        Point id2 = new Point((2 * hole.x - m3p.x),
                (2 * hole.y - m3p.y));
        Point idc1 = new Point((2 * id2.x - p1.x),
                (2 * id2.y - p1.y));
        Point idc2 = new Point((2 * id1.x - p3.x),
                (2 * id1.y - p3.y));
        Point idAvg = new Point();

        idAvg.x = (idCenter.x + idc1.x + idc2.x) / 3;
        idAvg.y = (idCenter.y + idc1.y + idc2.y) / 3;

        Point a0 = new Point();
        Point a1 = new Point();
        Point a2 = new Point();
        Point a3 = new Point();
        Point a4 = new Point();
        Point a5 = new Point();
        Point a6 = new Point();
        Point a7 = new Point();

        a0.x = (id2.x + idAvg.x) / 2;
        a0.y = (id2.y + idAvg.y) / 2;

        Integer idVal = 0;
        if (cardMat != null) {

            a7.x = (hole.x + idAvg.x) / 2;
            a7.y = (hole.y + idAvg.y) / 2;

            a1.x = (2*a0.x-a7.x);
            a1.y = (2*a0.y-a7.y);

            a6.x = (id1.x + idAvg.x) / 2;
            a6.y = (id1.y + idAvg.y) / 2;

            a4.x = (2 * idAvg.x - a0.x);
            a4.y = (2 * idAvg.y - a0.y);

            a5.x = (2 * idAvg.x - a1.x);
            a5.y = (2 * idAvg.y - a1.y);

            a2.x = (2 * idAvg.x - a6.x);
            a2.y = (2 * idAvg.y - a6.y);

            a3.x = (2 * idAvg.x - a7.x);
            a3.y = (2 * idAvg.y - a7.y);

            tempCol = (int) cardMat.get((int) idAvg.y, (int) idAvg.x)[0];
            tempId.add(tempCol);

            tempCol = (int) cardMat.get((int) a7.y, (int) a7.x)[0];
            tempId.add(tempCol);

            tempCol = (int) cardMat.get((int) a6.y, (int) a6.x)[0];
            tempId.add(tempCol);

            tempCol = (int) cardMat.get((int) a5.y, (int) a5.x)[0];
            tempId.add(tempCol);

            tempCol = (int) cardMat.get((int) a4.y, (int) a4.x)[0];
            tempId.add(tempCol);

            tempCol = (int) cardMat.get((int) a3.y, (int) a3.x)[0];
            tempId.add(tempCol);

            tempCol = (int) cardMat.get((int) a2.y, (int) a2.x)[0];
            tempId.add(tempCol);

            tempCol = (int) cardMat.get((int) a1.y, (int) a1.x)[0];
            tempId.add(tempCol);

            tempCol = (int) cardMat.get((int) a0.y, (int) a0.x)[0];
            tempId.add(tempCol);

            idVal = binary2decimal(tempId);
        }
        if(idMap.containsKey(String.valueOf(idVal))) {
            idVal = Integer.parseInt(idMap.get(String.valueOf(idVal)).getMappedId());
        } else {
            idVal = 0;
        }
        return idVal;
    }
    private Point getIdCenter(Point hole, Point pivot) {
        /*
         * hole will be the mid point of the id centre and pivot
         * centre
         */
        Point idCenter = new Point(2 * hole.x - pivot.x, 2 * hole.y
                - pivot.y);
        return idCenter;
    }
    private int binary2decimal(ArrayList<Integer> tempIdList){
        int sum = 0;
        for (int i = 0; i < tempIdList.size(); i++) {
            int curBit = tempIdList.get(tempIdList.size()-i-1);
            if (curBit > 0) {
                sum += Math.pow(2,i);
            }

        }
        /*
        if(idMap.containsKey(sum)){
            int decodedId = idMap.get(sum);
            return decodedId;
        }else{
            return 0;
        }
        */
        return sum;
    }

}
