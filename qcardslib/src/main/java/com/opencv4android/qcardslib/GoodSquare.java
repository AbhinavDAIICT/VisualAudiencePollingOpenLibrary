package com.opencv4android.qcardslib;

import org.opencv.core.Point;

/**
 * Created by abhinav on 27/2/16.
 */
public class GoodSquare {
    double area;
    double perimeter;
    Point detectedCenter;
    Point fixedCenter;

    public int getFixedCenterX() {
        return fixedCenterX;
    }

    public void setFixedCenterX(int fixedCenterX) {
        this.fixedCenterX = fixedCenterX;
    }

    public int getFixedCenterY() {
        return fixedCenterY;
    }

    public void setFixedCenterY(int fixedCenterY) {
        this.fixedCenterY = fixedCenterY;
    }

    int fixedCenterX;
    int fixedCenterY;

    public double getArea() {
        return area;
    }

    public Point getDetectedCenter() {
        return detectedCenter;
    }

    public void setDetectedCenter(Point detectedCenter) {
        this.detectedCenter = detectedCenter;
    }

    public Point getFixedCenter() {
        return fixedCenter;
    }

    public void setFixedCenter(Point fixedCenter) {
        this.fixedCenter = fixedCenter;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public double getPerimeter() {
        return perimeter;
    }

    public void setPerimeter(double perimeter) {
        this.perimeter = perimeter;
    }


}
