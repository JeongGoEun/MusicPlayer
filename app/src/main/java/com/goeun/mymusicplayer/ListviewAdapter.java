package com.goeun.mymusicplayer;

import android.app.LauncherActivity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 고은 on 2017-12-18.
 */

public class ListviewAdapter extends BaseAdapter {
    private Context mContext;
    private List<MainActivity.songInfo> items = new ArrayList<MainActivity.songInfo>();
    ViewHolder holder;
    LayoutInflater inflater;

    public ListviewAdapter(Context c) {
        //context, 곡 정보 가져오기
        mContext = c;
    }
    public void addItem(MainActivity.songInfo item){
        //곡 정보가 들어있는 클래스 listview에 넣음
        items.add(item);
    }
    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view==null) {
            holder = new ViewHolder();
            inflater= (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view=inflater.inflate(R.layout.playlist,viewGroup,false);
            holder.imgView=(ImageView)view.findViewById(R.id.imgView);
            holder.titleTxt=(TextView)view.findViewById(R.id.titleTxt);

            view.setTag(holder);
        }
        else{
            holder=(ViewHolder)view.getTag();
        }
        //이미지, 제목 저장
        holder.imgView.setImageURI(items.get(i).imgUri);
        holder.titleTxt.setText(items.get(i).title);
        return view;
    }

    private class ViewHolder{
        private TextView titleTxt;
        private ImageView imgView;
    }
}

