package com.opencv4android.qcardslib;

import org.opencv.core.Point;

/**
 * Created by abhinav on 19/3/16.
 */
public class CenterObjects {
    private Point center;
    private int centerX;
    private int centerY;

    public CenterObjects(Point center, int centerX, int centerY) {
        this.center = center;
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public Point getCenter() {
        return center;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }
}
