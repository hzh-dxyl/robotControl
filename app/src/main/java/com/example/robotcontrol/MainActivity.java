package com.example.robotcontrol;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 2;
    private ShareData shareData;
    private TextView textView;
    private ArrayList<String> list;
    private MyAdapter myAdapter;
    private ListView listView;  //已绑定列表视图
    private ListView listView2;  //可用列表视图
    private ArrayAdapter<String> listAdapter;  //列表视图的适配器
    private ArrayAdapter<String> listAdapter2;  //可用列表视图的适配器
    private BluetoothAdapter mBluetoothAdapter;  //本地蓝牙适配器
    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    //    private final UUID MY_UUID = UUID.fromString("f0a30c96-e903-47b9-965f-74b6c7076580");
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private int connected = 0;
    private Switch aSwitch;

    //广播接收
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED)
                    listAdapter2.add(bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(MainActivity.this, "扫描完成", Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                if (state == BluetoothAdapter.STATE_ON) {
                    Toast.makeText(MainActivity.this, "开始扫描", Toast.LENGTH_SHORT).show();
                    PrintDevice();
                    mBluetoothAdapter.startDiscovery();
                    /*Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(discoverableIntent, 1);
                    try {
                        acceptThread = new AcceptThread();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    acceptThread.start();
                     */
                }
            }
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                if (state == BluetoothDevice.BOND_BONDED) {
                    Toast.makeText(MainActivity.this, "已配对", Toast.LENGTH_SHORT).show();
                    listAdapter.clear();
                    listAdapter.notifyDataSetChanged();
                    listAdapter2.clear();
                    listAdapter2.notifyDataSetChanged();
                    PrintDevice();
                    mBluetoothAdapter.startDiscovery();
                }
            }
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "已连接", Toast.LENGTH_LONG).show();
                connected = 1;
                Intent intent1 = new Intent(MainActivity.this, ControlActivity.class);
                startActivity(intent1);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        shareData = (ShareData) getApplication();
        //找控件
        list=new ArrayList<String>();
        myAdapter=new MyAdapter(this,list);
        listView = (ListView) findViewById(R.id.listView);
        listView2 = (ListView) findViewById(R.id.listView2);
        aSwitch = (Switch) findViewById(R.id.switch3);
        textView = (TextView) findViewById(R.id.deviceName2);
        //实例化本地蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //适配列表视图数据
        //listView.setAdapter(myAdapter);
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(listAdapter);
        listAdapter2 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView2.setAdapter(listAdapter2);
        //添加监听的广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED); //蓝牙开关状态
        registerReceiver(broadcastReceiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(broadcastReceiver, filter);

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "此设备不支持蓝牙", Toast.LENGTH_SHORT).show();
        } else {
            textView.setText(mBluetoothAdapter.getName());
            if (mBluetoothAdapter.isEnabled()) {
                aSwitch.setChecked(true);
                PrintDevice();
                mBluetoothAdapter.startDiscovery();
            }
        }

        checkBluetoothAndLocationPermission();

/*
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION}, 2);*/
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Toast.makeText(getApplicationContext(), "开始配对", Toast.LENGTH_SHORT).show();
                    try {
                        Method method = BluetoothDevice.class.getMethod("createBond");
                        method.invoke(device);
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                //Toast.makeText(getApplicationContext(), "开始连接", Toast.LENGTH_SHORT).show();

                try {
                    connectThread = new ConnectThread(device);
                    connectThread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Toast.makeText(getApplicationContext(), "开始配对", Toast.LENGTH_SHORT).show();
                    try {
                        Method method = BluetoothDevice.class.getMethod("createBond");
                        method.invoke(device);
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                //Toast.makeText(getApplicationContext(), "开始连接", Toast.LENGTH_SHORT).show();

                try {
                    connectThread = new ConnectThread(device);
                    connectThread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mBluetoothAdapter == null) {
                    Toast.makeText(getApplicationContext(), "此设备不支持蓝牙", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isChecked) {
                    mBluetoothAdapter.enable();
                } else {
                    mBluetoothAdapter.cancelDiscovery();
                    mBluetoothAdapter.disable();
                    listAdapter.clear();
                    listAdapter.notifyDataSetChanged();
                    listAdapter2.clear();
                    listAdapter2.notifyDataSetChanged();
                }
            }
        });


    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mServerSocket;

        public AcceptThread() throws IOException {
            mServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("blueteeth", MY_UUID);
        }

        public void run() {
            shareData.socket = null;
            while (true) {
                try {
                    shareData.socket = mServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (shareData.socket != null) {
                    // 自定义方法
                    //manageConnectedSocket(socket);
                    ReadData readData = new ReadData();
                    readData.start();
                    try {
                        mServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }

            }
        }

        public void cancle() throws IOException {
            mServerSocket.close();
        }

    }

    private class ConnectThread extends Thread {
        private BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device) throws IOException {
            mDevice = device;
            // 这里的 UUID 需要和服务器的一致
            shareData.socket = device.createRfcommSocketToServiceRecord(MY_UUID);
        }

        public void run() {
            // 关闭发现设备
            mBluetoothAdapter.cancelDiscovery();
            try {
                if (!shareData.socket.isConnected()) {
                    shareData.socket.connect();
                }
            } catch (IOException connectException) {
                try {
                    shareData.socket.close();
                } catch (IOException closeException) {
                    return;
                }
            }
            // 自定义方法
            //manageConnectedSocket(mmSocket);
            /*ReadData readData = new ReadData();
            readData.start();*/
        }

        public void cancle() {
            try {
                shareData.socket.close();
            } catch (IOException closeException) {

            }
        }

    }

    public void PrintDevice() {
        //打印出已配对的设备
        //listAdapter.add("已配对");
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                listAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
        //list.add("可用");
        //myAdapter.notifyDataSetChanged();
    }

    private void checkBluetoothAndLocationPermission() {
//判断是否有访问位置的权限，没有权限，直接申请位置权限
        if ((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null)
            mBluetoothAdapter.cancelDiscovery();
        unregisterReceiver(broadcastReceiver);
    }

    private class ReadData extends Thread {
        InputStream inputStream;

        private ReadData() {
            if (connected != 1) return;
            try {
                inputStream = shareData.socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            if (connected != 1) return;
            int bytes = 0;
            byte[] buffer = new byte[256];
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final String readS = new String(buffer, 0, bytes);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(readS);
                    }
                });
            }
        }

    }

    public void write(String str) {
        if (connected != 1) return;
        byte[] buffer = str.getBytes();
        OutputStream mmOutStream;
        try {
            mmOutStream = shareData.socket.getOutputStream();
            mmOutStream.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

class MyAdapter extends BaseAdapter {
    private List<String> list;
    private LayoutInflater inflater;

    public MyAdapter(Context context,List<String> data){
        inflater=LayoutInflater.from(context);
        list=data;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public String getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView==null){
            convertView=inflater.inflate(android.R.layout.simple_list_item_1,null);
            if(getItem(position).equals("可用")){
                convertView = inflater.inflate(R.layout.listitem, null);
            }
        }
        if(getItem(position).equals("可用")){
            convertView = inflater.inflate(R.layout.listitem, null);
        }
        return convertView;
    }
}
