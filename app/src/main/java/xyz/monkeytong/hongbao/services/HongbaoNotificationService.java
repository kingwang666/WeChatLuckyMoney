package xyz.monkeytong.hongbao.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Created on 2020/1/13
 * Author: bigwang
 * Description:
 */
public class HongbaoNotificationService extends NotificationListenerService {
    private static final String TAG = "NotificationListener";

    private static final String PACKAGE_NAME = "com.tencent.mm";

    // Message tags
    private static final int MSG_NOTIFY = 1;
    private static final int MSG_STARTUP = 2;
    private static final int MSG_SNOOZE = 3;

    public static final String ACTION_STATE_CHANGE = "com.android.example.notificationlistener.STATE";


    private static boolean sConnected;

    public static boolean isConnected() {
        return sConnected;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void toggleSnooze(Context context) {
        if (sConnected) {
            Log.d(TAG, "scheduling snooze");
            if (sHandler != null) {
                sHandler.sendEmptyMessage(MSG_SNOOZE);
            }
        } else {
            Log.d(TAG, "trying to unsnooze");
            try {
                requestRebind(ComponentName.createRelative(context.getPackageName(), HongbaoNotificationService.class.getCanonicalName()));
            } catch (RuntimeException e) {
                Log.e(TAG, "failed to rebind service", e);
            }
        }
    }

    private static Handler sHandler;


    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate() {
        super.onCreate();
        sHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MSG_NOTIFY:
                        StatusBarNotification sbn = (StatusBarNotification) msg.obj;
                        if (PACKAGE_NAME.equals(sbn.getPackageName())){
                            CharSequence ticker = sbn.getNotification().tickerText;
                            if (ticker != null && ticker.toString().contains(HongbaoService.WECHAT_NOTIFICATION_TIP)){
                                clickNotification(sbn);
                            }
                        }

                        break;
                    case MSG_STARTUP:
                        sConnected = true;
                        LocalBroadcastManager.getInstance(HongbaoNotificationService.this).sendBroadcast(new Intent(ACTION_STATE_CHANGE));
                        break;
                    case MSG_SNOOZE:
                        Log.d(TAG, "trying to snooze");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            try {
                                requestUnbind();
                            } catch (RuntimeException e) {
                                Log.e(TAG, "failed to unbind service", e);
                            }
                        }
                        break;
                }
            }
        };
    }

    private void clickNotification(StatusBarNotification sbn) {
        if (sbn.getNotification().contentIntent != null) {
            try {
                sbn.getNotification().contentIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.d(TAG, "failed to send intent for " + sbn.getKey(), e);
            }
        }
        if ((sbn.getNotification().flags & Notification.FLAG_AUTO_CANCEL) != 0) {
            cancelNotification(sbn.getKey());
        }

    }

    @Override
    public void onDestroy() {
        sConnected = false;
        LocalBroadcastManager.getInstance(HongbaoNotificationService.this)
                .sendBroadcast(new Intent(ACTION_STATE_CHANGE));
        sHandler = null;
        super.onDestroy();
    }

    @Override
    public void onListenerConnected() {
        Log.w(TAG, "onListenerConnected: ");
        Message.obtain(sHandler, MSG_STARTUP).sendToTarget();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        Log.w(TAG, "onNotificationPosted: " + sbn.getKey());
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences != null && !sharedPreferences.getBoolean("pref_watch_notification", false)){
            return;
        }
        Message.obtain(sHandler, MSG_NOTIFY, sbn).sendToTarget();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
        Log.w(TAG, "onNotificationRemoved: " + sbn.getKey());
    }
}
