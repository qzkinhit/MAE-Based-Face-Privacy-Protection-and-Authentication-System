package com.example.maeapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TakePhoto extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = "wyt";

    private Camera camera; // 定义一个摄像头对象
    private boolean isPreview = false; // 是否为预览状态
    private boolean isReset = true; // 摄像头是否以及重置
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ImageButton bt_preview;

    private boolean isPhotoChecked = false;

    private static final String URL = "http://101.43.135.58:12340/";
//    private static final String URL = "http://192.168.0.100:12340/";
    private MAEService maeService;
    Retrofit retrofit;

    private boolean regNoMask;
    private boolean logWithMask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        int type = intent.getIntExtra("type", 1);
        if (type == 1)
            setContentView(R.layout.take_photo);
        else if (type == 2)
            setContentView(R.layout.take_photo2);
        regNoMask = intent.getBooleanExtra("reg_no_mask", false);
        logWithMask = intent.getBooleanExtra("log_with_mask", false);

        Toolbar toolbar = findViewById(R.id.tb);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 设置全屏显示

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) // 判断手机是否安装SD卡
            Toast.makeText(this, "请安装SD卡！", Toast.LENGTH_SHORT).show();

        // 申请摄像头权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            int checkCamera = ContextCompat.checkSelfPermission(TakePhoto.this, Manifest.permission.CAMERA);
//            int checkWrite = ContextCompat.checkSelfPermission(TakePhoto.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);


//            if (checkCamera == PackageManager.PERMISSION_GRANTED && checkWrite == PackageManager.PERMISSION_GRANTED) {
            if (checkCamera == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Granted");
                startCamera();
            }
            else {
                Log.i(TAG, "Not Granted");
//                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            }
        }
    }

    // 用户点击授权后的回调函数
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case 1:
//                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] ==PackageManager.PERMISSION_GRANTED) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted 授予权限
                    //处理授权之后逻辑
                    startCamera();
                } else {
                    // Permission Denied 权限被拒绝
                    Intent resIntent = new Intent();
                    resIntent.putExtra("isSuccess", false);
                    setResult(RESULT_OK, resIntent);
                    TakePhoto.this.finish();
                }
                break;
            default:
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    public void startCamera() {  // 启动摄像头

        Log.i(TAG, "startCamera: ");
        
        // 打开摄像头并预览
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder(); // 获取SurfaceHolder
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // 设置SurfaceView自己不维护缓冲

        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        Camera.Parameters parameters = camera.getParameters(); // 获取摄像头参数
        FrameLayout.LayoutParams svParams = (FrameLayout.LayoutParams) surfaceView.getLayoutParams();
        svParams.height = parameters.getPreviewSize().width;
        svParams.width = parameters.getPreviewSize().height;
        surfaceView.setLayoutParams(svParams);
    }

    final Camera.PictureCallback jpeg = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {

            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Matrix matrix = new Matrix();
            matrix.setRotate(270);  // 旋转图像
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            String base64 = "data:image/jpg;base64," + bitmapToBase64(bitmap);
//            String base64 = "data:image/jpg;base64,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";
//            saveBase64(base64);

            new Thread() {
                @Override
                public void run() {  // 创建子线程负责检测人脸和裁剪
//                    try {
//                        Thread.sleep(5000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    retrofit = new Retrofit.Builder().baseUrl(URL).build();
                    maeService = retrofit.create(MAEService.class);

                    RequestBody requestBody;
                    if (regNoMask) {
                        requestBody = RequestBody.create(MediaType.parse("application/json"),
                                "{" +
                                        "\"img_base64\":\"" + base64 + "\"," +
                                        "\"mask\":false" +
                                        "}");
                    } else {
                        requestBody = RequestBody.create(MediaType.parse("application/json"),
                                "{" +
                                        "\"img_base64\":\"" + base64 + "\"" +
                                        "}");
                    }

//                    try {
//                        final Buffer buffer = new Buffer();
//                        requestBody.writeTo(buffer);
//                        String s = buffer.readUtf8();
//                        Log.i(TAG, s);
//                    } catch (final IOException e) {
//                        e.printStackTrace();
//                    }

                    Call<ResponseBody> call = maeService.detect(requestBody);

                    try {
                        Response<ResponseBody> response = call.execute();
                        Gson gson = new Gson();
                        ResponseBody res = response.body();

                        if (res == null)
                            return;

                        String json = res.string();
                        MyBean myBean = gson.fromJson(json, MyBean.class);

                        if (!myBean.isSuccess())
                            return;

                        if (regNoMask && myBean.getMask() == 1) {  // 双重认证注册时不能戴口罩
                            Log.e(TAG, "wearing a mask while registering");
                            Looper.prepare();
                            Toast.makeText(TakePhoto.this, "请摘口罩！", Toast.LENGTH_SHORT).show();
                            Looper.loop();
                            return;
                        }

                        if (logWithMask && myBean.getMask() == 0) {  // 门禁打卡登录时必须戴口罩
                            Log.e(TAG, "not wearing a mask while logging in");
                            Looper.prepare();
                            Toast.makeText(TakePhoto.this, "请戴口罩！", Toast.LENGTH_SHORT).show();
                            Looper.loop();
                            return;
                        }

//                        MyBean angle = myBean.getAngle();
//                        float pitch = angle.getPitch();
//                        if (pitch < -5 || pitch > 5) {
//                            Log.e(TAG, "angle too large");
//                            Looper.prepare();
//                            Toast.makeText(TakePhoto.this, "请正视摄像头！", Toast.LENGTH_SHORT).show();
//                            Looper.loop();
//                            return;
//                        }

//                        saveBase64(myBean.getFace_img());
                        Bitmap detectedBitmap = base64ToBitmap(myBean.getFace_img().replaceAll("data:image/jpg;base64,", ""));
                        String imagePath = saveImage(detectedBitmap);
                        Log.i(TAG, "run: " + imagePath);
                        Log.i(TAG, "run: " + myBean.getPwd());

                        if (myBean.isSuccess()) {

                            Intent intent = new Intent();
                            intent.putExtra("isSuccess", true);
                            intent.putExtra("path", imagePath);
                            intent.putExtra("bioPassword", myBean.getPwd());
                            setResult(RESULT_OK, intent);
                            TakePhoto.this.finish();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }.start();


            isPreview = false;
            resetCamera(); // 重置预览
        }
    };

    private void resetCamera() {
        if (!isPreview) {
            camera.startPreview(); // 开启预览
            isPreview = true;
        }
        isReset = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
        isPreview = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止预览并释放摄像头资源
        Log.i(TAG, "onDestroy: ");
        isPhotoChecked = true;
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            isPreview = false;
        }
    }

    public void clipPhoto() throws InterruptedException {
        if (!isPreview) {
            isPreview = true;
        }
        try {

            camera.setPreviewDisplay(surfaceHolder); // 设置用于显示预览的SurfaceView
            Camera.Parameters parameters = camera.getParameters(); // 获取摄像头参数

            parameters.setPictureFormat(PixelFormat.JPEG); // 设置图片格式为JPG
            parameters.set("jpeg-quality", 80);

            camera.setParameters(parameters);
            camera.setDisplayOrientation(90);
            camera.startPreview();
            camera.autoFocus(null); // 设置自动对焦
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread() {
            @Override
            public void run() {
                try {
                    getCheckedPhoto();  // 创建子线程，负责抓取图像
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        Log.i(TAG, "surfaceCreated: ");
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.i(TAG, "surfaceChanged: ");
        try {
            clipPhoto();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        Log.i(TAG, "surfaceDestroyed: ");
    }

    public void getCheckedPhoto() throws InterruptedException {

        boolean firstFlag = true;
        while (true) {
            if (isPhotoChecked)
                break;
            if (firstFlag) {
                firstFlag = false;
                Thread.sleep(5000);  // 第一次拍照在创建页面5秒后
            } else
                Thread.sleep(3000);  // 之后每隔3秒拍照一次
            if (!isReset)
                continue;
            new Thread() {  // 创建新线程负责拍照
                @Override
                public void run() {
                    try {
                        isReset = false;
                        takePhoto();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    public synchronized void takePhoto() throws InterruptedException {
        if (camera != null && !isPhotoChecked)
            camera.takePicture(null, null, jpeg);
//        isPhotoChecked = true;

    }


    public static String bitmapToBase64(Bitmap bitmap) {  // 位图编码为base64

        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                byte[] bytes = baos.toByteArray();
                Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                Log.i(TAG, "ByteCount: " + bm.getByteCount() / 1024 + "\tByteLength:" + bytes.length / 1024);

                byte[] bitmapBytes = baos.toByteArray();
                baos.flush();
                baos.close();

                result = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);


            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static Bitmap base64ToBitmap(String base64) {  // base64解码为位图

        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return bitmap;
    }

    public String saveImage(Bitmap bitmap) {

        File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
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

    public void saveBase64(String base64) {

        try {
//            File file = new File(getExternalCacheDir(), "test.txt");
            File file = new File("/storage/emulated/0/Download/test22.txt");
            Log.i(TAG, "saveBase64: " + file.getPath());
            if (file.exists())
                file.delete();
            file.createNewFile();

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(base64.getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}