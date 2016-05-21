package com.opencv4android.qcardslib;

import java.util.LinkedHashMap;

/**
 * Created by abhinav on 28/2/16.
 */
public class ProcessFrame {
    private ProcessSettings processSettings;
    private LinkedHashMap<Integer, Integer> questionStatsMap = new LinkedHashMap<Integer, Integer>();
    LinkedHashMap<String, MappedObjects> idMap;

    public ProcessFrame(ProcessSettings processSettings, LinkedHashMap<String, MappedObjects> idMap) {
        this.processSettings = processSettings;
        this.idMap = idMap;
    }
}
