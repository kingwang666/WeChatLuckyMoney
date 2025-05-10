package xyz.monkeytong.hongbao.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import xyz.monkeytong.hongbao.R;

/**
 * Author: wangxiaojie6
 * Date: 2019/1/25
 */
public class KeepAliveService extends Service {

    public final static int NOTIFICATION_ID = 1001;
    private final static String CHANNEL_ID = "RedPacket";
    private final static String CHANNEL_NAME = "红包助手";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        boolean isPaused = intent.getBooleanExtra(HongbaoBroadcastReceiver.EXTRA_STATUS, false);
        startForeground(NOTIFICATION_ID, buildNotification(this, isPaused));
        return START_STICKY;
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.setSound(null, null);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
        channel.enableLights(false);
        channel.enableVibration(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            channel.setAllowBubbles(false);
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }

    public static Notification buildNotification(Context context, boolean isPaused) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context);
        }
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("红包助手")
                .setContentText("红包助手已启动")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setAutoCancel(false)
                .setOngoing(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setContentIntent(PendingIntent.getActivity(context, 100, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE));
        } else {
            builder.setContentIntent(PendingIntent.getActivity(context, 100, launch, PendingIntent.FLAG_UPDATE_CURRENT));
        }

        Intent statusIntent = new Intent();
        statusIntent.setPackage(context.getPackageName());
        statusIntent.setAction(HongbaoBroadcastReceiver.ACTION_FEATURE_STATUS);
        if (isPaused) {
            statusIntent.putExtra(HongbaoBroadcastReceiver.EXTRA_STATUS, false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.addAction(R.drawable.ic_play_circle_outline_24, "继续", PendingIntent.getBroadcast(context, 0, statusIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            } else {
                builder.addAction(R.drawable.ic_play_circle_outline_24, "继续", PendingIntent.getBroadcast(context, 0, statusIntent, PendingIntent.FLAG_UPDATE_CURRENT));
            }
        } else {
            statusIntent.putExtra(HongbaoBroadcastReceiver.EXTRA_STATUS, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.addAction(R.drawable.ic_pause_circle_outline_24, "暂停", PendingIntent.getBroadcast(context, 0, statusIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            } else {
                builder.addAction(R.drawable.ic_pause_circle_outline_24, "暂停", PendingIntent.getBroadcast(context, 0, statusIntent, PendingIntent.FLAG_UPDATE_CURRENT));
            }
        }

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
