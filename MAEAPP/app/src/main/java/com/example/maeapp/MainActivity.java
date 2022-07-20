package com.example.maeapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout lo_ident = (LinearLayout) findViewById(R.id.lo_ident);  // 双重认证系统
        lo_ident.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, Identify.class));
            }
        });

        LinearLayout lo_access = (LinearLayout) findViewById(R.id.lo_access);  // 门禁打卡系统
        lo_access.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, Access.class));
            }
        });

        LinearLayout lo_unlock = (LinearLayout) findViewById(R.id.lo_unlock); // 应用设备解锁
        lo_unlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                startActivity(new Intent(MainActivity.this, Unlock.class));
                startActivityForResult(new Intent(MainActivity.this, Unlock.class), 1);
            }
        });

        LinearLayout lo_towel = (LinearLayout) findViewById(R.id.lo_towel); // 微信刷脸取纸
        lo_towel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, Towel.class));
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {  // 设备解锁登录成功回调
            boolean isUnlocked = data.getBooleanExtra("isUnlocked", false);
            if (isUnlocked) {
//                Toast.makeText(MainActivity.this, "设备已解锁！", Toast.LENGTH_LONG);
                finish();  // App自行关闭，返回手机系统内部
            }
        }
    }
}