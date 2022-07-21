本文档为前后端的项目结构介绍

# 后端环境配置

后端文件：mae_API

## 环境信息

- Python 3.8.8
- MySQL 5.7.29

## 配置方法

pip install streamlit
cd mae_APItest
pip install -r requirements.txt

将模型文件放入path/to/pretrain/checkpoint.pth

api运行：
cd mae_APItest
python run_mae_vis.py

## 数据库配置

- mae_APItest/database.py 修改相应数据库信息
- 在数据库栏右键转储SQL文件（maes.sql文件）

# 后端API文档

下方传入的图片一定尽量是正方形图片

### /test

- 最佳appendant判断

```python
        @params:    img_base64: 用户人脸, default = "data:xxx"，见上mae_args类, 必选
                    checkpwd: 用户是否需要密码, default = true, 不选，默认值即可
                    pwd: 用户密码, default = "28", 必选
                    appendant: seed偏移量， default = 31, 必选
        @return:    JSON
                    {"success": True, "rec_img": "data:image/jpg;base64," + rec_img, "appendant": opts.appendant, "checkpwd": opts.checkpwd,"score":result_baidu['result']['score']}
        可在output/mask_img查看遮罩图，rec_img and ori_img,score为当前最佳附加值下的最佳得分，一定大于93分
```

### /signup

- 人脸注册

```python
        @params:    img_base64:用户base64图像，必选, default: "data:xxx见上mae_args"
 		    pwd: 用户密码, default = "28", 必选
                    checkpwd: 用户是否需要密码，必选, default: true
                    appendant: seed偏移量, 非必选，default = 31, checkpwd == true 时需要
                    username: 用户名，必选， default = ""
        @return: JSON: {"success": True, "patch_img": "data:image/jpg;base64," + patch_img,
            "mask_img": "data:image/jpg;base64," + mask_img, "appendant": , "checkpwd": }
           	patch_img为数据库存储图像块
            	mask_img为展示给用户的残缺图像

		JSON: {"success": False, "err_msg": "have existed username","err_code":2}
```

### /rec

- 人脸恢复

```python
        @params: username:"alice"; pwd:"asdf"(if checkpwd == true)
        @return: JSON {"success": True, "rec_img": "data:image/jpg;base64," + rec_img}
		 JSON  {"success": False, "err_msg": "not exists username","err_code":1 }
        rec_img 为复原后图像的base64
```
### /match

增加活体检测功能，新增live1和live2两个传入的post参数控制活体检测等级
活体检测控制 NONE: 不进行控制 LOW:较低的活体要求(高通过率 低攻击拒绝率) NORMAL: 一般的活体要求(平衡的攻击拒绝率, 通过率) HIGH: 较高的活体要求(高攻击拒绝率 低通过率) 默认NONE

- 人脸相似度对比

```python
        @params:    img1_base64: 用户人脸1, default = "data:xxx"，见上mae_args类, 必选
		    live1：对人脸1活体检测控制等级
		    img2_base64: 用户人脸2, default = "data:xxx"，见上mae_args类, 必选
		    live2：对人脸2活体检测控制等级

        @return:    JSON{"success": False, "err_msg":'password is not true'}
		    JSON{"success": False, "err_msg":'result_baidu['err_msg']}
		    JSON{"success": True, "score":result_baidu['result']['score']}
	score超过80分后出错的概率小于万分之一

```

### /detect

新增返回角度（俯仰角，左右角，平面旋转角度）用户检测生物密钥的可靠性
+angle	是	array	人脸旋转角度参数
++yaw	是	double	三维旋转之左右旋转角[-90(左), 90(右)]
++pitch	是	double	三维旋转之俯仰角度[-90(上), 90(下)]
++roll	是	double	平面内旋转角[-180(逆时针), 180(顺时针)]
新增口罩是否佩戴参数

- 人脸裁剪，给一张带有人脸的图，返回一张正方形的只有人脸部分的图

```    
        @params:    img_base64: 带有用户人脸的图, default = "data:xxx"，见上mae_args类, 必选
                  
      @ return JSON{"success": True, "angle":result_baidu['result']['face_list'][0]['angle'],"blur":result_baidu['result']['face_list'][0]['quality']['blur'],"mask":result_baidu['result']['face_list'][0]['mask']['type'],"completeness":result_baidu['result']['face_list'][0]['quality']['completeness'],'face_img':img64,"pwd":str(pwd),"code":20000}

注：可在output/cropped_img.jpg查看裁剪后的图，blur表示模糊度，0-1之间，建议小于0.001；completeness脸部完整度，0-1之间，建议大于0.99；pwd表示人脸生物密钥分析结果


```

百度人脸api具体详见api接口文档https://ai.baidu.com/ai-doc/FACE/ek37c1qiz

简易版本前端展示：
cd mae_web
streamlit run main.py

# Streamlit前端说明

本地streamlit项目文件:mae_web

streamlit端（主页兼注册页演示（在线拍照需要配置权限））:http://mae.zkabout.xyz:8501

## 在线拍照需要配置浏览器权限：

1.打开谷歌浏览器后地址栏输入下面链接:chrome://flags/#unsafely-treat-insecure-origin-as-secure

2.在打开的页面第一个输入框中输入你网页的地址，比如本页：http://mae.zkabout.xyz:8501/

3.点击右边按钮，选择Endble

4.点击右下角的重启按钮重启浏览器

# VUE前端说明

vue端code：https://github.com/qzkinhit/mae_vue

在线演示：（上班打卡系统在线演示）: https://mae.zkabout.xyz/

本文件夹为基于Vue框架的“见微”系统-员工打卡场景开发源码，开发基于vue-element-admin模板，其中人脸检测模块调用tracking.js，依赖包管理采用npm，运行前需通过`npm install`安装依赖库

## 几个重要的项目目录

-- node_modules		项目依赖包

-- src

​		-- api 						后端请求接口

​		-- componets			前端组件

​		-- router				   导航栏路由管理

​	 	-- views					页面主文件(.vue) 

​				-- auth.vue		认证页面

​				-- login/index.vue				登录页面

​				-- dashboard/index.vue		员工演示主页

# 安卓APP端说明

本地安卓APP端项目文件:MAEAPP

安卓端（app下载链接）: https://mae.zkabout.xyz/mae.apk

本项目的安卓APP端使用Android Studio IDE进行开发并生成程序包。运行该App的最低系统版本Android 5.0 (Lollipop)，推荐系统版本Android 11.0 (R)。设备硬件要求配有前置摄像头（必须）、支持NFC功能（非必须，若不支持则无法体验门禁打卡功能）。需要用到的权限有开启前置摄像头、使用互联网、使用NFC、读取手机状态等。其中，如开启前置摄像头等敏感权限需要在App首次运行时由用户手动授权，否则将会影响到App的正常功能。为了获得更好的使用体验，使用期间请关闭系统的深色模式。

此外，App中的微信刷脸取纸功能需要获取用户SIM卡的手机号码，但使用某些运营商（例如电信）的SIM卡可能出现无法获取的现象，这种情况下需要用户手动输入手机号进行注册/登录。

## 部分重要的项目目录

```
└─app
    │  build.gradle  # 项目配置文件
    │
    └─src
        └─main
            │  AndroidManifest.xml  # 应用清单
            │
            ├─java  # Java代码
            │  └─com
            │      └─example
            │          └─maeapp
            │                  Access.java  # 门禁打卡
            │                  Identify.java  # 双重认证
            │                  LoginedActivity.java  # 登录后页面
            │                  LoginFragment.java  # 双重认证登录页面
            │                  LoginFragment2.java  # 门禁打卡登录页面
            │                  MAEService.java  # 后端API
            │                  MainActivity.java  # 主页面
            │                  MyBean.java  # Json解析器
            │                  MyFragmentPagerAdapter.java
            │                  NFCBase.java  # NFC功能
            │                  RegisteredActivity.java  # 注册后页面
            │                  RegisterFragment.java  # 双重认证注册页面
            │                  RegisterFragment2.java  # 门禁打卡注册页面
            │                  TakePhoto.java  # 拍照页面
            │                  ToFragmentListener.java
            │                  Towel.java  # 刷脸取纸
            │                  Unlock.java  # 设备解锁
            │
            └─res  # 布局资源
                ├─drawable  # 位图资源文件
                │      accessregisterbg.png
                │      aimblack.png
                │      androidblack.png
                │      bottom_line.xml
                │      button_background.xml
                │      button_background2.xml
                │      button_background3.xml
                │      button_background_wxcancle.xml
                │      button_background_wxconfirm.xml
                │      camerabg.png
                │      camerabg2.png
                │      circle.xml
                │      circle2.xml
                │      edittext_background.xml
                │      edittext_background2.xml
                │      face_range.png
                │      flickrblack.png
                │      ic_baseline_arrow_back_24.xml
                │      ic_baseline_arrow_back_24_black.xml
                │      ic_launcher_background.xml
                │      image_background.xml
                │      logo.png
                │      logo_with_title.png
                │      rectangle_round_corner.xml
                │      registerbg.png
                │      registeredbg.png
                │      scenepagebg.png
                │      towel.png
                │      unlockbg.png
                │      wechatblack.png
                │
                ├─layout  # 布局资源文件
                │      activity_access.xml
                │      activity_identify.xml
                │      activity_logined.xml
                │      activity_logined2.xml
                │      activity_logined3.xml
                │      activity_logined4.xml
                │      activity_main.xml
                │      activity_registered.xml
                │      activity_registered2.xml
                │      activity_registered3.xml
                │      activity_registered4.xml
                │      activity_towel.xml
                │      activity_unlock.xml
                │      fragment_login.xml
                │      fragment_login2.xml
                │      fragment_register.xml
                │      fragment_register2.xml
                │      take_photo.xml
                │      take_photo2.xml
                │      top_layout.xml
                │
                └─values
                        colors.xml  # 颜色色值
                        strings.xml  # 字符串
                        themes.xml  # 主题配置
```
