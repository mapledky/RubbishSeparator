package com.maple.smartcan.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.maple.smartcan.R;
import com.maple.smartcan.service.JWebService;
import com.maple.smartcan.util.SmartCanUtil;
import com.maple.smartcan.util.ViewControl;
import com.maple.smartcan.util.order;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends PermissionActivity implements View.OnClickListener {
    //ui空间
    private ImageView iv_multi;//左上角多选择
    private TextView tv_time;//标题栏时间显示
    private ImageView iv_garbage;//标题栏垃圾桶图标
    private ImageView iv_question;//帮助图标

    private TextView tv_temp;//温度显示
    private TextView tv_water;//湿度显示
    private TextView tv_weight;//重量显示
    private TextView tv_fire;//可燃性气体显示
    private TextView tv_garbagestate;//垃圾桶状态显示：已满或否
    private TextView tv_garbageCloseorOn;//垃圾桶开合状态显示
    private FrameLayout frameLayout_open;//开启垃圾桶
    private TextView tv_touchtoopen;//开启
    private LinearLayout layout_mainshow;//主要展示界面
    private ImageView iv_back;//返回

    private ImageView iv_recycle;
    private ImageView iv_kitchen;
    private ImageView iv_other;
    private ImageView iv_harm;

    private TextView tv_recycle_open;
    private TextView tv_kitchen_open;
    private TextView tv_other_open;
    private TextView tv_harm_open;

    private FrameLayout frameLayout_num1;
    private FrameLayout frameLayout_num2;
    private FrameLayout frameLayout_num3;
    private FrameLayout frameLayout_num4;
    private FrameLayout frameLayout_num5;
    private FrameLayout frameLayout_num6;
    private FrameLayout frameLayout_num7;
    private FrameLayout frameLayout_num8;
    private FrameLayout frameLayout_num9;
    private FrameLayout frameLayout_num0;
    private FrameLayout frameLayout_agree;

    private ArrayList<SmartCanUtil> canlist = null;

    private LinkedBlockingQueue<byte[]> receive_candata;
    int refresh_UI = 0;
    private int current_can;//当前所在的页面
    /*
    0:四个垃圾桶纵览页面
    1：可回收垃圾桶页面
    2:厨余垃圾桶
    3：其他垃圾桶
    4:有害垃圾桶页面
     */

    private final int TIME_CHANGE = 201;
    private final int REFRESH_UI = 202;

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case TIME_CHANGE:
                    String time = (String) msg.obj;
                    tv_time.setText(time);
                    break;
                case REFRESH_UI:
                    refreshUiBydata();//根据数据更新当前的UI
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        registerAllBroad();
    }

    @SuppressLint("SetTextI18n")
    private void init() {
        iv_multi = findViewById(R.id.mainac_toppane_multi);
        tv_time = findViewById(R.id.mainac_toppane_time);
        //标题栏垃圾桶id显示
        TextView tv_id = findViewById(R.id.mainac_toppane_id);
        iv_garbage = findViewById(R.id.mainac_toppane_garbage);
        iv_question = findViewById(R.id.mainac_toppane_question);
        tv_temp = findViewById(R.id.mainac_despane_temprature);
        tv_water = findViewById(R.id.mainac_despane_water);
        tv_weight = findViewById(R.id.mainac_despane_weight);
        tv_fire = findViewById(R.id.mainac_despane_fire);
        tv_garbagestate = findViewById(R.id.mainac_despane_garbagestate);
        tv_garbageCloseorOn = findViewById(R.id.mainac_despane_garbagecloseoropen);
        tv_touchtoopen = findViewById(R.id.mainac_despane_touchtoopen);

        iv_recycle = findViewById(R.id.mainac_controlpane_recycle);
        iv_kitchen = findViewById(R.id.mainac_controlpane_kitchen);
        iv_other = findViewById(R.id.mainac_controlpane_other);
        iv_harm = findViewById(R.id.mainac_controlpane_harmful);
        iv_back = findViewById(R.id.mainac_controlpane_back);


        tv_recycle_open = findViewById(R.id.mainac_controlpane_recycleopen);
        tv_kitchen_open = findViewById(R.id.mainac_controlpane_kitchenopen);
        tv_other_open = findViewById(R.id.mainac_controlpane_otheropen);
        tv_harm_open = findViewById(R.id.mainac_controlpane_harmfulopen);

        //垃圾桶扫描器状态显示
        TextView tv_garbageScanOn = findViewById(R.id.mainac_despane_scanon);
        frameLayout_open = findViewById(R.id.mainac_despane_opencan);
        frameLayout_num0 = findViewById(R.id.mainac_controlpane_num0);
        frameLayout_num1 = findViewById(R.id.mainac_controlpane_num1);
        frameLayout_num2 = findViewById(R.id.mainac_controlpane_num2);
        frameLayout_num3 = findViewById(R.id.mainac_controlpane_num3);
        frameLayout_num4 = findViewById(R.id.mainac_controlpane_num4);
        frameLayout_num5 = findViewById(R.id.mainac_controlpane_num5);
        frameLayout_num6 = findViewById(R.id.mainac_controlpane_num6);
        frameLayout_num7 = findViewById(R.id.mainac_controlpane_num7);
        frameLayout_num8 = findViewById(R.id.mainac_controlpane_num8);
        frameLayout_num9 = findViewById(R.id.mainac_controlpane_num9);
        frameLayout_agree = findViewById(R.id.mainac_controlpane_agree);


        layout_mainshow = findViewById(R.id.mainac_despane_mainshow);
        iv_recycle.setOnClickListener(this);
        iv_kitchen.setOnClickListener(this);
        iv_other.setOnClickListener(this);
        iv_harm.setOnClickListener(this);
        tv_recycle_open.setOnClickListener(this);
        tv_other_open.setOnClickListener(this);
        tv_kitchen_open.setOnClickListener(this);
        tv_harm_open.setOnClickListener(this);
        frameLayout_num0.setOnClickListener(this);
        frameLayout_num1.setOnClickListener(this);
        frameLayout_num2.setOnClickListener(this);
        frameLayout_num3.setOnClickListener(this);
        frameLayout_num4.setOnClickListener(this);
        frameLayout_num5.setOnClickListener(this);
        frameLayout_num6.setOnClickListener(this);
        frameLayout_num7.setOnClickListener(this);
        frameLayout_num8.setOnClickListener(this);
        frameLayout_num9.setOnClickListener(this);
        frameLayout_agree.setOnClickListener(this);
        frameLayout_open.setOnClickListener(this);
        iv_multi.setOnClickListener(this);
        iv_garbage.setOnClickListener(this);
        iv_question.setOnClickListener(this);
        iv_back.setOnClickListener(this);

        //页面相关数据
        //可回收垃圾桶数据
        SmartCanUtil can_recycle = new SmartCanUtil();
        SmartCanUtil can_kitchen = new SmartCanUtil();
        SmartCanUtil can_other = new SmartCanUtil();
        SmartCanUtil can_harm = new SmartCanUtil();
        canlist = new ArrayList<>();
        canlist.add(can_recycle);
        canlist.add(can_kitchen);
        canlist.add(can_other);
        canlist.add(can_harm);

        receive_candata = new LinkedBlockingQueue<>();//接受读取数据的容器
        current_can = 0;//当前所在的页面
        //时钟线程
        SharedPreferences sharedPreferences = getSharedPreferences("account", Context.MODE_PRIVATE);
        String account = sharedPreferences.getString("Id", "");
        tv_id.setText(this.getResources().getString(R.string.code) + account);
        timeThread();

        //更新数据线程
        refreshCanData();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        if (ViewControl.avoidRetouch()) {
            switch (v.getId()) {
                case R.id.mainac_toppane_multi:
                    //点击左上角更多按钮
                    break;
                case R.id.mainac_toppane_garbage:
                    //点击右上角垃圾桶按钮
                    break;
                case R.id.mainac_toppane_question:
                    //帮助
                    break;
                case R.id.mainac_despane_opencan:
                    controlCan(0);
                    //点击开启垃圾桶
                    break;
                case R.id.mainac_controlpane_num0:
                    break;
                case R.id.mainac_controlpane_num1:
                    break;
                case R.id.mainac_controlpane_num2:
                    break;
                case R.id.mainac_controlpane_num3:
                    break;
                case R.id.mainac_controlpane_num4:
                    break;
                case R.id.mainac_controlpane_num5:
                    break;
                case R.id.mainac_controlpane_num6:
                    break;
                case R.id.mainac_controlpane_num7:
                    break;
                case R.id.mainac_controlpane_num8:
                    break;
                case R.id.mainac_controlpane_num9:
                    break;
                case R.id.mainac_controlpane_agree:
                    break;


                case R.id.mainac_controlpane_recycle:
                    toMainInfo(1);
                    break;
                case R.id.mainac_controlpane_kitchen:
                    toMainInfo(2);
                    break;
                case R.id.mainac_controlpane_harmful:
                    toMainInfo(3);
                    break;
                case R.id.mainac_controlpane_other:
                    toMainInfo(4);
                    break;

                case R.id.mainac_controlpane_recycleopen:
                    controlCan(1);
                    break;
                case R.id.mainac_controlpane_kitchenopen:
                    controlCan(2);
                    break;
                case R.id.mainac_controlpane_otheropen:
                    controlCan(3);
                    break;
                case R.id.mainac_controlpane_harmfulopen:
                    controlCan(4);
                    break;

                case R.id.mainac_controlpane_back:
                    backToMain();
                    break;

                default:
                    break;
            }
        }
    }


    //开启时钟线程
    private void timeThread() {
        new Thread(() -> {
            while (true) {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日 HH时mm分");
                Date date = new Date(System.currentTimeMillis());
                String time = sdf.format(date);
                Message msg = handler.obtainMessage();
                msg.what = TIME_CHANGE;
                msg.obj = time;
                msg.sendToTarget();
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    //更新垃圾桶数据线程
    private void refreshCanData() {
        new Thread(() -> {
            while (true) {
                byte[] data = null;
                try {
                    data = receive_candata.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assert data != null;
                switch (refresh_UI) {
                    case 0:
                        //温度ui
                        canlist.get(data[0] - 1).temp = (float) ((data[3] * 256 + data[4]) / 100.0);
                        break;
                    case 1:
                        //湿度
                        canlist.get(data[0] - 1).water = (float) ((data[3] * 256 + data[4]) / 100.0);
                        break;
                    case 2:
                        //可燃性气体浓度
                        int number = (int) (((data[3] * 256 + data[4]) * 3.3)/4096*100);
                        canlist.get(data[0] - 1).fire = (float) (number/100.0);
                        break;
                    case 3:
                        //重量
                        canlist.get(data[0] - 1).weight = (float) ((((data[3] * 256 + data[4]) * 256 + data[5]) * 256 + data[6]) / 100.0);
                        break;
                    case 4:
                        //桶满状态
                        canlist.get(data[0] - 1).canstate = (data[4]);
                        break;
                    case 5:
                        //推杆状态
                        canlist.get(data[0] - 1).openstate = data[3];
                        break;
                    case 6:
                        //用户id时效性
                        Log.i("receivedata", Arrays.toString(data));
                        break;
                    case 7:
                        //用户id
                        Log.i("receivedata", "id"+Arrays.toString(data));
                        break;
                    case 8:
                        //超声波数据
                        //刷新页面UI
                        Message msg = handler.obtainMessage();
                        msg.what = REFRESH_UI;
                        msg.sendToTarget();
                        break;
                    default:
                        break;

                }
                if (refresh_UI != 8) {
                    refresh_UI++;
                } else {
                    refresh_UI = 0;
                }

            }
        }).start();
    }


    //前往垃圾桶的详细数据页面
    private void toMainInfo(int i) {
        current_can = i;
        layout_mainshow.setVisibility(View.INVISIBLE);
        iv_back.setVisibility(View.VISIBLE);
        refreshUiBydata();
    }

    //返回主页面
    private void backToMain() {
        current_can = 0;
        layout_mainshow.setVisibility(View.VISIBLE);
        iv_back.setVisibility(View.INVISIBLE);
        refreshUiBydata();
    }

    //打开或关闭垃圾桶
    private void controlCan(int index) {
        SmartCanUtil smartCanUtil = null;

        if (index != 0) {
            smartCanUtil = canlist.get(index - 1);
            if (smartCanUtil.openstate == 0) {
                //当前状态,关闭
                Intent intent_broad = new Intent();
                intent_broad.setAction("com.maple.sendMsgReceiver");
                byte[] order_byte = order.command_opencan;
                order_byte[0] = (byte) index;
                intent_broad.putExtra("data", order_byte);
                sendBroadcast(intent_broad);
            } else {
                //当前状态，打开
                Intent intent_broad = new Intent();
                intent_broad.setAction("com.maple.sendMsgReceiver");
                byte[] order_byte = order.command_closecan;
                order_byte[0] = (byte) index;
                intent_broad.putExtra("data", order_byte);
                sendBroadcast(intent_broad);
            }
        } else {
            //当前在具体的页面
            smartCanUtil = canlist.get(current_can - 1);
            if (smartCanUtil.openstate == 0) {
                //当前状态,关闭
                Intent intent_broad = new Intent();
                intent_broad.setAction("com.maple.sendMsgReceiver");
                byte[] order_byte = order.command_opencan;
                order_byte[0] = (byte) current_can;
                intent_broad.putExtra("data", order_byte);
                sendBroadcast(intent_broad);
            } else {
                //当前状态，打开
                Intent intent_broad = new Intent();
                intent_broad.setAction("com.maple.sendMsgReceiver");
                byte[] order_byte = order.command_closecan;
                order_byte[0] = (byte) current_can;
                intent_broad.putExtra("data", order_byte);
                sendBroadcast(intent_broad);
            }
        }
    }


    //根据数据更新当前UI
    @SuppressLint("SetTextI18n")
    private void refreshUiBydata() {
        SmartCanUtil smartCanUtil;
        for (int i = 0; i < canlist.size(); i++) {
            smartCanUtil = canlist.get(i);
            //判断当前垃圾桶是否满
            if (smartCanUtil.canstate == 1) {
                //当前垃圾桶已满
                switch (i) {
                    case 0:
                        tv_recycle_open.setText(getResources().getString(R.string.full));
                        tv_recycle_open.setBackgroundColor(getResources().getColor(R.color.background3));
                        tv_recycle_open.setClickable(false);
                        break;
                    case 1:
                        tv_kitchen_open.setText(getResources().getString(R.string.full));
                        tv_recycle_open.setBackgroundColor(getResources().getColor(R.color.background3));
                        tv_recycle_open.setClickable(false);
                        break;
                    case 2:
                        tv_other_open.setText(getResources().getString(R.string.full));
                        tv_recycle_open.setBackgroundColor(getResources().getColor(R.color.background3));
                        tv_recycle_open.setClickable(false);
                        break;
                    case 3:
                        tv_harm_open.setText(getResources().getString(R.string.full));
                        tv_recycle_open.setBackgroundColor(getResources().getColor(R.color.background3));
                        tv_recycle_open.setClickable(false);
                        break;
                    default:
                        break;

                }
            } else {
                //当前垃圾桶未满
                if (smartCanUtil.openstate == 0) {
                    //垃圾桶未开
                    switch (i) {
                        case 0:
                            tv_recycle_open.setText(getResources().getString(R.string.open));
                            tv_recycle_open.setBackgroundColor(getResources().getColor(R.color.recycle));
                            tv_recycle_open.setClickable(true);
                            break;
                        case 1:
                            tv_kitchen_open.setText(getResources().getString(R.string.open));
                            tv_kitchen_open.setBackgroundColor(getResources().getColor(R.color.kitchen));
                            tv_kitchen_open.setClickable(true);
                            break;
                        case 2:
                            tv_other_open.setText(getResources().getString(R.string.open));
                            tv_other_open.setBackgroundColor(getResources().getColor(R.color.other));
                            tv_other_open.setClickable(true);
                            break;
                        case 3:
                            tv_harm_open.setText(getResources().getString(R.string.open));
                            tv_harm_open.setBackgroundColor(getResources().getColor(R.color.harm));
                            tv_harm_open.setClickable(true);
                            break;
                        default:
                            break;
                    }
                } else {
                    //垃圾桶开
                    switch (i) {
                        case 0:
                            tv_recycle_open.setText(getResources().getString(R.string.close));
                            tv_recycle_open.setBackgroundColor(getResources().getColor(R.color.recycle));
                            tv_recycle_open.setClickable(true);
                            break;
                        case 1:
                            tv_kitchen_open.setText(getResources().getString(R.string.close));
                            tv_kitchen_open.setBackgroundColor(getResources().getColor(R.color.kitchen));
                            tv_kitchen_open.setClickable(true);
                            break;
                        case 2:
                            tv_other_open.setText(getResources().getString(R.string.close));
                            tv_other_open.setBackgroundColor(getResources().getColor(R.color.other));
                            tv_other_open.setClickable(true);
                            break;
                        case 3:
                            tv_harm_open.setText(getResources().getString(R.string.close));
                            tv_harm_open.setBackgroundColor(getResources().getColor(R.color.harm));
                            tv_harm_open.setClickable(true);
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        if (current_can != 0) {
            //当前在具体的数据页面
            smartCanUtil = canlist.get(current_can - 1);
            tv_temp.setText(String.valueOf(smartCanUtil.temp));
            tv_water.setText(smartCanUtil.water + "%");
            tv_fire.setText(smartCanUtil.fire + "v");
            tv_weight.setText(smartCanUtil.weight + "kg");
            if (smartCanUtil.canstate == 0) {
                tv_garbagestate.setText(getResources().getString(R.string.online));
                tv_garbagestate.setTextColor(getResources().getColor(R.color.viewback5));

                frameLayout_open.setClickable(true);
                frameLayout_open.setBackgroundColor(getResources().getColor(R.color.background2));
            } else {
                tv_garbagestate.setText(getResources().getString(R.string.full));
                tv_garbagestate.setTextColor(getResources().getColor(R.color.stringcolor_grey));

                frameLayout_open.setClickable(false);
                frameLayout_open.setBackgroundColor(getResources().getColor(R.color.background3));
            }
            if (smartCanUtil.openstate == 0) {
                //关闭
                tv_garbageCloseorOn.setText(getResources().getText(R.string.close));
                tv_touchtoopen.setText(getResources().getText(R.string.touchtoopen));
            } else {
                //开启
                tv_garbageCloseorOn.setText(getResources().getText(R.string.open));
                tv_touchtoopen.setText(getResources().getText(R.string.touchtoclose));
            }
            //判断是否满
        }
    }


    //注册所有广播
    private void registerAllBroad() {
        registerreceivemsgReceiver();//注册消息返回广播
    }


    //接收数据的广播
    private class ReceiveMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取前端传递的发送信息
            byte[] content = intent.getByteArrayExtra("content");
            operateData(content);
        }
    }

    private void registerreceivemsgReceiver() {
        ReceiveMessageReceiver receiver = new ReceiveMessageReceiver();
        IntentFilter intentFilter = new IntentFilter("com.maple.backMsgReceiver");
        registerReceiver(receiver, intentFilter);
    }


    //处理返回的数据
    private void operateData(byte[] content) {
        Log.i("mainactivity", Arrays.toString(content));
        //数据为读取数据返回数据
        if (content[1] != 0x05) {
            try {
                receive_candata.put(content);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}