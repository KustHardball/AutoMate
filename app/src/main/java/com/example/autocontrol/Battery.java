package com.example.autocontrol;

import android.util.Log;

public class Battery {  //класс, содержащий таблицы уровней заряда для разных аккумуляоров
    public double[] status= new double [5]; //уровни заряда батареи
    private String[][] types ={{"li-po","3.7","3","4"},{"li-ion","3.7","3","4"},{"Ni-MH,Ni-cd","1.2","6","8","10"},{"Pb","6.0","2","3"},{"AA","1.5","6","8","10"}}; //1 значение-тип, 2- напряжение 1-й банки, далее возможные количетсва банок

    Battery() {
    }
    Battery(String type, int volt){  //создаем нужную нам таблицу уровня заряда
    if (type.equalsIgnoreCase(types[0][0])){ //li-po
        switch (volt){
            case 3:
                status[0]=9.82;
                status[1] = 11.18;
                status[2]= 11.39;
                status[3]=11.62;
                status[4] = 12.07;
                break;
            case 4:
                status[0]=13.09;
                status[1] = 14.91;
                status[2] = 15.18;
                status[3]=15.50;
                status[4] = 16.09;
                break;
            default:
                status[0]=9.82;
                status[1] = 11.18;
                status[2]= 11.39;
                status[3]=11.62;
                status[4] = 12.07;
                break;
        }
    }
}
public int getStatus(double val){  //возвращает уровень заряда по типу батареи и текущему нарпяжению
     int i = 0;
     while(i<5 && val > status[i]){
         i++;
     }
     return i;
}
public String[] getTypes(){ // возвращает массив возможных типов батарей
    String[] values;
    int index = 0;
    values = new String[types.length];
        for (String[] bat : types) {
            values[index]=bat[0];
            index++;
        }
return values;
}
public String[] getVolts(String type){ // возвращает массив возможных напряжений для заданного типа аккумов
        String[] values;
for (String[] bat : types) {
    if(bat [0] == type ){
        values = new String[bat.length-2];
        for(int i=2;i<bat.length;i++){
            values[i-2]=Double.toString(round((Double.valueOf(bat[i])*Double.valueOf(bat[1])), 2));
            Log.i("Battery", "знанчение "+bat[i]);
        }
        return values;
    }
}
return  null;
}
private double round(double number, int scale) { //округление
        int pow = 10;
        for (int i = 1; i < scale; i++)
            pow *= 10;
        double tmp = number * pow;
        return (double) (int) ((tmp - (int) tmp) >= 0.5 ? tmp + 1 : tmp) / pow;
    }
}
