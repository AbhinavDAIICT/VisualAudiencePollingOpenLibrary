package com.opencv4android.qcardslib;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by abhinav on 28/2/16.
 */

public class ProcessFrame {
    LinkedHashMap<String, MappedObjects> idMap;
    private ProcessSettings processSettings;
    private LinkedHashMap<Integer, Integer> questionStatsMap = new LinkedHashMap<Integer, Integer>();
    private int frameCount = 0;

    /**
     * ProcessFrame constructor takes in processSettings which tell about the various image processing settings
     * during frame processing. idMap is the mapping of card ids to student ids and the mapping of options
     * {W,X,Y,Z} to {A,B,C,D}
     *
     * @param processSettings ProcessSettings object specifying settings like adaptive thresholding, optionDisplay,
     *                        idDisplay, card highlight and image inversion
     * @param idMap           HashMap with actual id as key and MappedObjects as values. MappedObjects hold detected Id,
     *                        mapped Id and ID specific option mapping
     */
    public ProcessFrame(ProcessSettings processSettings, LinkedHashMap<String, MappedObjects> idMap) {
        this.processSettings = processSettings;
        this.idMap = idMap;
    }

    /**
     * This is the main image processing method from which other methods are called. The results from this method
     * must be used to finalise options and display frame with card option and id drawn for each card in a frame.
     *
     * @param grayMat grayscale matrix from the raw camera feed.
     * @param rgbaMat rgba matrix from the raw camera feed.
     * @return StatsAndMat object that hold the question stats captured in a single camera frame along with the
     * image matrix with card ID and Option
     */
    public StatsAndMat processThisFrame(Mat grayMat, Mat rgbaMat) {
        Mat filteredMat = filterFrame(grayMat);
        Mat displayMat = detectBlack(filteredMat, rgbaMat);
        StatsAndMat statsAndMat = new StatsAndMat(displayMat, questionStatsMap);
        return statsAndMat;
    }

    /**
     * filters the grayscale matrix to remove noise
     *
     * @param gray grayscale matrix of the camera feed
     * @return filtered grayscale matrix
     */
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
        if (tmpMat1 != null) {
            return tmpMat1;
        } else return gray;
    }

    /**
     * This method returns both the parent and child contours in cases where the parent has a child contour
     *
     * @param filtered filtered grayscale matrix
     * @return list containing MatOfPoint describing individual contours (both parent and child)
     */
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

    /**
     * Method to return only useful contours from among the parent-child contours returned from getContoursWithChild method
     *
     * @param tempContours List of parent-child contours
     * @param filtered     filtered grayscale matrix
     * @param colorMat     the rgba matrix from the camera feed
     * @return a list of GoodSquare objects
     */
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

    /**
     * Method to find centroid of a contour defined by a given MatOfPoint
     *
     * @param wrapper MatOfPoint describing a contour
     * @return center Point
     */
    private Point getContourCenter(MatOfPoint wrapper) {
        double moment00 = Imgproc.moments(wrapper).get_m00();
        double moment01 = Imgproc.moments(wrapper).get_m01();
        double moment10 = Imgproc.moments(wrapper).get_m10();
        double centerX = moment10 / moment00;
        double centerY = moment01 / moment00;
        return new Point(centerX, centerY);
    }

    /**
     * Method to reposition centroid to ensure that it is equidistant from all vertices
     *
     * @param p1       centroid Point calculated by getContourCenter method
     * @param filtered filtered grayscale matrix
     * @return centerAndPoints(CenterObjects) containing center Point and x-y coordinates as well
     */
    private CenterObjects centerFix(Point p1, Mat filtered) {
        int coordinateX = 0, coordinateY = 0;
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
        if (filtered.get(checkedY, checkedX)[0] == 0) {
            checkVal = 0;
        } else {
            checkVal = 1;
        }
        while (filtered.get(checkedY, checkedX)[0] == checkVal) {
            left++;
            checkedX -= 1;
        }
        checkedX = tempX;

        while (filtered.get(checkedY, checkedX)[0] == checkVal) {
            right++;
            checkedX += 1;
        }
        if (Math.abs(right - left) > 1) {
            checkedX = tempX;
            int pw = tempX + ((right - left) / 2);
            p1.x = pw;
            coordinateX = pw;
        }

        while (filtered.get(checkedY, checkedX)[0] == checkVal) {
            up++;
            checkedY -= 1;
        }
        checkedY = tempY;

        while (filtered.get(checkedY, checkedX)[0] == checkVal) {
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
        CenterObjects centerAndPoints = new CenterObjects(p, coordinateX, coordinateY);
        return centerAndPoints;
    }

    /**
     * Method to get the centroid of the triangle formed by the centers of the three squares grouped
     * together on a single card
     *
     * @param p1 center of square 1
     * @param p2 center of square 2
     * @param p3 center of square 3
     * @return centroid of triangle
     */
    private Point getCentroid(Point p1, Point p2, Point p3) {
        double cx = (p1.x + p2.x + p3.x) / 3;
        double cy = (p1.y + p2.y + p3.y) / 3;
        return new Point(cx, cy);
    }

    /**
     * Method to rearrange points and always assign the same label to the centers of the three squares
     *
     * @param p1   center of square 1
     * @param p2   center of square 2
     * @param p3   center of square 3
     * @param hole center of the card
     * @return array of Point containing set of rearranged points
     */
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

    /**
     * Method to find the card id
     *
     * @param p1       Point p1
     * @param pivot    Point pivot (between p1 and p2)
     * @param p3       Point p3
     * @param hole     Point hole (center of the card, also the midpoint of p1-p3
     * @param colorMat rgba Mat of the camera feed
     * @return int idval i.e the decoded id
     */
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

            a1.x = (2 * a0.x - a7.x);
            a1.y = (2 * a0.y - a7.y);

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
        if (idMap.containsKey(String.valueOf(idVal))) {
            idVal = Integer.parseInt(idMap.get(String.valueOf(idVal)).getMappedId());
        } else {
            idVal = 0;
        }
        return idVal;
    }

    /**
     * Method to locate the center of the id quadrant
     *
     * @param hole  center Point of the card
     * @param pivot center of the square diagonally opposite id quadrant center
     * @return center of the id quadrant
     */
    private Point getIdCenter(Point hole, Point pivot) {
        /*
         * hole will be the mid point of the id centre and pivot
         * centre
         */
        Point idCenter = new Point(2 * hole.x - pivot.x, 2 * hole.y
                - pivot.y);
        return idCenter;
    }

    /**
     * Method to get id value from the intensity values
     *
     * @param tempIdList list containing the intensity values at the 9 id points
     * @return int card ID as calculated
     */
    private int binary2decimal(ArrayList<Integer> tempIdList) {
        int sum = 0;
        for (int i = 0; i < tempIdList.size(); i++) {
            int curBit = tempIdList.get(tempIdList.size() - i - 1);
            if (curBit > 0) {
                sum += Math.pow(2, i);
            }

        }
        return sum;
    }

    /**
     * Method to decode option and fix p1, p2 position relative two pivot
     *
     * @param p1       Point p1
     * @param pivot    Point pivot
     * @param p3       Point p3
     * @param hole     Point hole (card center, triangle(p1-pivot-p3) hypotenuse midpoint)
     * @param centroid centroid of the triangle(p1-pivot-p3)
     * @return object array with decoded option and rearranged points
     */
    private Object[] decodeOption(Point p1, Point pivot, Point p3, Point hole, Point centroid) {
        int option;
        /*
          The combination of valX and valY will later be used to
          determine the option. Depending upon whether valX and valY
          are positive or negative, we get to know the relative
          position of the centroid with respect to the midpoint of the
          hypotenuse
        */

        double valX = centroid.x - hole.x;
        double valY = centroid.y - hole.y;

        /*
         * Calculating the x-distance between pivot-p1 and pivot-p3
         */
        int p1p = (int) (pivot.x - p1.x);
        int p3p = (int) (pivot.x - p3.x);

        /*
         * hole will be the mid point of the id centre and pivot
         * centre
         */
        Point idc = new Point(2 * hole.x - pivot.x, 2 * hole.y
                - pivot.y);
        // Point idNew = centerFixID
        Double angle = (double) 0;
        Double theta = (double) 0;
        if (valX > 0 && valY > 0) {

            option = 1;
                    /*
                     * swapping p1 and p3 to ensure that p3 is the left most
                     * anchor. This is important to ensure that ID is
                     * calculated correctly irrespective of rotation
                     */
            if (p1p > p3p) {
                Point temp = p1;
                p1 = p3;
                p3 = temp;
            }

        } else if (valX > 0 && valY < 0) {
            option = 4;
            if (p1p < p3p) {
                Point temp = p1;
                p1 = p3;
                p3 = temp;
            }

        } else if (valX < 0 && valY > 0) {
            option = 2;
            if (p1p > p3p) {
                Point temp = p1;
                p1 = p3;
                p3 = temp;
            }

        } else if (valX < 0 && valY < 0) {
            option = 3;

        } else {
            option = 5;
        }

        Object[] optionAndPoints = new Object[3];
        optionAndPoints[0] = option;
        optionAndPoints[1] = p1;
        optionAndPoints[2] = p3;
        return optionAndPoints;
    }

    /**
     * Method to group detected contours in groups of three based on distance between them
     *
     * @param goodSquareList list of all the useful contours
     * @return mapping of squares to cards
     */
    private LinkedHashMap<Integer, ArrayList<Point>> groupNeighbors(List<GoodSquare> goodSquareList) {
        //cardMap = new LinkedHashMap<Integer, ArrayList<Integer>>();
        LinkedHashMap<Integer, ArrayList<Point>> groupedCenterMap = new LinkedHashMap<Integer, ArrayList<Point>>();
        Point[] centerArray = new Point[goodSquareList.size()];
        List<Point> centerList = new ArrayList<Point>(goodSquareList.size());
        Iterator<GoodSquare> iterator = goodSquareList.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            Point center = iterator.next().fixedCenter;
            centerArray[i] = center;
            centerList.add(center);
            i++;
        }
        // Sort the array based on the x coordinate of the centres
        // Since points contain both x and y values, we need comparator to sort
        // them according to x values
        Arrays.sort(centerArray, new Comparator<Point>() {
            public int compare(Point a, Point b) {
                int xComp = Double.compare(a.x, b.x);
                if (xComp == 0)
                    return Double.compare(a.y, b.y);
                    // try removing y comparison and check effects
                else
                    return xComp;
            }
        });
        // get the ids of the contours in the sorted order
        int[] ids = new int[centerList.size()];
        for (i = 0; i < centerList.size(); i++) {
            ids[i] = centerList.indexOf(centerArray[i]);
        }

        int counter = 0;

        // rem will contain the ids that will be needed to be removed later
        ArrayList<Integer> rem = new ArrayList<Integer>();
        for (i = 0; i < centerList.size(); i++) {
            ArrayList<Integer> cardList = new ArrayList<Integer>();
            ArrayList<Point> groupedCenterList = new ArrayList<Point>();
            // cardList.add(0);
            ArrayList<Point> tempList = new ArrayList<Point>();
            ArrayList<Integer> tempId = new ArrayList<Integer>();

            // scale = length of a side
            double scale = goodSquareList.get(ids[i]).getPerimeter() / 4;
            if (!rem.contains(ids[i])) {

                // Finding potential neighbour centers in x+ direction
                for (int j = i; j < centerList.size()
                        && Math.abs(centerArray[i].x - centerArray[j].x) < 2 * scale; j++) {
                    double iArea = goodSquareList.get(ids[i]).getArea();
                    double jArea = goodSquareList.get(ids[j]).getArea();

					/*
                     * Area constraint important to check. It will eliminate too
					 * small or too big contours If area constraint is met, add
					 * the contour id to tempId and add the center to tempList
					 */
                    if (iArea < 1.2 * jArea && iArea > 0.8 * jArea) {
                        tempId.add(ids[j]);
                        tempList.add(centerArray[j]);
                    }
                }

                // Finding potential neighbor centers in x- direction
                for (int j = i - 1; j > 0
                        && Math.abs(centerArray[i].x - centerArray[j].x) < 2 * scale; j--) {
                    double iArea = goodSquareList.get(ids[i]).getArea();
                    double jArea = goodSquareList.get(ids[j]).getArea();
                    if (iArea < 1.2 * jArea && iArea > 0.8 * jArea) {
                        tempId.add(ids[j]);
                        tempList.add(centerArray[j]);
                    }

                }

				/*
                 * After the above two loops, we get a set of all potential
				 * neighbouring contours for contour with id 'i' based on the
				 * value of their x-coordinate. It can be imagined as a column in
				 * the image
				 */

				/*
                 * The 'for' loop below searches for the potential neighbours in
				 * the column obtained in the last two 'for' loops.
				 */

                for (Integer j = 0; j < tempList.size(); j++) {

					/*
                     * Checking in the column, if the y-coordinates are in
					 * permissible range then add the index of the contour to the
					 * cardList and also add it to the rem list so that we do not
					 * process it again
					 */
                    if (Math.abs(centerArray[i].y - tempList.get(j).y) < 2 * scale) {
                        cardList.add(centerList.indexOf(tempList.get(j)));
                        groupedCenterList.add(tempList.get(j));
                        rem.add(centerList.indexOf(tempList.get(j)));
                    }
                }

				/*
                 * Maintain a cardMap to hold the cardLists. cardMap holds
				 * only those cardLists which have more than 2 elements as we
				 * require at least three squares for a valid pattern
				 */
                if (cardList.size() > 2) {
                    //cardMap.put(counter, cardList);
                    groupedCenterMap.put(counter, groupedCenterList);
                    counter++;
                }
            }

        }
        return groupedCenterMap;
    }

    /**
     * Method to draw ID and option on the matrix
     *
     * @param colorMat          rgba matrix
     * @param filteredMat       filtered grayscale matrix
     * @param groupedCenterList list containing centers of squares belonging to same card
     * @return rgba mat with id and options
     */
    private Mat drawOptionsAndIds(Mat colorMat, Mat filteredMat, LinkedHashMap<Integer, ArrayList<Point>> groupedCenterList) {
        int option = 0;
        float[] radius = new float[1];
        for (Map.Entry<Integer, ArrayList<Point>> entry : groupedCenterList.entrySet()) {
            ArrayList<Point> cardGroupList = new ArrayList<Point>();
            cardGroupList = entry.getValue();
            Point p1 = cardGroupList.get(0);
            Point p2 = cardGroupList.get(1);
            Point p3 = cardGroupList.get(2);
            Point[] pointArray = {p1, p2, p3};

            Point centroid = getCentroid(p1, p2, p3);

            /*
            Draw a circle passing through all three points and get its
            center. Note that the center will be the midpoint of the
            hypotenuse of the triangle formed by points p1,p2 and p3.
            */
            MatOfPoint2f pMat = new MatOfPoint2f(pointArray);
            Point hole = new Point();
            Imgproc.minEnclosingCircle(pMat, hole, radius);

            Point[] arrangedPointArray = rearrangePoints(p1, p2, p3, hole);
            p1 = arrangedPointArray[1];
            Point pivot = arrangedPointArray[0];
            p3 = arrangedPointArray[2];
            hole = arrangedPointArray[3];

            Object[] optionAndPoints = decodeOption(p1, pivot, p3, hole, centroid);
            option = (int) optionAndPoints[0];
            p1 = (Point) optionAndPoints[1];
            p3 = (Point) optionAndPoints[2];

            int id = decodeId(p1, pivot, p3, hole, filteredMat);
            if (processSettings.idBool) {
                Point idCenter = getIdCenter(hole, pivot);
                Core.putText(colorMat, String.valueOf(id), idCenter,
                        Core.FONT_HERSHEY_SIMPLEX, 0.5,
                        new Scalar(0, 255, 0), 2);
            }

            option = getMappedOption(option, id);
            questionStatsMap.put(id, option);

/*
            Core.putText(colorMat, "p1", p1,
                    Core.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(0, 0, 255), 2);
            Core.putText(colorMat, "p3", p3,
                    Core.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(0, 0, 255), 2);
*/
            if (processSettings.answerBool) {
                Core.putText(colorMat, String.valueOf(option), hole,
                        Core.FONT_HERSHEY_SIMPLEX, 0.6,
                        new Scalar(0, 0, 255), 2);
            }
        }
        return colorMat;
    }

    /**
     * Method to get mapped option from {W,X,Y,Z} to {A,B,C,D} mapping
     *
     * @param option detected option
     * @param id     detected id
     * @return mapped option number
     */
    private int getMappedOption(int option, int id) {
        if (idMap.containsKey(String.valueOf(id))) {
            return idMap.get(String.valueOf(id)).getMappedOption(option);
        } else {
            return 5;
        }
    }

    /**
     * Method calling other methods for deciding option and id
     *
     * @param filtered filtered grayscale matrix
     * @param colorMat rgba matrix
     * @return rgba matrix to be rendered
     */
    private Mat detectBlack(Mat filtered, Mat colorMat) {
        Integer totContours = 0;
        Integer finContours = 0; /* finding contours with child*/
        List<MatOfPoint> tempContours = getContoursWithChild(filtered); /*From tempContours select the rectangles with white centers*/
        List<GoodSquare> goodSquareList = populateUsefulContourList(tempContours, filtered, colorMat);
        Mat mat = colorMat; /* finding the potential neighbours for each contour*/
        LinkedHashMap<Integer, ArrayList<Point>> groupedCenterList = groupNeighbors(goodSquareList); /* Group the neighbours, identify options and draw contours*/
        try {
            mat = drawOptionsAndIds(colorMat, filtered, groupedCenterList);
        } catch (NullPointerException e) {
            System.out.println("Exception" + e);
        }
        if (mat != null) return mat;
        else return colorMat;
    }

}
