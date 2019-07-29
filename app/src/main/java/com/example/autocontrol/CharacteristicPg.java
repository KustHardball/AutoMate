package com.example.autocontrol;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class CharacteristicPg extends AppCompatActivity {  // окно ввод параметров
    private EditText gunName,workPres, extPres, regul, barell;
    private Button btn;
    String MAC, UserName;
    Spinner bankSpinner,btrSpinner;
    ArrayAdapter<String> adapter,voltadapter;
    Context context;
    SwitchCompat environmentSwitch, capasitorSwitch;
    TextView view1, view2,view3;
    EditText edit1, edit2;
    int flag;
    int pos=1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle arguments = getIntent().getExtras();
        MAC = arguments.get("MAC").toString();
        UserName = arguments.get("UserName").toString();
        flag = Integer.valueOf(arguments.get("Flag").toString());
        context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_characteristic_pg);
        addListenerOnButton();
        interfaceIni();
        switchListener();

    }
    public void addListenerOnButton(){  //при нажатии кнопки проверяем поля, и если все нормально сохраняем результат в локальной БД и идем на основную страницу
        btn = (Button) findViewById(R.id.saveButton);
        btn.setOnClickListener(
                new View.OnClickListener(){
            @Override
            public void onClick (View v){
                if(gunName.getText().toString().length()>0){
                    if (workPres.getText().toString().matches("[-+]?\\d+") && Integer.parseInt(workPres.getText().toString())>=0 && Integer.parseInt(workPres.getText().toString())<=300) {
                        if (extPres.getText().toString().matches("[-+]?\\d+") &&Integer.parseInt(extPres.getText().toString())>=0 && Integer.parseInt(extPres.getText().toString())<=300){
                                if (barell.getText().toString().matches("[-+]?\\d+") && Integer.parseInt(barell.getText().toString())>=50 && Integer.parseInt(barell.getText().toString())<=1000){
                                    //saveToDB(gunName.getText().toString(),Integer.parseInt(workPres.getText().toString()),Integer.parseInt(extPres.getText().toString()),regul.getText().toString(),Integer.parseInt(barell.getText().toString()));
                                    DBHelper dbHelper = new DBHelper(CharacteristicPg.this);
                                    if(flag==0) {
                                        dbHelper.addUser(UserName, MAC);
                                    }
                                    int cap;
                                    if(capasitorSwitch.isChecked())cap = 0;
                                    else cap=1;
                                    dbHelper.saveParamToDB(gunName.getText().toString(),Integer.parseInt(workPres.getText().toString()),Integer.parseInt(extPres.getText().toString()),regul.getText().toString(),Integer.parseInt(barell.getText().toString()),MAC,UserName,btrSpinner.getSelectedItem().toString(),pos,cap);
                                    dbHelper.saveSafeMode(0,160);  //значение краниться в другой таблице, поэтому отдельно
                                    Toast.makeText(CharacteristicPg.this, "Данные сохранены", Toast.LENGTH_SHORT).show();
                                    Intent MainPage = new Intent(".BaseActivity");
                                    MainPage.putExtra("MAC",MAC);
                                    MainPage.putExtra("UserName",UserName);
                                    startActivity(MainPage);
                                }
                                else Toast.makeText(CharacteristicPg.this, "Длина ствола должно быть от 50 до 1000 мм.", Toast.LENGTH_SHORT).show();
                        }
                        else Toast.makeText(CharacteristicPg.this, "Выходное давление редуктора должно быть от 10 до 300 атмосфер", Toast.LENGTH_SHORT).show();
                    }
                    else Toast.makeText(CharacteristicPg.this, "Рабочее давление редуктора должно быть от 100 до 300 атмосфер", Toast.LENGTH_SHORT).show();
                }
                else Toast.makeText(CharacteristicPg.this, "Введите марку агрегата", Toast.LENGTH_SHORT).show();
            }

        }
        );

    }
    public  void spinnersIni(){  //спинеры для выбора аккумулятора, после выбора типа, обновляем спинер выбоора напряжения
        Battery bat = new Battery();
        adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, bat.getTypes());
        btrSpinner = (Spinner) findViewById(R.id.batterySpinner);
        btrSpinner.setAdapter(adapter);
        bankSpinner = (Spinner) findViewById(R.id.banksSpinner);
        voltadapter = new ArrayAdapter<String>(context, R.layout.spinner_item, bat.getVolts(bat.getTypes()[0]));
        bankSpinner.setAdapter(adapter);
        btrSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                Battery battery = new Battery();
                voltadapter = new ArrayAdapter<String>(context, R.layout.spinner_item, battery.getVolts(battery.getTypes()[position]));
                bankSpinner.setAdapter(voltadapter); //подставляем возможные значения напряжения для этого аккумулятора
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        bankSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                pos = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }
    public void interfaceIni(){
        spinnersIni();
        environmentSwitch = (SwitchCompat) findViewById(R.id.gasSwitch);
        capasitorSwitch = (SwitchCompat) findViewById(R.id.capSwitch);
        gunName = (EditText) findViewById(R.id.gunNameText);
        workPres = (EditText) findViewById(R.id.workPresText);
        extPres =(EditText) findViewById(R.id.extPresText);
        regul = (EditText) findViewById(R.id.regulText);
        barell = (EditText) findViewById(R.id.barellText);
        view1 = (TextView) findViewById(R.id.textView3);
        view2 = (TextView) findViewById(R.id.textView4);
        edit1 = (EditText) findViewById(R.id.workPresText);
        edit2 = (EditText) findViewById(R.id.extPresText);
        view3 = (TextView) findViewById(R.id.textView5);

}
    public  void switchListener(){
        capasitorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {  //переключатель наличия конденсатора
                if(isChecked) pos = 0;
                else pos = 1;
            }
        });

        environmentSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {  //переключатель для выборе среды, деактивирует некоторые окна если выбрана СО2
                if(isChecked){
                    view1.setBackgroundResource(R.drawable.unactiv_edit_text_style);
                    view2.setBackgroundResource(R.drawable.unactiv_edit_text_style);
                    view3.setBackgroundResource(R.drawable.unactiv_edit_text_style);
                    edit1.setBackgroundResource(R.drawable.unactiv_edit_text_style);
                    edit2.setBackgroundResource(R.drawable.unactiv_edit_text_style);
                    regul.setBackgroundResource(R.drawable.unactiv_edit_text_style);
                    edit1.setText("0");
                    edit2.setText("0");
                    edit1.setEnabled(false);
                    edit2.setEnabled(false);
                    regul.setEnabled(false);
                }
                else{
                    view1.setBackgroundResource(R.drawable.edit_text_style);
                    view2.setBackgroundResource(R.drawable.edit_text_style);
                    view3.setBackgroundResource(R.drawable.edit_text_style);
                    edit1.setBackgroundResource(R.drawable.edit_text_style);
                    edit2.setBackgroundResource(R.drawable.edit_text_style);
                    regul.setBackgroundResource(R.drawable.edit_text_style);
                    edit1.setText("200");
                    edit2.setText("55");
                    edit1.setEnabled(true);
                    edit2.setEnabled(true);
                    regul.setEnabled(true);
                }
            }
        });
    }

}
