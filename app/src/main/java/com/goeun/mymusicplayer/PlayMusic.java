package com.goeun.mymusicplayer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ExpandedMenuView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import static android.os.Environment.DIRECTORY_MUSIC;

/**
 * Created by 고은 on 2017-12-19.
 */

public class PlayMusic extends AppCompatActivity {
    ImageView albumImage;
    ImageButton playBtn, backBtn, forwardBtn;
    TextView musicTitleTxt, curTimeTxt, amoundTimeTxt;
    ProgressBar progressBar;

    String mPath, title, externPath = Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC).toString();
    Uri imageUri;
    List<String> filePath = new ArrayList<>();
    String[] fileNames;
    List<String> fileList = new ArrayList<>();

    SharedPreferences pref = null;
    SharedPreferences.Editor editor = null;
    String STATE;
    private boolean isBound = false;
    private IMyService mBinder = null;
    public MusicTask musicTask;
    /*pref : STATE : 음악 상태, CUR_MUSIC_INDEX : 현재 음악 인덱스*/

    //Broadcast Receiver
    BroadcastReceiver receiver = null, notiReceiver = null;
    Cursor listCursor = null;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playmusic);

        albumImage = (ImageView) findViewById(R.id.albumImage);
        playBtn = (ImageButton) findViewById(R.id.playBtn);
        backBtn = (ImageButton) findViewById(R.id.backBtn);
        forwardBtn = (ImageButton) findViewById(R.id.fastBtn);
        musicTitleTxt = (TextView) findViewById(R.id.musicTitleTxt);
        curTimeTxt = (TextView) findViewById(R.id.curTimeTxt);
        amoundTimeTxt = (TextView) findViewById(R.id.amountTimeTxt);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        Log.e("onCreate", "onCreate");

        /*현재 인덱스를 저장할 공유 프레퍼런스*/
        pref = getSharedPreferences("MUSIC_STATE", Context.MODE_PRIVATE);
        editor = pref.edit();

        /*리스트뷰에서 선택 한 이미지 세팅*/
        Intent it = getIntent();
        title = it.getStringExtra("title");
        imageUri = Uri.parse(it.getStringExtra("imageURI"));
        setPreference(title, imageUri);  //공유 프레퍼런스 설정

        albumImage.setImageURI(imageUri);
        musicTitleTxt.setText(title);
        playBtn.setImageResource(R.drawable.play);

        /*음악 리스트 배열에 저장*/
        File directory = new File(externPath);   //mp3 리스트 뽑아오기
        File[] files = directory.listFiles();
        fileNames = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".mp3");
            }
        });

        for (int i = 0; i < fileNames.length; i++) {
            filePath.add(fileNames[i]);
            //Log.e("fileName : ", fileNames[i]+" "+title);
        }
        String[] proj = {
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST
        };
        listCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);
        int cnt = 0;
        while (listCursor.moveToNext()) {
            if (listCursor.getInt(0) == 1) {    //음악 파일 이라면
                fileList.add(listCursor.getString(2));
                Log.e("fileName", listCursor.getString(2) + (cnt++) + "");
            }
        }
        /*Broadcast Receiver 선언 및 초기화*/
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.goeun.mymusicplayer.action.MUSIC_FINISHED");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int index = pref.getInt("CUR_MUSIC_INDEX", 0);  //현재 index받기
                if (index == fileList.size() - 1)    //마지막곡이라면 처음으로
                    index = 0;
                else    //다음 곡 위해 증가
                    index += 1;

                drawMusinInfo(index);

                mPath = externPath + "/" + filePath.get(index);
                editor.putInt("CUR_MUSIC_INDEX", index);
               // Log.e("receive", index + "  " + mPath);

                try {
                    //mBinder.setPosition(0);
                    mBinder.play(mPath);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
                editor.apply();
            }
        };
        registerReceiver(receiver, filter);  //리시버 등록

        IntentFilter notiFilter = new IntentFilter();
        notiFilter.addAction("com.goeun.mymusicplayer.action.play");
        notiFilter.addAction("com.goeun.mymusicplayer.action.back");
        notiFilter.addAction("com.goeun.mymusicplayer.action.fast");
        notiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.e("noti Receive", intent.getAction().toString());
                if (intent.getAction().equals(MyService.PLAY_ACTION)) {
                    musicControl(0);
                } else if (intent.getAction().equals(MyService.BACK_ACTION)) {
                    musicControl(1);
                } else if (intent.getAction().equals(MyService.FAST_ACTION)) {
                    musicControl(2);
                }
            }
        };
        registerReceiver(notiReceiver, notiFilter);  //notification 리시버 등록

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e("onRestart", "onRestart");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e("onStart", "onStart");
        Intent it = new Intent(this, MyService.class);
        bindService(it, mConnection, Context.BIND_AUTO_CREATE);
        startService(it);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e("onStop", "stop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
        unregisterReceiver(receiver);
        unregisterReceiver(notiReceiver);

        Log.e("onDestroy", "onDestroy");
    }

    public void onClick(View v) {
        if (isBound) {
            switch (v.getId()) {
                case R.id.playBtn: {    //재생
                    musicControl(0);
                    break;
                }
                case R.id.backBtn: {
                    musicControl(1);
                    break;
                }
                case R.id.fastBtn: {
                    //다음곡
                    musicControl(2);
                    break;
                }
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            musicTask=MusicTask.getInstance();
            musicTask.setUI(curTimeTxt,amoundTimeTxt,progressBar,playBtn);
            musicTask.setPref(pref);
            musicTask.setBinder(mBinder);

            mBinder = IMyService.Stub.asInterface(iBinder);   //바인딩
            isBound = true;
            Log.e("service", "connect-----------------");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
            Log.e("service", "disconnect-----------------");
        }
    };

    public void drawMusinInfo(int index) {
        listCursor.moveToPosition(index);
        Uri artPath = Uri.parse("content://media/external/audio/albumart");   //앨범 이미지 가져오기
        imageUri = ContentUris.withAppendedId(artPath, Integer.valueOf(listCursor.getString(1)));
        title = listCursor.getString(2);
        Log.e("drawMusicInfo", index + " " + imageUri.toString() + " " + listCursor.getString(2));

        new Thread(new Runnable() { //이미지 화면, 텍스트 바꾸기
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        title = listCursor.getString(2);
                        albumImage.setImageURI(imageUri);
                        musicTitleTxt.setText(title);
                        setPreference(title, imageUri);  //공유 프레퍼런스 설정
                        try {
                            Log.e("runOnUiThread", title);
                            mBinder.setNotiUI(imageUri.toString(), title, MyService.isPlay());
                        } catch (Exception e) {
                        }
                    }
                });
            }
        }).start();
    }

    public int getDuration() {
        int rtn = 0;
        if (mBinder != null) {
            try {
                rtn = mBinder.getDuration();
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
        return rtn;
    }

    public int getCurDuration() {
        int rtn = 0;
        if (mBinder != null) {
            try {
                rtn = mBinder.curDuration();
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
        return rtn;
    }


    public boolean isPaly() {
        boolean rtn = false;
        try {
            rtn = mBinder.isPlay();
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return rtn;
    }

    void setPreference(String title, Uri imageUri) {
        editor.putString("MUSIC_TITLE", title);
        editor.putString("MUSIC_IMAGE", imageUri.toString());
        editor.apply();
    }

    public void musicControl(int code) {
        switch (code) {
            case 0: {
                try {
                    int index = 0;
                    if (!isPaly()||!title.equals(pref.getString("MUSIC_TITLE", ""))) {
                        //정지상태 -> 실행, 새로 리스트뷰에서 가져왔다면
                        for (int i = 0; i < fileList.size(); i++) {
                            if (fileList.get(i).contains(title)) {
                                index = i;
                                break;
                            }
                        }
                        mPath = externPath + "/" + filePath.get(index);
                        Log.e("new Play", title + ", " + pref.getString("MUSIC_TITLE", "") + mPath);
                        try {
                            mBinder.play(mPath);
                            setPreference(title, imageUri);  //공유 프레퍼런스 설정
                            mBinder.setNotiUI(imageUri.toString(), title, isPaly());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else if (mBinder.isPlay()) {   //실행상태 -> 정지
                        mPath = externPath + "/" + filePath.get(index);
                        try {
                            mBinder.pause(mPath);
                            drawMusinInfo(pref.getInt("CUR_MUSIC_INDEX",0));
                            //mBinder.setNotiUI(pref.getString("MUSIC_ALBUM",""), pref.getString("MUSIC_TITLE",""), isPaly());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    editor.putInt("CUR_MUSIC_INDEX", index);   //현재 재생 파일 인덱스 저장
                    editor.apply();

                } catch (Exception e) {
                    Log.e("onClick", e.getMessage() + "  " + mPath + "  " + title);
                }
                break;
            }
            case 1: {
                //이전곡
                int curIndex = pref.getInt("CUR_MUSIC_INDEX", 0);
                if (curIndex == 0) {    //이전에 넘어갈것이 없으면 -> 마지막 곡으로
                    curIndex = fileList.size() - 1;
                } else {
                    curIndex=curIndex-1;
                }

                mPath = externPath + "/" + filePath.get(curIndex);
                editor.putInt("CUR_MUSIC_INDEX", curIndex);
                try {
                    mBinder.back(mPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                editor.apply();
                drawMusinInfo(curIndex);

               // Log.e("backBtn", mPath + " index : " + curIndex + "");
                progressBar.setMax(getDuration());
                break;
            }
            case 2: {
                int curIndex = pref.getInt("CUR_MUSIC_INDEX", 0);
                if (curIndex == fileList.size() - 1) {    //다음에 넘어갈것이 없으면 -> 처음으로
                    curIndex = 0;
                } else {
                    curIndex += 1;
                }

                mPath = externPath + "/" + filePath.get(curIndex);
                editor.putInt("CUR_MUSIC_INDEX", curIndex);
                try {
                    mBinder.fast(mPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                drawMusinInfo(curIndex);

                editor.apply();

                //Log.e("fastBtn", mPath + " index : " + curIndex + "");
                progressBar.setMax(getDuration());
                break;
            }
        }
    }
}
