package com.goeun.mymusicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by 고은 on 2017-12-25.
 */

public class MusicTask extends AsyncTask {
    private IMyService mBinder = null;
    private static MusicTask task;

    TextView curTimeTxt, amoundTimeTxt;
    ProgressBar progressBar;
    ImageButton playBtn;

    SharedPreferences pref=null;

    private MusicTask(){}

    public void setUI(TextView curTxt,TextView amoundTxt,ProgressBar bar,ImageButton btn){
        Log.e("setUI",curTxt.getText().toString());

        curTimeTxt=curTxt;
        amoundTimeTxt=amoundTxt;
        progressBar=bar;
        playBtn=btn;
    }

    public void setBinder(IMyService binder){
            mBinder=binder;
    }
    public void setPref(SharedPreferences preferences){
            pref=preferences;
    }
    public boolean isRunning(){
        if(task==null)
            return false;
        else
            return true;
    }

    public static MusicTask getInstance(){
        if(task==null){
            task=new MusicTask();
        }
        return task;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        Log.e("progressUpdate",MyService.isPlay()+"");
        while (this.isRunning()) { //연결이 되어있고, 재생 중이라면
            if (MyService.isPlay()) {
                try {
                    Thread.sleep(1000);
                    Log.e("progressUpdate",MyService.getDuration()+"");
                    publishProgress();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (isCancelled()) {
                    break;
                }
            }
        }
        return true;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);

        if (MyService.isPlay()) {
            playBtn.setImageResource(R.drawable.pause);
        } else {
            playBtn.setImageResource(R.drawable.play);
        }

        progressBar.setProgress(MyService.getCurDuration());
        double sec = (MyService.getCurDuration() % 60000) / 1000;
        sec = Math.floor(sec);

        progressBar.setMax(MyService.getDuration());
        double sec2 = (MyService.getDuration() % 60000) / 1000;
        sec2 = Math.floor(sec2);
        if (sec < 10)
            curTimeTxt.setText("0" + MyService.getCurDuration() / 60000 + " : 0" + (int) sec + "");
        else
            curTimeTxt.setText("0" + MyService.getCurDuration() / 60000 + " : " + (int) sec + "");

        if (sec2 < 10)
            amoundTimeTxt.setText(" / 0" + MyService.getDuration() / 60000 + " : 0" + (int) sec2 + "");
        else
            amoundTimeTxt.setText(" / 0" + MyService.getDuration() / 60000 + " : " + (int) sec2 + "");

        Log.e("progressUpdate",sec+" "+sec2+""+MyService.getDuration()+"");
        try {
            mBinder.setNotiUI(pref.getString("MUSIC_IMAGE", ""), pref.getString("MUSIC_TITLE", ""), isPlay());
        } catch (Exception e) {
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    public boolean isPlay() {
        boolean rtn = false;
        try {
            rtn = mBinder.isPlay();
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return rtn;
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

}
