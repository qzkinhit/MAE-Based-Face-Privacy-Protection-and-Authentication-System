package com.example.maeapp;

import static com.example.maeapp.TakePhoto.base64ToBitmap;
import static com.example.maeapp.TakePhoto.bitmapToBase64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

public class Towel extends AppCompatActivity {


    private static final String TAG = "wyt";
    private final static String URL = "http://101.43.135.58:12340/";
    //    private final static String URL = "http://192.168.0.100:12340/";

    private String bioPassword;
    private Bitmap imageBitmap;
    private String myUsername;
    private String myPassword;
    private Retrofit retrofit;
    private MAEService maeService;
    private ProgressDialog progressDialog;
    private String recBase64;
    private float matchScore;
    private String oriImagePath;

    private int appendant;
    private Bitmap maskedBitmap;
    private Bitmap dbBitmap;

    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_towel);

        phoneNumber = "";
        getPhoneNum();

        TextView tv_other_phone = (TextView) findViewById(R.id.tv_other_phone);  // 使用其他手机号
        tv_other_phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText et = (EditText) findViewById(R.id.et_phone_number);  // 手机号输入框
                et.setText("");
                et.setEnabled(true);
                et.requestFocus();
                InputMethodManager imm = (InputMethodManager)Towel.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(et, 0);
            }
        });

        Button bt_cancel = (Button) findViewById(R.id.bt_cancel);  // 返回按钮
        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Button bt_confirm = (Button) findViewById(R.id.bt_confirm);  // 确认按钮
        bt_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditText et_phone_number = (EditText) findViewById(R.id.et_phone_number);
                myUsername = et_phone_number.getText().toString();
                if (myUsername.equals("")) {
//                    Toast.makeText(Towel.this, "请启用获取手机号的权限或输入手机号！", Toast.LENGTH_LONG).show();
                    Toast.makeText(Towel.this, "获取手机号失败！请手动输入手机号", Toast.LENGTH_LONG).show();
                    return;
                }
                myUsername = "wx_" + myUsername;

                checkUserame();
            }
        });
    }

    public String getPhoneNum() {  // 获取手机号

        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.READ_PHONE_STATE}, 1);
        } else {
            phoneNumber = tm.getLine1Number();  // 手机号码
//            Log.i(TAG, "getPhoneNum: " + phoneNum);
            if (phoneNumber.equals(""))  // 该SIM卡中没有写入手机号，无法获取
                Toast.makeText(Towel.this, "获取手机号失败！请手动输入手机号", Toast.LENGTH_LONG).show();
        }

        myUsername = phoneNumber;
        myUsername = myUsername.replaceAll("\\+86", "");
        EditText et_phone_number = (EditText) findViewById(R.id.et_phone_number);
        et_phone_number.setText(myUsername);
        return phoneNumber;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {  // 权限申请的回调函数

        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {  // 已授权
                    getPhoneNum();
                } else {  // 未授权
                    Toast.makeText(Towel.this, "获取手机号失败！请手动输入手机号", Toast.LENGTH_LONG).show();
                    Log.i(TAG, "permission denied");
                }
                break;
            default:
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {  // 注册拍照页面回调
            boolean isSuccess = data.getBooleanExtra("isSuccess", false);
            if (isSuccess) {
                String imagePath = data.getStringExtra("path");
                bioPassword = data.getStringExtra("bioPassword");
                imageBitmap = BitmapFactory.decodeFile(imagePath);

                testImage();
            } else {
                Toast.makeText(Towel.this, "摄像头权限被禁止", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 2 && resultCode == RESULT_OK) {  // 登录拍照页面回调
            boolean isSuccess = data.getBooleanExtra("isSuccess", false);
            if (isSuccess) {
                oriImagePath = data.getStringExtra("path");
                bioPassword = data.getStringExtra("bioPassword");
                imageBitmap = BitmapFactory.decodeFile(oriImagePath);

                recPhoto();
            } else {
                Toast.makeText(Towel.this, "摄像头权限被禁止", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            finish();
        }
    }

    private void checkUserame() {  // 检查该手机号是否已经注册

        Observable.just(URL)
                .map(new Function<String, Boolean>() {

                    @NonNull
                    @Override
                    public Boolean apply(@NonNull String s) throws Exception {

                        retrofit = new Retrofit.Builder().baseUrl(s).build();
                        maeService = retrofit.create(MAEService.class);
                        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),
                                "{\"username\":\"" + myUsername +  "\"}");
                        Call<ResponseBody> call = maeService.checkUsername(requestBody);
                        MyBean myBean;

                        try {
                            Response<ResponseBody> response = call.execute();
                            Gson gson = new Gson();
                            String json = response.body().string();
                            myBean = gson.fromJson(json, MyBean.class);
                            if (myBean.isSuccess())
                                Log.e(TAG, "用户名不重复");
                            else
                                Log.e(TAG, "用户名重复！" + myUsername);

                            return  myBean.isSuccess();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return false;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        progressDialog = new ProgressDialog(Towel.this);
                        progressDialog.setTitle("正在检测用户名是否可用");
                        progressDialog.show();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        Log.i(TAG, "onNext: " + Thread.currentThread());

                        // 销毁加载图标
                        if (progressDialog != null)
                            progressDialog.dismiss();

                        if (aBoolean) {  // 手机号未注册过，注册
//                            Toast.makeText(getActivity(), "该用户名可用", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(Towel.this, TakePhoto.class);  // 跳转至拍照页面
                            intent.putExtra("type", 1);
                            startActivityForResult(intent, 1);
                        }
                        else {  // 手机号已经注册过，登录
//                            Toast.makeText(Towel.this, "该用户名已经存在", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(Towel.this, TakePhoto.class);  // 跳转至拍照页面
                            intent.putExtra("type", 1);
                            startActivityForResult(intent, 2);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void testImage() {  // 检测图像质量
        Observable.just(URL)
                .map(new Function<String, Boolean>() {

                    @NonNull
                    @Override
                    public Boolean apply(@NonNull String s) throws Exception {

                        // 设置超时时间
                        OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
                        OkHttpClient client = httpBuilder.readTimeout(3, TimeUnit.MINUTES)
                                .connectTimeout(3, TimeUnit.MINUTES).writeTimeout(3, TimeUnit.MINUTES) //设置超时
                                .build();

                        retrofit = new Retrofit
                                .Builder()
                                .baseUrl(s)
                                .client(client)
                                .build();
                        maeService = retrofit.create(MAEService.class);

                        String photoBase64 = "data:image/jpg;base64," + bitmapToBase64(imageBitmap);

                        RequestBody requestBody;
                        Log.i(TAG, "apply: " + bioPassword);

//                        CheckBox cb_nopwd = (CheckBox) findViewById(R.id.cb_nopwd);
//                        if (cb_nopwd.isChecked()) {  // 使用生物密钥
                            requestBody = RequestBody.create(MediaType.parse("application/json"),
                                    "{" +
                                            "\"img_base64\":\"" + photoBase64 + "\"," +
                                            "\"checkpwd\":false," +
                                            "\"pwd\":\"" + bioPassword +  "\"," +
                                            "\"username\":\"" + myUsername +  "\"" +
                                            "}");
//                        } else {  // 使用输入的密钥
//                            requestBody = RequestBody.create(MediaType.parse("application/json"),
//                                    "{" +
//                                            "\"img_base64\":\"" + photoBase64 + "\"," +
//                                            "\"checkpwd\":true," +
//                                            "\"pwd\":\"" + myPassword +  "\"," +
//                                            "\"username\":\"" + myUsername +  "\"" +
//                                            "}");
//                        }
                        Call<ResponseBody> call = maeService.test(requestBody);
                        MyBean myBean;

                        try {
                            Response<ResponseBody> response = call.execute();
                            Gson gson = new Gson();
                            String json = response.body().string();

                            // TODO 此处和下面返回体为空的处理
                            myBean = gson.fromJson(json, MyBean.class);
                            if (myBean.isSuccess())
                                Log.e(TAG, "照片可用");
                            else
                                Log.e(TAG, "照片不可用");

                            appendant = myBean.getAppendant();

                            return  myBean.isSuccess();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return false;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        progressDialog = new ProgressDialog(Towel.this);
                        progressDialog.setTitle("正在检测照片是否可用");
                        progressDialog.show();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        Log.i(TAG, "testPhoto onNext: " + aBoolean);

                        // 销毁加载图标
                        if (progressDialog != null)
                            progressDialog.dismiss();

                        if (aBoolean) {
//                            Toast.makeText(MainActivity.this, "照片可用", Toast.LENGTH_SHORT).show();
                            signup();
                        }
                        else
                            Toast.makeText(Towel.this, "照片不可用", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        // TODO 各种超时（包括takePhoto）
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void signup() {  // 注册
        Observable.just(URL)
                .map(new Function<String, Boolean>() {

                    @NonNull
                    @Override
                    public Boolean apply(@NonNull String s) throws Exception {

                        // 设置超时时间
                        OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
                        OkHttpClient client = httpBuilder.readTimeout(3, TimeUnit.MINUTES)
                                .connectTimeout(3, TimeUnit.MINUTES).writeTimeout(3, TimeUnit.MINUTES) //设置超时
                                .build();

                        retrofit = new Retrofit
                                .Builder()
                                .baseUrl(s)
                                .client(client)
                                .build();
                        maeService = retrofit.create(MAEService.class);

                        String photoBase64 = "data:image/jpg;base64," + bitmapToBase64(imageBitmap); // TODO 此处和上面的图片的压缩

                        RequestBody requestBody;
//                        CheckBox cb_nopwd = findViewById(R.id.cb_nopwd);
//                        if (cb_nopwd.isChecked()) {  // 使用生物密钥
                            requestBody = RequestBody.create(MediaType.parse("application/json"),
                                    "{" +
                                            "\"username\":\"" + myUsername + "\"," +
                                            "\"img_base64\":\"" + photoBase64 + "\"," +
                                            "\"pwd\":\"" + bioPassword + "\"," +
                                            "\"appendant\":" + appendant + "," +
                                            "\"checkpwd\":false" +
                                            "}");
//                        } else {  // 使用用户输入的密钥
//                            requestBody = RequestBody.create(MediaType.parse("application/json"),
//                                    "{" +
//                                            "\"username\":\"" + myUsername + "\"," +
//                                            "\"img_base64\":\"" + photoBase64 + "\"," +
//                                            "\"pwd\":\"" + myPassword + "\"," +
//                                            "\"appendant\":" + appendant + "," +
//                                            "\"checkpwd\":true" +
//                                            "}");
//
//                        }
                        Call<ResponseBody> call = maeService.signup(requestBody);
                        MyBean myBean;

                        try {
                            Response<ResponseBody> response = call.execute();
                            Gson gson = new Gson();
                            String json = response.body().string();

                            myBean = gson.fromJson(json, MyBean.class);
                            if (myBean.isSuccess())
                                Log.e(TAG, "注册成功");
                            else
                                Log.e(TAG, "注册失败");

                            maskedBitmap = base64ToBitmap(myBean.getMask_img().replaceAll("data:image/jpg;base64,", ""));
                            dbBitmap = base64ToBitmap(myBean.getPatch_img().replaceAll("data:image/jpg;base64,", ""));

                            return  myBean.isSuccess();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return false;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        progressDialog = new ProgressDialog(Towel.this);
                        progressDialog.setTitle("正在注册");
                        progressDialog.show();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        Log.i(TAG, "signup onNext: " + aBoolean);

                        // 销毁加载图标
                        if (progressDialog != null)
                            progressDialog.dismiss();

                        if (aBoolean) {
                            Toast.makeText(Towel.this, "注册成功", Toast.LENGTH_SHORT).show();

                            String maskedImagePath = saveImage(maskedBitmap, "masked_image.jpg");
                            String dbImagePath = saveImage(dbBitmap, "db_image.jpg");

                            Intent intent = new Intent(Towel.this, RegisteredActivity.class);
                            intent.putExtra("type", 4);
                            intent.putExtra("masked_image_path", maskedImagePath);
                            intent.putExtra("db_image_path", dbImagePath);
                            startActivityForResult(intent, 3);
                        }
                        else
                            Toast.makeText(Towel.this, "注册失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        // TODO 各种超时（包括takePhoto）
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }



    public String saveImage(Bitmap bitmap, String childPath) {  // 保存图像

//        File outputImage = new File(getActivity().getExternalCacheDir(), "output_image.jpg");
        File outputImage = new File(Towel.this.getExternalCacheDir(), childPath);
        try {
            if (outputImage.exists())
                outputImage.delete();

            outputImage.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            FileOutputStream fos = new FileOutputStream(outputImage);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outputImage.getPath();
    }

    private void recPhoto() {  // 恢复残缺图像

        Observable.just(URL)
                .map(new Function<String, Boolean>() {

                    @NonNull
                    @Override
                    public Boolean apply(@NonNull String s) throws Exception {

                        retrofit = new Retrofit.Builder().baseUrl(s).build();
                        maeService = retrofit.create(MAEService.class);

                        RequestBody requestBody;
//                        CheckBox cb_nopwd = findViewById(R.id.cb_nopwd);
//                        if (cb_nopwd.isChecked()) {  // 使用生物密钥
                            requestBody = RequestBody.create(MediaType.parse("application/json"),
                                    "{" +
                                            "\"username\":\"" + myUsername + "\"," +
                                            "\"checkpwd\":false," +
                                            "\"pwd\":\"" + bioPassword +  "\"" +
                                            "}");
//                        } else {  // 使用输入的密钥
//                            requestBody = RequestBody.create(MediaType.parse("application/json"),
//                                    "{" +
//                                            "\"username\":\"" + myUsername + "\"," +
//                                            "\"checkpwd\":true," +
//                                            "\"pwd\":\"" + myPassword +  "\"" +
//                                            "}");
//                        }

                        Call<ResponseBody> call = maeService.rec(requestBody);
                        MyBean myBean;

                        try {
                            Response<ResponseBody> response = call.execute();
                            Gson gson = new Gson();
                            String json = response.body().string();
                            myBean = gson.fromJson(json, MyBean.class);
                            if (myBean.isSuccess())
                                Log.e(TAG, "获取到恢复的图像");
                            else
                                Log.e(TAG, "获取恢复图像失败");

                            recBase64 = myBean.getRec_img().replaceAll("data:image/jpg;base64,", "");

                            return  myBean.isSuccess();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return false;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        progressDialog = new ProgressDialog(Towel.this);
                        progressDialog.setTitle("正在获取残缺图像");
                        progressDialog.show();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {

                        // 销毁加载图标
                        if (progressDialog != null)
                            progressDialog.dismiss();

                        if (aBoolean) {
//                            Toast.makeText(MainActivity.this, "获取恢复图像成功", Toast.LENGTH_SHORT).show();
//                            recBitmap = base64ToBitmap(recBase64);
//                            iv_photo.setImageBitmap(recBitmap);
                            matchPhoto();
                        }
                        else
                            Toast.makeText(Towel.this, "获取恢复图像失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void matchPhoto() {  // 人脸匹配

        Observable.just(URL)
                .map(new Function<String, Boolean>() {

                    @NonNull
                    @Override
                    public Boolean apply(@NonNull String s) throws Exception {

                        // 设置超时时间
                        OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
                        OkHttpClient client = httpBuilder.readTimeout(3, TimeUnit.MINUTES)
                                .connectTimeout(3, TimeUnit.MINUTES).writeTimeout(3, TimeUnit.MINUTES) //设置超时
                                .build();

                        retrofit = new Retrofit
                                .Builder()
                                .baseUrl(s)
                                .client(client)
                                .build();
                        maeService = retrofit.create(MAEService.class);

                        String photoBase64 = "data:image/jpg;base64," + bitmapToBase64(imageBitmap);
                        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),
                                "{" +
                                        "\"img1_base64\":\"" + photoBase64 + "\"," +
                                        "\"img2_base64\":\"" + "data:image/jpg;base64," + recBase64 + "\"" +
                                        "}");
                        Call<ResponseBody> call = maeService.match(requestBody);
                        MyBean myBean;

                        try {
                            Response<ResponseBody> response = call.execute();
                            Gson gson = new Gson();
                            String json = response.body().string();

                            myBean = gson.fromJson(json, MyBean.class);
                            if (myBean.isSuccess())
                                Log.e(TAG, "比对过程成功");
                            else
                                Log.e(TAG, "比对时出现错误");

                            matchScore = myBean.getScore();
                            Log.i(TAG, "apply: " + matchScore);

                            return  myBean.isSuccess();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return false;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        progressDialog = new ProgressDialog(Towel.this);
                        progressDialog.setTitle("正在比对人脸");
                        progressDialog.show();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        Log.i(TAG, "signup onNext: " + aBoolean);

                        // 销毁加载图标
                        if (progressDialog != null)
                            progressDialog.dismiss();

                        if (aBoolean) {
                            if (matchScore > 80.0) {  // 得分在80分以上时，错误率小于万分之一

                                Toast.makeText(Towel.this, "登录成功", Toast.LENGTH_SHORT).show();

                                Bitmap recImageBitmap = base64ToBitmap(recBase64);
                                String recImagePath = saveImage(recImageBitmap, "recovered_image.jpg");

                                Intent intent = new Intent(Towel.this, LoginedActivity.class);
                                intent.putExtra("type", 4);
                                intent.putExtra("ori_image_path", oriImagePath);
                                intent.putExtra("rec_image_path", recImagePath);
                                intent.putExtra("matchScore", matchScore);
                                startActivityForResult(intent, 3);
                            }
                            else
                                Toast.makeText(Towel.this, "登录失败", Toast.LENGTH_SHORT).show();
                        }
                        else
                            Toast.makeText(Towel.this, "比对时出现错误", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        // TODO 各种超时（包括takePhoto）
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

}