package com.miki.ccg.lightandneartest;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

/**
 * @author：ccg on 2019/6/13 19:58
 */
public class AutoScreenLightObserver extends ContentObserver {
    private int MSF_AUTO_SCREEN = 1;
    private Context mContext;
    private Handler mHandler;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public AutoScreenLightObserver(Context context, Handler handler) {
        super(handler);
        mContext = context;
        mHandler = handler;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        float nowBrightnessValue = 0;
        ContentResolver resolver = mContext.getContentResolver();
        try {
            nowBrightnessValue = Settings.System.getFloat(
                    resolver,
                    "screen_auto_brightness_adj"
            );
            // SCREEN_AUTO_BRIGHTNESS_ADJ = "screen_auto_brightness_adj"
            // 这个API被隐藏起来了，无法直接用Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ调用
        } catch (Exception e) {
            e.printStackTrace();
        }
        mHandler.obtainMessage(MSF_AUTO_SCREEN, nowBrightnessValue).sendToTarget();
    }
}
