package com.sk.weichat.ui.login

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.RequestParams
import com.sk.weichat.AppConstant
import com.sk.weichat.BuildConfig
import com.sk.weichat.MyApplication
import com.sk.weichat.R
import com.sk.weichat.bean.Code
import com.sk.weichat.bean.LoginRegisterResult
import com.sk.weichat.databinding.ActivityPhoneLoginBinding
import com.sk.weichat.helper.*
import com.sk.weichat.ui.account.DataDownloadActivity
import com.sk.weichat.ui.base.CoreManager
import com.sk.weichat.ui.base.ViewBindingActivity
import com.sk.weichat.ui.setAgreement
import com.sk.weichat.util.*
import com.sk.weichat.util.secure.LoginPassword
import com.xuan.xuanhttplibrary.okhttp.HttpUtils
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult
import com.xuan.xuanhttplibrary.okhttp.result.Result
import okhttp3.Call
import org.apache.http.Header
import java.io.File
import java.io.FileNotFoundException
import java.util.*

class PhoneLoginActivity : ViewBindingActivity<ActivityPhoneLoginBinding>() {
    // 验证码倒计时器
    private var countDownTimer: CountDownTimer? = null
    private var isCountingDown = false
    
    // 登录方式标记：true表示验证码登录，false表示密码登录
    private var isVerifyCodeLogin = true
    
    // 密码是否可见
    private var isPasswordVisible = false
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, PhoneLoginActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化UI
        initView()
        // 设置事件监听
        setListeners()

    }

    private fun initView() {
        // 初始化隐私政策复选框为选中状态
        binding.cbPrivacy.isChecked = true
        // 初始化登录按钮为不可用状态
        updateLoginButtonState()
        
        // 初始显示验证码登录
        showVerifyCodeLogin()

        binding.tvPrivacy.setAgreement()
    }

    private fun setListeners() {
        // 刷新图形码
        binding.imageIvRefresh.setOnClickListener { v: View? ->
            if (TextUtils.isEmpty(binding.etPhone.getText().toString())) {
                Toast.makeText(
                    mContext,
                    getString(R.string.tip_phone_number_empty_request_verification_code),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                requestImageCode()
            }
        }
        binding.tvHelp.setOnClickListener{
            ToastUtil.showToast(this, "帮助")
        }
        // 返回按钮点击事件
        binding.ivBack.setOnClickListener { onBackPressed() }

        // 手机号输入框文本变化监听
        binding.etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLoginButtonState()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // 验证码输入框文本变化监听
        binding.etVerifyCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLoginButtonState()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        
        // 密码输入框文本变化监听
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLoginButtonState()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // 获取验证码按钮点击事件
        binding.btnGetVerifyCode.setOnClickListener { 
            val phone = binding.etPhone.text.toString().trim()
            if (phone.isEmpty() || phone.length != 11) {
                ToastUtil.showToast(this, "请输入正确的手机号码")
                return@setOnClickListener
            }
            if (!isCountingDown) {
                startCountDown()
                sendVerifyCode(phone)
            }
        }

        // 登录按钮点击事件
        binding.btnLogin.setOnClickListener { 
            if (!binding.cbPrivacy.isChecked) {
                ToastUtil.showToast(this, "请阅读并同意隐私政策和服务协议")
                return@setOnClickListener
            }
            val phone = binding.etPhone.text.toString().trim()
            
            if (isVerifyCodeLogin) {
                val verifyCode = binding.etVerifyCode.text.toString().trim()
                if (verifyCode.length != 6) {
                    ToastUtil.showToast(this, "请输入6位验证码")
                    return@setOnClickListener
                }
                verifyAndLogin(phone, verifyCode)
            } else {
                val password = binding.etPassword.text.toString().trim()
                if (password.isEmpty() || password.length < 6) {
                    ToastUtil.showToast(this, "请输入正确的密码")
                    return@setOnClickListener
                }
                passwordLogin(phone, password)
            }
        }

        // 微信登录按钮点击事件
        binding.ivWechatLogin.setOnClickListener { 
            if (!binding.cbPrivacy.isChecked) {
                ToastUtil.showToast(this, "请阅读并同意隐私政策和服务协议")
                return@setOnClickListener
            }
            performWechatLogin()
        }

        // 隐私政策文本点击事件
        binding.tvPrivacy.setOnClickListener { openPrivacyPolicy() }
        
        // 登录方式切换按钮点击事件
        binding.tvSwitchLoginType.setOnClickListener {
            if (isVerifyCodeLogin) {
                showPasswordLogin()
            } else {
                showVerifyCodeLogin()
            }
        }
        
        // 密码显示/隐藏切换按钮点击事件
        binding.ivTogglePasswordVisibility.setOnClickListener { 
            togglePasswordVisibility()
        }
    }

    /**
     * 更新登录按钮状态
     */
    private fun updateLoginButtonState() {
        val phone = binding.etPhone.text.toString().trim()
        var isEnabled = phone.length == 11
        
        if (isVerifyCodeLogin) {
            val verifyCode = binding.etVerifyCode.text.toString().trim()
            isEnabled = isEnabled && verifyCode.isNotEmpty()
        } else {
            val password = binding.etPassword.text.toString().trim()
            isEnabled = isEnabled && password.isNotEmpty()
        }
        
        binding.btnLogin.isEnabled = isEnabled
        // 根据按钮状态更改背景和文字颜色
        if (isEnabled) {
            binding.btnLogin.background = getDrawable(R.drawable.bg_green_btn)
            binding.btnLogin.setTextColor(resources.getColor(R.color.white))
        } else {
            binding.btnLogin.background = getDrawable(R.drawable.bg_login_btn)
            binding.btnLogin.setTextColor(resources.getColor(R.color.gray))
        }
    }

    /**
     * 开始验证码倒计时
     */
    private fun startCountDown() {
        isCountingDown = true
        binding.btnGetVerifyCode.isEnabled = false
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.btnGetVerifyCode.text = "重新发送(${millisUntilFinished / 1000}s)"
            }

            override fun onFinish() {
                isCountingDown = false
                binding.btnGetVerifyCode.isEnabled = true
                binding.btnGetVerifyCode.text = "获取验证码"
            }
        }
        countDownTimer?.start()
    }

    /**
     * 发送验证码
     */
    private fun sendVerifyCode(phone: String) {
        val language = Locale.getDefault().language
        val params = HashMap<String, String>()
        params["phone"] = phone
        params["type"] = "login"
        params.put("language", language)
        params.put("areaCode", "86")
        params.put("telephone", phone)
        params.put("imgCode", binding.imageTv.text.toString())
        params.put("isRegister", "0")
        params.put("version", "1")
        HttpUtils.get().url(coreManager.config.SEND_AUTH_CODE)
            .params(params)
            .build()
            .execute(object : BaseCallback<Code>(Code::class.java) {
                override fun onResponse(result: ObjectResult<Code>) {
                    if (Result.checkSuccess(mContext, result)) {
                        if (BuildConfig.DEBUG) {
                            ToastUtil.showToast(mContext, "${result.data.code}")
                        } else {
                            ToastUtil.showToast(mContext, "验证码已发送")
                        }
                    } else {
                        countDownTimer?.cancel()
                        isCountingDown = false
                        showVerifyCodeLogin()
                        binding.btnGetVerifyCode.isEnabled = true
                    }
                }

                override fun onError(call: Call, e: Exception) {
                    ToastUtil.showNetError(mContext)
                    // 如果网络错误，取消倒计时
                    countDownTimer?.cancel()
                    isCountingDown = false
                    binding.btnGetVerifyCode.isEnabled = true
                    binding.btnGetVerifyCode.text = "获取验证码"
                    showVerifyCodeLogin()
                }
            })
    }

    /**
     * 验证码登录
     */
    private fun verifyAndLogin(phone: String, verifyCode: String) {
            val phoneNumber: String = phone
            DialogHelper.showDefaulteMessageProgressDialog(this)
            val params = HashMap<String, String>()
            params["xmppVersion"] = "1"
            // 附加信息
            params["model"] = DeviceInfoUtil.getModel()
            params["osVersion"] = DeviceInfoUtil.getOsVersion()
            params["serial"] = DeviceInfoUtil.getDeviceId(mContext)
            params["loginType"] = "1" //验证码登录

            // 地址信息
            val latitude = MyApplication.getInstance().bdLocationHelper.latitude
            val longitude = MyApplication.getInstance().bdLocationHelper.longitude
            if (latitude != 0.0) params["latitude"] = latitude.toString()
            if (longitude != 0.0) params["longitude"] = longitude.toString()
            if (MyApplication.IS_OPEN_CLUSTER) { // 服务端集群需要
                val area = PreferenceUtils.getString(this, AppConstant.EXTRA_CLUSTER_AREA)
                if (!TextUtils.isEmpty(area)) {
                    params["area"] = area
                }
            }
            LoginSecureHelper.smsLogin(
                this,
                coreManager,
                verifyCode,
                "86",
                phoneNumber,
                params,
                { t: Throwable ->
                    DialogHelper.dismissProgressDialog()
                    ToastUtil.showToast(
                        this,
                        this.getString(R.string.tip_login_secure_place_holder, t.message)
                    )
                }
            ) { result: ObjectResult<LoginRegisterResult> ->
                DialogHelper.dismissProgressDialog()
                if (!Result.checkSuccess(applicationContext, result)) {
                    return@smsLogin
                }

                LoginHelper.setLoginUser(mContext, coreManager, phone, result.data.password, result)
                val settings = result.data.settings
                MyApplication.getInstance().initPayPassword(result.data.userId, result.data.payPassword)
                YeepayHelper.saveOpened(mContext, result.data.walletUserNo == 1)
                PrivacySettingHelper.setPrivacySettings(this, settings)
                MyApplication.getInstance().initMulti()

                DataDownloadActivity.start(mContext, result.data.isupdate, "")
                finish()

        }
    }
    
    /**
     * 密码登录
     */
    private fun passwordLogin(phone: String, password: String) {
        ToastUtil.showToast(this, "正在密码登录...")
        // 加密之后的密码
        val digestPwd = LoginPassword.encodeMd5(password)
        DialogHelper.showDefaulteMessageProgressDialog(this)
        val params: MutableMap<String, String> = HashMap()
        params["xmppVersion"] = "1"
        // 附加信息+
        // 附加信息+
        params["model"] = DeviceInfoUtil.getModel()
        params["osVersion"] = DeviceInfoUtil.getOsVersion()
        params["serial"] = DeviceInfoUtil.getDeviceId(mContext)
        // 地址信息
        // 地址信息
        val latitude = MyApplication.getInstance().bdLocationHelper.latitude
        val longitude = MyApplication.getInstance().bdLocationHelper.longitude
        if (latitude != 0.0) params["latitude"] = latitude.toString()
        if (longitude != 0.0) params["longitude"] = longitude.toString()

        if (MyApplication.IS_OPEN_CLUSTER) { // 服务端集群需要
            val area = PreferenceUtils.getString(this, AppConstant.EXTRA_CLUSTER_AREA)
            if (!TextUtils.isEmpty(area)) {
                params["area"] = area
            }
        }

        LoginSecureHelper.secureLogin(
            this,
            coreManager,
            "86",
            phone,
            password,
            null,
            null,
            false,
            params,
            { t: Throwable ->
                DialogHelper.dismissProgressDialog()
                ToastUtil.showToast(
                    this,
                    this.getString(R.string.tip_login_secure_place_holder, t.message)
                )
            }
        ) { result: ObjectResult<LoginRegisterResult> ->
            DialogHelper.dismissProgressDialog()
            if (!Result.checkSuccess(
                    applicationContext,
                    result
                )
            ) {
                if (Result.checkError(
                        result,
                        Result.CODE_THIRD_NO_EXISTS
                    )
                ) {
                    // 如果返回1040306表示这个IM账号不存在，跳到注册页面让用户注册IM账号并绑定微信，

                } else if (Result.checkError(
                        result,
                        Result.CODE_THIRD_NO_PHONE
                    )
                ) {
                    // 微信没有绑定IM账号，跳到注册，注册页有回来登录老账号的按钮，

                    finish()
                }
                ToastUtil.showToast(mContext, "登录失败")
                return@secureLogin
            }
            ToastUtil.showToast(mContext, "登录成功")
            if (!TextUtils.isEmpty(result.data.authKey)) {
                DialogHelper.showMessageProgressDialog(
                    mContext,
                    getString(R.string.tip_need_auth_login)
                )
//                val authLogin: LoginActivity.CheckAuthLoginRunnable =
//                    LoginActivity.CheckAuthLoginRunnable(
//                        result.data.authKey, phoneNumber, digestPwd
//                    )
//                waitAuth(authLogin)
                return@secureLogin
            }

            start(
                password,
                result,
                phone,
                digestPwd
            )
        }

    }

    /**
     * 执行微信登录
     */
    private fun performWechatLogin() {
        ToastUtil.showToast(this, "正在进行微信登录...")
        // 这里应该调用微信SDK进行登录
        // 示例代码
        // WeChatHelper.login(this)
    }

    /**
     * 打开隐私政策页面
     */
    private fun openPrivacyPolicy() {
        // 跳转到隐私政策页面
        // 示例代码
        // val intent = Intent(this, WebViewActivity::class.java)
        // intent.putExtra("url", "https://example.com/privacy")
        // startActivity(intent)
    }
    
    /**
     * 显示验证码登录界面
     */
    private fun showVerifyCodeLogin() {
        requestImageCode()
        isVerifyCodeLogin = true
        binding.layoutVerifyCode.visibility = View.VISIBLE
        binding.llInputPwd.visibility = View.GONE
        binding.etPassword.setText("")
        binding.ivTogglePasswordVisibility.visibility = View.GONE
        binding.tvSwitchLoginType.text = "密码登录"
        binding.btnGetVerifyCode.text = "获取验证码"
        binding.btnLogin.text="验证并登录"
        binding.tvLoginType.text="手机号登录"
        updateLoginButtonState()
    }

    
    /**
     * 显示密码登录界面
     */
    private fun showPasswordLogin() {
        isVerifyCodeLogin = false
        binding.etVerifyCode.setText("")
        binding.layoutVerifyCode.visibility = View.GONE
        binding.llInputPwd.visibility = View.VISIBLE
        binding.ivTogglePasswordVisibility.visibility = View.VISIBLE
        binding.tvSwitchLoginType.text = "验证码登录"
        binding.btnLogin.text="登录"
        binding.tvLoginType.text="密码登录"
        updateLoginButtonState()
    }
    
    /**
     * 切换密码可见性
     */
    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            // 显示密码
            binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.ivTogglePasswordVisibility.setImageResource(R.drawable.ic_eye_open) // 假设资源文件存在
        } else {
            // 隐藏密码
            binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.ivTogglePasswordVisibility.setImageResource(R.drawable.ic_eye_close) // 假设资源文件存在
        }
        // 确保光标在文本末尾
        binding.etPassword.setSelection(binding.etPassword.text.length)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 取消倒计时
        countDownTimer?.cancel()
    }


    private fun start(
        password: String,
        result: ObjectResult<LoginRegisterResult>,
        phoneNumber: String,
        digestPwd: String
    ) {
        LoginHelper.setLoginUser(mContext, coreManager, phoneNumber, digestPwd, result)
        if (!TextUtils.isEmpty(result.data.headimgurl)) {
            saveAvatar(result.data.userId, result.data.headimgurl)
        }
        val settings = result.data.settings
        MyApplication.getInstance().initPayPassword(result.data.userId, result.data.payPassword)
        YeepayHelper.saveOpened(mContext, result.data.walletUserNo == 1)
        PrivacySettingHelper.setPrivacySettings(this, settings)
        MyApplication.getInstance().initMulti()

        // startActivity(new Intent(mContext, DataDownloadActivity.class));
        DataDownloadActivity.start(mContext, result.data.isupdate, password)
        finish()
    }


    /**
     * 第三方登录，将头像保存至本地
     */
    private fun saveAvatar(userId: String, headImageUrl: String) {
        ImageLoadHelper.loadBitmapCenterCropDontAnimate(MyApplication.getContext(), headImageUrl,
            { b: Bitmap? ->
                val path = FileUtil.saveBitmap(b)
                uploadAvatar(userId, File(path))
            }
        ) { e: java.lang.Exception? -> }
    }

    /**
     * 第三方登录，上传头像，上传成功与否都不管
     */
    private fun uploadAvatar(userId: String, file: File) {
        if (!file.exists()) {
            // 文件不存在
            return
        }
        val params = RequestParams()
        params.put("userId", userId)
        try {
            params.put("file1", file)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        val client = AsyncHttpClient()
        client.post(CoreManager.requireConfig(MyApplication.getContext()).AVATAR_UPLOAD_URL, params,
            object : AsyncHttpResponseHandler() {
                override fun onSuccess(arg0: Int, arg1: Array<Header>, arg2: ByteArray) {}
                override fun onFailure(
                    arg0: Int,
                    arg1: Array<Header>,
                    arg2: ByteArray,
                    arg3: Throwable
               ) {
                }
            })
    }


    /**
     * 请求图形验证码
     */
    private fun requestImageCode() {
        val params: MutableMap<String, String> = HashMap()
        params["telephone"] = "86".toString() + binding.etPhone.getText().toString().trim { it <= ' ' }
        val url = HttpUtils.get().url(coreManager.config.USER_GETCODE_IMAGE)
            .params(params)
            .buildUrl()
        Glide.with(mContext).load(url)
            .asBitmap()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(object : SimpleTarget<Bitmap?>() {


                override fun onLoadFailed(e: java.lang.Exception, errorDrawable: Drawable) {
                    Toast.makeText(
                        this@PhoneLoginActivity,
                        R.string.tip_verification_code_load_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    glideAnimation: GlideAnimation<in Bitmap?>?
                ) {
                    binding.imageIv.setImageBitmap(resource)
                }
            })
    }

}