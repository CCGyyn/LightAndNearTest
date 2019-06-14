package com.miki.ccg.lightandneartest;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

/**
 * @author：ccg on 2019/6/13 20:43
 */
public class IsAutoObserver extends ContentObserver {
    private int MSG_IS_AUTO = 2;
    Context mContext;
    Handler mHandler;
    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public IsAutoObserver(Context context, Handler handler) {
        super(handler);
        mContext = context;
        mHandler = handler;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        boolean automicBrightness = false;
        ContentResolver resolver = mContext.getContentResolver();
        try {
            automicBrightness = Settings.System.getInt(resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE)
                    == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        mHandler.obtainMessage(MSG_IS_AUTO, automicBrightness).sendToTarget();
    }
}
