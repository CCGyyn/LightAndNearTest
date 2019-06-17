package com.miki.ccg.lightandneartest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;


/**
 * @author ccg
 *
 */
@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class MainActivity extends AppCompatActivity implements SensorEventListener{
    /**
     * 接近传感值文本
     */
    private TextView tvProximity;
    /**
     * 光线强度文本
     */
    private TextView tvLight;
    /**
     * 当前屏幕亮度值
     */
    private TextView backLightText;
    /**
     * 设置屏幕亮度编辑框
     */
    private EditText screenEdit;
    /**
     * 设置屏幕亮度按钮
     */
    private Button screenButton;
    /**
     * 自动亮度调节开关
     */
    private SwitchCompat switchBrightness;
    /**
     * 测试时间文本
     */
    private TextView startTimeText;
    private TextView startChangeTimeText;
    private TextView stopChangeTimeText;
    private String startTime;
    private String startChangeTime;
    private String stopChangeTime;
    private String brightnessValue;
    private Timer timer;
    private int ii = 0;
    /**
     * 测试按钮
     */
    private Button excuteBt;
    /**
     * 是否正在进行测试
     */
    private boolean testFlag = false;
    /**
     * 开始变化标志
     */
    private boolean startChangeFlag = false;
    private Thread setBackLightThread;
    /**
     * 传感器
     */
    private SensorManager sensorManager;
    private Context mContext;
    /**
     * 监听回调值
     */
    private final int MSG_TEST = -1;
    private final int MSG_SYSTEM_SCREEN = 0;
    private final int MSG_AUTO_SCREEN = 1;
    private final int MSG_IS_AUTO = 2;
    private final int MSG_AUTO_DUMPSYS = 3;
    /**
     * 屏幕亮度编辑框最大值
     */
    private int maxEdit = 255;
    /**
     * 屏幕亮度编辑框最小值
     */
    private int minEdit = 0;
    /**
     * 自动背光亮度值计算用
     */
    private int mMinimumBackLight;
    private int mMaximumBackLight;
    /**
     * 系统屏幕亮度监听
     */
    private SystemScreenLightObserver systemScreenLightObserver;
    /**
     * 自动背光亮度监听
     */
    private AutoScreenLightObserver autoScreenLightObserver;
    /**
     * 是否开启自动亮度调节监听
     */
    private IsAutoObserver isAutoObserver;
    private DecimalFormat decimalFormat;
    private SimpleDateFormat dateFormat;
    private String TAG = getClass().getSimpleName();

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if(testFlag) {
                if(ii < 50) {
                    String str = getAutoBackLightByDumpsys();
                    backLightText.setText(str);
                    if(!brightnessValue.equals(str)) {
                        stopChangeTime = dateFormat.format(System.currentTimeMillis());
                        if(!startChangeFlag) {
                            startChangeFlag = true;
                            startChangeTime = dateFormat.format(System.currentTimeMillis());
                            startTimeText.setText(startChangeTime);
                        }
                        ii = 0;
                        brightnessValue = str;
                    }
                    return false;
                }
                clearAll();
                stopChangeTimeText.setText(stopChangeTime);
                return false;
            }
            switch (msg.what) {
                case MSG_SYSTEM_SCREEN: {
                    float nowBrightnessValue = (Float) msg.obj;
                    backLightText.setText(decimalFormat.format(nowBrightnessValue));
                    break;
                }
                case MSG_AUTO_SCREEN: {
                    float adj = (Float) msg.obj;
                    // 从[-1, 1]转换为[0, 255]
                    float nowBrightnessValue = (adj + 1) * ((mMaximumBackLight - mMinimumBackLight) / 2f);
                    backLightText.setText(decimalFormat.format(nowBrightnessValue));
                    break;
                }
                case MSG_IS_AUTO : {
                    boolean automicBrightness = (Boolean) msg.obj;
                    float nowBrightnessValue = 0;
                    if(!automicBrightness) {
                        nowBrightnessValue = getBrightnessValue(MainActivity.this);
                    } else {
                        nowBrightnessValue = getAutoBrightnessValue(MainActivity.this);
                    }
                    backLightText.setText(decimalFormat.format(nowBrightnessValue));
                    break;
                }
                case MSG_AUTO_DUMPSYS: {
                    String backLight = (String) msg.obj;
                    Log.d(TAG, "MSG_AUTO_DUMPSYS");
                    backLightText.setText(backLight);
                    Log.d(TAG, "finish");
                    break;
                }
                default:
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvProximity = (TextView) findViewById(R.id.tv_proximity);
        tvLight = (TextView) findViewById(R.id.tv_light);
        backLightText = (TextView) findViewById(R.id.back_light);
        screenEdit = (EditText) findViewById(R.id.screen_edit);
        screenButton = (Button) findViewById(R.id.screen_button);
        switchBrightness = (SwitchCompat) findViewById(R.id.switch_brightness);
        excuteBt = (Button) findViewById(R.id.test_bt);
        startTimeText = (TextView) findViewById(R.id.start_time);
        startChangeTimeText = (TextView) findViewById(R.id.change_start_time);
        stopChangeTimeText = (TextView) findViewById(R.id.change_end_time);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mContext = this;
        decimalFormat = new DecimalFormat("0");
        dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        // 隐藏API
        mMinimumBackLight = pm.getMinimumScreenBrightnessForVrSetting();
        mMaximumBackLight = pm.getMaximumScreenBrightnessForVrSetting();
        // 开关按钮注册
        switchBrightnessShow();
        // 设置屏幕亮度按钮注册
        screenButtonShow();
        // 测试按钮注册
        excuteBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimeText.setText("");
                startChangeTimeText.setText("");
                stopChangeTimeText.setText("");
                excuteBt.setText("Testing");
                excuteBt.setEnabled(false);
                testFlag = true;
                try {
                    setBackLightThread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 这里也许有错
                Log.d(TAG, "brightnessValue = " + brightnessValue);
                brightnessValue = backLightText.getText().toString();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mHandler.sendEmptyMessage(MSG_TEST);
                    }
                }, 10L, 200L);
                startTime = dateFormat.format(System.currentTimeMillis());
                startTimeText.setText(startTime);
            }
        });

        float nowBrightnessValue = 0;
        // 判断系统是否开启自动背光
        boolean isAutoBrightness = isAutoBrightness(MainActivity.this);
        if(isAutoBrightness) {
            switchBrightness.setChecked(true);
            nowBrightnessValue = getAutoBrightnessValue(MainActivity.this);
        } else {
            switchBrightness.setChecked(false);
            nowBrightnessValue = getBrightnessValue(MainActivity.this);
        }
        backLightText.setText(decimalFormat.format(nowBrightnessValue));
        // 实例化监听
        instanceObserver();
        setBackLightThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    boolean isAutoBrightness = isAutoBrightness(MainActivity.this);
                    if(isAutoBrightness) {
                        Log.d(TAG, "子线程adb命令--------");
                        String backLight = getAutoBackLightByDumpsys();
                        Log.d(TAG, backLight);
                        if(!backLight.isEmpty()) {
                            mHandler.obtainMessage(MSG_AUTO_DUMPSYS, backLight).sendToTarget();
                        }
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        setBackLightThread.start();
    }

    /**
     * 测试结束
     */
    private void clearAll() {
        excuteBt.setText("Start");
        excuteBt.setEnabled(true);
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
        ii = 0;
        testFlag = false;
        setBackLightThread.notify();
        startChangeFlag = false;
    }

    /**
     * 获取自动背光值
     * @return
     */
    private String getAutoBackLightByDumpsys() {
        String cmd = "dumpsys display";
        BufferedReader reader = null;
        String content = "";
        String backLight = "";
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuffer output = new StringBuffer();
            int read;
            char[] buffer = new char[1024];
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();
            content = output.toString();
            String[] s1 = content.split("mScreenAutoBrightness=");
            String[] s2 = s1[1].split(" ");
            backLight = s2[0];
            return backLight.trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return backLight;
    }

    /**
     * 实例化监听器
     */
    private void instanceObserver() {
        systemScreenLightObserver = new SystemScreenLightObserver(mContext, mHandler);
        autoScreenLightObserver = new AutoScreenLightObserver(mContext, mHandler);
        isAutoObserver = new IsAutoObserver(mContext, mHandler);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 注册传感器
        registerSensor();
        // 注册监听
        registerObserver();

    }

    /**
     * 注册监听
     */
    private void registerObserver() {
        // 监听Uri
        Uri uriSystemScreenLight = Settings.System.getUriFor(
                Settings.System.SCREEN_BRIGHTNESS
        );
        Uri uriAutoScreenLight = Settings.System.getUriFor(
                "screen_auto_brightness_adj"
        );
        Uri uriIsAuto = Settings.System.getUriFor(
                Settings.System.SCREEN_BRIGHTNESS_MODE
        );
        // 注册监听
        getContentResolver().registerContentObserver(
                uriSystemScreenLight,
                false,
                systemScreenLightObserver
        );
        getContentResolver().registerContentObserver(
                uriAutoScreenLight,
                false,
                autoScreenLightObserver
        );
        getContentResolver().registerContentObserver(
                uriIsAuto,
                false,
                isAutoObserver
        );
    }

    /**
     * 注册传感器
     */
    private void registerSensor() {
        // 获取传感器
        Sensor sensorProximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        Sensor sensorLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        // 注册传感器
        sensorManager.registerListener(
                this,
                sensorProximity,
                SensorManager.SENSOR_DELAY_UI
        );
        sensorManager.registerListener(
                this,
                sensorLight,
                SensorManager.SENSOR_DELAY_UI
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消注册
        getContentResolver().unregisterContentObserver(systemScreenLightObserver);
        getContentResolver().unregisterContentObserver(autoScreenLightObserver);
        getContentResolver().unregisterContentObserver(isAutoObserver);
    }

    /**
     * 设置屏幕亮度按钮
     */
    private void screenButtonShow() {
        screenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!screenEdit.getText().toString().isEmpty()) {
                    int value = Integer.parseInt(screenEdit.getText().toString());
                    if(value > maxEdit) {
                        screenEdit.setError("不能超过255");
                        return;
                    } else if(value < minEdit) {
                        screenEdit.setError("不能小于0");
                        return;
                    }
                    boolean isAutoBrightness = isAutoBrightness(MainActivity.this);
                    if(!isAutoBrightness) {
                        setScreenBrightness(MainActivity.this, value);
                    } else {
                        setAutoScreenBrightness(MainActivity.this, value);
                    }

                }
            }
        });
    }

    /**
     * 设置是否自动亮度调节开关
     */
    private void switchBrightnessShow() {
        switchBrightness.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    startAutoBrightness(MainActivity.this);
//                    setScreenBrightnessByActivity(MainActivity.this,-1);
                } else {
                    stopAutoBrightness(MainActivity.this);
//                    float l = getBrightnessByAcitivity();
                    /*float l = getBrightnessValue(MainActivity.this);
                    setScreenBrightness(MainActivity.this, (int) l);*/
                }
            }
        });
    }

    /**
     * 获取当前系统屏幕亮度
     * @return
     */
    private float getBrightnessValue(Activity activity) {
        float nowBrightnessValue = 0;
        ContentResolver resolver = activity.getContentResolver();
        try {
            nowBrightnessValue = Settings.System.getFloat(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS
                    );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nowBrightnessValue;
    }

    /**
     * 获取自动调节亮度值
     * @param activity
     * @return
     */
    private float getAutoBrightnessValue(Activity activity) {
        float nowBrightnessValue = 0;
        ContentResolver resolver = activity.getContentResolver();
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
        float adj = (nowBrightnessValue + 1) * ((mMaximumBackLight - mMinimumBackLight) / 2f);
        return adj;
    }

    /**
     * 获取当前activity亮度
     * @return
     */
    @Deprecated
    private float getBrightnessByAcitivity() {
        return getWindow().getAttributes().screenBrightness;
    }

    /**
     * 设置当前activity亮度
     * @param activity
     * @param value
     */
    @Deprecated
    private void setScreenBrightnessByActivity(Activity activity, int value) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        if(value != -1) {
            params.screenBrightness = value / 255f;
        } else {
            params.screenBrightness = value;
        }
        activity.getWindow().setAttributes(params);
    }

    /**
     * 设置系统屏幕亮度
     * @param activity
     * @param value
     */
    private void setScreenBrightness(Activity activity, int value) {
        Settings.System.putInt(activity.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                value
                );
        Uri uri = Settings.System.getUriFor(
                Settings.System.SCREEN_BRIGHTNESS
        );
        activity.getApplicationContext().getContentResolver().notifyChange(uri, systemScreenLightObserver);
    }

    /**
     * 设置自动亮度调节值
     * @param activity
     * @param value
     */
    private void setAutoScreenBrightness(Activity activity, int value) {
        float adj = value / ((mMaximumBackLight - mMinimumBackLight) / 2f) - 1;
        Settings.System.putFloat(activity.getContentResolver(),
                "screen_auto_brightness_adj",
                adj
                );
        Uri uri = Settings.System.getUriFor(
                "screen_auto_brightness_adj"
        );
        activity.getApplicationContext().getContentResolver().notifyChange(uri, autoScreenLightObserver);
    }
    /**
     * 判断是否开启自动亮度调节
     * @param activity
     * @return
     */
    public static boolean isAutoBrightness(Activity activity) {

        boolean automicBrightness = false;
        ContentResolver resolver = activity.getContentResolver();
        try {
            automicBrightness = Settings.System.getInt(resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE)
                    == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return automicBrightness;

    }

    /**
     * 开启自动亮度调节
     * @param activity
     */
    public static void startAutoBrightness(Activity activity) {

        Settings.System.putInt(activity.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    /**
     * 关闭自动亮度调节
     * @param activity
     */
    public static void stopAutoBrightness(Activity activity) {

        Settings.System.putInt(activity.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

    }
    /**
     * 传感器数据变化时回调
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_PROXIMITY:{
                StringBuffer sb = new StringBuffer();
                sb.append("接近传感值：");
                sb.append(String.valueOf(event.values[0]));
                tvProximity.setText(sb.toString());
                break;
            }
            case Sensor.TYPE_LIGHT:{
                StringBuffer sb = new StringBuffer();
                sb.append("光线强度：");
                sb.append(String.valueOf(event.values[0]));
                tvLight.setText(sb.toString());
                sb.setLength(0);
                break;
            }
            default:
        }
    }

    /**
     * 传感器精度变化时回调
     * @param sensor
     * @param accuracy 精度
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }



    @Override
    public synchronized ComponentName startForegroundServiceAsUser(Intent service, UserHandle user) {
        return null;
    }

}
