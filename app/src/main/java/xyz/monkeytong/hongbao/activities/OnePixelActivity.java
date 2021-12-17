package xyz.monkeytong.hongbao.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

/**
 * Author: wangxiaojie6
 * Date: 2019/1/28
 */
public class OnePixelActivity extends AppCompatActivity {

    public final static String ACTION_FINISH = "xyz.monkeytong.hongbao.ONE_PIXEL_FINISH";

    private BroadcastReceiver endReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置1像素
        Window window = getWindow();
        window.setGravity(Gravity.START | Gravity.TOP);
        WindowManager.LayoutParams params = window.getAttributes();
        params.x = 0;
        params.y = 0;
        params.height = 1;
        params.width = 1;
        window.setAttributes(params);

        if (endReceiver == null) {
            //结束该页面的广播
            endReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    finish();
                }
            };
            LocalBroadcastManager.getInstance(this).registerReceiver(endReceiver, new IntentFilter(ACTION_FINISH));

        }
        //检查屏幕状态
        checkScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkScreen();
    }

    /**
     * 检查屏幕状态  isScreenOn为true  屏幕“亮”结束该Activity
     */
    private void checkScreen() {

        PowerManager pm = (PowerManager) OnePixelActivity.this.getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm != null && pm.isInteractive();
        if (isScreenOn) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (endReceiver != null){
            LocalBroadcastManager.getInstance(this).unregisterReceiver(endReceiver);
            endReceiver = null;
        }
    }
}
