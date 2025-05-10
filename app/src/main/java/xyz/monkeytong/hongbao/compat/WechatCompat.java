package xyz.monkeytong.hongbao.compat;

import android.content.Context;
import android.content.pm.PackageInfo;

import xyz.monkeytong.hongbao.utils.SystemUtil;

/**
 * Created by wangxiaojie on 2025/5/8.
 */
public class WechatCompat {

    public static Wechat getWechat(Context context) {
        PackageInfo packageInfo = SystemUtil.getInstallApp(context, "com.tencent.mm");
        if (packageInfo == null) {
            return Wechat2841.getInstance();
        }
        int versionCode = packageInfo.versionCode;
        if (versionCode >= 2841) {
            return Wechat2841.getInstance();
        }
        return Wechat2841.getInstance();
    }
}
