package com.example.imgrecognition;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.SurfaceView;

import com.example.imgrecognition.filters.Filter;
import com.example.imgrecognition.filters.ar.ImageDetectionFilter;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImgRecognitionActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private JavaCameraView jcv;

    // The filters.
    private Filter[] mImageDetectionFilters;

    private int[] mImgs = {R.mipmap.a, R.mipmap.b, R.mipmap.c, R.mipmap.d, R.mipmap.e};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img_recognition);
        mThreadPool = Executors.newCachedThreadPool();
        jcv = (JavaCameraView) findViewById(R.id.jcv);
        jcv.setVisibility(SurfaceView.VISIBLE);
        jcv.setCvCameraViewListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initDebug();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (jcv != null)
            jcv.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (jcv != null)
            jcv.disableView();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    jcv.enableView();
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
    public void onCameraViewStarted(int width, int height) {
        int i = 0;
        mImageDetectionFilters = new Filter[mImgs.length];
        for (int mImg : mImgs) {
            Filter starryNight = null;
            try {
                starryNight = new ImageDetectionFilter(this, mImg);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mImageDetectionFilters[i] = starryNight;
            i++;
        }
    }

    @Override
    public void onCameraViewStopped() {

    }

    private long lastTime;
    /**
     * describe 管理发送消息的线程池
     */
    private ExecutorService mThreadPool;

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final Mat rgba = inputFrame.rgba();
        long time = System.currentTimeMillis() / 1000;
        if (time > lastTime) {
            if (mImageDetectionFilters != null) {
                for (int i = 0; i < mImageDetectionFilters.length; i++) {
                    final int y = i;
                    mThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (mImageDetectionFilters[y].match(rgba, rgba)) {
                                h.sendEmptyMessage(y);
                            }
                        }
                    });
                }
            }
        }
        lastTime = time;
        return rgba;
    }

    boolean isFinish;
    private Handler h = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!isFinish) {
                int i = msg.what;
                Intent intent = new Intent();
                intent.putExtra("data", i);
                setResult(RESULT_OK, intent);
                isFinish = true;
                finish();
            }
            super.handleMessage(msg);
        }
    };
}
