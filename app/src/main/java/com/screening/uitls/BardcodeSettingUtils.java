package com.screening.uitls;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.activity.R;
import com.screening.activity.BarcodeSettingActivity;
import com.screening.model.BarcodeBean;
import com.screening.ui.MyToast;
import com.suke.widget.SwitchButton;

public class BardcodeSettingUtils {

    /**
     * 设置扫码的状态、标识、位数限制
     */
    public static void setBarcode(Context mContext, String key, String identification, String digit, boolean state) {
        BarcodeBean barcodeBean = new BarcodeBean();
        barcodeBean.setIdentification(identification);
        barcodeBean.setDigit(digit);
        barcodeBean.setState(state);
        SPUtils.putBean(mContext, key, barcodeBean);
    }

    /**
     * 得到某种条形码的对象类，包括标识，字数，是否启用
     */
    public static BarcodeBean getBarcode(Context mContext, String key) {
        BarcodeBean barcodeBean = null;
        if (isBarcode(mContext, key)) {
            barcodeBean = (BarcodeBean) SPUtils.getBean(mContext, key);
        }
        return barcodeBean;
    }

    /**
     * 判断是否已经设置过扫码
     */
    public static boolean isBarcode(Context mContext, String key) {
        return SPUtils.contains(mContext, key);
    }

    /**
     * 设置扫码，设置完成后新添患者时不会再跳转至扫码设置界面
     */
    public static void setBarcodeSetting(Context mContext, String key, boolean setResult) {
        SPUtils.put(mContext, key, setResult);
    }


    /**
     * 得到扫码设置的状态
     *
     * @param mContext
     * @param et_Identification
     * @param et_size
     * @param isState
     * @param key
     * @return
     */
    public static boolean getBarcodeState(Context mContext, EditText et_Identification, EditText et_size, SwitchButton isState, String key) {
        BarcodeBean barcodeBean = BardcodeSettingUtils.getBarcode(mContext, key);
        boolean switchState = false;
        if (barcodeBean != null) {
            et_Identification.setText(barcodeBean.getIdentification());
            et_size.setText(barcodeBean.getDigit() + "");
            if (barcodeBean.isState()) {
                switchState = true;

            } else {
                switchState = false;

            }
            isState.setChecked(switchState);
        } else {
            switchState = false;
        }
        return switchState;
    }

    /**
     * 在筛查信息界面得到所有设置的码的信息,并设置扫描功能是否可用
     */
    public static void getScreenBarcode(int temp, String key, EditText editText, ImageView image, Context mContext) {
        BarcodeBean barcodeBean = null;
        boolean state = false;
        barcodeBean = BardcodeSettingUtils.getBarcode(mContext, key);
        if (barcodeBean != null) {
            state = barcodeBean.isState();
        }
        if (state) {
            editText.setEnabled(true);
            image.setEnabled(true);
            image.setVisibility(View.VISIBLE);
            switch (temp) {
                case 1:
                    Constant.hpv_id = barcodeBean.getIdentification();
                    Constant.hpv_size = barcodeBean.getDigit();
                    break;
                case 2:
                    Constant.cytology_id = barcodeBean.getIdentification();
                    Constant.cytology_size = barcodeBean.getDigit();
                    break;
                case 3:
                    Constant.gene_id = barcodeBean.getIdentification();
                    Constant.gene_size = barcodeBean.getDigit();
                    break;
                case 4:
                    Constant.dna_id = barcodeBean.getIdentification();
                    Constant.dna_size = barcodeBean.getDigit();
                    break;
                case 5:
                    Constant.other_id = barcodeBean.getIdentification();
                    Constant.other_size = barcodeBean.getDigit();
                    break;
            }
        } else {
            editText.setEnabled(false);
            image.setEnabled(false);
            image.setVisibility(View.GONE);
        }
    }


    /**
     *保存扫码设置的信息
     */
    public static boolean setBarcode(EditText et_identification,EditText et_size,SwitchButton sb_button,int temp,Context mContext){
        String identification = et_identification.getText().toString().trim();
        String size = et_size.getText().toString().trim();
        boolean state = sb_button.isChecked();
        if (!size.equals("") && !identification.equals("")) {
            //位数限制不能小于标识码长度
            int identSize = identification.length();
            int num = Integer.parseInt(size);
            switch (temp) {
                case 1:
                    if (num < identSize) {
                        et_size.requestFocus();
                        MyToast.showToast(mContext, mContext.getString(R.string.hpvwordlimit));
                        return false;
                    } else {
                        BardcodeSettingUtils.setBarcode(mContext, Constant.hpv_key, identification, size, state);
                    }
                    break;
                case 2:
                    if (num < identSize) {
                        et_size.requestFocus();
                        MyToast.showToast(mContext, mContext.getString(R.string.tctwordlimit));
                        return false;
                    } else {
                        BardcodeSettingUtils.setBarcode(mContext,Constant.cytology_key,identification,size,state);
                    }
                    break;
                case 3:
                    if (num < identSize) {
                        et_size.requestFocus();
                        MyToast.showToast(mContext, mContext.getString(R.string.genewordlimit));
                        return false;
                    } else {
                        BardcodeSettingUtils.setBarcode(mContext,Constant.gene_key,identification,size,state);
                    }
                    break;
                case 4:
                    if (num < identSize) {
                        et_size.requestFocus();
                        MyToast.showToast(mContext, mContext.getString(R.string.dnawordlimit));
                        return false;
                    } else {
                        BardcodeSettingUtils.setBarcode(mContext,Constant.dna_key,identification,size,state);
                    }
                    break;
                case 5:
                    if (num < identSize) {
                        et_size.requestFocus();
                        MyToast.showToast(mContext, mContext.getString(R.string.otherwordlimit));
                        return false;
                    } else {
                        BardcodeSettingUtils.setBarcode(mContext,Constant.other_key,identification,size,state);
                    }
                    break;
            }
        }else {
            switch (temp) {
                case 1:
                    BardcodeSettingUtils.setBarcode(mContext, Constant.hpv_key, identification, size, state);
                    break;
                case 2:
                    BardcodeSettingUtils.setBarcode(mContext,Constant.cytology_key,identification,size,state);
                    break;
                case 3:
                    BardcodeSettingUtils.setBarcode(mContext,Constant.gene_key,identification,size,state);
                    break;
                case 4:
                    BardcodeSettingUtils.setBarcode(mContext,Constant.dna_key,identification,size,state);
                    break;
                case 5:
                    BardcodeSettingUtils.setBarcode(mContext,Constant.other_key,identification,size,state);
                    break;
            }
        }
        return true;
    }
}
