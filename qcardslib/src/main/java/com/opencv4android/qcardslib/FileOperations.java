package com.opencv4android.qcardslib;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;

/**
 * Created by abhinav tripathi on 28/2/16.
 */
public class FileOperations {

    /**
     * Method to read idMap from assets
     * @param context activity context
     * @return id-mappedObjects mapping
     */
    public static LinkedHashMap<String, MappedObjects> readIdMap(Context context){
        LinkedHashMap<String, MappedObjects> idMap = new LinkedHashMap<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("idMap.csv")));
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                MappedObjects mappedObjects = new MappedObjects();
                int count = 0;
                String detectedId = null;
                StringTokenizer stringTokenizer = new StringTokenizer(mLine,",");
                while(stringTokenizer.hasMoreTokens()){
                    switch(count){
                        case 0:
                            detectedId = stringTokenizer.nextToken();
                            mappedObjects.setDetectedId(detectedId);
                            count++;
                            break;
                        case 1:
                            mappedObjects.setMappedId(stringTokenizer.nextToken());
                            count++;
                            break;
                        case 2:
                            mappedObjects.setMappedW(stringTokenizer.nextToken());
                            count++;
                            break;
                        case 3:
                            mappedObjects.setMappedX(stringTokenizer.nextToken());
                            count++;
                            break;
                        case 4:
                            mappedObjects.setMappedY(stringTokenizer.nextToken());
                            count++;
                            break;
                        case 5:
                            mappedObjects.setMappedZ(stringTokenizer.nextToken());
                            count=0;
                            break;
                        default:
                            count = 0;
                            break;
                    }
                    idMap.put(detectedId,mappedObjects);
                }
            }
        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
        return idMap;
    }
}
