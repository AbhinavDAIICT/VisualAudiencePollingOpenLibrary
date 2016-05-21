package com.opencv4android.qcardslib;

/**
 * Created by abhinav on 20/3/16.
 */
public class MappedObjects {
    String detectedId;
    String mappedId;
    String mappedW;
    String mappedX;
    String mappedY;
    String mappedZ;
    int mappedOption;

    public String getMappedZ() {
        return mappedZ;
    }

    public void setMappedZ(String mappedZ) {
        this.mappedZ = mappedZ;
    }

    public String getDetectedId() {
        return detectedId;
    }

    public void setDetectedId(String detectedId) {
        this.detectedId = detectedId;
    }

    public String getMappedId() {
        return mappedId;
    }

    public void setMappedId(String mappedId) {
        this.mappedId = mappedId;
    }

    public String getMappedW() {
        return mappedW;
    }

    public void setMappedW(String mappedW) {
        this.mappedW = mappedW;
    }

    public String getMappedX() {
        return mappedX;
    }

    public void setMappedX(String mappedX) {
        this.mappedX = mappedX;
    }

    public String getMappedY() {
        return mappedY;
    }

    public void setMappedY(String mappedY) {
        this.mappedY = mappedY;
    }

    public int getMappedOption(int option){
        switch(option){
            case 1:
                return Integer.parseInt(mappedW);
            case 2:
                return Integer.parseInt(mappedX);
            case 3:
                return Integer.parseInt(mappedY);
            case 4:
                return Integer.parseInt(mappedZ);
            default:
                return 5;
        }
    }

}
