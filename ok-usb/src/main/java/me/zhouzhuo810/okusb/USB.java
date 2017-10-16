package me.zhouzhuo810.okusb;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * 串口通讯
 *
 * Created by zz on 2017/10/13.
 */
public class USB {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private WeakReference<Activity> ctx;
    private UsbManager usbManager;
    private OnUsbChangeListener onUsbChangeListener;
    private UsbSerialPort port;

    private int BAUD_RATE = 115200;
    private int DATA_BITS = 8;
    private int STOP_BITS = UsbSerialPort.STOPBITS_1;
    private int PARITY = UsbSerialPort.PARITY_NONE;
    private int MAX_READ_BYTES = 100;

    private boolean DTR = false;
    private boolean RTS = false;

    //500毫秒读一次
    private int READ_DURATION = 1000;

    //读数据线程
    private Runnable readThread;


    private USB(Activity ctx, USBBuilder builder) {
        BAUD_RATE = builder.BAUD_RATE;
        DATA_BITS = builder.DATA_BITS;
        STOP_BITS = builder.STOP_BITS;
        PARITY = builder.PARITY;
        MAX_READ_BYTES = builder.MAX_READ_BYTES;
        READ_DURATION = builder.READ_DURATION;
        DTR = builder.DTR;
        RTS = builder.RTS;

        this.ctx = new WeakReference<Activity>(ctx);
        usbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
        initReadThread(this.ctx);
    }

    private void initReadThread(final WeakReference<Activity> ctx) {
        readThread = new Runnable() {
            @Override
            public void run() {
                while (port != null && !Thread.interrupted()) {
                    final byte[] data = new byte[MAX_READ_BYTES];
                    try {
                        int num = port.read(data, 3000);
                        if (num > 0) {
                            Activity activity = ctx.get();
                            if (activity != null) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (onUsbChangeListener != null) {
                                            onUsbChangeListener.onDataReceive(data);
                                        }
                                    }
                                });
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //0.5秒读一次。
                    try {
                        Thread.sleep(READ_DURATION);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }


    public static class USBBuilder {
        Activity act;

        private int BAUD_RATE = 115200;
        private int DATA_BITS = 8;
        private int STOP_BITS = UsbSerialPort.STOPBITS_1;
        private int PARITY = UsbSerialPort.PARITY_NONE;
        private int MAX_READ_BYTES = 100;

        private boolean DTR = false;
        private boolean RTS = false;

        private int READ_DURATION = 1000;

        public USBBuilder(Activity act) {
            this.act = act;
        }

        public USBBuilder setBaudRate(int baudRate) {
            this.BAUD_RATE = baudRate;
            return this;
        }

        public USBBuilder setDataBits(int dataBits) {
            this.DATA_BITS = dataBits;
            return this;
        }

        public USBBuilder setStopBits(int stopBits) {
            this.STOP_BITS = stopBits;
            return this;
        }

        public USBBuilder setParity(int parity) {
            this.PARITY = parity;
            return this;
        }

        public USBBuilder setMaxReadBytes(int maxReadBytes) {
            this.MAX_READ_BYTES = maxReadBytes;
            return this;
        }

        public USBBuilder setReadDuration(int readDuration) {
            this.READ_DURATION = readDuration;
            return this;
        }

        public USBBuilder setDTR(boolean dtr) {
            this.DTR = dtr;
            return this;
        }

        public USBBuilder setRTS(boolean rts) {
            this.RTS = rts;
            return this;
        }

        public USB build() {
            return new USB(act, this);
        }

    }

    public void setOnUsbChangeListener(OnUsbChangeListener onUsbChangeListener) {
        this.onUsbChangeListener = onUsbChangeListener;
        register();
    }

    public interface OnUsbChangeListener {

        void onUsbConnect();

        void onUsbDisconnect();

        void onDataReceive(byte[] data);

        void onUsbConnectFailed();

        void onPermissionGranted();

        void onPermissionRefused();

        void onDriverNotSupport();

        void onWriteDataFailed(String error);

        void onWriteSuccess(int num);

    }


    /**
     * 发送数据
     *
     * @param data         数据
     * @param timeoutMills 超时时间(ms)
     */
    public void writeData(byte[] data, int timeoutMills) {
        try {
            if (port != null) {
                Log.e("TTT", "write start");
                int num = port.write(data, timeoutMills);
                Log.e("TTT", "write end");
                if (onUsbChangeListener != null) {
                    onUsbChangeListener.onWriteSuccess(num);
                }
            } else {
                if (onUsbChangeListener != null) {
                    onUsbChangeListener.onWriteDataFailed(" port == null");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (onUsbChangeListener != null) {
                onUsbChangeListener.onWriteDataFailed(e.toString());
            }
        }

    }

    private void register() {
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        usbFilter.addAction(ACTION_USB_PERMISSION);
        Activity activity = ctx.get();
        if (activity != null) {
            activity.registerReceiver(mUsbPermissionActionReceiver, usbFilter);
        }
    }

    /**
     * 取消注册
     */
    public void destroy() {
        Activity activity = ctx.get();
        if (activity != null) {
            activity.unregisterReceiver(mUsbPermissionActionReceiver);
        }
        disConnectDevice();
        onUsbChangeListener = null;
    }

    /**
     * 断开连接
     */
    public void disConnectDevice() {
        if (port != null) {
            try {
                port.close();
                port = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (onUsbChangeListener != null) {
            onUsbChangeListener.onUsbDisconnect();
        }
    }

    private void requestPermission() {
        Activity activity = ctx.get();
        if (activity != null) {
            PendingIntent mPermissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
            //here do emulation to ask all connected usb device for permission

            for (final UsbDevice usbDevice : usbManager.getDeviceList().values()) {
                if (usbManager.hasPermission(usbDevice)) {
                    afterGetUsbPermission(usbDevice);
                } else {
                    usbManager.requestPermission(usbDevice, mPermissionIntent);
                }
            }
        }
    }

    private void afterGetUsbPermission(UsbDevice usbDevice) {
        // Find all available drivers from attached devices.
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            if (onUsbChangeListener != null) {
                onUsbChangeListener.onDriverNotSupport();
            }
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        try {
            UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
            if (connection == null) {
                if (usbDevice == null) {
                    requestPermission();
                }
                return;
            }
            // Read some data! Most have just one port (port 0).
            port = driver.getPorts().get(0);
            try {
                port.open(connection);
                port.setParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY);
                port.setDTR(DTR);
                port.setRTS(RTS);
                if (onUsbChangeListener != null) {
                    onUsbChangeListener.onUsbConnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (onUsbChangeListener != null) {
                    onUsbChangeListener.onUsbConnectFailed();
                }
            }
            Log.e("TTT", "thread start");
            new Thread(readThread).start();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }


    private final BroadcastReceiver mUsbPermissionActionReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                afterGetUsbPermission(null);
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                disConnectDevice();
            } else if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        //user choose YES for your previously popup window asking for grant perssion for this usb device
                        if (onUsbChangeListener != null) {
                            onUsbChangeListener.onPermissionGranted();
                        }
                        if (null != usbDevice) {
                            afterGetUsbPermission(usbDevice);
                        }
                    } else {
                        //user choose NO for your previously popup window asking for grant perssion for this usb device
                        if (onUsbChangeListener != null) {
                            onUsbChangeListener.onPermissionRefused();
                        }
                    }
                }
            }
        }
    };


}
