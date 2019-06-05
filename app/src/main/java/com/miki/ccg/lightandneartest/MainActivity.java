package com.miki.ccg.lightandneartest;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;


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
    SensorManager sensorManager;
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvProximity = (TextView) findViewById(R.id.tv_proximity);
        tvLight = (TextView) findViewById(R.id.tv_light);
        screenBrightness = (TextView) findViewById(R.id.screen_brightness);
        screenEdit = (EditText) findViewById(R.id.screen_edit);
        screenButton = (Button) findViewById(R.id.screen_button);
        seekBar = (SeekBar) findViewById(R.id.sb);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
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
        screenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int value = Integer.parseInt(screenEdit.getText().toString());
                setScreenBrightness(MainActivity.this, value);
            }
        });
    }

    /**
     * 获取当前屏幕亮度
     * @return
     */
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
     * 设置当前activity亮度
     * @param activity
     * @param value
     */
    private void setScreenBrightness(Activity activity, int value) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = value / 255f;
        activity.getWindow().setAttributes(params);
    }

    private void processShow() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

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
                int nowBrightnessValue = getBrightnessValue(MainActivity.this);
                sb.append("当前屏幕亮度：");
                sb.append(String.valueOf(nowBrightnessValue));
                screenBrightness.setText(sb.toString());
                sb.setLength(0);
                sb.append("光线强度：");
                sb.append(String.valueOf(event.values[0]));
                tvLight.setText(sb.toString());
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}
