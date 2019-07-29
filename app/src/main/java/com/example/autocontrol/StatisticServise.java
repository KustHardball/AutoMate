package com.example.autocontrol;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.FontsContract;
import android.util.Log;

public class StatisticServise extends Service {  //серви для отлова пакетов статистики с платы
    DBHelper dbHelper;
    BroadcastReceiver updateUIReciver;
    String MAC, UserName;
    int UserID,ParamID;
    public StatisticServise() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    public void onCreate() {
        dbHelper = new DBHelper(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction("GetSettingsPackage");  //получение статистики с выстрела
        filter.addAction("StopServices");
        updateUIReciver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("GetSettingsPackage")) {
                    dbHelper.addStat(intent,ParamID); // записываем в базу
                }
                else if (intent.getAction().equals("StopServices")){
                    onDestroy();
                }
            }
        };
        registerReceiver(updateUIReciver, filter);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(updateUIReciver);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle arguments = intent.getExtras();
        MAC = arguments.get("MAC").toString();
        UserName = arguments.get("UserName").toString();
        Cursor cursor=dbHelper.getLocalID(arguments.get("MAC").toString(),arguments.get("UserName").toString());
        if(cursor.moveToLast()) {  //считываем локальный ID пользователя
            int userIndex = cursor.getColumnIndex("UserID");
            UserID = cursor.getInt(userIndex);
            userIndex = cursor.getColumnIndex("ParamID");
            ParamID = cursor.getInt(userIndex);
        }
        cursor.close();
        return super.onStartCommand(intent, flags, startId);
    }
}
