package xyz.monkeytong.hongbao.compat;

import android.app.Application;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

import xyz.monkeytong.hongbao.HBApplication;

/**
 * Created by wangxiaojie on 2025/5/8.
 */
class Wechat2841 extends WechatBase {

    private static final String VIEW_ID_CHAT_ITEM = "com.tencent.mm:id/cj1";
    private static final String VIEW_ID_CHAT_ITEM_UNREAD = "com.tencent.mm:id/a_h";

    private static final String VIEW_ID_CHAT_ITEM_CONTENT = "com.tencent.mm:id/ht5";

    private static final String VIEW_ID_CHAT_TITLE = "com.tencent.mm:id/obn";

    private static final String VIEW_ID_CHAT_RECEIVE_ITEM = "com.tencent.mm:id/bkg";

    private static final String VIEW_ID_CHAT_RECEIVE_ITEM_TEXT = "com.tencent.mm:id/a3y";

    private static final String VIEW_ID_CHAT_RECEIVE_ITEM_CONTENT = "com.tencent.mm:id/a3m";

    private static final String VIEW_ID_CHAT_RECEIVE_DETAIL_OPEN = "com.tencent.mm:id/j6g";


    private static class Wechat2841Holder {
        private static final Wechat2841 INSTANCE = new Wechat2841();
    }

    public static Wechat2841 getInstance() {
        return Wechat2841Holder.INSTANCE;
    }

    protected Wechat2841() {
    }

    @Override
    @Nullable
    public AccessibilityNodeInfo findChatItem(@NonNull AccessibilityNodeInfo root) {
        return findChatItem(root, VIEW_ID_CHAT_ITEM, VIEW_ID_CHAT_ITEM_UNREAD, VIEW_ID_CHAT_ITEM_CONTENT);
    }

    @Override
    public boolean isGroupChat(@NonNull AccessibilityNodeInfo root) {
        return isGroupChat(root, VIEW_ID_CHAT_TITLE);
    }

    @Nullable
    @Override
    public AccessibilityNodeInfo findLuckyMoneyNode(@NonNull AccessibilityNodeInfo root, boolean onlyLastNode, boolean excludeExclusive, boolean openSelf) {
        /* 聊天会话窗口，遍历节点匹配“微信红包”，“领取红包”和"查看红包" */
        return getTheLastReceiveNode(root, VIEW_ID_CHAT_RECEIVE_ITEM, VIEW_ID_CHAT_RECEIVE_ITEM_TEXT, VIEW_ID_CHAT_RECEIVE_ITEM_CONTENT, onlyLastNode, excludeExclusive, openSelf);
    }

    @Nullable
    @Override
    public AccessibilityNodeInfo findOpenNode(@NonNull AccessibilityNodeInfo root) {
        return findOpenNode(root, VIEW_ID_CHAT_RECEIVE_DETAIL_OPEN);
    }


}
