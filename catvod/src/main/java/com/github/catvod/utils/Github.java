package com.github.catvod.utils;

import android.os.SystemClock;

import com.github.catvod.net.OkHttp;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Github {

    public static final String URL = "https://raw.githubusercontent.com/Tosencen/XMBOX/main";
    public static final String API_URL = "https://api.github.com/repos/Tosencen/XMBOX/releases/latest";
    
    // 国内镜像地址 - 使用Gitee作为镜像
    public static final String CN_URL = "https://gitee.com/ochenoktochen/XMBOX/raw/main";
    public static final String CN_API_URL = "https://gitee.com/api/v5/repos/ochenoktochen/XMBOX/releases/latest";
    
    // 存储测速结果
    private static Boolean useCnMirror = null;
    private static long lastCheckTime = 0;
    private static final long CHECK_INTERVAL = 24 * 60 * 60 * 1000; // 24小时

    private static String getUrl(String path, String name) {
        return URL + "/" + path + "/" + name;
    }
    
    private static String getCnUrl(String path, String name) {
        return CN_URL + "/" + path + "/" + name;
    }

    public static String getReleaseApi() {
        return useCnMirror() ? CN_API_URL : API_URL;
    }

    public static String getJson(boolean dev, String name) {
        if (useCnMirror()) {
            return getCnUrl("apk/" + (dev ? "dev" : "release"), name + ".json");
        } else {
            return getUrl("apk/" + (dev ? "dev" : "release"), name + ".json");
        }
    }

    public static String getApk(boolean dev, String name) {
        if (useCnMirror()) {
            return getCnUrl("apk/" + (dev ? "dev" : "release"), name + ".apk");
        } else {
            return getUrl("apk/" + (dev ? "dev" : "release"), name + ".apk");
        }
    }
    
    // 智能检测是否使用国内镜像
    public static boolean useCnMirror() {
        // 如果已经测试过并且在24小时内，直接返回上次的结果
        long currentTime = SystemClock.elapsedRealtime();
        if (useCnMirror != null && (currentTime - lastCheckTime < CHECK_INTERVAL)) {
            return useCnMirror;
        }
        
        // 进行网络测速
        useCnMirror = testMirrorSpeed();
        lastCheckTime = currentTime;
        return useCnMirror;
    }
    
    // 测试镜像速度
    private static boolean testMirrorSpeed() {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .build();
            
            // 测试国际源
            long startTime = System.currentTimeMillis();
            boolean intlSuccess = testUrl(client, URL + "/README.md");
            long intlTime = System.currentTimeMillis() - startTime;
            
            // 测试国内源
            startTime = System.currentTimeMillis();
            boolean cnSuccess = testUrl(client, CN_URL + "/README.md");
            long cnTime = System.currentTimeMillis() - startTime;
            
            // 如果两个都成功，选择更快的
            if (intlSuccess && cnSuccess) {
                return cnTime < intlTime;
            }
            
            // 如果只有一个成功，选择成功的那个
            if (intlSuccess) return false;
            if (cnSuccess) return true;
            
            // 如果都失败，默认国际源
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false; // 出错时默认使用国际源
        }
    }
    
    private static boolean testUrl(OkHttpClient client, String url) {
        Request request = new Request.Builder().url(url).build();
        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            boolean success = response.isSuccessful();
            response.close();
            return success;
        } catch (IOException e) {
            return false;
        }
    }
}
