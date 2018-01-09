package com.goeun.mymusicplayer;

import java.util.ArrayList;

/**
 * Created by 고은 on 2017-12-23.
 */

public class PlayList {
    ArrayList<MainActivity.songInfo> lists;

    public  PlayList(){
        lists=new ArrayList<>();
    }
    public void addItem(MainActivity.songInfo item){
        lists.add(item);
    }
    public MainActivity.songInfo getItem(int index){
        return lists.get(index);
    }
}
