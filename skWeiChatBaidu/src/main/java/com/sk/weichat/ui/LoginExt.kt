package com.sk.weichat.ui

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import com.sk.weichat.R
import com.sk.weichat.util.ToastUtil
import com.sk.weichat.view.LinkTouchMovementMethod

private fun getRenderText(
    context: Context,
    onLoginAgreementListener: OnLoginAgreementListener?
): CharSequence? {
    val content = "我已阅读并同意"
    val name1 = "《隐私政策》"
    val name2 = "《服务协议》"
    val name3 = "《未成年人个人信息保护规则》"
    val builder = SpannableStringBuilder(content + name1 + name2 + name3)
    // 设置《隐私政策》的前景色和点击事件
    builder.setSpan(
        ForegroundColorSpan(context.resources.getColor(R.color.theme_color)),
        content.length, content.length + name1.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    builder.setSpan(object : ClickableSpan() {
        override fun onClick(widget: View) {
            onLoginAgreementListener?.onPrivacyAgreementClick()
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = false
        }
    }, content.length, content.length + name1.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    // 设置《服务协议》的前景色和点击事件
    val start2 = content.length + name1.length
    builder.setSpan(
        ForegroundColorSpan(context.resources.getColor(R.color.theme_color)),
        start2, start2 + name2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    builder.setSpan(object : ClickableSpan() {
        override fun onClick(widget: View) {
            onLoginAgreementListener?.onUserAgreementClick()
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = false
        }
    }, start2, start2 + name2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    // 设置《未成年人个人信息保护规则》的前景色和点击事件
    val start3 = start2 + name2.length
    builder.setSpan(
        ForegroundColorSpan(context.resources.getColor(R.color.theme_color)),
        start3, start3 + name3.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    builder.setSpan(object : ClickableSpan() {
        override fun onClick(widget: View) {
            onLoginAgreementListener?.onMinorProtectClick()
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = false
        }
    }, start3, start3 + name3.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    return builder
}


interface OnLoginAgreementListener {
    fun onUserAgreementClick()
    fun onPrivacyAgreementClick()
    fun onMinorProtectClick()
}

fun TextView.setAgreement() {
    val linkTouchMovementMethod = LinkTouchMovementMethod()
//    this.setLinkTouchMovementMethod(linkTouchMovementMethod)
    this.movementMethod = linkTouchMovementMethod
    this.text = getRenderText(context, object : OnLoginAgreementListener {
        override fun onUserAgreementClick() {
            ToastUtil.showToast(context, "111")
        }

        override fun onPrivacyAgreementClick() {
            ToastUtil.showToast(context, "22")
        }

        override fun onMinorProtectClick() {
            ToastUtil.showToast(context, "333")
        }
    })

}