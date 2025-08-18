package com.sk.weichat.ui.tool;

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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.bean.OrderInfo;
import com.sk.weichat.bean.collection.CollectionEvery;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.helper.DialogHelper;
import com.sk.weichat.helper.ShareSdkHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.circle.range.SendShuoshuoActivity;
import com.sk.weichat.ui.message.InstantMessageActivity;
import com.sk.weichat.ui.message.MucChatActivity;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;

/**
 * web
 */
public class WebViewRoomActivity extends BaseActivity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_DOWNLOAD_URL = "download_url";
    private static final String TAG = "WebViewActivity";
    public static String FLOATING_WINDOW_URL;
    private TextView mTitleTv;
    private ImageView mTitleLeftIv;
    private ImageView mTitleRightIv;
    private ProgressBar mProgressBar;
    private WebView mWebView;
    private boolean isAnimStart = false;
    private int currentProgress;
    private String mName; // 网站名字，无论跳转多少网页都显示这个名字
    private String mUrl; // 网址URL
    private String mDownloadUrl;// ShareSdk 分享链接进来的应用下载地址(跳转，当本地不存在对应应用时使用)
    private JsSdkInterface jsSdkInterface;
    // js sdk设置的分享数据，
    private String shareBeanContent;
    // 群助手 交互的数据
    private String mShareParams;
    private FrameLayout frameLayout;
    private String mKey;

    public static void start(Context ctx, String name, String url, String key) {
        Intent intent = new Intent(ctx, WebViewRoomActivity.class);
        intent.putExtra("name", name);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra("key", key);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view_room);
        mName = getIntent().getStringExtra("name");
        mUrl = getIntent().getStringExtra(EXTRA_URL);
        mKey = getIntent().getStringExtra("key");
        mDownloadUrl = getIntent().getStringExtra(EXTRA_DOWNLOAD_URL);
        mShareParams = getIntent().getStringExtra("shareParams");
        initActionBar();
        initView();
        initClient();
        initEvent();
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (jsSdkInterface != null) {
            jsSdkInterface.release();
        }
        frameLayout.removeAllViews();
        super.onDestroy();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(view -> finish());
        mTitleTv = findViewById(R.id.tv_title_center);
        mTitleTv.setText(mName);
        mTitleLeftIv = findViewById(R.id.iv_title_left);
        mTitleLeftIv.setImageResource(R.drawable.icon_close);
        mTitleRightIv = findViewById(R.id.iv_title_right);
        mTitleRightIv.setImageResource(R.drawable.chat_more);
    }

    private void init() {
        int openStatus = openApp(mUrl);
        if (openStatus == 1) {// 该链接为跳转链接，方法内已跳转，直接return
            finish();
        } else if (openStatus == 2) {// 该链接为跳转链接，但本地未安装该应用，加载该应用下载地址
            load(mWebView, mDownloadUrl);
        } else if (openStatus == 5) {// 该链接为跳转链接，跳转到本地授权

        } else {// 0 | 3 | 4
            load(mWebView, mUrl);
        }
    }

    private void initView() {
        frameLayout = findViewById(R.id.fl_web);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mWebView = MucChatActivity.webViewList.get(mKey);
        if (mWebView == null) {
            mWebView = new WebView(mContext);
            /* 设置支持Js */
            mWebView.getSettings().setJavaScriptEnabled(true);
            /* 设置为true表示支持使用js打开新的窗口 */
            mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

            /* 设置缓存模式 */
            mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            mWebView.getSettings().setDomStorageEnabled(true);

            /* 设置为使用webview推荐的窗口 */
            mWebView.getSettings().setUseWideViewPort(true);
            /* 设置为使用屏幕自适配 */
            mWebView.getSettings().setLoadWithOverviewMode(true);
            /* 设置是否允许webview使用缩放的功能,我这里设为false,不允许 */
            mWebView.getSettings().setBuiltInZoomControls(false);
            /* 提高网页渲染的优先级 */
            mWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

            /* HTML5的地理位置服务,设置为true,启用地理定位 */
            mWebView.getSettings().setGeolocationEnabled(true);
            /* 设置可以访问文件 */
            mWebView.getSettings().setAllowFileAccess(true);

            // 网页播放视频有画面没声音<http://web.meiyanchat.com/aa.html>添加如下代码
            // 自动播放网页音乐
            mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);

            // 设置UserAgent标识
            mWebView.getSettings().setUserAgentString(mWebView.getSettings().getUserAgentString() + " app-shikuimapp");

            init();
        }
        frameLayout.addView(mWebView);
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
                if (openStatus == 1) {// 该链接为跳转链接，方法内已跳转，直接return
                    return true;
                } else if (openStatus == 2) {// 该链接为跳转链接，但本地未安装该应用，加载该应用下载地址
                    load(view, mDownloadUrl);
                } else if (openStatus == 5) {// 该链接为跳转链接， 该链接为跳转链接，跳转到本地授权

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

        // 获取网页加载进度
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                currentProgress = mProgressBar.getProgress();
                if (newProgress >= 100 && !isAnimStart) {
                    // 防止调用多次动画
                    isAnimStart = true;
                    mProgressBar.setProgress(newProgress);
                    // 开启属性动画让进度条平滑消失
                    startDismissAnimation(mProgressBar.getProgress());
                } else {
                    // 开启属性动画让进度条平滑递增
                    startProgressAnimation(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                // mTitleTv.setText(title);
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                super.onReceivedIcon(view, icon);
            }
        });

        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                // 不处理下载，直接抛出去，
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } catch (Exception ignored) {
                // 无论如何不要崩溃，比如没有浏览器，
                ToastUtil.showToast(WebViewRoomActivity.this, R.string.download_error);
            }
        });

        jsSdkInterface = new JsSdkInterface(this, new MyJsSdkListener());
        jsSdkInterface.setShareParams(mShareParams);
        mWebView.addJavascriptInterface(jsSdkInterface, "AndroidWebView");
    }

    private void initEvent() {
        mTitleRightIv.setOnClickListener(view -> {
            WebMoreDialog mWebMoreDialog = new WebMoreDialog(mContext, getCurrentUrl(), true, new WebMoreDialog.BrowserActionClickListener() {
                @Override
                public void floatingWindow() {
                    // 首页浮窗功能不可用，ui已隐藏
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
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setText(getCurrentUrl());
                    Toast.makeText(mContext, getString(R.string.tip_copied_to_clipboard), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void openOutSide() {
                    ExternalOpenDialog externalOpenDialog = new ExternalOpenDialog(mContext, getCurrentUrl());
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
                            mContext, title, url, url
                    );
                }

                @Override
                public void shareWechatMoments() {
                    String title = mTitleTv.getText().toString().trim();
                    String url = getCurrentUrl();
                    ShareSdkHelper.shareWechatMoments(
                            mContext, title, url, url
                    );
                }
            });
            mWebMoreDialog.show();
        });
    }

    /**
     * 根据url跳转至其他app
     */
    private int openApp(String url) {
        if (TextUtils.isEmpty(url)) {
            return 0;
        }
        try {
            if (!url.startsWith("http") && !url.startsWith("https") && !url.startsWith("ftp")) {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();
                String scheme = uri.getScheme();
                // host 和 scheme 都不能为null
                if (!TextUtils.isEmpty(host) && !TextUtils.isEmpty(scheme)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    if (AppUtils.isSupportIntent(this, intent)) {
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
        MucChatActivity.webViewList.put(mKey, view);
    }

    /****************************************************
     * Start
     ***************************************************/
    private String getCurrentUrl() {
        if (mWebView == null) {
            // 至少不能崩溃，初始化前有http请求，可能耗时，期间其他代码调用该方法不能崩溃，
            return "";
        }
        Log.e(TAG, mWebView.getUrl());
        String currentUrl = mWebView.getUrl();
        if (TextUtils.isEmpty(currentUrl)) {
            currentUrl = mUrl;
        }

        FLOATING_WINDOW_URL = currentUrl;

        if (currentUrl.contains("https://view.officeapps.live.com/op/view.aspx?src=")) {
            currentUrl = currentUrl.replace("https://view.officeapps.live.com/op/view.aspx?src=", "");
        }

        return currentUrl;
    }

    /**
     * 发送给朋友
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
            throw new IllegalStateException("未知类型: " + type);
        }
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setTimeSend(TimeUtils.sk_time_current_time());
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, AppConstant.NORMAL_INSTANT_ID, message)) {
            Intent intent = new Intent(WebViewRoomActivity.this, InstantMessageActivity.class);
            intent.putExtra("fromUserId", AppConstant.NORMAL_INSTANT_ID);
            intent.putExtra("messageId", message.getPacketId());
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(mContext, R.string.tip_message_wrap_failed, Toast.LENGTH_SHORT).show();
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
        HtmlFactory.instance().queryImage(str, new HtmlFactory.DataListener<String>() {// 检索该网页包含的图片

            @Override
            public void onResponse(List<String> data, String title) {
                if (data != null && data.size() > 0) {
                    String url = "";
                    for (int i = 0; i < data.size(); i++) {
                        if (!TextUtils.isEmpty(data.get(i))) {
                            url = data.get(i);
                            break;
                        }
                    }
                    if (!TextUtils.isEmpty(url)) { // 兼容各种情况
                        if (url.contains("http") && url.contains("com")) {
                            // 解析到正常的图片地址
                        } else if (url.contains("com")) {
                            // ex：m.baidu.com/se/static/img/iphone/logo_web.png
                            url = "https:" + url;
                        } else {
                            // ex：img/logo.png
                            if (!TextUtils.isEmpty(mWebView.getOriginalUrl())) {
                                String prefix = mWebView.getOriginalUrl().substring(0, mWebView.getOriginalUrl().lastIndexOf("/"));
                                url = prefix + "/" + url;
                            }
                        }
                    }
                    initChatByUrl(url);
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
     * 分享到生活圈
     */
    private void shareMoment() {
        Intent intent = new Intent(WebViewRoomActivity.this, SendShuoshuoActivity.class);
        intent.putExtra(Constants.BROWSER_SHARE_MOMENTS_CONTENT, getCurrentUrl());
        startActivity(intent);
    }

    /**
     * 收藏
     * 链接 当做 文本类型 收藏
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
        json.put("collectType", -1);// 与消息无关的收藏
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
                            Toast.makeText(mContext, getString(R.string.collection_success), Toast.LENGTH_SHORT).show();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    /**
     * 搜索页面内容
     */
    private void search() {
        MatchKeyWordEditDialog matchKeyWordEditDialog = new MatchKeyWordEditDialog(this, mWebView);
        Window window = matchKeyWordEditDialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);// 软键盘弹起
            matchKeyWordEditDialog.show();
        }
    }

    /**
     * 调整字体
     */
    private void setWebFontSiz() {
        ModifyFontSizeDialog modifyFontSizeDialog = new ModifyFontSizeDialog(this, mWebView);
        modifyFontSizeDialog.show();
    }

    /**
     * 投诉
     */
    private void report() {
        ComplaintDialog complaintDialog = new ComplaintDialog(this, report -> {
            Map<String, String> params = new HashMap<>();
            params.put("access_token", coreManager.getSelfStatus().accessToken);
            params.put("webUrl", getCurrentUrl());
            params.put("reason", String.valueOf(report.getReportId()));
            DialogHelper.showDefaulteMessageProgressDialog(WebViewRoomActivity.this);

            HttpUtils.get().url(coreManager.getConfig().USER_REPORT)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<Void>(Void.class) {

                        @Override
                        public void onResponse(ObjectResult<Void> result) {
                            DialogHelper.dismissProgressDialog();
                            if (result.getResultCode() == 1) {
                                ToastUtil.showToast(WebViewRoomActivity.this, R.string.report_success);
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
     * progressBar递增动画
     */
    private void startProgressAnimation(int newProgress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(mProgressBar, "progress", currentProgress, newProgress);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    /**
     * progressBar消失动画
     */
    private void startDismissAnimation(final int progress) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(mProgressBar, "alpha", 1.0f, 0.0f);
        anim.setDuration(1500);  // 动画时长
        anim.setInterpolator(new DecelerateInterpolator());
        // 关键, 添加动画进度监听器
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
                // 动画结束
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
            WebViewRoomActivity.this.shareBeanContent = shareBeanContent;
        }

        @Override
        public void onChooseSKPayInApp(String appId, String prepayId, String sign) {
            DialogHelper.showDefaulteMessageProgressDialog(mContext);
            Map<String, String> params = new HashMap<String, String>();
            params.put("access_token", coreManager.getSelfStatus().accessToken);
            params.put("appId", appId);
            params.put("prepayId", prepayId);
            params.put("sign", sign);

            // 获取订单信息
            HttpUtils.get().url(coreManager.getConfig().PAY_GET_ORDER_INFO)
                    .params(params)
                    .build()
                    .execute(new BaseCallback<OrderInfo>(OrderInfo.class) {

                        @Override
                        public void onResponse(ObjectResult<OrderInfo> result) {
                            DialogHelper.dismissProgressDialog();
                            if (result.getResultCode() == 1 && result.getData() != null) {
                                PayDialog payDialog = new PayDialog(mContext, appId, prepayId, sign, result.getData(), new PayDialog.PayResultListener() {
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
