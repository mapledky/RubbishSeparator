package com.maple.smartcan.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.maple.smartcan.activity.MainActivity;
import com.maple.smartcan.util.order;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import android_serialport_api.SerialPort;


/*
该sevice用于承载串口连接的相关实例以及接口
2021-1-24
Carry
 */

public class JWebService extends Service implements SerialPort.ReceiveListener {
    private final int PID = android.os.Process.myPid();
    private AssistServiceConnection mConnection;//辅助service

    public SerialPort serialPort;//串口的相关实例
    public boolean isSerialPortOpen = false;//串口是否链接

    public static String path;//硬件地址
    public static int baudrate;//串口的波特率


    public JWebService() {

    }


    @Override
    public void onCreate() {
        super.onCreate();
        setForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            initSocketClient();
            registerBroacast();//注册当前class所需的所有广播
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
    相关广播的接收组
     */

    //注册广播
    private void registerBroacast() {
        registerBaudrateReceiver();//注册波特率接收器广播
        registerSendmsgReceiver();//发送串口信息的广播
    }


    //获取到串口返回的数据
    @Override
    public void receiveMessage(byte[] message) {
        Intent intent_broad = new Intent();

        intent_broad.setAction("com.maple.backMsgReceiver");
        intent_broad.putExtra("content", message);
        sendBroadcast(intent_broad);
    }


    //发送串口信息的广播
    private class SendMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取前端传递的发送信息
            byte[] data = intent.getByteArrayExtra("data");
            ArrayList<byte[]> order_array = new ArrayList<>();
            order_array.add(data);
            if (serialPort.isPortOpen && isSerialPortOpen) {
                serialPort.addOrder(order_array);
            }
        }
    }

    private void registerSendmsgReceiver() {
        SendMessageReceiver receiver = new SendMessageReceiver();
        IntentFilter intentFilter = new IntentFilter("com.maple.sendMsgReceiver");
        registerReceiver(receiver, intentFilter);
    }

    //接受波特率参数的广播
    private class BaudrateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取前端传递的波特率参数
            path = Objects.requireNonNull(intent.getStringExtra("path"));
            baudrate = Integer.parseInt(Objects.requireNonNull(intent.getStringExtra("baudrate")));
            //尝试开启串口
            if (startSerialPort()) {
                //开启串口成功
                isSerialPortOpen = true;
                Intent intent_broad = new Intent();
                intent_broad.setAction("com.maple.OpenSerialPortReceiver");
                intent_broad.putExtra("result", "1");
                sendBroadcast(intent_broad);
            } else {
                //开启串口失败
                Intent intent_broad = new Intent();
                intent_broad.setAction("com.maple.OpenSerialPortReceiver");
                intent_broad.putExtra("result", "0");
                sendBroadcast(intent_broad);
            }
        }
    }

    private void registerBaudrateReceiver() {
        BaudrateReceiver baudrateReceiver = new BaudrateReceiver();
        IntentFilter intentFilter = new IntentFilter("com.maple.BaudrateReciver");
        registerReceiver(baudrateReceiver, intentFilter);
    }


    /*
    开启串口
     */
    private boolean startSerialPort() {
        try {
            serialPort = new SerialPort(path, baudrate, 0, this);
            serialPort.setReceiveListener(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serialPort.initUsb();
    }

    /**
     * 初始化websocket连接
     */
    private void initSocketClient() {
        mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//开启心跳检测
    }

    //对网络的心跳检测
    private static final long HEART_BEAT_RATE = 1000;//每隔1秒进行一次对长连接的心跳检测
    private final Handler mHandler = new Handler();
    private final Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            //检测串口连接情况
            if (serialPort != null && isSerialPortOpen) {
                if (!serialPort.isConnect()) {
                    //串口连接断开
                    serialPort.disconnect();
                    isSerialPortOpen = false;
                } else {
                    //获取垃圾桶的相关信息
                    if (serialPort.isPortOpen) {
                        ArrayList<byte[]> order_array = new ArrayList<>();
                        byte[] order_pre = null;
                        for (int i = 0; i < order.can_number; i++) {
                            order_pre = order.command_gettemp;
                            order_pre[0] = (byte) (i + 1);
                            order_array.add(order_pre);
                            order_pre = order.command_getwater;
                            order_pre[0] = (byte) (i + 1);
                            order_array.add(order_pre);
                            order_pre = order.command_getfire;
                            order_pre[0] = (byte) (i + 1);
                            order_array.add(order_pre);
                            order_pre = order.command_getWeight;
                            order_pre[0] = (byte) (i + 1);
                            order_array.add(order_pre);
                            order_pre = order.command_getCanState;
                            order_pre[0] = (byte) (i + 1);
                            order_array.add(order_pre);
                            order_pre = order.command_getopenState;
                            order_pre[0] = (byte) (i + 1);
                            order_array.add(order_pre);
                            order_pre = order.command_getIdTime;
                            order_pre[0] = (byte) (i + 1);
                            order_array.add(order_pre);
                            order_pre = order.command_getId;
                            order_pre[0] = (byte) (i + 1);
                            order_array.add(order_pre);
                            order_pre = order.command_getway;
                            order_pre[0] = (byte) (i + 1);
                            order_array.add(order_pre);
                        }
                        serialPort.addOrder(order_array);
                    }
                }
            }
            //获取网络情况检测是否有网络连接
            ConnectivityManager cwjManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cwjManager.getActiveNetworkInfo();
            if (info != null && info.isAvailable()) {
                //网络连接
                Log.i("maple_service", "network_connect");
            } else {
                //网络断开
                Log.i("maple_service", "network_disconnect");
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };


    private Notification getNotification() {
        // 定义一个notification
        Notification notification;
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        notification = new Notification.Builder(this)
                .setAutoCancel(true)
                .setContentTitle("maple")
                .setContentText("maple")
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .build();
        return notification;
    }


    @SuppressLint("ObsoleteSdkInt")
    private void setForeground() {
        // sdk < 18 , 直接调用startForeground即可,不会在通知栏创建通知
        if (Build.VERSION.SDK_INT < 18) {
            this.startForeground(PID, getNotification());
            return;
        }
        if (null == mConnection) {
            mConnection = new AssistServiceConnection();
        }
        this.bindService(new Intent(this, AssistService.class), mConnection,
                Service.BIND_AUTO_CREATE);

    }

    //辅助service，避免前台service被杀
    private class AssistServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            // sdk >=18
            // 的，会在通知栏显示service正在运行，这里不要让用户感知，所以这里的实现方式是利用2个同进程的service，利用相同的notificationID，
            // 2个service分别startForeground，然后只在1个service里stopForeground，这样即可去掉通知栏的显示
            Service assistService = ((AssistService.LocalBinder) binder)
                    .getService();
            JWebService.this.startForeground(PID, getNotification());
            assistService.startForeground(PID, getNotification());
            assistService.stopForeground(true);
            JWebService.this.unbindService(mConnection);
            mConnection = null;
        }
    }

    //辅助的保活service
    public static class AssistService extends Service {
        public class LocalBinder extends Binder {
            public AssistService getService() {
                return AssistService.this;
            }
        }

        @Override
        public IBinder onBind(Intent intent) {
            return new LocalBinder();
        }

        @Override
        public void onDestroy() {
            // TODO Auto-generated method stub
            super.onDestroy();
        }
    }

}
