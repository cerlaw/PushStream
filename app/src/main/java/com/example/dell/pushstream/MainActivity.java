package com.example.dell.pushstream;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by DELL on 2017/6/11.
 */

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private static final String RECORD_STATUS = "record_status";
    private static final int REQUEST_CODE = 1000;

    private boolean isHd = false;
    private boolean isStarted = false;
    private int mWidth;
    private int mHeight;
    private int mDensity;
    private long mexitTime = 0;

    private TextView mTextView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null){
            isStarted = savedInstanceState.getBoolean(RECORD_STATUS);
        }
        getView();
        getScreenBaseInfo();
        checkPermission();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.
                    WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void getScreenBaseInfo() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mWidth = metrics.widthPixels;
        mHeight = metrics.heightPixels;
        mDensity = metrics.densityDpi;
    }

    private void getView() {
        mTextView = (TextView) findViewById(R.id.start_stop_textView);
        if (!isStarted){
            unRecordUI();
        }else {
            RecordingUI();
        }
        mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStarted){
                    stopRecording();
                    unRecordUI();
                    Toast.makeText(MainActivity.this, "停止录屏", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "stop recording");
                }else {
                    startScreenRecording();
                }
            }
        });
        RadioGroup group = (RadioGroup) findViewById(R.id.radioGroup);
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (checkedId){
                    case R.id.sd:
                        isHd = false;
                        break;
                    case R.id.hd:
                        isHd = true;
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void RecordingUI() {
        isStarted = true;
        mTextView.setText(R.string.stop);
        mTextView.setBackgroundColor(getResources().getColor(R.color.recordingBackground));
        Toast.makeText(this, "开始录屏", Toast.LENGTH_SHORT).show();
    }

    private void unRecordUI() {
        isStarted = false;
        mTextView.setText(R.string.start);
        mTextView.setBackgroundColor(getResources().getColor(R.color.unrecordBackground));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(RECORD_STATUS, isStarted);
    }

    private void startScreenRecording(){
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(
                MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = manager.createScreenCaptureIntent();
        startActivityForResult(permissionIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE){
            if(resultCode == RESULT_OK){
                Intent intent = new Intent(MainActivity.this, RecordingService.class);
                intent.putExtra("code", resultCode);
                intent.putExtra("data", data);
                intent.putExtra("width", mWidth);
                intent.putExtra("height", mHeight);
                intent.putExtra("density", mDensity);
                intent.putExtra("quality", isHd);
                startService(intent);
                isStarted = true;
                RecordingUI();
                Log.d(TAG, "start recording");
            }else {
                Toast.makeText(this, "用户不同意录屏", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void stopRecording(){
        Intent intent = new Intent(this, RecordingService.class);
        stopService(intent);
        isStarted = false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            if (System.currentTimeMillis() - mexitTime > 2000){
                Toast.makeText(this, "再按一次退出录屏程序，返回桌面按HOME键", Toast.LENGTH_SHORT).
                        show();
                mexitTime = System.currentTimeMillis();
            }else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        stopRecording();
        super.onDestroy();
    }
}
