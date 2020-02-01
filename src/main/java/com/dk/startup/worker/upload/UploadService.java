package com.dk.startup.worker.upload;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "dk.fastdfs.innerDomain")
public class UploadService {
    private static final String UPLOAD_PATH="/group1/upload";

    @Value("${dk.fastdfs.innerDomain}")
    private String innerDomain;

    @Value("${dk.fastdfs.outerDomain}")
    private String outerDomain;

    public String upload(MultipartFile file) {
        String result = "";
        try {
            OkHttpClient httpClient = new OkHttpClient();
            MultipartBody multipartBody = new MultipartBody.Builder().
                    setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getOriginalFilename(),
                            RequestBody.create(MediaType.parse("multipart/form-data;charset=utf-8"),
                                    file.getBytes()))
                    .addFormDataPart("output", "json")
                    .build();

            Request request = new Request.Builder()
                    .url(innerDomain+UPLOAD_PATH)
                    .post(multipartBody)
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {
                    result = body.string();
                    System.out.println(result);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSONObject jsonObject = JSON.parseObject(result);

        return jsonObject.getString("src");
    }

    public String getFileSrc(String fileName){
        return outerDomain+(fileName.startsWith("/")?"":"/")+fileName;
    }
}



/**
 import com.google.gson.Gson;
 import com.qiniu.common.QiniuException;
 import com.qiniu.common.Zone;
 import com.qiniu.http.Response;
 import com.qiniu.storage.Configuration;
 import com.qiniu.storage.UploadManager;
 import com.qiniu.storage.model.DefaultPutRet;
 import com.qiniu.util.Auth;
 **/

/**
 * 七牛上传服务
 private static final String BUCKET = "scf";

 @Value("${oursoft.qiniu.accessKey}")
 private String accessKey;

 @Value("${oursoft.qiniu.secretKey}")
 private String secretKey;

 @Value("${oursoft.qiniu.domainOfBucket}")
 private String domainOfBucket;

 public String upload(InputStream file) {
 Configuration cfg = new Configuration(Zone.zone2());
 UploadManager uploadManager = new UploadManager(cfg);


 String key = null;
 try {
 Auth auth = Auth.create(accessKey, secretKey);
 String upToken = auth.uploadToken(BUCKET);
 try {
 Response response = uploadManager.put(file, key, upToken, null, null);
 //解析上传成功的结果
 DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
 System.out.println(putRet.key);
 System.out.println(putRet.hash);
 return putRet.key;
 } catch (QiniuException ex) {
 Response r = ex.response;
 System.err.println(r.toString());
 try {
 System.err.println(r.bodyString());
 } catch (QiniuException ex2) {
 //ignore
 }
 }
 } catch (Exception ex) {
 //ignore
 ex.printStackTrace();
 }
 return null;
 }

 public String getFileSrc(String fileName) throws UnsupportedEncodingException {
 String encodedFileName = URLEncoder.encode(fileName, "utf-8").replace("+", "%20");
 String publicUrl = String.format("%s/%s", domainOfBucket, encodedFileName);
 Auth auth = Auth.create(accessKey, secretKey);
 long expireInSeconds = 3600;
 String finalUrl = auth.privateDownloadUrl(publicUrl, expireInSeconds);
 System.out.println(finalUrl);
 return finalUrl;
 }
 **/