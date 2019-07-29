package com.example.autocontrol;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class appSettingsActivity extends AppCompatActivity {  //настройки приложения
Button userBtn, paramBtn, helpBtn;
EditText delayText;
Context context;
AlertDialog dialog1, dialog2;
AlertDialog.Builder alert;
String MAC, UserName;
String helpUrl = "http://188.134.79.235:90/main_grafic.php"; //адрес страници сайта
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_settings);
        context = this;
        interfaceIni();
        setTextListeners();
        addListenerOnButton();
        Bundle arguments = getIntent().getExtras();
        MAC = arguments.get("MAC").toString();
        UserName = arguments.get("UserName").toString();
        createDialog();
    }
    public void interfaceIni(){
        userBtn = (Button)findViewById(R.id.userBtn);  //полностью чистим БД и закрываемприлодение для перезапуска
        paramBtn =(Button)findViewById(R.id.parBtn);  //пользователь вводит новые параметры, которы становятся активными
        helpBtn=(Button)findViewById(R.id.helpButton);  //для захода наглавный сайт
        delayText=(EditText) findViewById(R.id.delayText);
        delayText.setText(Integer.toString(new DBHelper(context).getSyncDelay()));
    }
    public  void setTextListeners() {
        delayText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    checkDelay();
                }
            }

        });
    }
    public void addListenerOnButton() {
        helpBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkDelay();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(helpUrl));
                        startActivity(browserIntent);
                    }
                }
        );
        paramBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog1.show();
                    }
                }
        );
        userBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog2.show();
                    }
                }
        );
    }
    @Override
    protected void onDestroy() {
        checkDelay();  // перед выдодом запоминаем значение задержки синхронизации
        super.onDestroy();
    }
    public void checkDelay(){ //ароверяем введенное значение
        if (delayText.getText().toString().matches("[-+]?\\d+") && Integer.valueOf(delayText.getText().toString()) <= 360 && Integer.valueOf(delayText.getText().toString()) >= 0) {
            new DBHelper(context).setSyncDelay(Integer.valueOf(delayText.getText().toString()));
        }
        else {
            Toast.makeText(context, "Задержка может быть от 0 до 360 секунд", Toast.LENGTH_LONG).show();
        }
    }
    public void createDialog() {  // создаем 2 диалоговых окна для подтверждения действий кнопках
        alert= new AlertDialog.Builder(context);
        //alert.setTitle("Изменение параментров оружия сбросит статистику, продолжить?");
        alert.setMessage("Изменение параментров оружия сбросит статистику, продолжить?");
        alert.setIcon(R.drawable.mini_logo);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent CharPage = new Intent(".CharacteristicPg");
                CharPage.putExtra("MAC",MAC);
                CharPage.putExtra("UserName",UserName);
                CharPage.putExtra("Flag",1);
                startActivity(CharPage);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        dialog1 = alert.create();
        dialog1.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                dialog1.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundResource(R.drawable.btn_simple);
                dialog1.getButton(AlertDialog.BUTTON_NEGATIVE).setText("Нет");
                dialog1.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundResource(R.drawable.btn_simple);
                dialog1.getButton(AlertDialog.BUTTON_POSITIVE).setText("Да");
                dialog1.getWindow().setBackgroundDrawableResource(R.color.textColor);
            }
        });
        alert.setMessage("Действие уничтожит все данные и обнулит статистику, продолжить?");
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                new DBHelper(context).Refresh();  //удаляем БД
                Intent local = new Intent();
                local.setAction("StopServices");
                context.sendBroadcast(local);
                Intent intent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("finish", true);
                startActivity(intent);
                finish();
            }
        });
        dialog2 = alert.create();
        dialog2.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                dialog2.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundResource(R.drawable.btn_simple);
                dialog2.getButton(AlertDialog.BUTTON_NEGATIVE).setText("Нет");
                dialog2.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundResource(R.drawable.btn_simple);
                dialog2.getButton(AlertDialog.BUTTON_POSITIVE).setText("Да");
                dialog2.getWindow().setBackgroundDrawableResource(R.color.textColor);
            }
        });
    }
}

