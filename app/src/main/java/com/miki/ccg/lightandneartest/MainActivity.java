package com.miki.ccg.lightandneartest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.util.Spline;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.text.DecimalFormat;


/**
 * @author ccg
 *
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener{

    TextView tvProximity;
    TextView tvLight;
    TextView screenBrightness;
    EditText screenEdit;
    Button screenButton;
    SwitchCompat switchBrightness;
    SensorManager sensorManager;
    private SeekBar seekBar;
    private int maxEdit = 255;
    private int minEdit = 0;
    private int mMinimumBacklight;
    private int mMaximumBacklight;
    private ContentObserver mBrightnessObserver;
    Context mContext;
    Spline.MonotoneCubicSpline mScreenAutoBrightnessSpline;
    // 是否开启自动亮度调节
    boolean isScreenAutoBrightness = true;
    DecimalFormat decimalFormat=new DecimalFormat("0.0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvProximity = (TextView) findViewById(R.id.tv_proximity);
        tvLight = (TextView) findViewById(R.id.tv_light);
        screenBrightness = (TextView) findViewById(R.id.screen_brightness);
        screenEdit = (EditText) findViewById(R.id.screen_edit);
        screenButton = (Button) findViewById(R.id.screen_button);
        switchBrightness = (SwitchCompat) findViewById(R.id.switch_brightness);
        seekBar = (SeekBar) findViewById(R.id.sb);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        final TextView test = (TextView) findViewById(R.id.test);
        mContext = this;
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        // 隐藏API
        mMinimumBacklight = pm.getMinimumScreenBrightnessForVrSetting();
        mMaximumBacklight = pm.getMaximumScreenBrightnessForVrSetting();

        int[] luxInt = getResources().getIntArray(
                com.android.internal.R.array.config_autoBrightnessLevels);
        int[] screenBrightnessInt = getResources().getIntArray(
                com.android.internal.R.array.config_autoBrightnessLcdBacklightValues);
        float[] luxFloat = new float[9];
        float[] screenBrightnessFloat = new float[9];
        luxFloat[0] = 1;
        luxFloat[1] = 40;
        luxFloat[2] = 100;
        luxFloat[3] = 325;
        luxFloat[4] = 600;
        luxFloat[5] = 1250;
        luxFloat[6] = 2200;
        luxFloat[7] = 4000;
        luxFloat[8] = 10000;
        screenBrightnessFloat[0] = 0;
        screenBrightnessFloat[1] = 11;
        screenBrightnessFloat[2] = 22;
        screenBrightnessFloat[3] = 47;
        screenBrightnessFloat[4] = 61;
        screenBrightnessFloat[5] = 84;
        screenBrightnessFloat[6] = 107;
        screenBrightnessFloat[7] = 154;
        screenBrightnessFloat[8] = 212;
        /*for(int i = 0;i < luxInt.length;i++) {
            luxFloat[i] = (float) luxInt[i];
        }
        for (int j = 0;j < screenBrightnessInt.length;j++) {
            screenBrightnessFloat[j] = (float) screenBrightnessInt[j];
        }*/

        Log.d("Mainactivity", "createSpline前");
        Log.d("Mainactivity", String.valueOf(luxFloat.length));
        Log.d("Mainactivity", String.valueOf(screenBrightnessFloat.length));
        Spline spline = Spline.createSpline(luxFloat, screenBrightnessFloat);
        Log.d("Mainactivity", "createSpline后");
        mScreenAutoBrightnessSpline = (Spline.MonotoneCubicSpline) spline;
        Log.d("Mainactivity", "MonotoneCubicSpline");
//        mScreenAutoBrightnessSpline = new Spline.MonotoneCubicSpline();
        // 申请android.permission.WRITE_SETTINGS权限的方式
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 如果当前平台版本大于23
            if(!Settings.System.canWrite(this)) {
                // 如果没有修改系统的权限这请求修改系统的权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, 0);
            } else {

            }
        }

        // 开关按钮注册
        switchBrightnessShow();
        // 设置屏幕亮度按钮注册
        screenButtonShow();
        // 下拉条注册
        processShow();
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Message msg = new Message();
                        msg.what = 1;
                        handler.sendMessage(msg);
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            private Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case 1:
                            if(!isScreenAutoBrightness) {
                                StringBuffer sb = new StringBuffer();
                                float nowBrightnessValue = getBrightnessByAcitivity();
                                sb.append("当前屏幕亮度：");
                                if(nowBrightnessValue != -1) {
                                    sb.append(String.valueOf(nowBrightnessValue * 255f));
                                    screenBrightness.setText(sb.toString());
                                }
                            }
                            /*StringBuffer sb = new StringBuffer();
                            float nowBrightnessValue = getBrightnessByAcitivity();
                            sb.append("当前屏幕亮度：");
                            if(nowBrightnessValue != -1) {
                                sb.append(String.valueOf(nowBrightnessValue * 255f));
                                screenBrightness.setText(sb.toString());
                            } else {
                                *//*float autoBrightnessValue = getAutoBrightnessValue(MainActivity.this);
                                // 自动亮度调节计算公式
                                float adj = (autoBrightnessValue + 1) * ((mMaximumBacklight - mMinimumBacklight) / 2f);
                                sb.append(String.valueOf(adj));*//*
                            }*/
//                            screenBrightness.setText(sb.toString());
                            break;
                        default:
                    }
                }
            };
        }).start();
        mBrightnessObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                float auto = getAutoBrightnessValue(MainActivity.this);
                float adj = (auto + 1) * ((mMaximumBacklight - mMinimumBacklight) / 2f);
                test.setText(String.valueOf(adj));
            }
        };
    }

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
                    setScreenBrightnessByActivity(MainActivity.this, value);
                    switchBrightness.setChecked(false);
                }
            }
        });
    }

    private void switchBrightnessShow() {
        switchBrightness.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    isScreenAutoBrightness = true;
                    setScreenBrightnessByActivity(MainActivity.this,-1);
                } else {
                    isScreenAutoBrightness = false;
                    float l = getBrightnessByAcitivity();
                    if(l != -1) {
                        l *= 255f;
                        setScreenBrightnessByActivity(MainActivity.this, (int) l);
                    } else {
                        setScreenBrightnessByActivity(MainActivity.this, 255);
                    }
                }
            }
        });
    }

    /**
     * 获取当前屏幕亮度
     * @return
     */
    @Deprecated
    private int getBrightnessValue(Activity activity) {
        int nowBrightnessValue = 0;
        ContentResolver resolver = activity.getContentResolver();
        try {
            nowBrightnessValue = Settings.System.getInt(
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
        return nowBrightnessValue;
    }

    /**
     * 获取当前activity亮度
     * @return
     */
    private float getBrightnessByAcitivity() {
        return getWindow().getAttributes().screenBrightness;
    }

    /**
     * 设置当前activity亮度
     * @param activity
     * @param value
     */
    private void setScreenBrightnessByActivity(Activity activity, int value) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        if(value != -1) {
            params.screenBrightness = value / 255f;
        } else {
            params.screenBrightness = value;
        }
        activity.getWindow().setAttributes(params);
        seekBar.setProgress(value);
    }


    private void processShow() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(!fromUser) {
                } else {
                    setScreenBrightnessByActivity(MainActivity.this, seekBar.getProgress());
                    switchBrightness.setChecked(false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
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
                if(isScreenAutoBrightness) {
                    float sc = mScreenAutoBrightnessSpline.interpolate(event.values[0]);
                    Log.d("Mainactivity", "interpolate后");
                    sb.append("当前屏幕亮度：");
                    sb.append(decimalFormat.format(sc));
                    screenBrightness.setText(sb.toString());
                }
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
    protected void onResume() {
        super.onResume();
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
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor("screen_auto_brightness_adj"), true,
                mBrightnessObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        mContext.getContentResolver().unregisterContentObserver(mBrightnessObserver);
    }

    @Override
    public synchronized ComponentName startForegroundServiceAsUser(Intent service, UserHandle user) {
        return null;
    }

}
