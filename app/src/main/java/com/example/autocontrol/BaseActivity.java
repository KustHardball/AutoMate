package com.example.autocontrol;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;


public class BaseActivity extends AppCompatActivity { //основное Activity с основными органами управления и мониторами
    private String MAC, UserName;
    private  Context context;
    private BroadcastReceiver updateUIReciver;
    private Button DeviceSettingsBtn;
    private Button fireBtn, decrImp,incrImp,decrRate, incrRate,saveBtn;
    private TextView speed, pressure, voltage, impulse,setIText, setRText;
    private SwitchCompat cutoff, dRate,dImp, safe;
    private IntentFilter filter = new IntentFilter();
    private Animation rowAnim, blinkAnim, rotateAnim;
    private ImageView imageView, batImageView, loading, blImpulse,blSpeed,blVoltage,blPressure, impIcon,rateIcon;
    private ImageSwitcher status;
    private ImageButton settingsBtn;
    private TextView syncText, deviceText;
    private BluetoothAdapter bluetooth;
    private int rateOfFire;
    private Intent BTServIntent, SyncServ,StatisticIntent;
    private int textColor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        setFilters();
        Bundle arguments = getIntent().getExtras();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        MAC = arguments.get("MAC").toString();
        UserName = arguments.get("UserName").toString();
        interfaseIni(); //настраиваем интрерфейс
        receiver(); //устанавливаем приемник
        btStart();  //запускаем BT если не запущен
        textColor = ContextCompat.getColor(this, R.color.textColor);
        SetBattery(0);
        BTServIntent= new Intent(this, BTService.class);
        SyncServ= new Intent(this, SynchronizeServise.class);
        StatisticIntent= new Intent(this, StatisticServise.class);
        BTServIntent.putExtra("MAC",MAC);
        StatisticIntent.putExtra("MAC",MAC);
        StatisticIntent.putExtra("UserName",arguments.get("UserName").toString());
        SyncServ.putExtra("MAC",MAC);
        SyncServ.putExtra("UserName",arguments.get("UserName").toString());
        startService(BTServIntent);  //запускаем основные сервисы
        startService(StatisticIntent);
        startService(SyncServ);
        addListenerOnButton();
        DBHelper dbHelper = new DBHelper(this);
        syncText.setText("Последняя синхронизация: "+dbHelper.getLastSync()+"\nВаш логин: "+ new  DBHelper(context).getPassword());
        disconnected();
    }
    private void setFilters(){
        filter.addAction("BTDisco");  //для изменения интерфейса при отключении
        filter.addAction("BTConn");  //для изменения интерфейса при подключении
        filter.addAction("GetSettingsPackage"); //обновление данных при стрельбе
        filter.addAction("GetOrderedSettingsPackage");  //получение данных по запросу
        filter.addAction("SynchroSucess");  //успешная синхронизация
        filter.addAction("ImpulseChanged");  //изменился импульс у платы
        filter.addAction("OutOfRange"); //выход за пределы допустимых значений на плате
        filter.addAction("Saved");
        filter.addAction("NotSaved");
        filter.addAction("RateChanged");
        filter.addAction("CutoffChanged");
        filter.addAction("DimpChanged");
        filter.addAction("DrateChanged");
}
    public void addListenerOnButton(){
        fireBtn.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick (View v){
                        write("F^");
                    }
                }
        );

        DeviceSettingsBtn.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick (View v){
                        Intent setPage = new Intent(".DeviceSettingsActivity");
                        startActivity(setPage);
                    }
                }
        );

        decrImp.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick (View v){
                        write("-^");
                    }
                }
        );

        incrImp.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick (View v){
                        write("|^");
                    }
                }
        );

        incrRate.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick (View v){
                        int val = rateOfFire+10;
                        val = 6000000/val;
                        write("B"+String.valueOf(val)+ "^");
                    }
                }
        );
        decrRate.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick (View v){
                        int val = rateOfFire-10;
                        val = 6000000/val;
                        write("B"+String.valueOf(val)+ "^");
                    }
                }
        );

        saveBtn.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick (View v){
                        write("S^");
                    }
                }
        );
        settingsBtn.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick (View v){
                        Intent setActivity = new Intent(".appSettingsActivity");
                        setActivity.putExtra("MAC",MAC);
                        setActivity.putExtra("UserName",UserName);
                        startActivity(setActivity);
                    }
                }
        );

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
    }
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
        safe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(!isChecked){
                fireBtn.setBackgroundResource(R.drawable.oval_button_unactive);
                fireBtn.setEnabled(false);
                fireBtn.setTextColor(Color.parseColor("#ffffff"));
            }
            else{
                fireBtn.setBackgroundResource(R.drawable.oval_button);
                fireBtn.setEnabled(true);
                fireBtn.setTextColor(textColor);
            }
            }
        });
    }
    @Override
    protected void onDestroy() {
            try{
                if(updateUIReciver!=null)  //останавливаем приемник если запущен
                    unregisterReceiver(updateUIReciver);
            }catch(Exception e){}
            super.onDestroy();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("finish", true);
        stopService(BTServIntent); //останавливаем сервисы
        stopService(StatisticIntent);
        stopService(SyncServ);
        bluetooth.disable(); //закрываем БТ
        startActivity(intent);  //идем на начальную Activity с командой н остановку приложения
    }
    void interfaseIni(){
        speed =(TextView)findViewById(R.id.speedText);
        voltage =(TextView)findViewById(R.id.voltageText);
        impulse =(TextView)findViewById(R.id.impulseText);
        pressure =(TextView)findViewById(R.id.pressureText);
        setIText =(TextView)findViewById(R.id.setIText);
        setRText =(TextView)findViewById(R.id.setRText);
        fireBtn=(Button)findViewById(R.id.fireButton);  //кнопка выстрела
        decrImp=(Button)findViewById(R.id.decremImp);  //кнопка увеличения импульса
        incrImp=(Button)findViewById(R.id.incremImp);  //кнопка уменьшения импульса
        decrRate=(Button)findViewById(R.id.decremRate);  //кнопка увеличения импульса
        incrRate=(Button)findViewById(R.id.incremRate);  //кнопка уменьшения импульса
        DeviceSettingsBtn=(Button)findViewById(R.id.DeviceBtn);  //кнопка перехода в раздел настроек платы
        saveBtn=(Button)findViewById(R.id.SaveBtn);
        speed.setEnabled(false);
        voltage.setEnabled(false);
        impulse.setEnabled(false);
        pressure.setEnabled(false);
        DeviceSettingsBtn.setEnabled(false);
        fireBtn.setEnabled(false);
        rowAnim = AnimationUtils.loadAnimation(context, R.anim.sync_row_anim);
        blinkAnim = AnimationUtils.loadAnimation(context, R.anim.blinc_text);
        rotateAnim = AnimationUtils.loadAnimation(context, R.anim.connecting_rotate);
        imageView = (ImageView)findViewById(R.id.imageView);
        batImageView = (ImageView)findViewById(R.id.imageView2);
        loading = (ImageView)findViewById(R.id.loading);
        blImpulse =  (ImageView)findViewById(R.id.imageView5);
        blPressure =  (ImageView)findViewById(R.id.imageView10);
        blSpeed =  (ImageView)findViewById(R.id.imageView9);
        blVoltage =  (ImageView)findViewById(R.id.imageView8);
        status = (ImageSwitcher) findViewById(R.id.status);
        status.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView imageView = new ImageView(getApplicationContext());
                return imageView;
            }
        });
        Animation inAnimation = new AlphaAnimation(0, 1);
        inAnimation.setDuration(1000);
        Animation outAnimation = new AlphaAnimation(1, 0);
        outAnimation.setDuration(1000);
        status.setInAnimation(inAnimation);
        status.setOutAnimation(outAnimation);
        syncText = (TextView)findViewById(R.id.textView11);
        deviceText = (TextView)findViewById(R.id.textView13);
        cutoff = (SwitchCompat) findViewById(R.id.cutoff);
        dImp = (SwitchCompat) findViewById(R.id.dImp);
        dRate = (SwitchCompat) findViewById(R.id.dRate);
        safe = (SwitchCompat) findViewById(R.id.SafeSwitch);
        settingsBtn= (ImageButton) findViewById(R.id.settingsButton);
        rateIcon =  (ImageView)findViewById(R.id.imageView7);
        impIcon =  (ImageView)findViewById(R.id.imageView6);
    }
    void write (String str){  // отправка команд по БТ
        Intent local = new Intent();
        local.putExtra("String",str);
        local.setAction("UARTMSG");
        context.sendBroadcast(local);
    }
    public void SyncTurn(){  //обновляем данные по синхронизации (тлько анимация)
        imageView.startAnimation(rowAnim);
    }
    public void SetBattery(int val){  //устанавливаем соответствующую уровню заряда батареи картинку
        Log.i("BaseActivite", "получили напржение "+ Integer.toString(val));
        switch (val){
            case 0:
                batImageView.setImageResource(R.drawable.bat);
                break;
            case 1:
                batImageView.setImageResource(R.drawable.bat1);
                break;
            case 2:
                batImageView.setImageResource(R.drawable.bat2);
                break;
            case 3:
                batImageView.setImageResource(R.drawable.bat3);
                break;
            case 4:
                batImageView.setImageResource(R.drawable.bat4);
                break;
            case 5:
                batImageView.setImageResource(R.drawable.bat5);
                break;
                default:
                    batImageView.setImageResource(R.drawable.bat);
                    break;
        }
    }
    private void receiver(){
        updateUIReciver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals("BTConn")) { //запрашиваем пакет данных с платы если подключились
                    connected();  // делаем экран красивым
                    write("R^");
                }
                else if(intent.getAction().equals("BTDisco")) {
                    disconnected(); // делаем экран не красивым и депрессивным
                }
                else if(intent.getAction().equals("GetSettingsPackage")) {  //получаем пакет данных при выстреле и обновляем им данныена дисплее (напряжение из этого пакета не влияет на значек батарейки и используется для статистики)
                    int val = intent.getIntExtra("Impulse",0);
                    double dobtval = intent.getDoubleExtra("Voltage",0);
                    impulse.setText(String.valueOf(val)+" мкс");
                    voltage.setText(String.valueOf(dobtval)+" В");
                    val = intent.getIntExtra("Speed",0);
                    dobtval = intent.getDoubleExtra("Pressure",0);
                    speed.setText(String.valueOf(val)+ " м/с");
                    pressure.setText(String.valueOf(dobtval) + " Атм");
                }
                else if(intent.getAction().equals("GetOrderedSettingsPackage")) { // получаем ответ на запрошенный пакет данных и подставляемзначения на дисплей
                    int val = intent.getIntExtra("Impulse",0);
                    double dobtval = intent.getDoubleExtra("Voltage",0);
                    impulse.setText(String.valueOf(val)+" мкс");
                    setIText.setText(String.valueOf(val)+" мкс");
                    voltage.setText(String.valueOf(dobtval)+" В");
                    val = intent.getIntExtra("Speed",0);
                    dobtval = intent.getDoubleExtra("Pressure",0);
                    speed.setText(String.valueOf(val)+ " м/с");
                    pressure.setText(String.valueOf(dobtval) + " Атм");
                    val = intent.getIntExtra("ShootRate",0);
                    rateOfFire = val;
                    setRText.setText(String.valueOf(val) + " в/м");
                    SetBattery(intent.getIntExtra("VoltageStat",0));
                    disactivSwitchListener();
                    cutoff.setChecked(intent.getBooleanExtra("Cutoff",false));
                    dImp.setChecked(intent.getBooleanExtra("Shift",false));
                    dRate.setChecked(intent.getBooleanExtra("Rapid",false));
                    activSwitchListener();
                }
                else if(intent.getAction().equals("SynchroSucess")) {  //обновляем время синхронизации при её успехе
                    SyncTurn();
                    syncText.setText("Последняя синхронизация: "+intent.getStringExtra("Time")+"\nВаш логин: "+ new  DBHelper(context).getPassword());
                }
                else if(intent.getAction().equals("ImpulseChanged")) {
                    setIText.setText(intent.getIntExtra("Impulse",0)+" мкс");
                    Log.i("BaseUI", "импульс изменился на " + Integer.toString(intent.getIntExtra("Impulse",0)));
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
                else if(intent.getAction().equals("RateChanged")) {
                    setRText.setText(String.valueOf(intent.getIntExtra("ShootRate",0)) + " в/м");
                    rateOfFire = intent.getIntExtra("ShootRate",0);
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
    private void btStart(){
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (!bluetooth.isEnabled()) {// Bluetooth включаем, если выключен
            bluetooth.enable();
        }
        while (!bluetooth.isEnabled());
    }
    private void connected(){  //состояние интерфейса если подключены к плате
        DBHelper db = new DBHelper(this);
        deviceText.setText("Подключено к "+ db.getGunName());
        deviceText.setTextColor(Color.parseColor("#27ba42"));
        deviceText.clearAnimation();
        status.setImageResource(R.drawable.done);
        loading.clearAnimation();
        loading.setVisibility(View.GONE);
        blSpeed.setImageResource(R.drawable.speed);
        blPressure.setImageResource(R.drawable.pressure);
        blImpulse.setImageResource(R.drawable.impulse);
        blVoltage.setImageResource(R.drawable.voltage);
        dRate.setClickable(true);
        dImp.setClickable(true);
        cutoff.setClickable(true);
        safe.setClickable(true);
        DeviceSettingsBtn.setEnabled(true);
        decrImp.setEnabled(true);
        incrImp.setEnabled(true);
        decrRate.setEnabled(true);
        incrRate.setEnabled(true);
        saveBtn.setEnabled(true);
        setRText.setBackgroundResource(R.drawable.edit_text_style);
        setIText.setBackgroundResource(R.drawable.edit_text_style);
        impIcon.setImageResource(R.drawable.imp);
        rateIcon.setImageResource(R.drawable.rate);
    }
    private void disconnected(){ //состояние интерфейса если отключениы от платы
        deviceText.setText("Подключение...");
        deviceText.setTextColor(Color.parseColor("#ff708d"));
        deviceText.startAnimation(blinkAnim);
        SetBattery(0);
        status.setImageResource(R.drawable.connecting);
        loading.setVisibility(View.VISIBLE);
        loading.startAnimation(rotateAnim);
        blSpeed.setImageResource(R.drawable.not_active_speed);
        blPressure.setImageResource(R.drawable.not_activ_pressure);
        blImpulse.setImageResource(R.drawable.not_activ_pulse);
        blVoltage.setImageResource(R.drawable.not_active_voltage);
        speed.setText("");
        voltage.setText("");
        pressure.setText("");
        impulse.setText("");
        disactivSwitchListener();
        cutoff.setChecked(false);
        dRate.setChecked(false);
        dImp.setChecked(false);
        safe.setChecked(false);
        dRate.setClickable(false);
        dImp.setClickable(false);
        cutoff.setClickable(false);
        safe.setClickable(false);
        DeviceSettingsBtn.setEnabled(false);
        decrImp.setEnabled(false);
        incrImp.setEnabled(false);
        decrRate.setEnabled(false);
        incrRate.setEnabled(false);
        saveBtn.setEnabled(false);
        setRText.setBackgroundResource(R.drawable.unactiv_edit_text_style);
        setIText.setBackgroundResource(R.drawable.unactiv_edit_text_style);
        setRText.setText("");
        setIText.setText("");
        impIcon.setImageResource(R.drawable.impuls);
        rateIcon.setImageResource(R.drawable.sleeve);
    }
    private void toast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}

