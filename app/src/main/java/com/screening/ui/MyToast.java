package com.screening.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.activity.R;

public class MyToast {

    private static Toast toast;

    @SuppressLint("WrongConstant")
    public static void showToast(Context context, String msg){
        if(null == toast){
            toast = new Toast(context);
        }
        toast.setDuration(0);
        toast.setGravity(Gravity.CENTER,0,0);
        View view = LayoutInflater.from(context).inflate(R.layout.mytoast,null);
        TextView textView = view.findViewById(R.id.txt_toast);
        textView.setTextSize(20);
        textView.setTextColor(Color.BLACK);
        textView.setText(msg);
        toast.setView(view);
        toast.show();
    }

    public static void dissmissToast(){
        if(null != toast){
            toast.cancel();
        }
    }

}
