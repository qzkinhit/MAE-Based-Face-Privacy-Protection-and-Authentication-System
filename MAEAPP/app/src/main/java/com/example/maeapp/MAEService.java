package com.example.maeapp;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface MAEService {

    @POST("checkusername")  // 检查用户名
    Call<ResponseBody> checkUsername(@Body RequestBody body);

    @POST("detect")  // 检测人脸并裁剪
    Call<ResponseBody> detect(@Body RequestBody body);

    @POST("test")  // 测试人脸的质量
    Call<ResponseBody> test(@Body RequestBody body);

    @POST("signup")  // 注册
    Call<ResponseBody> signup(@Body RequestBody body);

    @POST("rec")  // 恢复图像
    Call<ResponseBody> rec(@Body RequestBody body);

    @POST("match")  // 人脸匹配
    Call<ResponseBody> match(@Body RequestBody body);
}
