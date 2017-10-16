package me.zhouzhuo810.okusbdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.nio.charset.Charset;

import me.zhouzhuo810.okusb.USB;

public class MainActivity extends AppCompatActivity {

    private USB usb;
    private EditText etData;
    private Button btnSend;
    private TextView tvResult;
    private Button btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etData = (EditText) findViewById(R.id.et_data);
        btnSend = (Button) findViewById(R.id.btn_send);
        btnClear = (Button) findViewById(R.id.btn_clear);
        tvResult = (TextView) findViewById(R.id.tv_result);

        usb = new USB.USBBuilder(this)
                .setBaudRate(115200)
                .setDataBits(8)
                .setStopBits(UsbSerialPort.STOPBITS_1)
                .setParity(UsbSerialPort.PARITY_NONE)
                .setMaxReadBytes(20)
                .setReadDuration(500)
                .setDTR(false)
                .setRTS(false)
                .build();


        usb.setOnUsbChangeListener(new USB.OnUsbChangeListener() {
            @Override
            public void onUsbConnect() {
                Toast.makeText(MainActivity.this, "conencted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUsbDisconnect() {
                Toast.makeText(MainActivity.this, "disconencted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDataReceive(byte[] data) {
//                final String rData = DataUtil.byteArrayToString(data);
//                tvResult.append(rData);
                tvResult.setText(new String(data, Charset.forName("GB2312")));
            }

            @Override
            public void onUsbConnectFailed() {
                Toast.makeText(MainActivity.this, "connect fail", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionGranted() {
                Toast.makeText(MainActivity.this, "permission ok", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionRefused() {
                Toast.makeText(MainActivity.this, "permission fail", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDriverNotSupport() {
                Toast.makeText(MainActivity.this, "no driver", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onWriteDataFailed(String error) {
                Toast.makeText(MainActivity.this, "write fail" + error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onWriteSuccess(int num) {
                Toast.makeText(MainActivity.this, "write ok " + num, Toast.LENGTH_SHORT).show();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = etData.getText().toString();
                usb.writeData(str.getBytes(Charset.forName("GB2312")), 500);
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvResult.setText("");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (usb != null) {
            usb.destroy();
        }

    }
}
