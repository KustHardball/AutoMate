package com.example.autocontrol;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class DBHelper extends SQLiteOpenHelper {  //класс работы с БД, выполнения запросо
    public static final String DATABASE_NAME = "AutoNutDB";
    public static final int DATABASE_VERSION = 1;
    private SQLiteDatabase db;
    Context cont;

    public DBHelper (Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION );
        db = this.getWritableDatabase();
        cont = context;
    }
    @Override
    public  void onCreate (SQLiteDatabase db){  //создаем таблицы при первом запуске приложения
        db.execSQL("CREATE TABLE Users (MAC varchar(20) NOT NULL,GlobalUserID INTEGER UNIQUE, UserName varchar(30) NOT NULL,Sync INTEGER DEFAULT 0, UserID INTEGER PRIMARY KEY AUTOINCREMENT , CONSTRAINT un_UserID UNIQUE (MAC,UserName) )");
        db.execSQL("CREATE TABLE Param (Length INTEGER NOT NULL, Device varchar(30) NOT NULL, LowPressure INTEGER,Pswrd varchar(6) DEFAULT '',Capasitor INTEGER,BatteryType varchar(10), BatteryVoltage INTEGER, HighPressure INTEGER, Regulator varchar(40), UserID INTEGER,Sync INTEGER DEFAULT 0,GlobalParamID INTEGER, ParamID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, CONSTRAINT un_ParamID UNIQUE (Device,Length,BatteryType,Capasitor,LowPressure,HighPressure,Regulator),FOREIGN KEY (UserID)  REFERENCES Users (UserID))");
        db.execSQL("CREATE TABLE Sys (LastSync varchar(30),Sync INTEGER DEFAULT 0,SyncDelay INTEGER DEFAULT 10,CurrentParam INTEGER, Protection INTEGER, ProtectionStatus INTEGER, ID INTEGER  PRIMARY KEY AUTOINCREMENT NOT NULL)");
        db.execSQL("CREATE TABLE Stat (impulse INTEGER, Speed INTEGER, Pressure REAL,Voltage REAL,ShootDate BIGINT, ParamID INTEGER, RecordID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,FOREIGN KEY (ParamID)  REFERENCES Param (ParamID))");
        db.execSQL("CREATE TABLE Settings (Impulse INTEGER NOT NULL, Protection INTEGER, ProtectionStatus INTEGER, LowImpulse INTEGER NOT NULL,Rate INTEGER not null,Length INTEGER not null,Sync INTEGER default 0,Splength INTEGER not null,Settings INTEGER,SettingsName varchar(30) NOT NULL,UserID INTEGER,SettingsID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,FOREIGN KEY (UserID)  REFERENCES Users (UserID))");

    }
    @Override
    public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion){ //удаляем таблицы при обновлении СУБД
        db.execSQL("drop table if exists Users");
        db.execSQL("drop table if exists Param");
        db.execSQL("drop table if exists Sys");
        db.execSQL("drop table if exists Stat");
        db.execSQL("drop table if exists Settings");
        onCreate(db);
    }
    public void Refresh(){  //метод для быстрого обнуления базы
        db.execSQL("drop table if exists Users");
        db.execSQL("drop table if exists Param");
        db.execSQL("drop table if exists Sys");
        db.execSQL("drop table if exists Stat");
        db.execSQL("drop table if exists Settings");
        onCreate(db);

    }
    public boolean checkUser(){ //проверяем есть ли несинхронизированные юзеры
        Cursor cursor= db.rawQuery("SELECT * FROM Users WHERE Sync = 0",new String[] {} );
        if(cursor.moveToFirst()) {
            return true;   //если нашли пользователей
        }
        return false;
    }
    public boolean checkParam(){  //проверяем, есть ли не синхронизированные параметры
        Cursor cursor= db.rawQuery("SELECT * FROM Param WHERE Sync = 0",new String[] {} );
        if(cursor.getCount()>0) { //параметров нет на сервере
            return true;  //если есть не синхронизированные параметры, проверяем связаные параметры
        }
        return false;
    }
    public boolean setSyncUserFlag (int UserID,String MAC,String UserName){ //отмечаем синхронизацию юзера
        db.execSQL("UPDATE Users SET Sync = 1,GlobalUserID = ? WHERE MAC = ? AND UserName = ?",new String[] { Integer.toString(UserID) ,MAC, UserName } );
        return true;
    }
    public boolean setSyncParamFlag (int id){  //отмечаем синхронизацию параметров
        db.execSQL("UPDATE Param SET Sync = 1 , GlobalParamID=? WHERE Sync = 0",new String[] {Integer.toString(id)} );
        Log.i("DBHelper", "проставили флаги параметрам");
        return true;
    }
    public Cursor regCheck(){
        Cursor cursor = db.query("Users",null,null,null,null,null,null);
            return cursor;
    }
    public void saveParamToDB(String gun,int wpres, int extpres, String reg, int length,String MAC, String UserName,String batType, int banks, int capasitor){ //сохранаем параметры, введенные ползьвателем
        ContentValues contentValues = new ContentValues();
        contentValues.put("Length", length);
        contentValues.put("Device", gun);
        contentValues.put("LowPressure", extpres);
        contentValues.put("HighPressure", wpres);
        contentValues.put("Regulator", reg);
        contentValues.put("BatteryType", batType);
        contentValues.put("BatteryVoltage", banks);
        contentValues.put("Capasitor", capasitor);
        Cursor cursor = db.rawQuery("SELECT UserID FROM Users WHERE MAC = ? AND UserName = ?",new String[] { MAC, UserName } );
        if(cursor.moveToFirst()) { //юзера нет на сервере
            int index = cursor.getColumnIndex("UserID");
            int UserID = cursor.getInt(index);
            contentValues.put("UserID", UserID);
        }
        db.insert("Param", null,contentValues);
        cursor= db.rawQuery("SELECT ParamID FROM Param ",null);  //вполлучаем последний ID и записываем его как нынешний активный
        if(cursor.moveToLast()) {
            int Index = cursor.getColumnIndex("ParamID");
            int val = cursor.getInt(Index);
            db.execSQL("UPDATE Sys SET CurrentParam = ? WHERE ID = 1",new String[] {Integer.toString(val)});
        }
    }
    public Cursor getNoSyncParam(String MAC, String UserName) {  //получаем список не синхронизированных параметров
        Cursor cursor= db.rawQuery("SELECT Param.Length, Param.Device, Param.LowPressure, Param.HighPressure,Param.Regulator, Param.Capasitor, Param.BatteryType,Param.BatteryVoltage  FROM Param INNER JOIN Users ON Param.UserID = Users.UserID WHERE Param.Sync = 0 and Users.UserName = ? and Users.MAC = ?",new String[] {UserName, MAC} );
        return cursor;
    }
    public Cursor getLocalID(String MAC, String UserName){
        Cursor cursor=db.rawQuery("SELECT Users.UserID,ParamID FROM Users JOIN Param ON Users.UserID=Param.UserID WHERE MAC = ? AND UserName = ?",new String[] { MAC, UserName } );
        return  cursor;
    }
    public void addStat(Intent intent,int ParamID){  //сохраняем пакет статистики за 1 выстерел
        ContentValues contentValues = new ContentValues();
        contentValues.put("Impulse", intent.getIntExtra("Impulse",0));
        contentValues.put("Speed", intent.getIntExtra("Speed",0));
        contentValues.put("Pressure", intent.getDoubleExtra("Pressure",0));
        contentValues.put("Voltage", intent.getDoubleExtra("Voltage",0));
        contentValues.put("ShootDate", intent.getLongExtra("Time",0));
        contentValues.put("ParamID", ParamID);
        db.insert("Stat", null,contentValues);
        Intent local = new Intent();
        local.setAction("StatSync");
        cont.sendBroadcast(local);
    }
    public boolean checkStat(){  //проверяем есть ли не отправленния на сервер статистмка(отправленная удаляется)
        Cursor cursor= db.rawQuery("SELECT * FROM Stat",new String[]{});
        if(cursor.moveToFirst()) {
            return true;
        }
        return false;
    }
    public int getParamID(){  //получаем ID активных парметров
        Cursor cursor= db.rawQuery("SELECT CurrentParam FROM Sys WHERE ID =1 ",null);
        if(cursor.moveToLast()) {
            int Index = cursor.getColumnIndex("CurrentParam");
            int val = cursor.getInt(Index);
            Log.i("DBHelper", "локальный ид "+Integer.toString(val));
            return val;
        }
        return 0;
    }
    public Cursor getStats(int id){  //получаем всю несинхронизированную статистику для отправки на сервер
        Cursor cursor = db.rawQuery("SELECT * FROM Stat WHERE ParamID =?",new String[]{Integer.toString(id)});
                return cursor;
    }
    public int getGlobalParamID(){  //получаем серверный ID активного параметра для отправки статистики на сервер по нему
        int val=getParamID();
            Cursor cursor= db.rawQuery("SELECT GlobalParamID FROM Param WHERE ParamID=? ",new String[]{Integer.toString(val)});
            if(cursor.moveToFirst()) {
                int Index = cursor.getColumnIndex("GlobalParamID");
                val = cursor.getInt(Index);
                return val;
            }
        return 0;
    }
    public void deleteStats(int id, int lastid){  //удаляем синхронизированные записи статистики
        db.execSQL("DELETE FROM Stat WHERE RecordID <= ? AND ParamID = ?",new String[] { Integer.toString(lastid) ,Integer.toString(id) } );
        Log.i("DBHelper", Integer.toString(id)+" и "+Integer.toString(lastid));
    }
    public String getGunName(){  // получаем название ствола для отображения на глваном Activity
        String val;
        Cursor cursor= db.rawQuery("SELECT Device FROM Param WHERE ParamID=? ",new String[]{Integer.toString(getParamID())});
        if(cursor.moveToLast()) {
            int Index = cursor.getColumnIndex("Device");
            val = cursor.getString(Index);
            Log.i("DBHelper", "дрын "+(val));
            return val;
        }
        return "не понятно";
    }
    public void recordLastSync(){  //записывем время последней синхронизации
        DateFormat df = new SimpleDateFormat("dd.MM.yy, HH:mm");
        db.execSQL("UPDATE Sys SET LastSync = ? WHERE ID = ?",new String[] {df.format(Calendar.getInstance().getTime()),Integer.toString(1)});
        Log.i("DBHelper", "записали дату");
        setSyncDelay(5);
    }
    public String getLastSync(){  //поллучаем время последней синхронизации
        Cursor cursor=db.rawQuery("SELECT LastSync FROM Sys WHERE ID = 1",new String[] {} );
        String val;
        if(cursor.moveToFirst()) {
            int Index = cursor.getColumnIndex("LastSync");
            val = cursor.getString(Index);
            Log.i("DBHelper", "прочли дату");
        }
        else val = "пусто";
        if(val == null) val = "пусто";
        return val;
    }
    public void recordDefaultSettings(){ //записываем некоторые дефолтные значение при первом старте приложения
        Cursor cursor=db.rawQuery("SELECT LastSync FROM Sys WHERE ID = 1",new String[] {} );
        if(!cursor.moveToFirst()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("SyncDelay", 5000);
            db.insert("Sys", null, contentValues);
        }
    }
    public void saveSafeMode(int status,int speed){  //сохраняем значение скорости для режима с защитой
        db.execSQL("UPDATE Sys SET Protection = ?, ProtectionStatus =? WHERE ID = ?",new String[] {Integer.toString(speed),Integer.toString(status),Integer.toString(1)});
        Log.i("DBHelper", "записали скорость защиты");
    }
    public  int[] getSaferMode(){  //получаем скорость для редима с защитой
        Cursor cursor= db.rawQuery("SELECT Protection, ProtectionStatus FROM Sys WHERE ID = 1 ",null);
        if(cursor.moveToLast()) {
            int[] arr = new int [2];
            int Index = cursor.getColumnIndex("Protection");
            arr[1] = cursor.getInt(Index);
            Index = cursor.getColumnIndex("ProtectionStatus");
            arr[0] = cursor.getInt(Index);
            return arr;
        }
        int[] arr = new int [2];
        arr[0]=0;
        arr[1]=0;
        return arr;
    }
    public void addUser(String user, String MAC){  //записываем юзера
        ContentValues contentValues = new ContentValues();
        contentValues.put("UserName", user);
        contentValues.put("MAC", MAC);
        db.insert("Users", null,contentValues);
    }
    public void savePassword(String pass){  //записываем идентификатор параметра, полученный с сервера
        db.execSQL("UPDATE Param SET Pswrd = ? WHERE ParamID = ?",new String[] {pass,Integer.toString(getParamID())});
        Log.i("DBHelper", "записали пароль "+ pass);
    }
    public String getPassword(){ // //получаем идентификатор текущего активного параметра
        String val;
        Cursor cursor= db.rawQuery("SELECT Pswrd FROM Param WHERE ParamID=? ",new String[]{Integer.toString(getParamID())});
        if(cursor.moveToLast()) {
            int Index = cursor.getColumnIndex("Pswrd");
            val = cursor.getString(Index);
            Log.i("DBHelper", "пароль "+(val));
            return val;
        }
        return "ожидание синхронихации";
    }
    public int getSyncDelay(){  //получаем значение задержки синхронизации
        Cursor cursor= db.rawQuery("SELECT SyncDelay FROM Sys WHERE ID=1 ",null);
        if(cursor.moveToFirst()) {
            int Index = cursor.getColumnIndex("SyncDelay");
            int val = cursor.getInt(Index);
            Log.i("DBHelper", "задержка"+Integer.toString(val));
            return val;
        }
        return 10;
    }
    public void setSyncDelay(int delay){ //записываем значение задержки синхронизции
        Log.i("DBHelper", "поставили задежку "+Integer.toString(delay));
        db.execSQL("UPDATE Sys SET SyncDelay = ? WHERE ID = 1",new String[] {Integer.toString(delay)});
    }
}
