package com.example.jdc.cam8;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.os.Bundle;
import android.view.WindowManager;
import com.example.jdc.cam8.utils.CheckPermissionUtils;

public class MainActivity extends AppCompatActivity{
    private String TAG = "testtt";
    private camera_func cam_func;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide(); //before AppCompat.V7 use "getActionBar()".
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        initPermission();
        cam_func = new camera_func();
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_main,cam_func).commit();
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_main,cam_func).commit();
    }


    private void initPermission() {
        String[] permissions = CheckPermissionUtils.checkPermission(this);
        if (permissions.length == 0) {

        } else {
            ActivityCompat.requestPermissions(this, permissions, 100);
        }
    }


    private void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
