package com.sk.weichat.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.sk.weichat.MyApplication
import com.sk.weichat.R
import com.sk.weichat.ui.base.ViewBindingActivity
import com.sk.weichat.util.ToastUtil
import com.sk.weichat.databinding.ActivityWecomeLoginBinding

class WeComeLoginActivity : ViewBindingActivity<ActivityWecomeLoginBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化UI
        initView()
        // 设置点击事件
        setListeners()
    }

    private fun initView() {
        // 设置隐私政策复选框默认选中
        binding.cbPrivacy.isChecked = true
        // 设置手机号（这里使用示例数据，实际应用中可能需要从本地存储或SIM卡读取）
        binding.tvPhoneNumber.text = "+86 137 **** 7678"
    }

    private fun setListeners() {
        // 一键登录按钮点击事件
        binding.btnOneKeyLogin.setOnClickListener {
            if (!binding.cbPrivacy.isChecked) {
                ToastUtil.showToast(this, "请阅读并同意隐私政策和服务协议")
                return@setOnClickListener
            }
            performOneKeyLogin()
        }

        // 微信登录按钮点击事件
        binding.btnWechatLogin.setOnClickListener {
            if (!binding.cbPrivacy.isChecked) {
                ToastUtil.showToast(this, "请阅读并同意隐私政策和服务协议")
                return@setOnClickListener
            }
            performWechatLogin()
        }

        // 其他账号登录点击事件
        binding.tvOtherLogin.setOnClickListener {
            navigateToOtherLogin()
        }

        // 隐私政策点击事件
        binding.tvPrivacyPolicy.setOnClickListener {
            openPrivacyPolicy()
        }

        // 服务协议点击事件
        binding.tvTermsService.setOnClickListener {
            openTermsService()
        }

        // 未成年人保护规则点击事件
        binding.tvMinorProtect.setOnClickListener {
            openMinorProtection()
        }
    }

    private fun performOneKeyLogin() {
        // 实现一键登录逻辑
        // 这里是示例代码，实际应用中需要集成相应的SDK
        ToastUtil.showToast(this, "正在进行一键登录...")
        // 模拟登录成功
        // startActivity(Intent(this, MainActivity::class.java))
        // finish()
    }

    private fun performWechatLogin() {
        // 实现微信登录逻辑
        // 这里是示例代码，实际应用中需要集成微信SDK
        ToastUtil.showToast(this, "正在进行微信登录...")
        // 调用微信SDK进行登录
        // WeChatHelper.login(this)
    }

    private fun navigateToOtherLogin() {
        // 跳转到其他账号登录页面
        // 这里假设已存在LoginActivity用于其他账号登录
        val intent = Intent(this, com.sk.weichat.ui.account.LoginActivity::class.java)
        startActivity(intent)
    }

    private fun openPrivacyPolicy() {
        // 打开隐私政策页面
        openWebPage("隐私政策")
    }

    private fun openTermsService() {
        // 打开服务协议页面
        openWebPage("服务协议")
    }

    private fun openMinorProtection() {
        // 打开未成年人保护规则页面
        openWebPage("未成年人保护规则")
    }

    private fun openWebPage(type: String) {
        // 这里是示例代码，实际应用中可能需要跳转到WebViewActivity或使用浏览器打开
        ToastUtil.showToast(this, "打开$type 页面")
        // val intent = Intent(this, WebViewActivity::class.java)
        // intent.putExtra("url", getPolicyUrl(type))
        // startActivity(intent)
    }
}