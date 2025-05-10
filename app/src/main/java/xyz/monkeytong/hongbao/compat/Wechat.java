package xyz.monkeytong.hongbao.compat;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by wangxiaojie on 2025/5/8.
 */
public interface Wechat {

    String WECHAT_NOTIFICATION_TIP = "[微信红包]";

    @NonNull
    String getGeneralActivityName();

    boolean isChatActivity(@Nullable String activityName);

    boolean isLuckyMoney(@Nullable String activityName);

    boolean isReceiveActivity(@Nullable String activityName);

    boolean isDetailActivity(@Nullable String activityName);

    @Nullable
    AccessibilityNodeInfo findChatItem(@NonNull AccessibilityNodeInfo root);

    boolean isGroupChat(@NonNull AccessibilityNodeInfo root);

    @Nullable
    AccessibilityNodeInfo findLuckyMoneyNode(@NonNull AccessibilityNodeInfo root, boolean onlyLastNode, boolean excludeExclusive, boolean openSelf);

    @Nullable
    AccessibilityNodeInfo findOpenNode(@NonNull AccessibilityNodeInfo root);

    boolean isNotAvailable(@NonNull AccessibilityNodeInfo root);

}
