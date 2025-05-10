package xyz.monkeytong.hongbao.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import xyz.monkeytong.hongbao.R;
import xyz.monkeytong.hongbao.permisson.PermissionUtils;
import xyz.monkeytong.hongbao.utils.SettingUtils;


final class MainActivityPermissionsDispatcher {

    private static final int REQUEST_POST_NOTIFICATION = 0;

    private static final String[] PERMISSION_NOTIFICATION = new String[]{Manifest.permission.POST_NOTIFICATIONS};

    static void checkPostNotification(@NonNull MainActivity target) {
        if (!PermissionUtils.hasSelfPermissions(target, PERMISSION_NOTIFICATION)) {
            ActivityCompat.requestPermissions(target, PERMISSION_NOTIFICATION, REQUEST_POST_NOTIFICATION);
        }
    }

    static void checkBatteryOptimizations(@NonNull MainActivity target){
        PowerManager powerManager = (PowerManager) target.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null){
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !powerManager.isIgnoringBatteryOptimizations(target.getPackageName())) {
            new AlertDialog.Builder(target)
                    .setTitle(R.string.battery_optimization)
                    .setMessage(R.string.battery_optimization_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SettingUtils.requestIgnoreBatteryOptimizations(target);
                        }
                    })
                    .show();
        }
    }

    static void onRequestPermissionsResult(@NonNull MainActivity target, int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_POST_NOTIFICATION:
                if (!PermissionUtils.verifyPermissions(permissions, grantResults) && !PermissionUtils.shouldShowRequestPermissionRationale(target, permissions)) {
                    showNotificationSettingsDialog(target);
                }
                break;
        }
    }

    private static void showNotificationSettingsDialog(@NonNull MainActivity target) {
        new AlertDialog.Builder(target)
                .setTitle(R.string.notification_permission)
                .setMessage(R.string.notification_permission_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SettingUtils.gotoNotificationSettings(target);
                    }
                })
                .show();

    }


}
