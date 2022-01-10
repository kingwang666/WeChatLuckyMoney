package xyz.monkeytong.hongbao.services;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;
import java.util.regex.Pattern;

import xyz.monkeytong.hongbao.BuildConfig;
import xyz.monkeytong.hongbao.R;
import xyz.monkeytong.hongbao.utils.PowerUtil;

public class HongbaoService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "HongbaoService";

    private static final String WECHAT_DETAILS_CH = "红包详情";
    private static final String WECHAT_DETAILS_2_CH = "红包记录";
    private static final String WECHAT_OPENED = "已存入零钱";
    private static final String WECHAT_BETTER_LUCK_CH = "手慢了";
    private static final String WECHAT_BETTER_LUCK_2_CH = "手慢了，红包派完了";
    private static final String WECHAT_EXPIRES_CH = "已超过24小时";
    private static final String WECHAT_EXPIRES_2_CH = "过期";
    private static final String WECHAT_VIEW_SELF_CH = "查看红包";
    private static final String WECHAT_VIEW_OTHERS_CH = "领取红包";
    private static final String WECHAT_VIEW_ALL_CH = "微信红包";
    public static final String WECHAT_NOTIFICATION_TIP = "[微信红包]";
    private static final String WECHAT_LUCKMONEY_RECEIVE_ACTIVITY = ".plugin.luckymoney.ui";//com.tencent.mm/.plugin.luckymoney.ui.En_fba4b94f  com.tencent.mm/com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI
    private static final String WECHAT_LUCKMONEY_RECEIVE_UI_ACTIVITY = "LuckyMoneyReceiveUI";
    private static final String WECHAT_LUCKMONEY_NOT_HOOK_RECEIVE_UI_ACTIVITY = "LuckyMoneyNotHookReceiveUI";
    private static final String WECHAT_LUCKMONEY_DETAIL_ACTIVITY = "LuckyMoneyDetailUI";
    private static final String WECHAT_LUCKMONEY_GENERAL_ACTIVITY = "LauncherUI";
    private static final String WECHAT_LUCKMONEY_CHATTING_ACTIVITY = "ChattingUI";
    private String currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY;

    private AccessibilityNodeInfo mReceiveNode, mUnpackNode;
    private boolean mLuckyMoneyPicked, mLuckyMoneyReceived;
    private int mUnpackCount = 0;
    private boolean mMutex = false, mListMutex = false, mChatMutex = false, mOpened = false;
    private boolean mRedPackOpening = false;
    private int mCurrentChatWindowsId;
    private int mCurrentReceiveWindowId;
    private int mCurrentDetailWindowId;

    private boolean mForceCheckWindow;

    private boolean mWatchList = false;
    private boolean mWatchChat = true;
    private boolean mOpenSelf = true;
    private int mOpenDelay;
    private boolean mBackAfterOpen = true;
    private boolean mOnlyLastNode = true;

    private final Pattern mGroupChat = Pattern.compile("\\(\\d+?\\)");

    private PowerUtil powerUtil;

    private Handler mHandler;

    private final Runnable mOpenPackCallback = new Runnable() {
        @Override
        public void run() {
            openPackIfNeed();
        }
    };

    /**
     * AccessibilityEvent
     *
     * @param event 事件
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        long startTime = 0;
        if (BuildConfig.DEBUG) {
            startTime = System.currentTimeMillis();
            Log.d(TAG, "class: " + event.getClassName() + "  type: " + event.getEventType() + " content type: " + event.getContentChangeTypes());
        }
        setCurrentActivityName(event);

        if (!mMutex) {
            if (mWatchList && watchList(event)) return;
            mListMutex = false;
        }

        if (!mChatMutex) {
            mChatMutex = true;
            if (mWatchChat) watchChat(event);
            mChatMutex = false;
        }
        if (BuildConfig.DEBUG) {
            long time = System.currentTimeMillis() - startTime;
            Log.d(TAG, "end time: " + time);
        }
    }

    private void watchChat(AccessibilityEvent event) {

        mReceiveNode = null;
        mUnpackNode = null;

        checkNodeInfo(event);

        /* 如果已经接收到红包并且还没有戳开 */
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "watchChat mLuckyMoneyReceived:" + mLuckyMoneyReceived + " mLuckyMoneyPicked:" + mLuckyMoneyPicked + " mReceiveNode:" + mReceiveNode);
        }
        if (mLuckyMoneyReceived && (mReceiveNode != null) && isInChatActivity()) {
            mMutex = true;
            mRedPackOpening = false;
            mReceiveNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            mOpened = true;
            mLuckyMoneyReceived = false;
            mLuckyMoneyPicked = true;
            if (mUnpackNode == null) {
                mUnpackCount += 1;
            }
            return;
        }
        openPackIfNeed();
    }

    private void openPackIfNeed() {
        /* 如果戳开但还未领取 */
        if (mUnpackCount >= 1 && (mUnpackNode != null) || canOpen()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "戳开红包！" + " mUnpackCount: " + mUnpackCount + " mUnpackNode: " + mUnpackNode);
            }
            if (mOpenDelay != 0) {
                getHandler().postDelayed(
                        new Runnable() {
                            public void run() {
                                try {
                                    openPacket();
                                } catch (Exception e) {
                                    mMutex = false;
                                    mLuckyMoneyPicked = false;
                                    mRedPackOpening = false;
                                    resetUnpackState();
                                }
                            }
                        },
                        mOpenDelay * 1000L);
            } else {
                openPacket();
            }
        }
    }

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler();
        }
        return mHandler;
    }

    private void openPacket() {
        if (mUnpackCount >= 1 && (mUnpackNode != null)) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "openPacket！");
            }
            mRedPackOpening = true;
            mUnpackNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            mOpened = true;
            resetUnpackState();
        }
    }

    private boolean canOpen() {
        if (!isInReceiveActivity()) {
            return false;
        }
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo == null) {
            getHandler().removeCallbacks(mOpenPackCallback);
            getHandler().postDelayed(mOpenPackCallback, 50);
            return false;
        }
        String currentActivityName = getCurrentActivityName();
        boolean hasNodes = this.hasOneOfThoseNodes(rootNodeInfo, WECHAT_OPENED,
                WECHAT_BETTER_LUCK_CH, WECHAT_BETTER_LUCK_2_CH,
                WECHAT_DETAILS_CH, WECHAT_DETAILS_2_CH, WECHAT_EXPIRES_CH, WECHAT_EXPIRES_2_CH);
        if (hasNodes) {
            getHandler().removeCallbacks(mOpenPackCallback);
            clickBackIfNeed(false);
            resetUnpackState();
            mRedPackOpening = false;
            return false;
        }
        //再次检查，以防上次没检测到
        if (mUnpackNode == null) {
            /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
            AccessibilityNodeInfo unpackNode = findOpenButton(rootNodeInfo);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "node2 " + unpackNode);
            }
            if (unpackNode != null && currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY)) {
                mUnpackNode = unpackNode;
                mUnpackCount += 1;
                getHandler().removeCallbacks(mOpenPackCallback);
            } else {
                getHandler().removeCallbacks(mOpenPackCallback);
                getHandler().postDelayed(mOpenPackCallback, 50);
                return false;
            }
        }
        return true;

    }

    private void resetUnpackState() {
        mUnpackNode = null;
        mUnpackCount = 0;
    }

    private void setCurrentActivityName(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            try {
                ComponentName componentName = new ComponentName(
                        event.getPackageName().toString(),
                        event.getClassName().toString()
                );
                getPackageManager().getActivityInfo(componentName, 0);
                currentActivityName = componentName.flattenToShortString();
                mForceCheckWindow = false;
                int currentWindowId = event.getWindowId();
                if (isInChatActivity(false)) {
                    mCurrentChatWindowsId = currentWindowId;
                } else if (isInReceiveActivity(false)) {
                    mCurrentReceiveWindowId = currentWindowId;
                } else if (isInDetailActivity(false)) {
                    mCurrentDetailWindowId = currentWindowId;
                }
            } catch (PackageManager.NameNotFoundException e) {
                int currentWindowId = getActiveWindowId(event);
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, currentWindowId + " ", e);
                }
                if (!checkCurrentActivityName(currentWindowId)) {
                    mForceCheckWindow = true;
                }
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "CurrentActivity: " + currentActivityName);
            }
        } else if (mForceCheckWindow) {
            checkCurrentActivityName();
        }
    }

    private int getActiveWindowId(AccessibilityEvent event) {
        AccessibilityNodeInfo info = getRootInActiveWindow();
        int id = -1;
        if (info != null) {
            id = info.getWindowId();
        } else if (event != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getContentChangeTypes() == AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED) {
            id = event.getWindowId();
        }
        return id;
    }

    private boolean checkCurrentActivityName() {
        AccessibilityNodeInfo info = getRootInActiveWindow();
        if (info == null) {
            return false;
        }
        return checkCurrentActivityName(info.getWindowId());
    }

    private boolean checkCurrentActivityName(int windowId) {
        if (windowId == -1) {
            return false;
        }
        if (windowId == mCurrentChatWindowsId) {
            currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY;
            mForceCheckWindow = false;
            return true;
        }
        if (windowId == mCurrentReceiveWindowId) {
            currentActivityName = WECHAT_LUCKMONEY_RECEIVE_ACTIVITY + "." + WECHAT_LUCKMONEY_NOT_HOOK_RECEIVE_UI_ACTIVITY;
            mForceCheckWindow = false;
            return true;
        }
        if (windowId == mCurrentDetailWindowId) {
            currentActivityName = WECHAT_LUCKMONEY_RECEIVE_ACTIVITY + "." + WECHAT_LUCKMONEY_DETAIL_ACTIVITY;
            mForceCheckWindow = false;
            return true;
        }
        return false;
    }

    private String getCurrentActivityName() {
        return currentActivityName;
    }


    private boolean isLuckyMoney() {
        if (isChatWindow()) {
            return false;
        }
        return getCurrentActivityName().contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY);
    }

    private boolean isInChatActivity() {
        return isInChatActivity(true);
    }

    private boolean isInChatActivity(boolean checkId) {
        int id;
        if (checkId && (id = getActiveWindowId(null)) != -1 && id != mCurrentChatWindowsId) {
            return false;
        }
        String currentActivityName = getCurrentActivityName();
        return currentActivityName.contains(WECHAT_LUCKMONEY_CHATTING_ACTIVITY) || currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY);
    }

    private boolean isGroupChat(AccessibilityNodeInfo rootNodeInfo) {
        int id;
        if ((id = getActiveWindowId(null)) != -1 && id != mCurrentChatWindowsId) {
            return false;
        }
        List<AccessibilityNodeInfo> nodeInfos = rootNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ipt");
        if (nodeInfos == null || nodeInfos.isEmpty()) {
            return true;
        }
        for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
            if (nodeInfo == null) {
                continue;
            }
            CharSequence text = nodeInfo.getText();
            if (text != null && text.length() > 0 && mGroupChat.matcher(text.toString()).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean isInReceiveActivity() {
        return isInReceiveActivity(true);
    }

    private boolean isInReceiveActivity(boolean checkId) {
        if (checkId && isChatWindow()) {
            return false;
        }
        String currentActivityName = getCurrentActivityName();
        return isLuckyMoney() && (currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_UI_ACTIVITY) || currentActivityName.contains(WECHAT_LUCKMONEY_NOT_HOOK_RECEIVE_UI_ACTIVITY));
    }

    private boolean isInDetailActivity() {
        return isInDetailActivity(true);
    }

    private boolean isInDetailActivity(boolean checkId) {
        if (checkId && isChatWindow()) {
            return false;
        }
        String currentActivityName = getCurrentActivityName();
        return isLuckyMoney() && (currentActivityName.contains(WECHAT_LUCKMONEY_DETAIL_ACTIVITY));
    }

    private boolean isChatWindow() {
        int id = getActiveWindowId(null);
        return id != -1 && mCurrentChatWindowsId != 0 && id == mCurrentChatWindowsId;
    }


    private boolean watchList(AccessibilityEvent event) {
        if (mListMutex || !isInChatActivity() || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
            return false;
        mListMutex = true;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return false;
        }
        List<AccessibilityNodeInfo> chatItems = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a4k");
        for (AccessibilityNodeInfo chatItem : chatItems) {
            List<AccessibilityNodeInfo> unreads = chatItem.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/iot");
            AccessibilityNodeInfo info;
            if (unreads.isEmpty() || (info = unreads.get(0)) == null) {
                continue;
            }
            if (TextUtils.isEmpty(info.getText())) {
                continue;
            }
            List<AccessibilityNodeInfo> contents = chatItem.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/e7t");
            if (contents.isEmpty() || (info = contents.get(0)) == null) {
                continue;
            }
            if (info.getText() != null && info.getText().toString().contains(WECHAT_NOTIFICATION_TIP)) {
                chatItem.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }
        return false;
    }

    private AccessibilityNodeInfo findOpenButton(AccessibilityNodeInfo node) {
        if (node == null || node.getWindowId() == mCurrentChatWindowsId || node.getWindowId() == mCurrentDetailWindowId || mRedPackOpening) {
            return null;
        }

        List<AccessibilityNodeInfo> buttons = node.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/f4f");
        if (buttons != null && buttons.size() == 1) {
            AccessibilityNodeInfo button = buttons.get(0);
            if (button != null) {
                return button;
            }
        }
        String currentActivityName = getCurrentActivityName();
        if (BuildConfig.DEBUG) {
            Log.e(TAG, node.getClassName().toString() + "   " + node.getContentDescription() + "  " + node.getText() + "  " + currentActivityName);
        }
        //非layout元素
        if ("android.widget.Button".equals(node.getClassName()))
            return node;
        else if (node.getChildCount() == 0) {
            return null;
        }


        //layout元素，遍历找button
        AccessibilityNodeInfo button;
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getWindowId() == mCurrentChatWindowsId || node.getWindowId() == mCurrentDetailWindowId || mRedPackOpening) {
                break;
            }
            button = findOpenButton(node.getChild(i));
            if (button != null)
                return button;
        }
        return null;
    }

    public AccessibilityNodeInfo getNewHongbaoNode(AccessibilityNodeInfo node) {
        try {
            /* The hongbao container node. It should be a LinearLayout. By specifying that, we can avoid text messages. */
            AccessibilityNodeInfo hongbaoNode = node.getParent();
            if (hongbaoNode == null) {
                return null;
            }
            CharSequence name = hongbaoNode.getClassName();
            if (!"android.widget.FrameLayout".equals(name == null ? null : name.toString()))
                return null;

            /* The text in the hongbao. Should mean something. */
            int count = hongbaoNode.getChildCount();
            String hongbaoContent = count >= 1 ? hongbaoNode.getChild(0).getText().toString() : null;
            if ("查看红包".equals(hongbaoContent)) {
                return null;
            }
            if (count > 1) {
                hongbaoContent = hongbaoNode.getChild(1).getText().toString();
                if (TextUtils.isEmpty(hongbaoContent) || hongbaoContent.contains("已领取") || hongbaoContent.contains("已被领完") || hongbaoContent.contains("已过期"))
                    return null;
            }
            if (!mOpenSelf) {
                Rect bounds = new Rect();
                hongbaoNode.getBoundsInScreen(bounds);
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                if (bounds.centerX() > metrics.widthPixels / 2) {
                    return null;
                }
            }
            return hongbaoNode;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void checkNodeInfo(final AccessibilityEvent event) {
        int eventType = event.getEventType();
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo == null) return;

        /* 聊天会话窗口，遍历节点匹配“微信红包”，“领取红包”和"查看红包" */
        AccessibilityNodeInfo receiveNode;
        if (isInChatActivity() && isGroupChat(rootNodeInfo) && (receiveNode = getTheLastReceiveNode(WECHAT_VIEW_ALL_CH, WECHAT_VIEW_OTHERS_CH, WECHAT_VIEW_SELF_CH)) != null) {
            mLuckyMoneyReceived = true;
            mReceiveNode = receiveNode;
            return;
        }

        /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
        AccessibilityNodeInfo unpackNode;
        if (isInReceiveActivity() && (unpackNode = findOpenButton(rootNodeInfo)) != null && (mUnpackNode == null || !mUnpackNode.equals(unpackNode))) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "checkNodeInfo  node2 " + unpackNode);
            }
            mUnpackNode = unpackNode;
            mUnpackCount += 1;
            return;
        }
        clickBackIfNeed(true);
    }

    private void clickBackIfNeed(boolean checkNodes) {
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo == null || !(isInDetailActivity() || isInReceiveActivity())) {
            return;
        }
        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”和“手慢了” */
        boolean hasNodes = !checkNodes || hasOneOfThoseNodes(rootNodeInfo, WECHAT_OPENED,
                WECHAT_BETTER_LUCK_CH, WECHAT_BETTER_LUCK_2_CH, WECHAT_DETAILS_CH, WECHAT_DETAILS_2_CH,
                WECHAT_EXPIRES_CH, WECHAT_EXPIRES_2_CH);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "checkNodeInfo  hasNodes:" + hasNodes + " opened: " + mOpened + " mMutex:" + mMutex);
        }
        if (hasNodes) {
            mMutex = false;
            mLuckyMoneyPicked = false;
            mRedPackOpening = false;
            resetUnpackState();
            if (mOpened && mBackAfterOpen) {
                mOpened = false;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "back click");
                }
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        }
    }

    private boolean hasOneOfThoseNodes(AccessibilityNodeInfo rootNodeInfo, String... texts) {
        List<AccessibilityNodeInfo> nodes;
        for (String text : texts) {
            if (text == null) continue;

            nodes = rootNodeInfo.findAccessibilityNodeInfosByText(text);

            if (nodes != null && !nodes.isEmpty()) return true;
        }
        return false;
    }

    private AccessibilityNodeInfo getTheLastReceiveNode(String... texts) {
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo == null) {
            return null;
        }

        List<AccessibilityNodeInfo> nodes;
        for (String text : texts) {
            if (text == null) {
                continue;
            }
            nodes = rootNodeInfo.findAccessibilityNodeInfosByText(text);
            if (nodes == null || nodes.isEmpty()) {
                continue;
            }
            if (mOnlyLastNode) {
                return getNewHongbaoNode(nodes.get(nodes.size() - 1));
            }
            for (int i = nodes.size() - 1; i >= 0; i--) {
                AccessibilityNodeInfo tempNode = nodes.get(i);
                if (tempNode == null) {
                    continue;
                }
                AccessibilityNodeInfo newHongBaoNode = getNewHongbaoNode(tempNode);
                if (newHongBaoNode != null) {
                    return newHongBaoNode;
                }
            }
        }
        return null;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        this.watchFlagsFromPreference();
    }

    private void watchFlagsFromPreference() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        mWatchList = sharedPreferences.getBoolean("pref_watch_list", mWatchList);
        mWatchChat = sharedPreferences.getBoolean("pref_watch_chat", mWatchChat);
        mOpenSelf = sharedPreferences.getBoolean("pref_watch_self", mOpenSelf);
        mOpenDelay = sharedPreferences.getInt("pref_open_delay", 0);
        mBackAfterOpen = sharedPreferences.getBoolean("pref_open_after_back", mBackAfterOpen);
        mOnlyLastNode = sharedPreferences.getBoolean("pref_only_last", mOnlyLastNode);

        this.powerUtil = PowerUtil.getInstance(this);
        boolean watchOnLockFlag = sharedPreferences.getBoolean("pref_keep_screen_on", false);
        this.powerUtil.handleWakeLock(watchOnLockFlag);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "pref_keep_screen_on":
                boolean changedValue = sharedPreferences.getBoolean(key, false);
                this.powerUtil.handleWakeLock(changedValue);
                break;
            case "pref_watch_list":
                mWatchList = sharedPreferences.getBoolean(key, mWatchList);
                break;
            case "pref_watch_chat":
                mWatchChat = sharedPreferences.getBoolean(key, mWatchChat);
                break;
            case "pref_watch_self":
                mOpenSelf = sharedPreferences.getBoolean(key, mOpenSelf);
                break;
            case "pref_open_delay":
                mOpenDelay = sharedPreferences.getInt(key, 0);
                break;
            case "pref_open_after_back":
                mBackAfterOpen = sharedPreferences.getBoolean(key, mBackAfterOpen);
                break;
            case "pref_only_last":
                mOnlyLastNode = sharedPreferences.getBoolean(key, mOnlyLastNode);
                break;
        }
    }

    @Override
    public void onInterrupt() {
        Toast.makeText(this, R.string.interrupt, Toast.LENGTH_SHORT).show();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        this.powerUtil.handleWakeLock(false);
    }

    @Override
    public void onDestroy() {
        this.powerUtil.handleWakeLock(false);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }
}