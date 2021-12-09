package xyz.monkeytong.hongbao.utils;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;

/**
 * Created by Zhongyi on 1/29/16.
 */
public class PowerUtil {

    private static final int WAKE_LOCK_LEVEL = PowerManager.SCREEN_DIM_WAKE_LOCK;

    private static volatile PowerUtil sInstance;

    private PowerManager.WakeLock wakeLock;

    public static PowerUtil getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PowerUtil.class) {
                if (sInstance == null) {
                    sInstance = new PowerUtil(context);
                }
            }
        }
        return sInstance;
    }

    private PowerUtil(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isWakeLockLevelSupported(WAKE_LOCK_LEVEL)){
            wakeLock = pm.newWakeLock(WAKE_LOCK_LEVEL | PowerManager.ON_AFTER_RELEASE, getClass().getName());
        }
    }


    @SuppressLint("WakelockTimeout")
    private void acquire() {
        if (wakeLock != null){
            wakeLock.acquire();
        }

    }

    private void release() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public void handleWakeLock(boolean keepScreenOn) {
        if (keepScreenOn) {
            acquire();
        } else {
            release();
        }
    }
}
