package com.screening.uitls;

import android.content.Context;
import android.util.Log;
import android.widget.EditText;

import com.activity.R;
import com.jakewharton.rxbinding3.widget.RxTextView;
import com.screening.model.FTPBean;
import com.screening.model.ListMessage;
import com.screening.ui.MyToast;
import com.util.ToastUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import es.dmoral.toasty.Toasty;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;


/**
 * 此工具类是用来验证信息，包括医生，身份证，手机号
 */
public class VerificationUtils {

    private String TAG_ = "verification";

    /**
     * 验证医生是否存在
     */

    public void getVerification(String docId) {
        if ("".equals(docId)) {
            if (getVerificationResult != null) {
                getVerificationResult.getResult(-2);
            }
            return;
        }

        Log.i("TAG_12", "getVerification: ");
        final String basic = Credentials.basic(Constant.MD5_USERNAME, Constant.MD5_PASSWORD);
        OkHttpClient client = new OkHttpClient.Builder()
                .authenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {
                        return response.request().newBuilder().header("Authorization", basic).build();
                    }
                }).build();
        Request request = new Request.Builder().url("https://wechat.flybiotech.cn/lfy/api/lfys-hospital-doctors/" + docId).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG_ + "failure", e.getMessage().toString());
                if (getVerificationResult != null) {
                    getVerificationResult.getResult(-3);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //{"data":{"ftpUsername":"v2hTLf","ftpPassword":"JTc47VCTtuXR"},"code":1,"message":"操作成功"}
                final String result = response.body().string();

                int code = jsonFTP(result);
                Log.e(TAG_ + "success", result);
                if (getVerificationResult != null) {
                    getVerificationResult.getResult(code);
                }
            }
        });
    }

    /**
     * 解析json，得到FTP账号密码
     */
    private int jsonFTP(String result) {
        int code = 0;
        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONObject object = jsonObject.getJSONObject("data");
            String ftpUsername = object.getString("ftpUsername");
            String ftpPassword = object.getString("ftpPassword");
            code = jsonObject.getInt("code");
            String message = jsonObject.getString("message");
            //v2hTLf,,,JTc47VCTtuXR
            Log.e(TAG_ + "ftp", ftpUsername + ",,," + ftpPassword);
            if (code == 1) {
                List<FTPBean> ftpBeans = LitePal.findAll(FTPBean.class);
                if (ftpBeans.size() == 0) {
                    FTPBean ftpBean = new FTPBean();
                    ftpBean.setFTPName(ftpUsername);
                    ftpBean.setFTPPassword(ftpPassword);
                    ftpBean.save();
                } else {
                    ftpBeans.get(0).setFTPName(ftpUsername);
                    ftpBeans.get(0).setFTPPassword(ftpPassword);
                    ftpBeans.get(0).save();
                }
            }
            return code;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return code;
    }


    static String REGEX_MOBILE = "^1([38][0-9]|4[579]|5[0-3,5-9]|6[6]|7[0135678]|9[89])\\d{8}$";

    /**
     * 验证是否为手机号
     */
    public static boolean isMobile(String mobile) {
        return Pattern.matches(REGEX_MOBILE, mobile);
    }

    /**
     * 验证是否为身份证
     */
    public static boolean isIDCard(String idCard) {
        String regx = "[0-9]{17}X";
        String regx1 = "[0-9]{17}x";
        String reg1 = "[0-9]{15}";
        String regex = "[0-9]{18}";
        return idCard.matches(regx) || idCard.matches(reg1) || idCard.matches(regex)|| idCard.matches(regx1);
    }

    /**
     * 判断必填项是否为空
     */
    public static void requiredBlank(EditText et_name,EditText et_idCard){
        String name = et_name.getText().toString().trim();
        String idCard = et_idCard.getText().toString().trim();
        if(name.equals("")){
            if(getVerificationResult != null){
                getVerificationResult.getVerifitionResult(13);
            }
            return;
        }
        if(idCard.equals("")){
            if(getVerificationResult != null){
                getVerificationResult.getVerifitionResult(14);
            }
            return;
        }
        if(!name.equals("") && !idCard.equals("")){
            if(getVerificationResult != null){
                getVerificationResult.getVerifitionResult(15);
            }
            return;
        }
    }


    /**
     * 验证输入的格式是否符合规则，包括身份证和电话
     */
    public static void verificationInput(Context mContext, EditText et_pPhone, EditText et_pRequiredID) {
        String phone = et_pPhone.getText().toString().trim();
        String idCard = et_pRequiredID.getText().toString().trim();
        //1.身份证不能为空
        if (idCard.equals("")) {
            MyToast.showToast(mContext,mContext.getString(R.string.patient_id_null));
            return;
        }
        //2.先判断身份证是否已经被使用，如果已使用，则弹出提示
        if (SearchMessage.getIdCard(idCard)) {//判断数据库中是否已存在该身份证号
            if (getVerificationResult != null) {
                getVerificationResult.getVerifitionResult(0);
            }
            return;
        }
        //3.身份证是否符合规则
        if (VerificationUtils.isIDCard(idCard)) {
            //再判断手机号是否为空，手机号可以为空，不为空时需要判断手机号是否符合规则
            if (phone.equals("")) {
                //保存完成后，开始创建本地文件夹，将身份证缓存下来
                if (getVerificationResult != null) {
                    getVerificationResult.getVerifitionResult(1);
                }
            } else {
                if (!phone.equals("") && isMobile(phone)) {
                    //保存完成后，开始创建本地文件夹，将身份证缓存下来
                    if (getVerificationResult != null) {
                        getVerificationResult.getVerifitionResult(1);
                    }
                } else {
                    MyToast.showToast(mContext,mContext.getString(R.string.case_phone_error));
                    return;
                }
            }

        } else {
            MyToast.showToast(mContext,mContext.getString(R.string.patient_id_error));
        }
    }

    /**
     * 保存时验证方法，包括为空验证
     */

    private static void getSaveVerifition(String content,EditText et_input,String id,String size,int temp){

    }

    /**
     * 验证hpv、细胞学、gene、dna、other是否已被使用并检测是否正确
     */
    public static void  getDuplicateChecking(EditText et_hpv, EditText et_cytology, EditText et_gene, EditText et_dna, EditText et_other, EditText et_idCard) {
        boolean checkResult = false;
        String hpv = et_hpv.getText().toString().trim();
        String cytology = et_cytology.getText().toString().trim();
        String gene = et_gene.getText().toString().trim();
        String dna = et_dna.getText().toString().trim();
        String other = et_other.getText().toString().trim();
        String idCard = et_idCard.getText().toString().trim();
        if (!hpv.equals("") && et_hpv.isEnabled()) {
            if(!Constant.hpv_size .equals("")){
                if (!hpv.contains(Constant.hpv_id) || !String.valueOf(hpv.length()).equals(Constant.hpv_size)) {
                    if (getVerificationResult != null) {
                        getVerificationResult.getVerifitionBarcodeResult(1);
                    }
                    return;
                }
            }else {
                if (!hpv.contains(Constant.hpv_id)) {
                    if (getVerificationResult != null) {
                        getVerificationResult.getVerifitionBarcodeResult(1);
                    }
                    return;
                }
            }
            checkResult = getChecking(1, hpv, idCard);
            //hpv已使用
            if (checkResult) {
                if (getVerificationResult != null) {
                    getVerificationResult.getVerifitionResult(2);
                }
                return;
            }
        }else if(hpv.equals("") && et_hpv.isEnabled()){
            if (getVerificationResult != null) {
                getVerificationResult.getVerifitionResult(8);
            }
            return;
        }


        if (!cytology.equals("") && et_cytology.isEnabled()) {
            if(!Constant.cytology_size.equals("")){
                if (!cytology.contains(Constant.cytology_id) || !String.valueOf(cytology.length()).equals(Constant.cytology_size)) {
                    if (getVerificationResult != null) {
                        getVerificationResult.getVerifitionBarcodeResult(2);
                    }
                    return;
                }
            }else {
                if (!cytology.contains(Constant.cytology_id)) {
                    if (getVerificationResult != null) {
                        getVerificationResult.getVerifitionBarcodeResult(2);
                    }
                    return;
                }
            }
            checkResult = getChecking(2, cytology, idCard);
            //细胞学已使用
            if (checkResult) {
                if (getVerificationResult != null) {
                    getVerificationResult.getVerifitionResult(3);
                }
                return;
            }
        }else if(cytology.equals("") && et_cytology.isEnabled()){
            if (getVerificationResult != null) {
                getVerificationResult.getVerifitionResult(9);
            }
            return;
        }


        if (!gene.equals("") && et_gene.isEnabled()) {
            if(!Constant.gene_size.equals("")){
                if (!gene.contains(Constant.gene_id) || !String.valueOf(gene.length()).equals(Constant.gene_size)) {
                    if (getVerificationResult != null) {
                        getVerificationResult.getVerifitionBarcodeResult(3);
                    }
                    return;
                }
            }else {
                if (!gene.contains(Constant.gene_id)) {
                    if (getVerificationResult != null) {
                        getVerificationResult.getVerifitionBarcodeResult(3);
                    }
                    return;
                }
            }
            checkResult = getChecking(3, gene, idCard);
            //基因已使用
            if (checkResult) {
                if (getVerificationResult != null) {
                    getVerificationResult.getVerifitionResult(4);
                }
                return;
            }
        }else if(gene.equals("") && et_gene.isEnabled()){
            if (getVerificationResult != null) {
                getVerificationResult.getVerifitionResult(10);
            }
            return;
        }


        if (!dna.equals("") && et_dna.isEnabled()) {
            if(!Constant.dna_size.equals("")){
                if (!dna.contains(Constant.dna_id) || !String.valueOf(dna.length()).equals(Constant.dna_size)) {
                    if (getVerificationResult != null) {
                        getVerificationResult.getVerifitionBarcodeResult(4);
                    }
                    return;
                }
            }else {
                if (!dna.contains(Constant.dna_id)) {
                    if (getVerificationResult != null) {
                        getVerificationResult.getVerifitionBarcodeResult(4);
                    }
                    return;
                }
            }

            checkResult = getChecking(4, dna, idCard);
            //dna已使用
            if (checkResult) {
                if (getVerificationResult != null) {
                    getVerificationResult.getVerifitionResult(5);
                }
                return;
            }
        }else if(dna.equals("") && et_dna.isEnabled()){
            if (getVerificationResult != null) {
                getVerificationResult.getVerifitionResult(11);
            }
            return;
        }



        if (!other.equals("") && et_other.isEnabled()) {
            if(!Constant.other_size.equals("")){
                if (!other.contains(Constant.other_id) || !String.valueOf(other.length()).equals(Constant.other_size)) {
                    if (getVerificationResult != null) {
                        getVerificationResult.getVerifitionBarcodeResult(5);
                    }
                    return;
                }
            }else {
                if (!other.contains(Constant.other_id)) {
                    if (getVerificationResult != null) {
                        getVerificationResult.getVerifitionBarcodeResult(5);
                    }
                    return;
                }
            }
            checkResult = getChecking(5, other, idCard);
            //其他已使用
            if (checkResult) {
                if (getVerificationResult != null) {
                    getVerificationResult.getVerifitionResult(6);
                }
                return;
            }
        }else if(other.equals("") && et_other.isEnabled()){
            if (getVerificationResult != null) {
                getVerificationResult.getVerifitionResult(12);
            }
            return;
        }

        if (getVerificationResult != null) {
            getVerificationResult.getVerifitionResult(7);
        }

    }


    private static boolean getChecking(int temp, String mag, String idCard) {
        List<ListMessage> listMessages = null;
        switch (temp) {
            case 1:
                listMessages = LitePal.where("HPV = ? and IDCard != ?", mag, idCard).find(ListMessage.class);
                break;
            case 2:
                listMessages = LitePal.where("cytology = ? and IDCard != ?", mag, idCard).find(ListMessage.class);
                break;
            case 3:
                listMessages = LitePal.where("gene = ? and IDCard != ?", mag, idCard).find(ListMessage.class);
                break;
            case 4:
                listMessages = LitePal.where("DNA = ? and IDCard != ?", mag, idCard).find(ListMessage.class);
                break;
            case 5:
                listMessages = LitePal.where("other = ? and IDCard != ?", mag, idCard).find(ListMessage.class);
                break;
        }
        if (listMessages.size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * 计算年龄,手动输入身份证
     */
    public static String getAge(String idCard) {
        String birthyear = "";
        int birthyearcast = 0;
        Calendar calendar = Calendar.getInstance();
        int yearSys = calendar.get(Calendar.YEAR);
        if (idCard.length() == 18) {
            birthyear = idCard.substring(6, 10);
            Log.e("ver", birthyear);
            birthyearcast = Integer.parseInt(birthyear);

        } else if (idCard.length() == 15) {
            birthyear = idCard.substring(7, 9);
            birthyear = "19" + birthyear;
            Log.e("ver1", birthyear);
            birthyearcast = Integer.parseInt(birthyear);
        }
        //判断输入的身份证年龄是否处于合理范围
        int age = yearSys - birthyearcast;
        if (age >= 20 && age <= 70) {
            return age + "";
        } else {
            return "0";
        }
    }

    private static String TAG = "ScanShowActivity";
    /**
     * 实时监测输入框的变化
     */
    public static void getChange(EditText et_hpv, EditText et_tct, EditText et_gene, EditText et_dna, EditText et_other,EditText et_idCard){
        RxTextView.textChanges(et_hpv)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<CharSequence>() {
                    @Override
                    public void accept(CharSequence charSequence) throws Exception {
                        Log.e(TAG+"_hpv_Verification","111111111");
                        verifaicationBarcode(et_hpv,et_idCard,1,0);
                    }
                });
        RxTextView.textChanges(et_tct)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<CharSequence>() {
                    @Override
                    public void accept(CharSequence charSequence) throws Exception {
                        verifaicationBarcode(et_tct,et_idCard,2,0);
                    }
                });
        RxTextView.textChanges(et_gene)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<CharSequence>() {
                    @Override
                    public void accept(CharSequence charSequence) throws Exception {
                        verifaicationBarcode(et_gene,et_idCard,3,0);
                    }
                });
        RxTextView.textChanges(et_dna)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<CharSequence>() {
                    @Override
                    public void accept(CharSequence charSequence) throws Exception {
                        verifaicationBarcode(et_dna,et_idCard,4,0);
                    }
                });
        RxTextView.textChanges(et_other)
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<CharSequence>() {
                    @Override
                    public void accept(CharSequence charSequence) throws Exception {
                        verifaicationBarcode(et_other,et_idCard,5,0);
                    }
                });
    }


    /**
     *判断码是否已使用和是否正确的通用方法
     */
    private static void currencyVerification(String msg,String id,String size,int type){
        if(size.equals("")){
            if(!msg.contains(id)){
                if(getVerificationResult != null){
                    getVerificationResult.getVerifitionBarcodeResult(type);
                }
                return;
            }
        }else {
            if(!msg.contains(id) || !String.valueOf(msg.length()).equals(size)){
                if (getVerificationResult != null) {
                    getVerificationResult.getVerifitionBarcodeResult(type);
                }
                return;
            }
        }
    }





    /**
     * 判断该码是否已被使用
     * style表示码的类型；temp表示是否进行判空
     */
    private static void verifaicationBarcode(EditText et_input,EditText et_idCard,int style,int temp){
        String msg = et_input.getText().toString().trim();
        String idCard = et_idCard.getText().toString().trim();
        boolean checkResult = false;
        switch (style){
            case 1://hpv
                if (!msg.equals("") && et_input.isEnabled()) {
                    currencyVerification(msg,Constant.hpv_id,Constant.hpv_size,1);
                    checkResult = getChecking(1, msg, idCard);
                    //hpv已使用
                    if (checkResult) {
                        if (getVerificationResult != null) {
                            getVerificationResult.getVerifitionResult(2);
                        }
                        return;
                    }
                }
                if(temp == 1){
                    if(msg.equals("") && et_input.isEnabled()){
                        if (getVerificationResult != null) {
                            getVerificationResult.getVerifitionResult(8);
                        }
                        return;
                    }
                }
                break;
            case 2:
                if (!msg.equals("") && et_input.isEnabled()) {
                    currencyVerification(msg,Constant.cytology_id,Constant.cytology_size,2);
                    checkResult = getChecking(2, msg, idCard);
                    //细胞学已使用
                    if (checkResult) {
                        if (getVerificationResult != null) {
                            getVerificationResult.getVerifitionResult(3);
                        }
                        return;
                    }
                }
                if(temp == 1){
                    if(msg.equals("") && et_input.isEnabled()){
                        if (getVerificationResult != null) {
                            getVerificationResult.getVerifitionResult(9);
                        }
                        return;
                    }
                }

                break;
            case 3:
                if (!msg.equals("") && et_input.isEnabled()) {
                    currencyVerification(msg,Constant.gene_id,Constant.gene_size,3);
                    checkResult = getChecking(3, msg, idCard);
                    //细胞学已使用
                    if (checkResult) {
                        if (getVerificationResult != null) {
                            getVerificationResult.getVerifitionResult(4);
                        }
                        return;
                    }
                }
                if(temp == 1){
                    if(msg.equals("") && et_input.isEnabled()){
                        if (getVerificationResult != null) {
                            getVerificationResult.getVerifitionResult(10);
                        }
                        return;
                    }
                }

                break;
            case 4:
                if (!msg.equals("") && et_input.isEnabled()) {
                    currencyVerification(msg,Constant.dna_id,Constant.dna_size,4);
                    checkResult = getChecking(4, msg, idCard);
                    //细胞学已使用
                    if (checkResult) {
                        if (getVerificationResult != null) {
                            getVerificationResult.getVerifitionResult(5);
                        }
                        return;
                    }
                }
                if(temp == 1){
                    if(msg.equals("") && et_input.isEnabled()){
                        if (getVerificationResult != null) {
                            getVerificationResult.getVerifitionResult(11);
                        }
                        return;
                    }
                }

                break;
            case 5:
                if (!msg.equals("") && et_input.isEnabled()) {
                    currencyVerification(msg,Constant.other_id,Constant.other_size,5);
                    checkResult = getChecking(5, msg, idCard);
                    if (checkResult) {
                        if (getVerificationResult != null) {
                            getVerificationResult.getVerifitionResult(6);
                        }
                        return;
                    }
                }
                if(temp == 1){
                    if(msg.equals("") && et_input.isEnabled()){
                        if (getVerificationResult != null) {
                            getVerificationResult.getVerifitionResult(12);
                        }
                        return;
                    }
                }

                break;
        }
    }


    public interface VerificationResult {
        void getResult(int code);

        void getVerifitionResult(int temp);//0代表身份证已用，1代表信息保存完成，开始连接wifi，2代表hpv,3代表细胞学，4代表基因，5代表dna,6代表其他,7代表所有码均是第一次使用,8代表hpv为空,9代表tct为空，10代表基因为空，11代表dna，12代表其他码为空,13代表姓名为空，14代表身份证为空，15代表必填项都不为空

        void getVerifitionBarcodeResult(int temp);//这是用来检测手动输入的码是否正确，只有错误时会调用此接口，1代表hpv,2代表细胞学，3代表基因，4代表dna吗，5代表其他
    }

    static VerificationResult getVerificationResult;

    public static void setGetVerificationResultListener(VerificationResult verificationResultListener) {
        getVerificationResult = verificationResultListener;
    }

    public static void setGetVerificationResultNull() {
        getVerificationResult = null;
    }
}
