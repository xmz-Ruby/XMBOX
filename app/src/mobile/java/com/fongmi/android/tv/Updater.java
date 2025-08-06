package com.fongmi.android.tv;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.databinding.DialogUpdateBinding;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Github;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

public class Updater implements Download.Callback {

    private DialogUpdateBinding binding;
    private Download download;
    private AlertDialog dialog;
    private boolean dev;
    private String downloadUrl;

    private File getFile() {
        return Path.cache("update.apk");
    }

    private String getApkName() {
        return "mobile-" + BuildConfig.FLAVOR_abi + ".apk";
    }

    private String getJson() {
        String url = Github.getReleaseApi();
        boolean usingCnMirror = Github.useCnMirror();
        Logger.d("Using CN Mirror: " + usingCnMirror);
        Logger.d("Update check URL: " + url);
        return url;
    }

    public static Updater create() {
        return new Updater();
    }

    public Updater() {
        this.download = Download.create("", getFile(), this);
    }

    public Updater force() {
        Notify.show(R.string.update_check);
        Setting.putUpdate(true);
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

    private boolean need(String tagName) {
        Logger.d("Current version: " + BuildConfig.VERSION_NAME);
        Logger.d("Latest version: " + tagName);
        if (tagName.startsWith("v")) tagName = tagName.substring(1);
        
        // 版本比较逻辑
        try {
            String[] currentParts = BuildConfig.VERSION_NAME.split("\\.");
            String[] remoteParts = tagName.split("\\.");
            
            // 比较主版本号
            for (int i = 0; i < Math.min(currentParts.length, remoteParts.length); i++) {
                int current = Integer.parseInt(currentParts[i]);
                int remote = Integer.parseInt(remoteParts[i]);
                
                if (remote > current) {
                    return Setting.getUpdate(); // 远程版本高于当前版本，需要更新
                } else if (remote < current) {
                    return false; // 远程版本低于当前版本，不需要更新
                }
                // 如果相等，继续比较下一级版本号
            }
            
            // 如果前面的版本号都相等，但远程版本有更多的版本号，视为更新
            if (remoteParts.length > currentParts.length) {
                return Setting.getUpdate();
            }
            
            return false; // 版本相同或远程版本较低，不需要更新
        } catch (NumberFormatException e) {
            // 如果版本号解析失败，退回到简单字符串比较
            Logger.e("Version parsing failed", e);
            return Setting.getUpdate() && !tagName.equals(BuildConfig.VERSION_NAME);
        }
    }

    private void doInBackground(Activity activity) {
        try {
            String jsonUrl = getJson();
            Logger.d("Fetching update info from: " + jsonUrl);
            String response = OkHttp.string(jsonUrl);
            Logger.d("Update check response: " + response);
            
            // 检查响应是否包含错误信息
            if (response.contains("rate limit exceeded")) {
                App.post(() -> Notify.show("检查更新失败：API请求过于频繁，请稍后重试"));
                return;
            }
            
            if (response.contains("Not Found Project") || response.contains("Not Found")) {
                App.post(() -> Notify.show("检查更新失败：更新服务暂时不可用"));
                return;
            }
            
            JSONObject release = new JSONObject(response);
            String tagName = release.getString("tag_name");
            String body = release.getString("body");
            JSONArray assets = release.getJSONArray("assets");
            
            // Find the correct APK asset
            String apkName = getApkName();
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if (asset.getString("name").equals(apkName)) {
                    downloadUrl = asset.getString("browser_download_url");
                    break;
                }
            }
            
            if (downloadUrl != null && need(tagName)) {
                download = Download.create(downloadUrl, getFile(), this);
                App.post(() -> show(activity, tagName, body));
            } else if (downloadUrl != null) {
                // 找到APK但不需要更新，提示已是最新版
                App.post(() -> Notify.show("已是最新版本 " + tagName));
                Logger.d("Already latest version: " + tagName);
            } else {
                // 未找到对应的APK文件
                App.post(() -> Notify.show("检查更新完成，未找到适合此设备的安装包"));
                Logger.d("APK not found for this device");
            }
        } catch (Exception e) {
            Logger.e("Update check failed", e);
            e.printStackTrace();
            // 添加用户友好的错误提示
            App.post(() -> {
                String errorMsg = "检查更新失败";
                if (e.getMessage() != null && e.getMessage().contains("rate limit")) {
                    errorMsg = "检查更新失败：请求过于频繁，请稍后重试";
                } else if (e.getMessage() != null && e.getMessage().contains("Not Found")) {
                    errorMsg = "检查更新失败：更新服务暂时不可用";
                } else {
                    errorMsg = "检查更新失败，请稍后重试";
                }
                Notify.show(errorMsg);
            });
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
        Logger.e("Download error: " + msg);
        Notify.show(msg);
        dismiss();
    }

    @Override
    public void success(File file) {
        FileUtil.openFile(file);
        dismiss();
    }
}
