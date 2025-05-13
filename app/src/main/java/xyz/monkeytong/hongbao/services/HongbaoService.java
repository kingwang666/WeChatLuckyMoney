package xyz.monkeytong.hongbao.services;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.Objects;

import xyz.monkeytong.hongbao.BuildConfig;
import xyz.monkeytong.hongbao.R;
import xyz.monkeytong.hongbao.compat.Wechat;
import xyz.monkeytong.hongbao.compat.WechatCompat;
import xyz.monkeytong.hongbao.utils.PowerUtil;

public class HongbaoService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "HongbaoService";

    private static final int WHAT_FIND_RECEIVED_NODE = 0;
    private static final int WHAT_FIND_OPEN_NODE = 1;

    private static final int DELAY_MAJOR_TIME = 40;
    private static final int DELAY_MINOR_TIME = 20;

    private boolean mIsPaused = false;

    private Wechat mWechat;
    private String mCurrentActivityName;
    private int mCurrentWindowId;
    private int mCurrentChatWindowId;

    private boolean mMutex = false;
    private boolean mListMutex = false;
    private boolean mOpened = false;
    private boolean mRedPackOpening = false;

    private boolean mWatchList = false;
    private boolean mWatchChat = true;
    private boolean mOpenSelf = true;
    private int mOpenDelay;
    private boolean mBackAfterOpen = true;
    private boolean mOnlyLastNode = true;
    private boolean mExcludeExclusive = true;

    private PowerUtil powerUtil;

    private Handler mHandler;

    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        mWechat = WechatCompat.getWechat(this);
        mCurrentActivityName = mWechat.getGeneralActivityName();
        mCurrentWindowId = 0;
        mCurrentChatWindowId = 0;
    }

    /**
     * AccessibilityEvent
     *
     * @param event 事件
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        long startTime = 0;
        try {
            if (BuildConfig.DEBUG) {
                startTime = System.currentTimeMillis();
                Log.d(TAG, "class: " + event.getClassName() + " window: " + event.getWindowId() + "  type: " + event.getEventType());
            }


            String oldActivity = getCurrentActivityName();
            setCurrentActivityName(event);

            if (mIsPaused) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onAccessibilityEvent: paused");
                }
                return;
            }

            String newActivity = getCurrentActivityName();

            boolean activityChanged = !Objects.equals(oldActivity, newActivity);
            if (activityChanged) {
                if (!isInReceiveActivity()) {
                    resetUnpackState();
                }
                if (!isInChatActivity()) {
                    getHandler().removeMessages(WHAT_FIND_RECEIVED_NODE);
                }
            }


            if (!mMutex) {
                if (mWatchList && watchList(event)) {
                    return;
                }
                mListMutex = false;
            }

            if (mWatchChat) {
                watchChat(event);
                watchLuckyMoneyDetail(event);
                clickBackIfNeed(true);
            }


        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "error", e);
            }
        } finally {
            if (BuildConfig.DEBUG) {
                long time = System.currentTimeMillis() - startTime;
                Log.d(TAG, "end time: " + time);
            }
        }
    }

    private boolean watchList(AccessibilityEvent event) {
        if (mListMutex || !isInChatActivity() || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return false;
        }
        mListMutex = true;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return false;
        }
        AccessibilityNodeInfo chatItem = mWechat.findChatItem(root);
        if (chatItem != null) {
            chatItem.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }
        return false;
    }

    private void watchChat(AccessibilityEvent event) {
        findReceiveNodeAndClick();
    }

    private void findReceiveNodeAndClick() {
        findReceiveNodeAndClick(false);
    }

    private void findReceiveNodeAndClick(boolean forceSendMessage) {
        if (!isInChatActivity()) {
            return;
        }
        AccessibilityNodeInfo receiveNode = findReceiveNodeInfo();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "watchChat find receive node:" + receiveNode);
        }
        /* 如果已经接收到红包并且还没有戳开 */
        if (receiveNode != null) {
            boolean refreshResult = receiveNode.refresh();
            Log.w(TAG, "receive node refresh: " + refreshResult);
            if (!refreshResult) {
                return;
            }
            mMutex = true;
            resetUnpackState();

            boolean result = receiveNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            mOpened = result;
            Log.w(TAG, "receive opened: " + mOpened);
            long delay;
            if (result) {
                delay = DELAY_MAJOR_TIME;
            } else {
                delay = DELAY_MINOR_TIME;
            }
            Handler handler = getHandler();
            //做检测 防止点击无效
            if (!handler.hasMessages(WHAT_FIND_RECEIVED_NODE)) {
                handler.sendMessageDelayed(handler.obtainMessage(WHAT_FIND_RECEIVED_NODE), delay);
            }
        } else if (forceSendMessage) {
            Handler handler = getHandler();
            //做检测 防止点击无效
            if (!handler.hasMessages(WHAT_FIND_RECEIVED_NODE)) {
                handler.sendMessageDelayed(handler.obtainMessage(WHAT_FIND_RECEIVED_NODE), DELAY_MAJOR_TIME);
            }
        }
    }


    private void watchLuckyMoneyDetail(AccessibilityEvent event) {
        if (mRedPackOpening || !mOpened) {
            return;
        }
        findOpenNodeAndClick(false);
    }

    private void sendFindNodeMessage(boolean shouldDelay) {
        Handler handler = getHandler();
        if (!handler.hasMessages(WHAT_FIND_OPEN_NODE)) {
            handler.sendMessageDelayed(handler.obtainMessage(WHAT_FIND_OPEN_NODE, shouldDelay), DELAY_MINOR_TIME);
        }
    }

    private void openPacketWithDelay(@NonNull AccessibilityNodeInfo openNode, boolean shouldDelay) {
        /* 如果戳开但还未领取 */
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "戳开红包！" + openNode);
        }
        if (mOpenDelay != 0 && shouldDelay) {
            getHandler().postDelayed(
                    new Runnable() {
                        public void run() {
                            try {
                                openPacket(openNode);
                            } catch (Exception e) {
                                mMutex = false;
                                resetUnpackState();
                            }
                        }
                    },
                    mOpenDelay * 1000L);
        } else {
            openPacket(openNode);
        }

    }

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new HongbaoHandler();
        }
        return mHandler;
    }

    private void openPacket(@NonNull AccessibilityNodeInfo openNode) {
        boolean result = openNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        Log.w(TAG, "openPacket！" + result);
        long delay;
        if (result) {
            mRedPackOpening = true;
            delay = DELAY_MAJOR_TIME;
        } else {
            delay = DELAY_MINOR_TIME;
        }
        Handler handler = getHandler();
        //做检测 防止点击无效
        if (!handler.hasMessages(WHAT_FIND_OPEN_NODE)) {
            handler.sendMessageDelayed(handler.obtainMessage(WHAT_FIND_OPEN_NODE, false), delay);
        }

    }

    private void findOpenNodeAndClick(boolean shouldDelay) {
        if (!isInReceiveActivity()) {
            return;
        }
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo == null) {
            sendFindNodeMessage(shouldDelay);
            return;
        }

        boolean notAvailable = mWechat.isNotAvailable(rootNodeInfo);
        if (notAvailable) {
            clickBackIfNeed(false);
            resetUnpackState();
            return;
        }

        /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
        AccessibilityNodeInfo openNode = findOpenButton(rootNodeInfo);
        if (openNode != null && openNode.refresh()) {
            Log.i(TAG, "find open node:" + openNode);
            getHandler().removeMessages(WHAT_FIND_OPEN_NODE);
            openPacketWithDelay(openNode, shouldDelay);
        } else {
            sendFindNodeMessage(shouldDelay);
        }
    }

    private void resetUnpackState() {
        getHandler().removeCallbacksAndMessages(null);
        mRedPackOpening = false;
    }

    private void setCurrentActivityName(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            try {
                ComponentName componentName = new ComponentName(
                        event.getPackageName().toString(),
                        event.getClassName().toString()
                );
                getPackageManager().getActivityInfo(componentName, 0);
                mCurrentActivityName = componentName.flattenToShortString();
                mCurrentWindowId = event.getWindowId();
                if (isInChatActivity(false)) {
                    mCurrentChatWindowId = mCurrentWindowId;
                }
                Log.i(TAG, "currentActivity: " + mCurrentActivityName + ", currentWindowId: " + mCurrentWindowId);
            } catch (PackageManager.NameNotFoundException e) {
                //ignore
            }

        }
    }

    private String getCurrentActivityName() {
        return mCurrentActivityName;
    }


    private boolean isLuckyMoney() {
        return mWechat.isLuckyMoney(getCurrentActivityName());
    }

    private boolean isInChatActivity() {
        return isInChatActivity(true);
    }

    private boolean isInChatActivity(boolean checkWindowId) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (checkWindowId) {
            if (nodeInfo != null && mCurrentChatWindowId != 0 && mCurrentChatWindowId == nodeInfo.getWindowId()) {
                return true;
            }
        }
        boolean result = mWechat.isChatActivity(getCurrentActivityName());
        if (checkWindowId && !result && nodeInfo != null && mCurrentWindowId != 0 && mCurrentWindowId != nodeInfo.getWindowId()) {
            return true;
        }
        return result;
    }

    private boolean isGroupChat(AccessibilityNodeInfo rootNodeInfo) {
        return mWechat.isGroupChat(rootNodeInfo);
    }

    private boolean isInReceiveActivity() {
        return isLuckyMoney() && mWechat.isReceiveActivity(getCurrentActivityName());
    }

    private boolean isInDetailActivity() {
        return isLuckyMoney() && mWechat.isDetailActivity(getCurrentActivityName());
    }


    private AccessibilityNodeInfo findOpenButton(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }
        return mWechat.findOpenNode(node);
    }


    @Nullable
    private AccessibilityNodeInfo findReceiveNodeInfo() {
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo == null) {
            return null;
        }
        /* 聊天会话窗口，遍历节点匹配“微信红包”，“领取红包”和"查看红包" */
        if (isGroupChat(rootNodeInfo)) {
            return mWechat.findLuckyMoneyNode(rootNodeInfo, mOnlyLastNode, mExcludeExclusive, mOpenSelf);
        }
        return null;
    }

    private void clickBackIfNeed(boolean checkNodes) {
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo == null || !(isInDetailActivity() || isInReceiveActivity())) {
            return;
        }
        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”和“手慢了” */
        boolean isNotAvailable = !checkNodes || mWechat.isNotAvailable(rootNodeInfo);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "not available:" + isNotAvailable + " opened: " + mOpened + " mMutex:" + mMutex);
        }
        if (isNotAvailable) {
            mMutex = false;
            resetUnpackState();
            if (mOpened && mBackAfterOpen) {
                mOpened = false;
                Log.w(TAG, "back click");
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        }
    }


    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        this.watchFlagsFromPreference();
        getHandler().removeCallbacksAndMessages(null);

        mMutex = false;
        mListMutex = false;
        mOpened = false;
        mRedPackOpening = false;
        mCurrentActivityName = mWechat.getGeneralActivityName();
        mCurrentWindowId = 0;
        mCurrentChatWindowId = 0;

        Intent service = new Intent(this, KeepAliveService.class);
        service.putExtra(HongbaoBroadcastReceiver.EXTRA_STATUS, mIsPaused);
        ContextCompat.startForegroundService(this, service);
        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = HongbaoBroadcastReceiver.register(this, this);
        }
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
        mExcludeExclusive = sharedPreferences.getBoolean("pref_exclude_exclusive", mExcludeExclusive);

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
            case "pref_exclude_exclusive":
                mExcludeExclusive = sharedPreferences.getBoolean(key, mExcludeExclusive);
                break;
        }
    }

    public void pause() {
        if (mIsPaused) {
            return;
        }
        Toast.makeText(this, R.string.pause, Toast.LENGTH_SHORT).show();
        mIsPaused = true;
        getHandler().removeCallbacksAndMessages(null);
        mMutex = false;
        mListMutex = false;
        mOpened = false;
        mRedPackOpening = false;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Notification notification = KeepAliveService.buildNotification(this, true);
        NotificationManagerCompat.from(this).notify(KeepAliveService.NOTIFICATION_ID, notification);

    }

    public void resume() {
        if (!mIsPaused) {
            return;
        }
        Toast.makeText(this, R.string.resume, Toast.LENGTH_SHORT).show();
        mIsPaused = false;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Notification notification = KeepAliveService.buildNotification(this, false);
        NotificationManagerCompat.from(this).notify(KeepAliveService.NOTIFICATION_ID, notification);
    }

    @Override
    public void onInterrupt() {
        Toast.makeText(this, R.string.interrupt, Toast.LENGTH_SHORT).show();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        this.powerUtil.handleWakeLock(false);
        getHandler().removeCallbacksAndMessages(null);
        Intent service = new Intent(this, KeepAliveService.class);
        stopService(service);
        if (mBroadcastReceiver != null) {
            HongbaoBroadcastReceiver.unregister(this, mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        this.powerUtil.handleWakeLock(false);
        getHandler().removeCallbacksAndMessages(null);
        Intent service = new Intent(this, KeepAliveService.class);
        stopService(service);
        if (mBroadcastReceiver != null) {
            HongbaoBroadcastReceiver.unregister(this, mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
    }

    private class HongbaoHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == WHAT_FIND_RECEIVED_NODE) {
                findReceiveNodeAndClick(true);
            } else if (msg.what == WHAT_FIND_OPEN_NODE) {
                boolean shouldDelay = false;
                if (msg.obj instanceof Boolean) {
                    shouldDelay = (boolean) msg.obj;
                }
                findOpenNodeAndClick(shouldDelay);
            }
        }
    }


}