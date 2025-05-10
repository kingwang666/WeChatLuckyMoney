package xyz.monkeytong.hongbao.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.lang.ref.WeakReference;

class HongbaoBroadcastReceiver extends BroadcastReceiver {

    static final String ACTION_FEATURE_STATUS = "xyz.monkeytong.hongbao.action.FEATURE_STATUS";
    static final String EXTRA_STATUS = "EXTRA_FEATURE_STATUS";

    static HongbaoBroadcastReceiver register(@NonNull Context context, @NonNull HongbaoService service) {
        HongbaoBroadcastReceiver receiver = new HongbaoBroadcastReceiver(service);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FEATURE_STATUS);
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);
        return receiver;
    }

    static void unregister(@NonNull Context context, @NonNull BroadcastReceiver receiver) {
        context.unregisterReceiver(receiver);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    private final WeakReference<HongbaoService> mServiceRef;

    private HongbaoBroadcastReceiver(HongbaoService service) {
        mServiceRef = new WeakReference<>(service);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_FEATURE_STATUS.equals(action)) {
            HongbaoService service = mServiceRef.get();
            if (service != null) {
                boolean pause = intent.getBooleanExtra(EXTRA_STATUS, false);
                if (pause) {
                    service.pause();
                } else {
                    service.resume();
                }
            }

        }
    }
}