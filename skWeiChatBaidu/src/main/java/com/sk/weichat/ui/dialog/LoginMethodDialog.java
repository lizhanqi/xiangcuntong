package com.sk.weichat.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.sk.weichat.R;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.LinkTouchMovementMethod;
import com.sk.weichat.view.MySpannableTextView;

/**
 * 登录方式选择底部弹窗
 */
public class LoginMethodDialog extends Dialog implements View.OnClickListener {
    private Context mContext;
    private OnLoginMethodListener mListener;
    private CheckBox mCbPrivacy;

    public LoginMethodDialog(Context context) {
        super(context, R.style.BottomDialog);
        this.mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置弹窗布局
        View contentView = LayoutInflater.from(mContext).inflate(R.layout.dialog_login_method, null);
        setContentView(contentView);

        // 设置弹窗宽度为屏幕宽度
        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        layoutParams.width = mContext.getResources().getDisplayMetrics().widthPixels;
        contentView.setLayoutParams(layoutParams);

        // 设置弹窗位置在底部
        Window window = getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(R.style.BottomDialog_Animation);

        // 初始化控件
        ImageView ivClose = findViewById(R.id.iv_close);
        LinearLayout llPhoneLogin = findViewById(R.id.ll_phone_login);
        mCbPrivacy = findViewById(R.id.cb_privacy);
        MySpannableTextView tvPrivacy = findViewById(R.id.tv_privacy);

        // 设置点击事件
        ivClose.setOnClickListener(this);
        llPhoneLogin.setOnClickListener(this);



        LinkTouchMovementMethod linkTouchMovementMethod = new LinkTouchMovementMethod();
        tvPrivacy.setLinkTouchMovementMethod(linkTouchMovementMethod);
        tvPrivacy.setMovementMethod(linkTouchMovementMethod);
        tvPrivacy.setText(getRenderText(new OnLoginAgreementListener() {
            @Override
            public void onUserAgreementClick() {
                ToastUtil.showToast(mContext,"111");
            }

            @Override
            public void onPrivacyAgreementClick() {
                ToastUtil.showToast(mContext,"22");
            }

            @Override
            public void onMinorProtectClick() {
                ToastUtil.showToast(mContext,"333");
            }
        }));

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_close) {
            dismiss();
        } else if (id == R.id.ll_phone_login) {
            if (!mCbPrivacy.isChecked()) {
                Toast.makeText(mContext, "请先阅读并同意隐私政策", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mListener != null) {
                mListener.onPhoneLoginClick();
            }
            dismiss();
        }
    }

    /**
     * 设置登录方式选择监听器
     */
    public void setOnLoginMethodListener(OnLoginMethodListener listener) {
        this.mListener = listener;
    }

    /**
     * 登录方式选择监听器接口
     */
    public interface OnLoginMethodListener {
        /**
         * 手机登录点击事件
         */
        void onPhoneLoginClick();
    }

    private CharSequence getRenderText(final OnLoginAgreementListener onLoginAgreementListener) {
        String content = "我已阅读并同意";
        String name1 = "《隐私政策》";
        String name2 = "《服务协议》";
        String name3 = "《未成年人个人信息保护规则》";
        SpannableStringBuilder builder;
        builder = new SpannableStringBuilder(content + name1 + name2 + name3);

        // 设置《隐私政策》的前景色和点击事件
        builder.setSpan(new ForegroundColorSpan(getContext().getResources().getColor(R.color.theme_color1)),
                content.length(), content.length() + name1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {

                if (onLoginAgreementListener != null) {
                    onLoginAgreementListener.onPrivacyAgreementClick();
                }
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setUnderlineText(false);
            }
        }, content.length(), content.length() + name1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 设置《服务协议》的前景色和点击事件
        int start2 = content.length() + name1.length();
        builder.setSpan(new ForegroundColorSpan(getContext().getResources().getColor(R.color.textColorDefault)),
                start2, start2 + name2.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {

                if (onLoginAgreementListener != null) {
                    onLoginAgreementListener.onUserAgreementClick();
                }
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setUnderlineText(false);
            }
        }, start2, start2 + name2.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 设置《未成年人个人信息保护规则》的前景色和点击事件
        int start3 = start2 + name2.length();
        builder.setSpan(new ForegroundColorSpan(getContext().getResources().getColor(R.color.textColorDefault)),
                start3, start3 + name3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                if (onLoginAgreementListener != null) {
                    onLoginAgreementListener.onMinorProtectClick();
                }
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setUnderlineText(false);
            }
        }, start3, start3 + name3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return builder;
    }


    public interface OnLoginAgreementListener {
        void onUserAgreementClick();
        void onPrivacyAgreementClick();
        void onMinorProtectClick();
    }

}