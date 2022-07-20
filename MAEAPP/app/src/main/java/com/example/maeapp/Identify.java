package com.example.maeapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class Identify extends AppCompatActivity  implements View.OnClickListener {

    private static final String TAG = "wyt";
    ViewPager2 viewPager;
    private LinearLayout llRegister, llLogin, llCurrent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify);
        initPager();
        initTabView();
    }

    private void initTabView() {  // 初始化导航页
        llRegister = findViewById(R.id.id_tab_register);
        llRegister.setOnClickListener(this);
        llLogin = findViewById(R.id.id_tab_login);
        llLogin.setOnClickListener(this);

        llRegister.setSelected(true);
        llCurrent = llRegister;
    }

    private void initPager() {
        viewPager = findViewById(R.id.id_viewpager);
        ArrayList<Fragment> fragments = new ArrayList<>();
//        fragments.add(RegisterFragment.newInstance("reg1"));
//        fragments.add(LoginFragment.newInstance("log1"));
        fragments.add(RegisterFragment.newInstance());
        fragments.add(LoginFragment.newInstance());
        MyFragmentPagerAdapter pagerAdapter = new MyFragmentPagerAdapter(getSupportFragmentManager(), getLifecycle(), fragments);
        viewPager.setAdapter(pagerAdapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            // 滚动动画
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            // 选择界面
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                changTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
    }

    private void changTab(int position) {
        llCurrent.setSelected(false);
        switch (position) {
            case R.id.id_tab_register:
                viewPager.setCurrentItem(0);
            case 0:
                llRegister.setSelected(true);
                llCurrent = llRegister;
                break;
            case R.id.id_tab_login:
                viewPager.setCurrentItem(1);
            case 1:
                llLogin.setSelected(true);
                llCurrent = llLogin;
                break;
        }
    }

    @Override
    public void onClick(View view) {
        changTab(view.getId());  // 切换导航页
    }
}