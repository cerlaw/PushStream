package com.example.dell.pushstream;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

/**
 * Created by DELL on 2017/6/13.
 */

public class MediaCodecActivity extends AppCompatActivity {

    private static final String TAG = "MediaCodec";
    private static final int REQUEST_CODE = 1;

    private MediaProjectionManager mManager;
    private ScreenRecorder mRecorder;
    private Button button;

    private int mWidth;
    private int mHeight;
    private int mDensity;
    private boolean isStarted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mediacodec_layout);
        getView();
        getBaseInfo();
        checkPermission();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.
                    WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void getBaseInfo() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mWidth = metrics.widthPixels;
        mHeight = metrics.heightPixels;
        mDensity = metrics.densityDpi;
    }

    private void getView() {
        button = ((Button) findViewById(R.id.mediaCodex_start));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecorder != null){
                    stopRecording();
                }else {
                    startRecording();
                }
            }
        });
    }

    private void startRecording() {
        mManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = mManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MediaProjection projection = mManager.getMediaProjection(resultCode, data);
        if (projection == null){
            Log.d(TAG, "projection is null");
            return;
        }else {
            File file = new File(Environment.getExternalStorageDirectory(),"record-" + mWidth +
            "x" + mHeight + "-" + System.currentTimeMillis() + ".mp4");
            final int bitRate = 6000000;
            mRecorder = new ScreenRecorder(mWidth, mHeight, bitRate, mDensity, projection,
                    file.getAbsolutePath());
            Log.d(TAG, "file in: " + file.getAbsolutePath());
            mRecorder.start();
            button.setText(R.string.stop);
            Toast.makeText(this, "Start Recording", Toast.LENGTH_SHORT).show();
//            moveTaskToBack(true);//意义:开启后回到桌面
        }
    }

    private void stopRecording() {
        isStarted = false;
        mRecorder.quit();
        mRecorder = null;
        button.setText(R.string.start);
        Toast.makeText(this, "Stop Recording", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //释放资源
        if (mRecorder != null){
            mRecorder.quit();
            mRecorder = null;
        }
    }
}
