package com.sk.weichat.ui.me;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.kuxin.im.wxapi.EventUpdateBandAccount;
import com.kuxin.im.wxapi.WXEntryActivity;
import com.sk.weichat.R;
import com.sk.weichat.bean.BindInfo;
import com.sk.weichat.bean.event.EventUpdateBandQqAccount;
import com.sk.weichat.bean.event.EventUpdateBandTelephoneAccount;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.QQHelper;
import com.sk.weichat.ui.account.LoginActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.AppUtils;
import com.sk.weichat.util.EventBusHelper;
import com.sk.weichat.view.SelectionFrame;
import com.tencent.tauth.Tencent;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.List;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

/**
 * 绑定账号
 */
public class BandAccountActivity extends BaseActivity implements View.OnClickListener {
    private TextView tvBindTelephone;
    private TextView tvBindWx;
    private TextView tvBindQq;
    private boolean isBindTel;
    private boolean isBindWx;
    private boolean isBindQq;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_band_account);
        initActionBar();
        initView();
        getBindInfo();
        EventBusHelper.register(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.bind_account_set));
    }

    private void initView() {
        findViewById(R.id.telephone_bind_rl).setVisibility(coreManager.getConfig().isNoRegisterThirdLogin
                ? View.VISIBLE : View.GONE);
        isBindTel = coreManager.isBindTelephone();

        tvBindTelephone = findViewById(R.id.tv_bind_telephone);
        tvBindWx = findViewById(R.id.tv_bind_wx);
        tvBindQq = findViewById(R.id.tv_bind_qq);
        findViewById(R.id.telephone_bind_rl).setOnClickListener(this);
        findViewById(R.id.wx_bind_rl).setOnClickListener(this);
        if (QQHelper.ENABLE) {
            findViewById(R.id.qq_bind_rl).setOnClickListener(this);
        } else {
            findViewById(R.id.qq_bind_rl).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.telephone_bind_rl:
                BandTelephoneActivity.start(mContext);
                break;
            case R.id.wx_bind_rl:
                showSelectDialog(LoginActivity.THIRD_TYPE_WECHAT, isBindWx, getString(R.string.wechat));
                break;
            case R.id.qq_bind_rl:
                showSelectDialog(LoginActivity.THIRD_TYPE_QQ, isBindQq, getString(R.string.qq));
                break;
        }
    }

    /**
     * 获取用户的绑定状态
     */
    private void getBindInfo() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        HttpUtils.get().url(coreManager.getConfig().USER_GET_BAND_ACCOUNT)
                .params("access_token", coreManager.getSelfStatus().accessToken)
                .build()
                .execute(new ListCallback<BindInfo>(BindInfo.class) {

                    @Override
                    public void onResponse(ArrayResult<BindInfo> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            List<BindInfo> data = result.getData();
                            for (BindInfo info : data) {
                                if (Integer.parseInt(LoginActivity.THIRD_TYPE_WECHAT) == info.getType()) {
                                    isBindWx = true;
                                } else if (Integer.parseInt(LoginActivity.THIRD_TYPE_QQ) == info.getType()) {
                                    isBindQq = true;
                                }
                            }
                            bindStatusRefresh();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        bindStatusRefresh();
                    }
                });
    }

    /**
     * 解绑
     *
     * @param type
     */
    private void unBindInfo(String type) {
        if (!isBindTel) {
            // 如未绑定手机号，解绑时需注意，账号必须关联一个绑定
            if ((TextUtils.equals(type, LoginActivity.THIRD_TYPE_WECHAT) && !isBindQq)
                    || (TextUtils.equals(type, LoginActivity.THIRD_TYPE_QQ) && !isBindWx)) {
                DialogHelper.tipDialog(mContext, getString(R.string.account_must_band_one));
                return;
            }
        }
        DialogHelper.showDefaulteMessageProgressDialog(this);
        HttpUtils.get().url(coreManager.getConfig().USER_UN_BAND_ACCOUNT)
                .params("access_token", coreManager.getSelfStatus().accessToken)
                .params("type", type)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (Result.checkSuccess(mContext, result)) {
                            if (TextUtils.equals(LoginActivity.THIRD_TYPE_WECHAT, type)) {
                                DialogHelper.dismissProgressDialog();
                                isBindWx = false;
                                bindStatusRefresh();
                            } else if (TextUtils.equals(LoginActivity.THIRD_TYPE_QQ, type)) {
                                DialogHelper.dismissProgressDialog();
                                isBindQq = false;
                                bindStatusRefresh();
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        bindStatusRefresh();
                    }
                });
    }

    private void bindStatusRefresh() {
        tvBindTelephone.setText(isBindTel
                ? coreManager.getSelf().getTelephoneNoAreaCode() : getString(R.string.no_band));
        tvBindWx.setText(getString(isBindWx
                ? R.string.banded : R.string.no_band));
        tvBindQq.setText(getString(isBindQq
                ? R.string.banded : R.string.no_band));
    }

    private void showSelectDialog(String type, boolean isBind, String name) {
        String content = isBind ? getResources().getString(R.string.tip_bind_third_place_holder, name) : getResources().getString(R.string.tip_unbind_third_place_holder, name);
        String buttonText = isBind ? getResources().getString(R.string.dialog_Relieve) : getResources().getString(R.string.dialog_go);
        SelectionFrame selectionFrame = new SelectionFrame(mContext);
        selectionFrame.setSomething(null, content, getString(R.string.cancel), buttonText,
                new SelectionFrame.OnSelectionFrameClickListener() {

                    @Override
                    public void cancelClick() {

                    }

                    @Override
                    public void confirmClick() {
                        if (isBind) {
                            unBindInfo(type);
                        } else {
                            if (TextUtils.equals(LoginActivity.THIRD_TYPE_WECHAT, type)) {
                                if (!AppUtils.isAppInstalled(mContext, "com.tencent.mm")) {
                                    Toast.makeText(mContext, getString(R.string.tip_no_wx_chat), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                WXEntryActivity.wxBand(mContext);
                            } else if (TextUtils.equals(LoginActivity.THIRD_TYPE_QQ, type)) {
                                if (!QQHelper.qqInstalled(mContext)) {
                                    Toast.makeText(mContext, getString(R.string.tip_no_qq_chat), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                QQHelper.qqBand(BandAccountActivity.this);
                            }
                        }
                    }
                });
        selectionFrame.show();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventUpdateBandAccount message) {
        isBindWx = "ok".equals(message.msg);
        bindStatusRefresh();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventUpdateBandQqAccount message) {
        isBindQq = "ok".equals(message.msg);
        bindStatusRefresh();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventUpdateBandTelephoneAccount message) {
        if ("ok".equals(message.msg)) {
            isBindTel = true;
            coreManager.getSelf().setTelephone(message.result);
        }
        bindStatusRefresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case com.tencent.connect.common.Constants.REQUEST_LOGIN:
            case com.tencent.connect.common.Constants.REQUEST_APPBAR:
                Tencent.onActivityResultData(requestCode, resultCode, data, QQHelper.getLoginListener(mContext));
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
