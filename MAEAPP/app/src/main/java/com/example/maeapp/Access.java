package com.example.maeapp;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class Access extends NFCBase implements View.OnClickListener {

    private static final String TAG = "wyt";
    ViewPager2 viewPager;
    private LinearLayout llRegister, llLogin, llCurrent;
    private ToFragmentListener mToRegisterListener;
    private ToFragmentListener mToLoginListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access);
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
        RegisterFragment2 registerFragment = RegisterFragment2.newInstance();
        LoginFragment2 loginFragment = LoginFragment2.newInstance();
        // RegisterFragment2中已经实现ToFragmentListener接口
        mToRegisterListener = registerFragment;
        mToLoginListener = loginFragment;
        fragments.add(registerFragment);
        fragments.add(loginFragment);
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

    private void changTab(int position) {  //
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

    @SuppressLint("MissingSuperCall")
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        readNFCTag(detectedTag);
    }

    private void readNFCTag(Tag tag) {  // 读取NFC卡ID
        if (tag == null)
            Log.e(TAG, "readNFCTag: tag is null");
        byte[] tagbytes = tag.getId();
        String str = bytesToHex(tagbytes);
        Log.i(TAG, "readNFCTag: " + str);

        if (llRegister.isSelected()) {
            if (mToRegisterListener != null)
                mToRegisterListener.getMsgFromActivity(str);
        } else {
            if (mToLoginListener != null)
                mToLoginListener.getMsgFromActivity(str);
        }
    }

    public static String bytesToHex(byte[] bytes) {  // 将读入字节转换成字符串
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if(hex.length() < 2){
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}