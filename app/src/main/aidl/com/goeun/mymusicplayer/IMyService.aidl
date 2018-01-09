// IMyService.aidl
package com.goeun.mymusicplayer;
// Declare any non-default types here with import statement


interface IMyService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void play(String musicPath);    //재생
    void pause(String musicPath);   //일시정지
    void fast(String musicPath);    //다음곡
    void back(String musicPath);    //이전곡
    boolean isPlay();   //실행중인지
    int getDuration();  //전체 시간
    int curDuration();  //총 시간
    void setPosition(int position);
    void setNotiUI(String imageUri,String title,boolean isPlay);   //노티 세팅 후 다시 재생
    //MediaPlayer getMusicPlayer();   //플레이어 getter
}
