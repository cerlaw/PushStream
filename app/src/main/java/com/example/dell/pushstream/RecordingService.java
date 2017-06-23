package com.example.dell.pushstream;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by DELL on 2017/6/11.
 */

public class RecordingService extends Service{

    private static final String TAG = "RecordingService";
    private static final int NOTIFICATION_ID = 3;

    private int mWidth;
    private int mHeight;
    private int mDensity;
    private int resultCode;
    private Intent resultData;
    private boolean isHd;

    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mDisplay;
    private NotificationManager manager;
    private String filePath;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "service onStartCommand is started");
        resultCode = intent.getIntExtra("code", -1);
        resultData = intent.getParcelableExtra("data");
        mWidth = intent.getIntExtra("width", 720);
        mHeight = intent.getIntExtra("height", 1280);
        mDensity = intent.getIntExtra("density", 1);
        isHd = intent.getBooleanExtra("quality", false);
        filePath = intent.getStringExtra("filePath");
        mMediaProjection = createMediaProjection();
        mMediaRecorder = createMediaRecorder();
        mDisplay = createVirtualDisplay();//必须在mediaRecorder.prepare()之后调用
        mMediaRecorder.start();
        initialNotification();

        return Service.START_NOT_STICKY;
        //返回这个值表示如果在执行完onStartCommand后，服务被异常kill掉，系统不会自动重启该服务。
    }

    private void initialNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher).
                setContentTitle(getResources().getString(R.string.app_name)).
                setContentText("正在录制视频").
                setOngoing(true).
                setDefaults(Notification.DEFAULT_VIBRATE);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 1, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pi);
        manager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private MediaRecorder createMediaRecorder() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date curDate = new Date(System.currentTimeMillis());
        String curTime = dateFormat.format(curDate).replace(" ", "");
        String quality = "SD";
        if (isHd) quality = "HD";

        MediaRecorder mediaRecorder = new MediaRecorder();
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoSize(mWidth, mHeight);
        int bitrate;
        if (isHd){
            mediaRecorder.setVideoEncodingBitRate(5 * mWidth * mHeight);
            mediaRecorder.setVideoFrameRate(60);
            bitrate = 5 * mWidth * mHeight / 1000;
        }else {
            mediaRecorder.setVideoEncodingBitRate(mWidth * mHeight);
            mediaRecorder.setVideoFrameRate(30);
            bitrate = mWidth * mHeight / 1000;
        }
        mediaRecorder.setOutputFile(filePath + "/" + quality
                + curTime + ".mp4");
        Log.d(TAG, filePath + "/" + quality + curTime + ".mp4");
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        }
//        Log.d(TAG, "SD video: " + quality + ", BitRate: " + bitrate + "kbps");
        return mediaRecorder;
    }

    private VirtualDisplay createVirtualDisplay() {
        Log.d(TAG, "create virtualDisplay");
        return mMediaProjection.createVirtualDisplay(TAG, mWidth, mHeight, mDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(),
                null, null);
    }

    private MediaProjection createMediaProjection() {
        return ((MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE)).
                getMediaProjection(resultCode, resultData);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "stop recording");
        super.onDestroy();
        if (mDisplay != null){
            mDisplay.release();
            mDisplay = null;
        }
        if (mMediaRecorder != null){
            mMediaRecorder.setOnErrorListener(null);
            mMediaProjection.stop();
            mMediaRecorder.reset();
        }
        if (mMediaProjection != null){
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        if (manager != null){
            manager.cancel(NOTIFICATION_ID);
        }

    }
}
