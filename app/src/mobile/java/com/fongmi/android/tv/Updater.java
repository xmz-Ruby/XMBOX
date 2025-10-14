package com.fongmi.android.tv;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.databinding.DialogUpdateBinding;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Github;
import com.github.catvod.utils.Logger;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

public class Updater implements Download.Callback {

    private DialogUpdateBinding binding;
    private final Download download;
    private AlertDialog dialog;
    private boolean dev;
    private boolean forceCheck; // 是否为手动检查

    private File getFile() {
        return Path.root("Download", "XMBOX-update.apk");
    }

    private String getJson() {
        return Github.getJson(dev, BuildConfig.FLAVOR_mode);
    }

    private String getApk() {
        // 使用JSON中指定的具体下载路径
        try {
            String response = OkHttp.string(getJson());
            JSONObject object = new JSONObject(response);
            JSONObject downloads = object.optJSONObject("downloads");
            if (downloads != null) {
                String abi = BuildConfig.FLAVOR_abi;
                String downloadPath = downloads.optString(abi);
                if (!downloadPath.isEmpty()) {
                    // 直接构建完整URL，不通过Github.getApk()避免重复添加路径
                    String baseUrl = Github.useCnMirror() ? 
                        "https://gitee.com/ochenoktochen/XMBOX-Release/raw/main" :
                        "https://raw.githubusercontent.com/Tosencen/XMBOX-Release/main";
                    String fullUrl = baseUrl + "/apk/" + (dev ? "dev" : "release") + "/" + downloadPath;
                    Logger.d("APK download URL: " + fullUrl);
                    return fullUrl;
                }
            }
        } catch (Exception e) {
            Logger.e("Failed to get download path from JSON: " + e.getMessage());
        }
        // 回退到原来的方式
        String fallbackUrl = Github.getApk(dev, BuildConfig.FLAVOR_mode + "-" + BuildConfig.FLAVOR_abi);
        Logger.d("APK fallback URL: " + fallbackUrl);
        return fallbackUrl;
    }

    public static Updater create() {
        return new Updater();
    }

    public Updater() {
        this.download = Download.create(getApk(), getFile(), this);
        this.forceCheck = false;
    }

    public Updater force() {
        Notify.show(R.string.update_check);
        Setting.putUpdate(true);
        this.forceCheck = true; // 标记为手动检查
        return this;
    }

    public Updater release() {
        this.dev = false;
        return this;
    }

    public Updater dev() {
        this.dev = true;
        return this;
    }

    private Updater check() {
        dismiss();
        return this;
    }

    public void start(Activity activity) {
        App.execute(() -> doInBackground(activity));
    }

    private boolean need(int code, String name) {
        return Setting.getUpdate() && (dev ? !name.equals(BuildConfig.VERSION_NAME) && code >= BuildConfig.VERSION_CODE : code > BuildConfig.VERSION_CODE);
    }

    private void doInBackground(Activity activity) {
        Logger.d("Updater: Starting update check...");
        try {
            String jsonUrl = getJson();
            Logger.d("Updater: JSON URL: " + jsonUrl);
            
            String response = OkHttp.string(jsonUrl);
            Logger.d("Updater: JSON response length: " + response.length());
            
            // 检查响应是否包含错误信息，只在手动检查时提示
            if (response.contains("rate limit exceeded")) {
                Logger.e("Updater: Rate limit exceeded");
                if (forceCheck) {
                    App.post(() -> Notify.show("检查更新失败：API请求过于频繁，请稍后重试"));
                }
                return;
            }
            
            if (response.contains("Not Found Project") || response.contains("Not Found")) {
                Logger.e("Updater: Project not found");
                if (forceCheck) {
                    App.post(() -> Notify.show("检查更新失败：更新服务暂时不可用"));
                }
                return;
            }
            
            JSONObject object = new JSONObject(response);
            String name = object.optString("name");
            String desc = object.optString("desc");
            int code = object.optInt("code");
            
            Logger.d("Updater: Remote version: " + name + " (code: " + code + ")");
            Logger.d("Updater: Local version: " + BuildConfig.VERSION_NAME + " (code: " + BuildConfig.VERSION_CODE + ")");
            
            if (need(code, name)) {
                Logger.d("Updater: Update needed, showing dialog");
                App.post(() -> show(activity, name, desc));
            } else {
                Logger.d("Updater: No update needed");
                // 只在手动检查时提示已是最新版
                if (forceCheck) {
                    App.post(() -> Notify.show("已是最新版本 " + name));
                }
                Logger.d("Already latest version: " + name);
            }
        } catch (Exception e) {
            Logger.e("Updater: Exception during update check: " + e.getMessage());
            e.printStackTrace();
            // 只在手动检查时提示网络错误
            if (forceCheck) {
                App.post(() -> Notify.show("检查更新失败：网络连接异常"));
            }
        }
    }

    private void show(Activity activity, String version, String desc) {
        binding = DialogUpdateBinding.inflate(LayoutInflater.from(activity));
        check().create(activity, ResUtil.getString(R.string.update_version, version)).show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this::confirm);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(this::cancel);
        binding.desc.setText(desc);
    }

    private AlertDialog create(Activity activity, String title) {
        return dialog = new MaterialAlertDialogBuilder(activity).setTitle(title).setView(binding.getRoot()).setPositiveButton(R.string.update_confirm, null).setNegativeButton(R.string.dialog_negative, null).setCancelable(false).create();
    }

    private void cancel(View view) {
        Setting.putUpdate(false);
        download.cancel();
        dialog.dismiss();
    }

    private void confirm(View view) {
        view.setEnabled(false);
        download.start();
    }

    private void dismiss() {
        try {
            if (dialog != null) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void progress(int progress) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(String.format(Locale.getDefault(), "%1$d%%", progress));
    }

    @Override
    public void error(String msg) {
        Notify.show(msg);
        dismiss();
    }

    @Override
    public void success(File file) {
        FileUtil.openFile(file);
        dismiss();
    }
}