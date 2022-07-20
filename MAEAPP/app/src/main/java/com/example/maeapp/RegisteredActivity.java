package com.example.maeapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class RegisteredActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        int type = intent.getIntExtra("type", 1);
        if (type == 1)  // 双重认证
            setContentView(R.layout.activity_registered);
        else if (type == 2)  // 门禁打卡
            setContentView(R.layout.activity_registered2);
        else if (type == 3)  // 设备解锁
            setContentView(R.layout.activity_registered3);
        else if (type == 4)  // 刷脸取纸
            setContentView(R.layout.activity_registered4);

        String maskedImagePath = intent.getStringExtra("masked_image_path");
        Bitmap maskedBitmap = BitmapFactory.decodeFile(maskedImagePath);
        ImageView iv_masked_image = findViewById(R.id.iv_masked_image);
        iv_masked_image.setImageBitmap(maskedBitmap);  // 被遮罩的图像

        String dbImagePath = intent.getStringExtra("db_image_path");
        Bitmap dbBitmap = BitmapFactory.decodeFile(dbImagePath);
        ImageView iv_db_image = findViewById(R.id.iv_db_image);
        iv_db_image.setImageBitmap(dbBitmap);  // 存于数据库中的图像

        Button bt_confirm = findViewById(R.id.bt_confirm);
        if (type == 4) {  // 刷脸取纸，点击确定跳转至出纸页面
            bt_confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(RegisteredActivity.this, LoginedActivity.class);
                    intent.putExtra("type", 4);
                    startActivityForResult(intent, 1);
                }
            });

            Toolbar toolbar = findViewById(R.id.tb);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(RegisteredActivity.this, LoginedActivity.class);
                    intent.putExtra("type", 4);
                    startActivityForResult(intent, 1);
                }
            });
        } else {  // 其余的直接返回
            bt_confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });

            Toolbar toolbar = findViewById(R.id.tb);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {  // 取纸页面回调
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
}