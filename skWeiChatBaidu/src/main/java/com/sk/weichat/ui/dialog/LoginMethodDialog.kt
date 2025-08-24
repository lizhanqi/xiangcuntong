package com.sk.weichat.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.sk.weichat.R
import com.sk.weichat.ui.setAgreement

/**
 * 登录方式选择底部弹窗
 */
class LoginMethodDialog(private val mContext: Context) : Dialog(
    mContext, R.style.BottomDialog
), View.OnClickListener {
    private var mListener: OnLoginMethodListener? = null
    private var mCbPrivacy: CheckBox? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置弹窗布局
        val contentView = LayoutInflater.from(mContext).inflate(R.layout.dialog_login_method, null)
        setContentView(contentView)

        // 设置弹窗宽度为屏幕宽度
        val layoutParams = contentView.layoutParams
        layoutParams.width = mContext.resources.displayMetrics.widthPixels
        contentView.layoutParams = layoutParams

        // 设置弹窗位置在底部
        val window = window
        window!!.setGravity(Gravity.BOTTOM)
        window.setWindowAnimations(R.style.BottomDialog_Animation)

        // 初始化控件
        val ivClose = findViewById<ImageView>(R.id.iv_close)
        val llPhoneLogin = findViewById<LinearLayout>(R.id.ll_phone_login)
        mCbPrivacy = findViewById(R.id.cb_privacy)
        val tvPrivacy = findViewById<TextView>(R.id.tv_privacy)

        // 设置点击事件
        ivClose.setOnClickListener(this)
        llPhoneLogin.setOnClickListener(this)
        tvPrivacy.setAgreement()
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.iv_close) {
            dismiss()
        } else if (id == R.id.ll_phone_login) {

            mListener?.onPhoneLoginClick()
            dismiss()
        }
    }

    /**
     * 设置登录方式选择监听器
     */
    fun setOnLoginMethodListener(listener: OnLoginMethodListener?) {
        mListener = listener
    }

    /**
     * 登录方式选择监听器接口
     */
    interface OnLoginMethodListener {
        /**
         * 手机登录点击事件
         */
        fun onPhoneLoginClick()
    }
}