package com.github.catvod.crawler;

import android.text.TextUtils;
import android.util.Log;

import com.orhanobut.logger.Logger;

public class SpiderDebug {

    private static final String TAG = "SpiderDebug";
    private static Object logMonitor;

    static {
        try {
            Class<?> clazz = Class.forName("com.fongmi.android.tv.utils.LogMonitor");
            logMonitor = clazz.getMethod("get").invoke(null);
        } catch (Exception e) {
            logMonitor = null;
        }
    }

    private static void addToMonitor(String level, String msg) {
        if (logMonitor != null) {
            try {
                logMonitor.getClass().getMethod("addLog", String.class, String.class, String.class)
                    .invoke(logMonitor, level, TAG, msg);
            } catch (Exception ignored) {
            }
        }
    }

    public static void log(Throwable th) {
        if (th != null) {
            String stackTrace = Log.getStackTraceString(th);
            Logger.t(TAG).e(th, th.getMessage());
            addToMonitor("ERROR", th.getMessage() + "\n" + stackTrace);
        }
    }

    public static void log(String msg) {
        if (!TextUtils.isEmpty(msg)) {
            Logger.t(TAG).d(msg);
            addToMonitor("DEBUG", msg);
        }
    }
}
