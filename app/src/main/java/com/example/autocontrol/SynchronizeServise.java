package com.example.autocontrol;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SynchronizeServise extends Service {  // сервис синхронизации с сервером
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    boolean trigger = true;
    String MAC,UserName;
    DBHelper dbHelper;
    BroadcastReceiver updateUIReciver;
    boolean statflag = true;
    Timer timer = new Timer(true);
    TimerTask syncTask;
    String newUser = "http://188.134.79.235:90/sync_users.php";
    String newParameter = "http://188.134.79.235:90/sync_parameters.php";
    String newStat = "http://188.134.79.235:90/sync_stat.php";
    int syncDelay =5000;
    Context con;
    Boolean synflag = false;


    public SynchronizeServise() {
        con = this;
    }
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    public void onCreate() {
        dbHelper = new DBHelper(this);  // подключамся к локальной БД
        IntentFilter filter = new IntentFilter();
        filter.addAction("StatSync");
        filter.addAction("StopServices");
        updateUIReciver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("StatSync") && statflag == false){
                    Log.i("SynchroService", "команда на синхронизацию статистики");
                    timer = new Timer();
                    timer.schedule(new SyncTask(timer,SynchronizeServise.this,con),syncDelay);
                    statflag = true;
                }
                else if (intent.getAction().equals("StopServices")){
                    Log.i("SynchroService", "команда на остановку");
                    onDestroy();
                }
            }
        };
        registerReceiver(updateUIReciver, filter);
        syncDelay = dbHelper.getSyncDelay()*1000; //получаем значение задержки статистики (для оптимизации трафика)
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(updateUIReciver);
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle arguments = intent.getExtras();
        MAC = arguments.get("MAC").toString();
        UserName = arguments.get("UserName").toString();
        syncTask = new SyncTask(timer,SynchronizeServise.this,this);
        if (isOnline(this)) { //если есть интернет, сразу синхронизируемся и ставим отметку
            checkLocal();
            trigger = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }
    boolean checkLocal(){   //проверяем синхронизацию пользователей, параметров и профилей
        boolean flag = false;
        if(dbHelper.checkUser()) { //юзера нет на сервере
            if(addUser()) flag = true;  //если есть не синхронизированные пользователи, проверяем связаные параметры
        }
        if(dbHelper.checkParam()) { //параметров нет на сервере
            if (addParameters()) flag = true;  //если есть не синхронизированные параметры, проверяем связаные параметры
        }
        if(addStat()) flag = true;
        if (flag) { //если синхронизация удалась, запоминаем время
            syncMes();
        }
        return true;
    }
    private void syncMes(){  // всем даем знать, что только что синхронизировались и запоминаем варемя
            DateFormat df = new SimpleDateFormat("dd.MM.yy, HH:mm");
            String date = df.format(Calendar.getInstance().getTime());
            Intent local = new Intent();
            local.setAction("SynchroSucess");
            local.putExtra("Time",date);
            dbHelper.recordLastSync();
            con.sendBroadcast(local);
        }
    private boolean addUser(){   // синхронизиуем таблицу Users
        Log.i("Синхронизатор", "добавляем юзера");
        RequestBody formBody = new FormBody.Builder().add("UserName", UserName) .add("MAC", MAC) .build();
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder() .url(newUser) .post(formBody) .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("Sync", e.toString());
                synflag = false;
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonText =response.body().string();
                JSONObject jsonRoot;
                try {
                    jsonRoot = new JSONObject(jsonText);
                    int success =  jsonRoot.getInt("success");
                    if (success >0){ //если пришел удачный пакет, делаем отметку о синхронизации и считываем ID пользователя
                    dbHelper.setSyncUserFlag(jsonRoot.getInt("UserID"),MAC,UserName);                              //записываем, что синхронизировано
                    }
                    synflag = true;
                    call.cancel();
                } catch (JSONException e) {
                    synflag = false;
                    e.printStackTrace();
                }

            }
        });
        while (!call.isCanceled()); //ждем ответа с сервера
        if (synflag){
            synflag = false;
            return true;
        }
        else return false;
    }
    private  boolean addParameters(){  //синхронизируем таблицу Param
        Parameters data = new Parameters(MAC, UserName, dbHelper);
        OkHttpClient client = new OkHttpClient();
        String msg = data.convertJSON().toString();
        RequestBody formBody = new FormBody.Builder().add("JSONPack", msg)  .build();
        //RequestBody Body = RequestBody.create(JSON,msg); не работает!!
        Request req = new Request.Builder().url(newParameter) .post(formBody).build();
        Call calling = client.newCall(req);
        calling.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                synflag = false;
                Log.i("Sync", e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonText = response.body().string();
                Log.i("Синхронизатор", "джисон:"+jsonText);
                JSONObject jsonRoot;
                try {
                    jsonRoot = new JSONObject(jsonText);
                    int success =  jsonRoot.getInt("success");
                    JSONObject arr= jsonRoot.getJSONObject("resp");
                    int id = arr.getInt("ParamID");
                    if (success >0){ //если пришел удачный пакет, делаем отметку о синхронизации и считываем ID пользователя
                    dbHelper.setSyncParamFlag(id);
                    dbHelper.savePassword(jsonRoot.getString("pswrd"));
                    synflag = true;
                    }
                } catch (JSONException e) {
                    synflag = false;
                    e.printStackTrace();
                }
                call.cancel();
            }
        });
        while(!calling.isCanceled()); //ожидаем ответа прежде чем идти дальше
        if(synflag){
            synflag = false;
            return true;
        }
        else return true;
    }
    private boolean addStat(){  // загружем статистику на сервер
        if(dbHelper.checkStat()){
            Log.i("SynchroService", "шлем пакет статистики");
            final Statistic data = new Statistic(dbHelper);
            OkHttpClient client = new OkHttpClient();
            String msg = data.convertJSON().toString();
            RequestBody formBody = new FormBody.Builder().add("JSONPack", msg)  .build();
            Request req = new Request.Builder().url(newStat) .post(formBody).build();
            Call calling = client.newCall(req);
            calling.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    statflag = false;
                    synflag = false;
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String jsonText = response.body().string();
                    Log.i("SynchroService", "ответ на пакет статистики " + jsonText);
                    JSONObject jsonRoot;
                    try {
                        jsonRoot = new JSONObject(jsonText);
                        int success =  jsonRoot.getInt("success");
                        if (success >0){ //если пришел удачный пакет, удаляем отправленные
                            dbHelper.deleteStats(data.id,data.lastRecordID);
                            synflag = true;
                        }
                    } catch (JSONException e) {
                        synflag = false;
                        e.printStackTrace();
                    }
                    call.cancel();
                    statflag = false;
                }
            });
            while(!calling.isCanceled());  //ждем ответа
        }
        else statflag = false;

        if (synflag){
            synflag = false;
            return true;
        }
        else return false;
    }
    public static boolean isOnline(Context context) { //проверка на наличие интернета
    ConnectivityManager cm =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo netInfo = cm.getActiveNetworkInfo();
    if (netInfo != null && netInfo.isConnectedOrConnecting())
    {
        return true;
    }
    Log.i("Синхронизатор", "нет интернета");
    return false;
}


}


class Parameters { //класс для получения параметров из БД и их конвертации в JSON

    private String UserName;
    private String MAC;
    private List<Param> parameters = new ArrayList<Param>();
    private JSONObject json;

    public  Parameters(String mc, String name, DBHelper db) {
        UserName = name;
        MAC = mc;
        Cursor cursor= db.getNoSyncParam(mc,name);
        if(cursor.moveToFirst()) {
            Param par = new Param();
            par.Length = cursor.getInt(cursor.getColumnIndex("Length"));
            par.Device= cursor.getString(cursor.getColumnIndex("Device"));
            par.Regulator= cursor.getString(cursor.getColumnIndex("Regulator"));
            par.LowPressure = cursor.getInt(cursor.getColumnIndex("LowPressure"));
            par.HighPressure = cursor.getInt(cursor.getColumnIndex("HighPressure"));
            par.BatteryType =  cursor.getString(cursor.getColumnIndex("BatteryType"));
            par.BatteryVoltage =  cursor.getInt(cursor.getColumnIndex("BatteryVoltage"));
            par.Capasitor =  cursor.getInt(cursor.getColumnIndex("Capasitor"));
            parameters.add(par);
            while (cursor.moveToNext()) {
                par.Length = cursor.getInt(cursor.getColumnIndex("Length"));
                par.Device= cursor.getString(cursor.getColumnIndex("Device"));
                par.Regulator= cursor.getString(cursor.getColumnIndex("Regulator"));
                par.LowPressure = cursor.getInt(cursor.getColumnIndex("LowPressure"));
                par.HighPressure = cursor.getInt(cursor.getColumnIndex("HighPressure"));
                par.HighPressure = cursor.getInt(cursor.getColumnIndex("HighPressure"));
                par.BatteryType =  cursor.getString(cursor.getColumnIndex("BatteryType"));
                par.BatteryVoltage =  cursor.getInt(cursor.getColumnIndex("BatteryVoltage"));
                par.Capasitor =  cursor.getInt(cursor.getColumnIndex("Capasitor"));
                parameters.add(par);
            }
            cursor.close();
            Log.i("Синхронизатор", "выгрузили параметры");
            }
        else{
            Log.i("Синхронизатор", "нет не синхронизированных параметров");
        }
        }

   public JSONObject convertJSON(){
   json = new JSONObject();
       try {
           json.put("UserName",UserName);
           json.put("MAC",MAC);
           JSONArray jsonArr = new JSONArray();
           for (Param par : this.parameters) {
               JSONObject pnObj = new JSONObject();
               pnObj.put("Length", par.Length);
               pnObj.put("Device", par.Device);
               pnObj.put("LowPressure", par.LowPressure);
               pnObj.put("HighPressure", par.HighPressure);
               pnObj.put("Regulator", par.Regulator);
               pnObj.put("BatteryType", par.BatteryType);
               pnObj.put("BatteryVoltage", par.BatteryVoltage);
               pnObj.put("Capasitor", par.Capasitor);
               jsonArr.put(pnObj);
           }
            json.put("Parameters",jsonArr);
           return json;
       } catch (JSONException e) {
           e.printStackTrace();
       }
       return null;
   }

   class Param {
        private int Length;
        private String Device;
        private int LowPressure;
        private int HighPressure;
        private String Regulator;
        private String BatteryType;
        private int Capasitor;
        private int BatteryVoltage;
    }
}
class Statistic{

    private ArrayList<Stats> statistics = new ArrayList<Stats>();
    private JSONObject json;
    public int id,glid, lastRecordID;

    public Statistic(DBHelper db){
        id=db.getParamID();
        Log.i("Синхронизатор", Integer.toString(id));
        Cursor cursor = db.getStats(id);
        glid=db.getGlobalParamID();
        if(cursor.moveToPosition(0)) {
            Stats stat = new Stats();
            int impIndex = cursor.getColumnIndex("impulse");
            int spIndex = cursor.getColumnIndex("Speed");
            int presIndex=cursor.getColumnIndex("Pressure");
            int voltIndex = cursor.getColumnIndex("Voltage");
            int shIndex = cursor.getColumnIndex("ShootDate");
            stat.Impulse = cursor.getInt(impIndex);
            stat.Speed= cursor.getInt(spIndex);
            stat.Pressure= cursor.getDouble(presIndex);
            stat.Voltage = cursor.getDouble(voltIndex);
            stat.ShootDate = cursor.getLong(shIndex);
            statistics.add(new Stats(stat));
            //int ind=1;
            while (cursor.moveToNext()) {
                stat.Impulse = cursor.getInt(impIndex);
                stat.Speed= cursor.getInt(spIndex);
                stat.Pressure= cursor.getDouble(presIndex);
                stat.Voltage = cursor.getDouble(voltIndex);
                stat.ShootDate = cursor.getLong(shIndex);
                statistics.add(new Stats(stat));
                //ind++;
            }
            if(cursor.moveToLast()) {
                lastRecordID = cursor.getInt(cursor.getColumnIndex("RecordID"));
            }
            cursor.close();
            Log.i("Синхронизатор", "выгрузили статистику");
        }
        }

    public JSONObject convertJSON(){
        json = new JSONObject();
        try {
            json.put("GlobalParamID",glid);
            JSONArray jsonArr = new JSONArray();
            for (Stats st : this.statistics) {
                JSONObject pnObj = new JSONObject();
                pnObj.put("Impulse", st.Impulse);
                pnObj.put("Speed", st.Speed);
                pnObj.put("Pressure", st.Pressure);
                pnObj.put("Voltage", st.Voltage);
                pnObj.put("ShootDate", st.ShootDate);
                jsonArr.put(pnObj);
            }
            json.put("Stats",jsonArr);
            return json;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

        class Stats{
        Stats (Stats st){
            Impulse =  st.Impulse;
            Speed = st.Speed;
            Pressure = st.Pressure;
            Voltage=st.Voltage;
            ShootDate = st.ShootDate;
        }
        Stats(){

        }
        private int Impulse;
        private int Speed;
        private double Pressure;
        private double Voltage;
        private long ShootDate;
        }
        }
class  SyncTask extends TimerTask {   //класс для синхронизации статистики по времени задержки
Timer timer;
Context context;
SynchronizeServise service;
    public SyncTask (Timer tim, SynchronizeServise serv, Context con){
        timer=tim;
        service=serv;
        context=con;
    }
    @Override
    public void run(){
        if (service.isOnline(context)) { //если есть интернет, сразу синхронизируемся и ставим отметку
            service.checkLocal();
        }
        timer.cancel();
        timer.purge();
    }
}