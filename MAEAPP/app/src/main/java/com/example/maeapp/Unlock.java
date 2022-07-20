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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
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

public class Unlock extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);

        myUsername = "";
//        getPhoneNum();
        myUsername = getUUID();  // 使用自定义全局ID作为用户名
//        Log.i(TAG, "onCreate: " + myUsername);

        Button bt_register = (Button) findViewById(R.id.bt_register);  // 注册按钮
        bt_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (myUsername.equals("")) {
//                    Toast.makeText(Unlock.this, "请启用获取手机号的权限！", Toast.LENGTH_LONG).show();
                    Toast.makeText(Unlock.this, "生成UUID失败！", Toast.LENGTH_LONG).show();
                    return;
                }

                CheckBox cb_nopwd = findViewById(R.id.cb_nopwd);
                EditText et_pwd = findViewById(R.id.et_pwd);
                myPassword = et_pwd.getText().toString();
                if (!cb_nopwd.isChecked() && myPassword.equals("")) {
                    Toast.makeText(Unlock.this, "请输入密码", Toast.LENGTH_SHORT).show();
                    return;
                }

                checkUserame();
            }
        });

        Button bt_unlock = (Button) findViewById(R.id.bt_unlock);  // 解锁按钮
        bt_unlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (myUsername.equals("")) {
                    Toast.makeText(Unlock.this, "请启用获取手机号的权限！", Toast.LENGTH_LONG).show();
                    return;
                }

                CheckBox cb_nopwd = findViewById(R.id.cb_nopwd);
                EditText et_pwd = findViewById(R.id.et_pwd);
                myPassword = et_pwd.getText().toString();
                if (!cb_nopwd.isChecked() && myPassword.equals("")) {
                    Toast.makeText(Unlock.this, "请输入密码", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(Unlock.this, TakePhoto.class);
                intent.putExtra("type", 1);
                startActivityForResult(intent, 2);
            }
        });

        CheckBox cb_nopwd = findViewById(R.id.cb_nopwd);  // 不设密码
        cb_nopwd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                EditText et_pwd = (EditText) findViewById(R.id.et_pwd);
                if (cb_nopwd.isChecked()) {
                    et_pwd.setText("");
                    et_pwd.setEnabled(false);
                }
                else
                    et_pwd.setEnabled(true);
            }
        });
    }

    public String getPhoneNum() {

        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNum = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.READ_PHONE_STATE}, 1);
        } else {
            phoneNum = tm.getLine1Number();  // 手机号码
            Log.i(TAG, "getPhoneNum: " + phoneNum);
        }

        myUsername = phoneNum;

        return phoneNum;
    }

    public static String genUUID() {  // 生成UUID
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public String getUUID() {  // 获取UUID

        File cacheFile = getExternalCacheDir();
//        Log.i(TAG, "getUUID: " + cacheFile.getAbsolutePath());
        if (!cacheFile.exists()) {
            cacheFile.mkdir();
            Log.i(TAG, "getUUID: mkdir");
        }

        File outputFile = new File(getExternalCacheDir(), "UUID.txt");
        if (outputFile.exists()) {  // 若文件存在，证明之前已经生成过，读取即可

            BufferedReader reader = null;
            StringBuilder content = null;
            try {
                FileReader fr = new FileReader(outputFile);
                content= new StringBuilder();
                reader = new BufferedReader(fr);
                String line;
                while ((line = reader.readLine()) != null)
                    content.append(line);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (reader!=null){
                    try {
                        reader.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
            Log.i(TAG, "current UUID: " + content.toString());
            return content.toString();
        }
        else {  // 若不存在，新建文件并存入生成的UUID
            String uuid = genUUID();
            try {
                Log.i(TAG, "save UUID: " + uuid);
                outputFile.createNewFile();
                FileWriter fw = new FileWriter(outputFile);
                fw.write(uuid);
                fw.flush();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return uuid;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case 1:
//                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] ==PackageManager.PERMISSION_GRANTED) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getPhoneNum();
                } else {
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
                Toast.makeText(Unlock.this, "摄像头权限被禁止", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 2 && resultCode == RESULT_OK) {  // 登录拍照页面回调
            boolean isSuccess = data.getBooleanExtra("isSuccess", false);
            if (isSuccess) {
                oriImagePath = data.getStringExtra("path");
                bioPassword = data.getStringExtra("bioPassword");
                imageBitmap = BitmapFactory.decodeFile(oriImagePath);

                recPhoto();
            } else {
                Toast.makeText(Unlock.this, "摄像头权限被禁止", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 3 && resultCode == RESULT_OK) {  // 登录成功页面回调
            boolean isUnlocked = data.getBooleanExtra("isUnlocked", false);
            if (isUnlocked) {
                Log.i(TAG, "Unlocked!");
                Intent intent = new Intent();
                intent.putExtra("isUnlocked", true);
                setResult(RESULT_OK, intent);
                Unlock.this.finish();
            }
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
                        progressDialog = new ProgressDialog(Unlock.this);
                        progressDialog.setTitle("正在检测用户名是否可用");
                        progressDialog.show();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        Log.i(TAG, "onNext: " + Thread.currentThread());

                        // 销毁加载图标
                        if (progressDialog != null)
                            progressDialog.dismiss();

                        if (aBoolean) {
//                            Toast.makeText(getActivity(), "该用户名可用", Toast.LENGTH_SHORT).show();

                            Intent intetnt = new Intent(Unlock.this, TakePhoto.class);
                            intetnt.putExtra("type", 1);
                            startActivityForResult(intetnt, 1);
                        }
                        else
                            Toast.makeText(Unlock.this, "该设备已经注册过", Toast.LENGTH_SHORT).show();
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

                        CheckBox cb_nopwd = (CheckBox) findViewById(R.id.cb_nopwd);
                        if (cb_nopwd.isChecked()) {  // 使用生物密钥
                            requestBody = RequestBody.create(MediaType.parse("application/json"),
                                    "{" +
                                            "\"img_base64\":\"" + photoBase64 + "\"," +
                                            "\"checkpwd\":false," +
                                            "\"pwd\":\"" + bioPassword +  "\"," +
                                            "\"username\":\"" + myUsername +  "\"" +
                                            "}");
                        } else {  // 使用输入的密钥
                            requestBody = RequestBody.create(MediaType.parse("application/json"),
                                    "{" +
                                            "\"img_base64\":\"" + photoBase64 + "\"," +
                                            "\"checkpwd\":true," +
                                            "\"pwd\":\"" + myPassword +  "\"," +
                                            "\"username\":\"" + myUsername +  "\"" +
                                            "}");
                        }
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
                        progressDialog = new ProgressDialog(Unlock.this);
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
                            Toast.makeText(Unlock.this, "照片不可用", Toast.LENGTH_SHORT).show();
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
                        CheckBox cb_nopwd = findViewById(R.id.cb_nopwd);
                        if (cb_nopwd.isChecked()) {  // 使用生物密钥
                            requestBody = RequestBody.create(MediaType.parse("application/json"),
                                    "{" +
                                            "\"username\":\"" + myUsername + "\"," +
                                            "\"img_base64\":\"" + photoBase64 + "\"," +
                                            "\"pwd\":\"" + bioPassword + "\"," +
                                            "\"appendant\":" + appendant + "," +
                                            "\"checkpwd\":false" +
                                            "}");
                        } else {  // 使用用户输入的密钥
                            requestBody = RequestBody.create(MediaType.parse("application/json"),
                                    "{" +
                                            "\"username\":\"" + myUsername + "\"," +
                                            "\"img_base64\":\"" + photoBase64 + "\"," +
                                            "\"pwd\":\"" + myPassword + "\"," +
                                            "\"appendant\":" + appendant + "," +
                                            "\"checkpwd\":true" +
                                            "}");

                        }
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
                        progressDialog = new ProgressDialog(Unlock.this);
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
                            Toast.makeText(Unlock.this, "注册成功", Toast.LENGTH_SHORT).show();

                            String maskedImagePath = saveImage(maskedBitmap, "masked_image.jpg");
                            String dbImagePath = saveImage(dbBitmap, "db_image.jpg");

                            Intent intent = new Intent(Unlock.this, RegisteredActivity.class);
                            intent.putExtra("type", 3);
                            intent.putExtra("masked_image_path", maskedImagePath);
                            intent.putExtra("db_image_path", dbImagePath);
                            startActivityForResult(intent, 1);
                        }
                        else
                            Toast.makeText(Unlock.this, "注册失败", Toast.LENGTH_SHORT).show();
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
        File outputImage = new File(Unlock.this.getExternalCacheDir(), childPath);
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
                        CheckBox cb_nopwd = findViewById(R.id.cb_nopwd);
                        if (cb_nopwd.isChecked()) {  // 使用生物密钥
                            requestBody = RequestBody.create(MediaType.parse("application/json"),
                                    "{" +
                                            "\"username\":\"" + myUsername + "\"," +
                                            "\"checkpwd\":false," +
                                            "\"pwd\":\"" + bioPassword +  "\"" +
                                            "}");
                        } else {  // 使用输入的密钥
                            requestBody = RequestBody.create(MediaType.parse("application/json"),
                                    "{" +
                                            "\"username\":\"" + myUsername + "\"," +
                                            "\"checkpwd\":true," +
                                            "\"pwd\":\"" + myPassword +  "\"" +
                                            "}");
                        }

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
                        progressDialog = new ProgressDialog(Unlock.this);
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
                            Toast.makeText(Unlock.this, "获取恢复图像失败", Toast.LENGTH_SHORT).show();
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
                        progressDialog = new ProgressDialog(Unlock.this);
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

                                Toast.makeText(Unlock.this, "登录成功", Toast.LENGTH_SHORT).show();

                                Bitmap recImageBitmap = base64ToBitmap(recBase64);
                                String recImagePath = saveImage(recImageBitmap, "recovered_image.jpg");

                                Intent intent = new Intent(Unlock.this, LoginedActivity.class);
                                intent.putExtra("type", 3);
                                intent.putExtra("ori_image_path", oriImagePath);
                                intent.putExtra("rec_image_path", recImagePath);
                                intent.putExtra("matchScore", matchScore);
                                startActivityForResult(intent, 3);
                            }
                            else
                                Toast.makeText(Unlock.this, "登录失败", Toast.LENGTH_SHORT).show();
                        }
                        else
                            Toast.makeText(Unlock.this, "比对时出现错误", Toast.LENGTH_SHORT).show();
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
