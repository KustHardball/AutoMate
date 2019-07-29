package com.example.autocontrol;


import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.*;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.UUID;
import java.util.Set;

public class BTActivity extends AppCompatActivity {  // окно выбора блютуз утройства

    private static final int REQUEST_ENABLE_BT = 1;
    private ListView list;
    ArrayAdapter<String> adapter;
    Bluth Bluetooth;
    private UUID myUUID;
    final Handler h = new Handler();
    AlertDialog.Builder alert;
    String MAC;
    String BLname;
    BroadcastReceiver updateUIReciver;
    Context cont;
    AlertDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt);
        cont = this;
        Bluetooth = new Bluth();  //объект для работы с Bluetooth
        if(Bluetooth.Start()) {  //изучаем техническую возможность телефона
            Toast.makeText(this, "Bluetooth отсутствует или неисправен", Toast.LENGTH_SHORT).show();
        }
        else{
            if(!Bluetooth.CheckOn()){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        listSpared(Bluetooth);
        myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        alert= new AlertDialog.Builder(BTActivity.this);  // создаем окно для ввода имени пользователя при подключении
        IntentFilter filter = new IntentFilter();   //приемник для вещаний с потока блютуса для вывода сообщений состояния Toast
        filter.addAction("CantConnect");
        filter.addAction("CheckDevice");
        filter.addAction("SuccesConnected");
        filter.addAction("GoToAlert");
        updateUIReciver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals("CantConnect")) {
                    Toast.makeText(BTActivity.this, "Подключиться не удалось, возможно устройство не включено!", Toast.LENGTH_SHORT).show();
                }
                else if (intent.getAction().equals("CheckDevice")){
                    Toast.makeText(BTActivity.this, "Проверьте устройство, похоже вы подключаетесь к какой-то ерунде вместо дрына", Toast.LENGTH_SHORT).show();
                }
                else if (intent.getAction().equals("SuccesConnected")){
                    Toast.makeText(BTActivity.this, "Вы успешно нашли дрын!", Toast.LENGTH_SHORT).show();
                }
                else if (intent.getAction().equals("GoToAlert")){
                    BTActivity.this.AlertConstruct();
                }
            }
        };
        registerReceiver(updateUIReciver, filter);
        listSpared(Bluetooth);
    }
    @Override
    protected void onDestroy() {
        try{
            if(updateUIReciver!=null)
                unregisterReceiver(updateUIReciver);
        }catch(Exception e){}
        super.onDestroy();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("finish", true); //команда на останов приложения
        startActivity(intent);
    }
    void AlertConstruct(){
        h.post(new Runnable() {
            @Override
            public void run() {
        alert.setTitle("Введите имя пользователя");
        alert.setIcon(R.drawable.mini_logo);
        final EditText input = new EditText(BTActivity.this);
        input.setTextColor(Color.parseColor("#949494"));
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();

                if (value.length()>0){
                    Intent CharPage = new Intent(".CharacteristicPg");
                    CharPage.putExtra("MAC",MAC);
                    CharPage.putExtra("UserName",value);
                    CharPage.putExtra("Flag",0);
                    startActivity(CharPage);
                }
                else {
                    toast("Имя не должно быть пустым");
                    AlertConstruct();
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
                dialog = alert.create();
                dialog.setOnShowListener( new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface arg0) {
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundResource(R.drawable.btn_simple);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundResource(R.drawable.btn_simple);
                        dialog.getWindow().setBackgroundDrawableResource(R.color.colorPrimary);
                    }
                });
        dialog.show();

            }
        });



    }
    public void toast(final String Text) {
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BTActivity.this, Text, Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void onButtonClick (View butt){  //показываем список спаренных устройств если не показался автоматически
        listSpared(Bluetooth);
    }
    public void listSpared(Bluth blue){  //работа с списком спаренных устройств
        list = (ListView)findViewById(R.id.listView);
        adapter = new ArrayAdapter<String>(this,R.layout.btdevices,blue.SearchSpared());
        list.setAdapter(adapter);
        list.setOnItemClickListener(
        new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick (AdapterView<?> adapterView, View view, int i, long l){
                    String val =(String)list.getItemAtPosition(i);
                    MAC = val.substring(val.length() - 17); // Вычленяем MAC-адрес
                    BLname = val.substring(1,val.length() - 19); //Вычлениваем имя устройства
                    BluetoothDevice device2 = Bluetooth.getAdapter().getRemoteDevice(MAC);
                    ThreadConnectBT myThreadConnectBTdevice = new ThreadConnectBT(device2, myUUID, true, cont);
                    myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
                }
            }
        );
        }






class Bluth {  //класс для проверок доступности Bluetooth
    BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
    boolean Start() { //проверяем техническую возможность телефона на Bluetooth
        if (bluetooth != null)// С Bluetooth все в порядке.
        {
            return false;

        } else
            return true;
    }

    boolean CheckOn() {  //проверяем включен ли
        if (bluetooth.isEnabled()) {// Bluetooth включен. Работаем.
            Log.i("Включен", "BT");
            return false;
        } else // Bluetooth выключен. Предложим пользователю включить его.
        {
            bluetooth.enable();
            Log.i("Включили", "BT");
            return true;

        }
    }

    String[] SearchSpared() {  //формируем список спаренных устройств
        Set<BluetoothDevice> pairedDevices = bluetooth.getBondedDevices();
        int i=0;
        if (pairedDevices.size() > 0) { // Если список спаренных устройств не пуст
            for (BluetoothDevice device : pairedDevices) {// проходимся в цикле по этому списку
                i++;
            }
            String[] devices = new String[i];
            int y=0;
            for (BluetoothDevice device : pairedDevices) {// проходимся в цикле по этому списку
                devices[y]=(device.getName() + "\n" + device.getAddress()); // Добавляем имена и адреса в mArrayAdapter, чтобы показать через ListView
                y++;
            }
            return  devices;

        }
        String[] none = new String[]{"Список пуст, найдите устройства через стандартный поиск Bluetooth устройств"};
        return  none;
    }
    BluetoothAdapter getAdapter(){
        return bluetooth;
    }
}

}