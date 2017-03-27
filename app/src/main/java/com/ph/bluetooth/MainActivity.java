package com.ph.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.ph.bluetooth.adapter.BlueAdapter;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    public static Context context;
    private BluetoothAdapter defaultAdapter;
    private ListView lv;

    private List<BluetoothDevice> list = new ArrayList<BluetoothDevice>();
    private BlueAdapter adapter;
    private MyReceiver receiver;
    private OutputStream ous;
    private InputStream ins;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        regist();

        bindAdapter();
    }

    private void bindAdapter() {
        adapter = new BlueAdapter(list);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(itemListener);
    }

    private void regist() {
        receiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);
    }

    private void init() {
        context = getApplicationContext();
        lv = (ListView) findViewById(R.id.lv);
        defaultAdapter = BluetoothAdapter.getDefaultAdapter();
    }


    public void openBlue(View v) {
        if (!defaultAdapter.enable()) {
            defaultAdapter.enable();
        }
    }

    public void closeBlue(View v) {
        list.clear();
        adapter.notifyDataSetChanged();
        if (defaultAdapter.isEnabled()) {
            defaultAdapter.disable();
        }
    }

    public void startScane(View v) {
        list.clear();
        adapter.notifyDataSetChanged();
        defaultAdapter.startDiscovery();
    }

    public void cancelScane(View v) {
        defaultAdapter.cancelDiscovery();
    }


    AdapterView.OnItemClickListener itemListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device = list.get(position);
            connectionDevice(device);
        }
    };

    private void connectionDevice(final BluetoothDevice mdevice) {
        new Thread() {
            @Override
            public void run() {
                try {
//                    BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID
//                            .fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    Method m = mdevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                    BluetoothSocket socket = (BluetoothSocket) m.invoke(mdevice, 1);
                    socket.connect();
                    ous = socket.getOutputStream();
                    ins = socket.getInputStream();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "成功连接设备", Toast.LENGTH_SHORT).show();
                        }
                    });
                    System.out.println("连接成功");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //扫描到设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                list.add(device);
                adapter.notifyDataSetChanged();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Toast.makeText(MainActivity.this, "开始扫描", Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(MainActivity.this, "扫描结束", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}


