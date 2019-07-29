package com.example.autocontrol;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BTService extends Service {  //сервис для мониторинга ВТ устройств и автоматического подключения к ним
    BroadcastReceiver updateUIReciver;
    String MAC;
    ConnectTask connect;
    ThreadConnected ConnectedThread;
    Timer timer = new Timer(true);
    Timer echo = new Timer(true);
    Timer setecho = new Timer (true);
    Timer info = new Timer (true);
    private boolean status = false;
    Context con;


    public BTService() {
        con = this;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction("BTDisco");
        filter.addAction("BTConn");
        filter.addAction("UARTMSG");
        filter.addAction("Echo"); // ехо - ответ от платы для быстрого определения недоступности платы
        filter.addAction("CloseSocket");  //команда на закрытие соединениия
        filter.addAction("StopServices");
        updateUIReciver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals("BTConn")) {
                    echo = new Timer(true);
                    setecho = new Timer(true);
                    info = new Timer(true);
                    setecho.schedule(new SetEchoTask(con),1000,800); // начинаем слать эхо
                    echo.schedule(new EchoTask(con),1600,1200); // ждем ответов эхо не более 1200мс.
                    info.schedule(new getInfo(con),60000,60000); //каждую минуту хотим получать пакет данных с платы для контроля за напряжение батареи
                    Log.i("BTService", "connect to "+MAC);
                }
                else if (intent.getAction().equals("BTDisco")){  // останавливаем все контролирующие таймеры
                    echo.cancel();
                    echo.purge();
                    setecho.cancel();
                    setecho.purge();
                    info.cancel();
                    info.purge();
                    status = false;
                    Log.i("BTService","disconnect from "+ MAC);
                }
                else if (intent.getAction().equals("UARTMSG")){  // получаем стоку на отправку и отправляем
                    Bundle arg = intent.getExtras();
                    String msg = arg.get("String").toString();
                    ConnectedThread.write(msg.getBytes());
                }
                else if (intent.getAction().equals("StopServices")){
                    Log.i("BTService", "команда на стоп");
                    onDestroy();
                }
                else if (intent.getAction().equals("CloseSocket")) {
                    connect.closeSoc();
                }
                else if (intent.getAction().equals("Echo")){ // если получае ответ ехо, обновляем таймаут
                    echo.cancel();
                    echo.purge();
                    echo = new Timer(true);
                    echo.schedule(new EchoTask(con),1200,1200);
                }
            }
        };
        registerReceiver(updateUIReciver, filter);
    }
    @Override
    public void onDestroy() {
        echo.cancel();
        echo.purge();
        setecho.cancel();
        setecho.purge();
        info.cancel();
        info.purge();
        super.onDestroy();
        unregisterReceiver(updateUIReciver);
        connect.closeSoc();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle arguments = intent.getExtras();
        MAC = arguments.get("MAC").toString();
        connect = new ConnectTask(MAC,this,timer,BTService.this);
        ConnectedThread = ((ConnectTask) connect).getThread();
        timer.schedule(connect,0,1000);
        return super.onStartCommand(intent, flags, startId);
    }
    public boolean getBTStatus(){
        return status;
    }
    public void setStatus(boolean stat){
        status = stat;
    }

}


class  ConnectTask extends TimerTask {   //класс для попыток подключения к блютуз устройству если не подключено
    BluetoothSocket bluetoothSocket = null;
    public  ThreadConnected myThreadConnected;
    public  int status;
    BTService serv;
    Timer timer;
    Context context;
    BluetoothDevice device;


    public ConnectTask (String MAC, BTService service, Timer tmr, Context con){
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        device = bluetooth.getRemoteDevice(MAC);
        context = con;
        serv=service;
        timer=tmr;
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            myThreadConnected = new ThreadConnected(bluetoothSocket, con);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public ThreadConnected getThread(){
        while (myThreadConnected==null);
        return myThreadConnected;
    }
 public void closeSoc(){
     try {
         bluetoothSocket.close();
     }
     catch (IOException e1) {
         e1.printStackTrace();
     }
 }
    @Override
    public void run(){ // пытаемся подключиться
        timer.purge();
        if (!serv.getBTStatus()) {
              Log.i("BTService", "Trying to connect");
            try {
                bluetoothSocket.connect();
                myThreadConnected.start();
                Log.i("BTService", "Connected");
                serv.setStatus(true);
                Intent local = new Intent();
                local.setAction("BTConn");
                context.sendBroadcast(local);
            } catch (IOException e) {
                Log.i("BTService", "Error");
                serv.setStatus(false);
                e.printStackTrace();
            }
        }
    }
}
class EchoTask extends TimerTask{
    Context context;

    public EchoTask(Context con){
        context = con;
    }
    @Override
    public void run(){ // если таймаут прошел, даем команду на отключение сокета для освобождения канала
        Intent intent = new Intent();
        intent.setAction("BTDisco");
        context.sendBroadcast(intent);
        Log.i("BTService", "команда на отсоединение из EchoTask");
    }
}
class SetEchoTask extends TimerTask {
    Context context;

    public SetEchoTask(Context con) {
        context = con;
    }

    @Override
    public void run() {
        Intent local = new Intent();
        local.putExtra("String", "Q^"); // отправляем эхо команду
        local.setAction("UARTMSG");
        context.sendBroadcast(local);
    }
}
class getInfo extends  TimerTask{
    Context context;
    public getInfo(Context con) {
        context = con;
    }
    @Override
    public void run() {
        Intent local = new Intent();
        local.putExtra("String", "R^");
        local.setAction("UARTMSG");
        context.sendBroadcast(local);
    }

}
