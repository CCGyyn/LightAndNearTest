package com.miki.ccg.lightandneartest;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

/**
 * @author：ccg on 2019/6/13 18:14
 */
public class SystemScreenLightObserver extends ContentObserver{
    private int MSG_SYSTEM_SCREEN = 0;
    private Context mContext;
    private Handler mHandler;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public SystemScreenLightObserver(Context context, Handler handler) {
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
                    Settings.System.SCREEN_BRIGHTNESS
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        mHandler.obtainMessage(MSG_SYSTEM_SCREEN, nowBrightnessValue).sendToTarget();
    }
}
