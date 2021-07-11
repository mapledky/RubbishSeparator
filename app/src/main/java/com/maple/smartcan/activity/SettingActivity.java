package com.maple.smartcan.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.maple.smartcan.R;
import com.maple.smartcan.adapter.StateAdapter;
import com.maple.smartcan.service.JWebService;
import com.maple.smartcan.util.AvailableState;
import com.maple.smartcan.util.ViewControl;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import dmax.dialog.SpotsDialog;

public class SettingActivity extends PermissionActivity implements View.OnClickListener, StateAdapter.ChooseStateListener {

    //相关UI组件
    private EditText et_baudrate;//波特率输入框
    private Button bt_connect;//尝试连接
    private SpotsDialog loadingDialog;//加载框
    private ListView lv_state;//展示地址栏
    private SmartRefreshLayout lv_refreshLayout;//listview的嵌套
    private SmartRefreshLayout lv_settingpane;//参数设定嵌套
    private TextView tv_agreement;//用户协议
    private ImageView iv_refresh;//刷新按钮

    //页面相关变量
    private String baudrate;//输入的波特率
    private ArrayList<AvailableState> statelist;//硬件地址
    private String path;//选择的地址

    //handler相关传递常量
    private final int CONNECT_READY = 201;//输入有效
    private final int CONNECT_NOTREADY = 202;//输入无效

    //适配器
    private StateAdapter adapter;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CONNECT_READY:
                    //输入有效
                    bt_connect.setClickable(true);
                    bt_connect.setBackground(getDrawable(R.drawable.blue_roundedittext));
                    bt_connect.setTextColor(getResources().getColor(R.color.stringcolor_white));
                    break;
                case CONNECT_NOTREADY:
                    bt_connect.setClickable(false);
                    bt_connect.setBackground(getDrawable(R.drawable.round_edittext));
                    bt_connect.setTextColor(getResources().getColor(R.color.stringcolor_grey));
                    //输入无效
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        registerBroadCast();//注册广播
        init();
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(this, JWebService.class);
        stopService(intent);
        super.onDestroy();
    }

    private void init() {
        //初始化
        et_baudrate = findViewById(R.id.setting_inputbaudrate);
        bt_connect = findViewById(R.id.setting_connect);
        lv_state = findViewById(R.id.setting_listview);
        lv_refreshLayout = findViewById(R.id.setting_refreshlayout);
        lv_settingpane = findViewById(R.id.setting_datapane);
        tv_agreement = findViewById(R.id.setting_agreement);
        iv_refresh = findViewById(R.id.refreshState);

        iv_refresh.setOnClickListener(this);

        bt_connect.setOnClickListener(this);
        tv_agreement.setOnClickListener(this);
        statelist = new ArrayList<>();

        adapter = new StateAdapter(statelist, this);
        adapter.setChooseStateListener(this);
        lv_state.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        decoratePane(lv_settingpane);
        decoratePane(lv_refreshLayout);
        decorateLoading();//初始化加载框
        //获取当前有效的串口地址
        getvailableState();
        //监听输入框中是否不为空
        et_baudrate.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence chars, int arg1, int arg2, int arg3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                Message msg = handler.obtainMessage();
                if (editable.length() > 0) {
                    //输入有效
                    msg.what = CONNECT_READY;
                    msg.sendToTarget();
                } else {
                    //输入无效
                    msg.what = CONNECT_NOTREADY;
                    msg.sendToTarget();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence chars, int arg1, int arg2, int arg3) {

            }
        });
    }


    @Override
    public void onClick(View view) {
        if (ViewControl.avoidRetouch()) {
            switch (view.getId()) {
                case R.id.setting_connect:
                    //尝试连接
                    baudrate = et_baudrate.getText().toString();
                    chooseLocation();//选择串口的硬件地址
                    break;
                case R.id.setting_agreement:
                    Intent intent = new Intent(SettingActivity.this, MainActivity.class);
                    startActivity(intent);
                    break;

                case R.id.refreshState:
                    getvailableState();
                    break;
                default:

                    break;
            }
        }
    }

    //装饰加载条
    private void decorateLoading() {
        loadingDialog = new SpotsDialog(this);
        loadingDialog.setCanceledOnTouchOutside(false);
    }

    //修饰弹性scrollview
    private void decoratePane(SmartRefreshLayout pane) {
        pane.setHeaderHeight(40);
        pane.setFooterHeight(30);
        pane.setEnableRefresh(false);//禁止
        pane.setEnableLoadMore(false);//设置下拉刷新允许
        pane.setEnableOverScrollDrag(true);
    }


    //通过对话框选择有效的串口硬件地址
    private void chooseLocation() {
        if (path != null) {
            if (!path.equals("")) {
                loadingDialog.show();
                openSerialPort();
            } else {
                Toast.makeText(this, getString(R.string.chooseserialport), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.chooseserialport), Toast.LENGTH_SHORT).show();
        }
    }

    //在选择区域选择了相关的地址
    @Override
    public void chooseState(int index) {
        AvailableState state = statelist.get(index);
        for (int i = 0; i < statelist.size(); i++) {
            statelist.get(i).choosed = 0;
        }
        statelist.get(index).choosed = 1;
        adapter.notifyDataSetChanged();
        path = state.state;
        Toast.makeText(this, state.state, Toast.LENGTH_SHORT).show();
    }

    //传入相关参数(波特率)开启串口
    private void openSerialPort() {
        //向service发送广播，通知service连接相关的串口
        Intent intent = new Intent();
        intent.setAction("com.maple.BaudrateReciver");
        intent.putExtra("baudrate", baudrate);
        intent.putExtra("path", path);
        sendBroadcast(intent);
    }

    /*
    当前页面所需的相应的广播
     */

    //注册所有的接收器
    private void registerBroadCast() {
        registerSerialPortReceiver();//注册串口开启是否成功的接收器
    }


    //接收串口开启成功或者失败的广播
    private class OpenSerialPortReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("result");
            assert result != null;
            if (result.equals("0")) {
                //开启失败
                loadingDialog.dismiss();
                Toast.makeText(SettingActivity.this, getResources().getString(R.string.serial_fail), Toast.LENGTH_SHORT).show();
            } else {
                //开启成功
                loadingDialog.dismiss();
                Toast.makeText(SettingActivity.this, getResources().getString(R.string.serial_success), Toast.LENGTH_SHORT).show();
                Intent intent_tomain = new Intent(SettingActivity.this, MainActivity.class);
                startActivity(intent_tomain);
            }
        }
    }

    public void registerSerialPortReceiver() {
        OpenSerialPortReceiver openSerialPortReceiver = new OpenSerialPortReceiver();
        IntentFilter intentFilter = new IntentFilter("com.maple.OpenSerialPortReceiver");
        registerReceiver(openSerialPortReceiver, intentFilter);
    }


    //获取有效的串口地址
    private void getvailableState() {
        loadingDialog.show();
        path = null;
        statelist.clear();
        search();
        adapter.notifyDataSetChanged();
        if (statelist.size() == 0) {
            //没有可用的串口地址
            Toast.makeText(this, getString(R.string.noserialport), Toast.LENGTH_SHORT).show();
            loadingDialog.dismiss();
        } else {
            loadingDialog.dismiss();
        }
    }

    //查询设备
    private void search() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        //try get enable printer dev
        if (drivers.size() > 0) {
            statelist.clear();
            for (UsbSerialDriver driver : drivers) {
                List<UsbSerialPort> ports = driver.getPorts();
                for (int i = 0; i < ports.size(); i++) {
                    AvailableState state = new AvailableState();
                    state.state = ports.get(i).getDevice().getDeviceName();
                    state.choosed = 0;
                    statelist.add(state);
                }
            }
            adapter.notifyDataSetChanged();
        }
    }
}