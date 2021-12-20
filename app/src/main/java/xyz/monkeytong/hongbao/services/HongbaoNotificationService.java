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
import android.os.Looper;
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

    public static final String ACTION_STATE_CHANGE = "com.android.example.notificationlistener.STATE";


    private static boolean sConnected;
    private static HongbaoNotificationService sService;

    public static boolean isConnected() {
        return sConnected;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void toggleSnooze(Context context) {
        if (sConnected) {
            Log.d(TAG, "trying to snooze");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    if (sService != null) {
                        sService.requestUnbind();
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, "failed to unbind service", e);
                }
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
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.w(TAG, "onListenerConnected: ");
        sConnected = true;
        sService = this;
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_STATE_CHANGE));
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        sConnected = false;
        sService = null;
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_STATE_CHANGE));
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        super.onNotificationPosted(sbn, rankingMap);
        Log.w(TAG, "onNotificationPosted: " + sbn.getKey());
        if (PACKAGE_NAME.equals(sbn.getPackageName())) {
            CharSequence ticker = sbn.getNotification().tickerText;
            if (ticker != null && ticker.toString().contains(HongbaoService.WECHAT_NOTIFICATION_TIP)) {
                clickNotification(sbn);
            }
        }

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
        super.onNotificationRemoved(sbn, rankingMap);
        Log.w(TAG, "onNotificationRemoved: " + sbn.getKey());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sConnected = false;
        sService = null;
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_STATE_CHANGE));
    }
}
