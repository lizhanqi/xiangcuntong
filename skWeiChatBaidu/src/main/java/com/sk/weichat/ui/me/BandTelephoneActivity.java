package com.sk.weichat.ui.me;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.Code;
import com.sk.weichat.bean.User;
import com.sk.weichat.bean.event.EventUpdateBandTelephoneAccount;
import com.sk.weichat.broadcast.OtherBroadcast;
import com.sk.weichat.db.dao.UserDao;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.ImageLoadHelper;
import com.sk.weichat.helper.PasswordHelper;
import com.sk.weichat.ui.account.SelectPrefixActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.tool.ButtonColorChange;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.ViewPiexlUtil;
import com.sk.weichat.util.secure.DH;
import com.sk.weichat.util.secure.LoginPassword;
import com.sk.weichat.util.secure.RSA;
import com.sk.weichat.util.secure.chat.SecureChatUtil;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.greenrobot.event.EventBus;
import okhttp3.Call;

/**
 * 绑定手机号
 */
public class BandTelephoneActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "BandTelephoneActivity";
    private TextView tv_prefix;
    private int mobilePrefix = 86;
    private EditText mPhoneNumberEdit;
    private EditText mPasswordEdit, mConfigPasswordEdit;
    // 图形验证码
    private EditText mImageCodeEdit;
    private ImageView mImageCodeIv;
    // 验证码
    private EditText mAuthCodeEdit;
    private String randcode;
    private Button btn_getCode;
    private Button btn_change;
    private int reckonTime = 60;
    private Handler mReckonHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0x1) {
                btn_getCode.setText("(" + reckonTime + ")");
                reckonTime--;
                if (reckonTime < 0) {
                    mReckonHandler.sendEmptyMessage(0x2);
                } else {
                    mReckonHandler.sendEmptyMessageDelayed(0x1, 1000);
                }
            } else if (msg.what == 0x2) {
                // 60秒结束
                btn_getCode.setText(getString(R.string.send));
                btn_getCode.setEnabled(true);
                reckonTime = 60;
            }
        }
    };

    private String dhPrivateKey, rsaPublicKey, rsaPrivateKey;

    public BandTelephoneActivity() {
        noLoginRequired();
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, BandTelephoneActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_band_telephone);
        initActionBar();
        initView();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(coreManager.isBindTelephone()
                ? getString(R.string.band_telephone_change) : getString(R.string.band_phone));
    }

    private void initView() {
        tv_prefix = findViewById(R.id.tv_prefix);
        mobilePrefix = PreferenceUtils.getInt(this, Constants.AREA_CODE_KEY, mobilePrefix);
        tv_prefix.setText("+" + mobilePrefix);
        tv_prefix.setOnClickListener(this);

        mPhoneNumberEdit = findViewById(R.id.phone_numer_edit);
        if (coreManager.isBindTelephone()) {
            mPhoneNumberEdit.setText(coreManager.getSelf().getTelephoneNoAreaCode());
        }

        findViewById(R.id.llSetPassword).setVisibility(coreManager.isBindTelephone() ? View.GONE : View.VISIBLE);
        mPasswordEdit = findViewById(R.id.password_edit);
        PasswordHelper.bindPasswordEye(mPasswordEdit, findViewById(R.id.tbEye));
        mConfigPasswordEdit = findViewById(R.id.confirm_password_edit);
        PasswordHelper.bindPasswordEye(mConfigPasswordEdit, findViewById(R.id.tbEyeConfirm));

        mImageCodeEdit = findViewById(R.id.image_tv);
        mImageCodeIv = findViewById(R.id.image_iv);
        findViewById(R.id.image_iv_refresh).setOnClickListener(this);
        mAuthCodeEdit = findViewById(R.id.auth_code_edit);
        btn_getCode = findViewById(R.id.send_again_btn);
        ButtonColorChange.colorChange(this, btn_getCode);
        btn_getCode.setOnClickListener(this);
        List<EditText> mEditList = new ArrayList<>();
        mEditList.add(mPasswordEdit);
        mEditList.add(mConfigPasswordEdit);
        mEditList.add(mImageCodeEdit);
        mEditList.add(mAuthCodeEdit);
        setBound(mEditList);

        btn_change = findViewById(R.id.login_btn);
        ButtonColorChange.colorChange(this, btn_change);
        btn_change.setOnClickListener(this);
        btn_change.setText(coreManager.isBindTelephone()
                ? getString(R.string.band_telephone_change) : getString(R.string.band_phone));

        // 请求图形验证码
        if (!TextUtils.isEmpty(mPhoneNumberEdit.getText().toString())) {
            requestImageCode();
        }
        mPhoneNumberEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // 手机号输入完成后自动刷新验证码，
                // 只在移开焦点，也就是点击其他EditText时调用，
                requestImageCode();
            }
        });
    }

    public void setBound(List<EditText> mEditList) {// 为Edit内的drawableLeft设置大小
        for (int i = 0; i < mEditList.size(); i++) {
            Drawable[] compoundDrawable = mEditList.get(i).getCompoundDrawables();
            Drawable drawable = compoundDrawable[0];
            if (drawable != null) {
                drawable.setBounds(0, 0, ViewPiexlUtil.dp2px(this, 20), ViewPiexlUtil.dp2px(this, 20));
                mEditList.get(i).setCompoundDrawables(drawable, null, null, null);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_prefix:
                // 选择国家区号
                Intent intent = new Intent(this, SelectPrefixActivity.class);
                startActivityForResult(intent, SelectPrefixActivity.REQUEST_MOBILE_PREFIX_LOGIN);
                break;
            case R.id.image_iv_refresh:
                if (TextUtils.isEmpty(mPhoneNumberEdit.getText().toString())) {
                    ToastUtil.showToast(this, getString(R.string.tip_phone_number_empty_request_verification_code));
                } else {
                    requestImageCode();
                }
                break;
            case R.id.send_again_btn:
                // 获取验证码
                String phoneNumber = mPhoneNumberEdit.getText().toString().trim();
                String imagecode = mImageCodeEdit.getText().toString().trim();
                if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(imagecode)) {
                    ToastUtil.showToast(mContext, getString(R.string.tip_phone_number_verification_code_empty));
                    return;
                }
                if (!configPassword()) {// 两次密码是否一致
                    return;
                }
                verifyTelephone(phoneNumber, imagecode);
                break;
            case R.id.login_btn:
                // 确认修改
                if (nextStep()) {
                    // 如果验证码正确，则可以绑定/换绑手机
                    bandTelephone();
                }
                break;
        }
    }

    /**
     * 绑定/换绑手机
     */
    private void bandTelephone() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        final String phoneNumber = mPhoneNumberEdit.getText().toString().trim();
        final String password = mPasswordEdit.getText().toString().trim();
        String authCode = mAuthCodeEdit.getText().toString().trim();
        Map<String, String> params = new HashMap<>();
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("telephone", mobilePrefix + phoneNumber);
        params.put("code", authCode);

        // 绑定手机时，需要输入密码，如开启端到端，生成key上传，账号变为端到端账号
        if (!coreManager.isBindTelephone()) {
            params.put("password", LoginPassword.encodeMd5(password));
            if (MyApplication.IS_SUPPORT_SECURE_CHAT) {
                // SecureFlag 将密钥对上传服务器
                DH.DHKeyPair dhKeyPair = DH.genKeyPair();
                String dhPublicKey = dhKeyPair.getPublicKeyBase64();
                dhPrivateKey = dhKeyPair.getPrivateKeyBase64();
                String aesEncryptDHPrivateKeyResult = SecureChatUtil.aesEncryptDHPrivateKey(password, dhPrivateKey);
                RSA.RsaKeyPair rsaKeyPair = RSA.genKeyPair();
                rsaPublicKey = rsaKeyPair.getPublicKeyBase64();
                rsaPrivateKey = rsaKeyPair.getPrivateKeyBase64();
                String aesEncryptRSAPrivateKeyResult = SecureChatUtil.aesEncryptRSAPrivateKey(password, rsaPrivateKey);
                params.put("dhPublicKey", dhPublicKey);
                params.put("dhPrivateKey", aesEncryptDHPrivateKeyResult);
                params.put("rsaPublicKey", rsaPublicKey);
                params.put("rsaPrivateKey", aesEncryptRSAPrivateKeyResult);
            }
        }

        HttpUtils.get().url(coreManager.getConfig().USER_WX_BIND_TELEPHONE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            // 绑定手机时，需要输入密码，生成key上传，账号变为端到端账号
                            if (!coreManager.isBindTelephone()
                                    && MyApplication.IS_SUPPORT_SECURE_CHAT) {
                                SecureChatUtil.setDHPrivateKey(coreManager.getSelf().getUserId(), dhPrivateKey);
                                SecureChatUtil.setRSAPublicKey(coreManager.getSelf().getUserId(), rsaPublicKey);
                                SecureChatUtil.setRSAPrivateKey(coreManager.getSelf().getUserId(), rsaPrivateKey);
                            }
                            downloadUserInfo();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    /**
     * 请求图形验证码
     */
    private void requestImageCode() {
        Map<String, String> params = new HashMap<>();
        params.put("telephone", mobilePrefix + mPhoneNumberEdit.getText().toString().trim());
        String url = HttpUtils.get().url(coreManager.getConfig().USER_GETCODE_IMAGE)
                .params(params)
                .buildUrl();
        ImageLoadHelper.loadBitmapWithoutCache(
                mContext,
                url,
                b -> {
                    mImageCodeIv.setImageBitmap(b);
                }, e -> {
                    Toast.makeText(BandTelephoneActivity.this, R.string.tip_verification_code_load_failed, Toast.LENGTH_SHORT).show();
                }
        );
    }

    /**
     * 请求验证码
     */
    private void verifyTelephone(String phoneNumber, String imageCode) {
        if (TextUtils.equals(phoneNumber, coreManager.getSelf().getTelephoneNoAreaCode())) {
            // 换绑的手机号与之前的一样
            ToastUtil.showToast(mContext, getString(R.string.tip_band_telephone_change));
            return;
        }
        DialogHelper.showDefaulteMessageProgressDialog(this);
        Map<String, String> params = new HashMap<>();
        String language = Locale.getDefault().getLanguage();
        params.put("language", language);
        params.put("areaCode", String.valueOf(mobilePrefix));
        params.put("telephone", phoneNumber);
        params.put("imgCode", imageCode);
        params.put("isRegister", String.valueOf(0));
        params.put("version", "1");

        /**
         * 只判断中国手机号格式
         */
        if (!StringUtils.isMobileNumber(phoneNumber) && mobilePrefix == 86) {
            Toast.makeText(this, getString(R.string.Input_11_phoneNumber), Toast.LENGTH_SHORT).show();
            return;
        }

        HttpUtils.get().url(coreManager.getConfig().SEND_AUTH_CODE)
                .params(params)
                .build(true, true)
                .execute(new BaseCallback<Code>(Code.class) {
                    @Override
                    public void onResponse(ObjectResult<Code> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            Toast.makeText(BandTelephoneActivity.this, R.string.verification_code_send_success, Toast.LENGTH_SHORT).show();
                            btn_getCode.setEnabled(false);
                            // 开始计时
                            mReckonHandler.sendEmptyMessage(0x1);
                            if (result.getData() != null && result.getData().getCode() != null) {
                                // 得到验证码
                                randcode = result.getData().getCode();
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    /**
     * 确认两次输入的密码是否一致
     */
    private boolean configPassword() {
        if (coreManager.isBindTelephone()) {
            return true;
        }
        String password = mPasswordEdit.getText().toString().trim();
        String confirmPassword = mConfigPasswordEdit.getText().toString().trim();
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            mPasswordEdit.requestFocus();
            mPasswordEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.password_empty_error));
            return false;
        }
        if (TextUtils.isEmpty(confirmPassword) || confirmPassword.length() < 6) {
            mConfigPasswordEdit.requestFocus();
            mConfigPasswordEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.confirm_password_empty_error));
            return false;
        }
        if (confirmPassword.equals(password)) {
            return true;
        } else {
            mConfigPasswordEdit.requestFocus();
            mConfigPasswordEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.password_confirm_password_not_match));
            return false;
        }
    }

    /**
     * 验证验证码
     */
    private boolean nextStep() {
        final String phoneNumber = mPhoneNumberEdit.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, getString(R.string.hint_input_phone_number), Toast.LENGTH_SHORT).show();
            return false;
        }
        /**
         * 只判断中国手机号格式
         */
        if (!StringUtils.isMobileNumber(phoneNumber) && mobilePrefix == 86) {
            Toast.makeText(this, getString(R.string.Input_11_phoneNumber), Toast.LENGTH_SHORT).show();
            return false;
        }
        String authCode = mAuthCodeEdit.getText().toString().trim();
        if (TextUtils.isEmpty(authCode)) {
            Toast.makeText(this, getString(R.string.input_message_code), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!TextUtils.isEmpty(randcode)) {
            if (authCode.equals(randcode)) {
                // 验证码正确
                return true;
            } else {
                Toast.makeText(this, getString(R.string.msg_code_not_ok), Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != SelectPrefixActivity.RESULT_MOBILE_PREFIX_SUCCESS)
            return;
        mobilePrefix = data.getIntExtra(Constants.MOBILE_PREFIX, 86);
        tv_prefix.setText("+" + mobilePrefix);
        // 图形验证码可能因区号失效，
        // 请求图形验证码
        if (!TextUtils.isEmpty(mPhoneNumberEdit.getText().toString())) {
            requestImageCode();
        }
    }

    /**
     * 下载个人基本资料
     */
    private void downloadUserInfo() {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);

        HttpUtils.get().url(coreManager.getConfig().USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {

                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            User user = result.getData();
                            // 设置登陆用户信息
                            boolean updateSuccess = UserDao.getInstance().saveUserLogin(user);
                            if (updateSuccess) {
                                // 如果成功，保存User变量，
                                Log.e(TAG, "绑定/换绑手机号成功，且更新本地user成功");
                            } else {
                                Log.e(TAG, "绑定/换绑手机号成功，但更新本地user失败");
                            }
                            coreManager.setSelf(user);
                            // 通知MeFragment更新
                            sendBroadcast(new Intent(OtherBroadcast.SYNC_SELF_DATE_NOTIFY));
                            EventBus.getDefault().post(new EventUpdateBandTelephoneAccount(user.getTelephone(), "ok"));
                        } else {
                            Log.e(TAG, "绑定/换绑手机号成功，但user/get<onResponse>接口调用失败");
                        }
                        finish();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Log.e(TAG, "绑定/换绑手机号成功，但user/get<onError>接口调用失败");
                        finish();
                    }
                });
    }
}
