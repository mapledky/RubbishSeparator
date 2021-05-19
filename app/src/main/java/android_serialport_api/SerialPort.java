package android_serialport_api;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class SerialPort {
    private static final String TAG = "SerialPortUtils";

    private UsbSerialPort usbport;//usb串口
    private UsbDevice usbdevice;//usb设备
    private UsbDeviceConnection usbDeviceConnection;//串口链接
    private SerialInputOutputManager inputOutputManager;

    private int baudrate = 115200;
    private String devicestate = null;
    private Context context = null;

    public boolean isPortOpen = false;

    private LinkedBlockingQueue<byte[]> orderlist = null;




    public SerialPort(String devicestate, int baudrate, int flags, Context context) throws SecurityException, IOException {
        this.context = context;
        this.baudrate = baudrate;
        this.devicestate = devicestate;
    }

    public boolean initUsb() {
        UsbSerialPort port = null;
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (drivers.size() > 0) {
            for (UsbSerialDriver driver : drivers) {
                List<UsbSerialPort> ports = driver.getPorts();
                for (int i = 0; i < ports.size(); i++) {
                    if (devicestate.equals(ports.get(i).getDevice().getDeviceName())) {
                        port = ports.get(i);
                        break;
                    }
                }
            }
        }
        assert port != null;
        usbport = port;
        usbdevice = port.getDriver().getDevice();
        orderlist = new LinkedBlockingQueue<byte[]>(100);
        return RequestNormalPermission(usbManager, port.getDriver().getDevice());
    }

    /**
     * 请求权限：一般来说有弹框
     */
    public boolean RequestNormalPermission(UsbManager usbManager, UsbDevice device) {
        if (!usbManager.hasPermission(device)) {
            PendingIntent PrtPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent("com.usb.permission"), 0);
            usbManager.requestPermission(device, PrtPermissionIntent);// will recall mReceiver
            return false;
        } else {
            return getUsbInfo(usbManager, device);
        }
    }

    /**
     * 获得授权USB的基本信息
     * 1、USB接口，一般是第一个
     * 2、USB设备的输入输出端
     */
    private boolean getUsbInfo(UsbManager usbManager, UsbDevice usbDevice) {
        StringBuilder sb = new StringBuilder();
        if (Build.VERSION.SDK_INT >= 23) {
            sb.append(String.format("VID:%04X  PID:%04X  ManuFN:%s  PN:%s V:%s",
                    usbDevice.getVendorId(),
                    usbDevice.getProductId(),
                    usbDevice.getManufacturerName(),
                    usbDevice.getProductName(),
                    usbDevice.getVersion()
            ));
        } else if (Build.VERSION.SDK_INT >= 21) {
            sb.append(String.format("VID:%04X  PID:%04X  ManuFN:%s  PN:%s",
                    usbDevice.getVendorId(),
                    usbDevice.getProductId(),
                    usbDevice.getManufacturerName(),
                    usbDevice.getProductName()
            ));
        } else {
            sb.append(String.format("VID:%04X  PID:%04X",
                    usbDevice.getVendorId(),
                    usbDevice.getProductId()
            ));
        }
        return connect(usbManager, usbDevice);//连接
    }

    public synchronized void addOrder(ArrayList<byte[]> order_array) {
        for (byte[] order : order_array) {
            try {
                orderlist.put(order);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isConnect() {
        //判断串口是否连接
        if (usbport != null) {
            return usbport.isOpen();
        }
        return false;
    }

    /**
     * usb设备的连接
     */

    private boolean connect(UsbManager usbManager, UsbDevice usbDevice) {
        usbDeviceConnection = usbManager.openDevice(usbDevice);
        if (usbDeviceConnection == null) {
            Log.i(TAG, "usb connect fail");
            return false;
        } else {
            Log.i(TAG, "usb connect success");
            setConnectionParam();
            return true;
        }
    }

    /**
     * 设置通讯参数
     */
    private void setConnectionParam() {
        try {
            if (usbdevice.getInterfaceCount() > 0) {
                usbport.open(usbDeviceConnection);
                usbport.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                usbport.setDTR(true);
                usbport.setRTS(true);
                startIoManager();

                isPortOpen = true;
                //打开消息发送线程
                sendDataThread();
            }
            //无通讯接口
            else {
                Log.i(TAG, "无通讯接口");
            }
        } catch (IOException e) {
            try {
                usbport.close();
            } catch (IOException e2) {
                // Ignore.
            }
            usbport = null;
        }
    }

    //发送信息的线程
    private void sendDataThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isPortOpen){
                    byte[] order = null;
                    try {
                        order = orderlist.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    sendData(order);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    //发送data
    public void sendData(byte[] data) {
        if (usbport != null) {
            try {
                usbport.write(data, 1000);
            } catch (IOException ignored) {

            }
        }
    }

    //消息返回接口
    public interface ReceiveListener {
        void receiveMessage(byte[] message);
    }

    private ReceiveListener listener = null;

    public void setReceiveListener(ReceiveListener listener) {
        this.listener = listener;
    }

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {

                }

                @Override
                public void onNewData(final byte[] data) {
                    listener.receiveMessage(data);
                }
            };

    /**
     * usb设备断开连接
     */
    public void disconnect() {
        Log.i(TAG, "usb disconnect...");
        stopIoManager();
        if (usbport != null) {
            try {
                usbport.close();
                isPortOpen = false;
            } catch (IOException e) {
                // Ignore.
            }
            usbport = null;
        }
    }

    private void stopIoManager() {
        if (inputOutputManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            inputOutputManager.stop();
            inputOutputManager = null;
        }
    }

    private void startIoManager() {
        if (inputOutputManager == null) {
            Log.i(TAG, "Starting io manager ..");
            inputOutputManager = new SerialInputOutputManager(usbport, mListener);
            ExecutorService mExecutor = Executors.newSingleThreadExecutor();
            mExecutor.submit(inputOutputManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }
}