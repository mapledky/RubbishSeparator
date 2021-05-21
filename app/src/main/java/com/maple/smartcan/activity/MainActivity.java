package com.maple.smartcan.activity;

import androidx.annotation.NonNull;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;

import com.bumptech.glide.Glide;
import com.maple.smartcan.R;
import com.maple.smartcan.network.HttpHelper;
import com.maple.smartcan.network.ServerCode;
import com.maple.smartcan.network.VollySimpleRequest;
import com.maple.smartcan.util.SmartCanUtil;
import com.maple.smartcan.util.ViewControl;
import com.maple.smartcan.util.order;


import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import dmax.dialog.SpotsDialog;

public class MainActivity extends PermissionActivity implements View.OnClickListener {

    private SpotsDialog loadingDialog;

    private TextView tv_time;//标题栏时间显示

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

    private TextView tv_recycle_open;
    private TextView tv_kitchen_open;
    private TextView tv_other_open;
    private TextView tv_harm_open;

    private TextView tv_inputphone;
    private ImageView iv_head;
    private TextView tv_name;
    private TextView tv_phone;
    private TextView tv_score;

    private String inputphoneNumber = "";//输入的电话号码

    //用户信息
    private String name;
    private String headstate = "0";
    private String score;
    private String user_id;
    private String phoneNumber;

    //用户信息刷新
    private int refreshUserInfo = 60;//60秒刷新


    private ArrayList<SmartCanUtil> canlist = null;

    private LinkedBlockingQueue<byte[]> receive_candata;
    int refresh_UI = 0;
    private int current_can;//当前所在的页面


    //线程总控变量
    boolean isThreadOpend = true;

    /*
    0:四个垃圾桶纵览页面
    1：可回收垃圾桶页面
    2:厨余垃圾桶
    3：其他垃圾桶
    4:有害垃圾桶页面
     */

    private final int TIME_CHANGE = 201;
    private final int REFRESH_UI = 202;
    private final int REFRESH_USERINFO = 203;

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
                case REFRESH_USERINFO:
                    refreshUserUi();
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        isThreadOpend = false;//关闭当前页面所有线程
    }

    @SuppressLint("SetTextI18n")
    private void init() {
        //ui空间
        //左上角多选择
        ImageView iv_multi = findViewById(R.id.mainac_toppane_multi);
        tv_time = findViewById(R.id.mainac_toppane_time);
        //标题栏垃圾桶id显示
        TextView tv_id = findViewById(R.id.mainac_toppane_id);
        //标题栏垃圾桶图标
        ImageView iv_garbage = findViewById(R.id.mainac_toppane_garbage);
        //帮助图标
        ImageView iv_question = findViewById(R.id.mainac_toppane_question);
        tv_temp = findViewById(R.id.mainac_despane_temprature);
        tv_water = findViewById(R.id.mainac_despane_water);
        tv_weight = findViewById(R.id.mainac_despane_weight);
        tv_fire = findViewById(R.id.mainac_despane_fire);
        tv_garbagestate = findViewById(R.id.mainac_despane_garbagestate);
        tv_garbageCloseorOn = findViewById(R.id.mainac_despane_garbagecloseoropen);
        tv_touchtoopen = findViewById(R.id.mainac_despane_touchtoopen);

        ImageView iv_recycle = findViewById(R.id.mainac_controlpane_recycle);
        ImageView iv_kitchen = findViewById(R.id.mainac_controlpane_kitchen);
        ImageView iv_other = findViewById(R.id.mainac_controlpane_other);
        ImageView iv_harm = findViewById(R.id.mainac_controlpane_harmful);
        iv_back = findViewById(R.id.mainac_controlpane_back);


        tv_recycle_open = findViewById(R.id.mainac_controlpane_recycleopen);
        tv_kitchen_open = findViewById(R.id.mainac_controlpane_kitchenopen);
        tv_other_open = findViewById(R.id.mainac_controlpane_otheropen);
        tv_harm_open = findViewById(R.id.mainac_controlpane_harmfulopen);

        //垃圾桶扫描器状态显示
        frameLayout_open = findViewById(R.id.mainac_despane_opencan);
        FrameLayout frameLayout_num0 = findViewById(R.id.mainac_controlpane_num0);
        FrameLayout frameLayout_num1 = findViewById(R.id.mainac_controlpane_num1);
        FrameLayout frameLayout_num2 = findViewById(R.id.mainac_controlpane_num2);
        FrameLayout frameLayout_num3 = findViewById(R.id.mainac_controlpane_num3);
        FrameLayout frameLayout_num4 = findViewById(R.id.mainac_controlpane_num4);
        FrameLayout frameLayout_num5 = findViewById(R.id.mainac_controlpane_num5);
        FrameLayout frameLayout_num6 = findViewById(R.id.mainac_controlpane_num6);
        FrameLayout frameLayout_num7 = findViewById(R.id.mainac_controlpane_num7);
        FrameLayout frameLayout_num8 = findViewById(R.id.mainac_controlpane_num8);
        FrameLayout frameLayout_num9 = findViewById(R.id.mainac_controlpane_num9);
        FrameLayout frameLayout_agree = findViewById(R.id.mainac_controlpane_agree);
        tv_inputphone = findViewById(R.id.mainac_controlpane_inputphone);
        iv_head = findViewById(R.id.userinfo_head);
        tv_name = findViewById(R.id.userinfo_name);
        tv_phone = findViewById(R.id.userinfo_phone);
        tv_score = findViewById(R.id.userinfo_score);

        Button bt_clear = findViewById(R.id.mainac_controlpane_clearphone);

        bt_clear.setOnClickListener(this);
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


        SharedPreferences sharedPreferences = getSharedPreferences("account", Context.MODE_PRIVATE);
        String account = sharedPreferences.getString("Id", "");
        tv_id.setText(this.getResources().getString(R.string.code) + account);
        decorateLoading();

        /*

        当前页面所有的异步线程

         */


        timeThread();//时钟线程

        refreshCanData();//更新垃圾桶数据线程

        uploadDataWithSocket();//通过socket上传数据到服务器线程

        canAutoOpenControl();//轮训判断,当垃圾桶开启，且超声波测距大于一米时，垃圾桶关闭，小于一米时垃圾桶开启

        refreshDefaultUserInfo();//用户信息刷新
    }

    //装饰加载条
    private void decorateLoading() {
        if (loadingDialog == null) {
            loadingDialog = new SpotsDialog(this);
            loadingDialog.setCanceledOnTouchOutside(false);
        }
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
                    touchNumber(0);
                    break;
                case R.id.mainac_controlpane_num1:
                    touchNumber(1);
                    break;
                case R.id.mainac_controlpane_num2:
                    touchNumber(2);
                    break;
                case R.id.mainac_controlpane_num3:
                    touchNumber(3);
                    break;
                case R.id.mainac_controlpane_num4:
                    touchNumber(4);
                    break;
                case R.id.mainac_controlpane_num5:
                    touchNumber(5);
                    break;
                case R.id.mainac_controlpane_num6:
                    touchNumber(6);
                    break;
                case R.id.mainac_controlpane_num7:
                    touchNumber(7);
                    break;
                case R.id.mainac_controlpane_num8:
                    touchNumber(8);
                    break;
                case R.id.mainac_controlpane_num9:
                    touchNumber(9);
                    break;
                case R.id.mainac_controlpane_agree:
                    agreeNumer();
                    break;

                case R.id.mainac_controlpane_clearphone:
                    clearNumber();
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

    //输入电话号码
    private void touchNumber(int i) {
        if (inputphoneNumber.length() != 11) {
            inputphoneNumber += String.valueOf(i);
            tv_inputphone.setText(inputphoneNumber);
        }
    }

    //清空电话
    private void clearNumber() {
        inputphoneNumber = "";
        tv_inputphone.setText("");
        tv_inputphone.setHint(getResources().getString(R.string.input_phone));
    }

    //确认电话号码
    private void agreeNumer() {
        if (inputphoneNumber.length() == 11) {
            getUserInfo(2, inputphoneNumber);
        } else {
            Toast.makeText(this, getResources().getString(R.string.phonetypeerror), Toast.LENGTH_SHORT).show();
        }
    }

    //获取用户信息
    private void getUserInfo(int type, String data) {
        loadingDialog.show();
        Map<String, String> params = new HashMap<>();
        if (type == 1) {
            //id获取
            params.put("requestCode", ServerCode.GETINFO_ID);
            params.put("user_id", data);
        } else {
            //phoneNumber获取
            params.put("requestCode", ServerCode.GETINFO_NUMBER);
            params.put("phoneNumber", data);
        }
        VollySimpleRequest.getInstance(this).sendStringRequest(Request.Method.POST, HttpHelper.MAIN_MOBILE, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                String result = jsonObject.getString("result");
                if (result.equals("1")) {
                    inputphoneNumber = "";
                    tv_inputphone.setText(getString(R.string.input_phone));
                    //获取成功
                    loadingDialog.dismiss();
                    user_id = jsonObject.getString("Id");
                    phoneNumber = jsonObject.getString("phoneNumber");
                    name = jsonObject.getString("username");
                    headstate = jsonObject.getString("headstate");
                    score = jsonObject.getString("score");

                    refreshUserUi();//展示用户的信息
                    addUserScore();
                } else {
                    loadingDialog.dismiss();
                    Toast.makeText(MainActivity.this, getString(R.string.fail), Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                loadingDialog.dismiss();
                e.printStackTrace();
            }
        }, error -> loadingDialog.dismiss(), params);
    }

    //获取到了user_id
    private void getUser_id(int can_index, String id) {
        SmartCanUtil canUtil = canlist.get(can_index - 1);
        if (canUtil.openstate == 0) {
            //垃圾桶关闭状态,打开
            Intent intent_broad = new Intent();
            intent_broad.setAction("com.maple.sendMsgReceiver");
            byte[] order_byte = order.command_opencan;
            order_byte[0] = (byte) can_index;
            intent_broad.putExtra("data", order_byte);
            sendBroadcast(intent_broad);
        }
        //获取用户信息并添加用户积分
        getUserInfo(1, id);
    }

    //刷新展示用户的相关信息
    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    private void refreshUserUi() {
        if (headstate != null) {
            if (!headstate.equals("0")) {
                Glide.with(this).load(headstate).into(iv_head);
            } else {
                iv_head.setImageDrawable(getDrawable(R.drawable.qrcode));
            }
        }
        if (name != null) {
            tv_name.setText(name);
        }
        if (phoneNumber != null) {
            tv_phone.setText(getResources().getString(R.string.phone) + phoneNumber);
        }
        if (score != null) {
            tv_score.setText(score);
        }
        refreshUserInfo = 60;

    }

    private void addUserScore() {
        if (user_id != null) {
            loadingDialog.show();
            Map<String, String> params = new HashMap<>();
            params.put("requestCode", ServerCode.ADDUSERSCORE);
            params.put("user_id", user_id);
            VollySimpleRequest.getInstance(this).sendStringRequest(Request.Method.POST, HttpHelper.MAIN_CONTROL, response -> {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String result = jsonObject.getString("result");
                    if (result.equals("1")) {
                        score = String.valueOf(Integer.parseInt(score) + 1);
                        refreshUserUi();//展示用户的信息
                    } else {
                        loadingDialog.dismiss();
                        Toast.makeText(MainActivity.this, getString(R.string.hourperone), Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    loadingDialog.dismiss();
                    e.printStackTrace();
                }
            }, error -> loadingDialog.dismiss(), params);
        }
    }

    //用户信息刷新县城
    private void refreshDefaultUserInfo() {
        new Thread(() -> {
            while (isThreadOpend) {
                if (refreshUserInfo == 0) {
                    headstate = "0";
                    name = getResources().getString(R.string.username);
                    phoneNumber = "";
                    user_id = null;
                    score = "0";
                    Message msg = handler.obtainMessage();
                    msg.what = REFRESH_USERINFO;
                    msg.sendToTarget();
                } else {
                    refreshUserInfo--;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //开启时钟线程
    private void timeThread() {
        new Thread(() -> {
            while (isThreadOpend) {
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
            while (isThreadOpend) {
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
                        if (data.length >= 5) {
                            canlist.get(data[0] - 1).temp = (float) ((data[3] * 256 + data[4]) / 100.0);
                        }
                        break;
                    case 1:
                        //湿度
                        if (data.length >= 5) {
                            canlist.get(data[0] - 1).water = (float) ((data[3] * 256 + data[4]) / 100.0);
                        }
                        break;
                    case 2:
                        //可燃性气体浓度
                        if (data.length >= 5) {
                            int number = (int) (((data[3] * 256 + data[4]) * 3.3) / 4096 * 100);
                            canlist.get(data[0] - 1).fire = (float) (number / 100.0);
                        }
                        break;
                    case 3:
                        //重量
                        if (data.length >= 7) {
                            canlist.get(data[0] - 1).weight = (float) ((((data[3] * 256 + data[4]) * 256 + data[5]) * 256 + data[6]) / 100.0);
                        }
                        break;
                    case 4:
                        //桶满状态
                        if (data.length >= 5) {
                            if (data[4] == 0) {
                                canlist.get(data[0] - 1).canstate = 1;
                            } else {
                                canlist.get(data[0] - 1).canstate = 0;
                            }
                        }
                        break;
                    case 5:
                        //推杆状态
                        if (data.length >= 4) {
                            canlist.get(data[0] - 1).openstate = data[3];
                        }
                        break;
                    case 6:
                        //用户id时效性
                        if (data.length >= 4) {
                            int useful = data[3];
                            if (useful == 1) {
                                //用户id有效

                                try {
                                    data = receive_candata.take();
                                    //获取到了user_id
                                    getUser_id(data[0], String.valueOf(data[3]));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                refresh_UI++;
                            }
                        }
                        break;
                    case 7:
                        //无效用户id

                        break;
                    case 8:
                        //超声波数据
                        if (data.length >= 5) {
                            canlist.get(data[0] - 1).distant = data[3] * 256 + data[4];

                            //刷新页面UI
                            Message msg = handler.obtainMessage();
                            msg.what = REFRESH_UI;
                            msg.sendToTarget();
                        }
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
        SmartCanUtil smartCanUtil;
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

    //垃圾桶自动开合
    public void canAutoOpenControl() {
        new Thread(() -> {
            while (isThreadOpend) {
                SmartCanUtil canUtil;
                for (int i = 0; i < canlist.size(); i++) {
                    canUtil = canlist.get(i);
                    if (canUtil.distant >= 1000 && canUtil.openstate == 1) {
                        //超声波距离大于一米且垃圾桶打开
                        Intent intent_broad = new Intent();
                        intent_broad.setAction("com.maple.sendMsgReceiver");
                        byte[] order_byte = order.command_closecan;
                        order_byte[0] = (byte) (i + 1);
                        intent_broad.putExtra("data", order_byte);
                        sendBroadcast(intent_broad);
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //上传数据到数据库
    public void uploadDataWithSocket() {
        new Thread(() -> {
            while (isThreadOpend) {
                SmartCanUtil canUtil;
                canUtil = canlist.get(0);//可回收垃圾
                String recycle = canUtil.temp + "&" + canUtil.water + "&" + canUtil.fire + "&" + canUtil.weight + "&" + canUtil.canstate + "&" + canUtil.openstate;
                canUtil = canlist.get(1);//厨余垃圾
                String kitchen = canUtil.temp + "&" + canUtil.water + "&" + canUtil.fire + "&" + canUtil.weight + "&" + canUtil.canstate + "&" + canUtil.openstate;
                canUtil = canlist.get(2);//其他垃圾
                String other = canUtil.temp + "&" + canUtil.water + "&" + canUtil.fire + "&" + canUtil.weight + "&" + canUtil.canstate + "&" + canUtil.openstate;
                canUtil = canlist.get(3);//有害
                String harm = canUtil.temp + "&" + canUtil.water + "&" + canUtil.fire + "&" + canUtil.weight + "&" + canUtil.canstate + "&" + canUtil.openstate;

                String message = ServerCode.UPLOAD_DATA + "/" + recycle + "/" + kitchen + "/" + other + "/" + harm;
                Intent intent = new Intent();
                intent.putExtra("message", message);
                intent.setAction("com.maple.sendSocketMsgReceiver");
                sendBroadcast(intent);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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