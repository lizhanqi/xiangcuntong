package com.sk.weichat.ui.login

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import com.cmic.gen.sdk.auth.GenAuthnHelper
import com.cmic.gen.sdk.auth.GenTokenListener
import com.cmic.gen.sdk.view.GenAuthThemeConfig
import com.cmic.gen.sdk.view.GenLoginClickListener
import com.example.qrcode.Constant
import com.mobile.auth.AuthSDK
import com.mobile.auth.AuthSDK.getPhoneInfo
import com.mobile.auth.AuthSDK.showAuth
import com.sk.weichat.R
import com.sk.weichat.databinding.ActivityWecomeLoginBinding
import com.sk.weichat.ui.base.ViewBindingActivity
import com.sk.weichat.ui.dialog.LoginMethodDialog
import com.sk.weichat.util.ToastUtil
import org.json.JSONException
import org.json.JSONObject


class WeComeLoginActivity : ViewBindingActivity<ActivityWecomeLoginBinding>() {
// 创建并显示登录方式选择弹窗
    val dialog by lazy{
            LoginMethodDialog(this) .apply {
            setOnLoginMethodListener(object : LoginMethodDialog.OnLoginMethodListener{
                override fun onPhoneLoginClick() {
                     startActivity(Intent(this@WeComeLoginActivity,PhoneLoginActivity::class.java))
                }
            })
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化UI
        initView()
        // 设置点击事件
        setListeners()
    }

    val mGenAuthnHelper by lazy { GenAuthnHelper.getInstance(mContext.applicationContext) }
    private val operatorArray = arrayOf("未知", "移动", "联通", "电信", "香港移动")
    private val networkArray = arrayOf("未知", "数据流量", "纯WiFi", "流量+WiFi")

    /**
     * operatorType获取网络运营商: 0.未知 1.移动流量 2.联通流量网络 3.电信流量网络
     * networkType 网络状态：0未知；1流量 2 wifi；3 数据流量+wifi
     */
    private fun getNetAndOperator() {
        val jsonObject: JSONObject = mGenAuthnHelper.getNetworkType(mContext)
        val operator: Int
        val net: Int
        try {
            operator = jsonObject.getString("operatortype").toInt()
            net = jsonObject.getString("networktype").toInt()
            jsonObject.put("operatorType", operatorArray.get(operator))
            jsonObject.put("networkType", networkArray.get(net))
            Log.d("网络", "网络类型:${networkArray.get(net)} 运营商:${operatorArray[operator]}")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }


    private fun initView() {
        // 设置隐私政策复选框默认选中
        binding.cbPrivacy.isChecked = true
        // 设置手机号（这里使用示例数据，实际应用中可能需要从本地存储或SIM卡读取）
        binding.tvPhoneNumber.text = "+86 137 **** 7678"

    }

    private fun setListeners() {

        AuthSDK.init(this.applicationContext)
        // 一键登录按钮点击事件
        binding.btnOneKeyLogin.setOnClickListener {
            PhoneLoginActivity.start(this)

//            getPhoneInfo { result ->
//                if (result is AuthSDK.PhoneInfoResult.Success) {
//                    // 处理成功逻辑
//                    println("运营商类型：${result.operatorType}")
//                    println("有效期：${result.scripExpiresIn}秒")
//                    // 可以使用原始数据result.rawData做更多处理
//                    showAuth()
//                } else {
//
//                }
//            }
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
        dialog.show()
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