package com.mobile.auth

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.cmic.gen.sdk.auth.GenAuthnHelper
import com.cmic.gen.sdk.auth.GenTokenListener
import com.cmic.gen.sdk.view.GenAuthThemeConfig
import com.cmic.gen.sdk.view.GenLoginClickListener
import com.mobile.auth.tokenValidate.Request
import com.mobile.auth.tokenValidate.RequestCallback
import com.mobile.auth.util.Constant
import org.json.JSONObject

object AuthSDK {
    lateinit var mContext: Application
    fun init(context: Context) {
        this.mContext = context.applicationContext as Application
    }

    val APP_ID = Constant.APP_ID
    val APP_KEY = Constant.APP_KEY
    private val CMCC_SDK_REQUEST_GET_PHONE_INFO_CODE = 1111
    val mGenAuthnHelper by lazy {
        GenAuthnHelper.getInstance(mContext.applicationContext).apply {
            GenAuthnHelper.setDebugMode(true)
            setPageInListener { resultCode ->
                if (resultCode == "200087") {
                    Toast.makeText(mContext, "初始化完成", Toast.LENGTH_SHORT).show()
                    Log.d("认证", " page in---------------")
                }
            }
        }
    }
    private val operatorArray = arrayOf("未知", "移动", "联通", "电信", "香港移动")
    private val networkArray = arrayOf("未知", "数据流量", "纯WiFi", "流量+WiFi")


    /**
     * operatorType获取网络运营商: 0.未知 1.移动流量 2.联通流量网络 3.电信流量网络
     * networkType 网络状态：0未知；1流量 2 wifi；3 数据流量+wifi
     */
    fun getNetAndOperator(): Pair<String, String> {
        val jsonObject: JSONObject = mGenAuthnHelper.getNetworkType(mContext)
        var operator: Int
        var net: Int
        return kotlin.runCatching {
            operator = jsonObject.getString("operatortype").toInt()
            net = jsonObject.getString("networktype").toInt()
            jsonObject.put("operatorType", operatorArray.get(operator))
            jsonObject.put("networkType", networkArray.get(net))
            Log.d("网络", "网络类型:${networkArray.get(net)} 运营商:${operatorArray[operator]}")
            Pair(operatorArray.get(operator), networkArray.get(net))
        }.getOrElse {
            Pair(operatorArray.get(0), networkArray.get(0))
        }
    }
    // 定义一个密封类来封装结果状态（成功/失败）
    sealed class PhoneInfoResult {
        // 成功时携带返回的数据
        data class Success(
            val operatorType: Int,
            val scripExpiresIn: String,
            val rawData: JSONObject?
        ) : PhoneInfoResult()

        // 失败时携带错误码和错误信息
        data class Failure(
            val errorCode: Int,
            val errorMessage: String?,
            val rawData: JSONObject?
        ) : PhoneInfoResult()
    }

    fun getPhoneInfo(callback: (PhoneInfoResult) -> Unit) {
        mGenAuthnHelper.getPhoneInfo(
            APP_ID,
            APP_KEY,
            { p0, p1 ->
                if (p0 == CMCC_SDK_REQUEST_GET_PHONE_INFO_CODE && p1?.optString("resultCode") == "103000") {
                    // 成功：解析数据并返回Success
                    val operatorType = p1.optInt("operatorType")
                    val scripExpiresIn = p1.optString("scripExpiresIn")
                    callback(PhoneInfoResult.Success(operatorType, scripExpiresIn, p1))
                } else {
                    // 失败：返回错误信息
                    val errorMsg = p1?.optString("errorMsg") ?: "未知错误"
                    callback(PhoneInfoResult.Failure(p0, errorMsg, p1))
                }
                Log.d("认证", "$p0   结果 $p1")
            },
            CMCC_SDK_REQUEST_GET_PHONE_INFO_CODE
        )
    }

   private val themeConfigBuilder = GenAuthThemeConfig.Builder()
        .setStatusBar(-0xff7930, true) //状态栏颜色、字体颜色
        //                .setAuthContentView(getLayoutInflater().inflate(R.layout.empty_layout,relativeLayout,false))
//            .setAuthLayoutResID(R.layout.empty_layout)
        .setClauseStatusColor(Color.RED)
//            .setClauseLayoutResID(R.layout.title_layout, "returnId")
        .setNavTextSize(10)
        .setNavTextColor(-0xff7a30) //导航栏字体颜色
        .setNavColor(Color.BLUE) //                .setNavHidden(true)
        .setNumberSize(30, true) ////手机号码字体大小
        .setNumberColor(-0xcccccd) //手机号码字体颜色
        //                .setNumberOffsetX(30)//号码栏X偏移量
        .setNumFieldOffsetY_B(100)
        .setNumFieldOffsetY(100) //号码栏Y偏移量
        //                .setLogoOffsetY(100)
        //                .setLogoOffsetY_B(0)
        .setLogoScaleType(ImageView.ScaleType.CENTER_INSIDE)
        .setLogo(58, 20) //                .setLogBtnText("本机号码一键登录")//登录按钮文本
        .setLogBtnTextColor(-0x1) //登录按钮文本颜色
        .setLogBtnImgPath("umcsdk_login_btn_bg") //登录按钮背景
        .setLogBtnText(" ", -0x1, 15, false)
        .setLogBtnOffsetY_B(200) //登录按钮Y偏移量
        .setLogBtnOffsetY(200) //登录按钮Y偏移量
        //                .setLogBtn(500,30)
        .setLogBtnMargin(30, 30)
        .setCheckTipText("")
        .setGenBackPressedListener {

            Toast.makeText(mContext, "返回键回调", Toast.LENGTH_SHORT).show()
        }
        .setLogBtnClickListener(object : GenLoginClickListener {
            override fun onLoginClickStart(context: Context?, jsonObj: JSONObject?) {
                Log.d("认证", "onLoginClickStart    $jsonObj")
                Toast.makeText(context, "LoginClickStart", Toast.LENGTH_SHORT).show()

            }

            override fun onLoginClickComplete(context: Context?, jsonObj: JSONObject?) {
                Toast.makeText(context, "LoginClickStart", Toast.LENGTH_SHORT).show()
                Log.d("认证", "onLoginClickComplete    $jsonObj")

            }
        })
        .setGenCheckBoxListener { context, jsonObj ->
            Toast.makeText(
                context,
                "自定义勾选提示",
                Toast.LENGTH_LONG
            ).show()
        }
        .setGenAuthLoginListener { context, authLoginCallBack ->
        }
        .setGenCheckedChangeListener { b -> Log.d("是否勾选协议", b.toString() + "") }
        .setClauseClickListener { clauseName, clauseUrl ->
            Toast.makeText(
                mContext,
                """
                    $clauseName
                    $clauseUrl
                    """.trimIndent(),
                Toast.LENGTH_SHORT
            ).show()
        }
        .setCheckedImgPath("umcsdk_check_image") //checkbox被勾选图片
        .setUncheckedImgPath("umcsdk_uncheck_image") //checkbox未被勾选图片
        .setCheckBoxImgPath("umcsdk_check_image", "umcsdk_uncheck_image", 9, 9)
        .setPrivacyState(false) //授权页check
        .setCheckBoxAccurateClick(false)
        .setPrivacyPageFullScreen(true)
        .setPrivacyAlignment(
            "登录即同意" + GenAuthThemeConfig.PLACEHOLDER + "应用自定义服务条款一、应用自定义服务条款二、条款3和条款4并使用本机号码校验",
            "应用自定义服务条款一",
            "https://www.baidu.com",
            "应用自定义服务条款二",
            "https://www.hao123.com",
            "条款3",
            "http://www.sina.com",
            "条款4",
            "http://gz.58.com"
        )
        .setPrivacyText(10, -0x99999a, -0xff7a30, false, true)
        .setClauseColor(-0x99999a, -0xff7a30) //条款颜色
        .setPrivacyMargin(20, 30)
        .setPrivacyOffsetY(30) //隐私条款Y偏移量
        .setPrivacyOffsetY_B(50) //隐私条款Y偏移量
        .setCheckBoxLocation(1) //                .setAppLanguageType(2)
        .setBackButton(true)
        .setWebDomStorage(true)
        .setPrivacyAnimation("umcsdk_anim_shake")
        .setPrivacyBookSymbol(true) //                .setAuthPageActIn("in_activity","out_activity")
        //                .setAuthPageActOut("in_activity","out_activity")
        //                .setAuthPageWindowMode(300, 300)
        //                .setAuthPageWindowOffset(0,0)
        .setThemeId(R.style.loginDialog)
        .setAuthPageWindowMode(300, 300)
        .setNumFieldOffsetY(50)
        .setLogBtnOffsetY(120)
    private const val CMCC_SDK_REQUEST_LOGIN_AUTH_CODE = 3333
    fun showAuth() {
        themeConfigBuilder.setAuthPageWindowMode(0, 0).setThemeId(-1)
        mGenAuthnHelper.authThemeConfig = themeConfigBuilder.build()
        mGenAuthnHelper.loginAuth(
            Constant.APP_ID,
            Constant.APP_KEY,
            { p0, p1 ->
                Log.d("认证", "$p0   结果 $p1")
                if (p0 == CMCC_SDK_REQUEST_LOGIN_AUTH_CODE && p1?.optString("resultCode") == "103000") {
                    if (p1.has("token")) {
                        mGenAuthnHelper.quitAuthActivity()
                    }
                }
            },
            CMCC_SDK_REQUEST_LOGIN_AUTH_CODE
        )

    }

    fun tokenValidate(appId: String?, appKey: String?, token: String?, listener: GenTokenListener) {
        val values = Bundle()
        values.putString("appSecret", appKey)
        values.putString("appId", appId)
        values.putString("token", token)
        Request.getInstance(mContext).tokenValidate(values, object : RequestCallback {
            override fun onRequestComplete(
                resultCode: String?,
                resultDes: String?,
                jsonObj: JSONObject
            ) {
                Log.i("Token校验结果：", jsonObj.toString())

                Toast.makeText(mContext, "返回键回调", Toast.LENGTH_SHORT).show()
            }
        })
    }

}