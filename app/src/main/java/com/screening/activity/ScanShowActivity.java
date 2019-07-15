package com.screening.activity;

import android.bluetooth.BluetoothDevice;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.Manager.DataManager;
import com.activity.LiveVidActivity;
import com.activity.R;
import com.huashi.bluetooth.HsInterface;
import com.huashi.bluetooth.IDCardInfo;
import com.jakewharton.rxbinding3.widget.RxTextView;
import com.kalu.ocr.CaptureActivity;
import com.logger.LogHelper;
import com.model.DevModel;
import com.screening.model.BarcodeBean;
import com.screening.model.Item;
import com.screening.model.ListMessage;
import com.screening.rotatedialog.dialog.MyDialog;
import com.screening.ui.MyToast;
import com.screening.ui.WifiLoadingAnim;
import com.screening.uitls.BardcodeSettingUtils;
import com.screening.uitls.BluetoothUtils;
import com.screening.uitls.Constant;
import com.screening.uitls.ModifyORAdd;
import com.screening.uitls.SearchMessage;
import com.screening.uitls.VerificationUtils;
import com.screening.wifi.WifiConnectUtil;
import com.southtech.thSDK.lib;
import com.util.AlignedTextUtils;
import com.util.Constss;
import com.util.FileUtil;
import com.util.SouthUtil;
import com.util.ToastUtils;
import com.view.LoadingDialog;

import org.jetbrains.annotations.NotNull;
import org.litepal.LitePal;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import exocr.exocrengine.EXOCRModel;
import es.dmoral.toasty.Toasty;
import exocr.exocrengine.EXOCRModel;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class ScanShowActivity extends AppCompatActivity implements MyDialog.OnDialogButtonClickListener, View.OnClickListener,
        WifiConnectUtil.WifiConnectResultListener, VerificationUtils.VerificationResult {
    private TextView tv01, tv02, tv03, tv04, tv05, tv06, tv07, tv08, tv09, tv10,
            tv11, tv12, tv13, tv14, tv15, tv16, tv17, tv18, tv19, tv20, tv21, tv22, title_text;
    private String[] tvName;//标识字段的名称集合
    private String[] marryDatas;//婚否
    private String[] bhdDatas;//避孕方式
    private Spinner sp_marray, sp_contraception;
    private MyDialog myDialog;
    private EditText et_pId, et_pName, et_pAge, et_pSex, et_pPhone, et_pLocalPhone, et_pDate, et_pOccupation,
            et_smoking, et_pSexualpartners, et_pGestationaltimes,
            pAbortion, et_pRequiredID, et_pRequiredHPV, et_pRequiredCytology,
            et_pDetailedaddress, et_pRemarks, et_pRequiredGene, et_pRequiredDNA, et_pRequiredOther;
    private List<ListMessage> listMessages;
    private ImageView iv_screen, iv_scanhpv, iv_scanxbx, iv_scanjyjc, iv_scandna, iv_scanother, iv_scanidcard;
    private Button btn_left, btn_right, bt_clear, bt_save, bt_getImage, bt_add;
    private WifiLoadingAnim wifiLoadingAnim;
    private String marry = "无", birthControlMode = "无";
    private String screenId = "default";
    private Timer timer;
    private ModifyORAdd modifyORAdd;
    private Handler mHandler = new ShowHandler(this);
    private int id = -1;
    private boolean isSave = false;
    private BluetoothUtils bluetoothUtils;
    private Item mConst;
    private String searchMsg;
    private DataManager dataManager = DataManager.getInstance();
    private LoadingDialog mDialog;
    private boolean isFront = false;//判断当前页面是否在前台

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//禁止屏幕休眠
        setContentView(R.layout.activity_scan_show);
        id = getIntent().getIntExtra("id", -1);
        initView();
        initTextView();
        initData();
        initClick();
        initSelectItemClick();
        initEditTextClick();
        WifiConnectUtil.Companion.getInstance().setWifiConnectListener(this, null);
        VerificationUtils.getChange(et_pRequiredHPV,et_pRequiredCytology,et_pRequiredGene,et_pRequiredDNA,et_pRequiredOther,et_pRequiredID);
    }

    private void initEditTextClick() {
        RxTextView.textChanges(et_pRequiredID)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<CharSequence>() {
                    @Override
                    public void accept(CharSequence charSequence) throws Exception {
                        String age = VerificationUtils.getAge(et_pRequiredID.getText().toString().trim());
                        et_pAge.setText(age);
                        boolean idCardSaved = SearchMessage.getIdCard(et_pRequiredID.getText().toString().trim());
                        if(idCardSaved){
                            MyToast.showToast(ScanShowActivity.this,getString(R.string.patient_had));
                        }
                    }
                });
        RxTextView.textChanges(et_pLocalPhone)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<CharSequence>() {
                    @Override
                    public void accept(CharSequence charSequence) throws Exception {
                        String phone = et_pPhone.getText().toString().trim();
                        if(phone.equals("")){
                            return;
                        }
                        boolean phoneCorrect = VerificationUtils.isMobile(et_pPhone.getText().toString().trim());
                        if(!phoneCorrect){
                            MyToast.showToast(ScanShowActivity.this,getString(R.string.case_phone_error));
                        }
                    }
                });
    }

    private Disposable mDisposable = null;

    private void dispose() {
        if (mDisposable != null) {
            mDisposable.dispose();
        }
    }

    private Observable observable = null;
    private Observable observable1 = null;

    private void searchDevss() {
        showDiolog(getString(R.string.search_szb));
        dispose();


        observable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {

//                Thread.sleep(1000);
                searchMsg = lib.jthNet_SearchDev_old();
                String msgType = analyzeDevMsg(searchMsg);
                LogHelper.e("msgType= " + msgType + " ,isFront= " + isFront);
                if (msgType.equals("error1")) { //当前平板没有搜索到主机设备
                    Thread.sleep(5000);
                    emitter.onError(new Throwable("error1"));
                } else if (msgType.equals("error2")) {

                } else if (msgType.equals("error3")) {

                } else if (msgType.equals("error4")) {

                } else if (msgType.equals("success")) {
                    emitter.onNext("success");
                }
            }
        }).retry(5);

        observable1 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                Thread.sleep(1000);
                searchMsg = lib.jthNet_SearchDev_old();
                String msgType = analyzeDevMsg(searchMsg);
                LogHelper.e("msgType= " + msgType);
                if (msgType.equals("error1")) { //当前平板没有搜索到主机设备
                    Thread.sleep(5000);
                    emitter.onError(new Throwable("error1"));
                } else if (msgType.equals("error2")) {

                } else if (msgType.equals("error3")) {

                } else if (msgType.equals("error4")) {

                } else if (msgType.equals("success")) {
                    emitter.onNext("success");
                }
            }
        }).onErrorResumeNext(new Function<Throwable, ObservableSource<? extends String>>() {
            @Override
            public ObservableSource<? extends String> apply(Throwable throwable) throws Exception {
                return observable;
            }
        });

        observable1.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposable = d;
                    }

                    @Override
                    public void onNext(String s) {
                        if (s.equals("success") && isFront) {
                            Intent intent = new Intent(ScanShowActivity.this, LiveVidActivity.class);
                            intent.putExtra("user_id", et_pRequiredID.getText().toString().trim());// id
                            intent.putExtra("dev", mDevModel);
                            Log.e("TAG_", "handleMessage: id =" + et_pRequiredID.getText().toString().trim());
                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        LogHelper.e("throwable e=" + e.getMessage());
                        MyToast.showToast(ScanShowActivity.this, getString(R.string.image_setting_STA_wifi));
//                        Toasty.normal(ScanShowActivity.this, getString(R.string.image_setting_STA_wifi)).show();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private DevModel mDevModel = null;

    //分析从主机设备上的到的消息
    private String analyzeDevMsg(String msg) {
        //当前平板没有搜索到主机设备
        if ("".equals(msg)) return "error1";

        String[] devArray = msg.split("@");
        if (devArray.length == 0) return "error2";
        //add 0407 搜索设备全部删除
        if (!dataManager.deleteAllDev()) {
            return "error3";
        }

        for (int i = 0; i < devArray.length; i++) {
            String[] array = devArray[i].split(",");
            if (array.length < 6) {
                continue;
            }
            mDevModel = new DevModel();
            mDevModel.ip = array[0];
            mDevModel.sn = array[1];
            mDevModel.online = DevModel.EnumOnlineState.Online;
            mDevModel.dataport = array[2];
            mDevModel.httpport = array[3];
            mDevModel.name = array[5];

            if (!dataManager.addDev(mDevModel)) {
                return "error4";
            }
        }
        return "success";
    }

    //wifi连接成功
    @Override
    public void wifiConnectSuccess(String type) {
        stopAnim();
        if (type.equals(Constss.WIFI_TYPE_SZB)) {

            FileUtil.createFileFolder(ScanShowActivity.this, et_pId.getText().toString().trim(), et_pRequiredID.getText().toString().trim());
            //设置默认值
            Constss.picYuanTuCount = 0;
            Constss.picCuSuanBaiCount = 0;
            Constss.picCuSuanBaiAutoCount = 0;
            Constss.picDianYouCount = 0;
            Constss.picDianYouAutoCount = 0;
            Constss.vedioCount = 0;
            searchDevss();

        } else {
            dismissDiolog();
        }
    }

    @Override
    public void wifiConnectScaning(@NotNull String type) {
//        showDiolog(getString(R.string.wifiProcessMsg));
        showDiolog(type);
    }

    @Override
    public void wifiConnectFalid(@NotNull String type, @NotNull String errorMsg) {
        dismissDiolog();
        stopAnim();
        Toasty.normal(this, errorMsg).show();
    }

    @Override
    public void getResult(int code) {

    }

    @Override
    public void getVerifitionResult(int temp) {
        switch (temp) {
            case 0:
                MyToast.showToast(ScanShowActivity.this,getString(R.string.patient_had));
                break;
            case 1:
                //保存完成后，开始创建本地文件夹，将身份证缓存下来
                String idCard = et_pRequiredID.getText().toString().trim();
                mConst.setScreeningId(idCard);
                boolean saveResult = modifyORAdd.addMessage();
                if (saveResult) {
                    isSave = FileUtil.createFileFolder(ScanShowActivity.this, et_pId.getText().toString().trim(), idCard);
                    //创建成功后，开始链结wifi
                    if (isSave) {
                        initConectWIfi();
                    }
                }
                break;
            case 2:
                et_pRequiredHPV.requestFocus();
                MyToast.showToast(ScanShowActivity.this,getString(R.string.hpvhad));
                break;
            case 3:
                et_pRequiredCytology.requestFocus();
                MyToast.showToast(ScanShowActivity.this,getString(R.string.RequiredCytology_had));
                break;
            case 4:
                et_pRequiredGene.requestFocus();
                MyToast.showToast(ScanShowActivity.this,getString(R.string.RequiredGene_had));
                break;
            case 5:
                et_pRequiredDNA.requestFocus();
                MyToast.showToast(ScanShowActivity.this,getString(R.string.Requireddna));
                break;
            case 6:
                et_pRequiredOther.requestFocus();
                MyToast.showToast(ScanShowActivity.this,getString(R.string.Requiredother_had));
                break;
            case 7:
                VerificationUtils.verificationInput(ScanShowActivity.this, et_pPhone, et_pRequiredID);
                break;
            case 8:
                MyToast.showToast(ScanShowActivity.this,getString(R.string.RequiredHPV_null));
                et_pRequiredHPV.requestFocus();
                break;
            case 9:
                MyToast.showToast(ScanShowActivity.this,getString(R.string.RequiredCytology_null));
                et_pRequiredCytology.requestFocus();
                break;
            case 10:
                MyToast.showToast(ScanShowActivity.this,getString(R.string.Requiredgene_null));
                et_pRequiredGene.requestFocus();
                break;
            case 11:
                MyToast.showToast(ScanShowActivity.this,getString(R.string.Requireddna_null));
                et_pRequiredDNA.requestFocus();
                break;
            case 12:
                MyToast.showToast(ScanShowActivity.this,getString(R.string.Requiredother_null));
                et_pRequiredOther.requestFocus();
                break;
            case 13:
                MyToast.showToast(ScanShowActivity.this,getString(R.string.patient_select_name));
                et_pName.requestFocus();
                break;
            case 14:
                et_pRequiredID.requestFocus();
                MyToast.showToast(ScanShowActivity.this,getString(R.string.patient_select_ID));
                break;
            case 15:
                VerificationUtils.getDuplicateChecking(et_pRequiredHPV, et_pRequiredCytology, et_pRequiredGene, et_pRequiredDNA, et_pRequiredOther, et_pRequiredID);
                break;
        }
    }

    private String TAG = "ScanShowActivity";

    private class ShowThread extends Thread {
        private WeakReference<ScanShowActivity> activity;

        public ShowThread(ScanShowActivity activity) {
            this.activity = new WeakReference<ScanShowActivity>(activity);
        }

        public void run() {
            ScanShowActivity act = activity.get();
            if (act != null) {
                Message message = mHandler.obtainMessage();
                listMessages = LitePal.findAll(ListMessage.class);
                if (listMessages.size() > 0) {
                    message.what = 1;
                } else {
                    message.what = 2;
                }
                mHandler.sendMessage(message);
            }
        }
    }


    //连接蓝牙
    private void initConnectBlue() {
        showDiolog(getString(R.string.linkingBlue));

        new Thread(new Runnable() {
            @Override
            public void run() {
                Constss.hsBlueApi.setmInterface(new HsInterface() {
                    @Override
                    public void reslut2Devices(Map<String, List<BluetoothDevice>> map) {

                    }
                });
                Constss.hsBlueApi.scanf();
                int ret = Constss.hsBlueApi.connect(bluetoothUtils.initGetBlue(Constss.Bluetooth_Path));
                if (ret == 0) {
                    Constss.isConnBlue = true;
                    try {
                        Thread.sleep(4000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dismissDiolog();
                                ToastUtils.showToast(ScanShowActivity.this, getString(R.string.BlueConnected));
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } else {
                    Constss.isConnBlue = true;
                }
            }
        }).start();

    }

    private class ShowHandler extends Handler {

        private WeakReference<ScanShowActivity> activity;

        public ShowHandler(ScanShowActivity activity) {
            this.activity = new WeakReference<ScanShowActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ScanShowActivity act = activity.get();
            if (act != null) {

                switch (msg.what) {
                    case 1:
                        et_pId.setText(Integer.parseInt(listMessages.get(listMessages.size() - 1).getpId()) + 1 + "");
                        break;
                    case 2:
                        et_pId.setText("1");
                        break;

                    case 3:
                        SouthUtil.showToast(ScanShowActivity.this, getString(R.string.saveMsgSuccess));
                        break;

                    case 4:
                        SouthUtil.showToast(ScanShowActivity.this, getString(R.string.saveMsgFaild));
                        break;

                    default:
                        break;
                }
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
//        EXOCRDict.InitDict(this);
        if (id == 1) {
            new ShowThread(this).start();
        }
        Log.e("blueLink", Constss.isConnBlue + ",,," + Constss.hsBlueApi);
        String blueName = bluetoothUtils.initGetBlue(Constss.Bluetooth_Path);
        if (blueName == null || blueName == "") {
//            ToastUtils.showToast(this, getString(R.string.setting_blue_title));
        } else {
            if (!Constss.isConnBlue) {
//                Log.e("是否连接", Constss.isFtpTrue + "");
                initConnectBlue();
            }
        }
        BardcodeSettingUtils.getScreenBarcode(1, Constant.hpv_key, et_pRequiredHPV, iv_scanhpv, ScanShowActivity.this);
        BardcodeSettingUtils.getScreenBarcode(2, Constant.cytology_key, et_pRequiredCytology, iv_scanxbx, ScanShowActivity.this);
        BardcodeSettingUtils.getScreenBarcode(3, Constant.gene_key, et_pRequiredGene, iv_scanjyjc, ScanShowActivity.this);
        BardcodeSettingUtils.getScreenBarcode(4, Constant.dna_key, et_pRequiredDNA, iv_scandna, ScanShowActivity.this);
        BardcodeSettingUtils.getScreenBarcode(5, Constant.other_key, et_pRequiredOther, iv_scanother, ScanShowActivity.this);
    }


    private IDCardInfo ic;
    int ret;

    //开始读卡
    private void initReadCard() {
        if (!Constss.isConnBlue) {
            return;
        }

        Constss.hsBlueApi.aut();
        ret = Constss.hsBlueApi.Authenticate(500);
        ic = new IDCardInfo();
        ret = Constss.hsBlueApi.Read_Card(ic, 2000);

        if (ret == 1) {
            if (ic.getcertType() == " ") {
                et_pName.setText(ic.getPeopleName());
                et_pRequiredID.setText(ic.getIDCard());
                int age = getAge(ic.getBirthDay());
                Log.e("birth", ic.getBirthDay().toString() + "////" + age);
                if (age != -1) {
                    et_pAge.setText(age + "");
                } else {
                    SouthUtil.showToast(this, getString(R.string.systemTime));
                }

            }
        }
    }

    /**
     * 计算年龄
     */
    private int getAge(Date birth) {
        Calendar calendar = Calendar.getInstance();
        //如果出生日期大于当前时间，就返回
        if (calendar.before(birth)) {
            return -1;
        }
        int yearSys = calendar.get(Calendar.YEAR);
        calendar.setTime(birth);
        int yearBirth = calendar.get(Calendar.YEAR);
        int age = yearSys - yearBirth;
        return age;
    }

    @Override
    protected void onPause() {
        super.onPause();
        id = -1;
    }

    private void initClick() {
        bt_clear.setOnClickListener(this);
        bt_save.setOnClickListener(this);
        bt_getImage.setOnClickListener(this);
        iv_scanjyjc.setOnClickListener(this);
        iv_scandna.setOnClickListener(this);
        iv_scanhpv.setOnClickListener(this);
        iv_scanxbx.setOnClickListener(this);
        btn_left.setOnClickListener(this);
        iv_scanother.setOnClickListener(this);
//        iv_scanidcard.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_clearM:
                modifyORAdd.againAsk(getString(R.string.clearTitle), getString(R.string.clearMessage));
                break;
            case R.id.btn_left:
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(ClipData.newPlainText(null, ""));
                Intent intent = new Intent(ScanShowActivity.this, MainActivity.class);
                intent.putExtra("canshu", 0);
                startActivity(intent);
                if (timer != null) {
                    timer.cancel();
                }
                break;
            case R.id.bt_getImage:


                if (isSave) {
                    initConectWIfi();
                } else {
                    //先判断必填项是否为空
                    VerificationUtils.requiredBlank(et_pName,et_pRequiredID);
                }

                break;
            case R.id.iv_scanhpv:
                startActivityForResult(new Intent(this, ScanningActivity.class), 1);
                break;
            case R.id.iv_scandna:
                startActivityForResult(new Intent(this, ScanningActivity.class), 4);
                break;
            case R.id.iv_scanxbx:
                startActivityForResult(new Intent(this, ScanningActivity.class), 2);
                break;
            case R.id.iv_scanjyjc:
                startActivityForResult(new Intent(this, ScanningActivity.class), 3);
                break;
            case R.id.bt_save:
                if (Constss.isConnBlue) {
                    initReadCard();
                } else {
                    ToastUtils.showToast(this, "正在连接蓝牙");
                }

                break;
            case R.id.iv_scanother:
                startActivityForResult(new Intent(this, ScanningActivity.class), 5);
                break;
            case R.id.iv_scanidcard:
                Intent scanIntent = new Intent(getApplicationContext(), CaptureActivity.class);
                scanIntent.putExtra(CaptureActivity.INTNET_FRONT, true);
                startActivityForResult(scanIntent, REQUEST_CODE_FRONT);
                break;
            default:
                break;
        }
    }

    private void getScanResult(String result,String id,String size,EditText et_text,int temp){
        if(result.contains(id)){
            if(null != size && !size.equals("")){
                if(Integer.parseInt(size) == result.length()){
                    et_text.setText(result);
                }else {
                    MyToast.showToast(ScanShowActivity.this, getString(R.string.barcode_error));
                    startActivityForResult(new Intent(this, ScanningActivity.class), temp);
                }
            }else {
                et_pRequiredHPV.setText(result);
            }
        }else {
            MyToast.showToast(ScanShowActivity.this, getString(R.string.barcode_error));
            startActivityForResult(new Intent(this, ScanningActivity.class), temp);
        }
    }

    public static final int REQUEST_CODE_FRONT = 1000;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String result = data.getStringExtra("text");
            if (!TextUtils.isEmpty(result)) {
                if (requestCode == 1) {
                    getScanResult(result,Constant.hpv_id,Constant.hpv_size,et_pRequiredHPV,1);

                } else if (requestCode == 2) {
                    getScanResult(result,Constant.cytology_id,Constant.cytology_size,et_pRequiredCytology,2);

                } else if (requestCode == 3) {
                    getScanResult(result,Constant.gene_id,Constant.gene_size,et_pRequiredGene,3);

                } else if (requestCode == 4) {
                    getScanResult(result,Constant.dna_id,Constant.dna_size,et_pRequiredDNA,4);

                } else if (requestCode == 5) {
                    getScanResult(result,Constant.other_id,Constant.other_size,et_pRequiredOther,5);
                }
            }
        }
    }

    @Override
    public void getVerifitionBarcodeResult(int temp) {
        switch (temp) {
            case 1:
                MyToast.showToast(ScanShowActivity.this, getString(R.string.RequiredHPV_error));
//                ToastUtils.showToast(ScanShowActivity.this, getString(R.string.RequiredHPV_error));
                break;
            case 2:
                MyToast.showToast(ScanShowActivity.this, getString(R.string.RequiredCytology_error));
//                ToastUtils.showToast(ScanShowActivity.this, getString(R.string.RequiredCytology_error));
                break;
            case 3:
                MyToast.showToast(ScanShowActivity.this, getString(R.string.Requiredgene_error));
//                ToastUtils.showToast(ScanShowActivity.this, getString(R.string.Requiredgene_error));
                break;
            case 4:
                MyToast.showToast(ScanShowActivity.this, getString(R.string.Requireddna_error));
//                ToastUtils.showToast(ScanShowActivity.this, getString(R.string.Requireddna_error));
                break;
            case 5:
                MyToast.showToast(ScanShowActivity.this, getString(R.string.Requiredother_error));
//                ToastUtils.showToast(ScanShowActivity.this, getString(R.string.Requiredother_error));
                break;
        }
    }

    private void initConectWIfi() {
        showDiolog(getString(R.string.wifi_start_connect_szb));
        startAnim();
        WifiConnectUtil.Companion.getInstance().startConnectWifi(Constss.WIFI_TYPE_SZB);


    }

    //根据条件，判断是否启动WiFi加载动画，或者停止动画
    //    //根据条件，判断是否启动WiFi加载动画，或者停止动画
    private void startAnim() {
        if (wifiLoadingAnim != null) {
            wifiLoadingAnim.setVisibility(View.VISIBLE);
            wifiLoadingAnim.startAnim();
        }
    }

    private void stopAnim() {
        if (wifiLoadingAnim != null) {
            wifiLoadingAnim.stopAnim();
            wifiLoadingAnim.setVisibility(View.GONE);
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        isFront = true;
        isSave = false;
    }


    private void initView() {
        bluetoothUtils = new BluetoothUtils(ScanShowActivity.this);
        VerificationUtils.setGetVerificationResultListener(this);
        mConst = Item.getOneItem(ScanShowActivity.this);
        iv_scandna = findViewById(R.id.iv_scandna);
        iv_scanhpv = findViewById(R.id.iv_scanhpv);
        iv_scanxbx = findViewById(R.id.iv_scanxbx);
        iv_scanjyjc = findViewById(R.id.iv_scanjyjc);
        iv_scanother = findViewById(R.id.iv_scanother);
        et_pRequiredOther = findViewById(R.id.et_pRequiredOther);
        et_pRequiredDNA = findViewById(R.id.et_pRequiredDNA);
        et_pRequiredGene = findViewById(R.id.et_pRequiredGene);
        iv_screen = findViewById(R.id.iv_screen);
        iv_screen.setVisibility(View.GONE);
        bt_add = findViewById(R.id.bt_add);
        bt_clear = findViewById(R.id.bt_clearM);
        bt_save = findViewById(R.id.bt_save);
        bt_save.setText("读卡");
        bt_getImage = findViewById(R.id.bt_getImage);
        et_pId = findViewById(R.id.et_pId);
        et_pName = findViewById(R.id.et_pName);
        et_pAge = findViewById(R.id.et_pAge);
        et_pSex = findViewById(R.id.et_pSex);
        et_pPhone = findViewById(R.id.et_pPhone);
        et_pLocalPhone = findViewById(R.id.et_pLocalPhone);
        et_pDate = findViewById(R.id.et_pDate);
        et_pOccupation = findViewById(R.id.et_pOccupation);
        et_smoking = findViewById(R.id.et_smoking);
        et_pSexualpartners = findViewById(R.id.et_pSexualpartners);
        et_pGestationaltimes = findViewById(R.id.et_pGestationaltimes);
        pAbortion = findViewById(R.id.pAbortion);
        et_pRequiredID = findViewById(R.id.et_pRequiredID);
        et_pRequiredHPV = findViewById(R.id.et_pRequiredHPV);
        et_pRequiredCytology = findViewById(R.id.et_pRequiredCytology);
        et_pDetailedaddress = findViewById(R.id.et_pDetailedaddress);
        et_pRemarks = findViewById(R.id.et_pRemarks);
        sp_marray = findViewById(R.id.sp_marray);
        sp_contraception = findViewById(R.id.sp_contraception);
        title_text = findViewById(R.id.title_text);
        title_text.setText(getString(R.string.patients_information));
        btn_left = findViewById(R.id.btn_left);
        btn_left.setVisibility(View.VISIBLE);
        iv_scanidcard = findViewById(R.id.iv_scanidcard);
        iv_scanidcard.setVisibility(View.GONE);
        btn_left.setText(getString(R.string.patient_return));
        mConst.setMarry(getString(R.string.patient_nothing));
        mConst.setBirthControlMode(getString(R.string.patient_nothing));
        wifiLoadingAnim = (WifiLoadingAnim) findViewById(R.id.wifiLoadingAnim);
        modifyORAdd = new ModifyORAdd(ScanShowActivity.this, et_pId, et_pName, et_pAge, et_pSex, et_pPhone, et_pLocalPhone, et_pDate, et_pOccupation, et_smoking, et_pSexualpartners, et_pGestationaltimes,
                pAbortion, et_pRequiredID, et_pRequiredHPV, et_pRequiredCytology, et_pDetailedaddress, et_pRemarks, marry, birthControlMode, et_pRequiredGene, screenId, et_pRequiredDNA, et_pRequiredOther);

        myDialog = new MyDialog(getApplicationContext());
        myDialog.setOnButtonClickListener(this);
    }

    private void initData() {
        marryDatas = new String[]{getString(R.string.patient_nothing), getString(R.string.patient_married), getString(R.string.patient_unmarried)};
        bhdDatas = new String[]{getString(R.string.patient_nothing), getString(R.string.patient_condom), getString(R.string.patient_acyterion), getString(R.string.patient_Contraceptive_needle),
                getString(R.string.patient_female_condom), getString(R.string.patient_Ligation), getString(R.string.patient_Women__IUD)};
        ArrayAdapter<String> adapterMarry = new ArrayAdapter<String>(ScanShowActivity.this, R.layout.adapter_item, R.id.sp_textview, marryDatas);
        ArrayAdapter<String> adapterSource = new ArrayAdapter<String>(ScanShowActivity.this, R.layout.adapter_item, R.id.sp_textview, bhdDatas);
        sp_marray.setAdapter(adapterMarry);
        sp_contraception.setAdapter(adapterSource);
    }

    private void initTextView() {
        tvName = new String[]{getString(R.string.pId), getString(R.string.pName), getString(R.string.pAge), getString(R.string.pSex), getString(R.string.RequiredPhone),
                getString(R.string.localPhone), getString(R.string.birthData), getString(R.string.Occupation), getString(R.string.smoking), getString(R.string.sexualpartners), getString(R.string.Gestationaltimes), getString(R.string.abortion),
                getString(R.string.RequiredID), getString(R.string.RequiredHPV),
                getString(R.string.RequiredCytology), getString(R.string.RequiredGene), getString(R.string.dna), getString(R.string.other), getString(R.string.Detailedaddress),
                getString(R.string.Remarks), getString(R.string.marry),
                getString(R.string.contraception)};
        tv01 = (TextView) findViewById(R.id.tv_1);
        tv02 = (TextView) findViewById(R.id.tv_2);
        tv03 = (TextView) findViewById(R.id.tv_3);
        tv04 = (TextView) findViewById(R.id.tv_4);
        tv05 = (TextView) findViewById(R.id.tv_5);
        tv06 = (TextView) findViewById(R.id.tv_6);
        tv07 = (TextView) findViewById(R.id.tv_7);
        tv08 = (TextView) findViewById(R.id.tv_8);
        tv09 = (TextView) findViewById(R.id.tv_9);
        tv10 = (TextView) findViewById(R.id.tv_10);
        tv11 = (TextView) findViewById(R.id.tv_11);
        tv12 = (TextView) findViewById(R.id.tv_12);
        tv13 = (TextView) findViewById(R.id.tv_13);
        tv14 = (TextView) findViewById(R.id.tv_14);
        tv15 = (TextView) findViewById(R.id.tv_15);
        tv16 = (TextView) findViewById(R.id.tv_16);
        tv17 = (TextView) findViewById(R.id.tv_17);
        tv18 = (TextView) findViewById(R.id.tv_18);
        tv19 = (TextView) findViewById(R.id.tv_19);
        tv20 = (TextView) findViewById(R.id.tv_20);
        tv21 = findViewById(R.id.tv_21);
        tv22 = findViewById(R.id.tv_22);
        TextView[] tvData = {tv01, tv02, tv03, tv04, tv05, tv06, tv07, tv08, tv09, tv10, tv11, tv12, tv13, tv14, tv15, tv16, tv17, tv18, tv19, tv20, tv21, tv22};
        for (int i = 0; i < tvName.length; i++) {
            if (tvData[i] != null) {
                tvData[i].setText(AlignedTextUtils.justifyString(tvName[i], 5));
            }
        }
    }

    @Override
    public void okButtonClick() {

    }

    /**
     * spinner的点击事件响应
     */
    public void initSelectItemClick() {
        sp_marray.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        mConst.setMarry(getString(R.string.patient_nothing));
                        break;
                    case 1:
                        mConst.setMarry(getString(R.string.patient_married));
                        break;
                    case 2:
                        mConst.setMarry(getString(R.string.patient_unmarried));
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        sp_contraception.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0://病人避孕方式
                        mConst.setBirthControlMode(getString(R.string.patient_nothing));
                        break;
                    case 1://病人避孕方式
                        mConst.setBirthControlMode(getString(R.string.patient_condom));
                        break;
                    case 2://
                        mConst.setBirthControlMode(getString(R.string.patient_acyterion));
                        break;
                    case 3://
                        mConst.setBirthControlMode(getString(R.string.patient_Contraceptive_needle));
                        break;
                    case 4://
                        mConst.setBirthControlMode(getString(R.string.patient_female_condom));
                        break;
                    case 5://
                        mConst.setBirthControlMode(getString(R.string.patient_Ligation));
                        break;
                    case 6://
                        mConst.setBirthControlMode(getString(R.string.patient_Women__IUD));
                        break;
                    default:
                        break;

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }


    private void showDiolog(String msg) {
        if (!isFront) return;
        if (mDialog != null && mDialog.isShow()) {
            mDialog.setMessage(msg);
        } else {
            if (mDialog == null) {
                mDialog = new LoadingDialog(ScanShowActivity.this, true);
            }
            mDialog.setMessage(msg);
            mDialog.dialogShow();
        }
    }

    private void dismissDiolog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isFront = false;
        dismissDiolog();
        stopAnim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dispose();
    }


}
