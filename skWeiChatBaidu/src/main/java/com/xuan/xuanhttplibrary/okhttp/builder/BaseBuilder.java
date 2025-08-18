package com.xuan.xuanhttplibrary.okhttp.builder;


import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.sk.weichat.BuildConfig;
import com.sk.weichat.MyApplication;
import com.sk.weichat.helper.LoginSecureHelper;
import com.sk.weichat.util.LogUtils;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author Administrator
 * @time 2017/3/30 0:14
 * @des ${TODO}
 */

public abstract class BaseBuilder {
    private static final Object accessTokenLock = new Object();
    private static boolean accessTokenRefreshing = false;
    private static Queue<Runnable> accessTokenRefreshCallbackList = new LinkedList<>();

    @NonNull
    protected final Map<String, String> params = new LinkedHashMap<>();
    protected String url;
    protected Object tag;
    protected Request build;
    protected IOException crashed;
    private boolean mac;
    private Boolean beforeLogin;
    private boolean delayBuild;

    public static void addAccessTokenRefreshCallback(Runnable runnable) {
        accessTokenRefreshCallbackList.offer(runnable);
    }

    private static void callOnAccessTokenRefreshed() {
        accessTokenRefreshing = false;
        while (!accessTokenRefreshCallbackList.isEmpty()) {
            Runnable callback = accessTokenRefreshCallbackList.remove();
            callback.run();
        }
    }

    protected String getUserAgent() {
        StringBuilder result = new StringBuilder(64);
        result.append("shiku_im/");
        result.append(BuildConfig.VERSION_NAME); // such as 1.1.0
        result.append(" (Linux; U; Android ");

        String version = Build.VERSION.RELEASE; // "1.0" or "3.4b5"
        result.append(version.length() > 0 ? version : "1.0");

        // add the model for the release build
        if ("REL".equals(Build.VERSION.CODENAME)) {
            String model = Build.MODEL;
            if (model.length() > 0) {
                result.append("; ");
                result.append(model);
            }
        }
        String id = Build.ID; // "MASTER" or "M4-rc20"
        if (id.length() > 0) {
            result.append(" Build/");
            result.append(id);
        }
        result.append(")");
        return result.toString();
    }

    public abstract BaseBuilder url(String url);

    public abstract BaseBuilder tag(Object tag);

    public BaseCall build() {
        return build(true);
    }

    /**
     * 是否需要添加验参，
     *
     * @param mac 是否添加验参，
     */
    public BaseCall build(boolean mac) {
        return build(mac, false);
    }

    /**
     * @param mac         是否添加验参，
     * @param beforeLogin 是否强制按登录前接口添加验参，true无视accessToken按登录前添加验参，false表示登录后添加验参，
     */
    public BaseCall build(boolean mac, Boolean beforeLogin) {
        this.mac = mac;
        this.beforeLogin = beforeLogin;
        String language = Locale.getDefault().getLanguage();
        if (TextUtils.equals(language.toLowerCase(), "tw")) {// 繁体服务端要求传big5
            language = "big5";
        }
        params("language", language);
        if (mac) {
            if (isAccessTokenRefreshing()) {
                delayBuild = true;
            } else {
                addMac(beforeLogin);
            }
        }
        return abstractBuild();
    }

    public abstract BaseCall abstractBuild();

    public abstract BaseBuilder params(String k, String v);

    /**
     * 给所有接口调添加Mac,
     */
    public BaseBuilder addMac(Boolean beforeLogin) {
        LoginSecureHelper.generateHttpParam(MyApplication.getContext(), params, beforeLogin);
        return this;
    }

    public boolean isAccessTokenRefreshing() {
        if (accessTokenRefreshing) {
            synchronized (accessTokenLock) {
                if (accessTokenRefreshing) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkCrashed(Callback callback) {
        if (crashed != null) {
            // 这里参数call只能是null, url异常没法创建call,
            callback.onFailure(null, crashed);
            return true;
        }
        return false;
    }

    public class BaseCall {
        public void execute(Callback callback) {
            if (checkCrashed(callback)) {
                return;
            }
            if (delayBuild) {
                if (isAccessTokenRefreshing()) {
                    addAccessTokenRefreshCallback(() -> {
                        build(mac, beforeLogin);
                        LogUtils.log("HTTP", "rebuild " + url);
                        OkHttpClient mOkHttpClient = HttpUtils.getInstance().getOkHttpClient();
                        mOkHttpClient.newCall(build).enqueue(callback);
                    });
                    return;
                } else {
                    build(mac, beforeLogin);
                    LogUtils.log("HTTP", "rebuild " + url);
                }
            }
            OkHttpClient mOkHttpClient = HttpUtils.getInstance().getOkHttpClient();
            mOkHttpClient.newCall(build).enqueue(callback);
        }

        /**
         * 当前线程同步执行http调用，
         * 用以实现一些场景一个线程调用多次http请求，
         * 因此该方法不能在主线程调用，
         */
        @WorkerThread
        public void executeSync(Callback callback) {
            if (checkCrashed(callback)) {
                return;
            }
            if (delayBuild) {
                if (isAccessTokenRefreshing()) {
                    Object current = new Object();
                    addAccessTokenRefreshCallback(() -> {
                        build(mac, beforeLogin);
                        LogUtils.log("HTTP", "rebuild " + url);
                        synchronized (current) {
                            current.notify();
                        }
                    });
                    synchronized (current) {
                        try {
                            current.wait();
                        } catch (InterruptedException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                } else {
                    build(mac, beforeLogin);
                    LogUtils.log("HTTP", "rebuild " + url);
                }
            }
            OkHttpClient mOkHttpClient = HttpUtils.getInstance().getOkHttpClient();
            Call call = mOkHttpClient.newCall(build);
            try {
                Response response = call.execute();
                callback.onResponse(call, response);
            } catch (IOException e) {
                callback.onFailure(call, e);
            }
        }

        @WorkerThread
        public void refreshAccessTokenSync(Callback callback) {
            if (checkCrashed(callback)) {
                return;
            }
            synchronized (accessTokenLock) {
                accessTokenRefreshing = true;
            }
            OkHttpClient mOkHttpClient = HttpUtils.getInstance().getOkHttpClient();
            Call call = mOkHttpClient.newCall(build);
            try {
                Response response = call.execute();
                callback.onResponse(call, response);
            } catch (IOException e) {
                callback.onFailure(call, e);
            }
            synchronized (accessTokenLock) {
                callOnAccessTokenRefreshed();
            }
        }
    }
}
