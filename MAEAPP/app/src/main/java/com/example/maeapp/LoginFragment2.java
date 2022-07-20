package com.example.maeapp;

import static android.app.Activity.RESULT_OK;
import static com.example.maeapp.TakePhoto.base64ToBitmap;
import static com.example.maeapp.TakePhoto.bitmapToBase64;
import static com.example.maeapp.NFCBase.isNFCAvailable;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
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


public class LoginFragment2 extends Fragment implements ToFragmentListener {

    private static final String TAG = "wyt";
    private final static String URL = "http://101.43.135.58:12340/";
//    private final static String URL = "http://192.168.0.100:12340/";

    View rootView;
    private Bitmap imageBitmap;
    private String myUsername;
    TextView tv_username;
    private ProgressDialog progressDialog;
    private String bioPassword;
    private Retrofit retrofit;
    private MAEService maeService;
    private String myPassword;

    private boolean isNFCScanned;
    private String oriImagePath;
    private String recBase64;
    private float matchScore;
    private int NFCState = 0;

    public LoginFragment2() {
        // Required empty public constructor
    }

    public static LoginFragment2 newInstance() {
        LoginFragment2 fragment = new LoginFragment2();
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
            rootView = inflater.inflate(R.layout.fragment_login2, container, false);
        initView();
        return rootView;
    }

    private void initView() {

        Button button = rootView.findViewById((R.id.bt));  // 下一步按钮
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TextView editText = (TextView) rootView.findViewById(R.id.tv_username);
                myUsername = editText.getText().toString();
                if (!isNFCScanned) {
                    Toast.makeText(getActivity(), "请刷NFC卡", Toast.LENGTH_SHORT).show();
                    return;
                }

                CheckBox cb_nopwd = rootView.findViewById(R.id.cb_nopwd);
                EditText et_pwd = rootView.findViewById(R.id.et_pwd);
                myPassword = et_pwd.getText().toString();
                if (!cb_nopwd.isChecked() && myPassword.equals("")) {
                    Toast.makeText(getActivity(), "请输入密码", Toast.LENGTH_SHORT).show();
                    return;
                }

                checkUserame();

            }
        });

        CheckBox cb_nopwd = rootView.findViewById(R.id.cb_nopwd);  // 不设密码
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

        // 检查NFC状态是否可用
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
        } else if (NFCState == 2) {  // 设备NFC功能未开启
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {  // 拍照页面回调
            boolean isSuccess = data.getBooleanExtra("isSuccess", false);
            if (isSuccess) {
                oriImagePath = data.getStringExtra("path");
                bioPassword = data.getStringExtra("bioPassword");
                imageBitmap = BitmapFactory.decodeFile(oriImagePath);

                recPhoto();
            } else {
                Toast.makeText(getActivity(), "摄像头权限被禁止", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 2) {  // 登录成功页面回调

            TextView tv_username = (TextView) rootView.findViewById(R.id.tv_username);
            tv_username.setText("请刷NFC卡");
            EditText et_pwd = (EditText) rootView.findViewById(R.id.et_pwd);
            et_pwd.setText("");
        }
    }


    private void checkUserame() {  // 检查用户名是否存在

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
                        progressDialog.setTitle("正在检测用户名是否可用");
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
                            Toast.makeText(getActivity(), "该用户不存在", Toast.LENGTH_SHORT).show();
                        }
                        else {
//                            Toast.makeText(getActivity(), "该用户名已经存在", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(getActivity(), TakePhoto.class);
                            intent.putExtra("type", 2);
                            intent.putExtra("log_with_mask", true);
                            startActivityForResult(intent, 1);  // 跳转至拍照页面
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


    private void recPhoto() {  // 使用用户名和密码恢复图像

        Observable.just(URL)
                .map(new Function<String, Boolean>() {

                    @NonNull
                    @Override
                    public Boolean apply(@NonNull String s) throws Exception {

                        retrofit = new Retrofit.Builder().baseUrl(s).build();
                        maeService = retrofit.create(MAEService.class);

                        RequestBody requestBody;
                        CheckBox cb_nopwd = rootView.findViewById(R.id.cb_nopwd);
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
                        progressDialog = new ProgressDialog(getActivity());
                        progressDialog.setTitle("正在获取残缺图像");
                        progressDialog.setCanceledOnTouchOutside(false);
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
                        else {
                            Toast.makeText(getActivity(), "获取恢复图像失败", Toast.LENGTH_SHORT).show();
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
                        progressDialog = new ProgressDialog(getActivity());
                        progressDialog.setTitle("正在比对人脸");
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
                            if (matchScore > 80.0) {  // 得分在80分以上时，错误率小于万分之一

                                Toast.makeText(getActivity(), "登录成功", Toast.LENGTH_SHORT).show();

                                Bitmap recImageBitmap = base64ToBitmap(recBase64);
                                String recImagePath = saveImage(recImageBitmap, "recovered_image.jpg");

                                Intent intent = new Intent(getActivity(), LoginedActivity.class);
                                intent.putExtra("type", 2);
                                intent.putExtra("ori_image_path", oriImagePath);
                                intent.putExtra("rec_image_path", recImagePath);
                                intent.putExtra("matchScore", matchScore);
                                startActivityForResult(intent, 2);
                            }
                            else {
                                Toast.makeText(getActivity(), "登录失败", Toast.LENGTH_SHORT).show();
                                resetFragment();
                            }
                        }
                        else {
                            Toast.makeText(getActivity(), "比对时出现错误", Toast.LENGTH_SHORT).show();
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

    @Override
    public void getMsgFromActivity(String msg) {  // 从调用者Activity中获取信息

        if (rootView != null && tv_username != null) {
            tv_username.setText(msg);
            myUsername = msg;
            isNFCScanned = true;
        }
    }

    private void resetFragment() {
        tv_username.setText("请刷NFC卡");
        isNFCScanned = false;
    }
}