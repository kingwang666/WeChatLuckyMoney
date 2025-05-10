package xyz.monkeytong.hongbao;

import android.app.Application;

/**
 * Created on 2020/1/13
 * Author: bigwang
 * Description:
 */
public class HBApplication extends Application {

    public static final String LISTENER_PATH = "xyz.monkeytong.hongbao/xyz.monkeytong.hongbao.services.HongbaoNotificationService";

    private static HBApplication sInstance;

    public static HBApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

}
