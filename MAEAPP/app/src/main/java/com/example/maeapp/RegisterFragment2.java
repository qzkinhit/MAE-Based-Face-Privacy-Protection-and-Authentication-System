package com.example.maeapp;

import static android.app.Activity.RESULT_OK;
import static com.example.maeapp.TakePhoto.base64ToBitmap;
import static com.example.maeapp.TakePhoto.bitmapToBase64;
import static com.example.maeapp.NFCBase.isNFCAvailable;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NfcAdapter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
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


public class RegisterFragment2 extends Fragment implements ToFragmentListener {

    private static final String TAG = "wyt";
    private final static String URL = "http://101.43.135.58:12340/";
//    private final static String URL = "http://192.168.0.100:12340/";

    private boolean isNFCScanned;
    View rootView;
    private Bitmap imageBitmap;
    private String myUsername;
    TextView tv_username;
    private ProgressDialog progressDialog;
    private String bioPassword;
    private int appendant;
    private Retrofit retrofit;
    private MAEService maeService;
    private String myPassword;
    private Bitmap maskedBitmap;
    private Bitmap dbBitmap;

    protected NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private int NFCState = 0;

    public RegisterFragment2() {
        // Required empty public constructor
    }

    public static RegisterFragment2 newInstance() {
        RegisterFragment2 fragment = new RegisterFragment2();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (rootView == null)
            rootView = inflater.inflate(R.layout.fragment_register2, container, false);
        initView();
        return rootView;
    }

    private void initView() {

        Button button = rootView.findViewById((R.id.bt));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TextView editText = (TextView) rootView.findViewById(R.id.tv_username);  // 显示NFC卡ID
                myUsername = editText.getText().toString();
                if (!isNFCScanned) {
                    Toast.makeText(getActivity(), "请刷NFC卡", Toast.LENGTH_SHORT).show();
                    return;
                }

                CheckBox cb_nopwd = rootView.findViewById(R.id.cb_nopwd);  // 不设密码
                EditText et_pwd = rootView.findViewById(R.id.et_pwd);  // 密码输入框
                myPassword = et_pwd.getText().toString();
                if (!cb_nopwd.isChecked() && myPassword.equals("")) {
                    Toast.makeText(getActivity(), "请输入密码", Toast.LENGTH_SHORT).show();
                    return;
                }

                checkUserame();
            }
        });

        CheckBox cb_nopwd = rootView.findViewById(R.id.cb_nopwd);
        cb_nopwd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                EditText et_pwd = (EditText) rootView.findViewById(R.id.et_pwd);
                if (cb_nopwd.isChecked()) {
                    et_pwd.setText("");
                    et_pwd.setEnabled(false);
                }
                else
                    et_pwd.setEnabled(true);
            }
        });

        tv_username = rootView.findViewById(R.id.tv_username);

        NFCState = isNFCAvailable();
        if (NFCState == 1) {  // 设备无NFC功能
            tv_username.setText("该设备不支持NFC");
            FrameLayout frameLayout = rootView.findViewById(R.id.fl_noNFC);
            frameLayout.setVisibility(View.VISIBLE);

            EditText et_pwd = (EditText) rootView.findViewById(R.id.et_pwd);
            et_pwd.setEnabled(false);
            Button bt = (Button) rootView.findViewById(R.id.bt);
            bt.setEnabled(false);
            CheckBox cb = (CheckBox) rootView.findViewById(R.id.cb_nopwd);
            cb.setEnabled(false);
        } else if (NFCState == 2) {  // 设备未开启NFC功能
            tv_username.setText("请先启用NFC功能");
            FrameLayout frameLayout = rootView.findViewById(R.id.fl_NFCoff);
            frameLayout.setVisibility(View.VISIBLE);

            EditText et_pwd = (EditText) rootView.findViewById(R.id.et_pwd);
            et_pwd.setEnabled(false);
            Button bt = (Button) rootView.findViewById(R.id.bt);
            bt.setEnabled(false);
            CheckBox cb = (CheckBox) rootView.findViewById(R.id.cb_nopwd);
            cb.setEnabled(false);
        }
    }

    // 从Activity中获取NFC数据
    @Override
    public void getMsgFromActivity(String msg) {

        if (rootView != null && tv_username != null) {
            tv_username.setText(msg);
            myUsername = msg;
            isNFCScanned = true;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {  // 拍照页面回调
            boolean isSuccess = data.getBooleanExtra("isSuccess", false);
            if (isSuccess) {
                String imagePath = data.getStringExtra("path");
                bioPassword = data.getStringExtra("bioPassword");
                imageBitmap = BitmapFactory.decodeFile(imagePath);

                testImage();
            } else {
                Toast.makeText(getActivity(), "摄像头权限被禁止", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 2) {  // 注册成功页面回调

            TextView tv_username = (TextView) rootView.findViewById(R.id.tv_username);
            tv_username.setText("请刷NFC卡");
            EditText et_pwd = (EditText) rootView.findViewById(R.id.et_pwd);
            et_pwd.setText("");
        }
    }

    private void checkUserame() {  // 检查用户名是否已被使用

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
                                Log.e(TAG, "用户名重复！");

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
                        progressDialog = new ProgressDialog(getActivity());
                        progressDialog.setTitle("正在检测卡号是否可用");
                        progressDialog.setCanceledOnTouchOutside(false);
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

                            Intent intetnt = new Intent(getActivity(), TakePhoto.class);
                            intetnt.putExtra("type", 2);
                            startActivityForResult(intetnt, 1);
                        }
                        else {
                            Toast.makeText(getActivity(), "该卡已经注册过", Toast.LENGTH_SHORT).show();
                            resetFragment();
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

    private void testImage() {  // 测试照片质量
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

                        CheckBox cb_nopwd = (CheckBox) rootView.findViewById(R.id.cb_nopwd);
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
                        progressDialog = new ProgressDialog(getActivity());
                        progressDialog.setTitle("正在检测照片是否可用");
                        progressDialog.setCanceledOnTouchOutside(false);
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
                        else {
                            Toast.makeText(getActivity(), "照片不可用", Toast.LENGTH_SHORT).show();
                            resetFragment();
                        }
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
                        CheckBox cb_nopwd = rootView.findViewById(R.id.cb_nopwd);
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
                        progressDialog = new ProgressDialog(getActivity());
                        progressDialog.setTitle("正在注册");
                        progressDialog.setCanceledOnTouchOutside(false);
                        progressDialog.show();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        Log.i(TAG, "signup onNext: " + aBoolean);

                        // 销毁加载图标
                        if (progressDialog != null)
                            progressDialog.dismiss();

                        if (aBoolean) {
                            Toast.makeText(getActivity(), "注册成功", Toast.LENGTH_SHORT).show();

                            String maskedImagePath = saveImage(maskedBitmap, "masked_image.jpg");
                            String dbImagePath = saveImage(dbBitmap, "db_image.jpg");

                            Intent intent = new Intent(getActivity(), RegisteredActivity.class);
                            intent.putExtra("type", 2);
                            intent.putExtra("masked_image_path", maskedImagePath);
                            intent.putExtra("db_image_path", dbImagePath);
                            resetFragment();
                            startActivityForResult(intent, 2);
                        }
                        else {
                            Toast.makeText(getActivity(), "注册失败", Toast.LENGTH_SHORT).show();
                            resetFragment();
                        }
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
        File outputImage = new File(getActivity().getExternalCacheDir(), childPath);
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


    private void resetFragment() {
        tv_username.setText("请刷NFC卡");
        isNFCScanned = false;
    }
}