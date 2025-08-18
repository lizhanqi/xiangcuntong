package com.fanjun.keeplive.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.fanjun.keeplive.utils.ServiceUtils;
import com.sk.weichat.R;

/**
 * 定时器
 * 安卓5.0及以上
 */
@SuppressWarnings(value = {"unchecked", "deprecation"})
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public final class JobHandlerService extends JobService {
    private JobScheduler mJobScheduler;
    private int jobId = 100;

    private NotificationManager mNotificationManager;
    private String channelId = "channelId1";//渠道id
    private Notification.Builder builder;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startService(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            mJobScheduler.cancel(jobId);
            JobInfo.Builder builder = new JobInfo.Builder(jobId,
                    new ComponentName(getPackageName(), JobHandlerService.class.getName()));
            if (Build.VERSION.SDK_INT >= 24) {
                builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS); //执行的最小延迟时间
                builder.setOverrideDeadline(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);  //执行的最长延时时间
                builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
                builder.setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR);//线性重试方案
            } else {
                builder.setPeriodic(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
            }
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            builder.setRequiresCharging(true); // 当插入充电器，执行该任务
            mJobScheduler.schedule(builder.build());
        }
        return START_STICKY;
    }

    private void startService(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            int importance = NotificationManager.IMPORTANCE_MIN; //重要性级别 开启通知，不会弹出，但没有提示音，状态栏中无显示
            //第二个参数与channelId对应
            // 不创建这个渠道，就弹不出这个通知，测试不影响进程优先级，
            builder = new Notification.Builder(this, channelId);
            builder.setContentTitle(getString(R.string.app_name));
            builder.setContentText("");
            builder.setDefaults(Notification.DEFAULT_ALL);
            builder.setAutoCancel(true);
            builder.setShowWhen(true);//时间是否显示
            builder.setSmallIcon(android.R.drawable.stat_notify_chat);
            mNotificationManager.notify(13691, builder.build());
            startForeground(13691, builder.build());
        }
        //启动本地服务
        Intent localIntent = new Intent(context, LocalService.class);
        //启动守护进程
        Intent guardIntent = new Intent(context, RemoteService.class);
        startService(localIntent);
        startService(guardIntent);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (!ServiceUtils.isServiceRunning(getApplicationContext(), "com.fanjun.keeplive.service.LocalService") || !ServiceUtils.isRunningTaskExist(getApplicationContext(), getPackageName() + ":remote")) {
            startService(this);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (!ServiceUtils.isServiceRunning(getApplicationContext(), "com.fanjun.keeplive.service.LocalService") || !ServiceUtils.isRunningTaskExist(getApplicationContext(), getPackageName() + ":remote")) {
            startService(this);
        }
        return false;
    }
}
