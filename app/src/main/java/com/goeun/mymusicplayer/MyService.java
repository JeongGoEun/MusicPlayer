package com.goeun.mymusicplayer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

/**
 * Created by 고은 on 2017-12-20.
 */

public class MyService extends Service implements MediaPlayer.OnPreparedListener {
    private static MediaPlayer mMediaPlayer = null;
    int cnt = 0, position = 0, duration = 0;
    private static boolean isPlay = false;
    SharedPreferences pref = null;
    SharedPreferences.Editor editor = null;

    MusicTask musicTask;

    //Notification
    Notification noti = null;
    NotificationManager notiManager = null;
    NotificationCompat.Builder builder;
    RemoteViews remoteView = null;

    public static String MAIN_ACTION = "com.goeun.mymusicplayer.action.main";   //noti action들
    public static String FAST_ACTION = "com.goeun.mymusicplayer.action.fast";
    public static String BACK_ACTION = "com.goeun.mymusicplayer.action.back";
    public static String PLAY_ACTION = "com.goeun.mymusicplayer.action.play";

    IMyService.Stub mBinder = new IMyService.Stub() {
        @Override
        public void setPosition(int pos) throws RemoteException {
            position=pos;
        }

        @Override
        public void play(String musicPath) throws RemoteException {
            if (position == 0) {   //처음부터 재생이라면
                mMediaPlayer.reset();
                try {
                    mBinder.setPosition(0);
                    mMediaPlayer.setDataSource(musicPath);
                } catch (Exception e) {
                   // e.printStackTrace();
                }
                mMediaPlayer.prepareAsync();

            } else {   //pause->재생
                mMediaPlayer.seekTo(position);
                mMediaPlayer.start();
            }
            isPlay=true;
        }

        @Override
        public void pause(String musicPath) throws RemoteException {
            isPlay = false;
            position = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.pause();
        }

        @Override
        public void fast(String musicPath) throws RemoteException {
            position = 0;
            isPlay = false;
            play(musicPath);
        }

        @Override
        public void back(String musicPath) throws RemoteException {
            position = 0;
            isPlay = false;
            play(musicPath);
        }

        @Override
        public int getDuration() throws RemoteException {
            return duration;
        }

        @Override
        public int curDuration() throws RemoteException {
            return mMediaPlayer.getCurrentPosition();
        }

        @Override
        public void setNotiUI(String imageUri, String title, boolean isPlay) throws RemoteException {
            showNotification(imageUri,title);
        }

        @Override
        public boolean isPlay() throws RemoteException {
            return mMediaPlayer.isPlaying();
        }
    };

    @Override
    public void onCreate() {
        pref = getSharedPreferences("MUSIC_STATE", Context.MODE_PRIVATE);
        editor = pref.edit();

        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        musicTask=MusicTask.getInstance();
        if(!mMediaPlayer.isPlaying())
            musicTask.execute();
        return START_STICKY;
    }

    public static void setDuration(){
        mMediaPlayer.seekTo(0);
        mMediaPlayer.pause();
    }

    public static boolean isPlay(){
        return mMediaPlayer.isPlaying();
    }

    public static int getDuration()  {
        return mMediaPlayer.getDuration();
    }

    public static int getCurDuration() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public IBinder onBind(final Intent intent) {
        if (mMediaPlayer == null) { //미디어 생성 후 바인더 리턴
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    position = 0;

                    Intent it = new Intent();
                    it.setAction("com.goeun.mymusicplayer.action.MUSIC_FINISHED");
                    sendBroadcast(it);
                }
            });

            mMediaPlayer.setOnPreparedListener(this);
        }
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        notiManager.cancelAll();

        Log.e("Service destroy","destroy");
        return super.onUnbind(intent);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.e("onPrepared", "media Play");
        isPlay = true;
        mediaPlayer.start();
        duration = mediaPlayer.getDuration();
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null)
            mMediaPlayer.release();
    }

    private void showNotification(String musicPath,String musicTitle) {
        Intent mainIntent = new Intent(getApplicationContext(), PlayMusic.class);
        mainIntent.setAction(MAIN_ACTION);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent mainPending = PendingIntent.getActivity(this, 0, mainIntent, 0);

        Intent playIntent = new Intent();
        playIntent.setAction(PLAY_ACTION);
        PendingIntent playPending = PendingIntent.getBroadcast(this, 0, playIntent, 0);

        Intent backIntent = new Intent();
        backIntent.setAction(BACK_ACTION);
        PendingIntent backPending = PendingIntent.getBroadcast(this, 0, backIntent, 0);

        Intent fastIntent = new Intent();
        fastIntent.setAction(FAST_ACTION);
        PendingIntent fastPending = PendingIntent.getBroadcast(this, 0, fastIntent, 0);

        remoteView = new RemoteViews(this.getPackageName(), R.layout.noti_view);
        Uri imageUri = Uri.parse(musicPath);
        String title = musicTitle;

        //try{Thread.sleep(1000);}catch (Exception e){}
        Log.e("showNoti", imageUri + "  " + title+mMediaPlayer.isPlaying()+isPlay+"");

        remoteView.setImageViewUri(R.id.notiImg, imageUri);
        remoteView.setTextViewText(R.id.notiTitle, title);

        if(isPlay) {
            remoteView.setImageViewResource(R.id.notiPlayBtn, R.drawable.pause);
        }
        else {
            remoteView.setImageViewResource(R.id.notiPlayBtn, R.drawable.play);
        }

        remoteView.setImageViewResource(R.id.notiBackBtn, R.drawable.backward);
        remoteView.setImageViewResource(R.id.notiFastBtn, R.drawable.forward);

        remoteView.setOnClickPendingIntent(R.id.notiImg, mainPending);
        remoteView.setOnClickPendingIntent(R.id.notiPlayBtn, playPending);
        remoteView.setOnClickPendingIntent(R.id.notiBackBtn, backPending);
        remoteView.setOnClickPendingIntent(R.id.notiFastBtn, fastPending);

        builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("My Music Player")
                .setContentText(title)
                .setCustomBigContentView(remoteView);

        if(isPlay) {
            builder.setSmallIcon(android.R.drawable.ic_media_play);
        }
        else {
            builder.setSmallIcon(android.R.drawable.ic_media_pause);
        }

        noti = builder.build();
        notiManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notiManager.notify(1, noti);
        cnt=0;
    }
}
