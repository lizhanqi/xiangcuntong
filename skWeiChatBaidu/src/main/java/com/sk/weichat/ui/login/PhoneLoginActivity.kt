package com.sk.weichat.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import com.sk.weichat.R
import com.sk.weichat.bean.Code
import com.sk.weichat.databinding.ActivityPhoneLoginBinding
import com.sk.weichat.ui.base.ViewBindingActivity
import com.sk.weichat.util.ToastUtil
import com.xuan.xuanhttplibrary.okhttp.HttpUtils
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult
import com.xuan.xuanhttplibrary.okhttp.result.Result
import okhttp3.Call
import java.util.*

class PhoneLoginActivity : ViewBindingActivity<ActivityPhoneLoginBinding>() {
    // 验证码倒计时器
    private var countDownTimer: CountDownTimer? = null
    private var isCountingDown = false
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
    }

    private fun setListeners() {
        binding.tvHelp.setOnClickListener{
            ToastUtil.showToast(this, "帮助")

        }
        // 返回按钮点击事件
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }

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
            val verifyCode = binding.etVerifyCode.text.toString().trim()
            if (verifyCode.length != 6) {
                ToastUtil.showToast(this, "请输入6位验证码")
                return@setOnClickListener
            }
            verifyAndLogin(phone, verifyCode)
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
        binding.tvPrivacy.setOnClickListener {
            // 跳转到隐私政策页面
            openPrivacyPolicy()
        }
    }

    /**
     * 更新登录按钮状态
     */
    private fun updateLoginButtonState() {
        val phone = binding.etPhone.text.toString().trim()
        val verifyCode = binding.etVerifyCode.text.toString().trim()
        val isEnabled = phone.length == 11 && verifyCode.isNotEmpty()
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
        val language = Locale.getDefault().language;



        val params = HashMap<String, String>()
        params["phone"] = phone
        params["type"] = "login"
        params.put("language", language);
        params.put("areaCode", "86");
        params.put("telephone", phone);
        params.put("imgCode", "imageCodeStr");
        params.put("isRegister", "0");
        params.put("version", "1");
        // 这里应该调用实际的发送验证码接口
        // 示例代码
        HttpUtils.get().url(coreManager.config.SEND_AUTH_CODE)
            .params(params)
            .build()
            .execute(object : BaseCallback<Code>(Code::class.java) {
                override fun onResponse(result: ObjectResult<Code>) {
                    if (Result.checkSuccess(mContext, result)) {
                        ToastUtil.showToast(mContext, "验证码已发送")
                    } else {
                        ToastUtil.showToast(mContext, "发送错误")
                    }
                }

                override fun onError(call: Call, e: Exception) {
                    ToastUtil.showNetError(mContext)
                    // 如果网络错误，取消倒计时
                    countDownTimer?.cancel()
                    isCountingDown = false
                    binding.btnGetVerifyCode.isEnabled = true
                    binding.btnGetVerifyCode.text = "获取验证码"
                }
            })
    }

    /**
     * 验证并登录
     */
    private fun verifyAndLogin(phone: String, verifyCode: String) {
        ToastUtil.showToast(this, "正在验证登录...")
        
        val params = HashMap<String, String>()
        params["phone"] = phone
        params["verifyCode"] = verifyCode

        // 这里应该调用实际的登录接口
        // 示例代码
        /*
        HttpUtils.get().url(coreManager.getConfig().ACCESS_PHONE_LOGIN)
            .params(params)
            .build()
            .execute(object : BaseCallback<LoginResult>(LoginResult::class.java) {
                override fun onResponse(result: ObjectResult<LoginResult>) {
                    if (Result.checkSuccess(mContext, result)) {
                        // 登录成功，处理登录结果
                        // 例如保存用户信息、跳转到主界面等
                        // startActivity(Intent(mContext, MainActivity::class.java))
                        // finish()
                    }
                }

                override fun onError(call: Call, e: Exception) {
                    ToastUtil.showNetError(mContext)
                }
            })
        */
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

    override fun onDestroy() {
        super.onDestroy()
        // 取消倒计时
        countDownTimer?.cancel()
    }
}