package com.example.autocontrol;

import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class UARTParser {  // расшифровывает полученную строку и отправляет широковещательные пакеты кому че делать
    Context con;
    double koef = 61.84; //коэфициент для напряжения  //59.48
    Battery bat;



    public UARTParser(Context c){
        con = c;
        bat = new Battery("li-po",3);
    }

    void startParse(String val){  //парсим команды с БТ, преобразовываем значения иотправляем на ресиверы соответствующие команды
        Log.i("Parser", "получено "+ val);
        Intent local = new Intent();
        int index, value, prindex;
        char[] dst=new char[10];  //буфер для хранения числового значения параметра для последующего преобразования в int
        String v;  // строка для буфера
        double dobvalue;
        switch (val.charAt(0)) {
            case 'R':  //рассылаем данные, полученные по запросу
                Log.i("Parser", "received R");
                index = val.indexOf('*');  //импульс
                val.getChars(2,index,dst,0);
                //int offset = 0; //костыль для отсечения случайного знака
                //if (dst[0]=='^') offset = 1;
                v = String.copyValueOf(dst,0,index-2);
                value = Integer.parseInt(v)*10;
                local.setAction("GetOrderedSettingsPackage");
                local.putExtra("Impulse",value);

                prindex=index+1;  //нижний импульс
                index = val.indexOf('*',index+1);
                val.getChars(prindex,index,dst,0);
                v = String.copyValueOf(dst,0,index-prindex);
                value = Integer.parseInt(v)*10;
                local.putExtra("LowImpulse",value);

                prindex=index+1;  //скорострельность
                index = val.indexOf('*',index+1);
                val.getChars(prindex,index,dst,0);
                v = String.copyValueOf(dst,0,index-prindex);
                value = (6000000/(Integer.parseInt(v)));  // высчитываем скорострельнос в выс. в мин.
                local.putExtra("ShootRate",value);

                prindex=index+1; //длина отсечки
                index = val.indexOf('*',index+1);
                val.getChars(prindex,index,dst,0);
                v = String.copyValueOf(dst,0,index-prindex);
                value = Integer.parseInt(v);
                local.putExtra("CutoffLenght",value);

                prindex=index+1; //длина для динамических режимов
                index = val.indexOf('*',index+1);
                val.getChars(prindex,index,dst,0);
                v = String.copyValueOf(dst,0,index-prindex);
                value = Integer.parseInt(v);
                local.putExtra("Splenght",value);

                prindex=index+1; //режим
                index = val.indexOf('*',index+1);
                val.getChars(prindex,index,dst,0);
                v = String.copyValueOf(dst,0,index-prindex);
                value = Integer.parseInt(v);
                switch(value){
                    case 1:
                        local.putExtra("Cutoff",true);
                        break;
                    case 2:
                        local.putExtra("Rapid",true);
                        break;
                    case 4:
                        local.putExtra("Shift",true);
                        break;
                    case 3:
                        local.putExtra("Rapid",true);
                        local.putExtra("Cutoff",true);
                        break;
                    case 6:
                        local.putExtra("Rapid",true);
                        local.putExtra("Shift",true);
                        break;
                    case 5:
                        local.putExtra("Cutoff",true);
                        local.putExtra("Shift",true);
                        break;
                    case 7:
                        local.putExtra("Cutoff",true);
                        local.putExtra("Shift",true);
                        local.putExtra("Rapid",true);
                        break;
                        default:
                            local.putExtra("Rapid",false);
                            local.putExtra("Shift",false);
                            local.putExtra("Cutoff",false);
                }
                prindex=index+1; //напряжение
                index = val.indexOf('*',index+1);
                val.getChars(prindex,index,dst,0);
                v = String.copyValueOf(dst,0,index-prindex);
                dobvalue = (double)Integer.parseInt(v)/koef;
                int st = bat.getStatus(dobvalue);
                local.putExtra("Voltage",round(dobvalue, 2));
                local.putExtra("VoltageStat",st);

                prindex=index+1; //давление
                index = val.indexOf('*',index+1);
                val.getChars(prindex,index,dst,0);
                v = String.copyValueOf(dst,0,index-prindex);
                dobvalue = (double)Integer.parseInt(v)/10.3;  //исправить коэфициент!!!
                local.putExtra("Pressure",round(dobvalue, 2));

                prindex=index+1; //скорость
                index = val.indexOf('*',index+1);
                val.getChars(prindex,index,dst,0);
                v = String.copyValueOf(dst,0,index-prindex);
                value = Integer.parseInt(v);
                local.putExtra("Speed",value);

                con.sendBroadcast(local);
                break;
            case 'L':  //рассылаем данные, полученные при выстреле
                long time=System.currentTimeMillis();
                Log.i("Время", String.valueOf(time));
                index = val.indexOf('*');
                val.getChars(2,index,dst,0);
                v = String.copyValueOf(dst,0,index-2);
                value = Integer.parseInt(v)*10;
                local.setAction("GetSettingsPackage");
                local.putExtra("Impulse",value);

                prindex=index+1;
                index = val.indexOf('*',index+1);
                val.getChars(prindex,index,dst,0);
                v = String.copyValueOf(dst,0,index-prindex);
                dobvalue = (double)Integer.parseInt(v)/koef;
                local.putExtra("Voltage",round(dobvalue, 2));

                prindex=index+1;
                index = val.indexOf('*',index+1);
                val.getChars(prindex,index,dst,0);
                v = String.copyValueOf(dst,0,index-prindex);
                dobvalue = (double)Integer.parseInt(v)/10.3;  //исправить коэфициент!!!
                local.putExtra("Pressure",round(dobvalue, 2));

                prindex=index+1;
                index = val.indexOf('*',index+1);
                val.getChars(prindex,index,dst,0);
                v = String.copyValueOf(dst,0,index-prindex);
                value = Integer.parseInt(v);
                local.putExtra("Speed",value);
                local.putExtra("Time",time);

                con.sendBroadcast(local);
                break;
            case '|'  :
                if(val.charAt(2)=='O' && val.charAt(3)=='K') {
                    local.setAction("ImpulseChanged");
                    index = val.indexOf('*',4);
                    val.getChars(4,index,dst,0);
                    v = String.copyValueOf(dst,0,index-4);
                    local.putExtra("Impulse", Integer.parseInt(v)*10);
                    con.sendBroadcast(local);
                }
                else{
                    local.setAction("OutOfRange");
                    con.sendBroadcast(local);
                }
            case '-'  :
                if(val.charAt(2)=='O' && val.charAt(3)=='K') {
                    local.setAction("ImpulseChanged");
                    index = val.indexOf('*',4);
                    val.getChars(4,index,dst,0);
                    v = String.copyValueOf(dst,0,index-4);
                    local.putExtra("Impulse", Integer.parseInt(v)*10);
                    con.sendBroadcast(local);
                }
                else{
                    local.setAction("OutOfRange");
                    con.sendBroadcast(local);
                }
                break;
            case 'S'  :  //ответ на попытку сохранить настройки
                if(val.charAt(2)=='O' && val.charAt(3)=='K') {
                    local.setAction("Saved");
                    con.sendBroadcast(local);
                }
                else{
                    local.setAction("NotSaved");
                    con.sendBroadcast(local);
                }
                break;

            case 'B'  :  //рассылаем данные, полученные при выстреле
                int pos = val.indexOf('^',2);
                if(val.charAt(pos+1)=='O' && val.charAt(pos+2)=='K') {
                    local.setAction("RateChanged");
                    index = val.indexOf('*',pos);
                    Log.i("Parser", "позиции "+Integer.toString(pos)+" и "+Integer.toString(index));
                    val.getChars(pos+3,index,dst,0);
                    v = String.copyValueOf(dst,0,index-(pos+3));
                    value = (6000000/(Integer.parseInt(v)));
                    Log.i("Parser", "скорострельность изменена на "+v);
                    local.putExtra("ShootRate", value);
                    con.sendBroadcast(local);
                }
                else{
                    local.setAction("OutOfRange");
                    con.sendBroadcast(local);
                }
                break;
            case 'C'  :
                if(val.charAt(2)=='O' && val.charAt(3)=='K') {
                    local.setAction("CutoffChanged");
                    if (val.charAt(4) =='0') {
                        local.putExtra("Status", false);
                    }
                    else if (val.charAt(4) =='1') {
                        local.putExtra("Status", true);
                    }
                    con.sendBroadcast(local);
                }
                break;
            case 'O'  :
                if(val.charAt(2)=='O' && val.charAt(3)=='K') {
                    local.setAction("DimpChanged");
                    if (val.charAt(4) =='0') {
                        local.putExtra("Status", false);
                    }
                    else if (val.charAt(4) =='1') {
                        local.putExtra("Status", true);
                    }
                    con.sendBroadcast(local);
                }
                break;
            case 'D'  :
                if(val.charAt(2)=='O' && val.charAt(3)=='K') {
                    local.setAction("DrateChanged");
                    if (val.charAt(4) =='0') {
                        local.putExtra("Status", false);
                    }
                    else if (val.charAt(4) =='1') {
                        local.putExtra("Status", true);
                    }
                    con.sendBroadcast(local);
                }
                break;
                default:
                    break;
        }

    }
    private double round(double number, int scale) {  //метод для округления значений
        int pow = 10;
        for (int i = 1; i < scale; i++)
            pow *= 10;
        double tmp = number * pow;
        return (double) (int) ((tmp - (int) tmp) >= 0.5 ? tmp + 1 : tmp) / pow;
    }

}

