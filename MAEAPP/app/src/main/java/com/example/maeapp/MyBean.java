package com.example.maeapp;

import com.google.gson.annotations.Expose;

public class MyBean {

    @Expose
    private boolean success;

    @Expose
    private String face_img;

    @Expose
    private String mask_img;

    @Expose
    private String patch_img;

    @Expose
    private String rec_img;

    @Expose
    private float score;

    @Expose
    private int appendant;

    @Expose
    private String pwd;

    @Expose
    private boolean checkpwd;

    @Expose
    private int mask;

    @Expose
    private MyBean angle;

    @Expose
    private float pitch;

    public MyBean(boolean success, String face_img, String mask_img) {
        this.success = success;
        this.face_img = face_img;
        this.mask_img = mask_img;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFace_img() {
        return face_img;
    }

    public String getMask_img() {
        return mask_img;
    }

    public String getPatch_img() {
        return patch_img;
    }

    public String getRec_img() {
        return rec_img;
    }

    public float getScore() {
        return score;
    }

    public int getAppendant() {
        return appendant;
    }

    public String getPwd() {
        return pwd;
    }

    public int getMask() {
        return mask;
    }

    public boolean isCheckpwd() {
        return checkpwd;
    }

    public MyBean getAngle() {
        return angle;
    }

    public float getPitch() {
        return pitch;
    }
}
