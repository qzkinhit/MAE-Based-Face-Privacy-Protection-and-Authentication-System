package com.example.maeapp;

import static android.app.Activity.RESULT_OK;

import static com.example.maeapp.TakePhoto.base64ToBitmap;
import static com.example.maeapp.TakePhoto.bitmapToBase64;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import okio.Buffer;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RegisterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RegisterFragment extends Fragment {

    private static final String ARG_TEXT = "param1";
    private static final String TAG = "wyt";
    private final static String URL = "http://101.43.135.58:12340/";
//    private final static String URL = "http://192.168.0.100:12340/";

    private String mTextString;
    View rootView;
    private Bitmap imageBitmap;
    private String myUsername;
    private ProgressDialog progressDialog;
    private String bioPassword;
    private int appendant;
    private Retrofit retrofit;
    private MAEService maeService;
    private String myPassword;
    private Bitmap maskedBitmap;
    private Bitmap dbBitmap;

    public RegisterFragment() {
        // Required empty public constructor
    }

    public static RegisterFragment newInstance() {
        RegisterFragment fragment = new RegisterFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTextString = getArguments().getString(ARG_TEXT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (rootView == null)
                rootView = inflater.inflate(R.layout.fragment_register, container, false);
        initView();
        return rootView;
    }

    private void initView() {

        Button button = rootView.findViewById((R.id.bt));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditText editText = (EditText) rootView.findViewById(R.id.et_username);  // 用户名输入框
                myUsername = editText.getText().toString();
                if (myUsername.equals("")) {
                    Toast.makeText(getActivity(), "请输入用户名", Toast.LENGTH_SHORT).show();
                    return;
                }

                CheckBox cb_nopwd = rootView.findViewById(R.id.cb_nopwd);  // 密码输入框
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

        EditText et_username = (EditText) rootView.findViewById(R.id.et_username);
        // 规定输入字符的类型只能是英文字母、数字和空格
        et_username.setKeyListener(new DigitsKeyListener() {
            @Override
            public int getInputType() {
                return InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL;
            }

            @NonNull
            @Override
            protected char[] getAcceptedChars() {
                return "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
            }
        });
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

            EditText et_username = (EditText) rootView.findViewById(R.id.et_username);
            et_username.setText("");
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
//                            Toast.makeText(getActivity(), "该用户名可用", Toast.LENGTH_SHORT).show();

                            Intent intetnt = new Intent(getActivity(), TakePhoto.class);
                            intetnt.putExtra("type", 1);
                            intetnt.putExtra("reg_no_mask", true);
                            startActivityForResult(intetnt, 1);  // 跳转至拍照页面
                        }
                        else
                            Toast.makeText(getActivity(), "该用户名已经存在", Toast.LENGTH_SHORT).show();
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
                        else
                            Toast.makeText(getActivity(), "照片不可用", Toast.LENGTH_SHORT).show();
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
                            intent.putExtra("type", 1);
                            intent.putExtra("masked_image_path", maskedImagePath);
                            intent.putExtra("db_image_path", dbImagePath);
                            startActivityForResult(intent, 2);
                        }
                        else
                            Toast.makeText(getActivity(), "注册失败", Toast.LENGTH_SHORT).show();
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
}