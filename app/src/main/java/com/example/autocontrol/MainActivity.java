package com.example.autocontrol;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;


public class MainActivity extends AppCompatActivity {
    String UserName, MAC;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DBHelper dbHelper = new DBHelper(this);  // подключамся к локальной БД
        //dbHelper.Refresh();  //строка для обнуления базы
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra("finish", false)) {  //вызов из других Activity закрывает приложение
            Log.i("MainActivity", "закрываемся");
            finish();
            System.exit(0);
        }
        setContentView(R.layout.activity_main);
        Cursor cursor=dbHelper.regCheck();
            if(cursor.moveToFirst()){  //считываем учетные данные, если они есть и запускаем главное рабочее Activity
                int userIndex = cursor.getColumnIndex("UserName");
                int MACIndex = cursor.getColumnIndex("MAC");
                UserName = cursor.getString(userIndex);
                MAC = cursor.getString(MACIndex);
                cursor.close();
                Intent MainPage = new Intent(".BaseActivity");
                MainPage.putExtra("MAC",MAC);
                MainPage.putExtra("UserName",UserName);
                startActivity(MainPage);
            }
            else{ //переходим к аутентификации устройства если данных нет
                dbHelper.recordDefaultSettings();  //записываем в настройки приложения значения по-умолчанию
                Intent btpage = new Intent(".BTActivity");
                startActivity(btpage);
            }


    }

}
