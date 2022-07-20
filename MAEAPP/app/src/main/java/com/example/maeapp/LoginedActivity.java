package com.example.maeapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoginedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        int type = intent.getIntExtra("type", 1);
        if (type == 1) {  // 双重认证
            setContentView(R.layout.activity_logined);
            String oriImagePath = intent.getStringExtra("ori_image_path");
            Bitmap oriBitmap = BitmapFactory.decodeFile(oriImagePath);
            ImageView iv_ori_image = findViewById(R.id.iv_ori_image);
            iv_ori_image.setImageBitmap(oriBitmap);  // 登录时上传的图像

            String recImagePath = intent.getStringExtra("rec_image_path");
            Bitmap recBitmap = BitmapFactory.decodeFile(recImagePath);
            ImageView iv_rec_image = findViewById(R.id.iv_rec_image);
            iv_rec_image.setImageBitmap(recBitmap);  // 恢复后的图像

            float matchScore = intent.getFloatExtra("matchScore", 0.0f);
            DecimalFormat decimalFormat = new DecimalFormat( ".00" );
            String score = decimalFormat.format(matchScore);
            TextView tv_similarity = findViewById(R.id.tv_similarity);
            tv_similarity.setText("相似度得分为" + score);

            Button bt_confirm = findViewById(R.id.bt_confirm);
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

        } else if (type == 2) {  // 门禁打卡
            setContentView(R.layout.activity_logined2);

            TextView textView = (TextView) findViewById(R.id.tv_time);
            Date date = new Date();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
            textView.setText(simpleDateFormat.format(date));  // 显示打卡时间

            Button button = findViewById(R.id.bt_confirm);
            button.setOnClickListener(new View.OnClickListener() {
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

        } else if (type == 3) {  // 设备解锁
            setContentView(R.layout.activity_logined3);
            Button button = findViewById(R.id.bt_confirm);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent();
                    intent.putExtra("isUnlocked", true);
                    setResult(RESULT_OK, intent);
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

        } else if (type == 4) {  // 刷脸取纸
            setContentView(R.layout.activity_logined4);
            Button bt_confirm = (Button) findViewById(R.id.bt_confirm);
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
}