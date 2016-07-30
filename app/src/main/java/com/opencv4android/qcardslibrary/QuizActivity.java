package com.opencv4android.qcardslibrary;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;

import com.opencv4android.qcardslib.FileOperations;
import com.opencv4android.qcardslib.MappedObjects;
import com.opencv4android.qcardslib.ProcessFrame;
import com.opencv4android.qcardslib.ProcessSettings;
import com.opencv4android.qcardslib.StatsAndMat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.LinkedHashMap;

public class QuizActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    SharedPreferences preferences;
    LinkedHashMap<String, MappedObjects> idMap = new LinkedHashMap<>();
    private CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("TAG", "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.javaCameraSurfaceView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        prepareIdMap();
    }

    public void prepareIdMap() {
        idMap = FileOperations.readIdMap(this);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        ProcessSettings processSettings = new ProcessSettings(false, false, false, true, true);
        ProcessFrame processObject = new ProcessFrame(processSettings, idMap);
        Mat grayMat = inputFrame.gray();
        Mat rgbaMat = inputFrame.rgba();
        StatsAndMat statsAndMat = processObject.processThisFrame(grayMat, rgbaMat);
        return statsAndMat.getRenderMat();
    }



}
