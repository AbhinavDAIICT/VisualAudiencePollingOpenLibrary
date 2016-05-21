package com.opencv4android.qcardslib;

/**
 * Created by abhinav on 28/2/16.
 */
public class ProcessSettings {
    boolean adaptiveBool;
    boolean highlightBool;
    boolean invertBool;
    boolean idBool;
    boolean answerBool;

    public ProcessSettings(boolean adaptiveBool, boolean highlightBool, boolean invertBool, boolean idBool, boolean answerBool) {
        this.adaptiveBool = adaptiveBool;
        this.highlightBool = highlightBool;
        this.invertBool = invertBool;
        this.idBool = idBool;
        this.answerBool = answerBool;
    }

    public boolean isAdaptiveBool() {
        return adaptiveBool;
    }

    public void setAdaptiveBool(boolean adaptiveBool) {
        this.adaptiveBool = adaptiveBool;
    }

    public boolean isHighlightBool() {
        return highlightBool;
    }

    public void setHighlightBool(boolean highlightBool) {
        this.highlightBool = highlightBool;
    }

    public boolean isInvertBool() {
        return invertBool;
    }

    public void setInvertBool(boolean invertBool) {
        this.invertBool = invertBool;
    }

    public boolean isIdBool() {
        return idBool;
    }

    public void setIdBool(boolean idBool) {
        this.idBool = idBool;
    }

    public boolean isAnswerBool() {
        return answerBool;
    }

    public void setAnswerBool(boolean answerBool) {
        this.answerBool = answerBool;
    }
}
