package com.example.autocontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceSettingsActivity extends AppCompatActivity {  // Activity настройки платы, значения идут из платы
    IntentFilter filter = new IntentFilter();
    BroadcastReceiver updateUIReciver;
    Context context;
    SwitchCompat cutoff, dRate,dImp, safer;
    Button saveBtn;
    EditText impulse,limpulse, rate, cutofflenth, splenth, saferText;
    String speed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_settings);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        context=this;
        interfaseIni();
        setFilters();
        receiver();
        saveBtn.setOnClickListener(  //кнопка сохранеия, отправляем команду сохранения в энергонезависимую память на плату
                new View.OnClickListener(){
                    @Override
                    public void onClick (View v){
                        write("S^");
                        DBHelper db = new DBHelper(context);
                        if (safer.isChecked()) {
                            db.saveSafeMode(1,Integer.valueOf(saferText.getText().toString()));
                        }
                        else  db.saveSafeMode(0,Integer.valueOf(saferText.getText().toString()));
                    }
                }
        );
        DBHelper db = new DBHelper(context);
        int[] arr;
        arr = db.getSaferMode();
        speed = Integer.toString(arr[1]);
        saferText.setText(speed);
        if (arr[0]>0){
            safer.setChecked(true);
        }
    }
    @Override
    protected void onStart(){
        super.onStart();
        write("R^"); //хотим пакет с данными, который подставим в поля
        setTextListeners();
    }
    @Override
    protected void onDestroy() {
        try{
            if(updateUIReciver!=null) //снимаем регистрацию рессивера если оон есть
                unregisterReceiver(updateUIReciver);
        }catch(Exception e){}
        super.onDestroy();
    }
    private void back(){
        this.onBackPressed();
    }
    private void receiver(){
        updateUIReciver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals("BTDisco")) {
                    back();
                }
                else if(intent.getAction().equals("GetOrderedSettingsPackage")) { //подставляем значения, пришедшие с платы
                    Log.i("SettingsActivity", "received R");
                    int val = intent.getIntExtra("Impulse",0);
                    double dobtval = intent.getDoubleExtra("Voltage",0);
                    impulse.setText(String.valueOf(val));
                    val = intent.getIntExtra("LowImpulse",0);
                    limpulse.setText(String.valueOf(val));
                    val = intent.getIntExtra("ShootRate",0);
                    rate.setText(String.valueOf(val));
                    cutofflenth.setText(String.valueOf(intent.getIntExtra("CutoffLenght",0)));
                    splenth.setText(String.valueOf(intent.getIntExtra("Splenght",0)));
                    disactivSwitchListener();
                    cutoff.setChecked(intent.getBooleanExtra("Cutoff",false));
                    dImp.setChecked(intent.getBooleanExtra("Shift",false));
                    dRate.setChecked(intent.getBooleanExtra("Rapid",false));
                    activSwitchListener();
                }
                else if(intent.getAction().equals("OutOfRange")) {
                    toast("Значение выходит за пределы допустимого");
                }
                else if(intent.getAction().equals("Saved")) {
                    toast("Настройки сохранены");
                }
                else if(intent.getAction().equals("NotSaved")) {
                    toast("Изменений нет, сохранять нечего");
                }
                else if(intent.getAction().equals("CutoffChanged")) {
                    disactivSwitchListener();
                    cutoff.setChecked(intent.getBooleanExtra("Status",false));
                    activSwitchListener();
                }
                else if(intent.getAction().equals("DimpChanged")) {
                    disactivSwitchListener();
                    dImp.setChecked(intent.getBooleanExtra("Status",false));
                    activSwitchListener();
                }
                else if(intent.getAction().equals("DrateChanged")) {
                    disactivSwitchListener();
                    dRate.setChecked(intent.getBooleanExtra("Status",false));
                    activSwitchListener();
                }
            }
        };
        registerReceiver(updateUIReciver, filter);
    }
    void interfaseIni(){
        saveBtn=(Button)findViewById(R.id.SaveBtn);
        impulse = (EditText) findViewById(R.id.impText);
        limpulse = (EditText) findViewById(R.id.limpText);
        rate = (EditText) findViewById(R.id.rateText);
        cutofflenth = (EditText) findViewById(R.id.rateLength);
        splenth = (EditText) findViewById(R.id.dynamicLength);
        cutoff = (SwitchCompat) findViewById(R.id.cutoff);
        dImp = (SwitchCompat) findViewById(R.id.dImp);
        dRate = (SwitchCompat) findViewById(R.id.dRate);
        safer =(SwitchCompat) findViewById(R.id.safer);
        saferText = (EditText) findViewById(R.id.speed);
    }
    private void setFilters(){
        filter.addAction("BTDisco");  //для изменения интерфейса при отключении
        filter.addAction("BTConn");  //для изменения интерфейса при подключении
        filter.addAction("GetSettingsPackage"); //обновление данных при стрельбе
        filter.addAction("GetOrderedSettingsPackage");  //получение данных по запросу
        filter.addAction("SynchroSucess");  //успешная синхронизация
        filter.addAction("ImpulseChanged");  //изменился импульс у платы
        filter.addAction("OutOfRange"); //выход за пределы допустимых значений на плате
        filter.addAction("Saved"); //плата успешно все сохранила
        filter.addAction("NotSaved");  //плата ничего не сохранила ибо не очень то и надо
        filter.addAction("RateChanged");
        filter.addAction("CutoffChanged");
        filter.addAction("DimpChanged");
        filter.addAction("DrateChanged");
    }
    void write (String str){  //отправить команду по БТ
        Intent local = new Intent();
        local.putExtra("String",str);
        local.setAction("UARTMSG");
        context.sendBroadcast(local);
    }
    private void toast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    public void disactivSwitchListener(){  //бездействем при программном изменении значения свитчей
        cutoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            }
        });
        dImp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            }
        });
        dRate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            }
        });
        safer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            }
        });
    }  //не хотим реагировать на программные переключения переключателей
    public  void activSwitchListener(){
        cutoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                write("C^");
            }
        });
        dImp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                write("O^");
            }
        });
        dRate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                write("D^");
            }
        });
        safer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            }
        });
    }
    public  void setTextListeners() {
        impulse.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (impulse.getText().toString().matches("[-+]?\\d+") && Integer.valueOf(impulse.getText().toString()) <= 10000 && Integer.valueOf(impulse.getText().toString()) >= 2000) {
                        write("A" + Integer.parseInt(impulse.getText().toString())/10 + "^");
                    } else {
                        Toast.makeText(context, "Вне допустимого диапазона", Toast.LENGTH_SHORT).show();
                        write("R^");
                    }

                }
            }

        });
        limpulse.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (limpulse.getText().toString().matches("[-+]?\\d+") && Integer.valueOf(limpulse.getText().toString()) < 10000 && Integer.valueOf(limpulse.getText().toString()) > 1000) {
                        write("P" + limpulse.getText().toString() + "^");
                    } else {
                        Toast.makeText(context, "Вне допустимого диапазона", Toast.LENGTH_SHORT).show();
                        write("R^");
                    }

                }
            }

        });
        rate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (rate.getText().toString().matches("[-+]?\\d+") && Integer.valueOf(rate.getText().toString()) <= 1200 && Integer.valueOf(rate.getText().toString()) >= 300) {
                        write("B" + Integer.toString(6000000/Integer.valueOf(rate.getText().toString())) + "^");
                    } else {
                        Toast.makeText(context, "Вне допустимого диапазона", Toast.LENGTH_SHORT).show();
                        write("R^");
                    }

                }
            }

        });
        cutofflenth.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if ( cutofflenth.getText().toString().matches("[-+]?\\d+") && Integer.valueOf(cutofflenth.getText().toString()) <= 1200 && Integer.valueOf(cutofflenth.getText().toString()) > 0) {
                        write("X" + cutofflenth.getText().toString() + "^");
                    } else {
                        Toast.makeText(context, "Вне допустимого диапазона", Toast.LENGTH_SHORT).show();
                        write("R^");
                    }

                }
            }

        });
        splenth.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (splenth.getText().toString().matches("[-+]?\\d+") && Integer.valueOf(splenth.getText().toString()) <= 1200 && Integer.valueOf(splenth.getText().toString()) >0) {
                        write("Z" + splenth.getText().toString() + "^");
                    } else {
                        Toast.makeText(context, "Вне допустимого диапазона", Toast.LENGTH_SHORT).show();
                        write("R^");
                    }

                }
            }

        });
        saferText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (saferText.getText().toString().matches("[-+]?\\d+") && Integer.valueOf(saferText.getText().toString()) <= 250 && Integer.valueOf(saferText.getText().toString()) >=100) {
                        speed = saferText.getText().toString();
                    } else {
                        Toast.makeText(context, "Вне допустимого диапазона", Toast.LENGTH_SHORT).show();
                        saferText.setText(speed);
                    }

                }
            }

        });

    }  //после ввода значений в поля, проверяем их и если все хорошо, отправляем новое значение в плату, если плохо, читаем старое значение из платы

}
