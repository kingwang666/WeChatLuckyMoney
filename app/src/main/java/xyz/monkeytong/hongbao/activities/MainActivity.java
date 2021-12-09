package xyz.monkeytong.hongbao.activities;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;

import xyz.monkeytong.hongbao.HBApplication;
import xyz.monkeytong.hongbao.R;
import xyz.monkeytong.hongbao.services.HongbaoNotificationService;
import xyz.monkeytong.hongbao.services.KeepAliveService;
import xyz.monkeytong.hongbao.utils.ConnectivityUtil;
import xyz.monkeytong.hongbao.utils.UpdateTask;


public class MainActivity extends AppCompatActivity implements AccessibilityManager.AccessibilityStateChangeListener {

    //开关切换按钮
    private TextView pluginStatusText;
    private ImageView pluginStatusIcon;

    private TextView notifitionLaunchText;
    private ImageView notifitionLaunchIcon;

    private View snoozeView;
    private TextView notifitionSnoozeText;
    private ImageView notifitionSnoozeIcon;
    //AccessibilityService 管理
    private AccessibilityManager accessibilityManager;
    private OnePixelReceiver mReceiver;

    private final BroadcastReceiver mStateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateNoticeService();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        pluginStatusText = findViewById(R.id.layout_control_accessibility_text);
        pluginStatusIcon = findViewById(R.id.layout_control_accessibility_icon);

        notifitionLaunchText = findViewById(R.id.layout_notification_launch_text);
        notifitionLaunchIcon = findViewById(R.id.layout_notification_launch_icon);

        snoozeView = findViewById(R.id.layout_snooze_notification);
        notifitionSnoozeText = findViewById(R.id.layout_snooze_notification_text);
        notifitionSnoozeIcon = findViewById(R.id.layout_snooze_notification_icon);

        explicitlyLoadPreferences();

        //监听AccessibilityService 变化
        accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        accessibilityManager.addAccessibilityStateChangeListener(this);
        updateHongbaoServiceStatus();
        if (mReceiver == null) {
            mReceiver = new OnePixelReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            registerReceiver(mReceiver, filter);
        }

        pluginStatusText.postDelayed(new Runnable() {
            @Override
            public void run() {
                NotificationManagerCompat manager = NotificationManagerCompat.from(MainActivity.this);
                if (!manager.areNotificationsEnabled()) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("通知")
                            .setMessage("未获取到通知权限。是否给予权限？")
                            .setNegativeButton("否", null)
                            .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setNotificationEnabled();
                                }
                            })
                            .show();
                }
            }
        }, 500);
    }

    private void setNotificationEnabled() {
        try {
            // 根据isOpened结果，判断是否需要提醒用户跳转AppInfo页面，去打开App通知权限
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                //这种方案适用于 API 26, 即8.0（含8.0）以上可以用
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, getApplicationInfo().uid);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                //这种方案适用于 API21——25，即 5.0——7.1 之间的版本可以使用
                intent.putExtra("app_package", getPackageName());
                intent.putExtra("app_uid", getApplicationInfo().uid);
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
            }
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();

            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }
    }

    private void explicitlyLoadPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.general_preferences, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(mStateListener, new IntentFilter(HongbaoNotificationService.ACTION_STATE_CHANGE));
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateHongbaoServiceStatus();
        updateNoticeService();
        // Check for update when WIFI is connected or on first time.
        if (ConnectivityUtil.isWifi(this) || UpdateTask.count == 0)
            new UpdateTask(this, false).update();
    }

    @Override
    public void onBackPressed() {
        String listeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (listeners != null && listeners.contains(HBApplication.LISTENER_PATH)) {
            Toast.makeText(this, "通知服务会一直运行。只能手动关闭！！！", Toast.LENGTH_SHORT).show();
        }
        if (!moveTaskToBack(false)) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.unregisterReceiver(mStateListener);
        if (isFinishing()) {
            String listeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
            if (listeners != null && listeners.contains(HBApplication.LISTENER_PATH)) {
                Toast.makeText(this, "通知监听服务运行中，请关闭", Toast.LENGTH_SHORT).show();
            }
        }
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        //移除监听服务
        accessibilityManager.removeAccessibilityStateChangeListener(this);
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.onDestroy();
    }

    public void openAccessibility(View view) {
        try {
            Toast.makeText(this, getString(R.string.turn_on_toast) + pluginStatusText.getText(), Toast.LENGTH_SHORT).show();
            Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(accessibleIntent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.turn_on_error_toast), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }


    public void launchNotificationService(View view) {
        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
    }


    public void snoozeNotificationService(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            HongbaoNotificationService.toggleSnooze(this);
        } else {
            ComponentName notificationService = new ComponentName(this, HongbaoNotificationService.class);
            PackageManager pm = getPackageManager();
            if (HongbaoNotificationService.isConnected()) {
                Toast.makeText(this, "暂停功能只支持Android7.0及以上", Toast.LENGTH_SHORT).show();
            } else {
                pm.setComponentEnabledSetting(notificationService, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                pm.setComponentEnabledSetting(notificationService, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }
        }
    }


    public void openGitHub(View view) {
        Intent webViewIntent = new Intent(this, WebViewActivity.class);
        webViewIntent.putExtra("title", getString(R.string.webview_github_title));
        webViewIntent.putExtra("url", "https://github.com/kingwang666/WeChatLuckyMoney");
        startActivity(webViewIntent);
    }


    public void openSettings(View view) {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        settingsIntent.putExtra("title", getString(R.string.preference));
        settingsIntent.putExtra("frag_id", "GeneralSettingsFragment");
        startActivity(settingsIntent);
    }


    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        updateHongbaoServiceStatus();
    }

    /**
     * 更新当前 HongbaoService 显示状态
     */
    private void updateHongbaoServiceStatus() {
        if (isServiceEnabled()) {
            Intent service = new Intent(this, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service);
            } else {
                startService(service);
            }
            pluginStatusText.setText(R.string.service_off);
            pluginStatusIcon.setBackgroundResource(R.mipmap.ic_stop);
        } else {
            Intent service = new Intent(this, KeepAliveService.class);
            stopService(service);
            pluginStatusText.setText(R.string.service_on);
            pluginStatusIcon.setBackgroundResource(R.mipmap.ic_start);
        }
    }

    private void updateNoticeService() {
        String listeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (listeners != null && listeners.contains(HBApplication.LISTENER_PATH)) {
            notifitionLaunchText.setText(R.string.service_off_notification);
            notifitionLaunchIcon.setBackgroundResource(R.mipmap.ic_stop);
            snoozeView.setEnabled(true);
            boolean connected = HongbaoNotificationService.isConnected();
            if (connected) {
                notifitionSnoozeText.setText(R.string.service_snooze_notification);
                notifitionSnoozeIcon.setBackgroundResource(R.mipmap.ic_stop);
            } else {
                notifitionSnoozeText.setText(R.string.service_unsnooze_notification);
                notifitionSnoozeIcon.setBackgroundResource(R.mipmap.ic_start);
            }
        } else {
            notifitionLaunchText.setText(R.string.service_on_notification);
            notifitionLaunchIcon.setBackgroundResource(R.mipmap.ic_start);
            snoozeView.setEnabled(false);
            notifitionSnoozeText.setText(R.string.service_unsnooze_notification);
            notifitionSnoozeIcon.setBackgroundResource(R.mipmap.ic_start);
        }
    }

    /**
     * 获取 HongbaoService 是否启用状态
     *
     * @return
     */
    private boolean isServiceEnabled() {
        List<AccessibilityServiceInfo> accessibilityServices =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info.getId().equals(getPackageName() + "/.services.HongbaoService")) {
                return true;
            }
        }
        return false;
    }


    private static class OnePixelReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {    //屏幕关闭启动1像素Activity
                Intent it = new Intent(context, OnePixelActivity.class);
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(it);
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {   //屏幕打开 结束1像素
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("finish"));
            }
        }
    }
}
