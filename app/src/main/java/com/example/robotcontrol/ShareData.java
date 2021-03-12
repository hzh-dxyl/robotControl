package com.example.robotcontrol;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;

@SuppressLint("Registered")
public class ShareData extends Application {
    public BluetoothSocket socket;

    @Override
    public void onCreate(){
        super.onCreate();
    }

    public void write(char c) {
        if (socket==null) return;
        OutputStream mmOutStream;
        try {
            mmOutStream = socket.getOutputStream();
            mmOutStream.write(c);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(int num){
        if(socket==null) return;
        OutputStream stream;
        try {
            stream=socket.getOutputStream();
            stream.write(num);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
