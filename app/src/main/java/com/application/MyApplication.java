package com.application;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.multidex.MultiDex;
import android.support.v7.widget.ViewUtils;
import android.util.Log;

import com.activity.R;
import com.huashi.bluetooth.HSBlueApi;
import com.logger.LogHelper;
import com.logger.TxtFormatStrategy;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.BuildConfig;
import com.orhanobut.logger.DiskLogAdapter;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;
import com.screening.activity.ScanShowActivity;
import com.screening.uitls.LogWriteUtils;
import com.squareup.leakcanary.LeakCanary;
import com.tencent.bugly.crashreport.CrashReport;
import com.util.Constss;
import com.util.ToastUtils;

import org.litepal.LitePal;
import org.litepal.LitePalApplication;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;


/**
 * Created by dell on 2018/4/23.
 */

public class MyApplication extends LitePalApplication{

    private static MyApplication mInstance;
    private static boolean isShow = false;
    protected static Context context;
    private String TAG = "TAG_MYAPP";
    private HSBlueApi api;
    String filepath = "";


    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        LitePal.initialize(this);
        MultiDex.install(this);
        CrashHandler.getInstance().init(this);
        initLogger();
        initBlueApi();
        initBlueState();
        serRxJavaErrorHandler();
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            // This process is dedicated to LeakCanary for heap analysis.
//            // You should not init your app in this process.
//            return;
//        }
//        LeakCanary.install(this);



        //        建议在测试阶段建议设置成true，发布时设置为false。
        CrashReport.initCrashReport(getApplicationContext(), "020812f7c4", true);

        LogWriteUtils.init(getApplicationContext());
    }

    //注册蓝牙相关api
    private void initBlueApi(){
        if(Constss.hsBlueApi==null){
            filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wltlib";// 授权目录
            api = new HSBlueApi(this, filepath);
            api.init();
            Constss.hsBlueApi=api;
        }

    }

    private void initBlueState(){
        //注册监听
        IntentFilter stateChangeFilter = new IntentFilter(
                BluetoothAdapter.ACTION_STATE_CHANGED);
        IntentFilter connectedFilter = new IntentFilter(
                BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter disConnectedFilter = new IntentFilter(
                BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(stateChangeReceiver, stateChangeFilter);
        registerReceiver(stateChangeReceiver, connectedFilter);
        registerReceiver(stateChangeReceiver, disConnectedFilter);
    }

    private BroadcastReceiver stateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                //连接上了
                Constss.isConnBlue=true;
//                ToastUtils.showToast(context,"已连接");
            } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                //蓝牙连接被切断
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                Constss.isConnBlue=false;
//                ToastUtils.showToast(context,"未连接");
                return;
            }
        }
    };
    public static synchronized MyApplication getInstance() {

        return mInstance;
    }



    private void initLogger() {
        //DEBUG版本才打控制台log BuildConfig.DEBUG

        if (true) {
            Logger.addLogAdapter(new AndroidLogAdapter(PrettyFormatStrategy.newBuilder().
                    tag("TAG_1_PAGELOG").build()));
        }
        //把log存到本地
        Logger.addLogAdapter(new DiskLogAdapter(TxtFormatStrategy.newBuilder().
                tag(getString(R.string.app_name)).build(getPackageName(), getString(R.string.app_name))));
    }

    //RxJava2 当取消订阅后(dispose())，RxJava抛出的异常后续无法接收(此时后台线程仍在跑，
    // 可能会抛出IO等异常),全部由RxJavaPlugin接收，需要提前设置ErrorHandler
    private void serRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                LogHelper.e("rxjava throwable.getMessage() = "+throwable.getMessage());
            }
        });

    }




}
