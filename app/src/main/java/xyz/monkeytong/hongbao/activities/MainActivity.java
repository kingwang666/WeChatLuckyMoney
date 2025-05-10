package xyz.monkeytong.hongbao.activities;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.util.List;

import xyz.monkeytong.hongbao.BuildConfig;
import xyz.monkeytong.hongbao.HBApplication;
import xyz.monkeytong.hongbao.R;
import xyz.monkeytong.hongbao.services.HongbaoNotificationService;
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

    private UpdateTask mUpdateTask;

    private AccessibilityServicesStateChangeListenerCompat mAccessibilityServicesStateChangeListener;

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

        ((TextView) findViewById(R.id.version_tv)).setText(BuildConfig.VERSION_NAME);

        explicitlyLoadPreferences();

        //监听AccessibilityService 变化
        accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mAccessibilityServicesStateChangeListener = new AccessibilityServicesStateChangeListenerCompat();
            mAccessibilityServicesStateChangeListener.bind(accessibilityManager);
        } else {
            accessibilityManager.addAccessibilityStateChangeListener(this);
        }
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
                MainActivityPermissionsDispatcher.checkPostNotification(MainActivity.this);
                MainActivityPermissionsDispatcher.checkBatteryOptimizations(MainActivity.this);
            }
        }, 500);
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
        if (ConnectivityUtil.isWifi(this) && !UpdateTask.sHomeRequest) {
            mUpdateTask = new UpdateTask(this, false);
            mUpdateTask.update();
        }
    }

    @Override
    public void onBackPressed() {
        String listeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (listeners != null && listeners.contains(HBApplication.LISTENER_PATH)) {
            Toast.makeText(this, R.string.warrning_notication_running, Toast.LENGTH_SHORT).show();
        }
        if (!moveTaskToBack(false)) {
            super.onBackPressed();
        }
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
                Toast.makeText(this, R.string.warrning_notication_pause, Toast.LENGTH_SHORT).show();
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
            pluginStatusText.setText(R.string.service_off);
            pluginStatusIcon.setBackgroundResource(R.mipmap.ic_stop);
        } else {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.unregisterReceiver(mStateListener);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        String listeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (listeners != null && listeners.contains(HBApplication.LISTENER_PATH)) {
            Toast.makeText(this, R.string.error_notication_running, Toast.LENGTH_SHORT).show();
        }
        //移除监听服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (mAccessibilityServicesStateChangeListener != null) {
                mAccessibilityServicesStateChangeListener.unbind(accessibilityManager);
            }
        } else {
            accessibilityManager.removeAccessibilityStateChangeListener(this);
        }
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        if (mUpdateTask != null) {
            mUpdateTask.cancel(true);
            mUpdateTask = null;
        }
        super.onDestroy();
    }


    private static class OnePixelReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {    //屏幕关闭启动1像素Activity
                Intent it = new Intent(context, OnePixelActivity.class);
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(it);
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {   //屏幕打开 结束1像素
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(OnePixelActivity.ACTION_FINISH));
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private class AccessibilityServicesStateChangeListenerCompat {

        private final AccessibilityManager.AccessibilityServicesStateChangeListener mListener = new AccessibilityManager.AccessibilityServicesStateChangeListener() {

            @Override
            public void onAccessibilityServicesStateChanged(AccessibilityManager manager) {
                updateHongbaoServiceStatus();
            }
        };

        public void bind(AccessibilityManager manager) {
            manager.addAccessibilityServicesStateChangeListener(mListener);
        }

        public void unbind(AccessibilityManager manager) {
            manager.removeAccessibilityServicesStateChangeListener(mListener);
        }
    }
}
