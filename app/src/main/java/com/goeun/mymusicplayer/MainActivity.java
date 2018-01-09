package com.goeun.mymusicplayer;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.List;

import static android.R.attr.path;
import static android.os.Environment.DIRECTORY_MUSIC;

public class MainActivity extends AppCompatActivity {
    private int STORAGE_PERMISSION_CODE = 23;
    ListView listView;
    ListviewAdapter adapter;
    public static PlayList playList;
    Cursor listCursor;
    boolean permission = false;

    SharedPreferences pref = null;
    SharedPreferences.Editor editor = null;
    //MUSIC_STATE가 파일 이름

    private MediaScannerConnection.MediaScannerConnectionClient mScanClient = new MediaScannerConnection.MediaScannerConnectionClient() {
        @Override
        public void onMediaScannerConnected() {
            //Log.i("onMediaScan", "onMediaScannerConnected");
            File file = Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC); // 외장 디렉토리 가져옴

            File[] fileNames = file.listFiles(new FilenameFilter() {               // 특정 확장자만 가진 파일들을 필터링함
                public boolean accept(File dir, String name) {
                    return name.endsWith(".mp3");
                }
            });

            if (fileNames != null) {
                for (int i = 0; i < fileNames.length; i++) {        //  파일 갯수 만큼   scanFile을 호출함
                    msc.scanFile(fileNames[i].getAbsolutePath(), null);
                }
            }
        }

        @Override
        public void onScanCompleted(String s, Uri uri) {
            //Log.i("scanComplete", "onScanCompleted(" + path + ", " + uri.toString() + ")");     // 스캐닝한 정보를 출력해봄
        }
    };
    private MediaScannerConnection msc = new MediaScannerConnection(this, mScanClient);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = getSharedPreferences("MUSIC_STATE", Context.MODE_PRIVATE);
        editor = pref.edit();
        editor.putString("STATE", "PAUSE");   //기본 설정 초기화
        editor.apply();

        listView = (ListView) findViewById(R.id.listView);
        adapter = new ListviewAdapter(this);
        playList = new PlayList();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapterView, View view, int i, long l) {
                Intent it = new Intent(getApplicationContext(), PlayMusic.class);
                songInfo info = (songInfo) adapter.getItem(i);

                it.putExtra("title", info.title);
                it.putExtra("imageURI", info.imgUri.toString());
                it.setAction("com.goeun.action.PLAY");  //재생
                startActivity(it);
            }
        });

        if(!msc.isConnected())
            msc.connect();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.e("permission","true");
            settingListView();
        } else {
            Log.e("permission","false");
            requestStoragePermission();
        }

        if (listCursor != null)
            listCursor.close();

        listView.setAdapter(adapter);
        if (isReadStorageAllowed())
            return;
    }

    public class songInfo {
        //곡 정보를 가지고 있을 클래스
        String title;
        String name;
        Uri imgUri;
    }

    private boolean isReadStorageAllowed() {
        //Getting the permission status
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        //If permission is granted returning true
        if (result == PackageManager.PERMISSION_GRANTED)
            return true;

        //If permission is not granted returning false
        return false;
    }

    //Requesting permission
    private void requestStoragePermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

        }

        //And finally ask for the permission
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }

    //This method will be called when the user will tap on allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //Checking the request code of our request
        if (requestCode == STORAGE_PERMISSION_CODE) {
            //If permission is granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                settingListView();
            }
        } else {
            return;
        }
        Log.e("permission", permissions + "");
    }

    public void settingListView() {
        String[] proj = {
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST
        };

        listCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);
       // Log.e("MediaPath", MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString());

        if (listCursor != null && listCursor.getCount() > 0) {
           // Log.e("cursor data", listCursor.getCount() + "");
            songInfo info;
            //음악파일, 앨범아이디, 음원명, 가수명
            while (listCursor.moveToNext()) {
                try {
                    if (listCursor.getInt(0) == 1) {    //음악 파일 이라면
                        Uri artPath = Uri.parse("content://media/external/audio/albumart");
                        Uri uri = ContentUris.withAppendedId(artPath, Integer.valueOf(listCursor.getString(1)));
                        //이미지 설정하기 위해 앨범 uri id값으로 저장

                        info = new songInfo();
                        info.imgUri = uri;
                        info.title = listCursor.getString(2);
                        info.name = listCursor.getString(3);

                        //Log.e("music info",info.title+"  "+info.name+" "+info.imgUri.toString());

                        adapter.addItem(info);  //어댑터에 추가
                        playList.addItem(info);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged(); //어댑터뷰 업데이트
                            }
                        });

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
