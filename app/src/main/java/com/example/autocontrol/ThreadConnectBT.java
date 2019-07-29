package com.example.autocontrol;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.io.IOException;


public class ThreadConnectBT extends Thread { // Поток для коннекта с Bluetooth
    private BluetoothSocket bluetoothSocket = null;
    public ThreadConnected myThreadConnected;
    Context con;


    public ThreadConnectBT(BluetoothDevice device, UUID myUUID, boolean proof, Context cont) {
        con = cont;
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() { // Коннект при первом подключении
        boolean success = false;
        try {
            bluetoothSocket.connect();
            success = true;
        }
        catch (IOException e) {
            e.printStackTrace();
            con.sendBroadcast(new Intent("CantConnect"));
            try {
                bluetoothSocket.close();
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        if(success) {  // Если законнектились
            myThreadConnected = new ThreadConnected(bluetoothSocket,con);
            myThreadConnected.start(); // запуск потока приёма и отправки данных
            try {  // ждем ответ от устройства
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();

            }
            myThreadConnected.write("^".getBytes());
            try {  // ждем ответ от устройства
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            myThreadConnected.sbprint="";
            myThreadConnected.write("K^".getBytes());
            try {  // ждем ответ от устройства
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!myThreadConnected.sbprint.equals("K^AutoNut")){ // если плата ответила должным образом
                con.sendBroadcast(new Intent("CheckDevice"));
                cancel();
                myThreadConnected.sbprint="";
            }
            else{
                con.sendBroadcast(new Intent("SuccesConnected"));
                con.sendBroadcast(new Intent("GoToAlert"));
                cancel();
            }
        }

    }
    public void cancel() { // принудительно закрываем соединение
        try {
            bluetoothSocket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

}
class ThreadConnected extends Thread {    // Поток - приём и отправка данных
    private final InputStream connectedInputStream;
    private final OutputStream connectedOutputStream;
    public String sbprint = "0";
    public boolean sus = false;
    UARTParser msj;
    Context con;
    public ThreadConnected(BluetoothSocket socket, Context context) {
        msj = new UARTParser(context);
        con=context;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
            sus=true;
        }
        catch (IOException e) {
            e.printStackTrace();
            sus = false;
        }
        connectedInputStream = in;
        connectedOutputStream = out;
    }
    @Override
    public void run() { // Приём данных
        while (true) {
            try {
                StringBuilder sb = new StringBuilder();
                byte[] buffer = new byte[512];
                int bytes = connectedInputStream.read(buffer);
                String strIncom = new String(buffer, 0, bytes);
                sb.append(strIncom); // собираем символы в строку
                int endOfLineIndex = sb.indexOf("\n"); // определяем конец строки
                if (endOfLineIndex > 0) {
                    sbprint = sb.substring(0, endOfLineIndex);
                    sb.delete(0, sb.length());
                    Intent local = new Intent();
                    local.setAction("Echo");
                    con.sendBroadcast(local);
                    msj.startParse(sbprint);
                }
            } catch (IOException e) {
                break;
            }
        }
    }

    public void write(byte[] buffer) {  // отправка данных
        try {
            connectedOutputStream.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            connectedOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
