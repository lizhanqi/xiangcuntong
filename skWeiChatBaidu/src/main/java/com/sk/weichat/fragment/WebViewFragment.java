package com.sk.weichat.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.sk.weichat.R;
import com.sk.weichat.bean.OrderInfo;
import com.sk.weichat.bean.WebCallback;
import com.sk.weichat.bean.collection.CollectionEvery;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.ShareSdkHelper;
import com.sk.weichat.ui.account.AuthorDialog;
import com.sk.weichat.ui.base.EasyFragment;
import com.sk.weichat.ui.circle.range.SendShuoshuoActivity;
import com.sk.weichat.ui.message.InstantMessageActivity;
import com.sk.weichat.ui.tool.HtmlFactory;
import com.sk.weichat.ui.tool.JsSdkInterface;
import com.sk.weichat.ui.tool.WebViewActivity;
import com.sk.weichat.util.AppUtils;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.JsonUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.ComplaintDialog;
import com.sk.weichat.view.ExternalOpenDialog;
import com.sk.weichat.view.MatchKeyWordEditDialog;
import com.sk.weichat.view.ModifyFontSizeDialog;
import com.sk.weichat.view.PayDialog;
import com.sk.weichat.view.WebMoreDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.HttpUrl;

/**
 * web
 */
public class WebViewFragment extends EasyFragment {
    private static final String TAG = "WebViewFragment";
    private TextView mTitleTv;
    private ImageView mTitleRightIv;
    private ProgressBar mProgressBar;
    private WebView mWebView;
    private boolean isAnimStart = false;
    private int currentProgress;
    private String mUrl; // 缃戝潃URL
    private String mDownloadUrl;// ShareSdk 鍒嗕韩閾炬帴杩涙潵鐨勫簲鐢ㄤ笅杞藉湴鍧�(璺宠浆锛屽綋鏈湴涓嶅瓨鍦ㄥ搴斿簲鐢ㄦ椂浣跨敤)
    private JsSdkInterface jsSdkInterface;
    // js sdk璁剧疆鐨勫垎浜暟鎹紝
    private String shareBeanContent;

    @Override
    protected int inflateLayoutId() {
        return R.layout.activity_web_view;
    }

    @Override
    protected void onActivityCreated(Bundle savedInstanceState, boolean createView) {
        mUrl = coreManager.getConfig().homeAddress;
        initActionBar();
        init();
    }

    @Override
    public void onDestroy() {
        if (jsSdkInterface != null) {
            jsSdkInterface.release();
        }
        super.onDestroy();
    }

    private void initActionBar() {
        findViewById(R.id.iv_title_left).setVisibility(View.GONE);
        mTitleTv = findViewById(R.id.tv_title_center);
        mTitleRightIv = findViewById(R.id.iv_title_right);
        mTitleRightIv.setImageResource(R.drawable.chat_more);
        mTitleRightIv.setOnClickListener(this);
    }

    private void init() {
        initView();
        initClient();
        initEvent();

        int openStatus = openApp(mUrl);
        if (openStatus == 1) {// 璇ラ摼鎺ヤ负璺宠浆閾炬帴锛屾柟娉曞唴宸茶烦杞紝鐩存帴return
            requireActivity().finish();
        }
        if (openStatus == 2) {// 璇ラ摼鎺ヤ负璺宠浆閾炬帴锛屼絾鏈湴鏈畨瑁呰搴旂敤锛屽姞杞借搴旂敤涓嬭浇鍦板潃
            load(mWebView, mDownloadUrl);
        } else if (openStatus == 5) {// 璇ラ摼鎺ヤ负璺宠浆閾炬帴锛岃烦杞埌鏈湴鎺堟潈
        } else {// 0 | 3 | 4
            load(mWebView, mUrl);
        }
    }

    private void initView() {
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mWebView = (WebView) findViewById(R.id.mWebView);
        /* 璁剧疆鏀寔Js */
        mWebView.getSettings().setJavaScriptEnabled(true);
        /* 璁剧疆涓簍rue琛ㄧず鏀寔浣跨敤js鎵撳紑鏂扮殑绐楀彛 */
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        /* 璁剧疆缂撳瓨妯″紡 */
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.getSettings().setDomStorageEnabled(true);

        /* 璁剧疆涓轰娇鐢╳ebview鎺ㄨ崘鐨勭獥鍙� */
        mWebView.getSettings().setUseWideViewPort(true);
        /* 璁剧疆涓轰娇鐢ㄥ睆骞曡嚜閫傞厤 */
        mWebView.getSettings().setLoadWithOverviewMode(true);
        /* 璁剧疆鏄惁鍏佽webview浣跨敤缂╂斁鐨勫姛鑳�,鎴戣繖閲岃涓篺alse,涓嶅厑璁� */
        mWebView.getSettings().setBuiltInZoomControls(false);
        /* 鎻愰珮缃戦〉娓叉煋鐨勪紭鍏堢骇 */
        mWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

        /* HTML5鐨勫湴鐞嗕綅缃湇鍔�,璁剧疆涓簍rue,鍚敤鍦扮悊瀹氫綅 */
        mWebView.getSettings().setGeolocationEnabled(true);
        /* 璁剧疆鍙互璁块棶鏂囦欢 */
        mWebView.getSettings().setAllowFileAccess(true);

        // 缃戦〉鎾斁瑙嗛鏈夌敾闈㈡病澹伴煶<http://web.meiyanchat.com/aa.html>娣诲姞濡備笅浠ｇ爜
        // 鑷姩鎾斁缃戦〉闊充箰
        mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        // 璁剧疆UserAgent鏍囪瘑
        mWebView.getSettings().setUserAgentString(mWebView.getSettings().getUserAgentString() + " app-shikuimapp");
    }

    private void initClient() {
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setAlpha(1.0f);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.e(TAG, "shouldOverrideUrlLoading: " + url);
                if (url.startsWith("tel:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                int openStatus = openApp(url);
                if (openStatus == 1) {// 璇ラ摼鎺ヤ负璺宠浆閾炬帴锛屾柟娉曞唴宸茶烦杞紝鐩存帴return
                    return true;
                } else if (openStatus == 2) {// 璇ラ摼鎺ヤ负璺宠浆閾炬帴锛屼絾鏈湴鏈畨瑁呰搴旂敤锛屽姞杞借搴旂敤涓嬭浇鍦板潃
                    load(view, mDownloadUrl);
                } else if (openStatus == 5) {// 璇ラ摼鎺ヤ负璺宠浆閾炬帴锛� 璇ラ摼鎺ヤ负璺宠浆閾炬帴锛岃烦杞埌鏈湴鎺堟潈

                } else { // 0 | 3 | 4
                    load(view, url);
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });

        // 鑾峰彇缃戦〉鍔犺浇杩涘害
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                currentProgress = mProgressBar.getProgress();
                if (newProgress >= 100 && !isAnimStart) {
                    // 闃叉璋冪敤澶氭鍔ㄧ敾
                    isAnimStart = true;
                    mProgressBar.setProgress(newProgress);
                    // 寮�鍚睘鎬у姩鐢昏杩涘害鏉″钩婊戞秷澶�
                    startDismissAnimation(mProgressBar.getProgress());
                } else {
                    // 寮�鍚睘鎬у姩鐢昏杩涘害鏉″钩婊戦�掑
                    startProgressAnimation(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                mTitleTv.setText(title);
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                super.onReceivedIcon(view, icon);
            }
        });

        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                // 涓嶅鐞嗕笅杞斤紝鐩存帴鎶涘嚭鍘伙紝
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } catch (Exception ignored) {
                // 鏃犺濡備綍涓嶈宕╂簝锛屾瘮濡傛病鏈夋祻瑙堝櫒锛�
                ToastUtil.showToast(requireActivity(), R.string.download_error);
            }
        });

        jsSdkInterface = new JsSdkInterface(requireContext(), new MyJsSdkListener());
        mWebView.addJavascriptInterface(jsSdkInterface, "AndroidWebView");
    }

    private void initEvent() {
        mTitleRightIv.setOnClickListener(view -> {
            WebMoreDialog mWebMoreDialog = new WebMoreDialog(requireActivity(), getCurrentUrl(), true, new WebMoreDialog.BrowserActionClickListener() {
                @Override
                public void floatingWindow() {
                    // 棣栭〉娴獥鍔熻兘涓嶅彲鐢紝ui宸查殣钘�
                }

                @Override
                public void sendToFriend() {
                    forwardToFriend();
                }

                @Override
                public void shareToLifeCircle() {
                    shareMoment();
                }

                @Override
                public void collection() {
                    onCollection(getCurrentUrl());
                }

                @Override
                public void searchContent() {
                    search();
                }

                @Override
                public void copyLink() {
                    ClipboardManager clipboardManager = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setText(getCurrentUrl());
                    Toast.makeText(requireActivity(), getString(R.string.tip_copied_to_clipboard), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void openOutSide() {
                    ExternalOpenDialog externalOpenDialog = new ExternalOpenDialog(requireActivity(), getCurrentUrl());
                    externalOpenDialog.show();
                }

                @Override
                public void modifyFontSize() {
                    setWebFontSiz();
                }

                @Override
                public void refresh() {
                    mWebView.reload();
                }

                @Override
                public void complaint() {
                    report();
                }

                @Override
                public void shareWechat() {
                    String title = mTitleTv.getText().toString().trim();
                    String url = getCurrentUrl();
                    ShareSdkHelper.shareWechat(
                            requireActivity(), title, url, url
                    );
                }

                @Override
                public void shareWechatMoments() {
                    String title = mTitleTv.getText().toString().trim();
                    String url = getCurrentUrl();
                    ShareSdkHelper.shareWechatMoments(
                            requireActivity(), title, url, url
                    );
                }
            });
            mWebMoreDialog.show();
        });
    }

    /**
     * 鏍规嵁url璺宠浆鑷冲叾浠朼pp
     */
    private int openApp(String url) {
        if (TextUtils.isEmpty(url)) {
            return 0;
        }
        try {
            // 鍐呴儴鎺堟潈
            //  http://192.168.0.141:8080/websiteAuthorh/appAuth.html?appId=sk7c4fd05f92c7460a&callbackUrl=http://192.168.0.141:8080/websiteAuthorh/test.html
            if (url.contains("websiteAuthorh/index.html")) {
                String webAppName = WebViewActivity.URLRequest(url).get("webAppName");
                String webAppsmallImg = WebViewActivity.URLRequest(url).get("webAppsmallImg");
                String appId = WebViewActivity.URLRequest(url).get("appId");
                String redirectURL = WebViewActivity.URLRequest(url).get("callbackUrl");

                Log.e(TAG, "openApp: " + webAppName + "," + webAppsmallImg + "," + url);
                AuthorDialog dialog = new AuthorDialog(requireActivity());
                dialog.setDialogData(webAppName, webAppsmallImg);
                dialog.setmConfirmOnClickListener(new AuthorDialog.ConfirmOnClickListener() {
                    @Override
                    public void confirm() {
                        HttpUtils.get().url(coreManager.getConfig().AUTHOR_CHECK)
                                .params("appId", appId)
                                .params("state", coreManager.getSelfStatus().accessToken)
                                .params("callbackUrl", redirectURL)
                                .build().execute(new BaseCallback<WebCallback>(WebCallback.class) {

                            @Override
                            public void onResponse(ObjectResult<WebCallback> result) {
                                if (Result.checkSuccess(requireContext(), result) && result.getData() != null) {
                                    String html = HttpUrl.parse(result.getData().getCallbackUrl()).newBuilder()
                                            .addQueryParameter("code", result.getData().getCode())
                                            .build()
                                            .toString();
                                    load(mWebView, html);
                                }
                            }

                            @Override
                            public void onError(Call call, Exception e) {

                            }
                        });
                    }

                    @Override
                    public void AuthorCancel() {

                    }
                });

                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                return 5;
            }

            if (!url.startsWith("http") && !url.startsWith("https") && !url.startsWith("ftp")) {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();
                String scheme = uri.getScheme();
                // host 鍜� scheme 閮戒笉鑳戒负null
                if (!TextUtils.isEmpty(host) && !TextUtils.isEmpty(scheme)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    if (AppUtils.isSupportIntent(requireActivity(), intent)) {
                        startActivity(intent);
                        return 1;
                    } else {
                        return 2;
                    }
                }
            }
        } catch (Exception e) {
            return 3;
        }
        return 4;
    }

    private void load(WebView view, String url) {
        view.loadUrl(url);
    }

    /****************************************************
     * Start
     ***************************************************/
    private String getCurrentUrl() {
        if (mWebView == null) {
            // 鑷冲皯涓嶈兘宕╂簝锛屽垵濮嬪寲鍓嶆湁http璇锋眰锛屽彲鑳借�楁椂锛屾湡闂村叾浠栦唬鐮佽皟鐢ㄨ鏂规硶涓嶈兘宕╂簝锛�
            return "";
        }
        Log.e(TAG, mWebView.getUrl());
        String currentUrl = mWebView.getUrl();
        if (TextUtils.isEmpty(currentUrl)) {
            currentUrl = mUrl;
        }

        WebViewActivity.FLOATING_WINDOW_URL = currentUrl;

        if (currentUrl.contains("https://view.officeapps.live.com/op/view.aspx?src=")) {
            currentUrl = currentUrl.replace("https://view.officeapps.live.com/op/view.aspx?src=", "");
        }

        return currentUrl;
    }

    /**
     * 鍙戦�佺粰鏈嬪弸
     */
    private void initChatByUrl(String url) {
        String title = mTitleTv.getText().toString().trim();
        String content = JsonUtils.initJsonContent(title, getCurrentUrl(), url);
        initChatByContent(content, XmppMessage.TYPE_LINK);
    }

    private void initChatByContent(String content, int type) {
        String mLoginUserId = coreManager.getSelf().getUserId();

        ChatMessage message = new ChatMessage();
        message.setType(type);
        if (type == XmppMessage.TYPE_LINK) {
            message.setContent(content);
        } else if (type == XmppMessage.TYPE_SHARE_LINK) {
            message.setObjectId(content);
        } else {
            throw new IllegalStateException("鏈煡绫诲瀷: " + type);
        }
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setTimeSend(TimeUtils.sk_time_current_time());
        // Todo 灏嗗皝瑁呭ソ鐨勬秷鎭瓨鍏�10010 鍙风殑msg 琛ㄥ唴锛屽湪璺宠浆鑷宠浆鍙�->鑱婂ぉ鐣岄潰(璺宠浆浼犲�煎潎涓�10010鍙蜂笌msgId)锛屼箣鍚庡湪鑱婂ぉ鐣岄潰鍐呴�氳繃杩欎袱涓�兼煡璇㈠埌瀵圭敤娑堟伅锛屽彂閫�
        String mNewUserId = "10010";
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mNewUserId, message)) {
            Intent intent = new Intent(requireActivity(), InstantMessageActivity.class);
            intent.putExtra("fromUserId", mNewUserId);
            intent.putExtra("messageId", message.getPacketId());
            startActivity(intent);
            requireActivity().finish();
        } else {
            Toast.makeText(requireContext(), R.string.tip_message_wrap_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void forwardToFriend() {
        if (shareBeanContent != null) {
            initChatByContent(shareBeanContent, XmppMessage.TYPE_SHARE_LINK);
        } else {
            selectShareImage();
        }
    }

    private void selectShareImage() {
        String str = mWebView.getUrl();
        if (TextUtils.isEmpty(str)) {
            str = getCurrentUrl();
        }
        HtmlFactory.instance().queryImage(str, new HtmlFactory.DataListener<String>() {// 妫�绱㈣缃戦〉鍖呭惈鐨勫浘鐗�

            @Override
            public void onResponse(List<String> data, String title) {
                if (data != null && data.size() > 0) {
                    initChatByUrl(data.get(0));
                } else {
                    initChatByUrl("");
                }
            }

            @Override
            public void onError(String error) {
                initChatByUrl("");
            }
        });
    }

    /**
     * 鍒嗕韩鍒扮敓娲诲湀
     */
    private void shareMoment() {
        Intent intent = new Intent(requireContext(), SendShuoshuoActivity.class);
        intent.putExtra(Constants.BROWSER_SHARE_MOMENTS_CONTENT, getCurrentUrl());
        startActivity(intent);
    }

    /**
     * 鏀惰棌
     * 閾炬帴 褰撳仛 鏂囨湰绫诲瀷 鏀惰棌
     */
    private String collectionParam(String content) {
        com.alibaba.fastjson.JSONArray array = new com.alibaba.fastjson.JSONArray();
        com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
        int type = CollectionEvery.TYPE_TEXT;
        json.put("type", String.valueOf(type));
        String msg = "";
        String collectContent = "";
        msg = content;
        collectContent = content;
        json.put("msg", msg);
        json.put("collectContent", collectContent);
        json.put("collectType", -1);// 涓庢秷鎭棤鍏崇殑鏀惰棌
        array.add(json);
        return JSON.toJSONString(array);
    }

    private void onCollection(final String content) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("emoji", collectionParam(content));

        HttpUtils.get().url(coreManager.getConfig().Collection_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(requireContext(), getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(requireContext(), result.getResultMsg());
                        } else {
                            ToastUtil.showToast(requireContext(), R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(requireContext());
                    }
                });
    }

    /**
     * 鎼滅储椤甸潰鍐呭
     */
    private void search() {
        MatchKeyWordEditDialog matchKeyWordEditDialog = new MatchKeyWordEditDialog(requireContext(), mWebView);
        Window window = matchKeyWordEditDialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 杞敭鐩樺脊璧�
            matchKeyWordEditDialog.show();
        }
    }

    /**
     * 璋冩暣瀛椾綋
     */
    private void setWebFontSiz() {
        ModifyFontSizeDialog modifyFontSizeDialog = new ModifyFontSizeDialog(requireContext(), mWebView);
        modifyFontSizeDialog.show();
    }

    /**
     * 鎶曡瘔
     */
    private void report() {
        ComplaintDialog complaintDialog = new ComplaintDialog(requireActivity(), report -> {
            Map<String, String> params = new HashMap<>();
            params.put("access_token", coreManager.getSelfStatus().accessToken);
            params.put("webUrl", getCurrentUrl());
            params.put("reason", String.valueOf(report.getReportId()));
            DialogHelper.showDefaulteMessageProgressDialog(requireContext());

            HttpUtils.get().url(coreManager.getConfig().USER_REPORT)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {

                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            DialogHelper.dismissProgressDialog();
                            if (result.getResultCode() == 1) {
                                ToastUtil.showToast(requireActivity(), R.string.report_success);
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            DialogHelper.dismissProgressDialog();
                        }
                    });
        });
        complaintDialog.show();
    }

    /****************************************************
     * End
     ***************************************************/

    /**
     * progressBar閫掑鍔ㄧ敾
     */
    private void startProgressAnimation(int newProgress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(mProgressBar, "progress", currentProgress, newProgress);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    /**
     * progressBar娑堝け鍔ㄧ敾
     */
    private void startDismissAnimation(final int progress) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(mProgressBar, "alpha", 1.0f, 0.0f);
        anim.setDuration(1500);  // 鍔ㄧ敾鏃堕暱
        anim.setInterpolator(new DecelerateInterpolator());
        // 鍏抽敭, 娣诲姞鍔ㄧ敾杩涘害鐩戝惉鍣�
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fraction = valueAnimator.getAnimatedFraction();      // 0.0f ~ 1.0f
                int offset = 100 - progress;
                mProgressBar.setProgress((int) (progress + offset * fraction));
            }
        });

        anim.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                // 鍔ㄧ敾缁撴潫
                mProgressBar.setProgress(0);
                mProgressBar.setVisibility(View.GONE);
                isAnimStart = false;
            }
        });
        anim.start();
    }

    private class MyJsSdkListener implements JsSdkInterface.Listener {

        @Override
        public void onFinishPlay(String path) {
            mWebView.evaluateJavascript("playFinish()", value -> {
            });
        }

        @Override
        public void onUpdateShareData(String shareBeanContent) {
            WebViewFragment.this.shareBeanContent = shareBeanContent;
        }

        @Override
        public void onChooseSKPayInApp(String appId, String prepayId, String sign) {
            DialogHelper.showDefaulteMessageProgressDialog(requireContext());
            Map<String, String> params = new HashMap<String, String>();
            params.put("access_token", coreManager.getSelfStatus().accessToken);
            params.put("appId", appId);
            params.put("prepayId", prepayId);
            params.put("sign", sign);

            // 鑾峰彇璁㈠崟淇℃伅
            HttpUtils.get().url(coreManager.getConfig().PAY_GET_ORDER_INFO)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<OrderInfo>(OrderInfo.class) {

                        @Override
                        public void onResponse(ObjectResult<OrderInfo> result) {
                            DialogHelper.dismissProgressDialog();
                            if (result.getResultCode() == 1 && result.getData() != null) {
                                PayDialog payDialog = new PayDialog(requireContext(), appId, prepayId, sign, result.getData(), new PayDialog.PayResultListener() {
                                    @Override
                                    public void payResult(String result) {
                                        mWebView.loadUrl("javascript:sk.paySuccess(" + result + ")");
                                    }
                                });
                                payDialog.show();
                            }
                        }

                        @Override
                        public void onError(Call call, Exception e) {
                            DialogHelper.dismissProgressDialog();
                        }
                    });
        }

    }
}