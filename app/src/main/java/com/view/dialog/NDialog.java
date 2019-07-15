package com.view.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.activity.R;
import com.util.SouthUtil;

/**
 * Created by necer on 2018/12/17.
 */
public abstract class NDialog {

    protected Context mContext;

    protected float dialogCornersRadius;//弹窗圆角大小
    protected int dialogBgColor = -1;//弹窗颜色
    protected int dialogWidth;
    private int dialogHeight;
    protected int dialogGravity;
    private float dimAmount;//弹出时背景的灰度
    protected int screenWith;
    private int windowAnimation;
    private boolean cancleable = true;
    private boolean canceledOnTouchOutside = true;
    protected boolean isFromBottom;


    protected String positiveButtonText;
    protected String negativeButtonText;

    protected int positiveButtonColor;
    protected int negativeButtonColor;

    protected float positiveButtonSize;
    protected float negativeButtonSize;

    protected String message;
    protected String title;

    protected DialogInterface.OnClickListener positiveOnClickListener;
    protected DialogInterface.OnClickListener negativeOnClickListener;

    public NDialog(Context context) {
        this.mContext = context;

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        screenWith = dm.widthPixels;
        dimAmount = 0.5f;
        dialogWidth = screenWith * 5 / 7;
        dialogGravity = Gravity.CENTER;
        dialogCornersRadius = 3f;
    }

    public AlertDialog create() {

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeButtonText, negativeOnClickListener)
                .setPositiveButton(positiveButtonText, positiveOnClickListener);
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AlertDialog dialogg = (AlertDialog) dialog;
                setDialogDetails(mContext, dialogg);
                dialogg.setCanceledOnTouchOutside(canceledOnTouchOutside);
                dialogg.setCancelable(cancleable);
                setDialogWindow(dialogg);
            }
        });
        return alertDialog;
    }

    protected abstract void setDialogDetails(Context context, AlertDialog alertDialog);

    private void setDialogWindow(AlertDialog alertDialog) {

        Window window = alertDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        window.setDimAmount(dimAmount);
        window.setGravity(dialogGravity);

        if (dialogGravity != Gravity.CENTER) {
            lp.y = (screenWith - dialogWidth) / 2;
        }

        window.setLayout(dialogWidth, dialogHeight == 0 ? LinearLayout.LayoutParams.WRAP_CONTENT : dialogHeight);
//        window.setBackgroundDrawable(getGradientDrawable(dialogBgColor == -1 ? Color.WHITE : dialogBgColor));

        if (windowAnimation != 0) {
            window.setWindowAnimations(windowAnimation);
        } else if (isFromBottom) {
            window.setWindowAnimations(R.style.dialog_anim_style);
        }

    }

    protected GradientDrawable getGradientDrawable(int color) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(color);
        float cornersRadius = SouthUtil.dp2px(dialogCornersRadius);
        gradientDrawable.setCornerRadii(new float[]{cornersRadius, cornersRadius, cornersRadius, cornersRadius, cornersRadius, cornersRadius, cornersRadius, cornersRadius});
        return gradientDrawable;
    }

    //dialog宽度，建议依屏幕的宽为参考
    public NDialog setDialogWidth(int dialogWidthPx) {
        this.dialogWidth = dialogWidthPx;
        return this;
    }

    //dialog高度，建议依屏幕的高为参考
    public NDialog setDialogHeight(int dialogHeightPx) {
        this.dialogHeight = dialogHeightPx;
        return this;
    }

    //dialog是否可取消
    public NDialog setCancelable(boolean cancleable) {
        this.cancleable = cancleable;
        return this;
    }

    //点击dialog外部是否可取消
    public NDialog setCanceledOnTouchOutside(boolean canceledOnTouchOutside) {
        this.canceledOnTouchOutside = canceledOnTouchOutside;
        return this;
    }

    //dialog是否底部弹出
    public NDialog setIsFromBottom(boolean isFromBottom) {
        this.isFromBottom = isFromBottom;
        return this;
    }

    //dialog弹出动画
    public NDialog setWindowAnimation(int windowAnimation) {
        this.windowAnimation = windowAnimation;
        return this;
    }

    //dialog弹出位置
    public NDialog setDialogGravity(int gravity) {
        this.dialogGravity = gravity;
        return this;
    }

    //dialog的背景颜色
    public NDialog setDialogBgColor(int dialogBgColor) {
        this.dialogBgColor = dialogBgColor;
        return this;
    }

    //dialog弹出时背景的灰度 0.0f-1.0f 0.0f为透明 1.0f为全黑
    public NDialog setDimAmount(float dimAmount) {
        this.dimAmount = dimAmount;
        return this;
    }

    //dialog的圆角
    public NDialog setDialogCornersRadius(float dialogCornersRadiusDp) {
        this.dialogCornersRadius = dialogCornersRadiusDp;
        return this;
    }

}
