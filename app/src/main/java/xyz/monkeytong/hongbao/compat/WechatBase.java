package xyz.monkeytong.hongbao.compat;

import android.app.Application;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

import xyz.monkeytong.hongbao.HBApplication;

/**
 * Created on 2025/5/10
 * Author: wangxiaojie
 * Description:
 */
public class WechatBase implements Wechat {


    protected static final String WECHAT_VIEW_SELF_CH = "查看红包";
    protected static final String WECHAT_VIEW_OTHERS_CH = "领取红包";
    protected static final String WECHAT_VIEW_ALL_CH = "微信红包";

    protected static final String WECHAT_VIEW_PICKED = "已领取";
    protected static final String WECHAT_VIEW_PICKED_2 = "已被领完";
    protected static final String WECHAT_VIEW_PICKED_3 = "已过期";
    protected static final String WECHAT_VIEW_PICKED_4 = "的专属红包";


    protected static final String WECHAT_OPENED = "存入零钱";
    protected static final String WECHAT_BETTER_LUCK_2_CH = "手慢了，红包派完了";
    protected static final String WECHAT_EXPIRES_CH = "已超过24小时";
    protected static final String WECHAT_EXPIRES_2_CH = "过期";


    protected static final String WECHAT_LUCKMONEY_RECEIVE_ACTIVITY = ".plugin.luckymoney.ui";//com.tencent.mm/.plugin.luckymoney.ui.En_fba4b94f  com.tencent.mm/com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI
    protected static final String WECHAT_LUCKMONEY_RECEIVE_UI_ACTIVITY = "LuckyMoneyReceiveUI";
    protected static final String WECHAT_LUCKMONEY_NOT_HOOK_RECEIVE_UI_ACTIVITY = "LuckyMoneyNotHookReceiveUI";
    protected static final String WECHAT_LUCKMONEY_DETAIL_ACTIVITY = "LuckyMoneyDetailUI";
    protected static final String WECHAT_LUCKMONEY_BEFORE_DETAIL = "LuckyMoneyBeforeDetailUI";
    protected static final String WECHAT_LUCKMONEY_GENERAL_ACTIVITY = "LauncherUI";
    protected static final String WECHAT_LUCKMONEY_CHATTING_ACTIVITY = "ChattingUI";

    protected final Pattern mGroupChat = Pattern.compile("\\(\\d+?\\)");



    private static class WechatBaseHolder {
        private static final WechatBase INSTANCE = new WechatBase();
    }

    public static WechatBase getInstance() {
        return WechatBaseHolder.INSTANCE;
    }

    protected WechatBase() {
    }

    @Override
    @NonNull
    public String getGeneralActivityName() {
        return WECHAT_LUCKMONEY_GENERAL_ACTIVITY;
    }


    @Override
    public boolean isChatActivity(@Nullable String activityName) {
        if (activityName == null) {
            return false;
        }
        return activityName.contains(WECHAT_LUCKMONEY_CHATTING_ACTIVITY) || activityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY);
    }

    @Override
    public boolean isLuckyMoney(@Nullable String activityName) {
        return activityName != null && activityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY);
    }

    @Override
    public boolean isReceiveActivity(@Nullable String activityName) {
        if (activityName == null) {
            return false;
        }
        return activityName.contains(WECHAT_LUCKMONEY_RECEIVE_UI_ACTIVITY) || activityName.contains(WECHAT_LUCKMONEY_NOT_HOOK_RECEIVE_UI_ACTIVITY);
    }

    @Override
    public boolean isDetailActivity(@Nullable String activityName) {
        if (activityName == null) {
            return false;
        }
        return activityName.contains(WECHAT_LUCKMONEY_DETAIL_ACTIVITY) || activityName.contains(WECHAT_LUCKMONEY_BEFORE_DETAIL);
    }

    @Nullable
    @Override
    public AccessibilityNodeInfo findChatItem(@NonNull AccessibilityNodeInfo root) {
        return null;
    }

    @Nullable
    protected AccessibilityNodeInfo findChatItem(@NonNull AccessibilityNodeInfo root,
                                                 @NonNull String chatItemViewId,
                                                 @NonNull String unreadViewId,
                                                 @NonNull String contentViewId) {
        List<AccessibilityNodeInfo> chatItems = root.findAccessibilityNodeInfosByViewId(chatItemViewId);
        for (AccessibilityNodeInfo chatItem : chatItems) {
            List<AccessibilityNodeInfo> unreads = chatItem.findAccessibilityNodeInfosByViewId(unreadViewId);
            if (unreads.isEmpty() || unreads.get(0) == null) {
                continue;
            }
            List<AccessibilityNodeInfo> contents = chatItem.findAccessibilityNodeInfosByViewId(contentViewId);
            AccessibilityNodeInfo info;
            if (contents.isEmpty() || (info = contents.get(0)) == null) {
                continue;
            }
            if (!info.refresh()) {
                continue;
            }
            if (info.getText() != null && info.getText().toString().contains(WECHAT_NOTIFICATION_TIP)) {
                return chatItem;
            }
        }
        return null;
    }

    @Override
    public boolean isGroupChat(@NonNull AccessibilityNodeInfo root) {
        return false;
    }

    protected boolean isGroupChat(@NonNull AccessibilityNodeInfo root, @NonNull String groupTitleId) {
        List<AccessibilityNodeInfo> nodeInfos = root.findAccessibilityNodeInfosByViewId(groupTitleId);
        if (nodeInfos == null || nodeInfos.isEmpty()) {
            return true;
        }
        for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
            if (nodeInfo == null) {
                continue;
            }
            CharSequence text = nodeInfo.getText();
            if (TextUtils.isEmpty(text) || mGroupChat.matcher(text.toString()).find()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public AccessibilityNodeInfo findLuckyMoneyNode(@NonNull AccessibilityNodeInfo root, boolean onlyLastNode, boolean excludeExclusive, boolean openSelf) {
        return null;
    }



    @Nullable
    protected AccessibilityNodeInfo getTheLastReceiveNode(AccessibilityNodeInfo root,
                                                          @NonNull String receiveViewId,
                                                          @NonNull String receiveItemTextViewId,
                                                          @NonNull String receiveItemContentId,
                                                          boolean onlyLastNode,
                                                          boolean excludeExclusive,
                                                          boolean openSelf) {
        if (root == null) {
            return null;
        }

        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(receiveViewId);
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        for (int i = nodes.size() - 1; i >= 0; i--) {
            AccessibilityNodeInfo tempNode = nodes.get(i);
            if (tempNode == null || !tempNode.refresh()) {
                continue;
            }
            if (isLuckyMoneyNode(tempNode, receiveItemTextViewId)) {
                if (isCanOpenNode(tempNode, receiveItemContentId, excludeExclusive, openSelf)) {
                    return tempNode;
                } else if (onlyLastNode) {
                    break;
                }
            }
        }
        return null;
    }

    private boolean isLuckyMoneyNode(AccessibilityNodeInfo node, String receiveItemTextViewId) {
        List<AccessibilityNodeInfo> nodes = node.findAccessibilityNodeInfosByViewId(receiveItemTextViewId);
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        AccessibilityNodeInfo textNode = nodes.get(0);
        String text = textNode.getText() == null ? "" : textNode.getText().toString();
        if (TextUtils.isEmpty(text)) {
            return hasOneOfThoseNodes(node, WECHAT_VIEW_ALL_CH, WECHAT_VIEW_OTHERS_CH, WECHAT_VIEW_SELF_CH);
        }
        return text.contains(WECHAT_VIEW_ALL_CH) || text.contains(WECHAT_VIEW_OTHERS_CH) || text.contains(WECHAT_VIEW_SELF_CH);
    }

    private boolean isCanOpenNode(AccessibilityNodeInfo node, String receiveItemContentId, boolean excludeExclusive, boolean openSelf) {
        try {
            List<AccessibilityNodeInfo> contentNodes = node.findAccessibilityNodeInfosByViewId(receiveItemContentId);
            if (contentNodes == null || contentNodes.isEmpty()) {
                return true;
            }
            AccessibilityNodeInfo contentNode = contentNodes.get(0);
            String content = contentNode.getText() == null ? "" : contentNode.getText().toString();
            if (TextUtils.isEmpty(content)) {
                return !hasOneOfThoseNodes(node, WECHAT_VIEW_PICKED, WECHAT_VIEW_PICKED_2, WECHAT_VIEW_PICKED_3, WECHAT_VIEW_PICKED_4);
            }
            if (content.contains(WECHAT_VIEW_PICKED) || content.contains(WECHAT_VIEW_PICKED_2) || content.contains(WECHAT_VIEW_PICKED_3) || (excludeExclusive && content.endsWith(WECHAT_VIEW_PICKED_4))) {
                return false;
            }

            if (!openSelf) {
                Rect bounds = new Rect();
                Application application = HBApplication.getInstance();
                if (application != null) {
                    node.getBoundsInScreen(bounds);
                    DisplayMetrics metrics = application.getResources().getDisplayMetrics();
                    return bounds.centerX() <= metrics.widthPixels / 2;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected boolean hasOneOfThoseNodes(AccessibilityNodeInfo node, String... texts) {
        List<AccessibilityNodeInfo> nodes;
        for (String text : texts) {
            if (text == null) continue;
            nodes = node.findAccessibilityNodeInfosByText(text);

            if (nodes != null && !nodes.isEmpty()) return true;
        }
        return false;
    }

    @Nullable
    @Override
    public AccessibilityNodeInfo findOpenNode(@NonNull AccessibilityNodeInfo root) {
        return null;
    }

    @Nullable
    public AccessibilityNodeInfo findOpenNode(@NonNull AccessibilityNodeInfo root, @NonNull String openViewId) {
        List<AccessibilityNodeInfo> buttons = root.findAccessibilityNodeInfosByViewId(openViewId);
        if (buttons != null && !buttons.isEmpty()) {
            return buttons.get(0);
        }
        return null;
    }


    @Override
    public boolean isNotAvailable(@NonNull AccessibilityNodeInfo root) {
        return hasOneOfThoseNodes(root, WECHAT_OPENED, WECHAT_BETTER_LUCK_2_CH, WECHAT_EXPIRES_CH, WECHAT_EXPIRES_2_CH);
    }

}
