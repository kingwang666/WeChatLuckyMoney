package xyz.monkeytong.hongbao.services;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;
import java.util.regex.Pattern;

import xyz.monkeytong.hongbao.R;
import xyz.monkeytong.hongbao.utils.PowerUtil;

public class HongbaoService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "HongbaoService";

    private static final String WECHAT_DETAILS_CH = "红包详情";
    private static final String WECHAT_DETAILS_2_CH = "红包记录";
    private static final String WECHAT_OPENED = "已存入零钱";
    private static final String WECHAT_BETTER_LUCK_CH = "手慢了";
    private static final String WECHAT_BETTER_LUCK_2_CH = "手慢了，红包派完了";
    private static final String WECHAT_BETTER_LUCK_3_CH = "看看大家的手气";
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

    private AccessibilityNodeInfo /*rootNodeInfo,*/ mReceiveNode, mUnpackNode;
    private boolean mLuckyMoneyPicked, mLuckyMoneyReceived;
    private int mUnpackCount = 0;
    private boolean mMutex = false, mListMutex = false, mChatMutex = false, mOpened = false;
    private boolean mRedPackOpening = false;
    private int mCurrentChatWindowsId;
    private int mCurrentReceiveWindowId;
    private int mCurrentDetailWindowId;

    private boolean mForceCheckWindow;

    private final Pattern mGroupChat = Pattern.compile("\\(\\d+?\\)");

    private PowerUtil powerUtil;
    private SharedPreferences sharedPreferences;

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

        setCurrentActivityName(event);
        if (!isInReceiveActivity() || getActiveWindowId(event) == mCurrentChatWindowsId) {
            mRedPackOpening = false;
            getHandler().removeCallbacks(mOpenPackCallback);
        }
        Log.d(TAG, "class: " + event.getClassName() + "  type: " + event.getEventType() + " content type: " + event.getContentChangeTypes());

        if (sharedPreferences == null) return;
        if (!mMutex) {
            if (sharedPreferences.getBoolean("pref_watch_list", false) && watchList(event)) return;
            mListMutex = false;
        }

        if (!mChatMutex) {
            mChatMutex = true;
            if (sharedPreferences.getBoolean("pref_watch_chat", false)) watchChat(event);
            mChatMutex = false;
        }
    }

    private void watchChat(AccessibilityEvent event) {
//        this.rootNodeInfo = getRootInActiveWindow();

//        if (rootNodeInfo == null) return;

        mReceiveNode = null;
        mUnpackNode = null;

        checkNodeInfo(event.getEventType());

        /* 如果已经接收到红包并且还没有戳开 */
        Log.d(TAG, "watchChat mLuckyMoneyReceived:" + mLuckyMoneyReceived + " mLuckyMoneyPicked:" + mLuckyMoneyPicked + " mReceiveNode:" + mReceiveNode);
        if (mLuckyMoneyReceived && (mReceiveNode != null) && isInChatActivity()) {
            mMutex = true;
            mOpened = true;
            mReceiveNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
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
        Log.d(TAG, "戳开红包！" + " mUnpackCount: " + mUnpackCount + " mUnpackNode: " + mUnpackNode);
        if (mUnpackCount >= 1 && (mUnpackNode != null) || canOpen()) {
            int delayFlag = sharedPreferences.getInt("pref_open_delay", 0) * 1000;
            if (delayFlag != 0) {
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
                        delayFlag);
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
            Log.d(TAG, "openPacket！");
            mOpened = true;
            mRedPackOpening = true;
            mUnpackNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            resetUnpackState();
        }
    }

    private boolean canOpen() {
        if (!isInReceiveActivity()) {
            return false;
        }
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo == null) {
            return false;
        }
        String currentActivityName = getCurrentActivityName();
        boolean hasNodes = this.hasOneOfThoseNodes(rootNodeInfo, WECHAT_OPENED,
                WECHAT_BETTER_LUCK_CH, WECHAT_BETTER_LUCK_2_CH, WECHAT_BETTER_LUCK_3_CH,
                WECHAT_DETAILS_CH, WECHAT_DETAILS_2_CH, WECHAT_EXPIRES_CH, WECHAT_EXPIRES_2_CH);
        if (hasNodes) {
            clickBackIfNeed();
            resetUnpackState();
            mRedPackOpening = false;
            getHandler().removeCallbacks(mOpenPackCallback);
            return false;
        }
        //再次检查，以防上次没检测到
        if (mUnpackNode == null) {
            /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
            AccessibilityNodeInfo unpackNode = findOpenButton(rootNodeInfo);
            Log.d(TAG, "node2 " + unpackNode);
            if (unpackNode != null && currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY)) {
                mUnpackNode = unpackNode;
                mUnpackCount += 1;
                getHandler().removeCallbacks(mOpenPackCallback);
            } else {
                getHandler().removeCallbacks(mOpenPackCallback);
                getHandler().postDelayed(mOpenPackCallback, 100);
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
                Log.e(TAG, currentWindowId + " ", e);
                if (!checkCurrentActivityName(currentWindowId)){
                    mForceCheckWindow = true;
                }
            }
            Log.d(TAG, "CurrentActivity: " + currentActivityName);
        } else if (mForceCheckWindow) {
            checkCurrentActivityName();
        }
    }

    private int getActiveWindowId(AccessibilityEvent event){
        AccessibilityNodeInfo info = getRootInActiveWindow();
        if (info != null){
            return info.getWindowId();
        }
        if (event != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getContentChangeTypes() == AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED){
            return event.getWindowId();
        }
        return -1;
    }

    private boolean checkCurrentActivityName() {
        AccessibilityNodeInfo info = getRootInActiveWindow();
        if (info == null) {
            return false;
        }
        return checkCurrentActivityName(info.getWindowId());
    }

    private boolean checkCurrentActivityName(int windowId) {
        if (windowId == -1){
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
        if (getActiveWindowId(null) == mCurrentChatWindowsId){
            return false;
        }
        return getCurrentActivityName().contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY);
    }

    private boolean isInChatActivity() {
        return isInChatActivity(true);
    }

    private boolean isInChatActivity(boolean checkId) {
        if (checkId && getActiveWindowId(null) != mCurrentChatWindowsId){
            return false;
        }
        String currentActivityName = getCurrentActivityName();
        return currentActivityName.contains(WECHAT_LUCKMONEY_CHATTING_ACTIVITY) || currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY);
    }

    private boolean isGroupChat(AccessibilityNodeInfo rootNodeInfo) {
        if (getActiveWindowId(null) != mCurrentChatWindowsId){
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
        if (checkId && getActiveWindowId(null) == mCurrentChatWindowsId){
            return false;
        }
        String currentActivityName = getCurrentActivityName();
        return isLuckyMoney() && (currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_UI_ACTIVITY) || currentActivityName.contains(WECHAT_LUCKMONEY_NOT_HOOK_RECEIVE_UI_ACTIVITY));
    }

    private boolean isInDetailActivity() {
        return isInDetailActivity(true);
    }

    private boolean isInDetailActivity(boolean checkId) {
        if (checkId && getActiveWindowId(null) == mCurrentChatWindowsId){
            return false;
        }
        String currentActivityName = getCurrentActivityName();
        return isLuckyMoney() && (currentActivityName.contains(WECHAT_LUCKMONEY_DETAIL_ACTIVITY));
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
        if (node == null || node.getWindowId() == mCurrentChatWindowsId || node.getWindowId() == mCurrentDetailWindowId || mRedPackOpening){
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
        Log.e(TAG, node.getClassName().toString() + "   " + node.getContentDescription() + "  " + node.getText() + "  " + currentActivityName);
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
                if (TextUtils.isEmpty(hongbaoContent) || hongbaoContent.contains("已被领完") || hongbaoContent.contains("已领取") || hongbaoContent.contains("已过期"))
                    return null;
            }
            boolean self = sharedPreferences.getBoolean("pref_watch_self", false);
            if (!self) {
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

    private void checkNodeInfo(final int eventType) {
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo == null) return;

        /* 聊天会话窗口，遍历节点匹配“微信红包”，“领取红包”和"查看红包" */
        AccessibilityNodeInfo nodeText;
        if (isInChatActivity() && isGroupChat(rootNodeInfo) && (nodeText = getTheLastNode(WECHAT_VIEW_ALL_CH, WECHAT_VIEW_OTHERS_CH, WECHAT_VIEW_SELF_CH)) != null) {
            AccessibilityNodeInfo receiveNode = getNewHongbaoNode(nodeText);
            if (receiveNode != null) {
                mLuckyMoneyReceived = true;
                mReceiveNode = receiveNode;
            }
            return;
        }

        /* 为了能发现 拆红包的按钮 */
        if (isInReceiveActivity() && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
        AccessibilityNodeInfo unpackNode;
        if (isInReceiveActivity() && (unpackNode = findOpenButton(rootNodeInfo)) != null && (mUnpackNode == null || !mUnpackNode.equals(unpackNode))) {
            Log.d(TAG, "checkNodeInfo  node2 " + unpackNode);
            mUnpackNode = unpackNode;
            mUnpackCount += 1;
            return;
        }
        clickBackIfNeed();
    }

    private void clickBackIfNeed() {
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo == null) return;
        String currentActivityName = getCurrentActivityName();
        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”和“手慢了” */
        boolean hasNodes = hasOneOfThoseNodes(rootNodeInfo, WECHAT_OPENED,
                WECHAT_BETTER_LUCK_CH, WECHAT_BETTER_LUCK_2_CH, WECHAT_DETAILS_CH, WECHAT_DETAILS_2_CH, WECHAT_BETTER_LUCK_3_CH,
                WECHAT_EXPIRES_CH, WECHAT_EXPIRES_2_CH);
        Log.d(TAG, "checkNodeInfo  hasNodes:" + hasNodes + " opened: " + mOpened + " mMutex:" + mMutex + " name: " + currentActivityName);
        if ((isInDetailActivity() || isInReceiveActivity()) && hasNodes) {
            mMutex = false;
            mLuckyMoneyPicked = false;
            mRedPackOpening = false;
            resetUnpackState();
            if (mOpened && sharedPreferences.getBoolean("pref_open_after_back", false)) {
                mOpened = false;
                Log.d(TAG, "back click");
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

    private AccessibilityNodeInfo getTheLastNode(String... texts) {
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo == null) {
            return null;
        }
        int bottom = 0;
        AccessibilityNodeInfo lastNode = null, tempNode;
        List<AccessibilityNodeInfo> nodes;
        for (String text : texts) {
            if (text == null) continue;
            nodes = rootNodeInfo.findAccessibilityNodeInfosByText(text);

            if (nodes != null && !nodes.isEmpty()) {
                tempNode = nodes.get(nodes.size() - 1);
                if (tempNode == null) return null;
                Rect bounds = new Rect();
                tempNode.getBoundsInScreen(bounds);
                if (bounds.bottom > bottom) {
                    bottom = bounds.bottom;
                    lastNode = tempNode;
                }
            }
        }
        return lastNode;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        this.watchFlagsFromPreference();
    }

    private void watchFlagsFromPreference() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        this.powerUtil = PowerUtil.getInstance(this);
        boolean watchOnLockFlag = sharedPreferences.getBoolean("pref_keep_screen_on", false);
        this.powerUtil.handleWakeLock(watchOnLockFlag);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_keep_screen_on")) {
            boolean changedValue = sharedPreferences.getBoolean(key, false);
            this.powerUtil.handleWakeLock(changedValue);
        }
    }

    @Override
    public void onInterrupt() {
        Toast.makeText(this, R.string.interrupt, Toast.LENGTH_SHORT).show();
        this.powerUtil.handleWakeLock(false);
    }

    @Override
    public void onDestroy() {
        this.powerUtil.handleWakeLock(false);
        super.onDestroy();
    }
}