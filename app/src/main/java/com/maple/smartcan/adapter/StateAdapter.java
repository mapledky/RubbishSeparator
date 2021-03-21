package com.maple.smartcan.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.maple.smartcan.R;
import com.maple.smartcan.util.AvailableState;
import com.maple.smartcan.util.ViewControl;

import java.util.ArrayList;

/*
用于展示当前可用的串口地址
 */
public class StateAdapter extends BaseAdapter {
    private ArrayList<AvailableState> stateList = new ArrayList<>();
    private Context context;

    public StateAdapter(ArrayList<AvailableState> list, Context context) {
        this.stateList = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        return stateList.size();
    }

    @Override
    public Object getItem(int i) {
        return stateList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    private ChooseStateListener listener;

    public interface ChooseStateListener {
        void chooseState(int index);
    }

    public void setChooseStateListener(ChooseStateListener chooseStateListener) {
        this.listener = chooseStateListener;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.state_list, null);
        }

        String text = stateList.get(i).state;
        if(stateList.get(i).choosed==1){
            //选择
            TextView tv_state = convertView.findViewById(R.id.state_list_text);
            tv_state.setTextColor(context.getResources().getColor(R.color.stringcolor_white));
            convertView.setBackgroundColor(context.getResources().getColor(R.color.background2));
            tv_state.setText(text);
        } else {
            //wei选择
            TextView tv_state = convertView.findViewById(R.id.state_list_text);
            tv_state.setTextColor(context.getResources().getColor(R.color.stringcolor_black));
            convertView.setBackgroundColor(context.getResources().getColor(R.color.background1));
            tv_state.setText(text);
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ViewControl.avoidRetouch()) {
                    listener.chooseState(i);
                }
            }
        });
        return convertView;
    }
}
