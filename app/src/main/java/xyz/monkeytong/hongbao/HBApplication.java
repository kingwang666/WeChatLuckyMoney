package xyz.monkeytong.hongbao;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;

import java.util.List;

import xyz.monkeytong.hongbao.services.HongbaoNotificationService;
import xyz.monkeytong.hongbao.utils.SystemUtil;

/**
 * Created on 2020/1/13
 * Author: bigwang
 * Description:
 */
public class HBApplication extends Application {

    public static final String LISTENER_PATH = "xyz.monkeytong.hongbao/xyz.monkeytong.hongbao.services.HongbaoNotificationService";

    @Override
    public void onCreate() {
        super.onCreate();
    }

}
