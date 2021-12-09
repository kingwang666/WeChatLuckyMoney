package xyz.monkeytong.hongbao.utils;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Process;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import static android.content.pm.PackageManager.GET_SERVICES;

/**
 * Created on 2016/10/19.
 * Author: wang
 */

public class SystemUtil {

    @Nullable
    public static ActivityManager.RunningAppProcessInfo getProcess(@NonNull Context context) {
        return getProcess(context, android.os.Process.myPid());
    }

    @Nullable
    public static ActivityManager.RunningAppProcessInfo getProcess(@NonNull Context context, int pid) {
        ActivityManager am = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            if (runningProcesses != null) {
                for (ActivityManager.RunningAppProcessInfo info : runningProcesses) {
                    if (info.pid == pid) {
                        return info;
                    }
                }
            }
        }
        return null;
    }


    @Nullable
    public static String getProcessName(@NonNull Context context) {
        return getProcessName(context, android.os.Process.myPid());
    }

    @Nullable
    public static String getProcessName(@NonNull Context context, int pid) {
        ActivityManager.RunningAppProcessInfo info = getProcess(context, pid);
        if (info != null) {
            return info.processName;
        }
        return null;
    }


    @Nullable
    public static String getProcessName() {
        try {
            File file = new File("/proc/" + android.os.Process.myPid() + "/" + "cmdline");
            BufferedReader mBufferedReader = new BufferedReader(new FileReader(file));
            String processName = mBufferedReader.readLine().trim();
            mBufferedReader.close();
            return processName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isInMainProcess(@NonNull Context context) {
        String processName = getProcessName(context);
        return !TextUtils.isEmpty(processName) && context.getPackageName().equals(processName);
    }

    public static boolean isInMainProcess(@NonNull String packageName) {
        String processName = getProcessName();
        return !TextUtils.isEmpty(processName) && packageName.equals(processName);
    }

    public static boolean isInServiceProcess(@NonNull Context context, @NonNull Class<? extends Service> serviceClass) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), GET_SERVICES);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        String mainProcess = packageInfo.applicationInfo.processName;
        ComponentName component = new ComponentName(context, serviceClass);
        ServiceInfo serviceInfo;
        try {
            serviceInfo = packageManager.getServiceInfo(component, 0);
        } catch (PackageManager.NameNotFoundException ignored) {
            // Service is disabled.
            return false;
        }
        if (serviceInfo.processName.equals(mainProcess)) {
            // Technically we are in the service process, but we're not in the service dedicated process.
            return false;
        }
        int myPid = android.os.Process.myPid();
        ActivityManager.RunningAppProcessInfo myProcess = getProcess(context, myPid);
        if (myProcess == null) {
            return false;
        }
        return myProcess.processName.equals(serviceInfo.processName);
    }

    public static boolean isServiceRunningInCurrentProgress(Context context, Class<? extends Service> clazz) {
        ComponentName notificationService = new ComponentName(context, clazz);
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices == null || runningServices.isEmpty()) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (service.service.equals(notificationService)) {
                if (service.pid == Process.myPid()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isServiceRunning(Context context, Class<? extends Service> clazz) {
        ComponentName notificationService = new ComponentName(context, clazz);
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices == null || runningServices.isEmpty()) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (service.service.equals(notificationService)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static List<PackageInfo> getAllInstallApp(@Nullable Context context) {
        if (context == null) {
            return null;
        }
        final PackageManager pm = context.getApplicationContext().getPackageManager();
        return pm.getInstalledPackages(0);
    }

    @Nullable
    public static PackageInfo getInstallApp(@Nullable Context context, @Nullable String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return null;
        }
        try {
            return context.getApplicationContext().getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static Intent getLaunch(@Nullable Context context, @Nullable String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return null;
        }
        return context.getApplicationContext().getPackageManager().getLaunchIntentForPackage(packageName);
    }

    @Nullable
    public static PackageInfo getInstallApp2(@Nullable Context context, @Nullable String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return null;
        }
        List<PackageInfo> packages = getAllInstallApp(context);
        if (packages == null || packages.isEmpty()) {
            return null;
        }
        for (int i = 0; i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);
            if (packageInfo.packageName.equals(packageName)) {
                return packageInfo;
            }
        }
        return null;
    }

    public static boolean isInstalled(@Nullable Context context, @Nullable String packageName) {
        return getInstallApp(context, packageName) != null;
    }

    public static void copyToClipboard(@Nullable Context context, @Nullable CharSequence text) {
        copyToClipboard(context, null, text);
    }

    public static void copyToClipboard(@Nullable Context context, @Nullable String label, @Nullable CharSequence text) {
        if (context == null || TextUtils.isEmpty(text)) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) context.getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
        }
    }

    @Nullable
    public static CharSequence getFormClipboard(@Nullable Context context) {
        return getFormClipboard(context, null);
    }

    @Nullable
    public static CharSequence getFormClipboard(@Nullable Context context, @Nullable String label) {
        if (context == null) {
            return null;
        }
        context = context.getApplicationContext();
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                if (label == null || label.contentEquals(clip.getDescription().getLabel())) {
                    return clip.getItemAt(0).coerceToText(context);
                }
            }
        }
        return null;
    }
}
