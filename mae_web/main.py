import streamlit as st
import numpy as np
import cv2
import base64
import requests
import streamlit.components.v1 as components

flag1 = False  # 是否成功上传图片
flag2 = False  # 是否填写了密码
flag3 = False  # 是否已经注册

url = 'http://localhost:12340'  # mae服务器地址
def findAppendant(checkpwd,key,username,img64):
            r = requests.post(url+'/test', json={
                'checkpwd':checkpwd,
                'pwd': key,
                'username':username,
                'img_base64': img64
            })
            if r.status_code == 200:  # 重要的检查
                    result_test = r.json()
                    # st.text(result)
                    if(result_test['success']==False):
                        st.error(result_test['err_msg'])
                    else:
                        return [result_test['score'],result_test['appendant']]
            else:
                st.info('未知错误，请重试')

if __name__ == "__main__":              
    st.title('“见微”系统 项目演示')
    st.subheader("基于mae认证的衍生应用系列场景：\n")
    st.markdown('&emsp;&emsp;<a href="https://mae.zkabout.xyz">[web端]基于mae认证的办公打卡系统</a>', unsafe_allow_html=True) 
    st.markdown('&emsp;&emsp;<a href="https://mae.zkabout.xyz/mae.apk">[安卓端]基于MAE的移动设备场景</a>', unsafe_allow_html=True) 
    option = st.selectbox('功能选择',('项目介绍','登录认证', '注册'))
    if(option=="项目介绍"):
         st.header('“见微”——基于MAE的人脸隐私保护和双重认证系统')
         st.subheader("\n")
         video_file = open('myvideo.mp4', 'rb')
         video_bytes = video_file.read()
         st.video(video_bytes)
         st.subheader("\n")
         st.header("一、项目介绍")
         st.subheader("\n")
         st.subheader("我组希望用一种新的图像语义补全模型，从数据库存储层面解决一般场景下的人脸隐私保护的问题。同时实现一种可靠的双重加密认证系统。")
         st.subheader("\n")
         st.header("二、项目流程图")
         st.subheader("\n")
         opencv_image = cv2.imread('process_img.jpg')
         st.image(opencv_image, channels='BGR')
         st.header("三、项目优势")
         st.subheader("\n")
         st.subheader("1.整个认证过程中，系统中不可能也没有能力还原100%的人脸信息，这保证了整个系统中无法被盗用清晰的人脸图片。\n")
         st.subheader("2.即使原理上能复原部分人脸信息，也无法有人独立做到这一点，需要双重密钥认证，即人脸和密钥缺一不可。\n")
         st.subheader("3.复原后的图像绝无可能用于其它系统（其它任何的人脸系统），因为不同MAE系统的密钥转mask位置的加密算法不同且不开源，保证了同一个密钥和图像块不可能用于其它系统中。\n")
         st.subheader("4.加密过程对用户透明，用户知道服务器存储的图像块，加密过程简单。\n")
         st.subheader("5.人脸+密钥双重认证使得准确率大大提高基本不可能识别失误。\n")
         st.subheader("6.服务器端不接手图像原图，只接手25%的碎片化去图像化信息\n")
         st.subheader("7.在我们设置的一些场景下可以遮挡识别和注册，戴口罩满足疫情防控要求。\n")
         st.subheader("8.活体防伪技术和模糊检测技术，去除不合格的脸（在本系统中为了方便演示没有）\n")
    elif(option=="注册"):
        result=None;
        st.header('注册，请上传您的人脸图片和密码')
        st.subheader("在线拍照需要配置浏览器权限：")
        st.subheader("1.打开谷歌浏览器后地址栏输入下面链接:chrome://flags/#unsafely-treat-insecure-origin-as-secure")
        st.subheader("2.在打开的页面第一个输入框中输入你网页的地址，比如本页：http://mae.zkabout.xyz:8501/")
        st.subheader("3.点击右边按钮，选择Endble")
        st.subheader("4.点击右下角的重启按钮重启浏览器")
        genre = st.radio("选择上传图片方式",('上传图片', '在线拍照'))
        if genre == '在线拍照':
            opencv_image = st.camera_input("Take a picture")

        else:
            opencv_image = st.file_uploader("上传人脸图片", type='jpg')
        if opencv_image is not None:
            st.text('注册时的人脸图像')
            file_bytes = np.asarray(bytearray(opencv_image.read()), dtype=np.uint8)
            opencv_image = cv2.imdecode(file_bytes, 1)
            # if(opencv_image.shape[0]>2000 or opencv_image.shape[1] >2000):
            #         opencv_image=cv2.resize(opencv_image,(0,0),fx=0.2,fy=0.2,interpolation=cv2.INTER_NEAREST) 
            cv2.imwrite('ori_face.jpg', opencv_image)
            # params = [cv2.IMWRITE_JPEG_QUALITY, 10]  # ratio:0~100
            # msg = cv2.imencode(".jpg", opencv_image, params)[1]
            # msg = (np.array(msg)).tobytes()
            # print("msg:", len(msg))
            # opencv_image = cv2.imdecode(np.frombuffer(msg, np.uint8), cv2.IMREAD_COLOR)
            # cv2.imwrite('ori_face.jpg', opencv_image)
            # 转base64
            img_encode = cv2.imencode('.jpg', opencv_image)[1]
            data_encode = np.array(img_encode)
            img_b64 = data_encode.tobytes()
            img64 = base64.b64encode(img_b64).decode('utf-8')
            img64 = 'data:image/jpg;base64,' + img64
            r = requests.post(url+'/detect', json={
                    'img_base64': img64,
                    'mask':True
                })
            # r1 = requests.post(url+'/skin_pwd', json={
            #         'img_base64': img64,
            #     })
            result = r.json()
            if(result['success']==False):
                 st.error(result['err_msg'])
            elif(result['completeness']>0.99 and result['blur']<0.01):
                key=result['pwd'];
                st.info('分析出的对应人脸特征密钥为：'+str(key));
                face_img = result['face_img'][22:]
                # st.text((mask_img))
                imgData = base64.b64decode(face_img)
                nparr = np.frombuffer(imgData, np.uint8)
                img_np = cv2.imdecode(nparr, 1)
                st.image(img_np, channels='BGR',width=200)
                cv2.imwrite('face_img.jpg', img_np)
                # 转base64
                img_encode = cv2.imencode('.jpg', img_np)[1]
                data_encode = np.array(img_encode)
                img_b64 = data_encode.tobytes()
                img64 = base64.b64encode(img_b64).decode('utf-8')
                img64 = 'data:image/jpg;base64,' + img64
                flag1 = True
            else:
                 st.error('上传的人脸不够清晰')
            # st.text(img64)
            flag1 = True
        username = st.text_input('输入用户名', type='default')
        agree = st.checkbox('是否需要设置密钥？不需要密钥时，我们将采用人脸特征密钥进行注册')
        st.info("注：人脸特征密钥我们对人脸进行分析获取的生物密钥，该密钥只有在获取到完整清晰人脸时才能分析出，服务器端数据库中的碎片化图像是无法分析出密钥的\n")
        if agree:
            key = st.text_input('设置密码', type='password')
            if key is not None and key != '':
                flag2 = True
                checkpwd=True;
        else:
            flag2 = True
            checkpwd=False;
        if st.button('确认') and flag3 is False:
            if flag1 and flag2:
                [score,appendant]=findAppendant(checkpwd,key,username,img64)   
                r = requests.post(url+'/signup', json={
                    'checkpwd':checkpwd,
                    'pwd': key,
                    'username':username,
                    'img_base64': img64,
                    'appendant':appendant
                })
                if r.status_code == 200:  # 重要的检查
                        result = r.json()
                        # st.text(result)
                        if(result['success']==False):
                            st.error(result['err_msg'])
                        else:
                            st.info('设置成功，接下来可以识别')
                            mask_img = result['mask_img'][22:]
                            # add
                            st.text('遮罩的图像')
                            imgData = base64.b64decode(mask_img)
                            nparr = np.frombuffer(imgData, np.uint8)
                            img_np = cv2.imdecode(nparr, 1)
                            st.image(img_np, channels='BGR')
                            cv2.imwrite('mask_img.jpg', img_np)
                                # st.text(result)
                            mask_img = result['mask_img']
                            # add
                            st.text('数据库中存储的图像')
                            patch_img = result['patch_img'][22:]
                            # st.text((mask_img))
                            imgData = base64.b64decode(patch_img)
                            nparr = np.frombuffer(imgData, np.uint8)
                            img_np = cv2.imdecode(nparr, 1)
                            st.image(img_np, channels='BGR')
                            cv2.imwrite('patch_img.jpg', img_np)
                                # st.text(result)
                            flag3 = True
                else:
                    st.error('未知错误，请重试')
            else:
                st.error('设置失败，请确认您上传了人脸和密码')

    elif(option=="登录认证"):
        st.header('此页面用于登录认证，确认是否注册成功')
        flag1 = False
        flag2 = False
        flag4 = False  # 是否传了图片和密码
        # if flag3 is True:
        st.header('识别，请上传您的人脸并输入密码')
        st.subheader("在线拍照需要配置浏览器权限：")
        st.subheader("1.打开谷歌浏览器后地址栏输入下面链接:chrome://flags/#unsafely-treat-insecure-origin-as-secure")
        st.subheader("2.在打开的页面第一个输入框中输入你网页的地址，比如本页：http://mae.zkabout.xyz:8501/")
        st.subheader("3.点击右边按钮，选择Endble")
        st.subheader("4.点击右下角的重启按钮重启浏览器")
        # add
        upload_face = None
        genre = st.radio("选择上传图片方式",('上传图片', '在线拍照'))
        if genre == '在线拍照':
            upload_face = st.camera_input("Take a photo")

        else:
            upload_face = st.file_uploader("认证人脸图片", type='jpg')

        username1 = st.text_input('输入用户名', type='default')
        if (upload_face != None): st.text('识别的人脸图像')

        if upload_face is not None:
            file_bytes = np.asarray(bytearray(upload_face.read()), dtype=np.uint8)
            opencv_image = cv2.imdecode(file_bytes, 1)
            if(opencv_image.shape[0]>2000 or opencv_image.shape[0] >2000):
                    opencv_image=cv2.resize(opencv_image,(0,0),fx=0.2,fy=0.2,interpolation=cv2.INTER_NEAREST) 
            img_encode = cv2.imencode('.jpg', opencv_image)[1]
            data_encode = np.array(img_encode)
            img_b64 = data_encode.tobytes()
            img64 = base64.b64encode(img_b64).decode('utf-8')
            img64 = 'data:image/jpg;base64,' + img64
            r = requests.post(url+'/detect', json={
                    'img_base64': img64,
                    'mask':False
                })
            result = r.json()
            if(result['success']==False):
                 st.error(result['err_msg'])
            elif(result['completeness']>0.99 and result['blur']<0.01):
                key1=result['pwd'];
                st.info('分析出的对应人脸特征密钥为：'+str(key1));
                face_img = result['face_img'][22:]
                # st.text((mask_img))
                imgData = base64.b64decode(face_img)
                nparr = np.frombuffer(imgData, np.uint8)
                img_np = cv2.imdecode(nparr, 1)
                st.image(img_np, channels='BGR',width=200)
                cv2.imwrite('face_img.jpg', img_np)
                # 转base64
                img_encode = cv2.imencode('.jpg', img_np)[1]
                data_encode = np.array(img_encode)
                img_b64 = data_encode.tobytes()
                img64 = base64.b64encode(img_b64).decode('utf-8')
                img64 = 'data:image/jpg;base64,' + img64
                flag1 = True
            else:
                 st.error('上传的人脸不够清晰')
 
        agree = st.checkbox('是否需要设置密钥？不需要密钥时，我们将采用人脸特征密钥进行注册')
        st.info("注：人脸特征密钥我们对人脸进行分析获取的生物密钥，该密钥只有在获取到完整清晰人脸时才能分析出，服务器端数据库中的碎片化图像是无法分析出密钥的\n")
        if agree:
                key1 = st.text_input('输入密码', type='password')
                checkpwd=True
                if key1 is not None and key1 != '': 
                    flag2 = True
        else:
            checkpwd=False
            flag2=True
        if flag1 and flag2:

            r = requests.post(url+'/rec', json={
                'checkpwd':checkpwd,
                'pwd': key1,
                'username': username1
            })
            result = r.json()
        if st.button('确 认') and flag4 is False:
            if flag1 and flag2:
                # 得到复原图的base64
                # st.text(result)
                if(result['success']==False):
                    st.error(result['err_msg'])
                else:
                    result_img = result['rec_img'][22:]
                    # print(result)
                    # add
                    st.text('恢复后的图像')
                    imgData = base64.b64decode(result_img)
                    nparr = np.frombuffer(imgData, np.uint8)
                    img_np = cv2.imdecode(nparr, 1)
                    st.image(img_np, channels='BGR')
                    cv2.imwrite('ori_face.jpg', img_np)

                    r = requests.post(url+'/match', json={
                            "img1_base64": result['rec_img'],
                            "img2_base64": img64,
                        })
                    result = r.json()
                    if(result['success']!=True):
                        st.error(result['err_msg'])
                    else:
                        st.text('相似度得分为' + str(result['score']))
                        if result['score']>=80:
                            st.info("认证成功,是同一个人")
                        else:
                            st.info("认证失败，不是一个人或密钥错误")
                        st.success("推荐80分作为阈值，此分值对应万分之一误识率。")
            else:
                st.info('没有上传图片或没有输入密码！')

