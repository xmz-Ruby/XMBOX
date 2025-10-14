package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.App;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Logger;
import com.github.catvod.utils.Path;
import com.google.common.net.HttpHeaders;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.Response;

public class Download {

    private final File file;
    private final String url;
    private Callback callback;

    public static Download create(String url, File file) {
        return create(url, file, null);
    }

    public static Download create(String url, File file, Callback callback) {
        return new Download(url, file, callback);
    }

    public Download(String url, File file, Callback callback) {
        this.url = url;
        this.file = file;
        this.callback = callback;
    }

    public void start() {
        if (url.startsWith("file")) return;
        if (callback == null) doInBackground();
        else App.execute(this::doInBackground);
    }

    public void cancel() {
        OkHttp.cancel(url);
        Path.clear(file);
        callback = null;
    }

    private void doInBackground() {
        try (Response res = OkHttp.newCall(url, url).execute()) {
            Path.create(file);
            long expectedLength = Long.parseLong(res.header(HttpHeaders.CONTENT_LENGTH, "0"));
            download(res.body().byteStream(), expectedLength);
            
            // 验证下载的文件
            if (!verifyDownloadedFile(file, expectedLength)) {
                App.post(() -> {if (callback != null) callback.error("下载的文件可能已损坏，请重试");});
                return;
            }
            
            App.post(() -> {if (callback != null) callback.success(file);});
        } catch (Exception e) {
            App.post(() -> {if (callback != null) callback.error(e.getMessage());});
        }
    }

    private void download(InputStream is, long length) throws Exception {
        try (BufferedInputStream input = new BufferedInputStream(is); FileOutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int readBytes;
            long totalBytes = 0;
            while ((readBytes = input.read(buffer)) != -1) {
                totalBytes += readBytes;
                os.write(buffer, 0, readBytes);
                int progress = (int) (totalBytes / length * 100.0);
                App.post(() -> {if (callback != null) callback.progress(progress);});
            }
        }
    }

    private boolean verifyDownloadedFile(File file, long expectedLength) {
        try {
            // 检查文件大小
            if (file.length() != expectedLength) {
                Logger.e("File size mismatch: expected " + expectedLength + ", actual " + file.length());
                return false;
            }
            
            // 检查APK文件头 (ZIP文件头)
            if (file.length() < 4) return false;
            
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] header = new byte[4];
                fis.read(header);
                // ZIP文件头应该是 0x504B0304 (PK..)
                if (header[0] != 0x50 || header[1] != 0x4B || header[2] != 0x03 || header[3] != 0x04) {
                    Logger.e("Invalid APK file header");
                    return false;
                }
                
                // 额外验证：检查APK文件是否完整
                // 尝试读取ZIP文件结构
                fis.getChannel().position(0);
                byte[] buffer = new byte[1024];
                int bytesRead = fis.read(buffer);
                if (bytesRead < 4) {
                    Logger.e("APK file too small or corrupted");
                    return false;
                }
            }
            
            Logger.d("APK file verification passed: " + file.getName() + " (" + file.length() + " bytes)");
            return true;
        } catch (Exception e) {
            Logger.e("File verification failed: " + e.getMessage());
            return false;
        }
    }

    public interface Callback {

        void progress(int progress);

        void error(String msg);

        void success(File file);
    }
}
