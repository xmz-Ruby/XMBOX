package com.fongmi.android.tv.utils;

import android.util.Log;

import com.fongmi.android.tv.Setting;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogMonitor {

    private static final int MAX_LOGS = 1000; // 最多保存1000条日志
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    private static class Loader {
        static volatile LogMonitor INSTANCE = new LogMonitor();
    }

    public static LogMonitor get() {
        return Loader.INSTANCE;
    }

    private final CopyOnWriteArrayList<LogEntry> logs;

    private LogMonitor() {
        logs = new CopyOnWriteArrayList<>();
    }

    public static class LogEntry {
        public final String timestamp;
        public final String level;
        public final String tag;
        public final String message;
        public final String threadName;

        public LogEntry(String level, String tag, String message) {
            this.timestamp = DATE_FORMAT.format(new Date());
            this.level = level;
            this.tag = tag;
            this.message = message;
            this.threadName = Thread.currentThread().getName();
        }

        public String toJson() {
            return String.format("{\"timestamp\":\"%s\",\"level\":\"%s\",\"tag\":\"%s\",\"message\":\"%s\",\"thread\":\"%s\"}",
                escapeJson(timestamp),
                escapeJson(level),
                escapeJson(tag),
                escapeJson(message),
                escapeJson(threadName));
        }

        private String escapeJson(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
        }
    }

    public void addLog(String level, String tag, String message) {
        if (!Setting.isLogMonitorEnabled()) return;

        // 限制日志数量
        if (logs.size() >= MAX_LOGS) {
            logs.remove(0);
        }

        logs.add(new LogEntry(level, tag, message));
    }

    public void d(String tag, String message) {
        addLog("DEBUG", tag, message);
        Log.d(tag, message);
    }

    public void i(String tag, String message) {
        addLog("INFO", tag, message);
        Log.i(tag, message);
    }

    public void w(String tag, String message) {
        addLog("WARN", tag, message);
        Log.w(tag, message);
    }

    public void w(String tag, String message, Throwable tr) {
        addLog("WARN", tag, message + "\n" + Log.getStackTraceString(tr));
        Log.w(tag, message, tr);
    }

    public void e(String tag, String message) {
        addLog("ERROR", tag, message);
        Log.e(tag, message);
    }

    public void e(String tag, String message, Throwable tr) {
        addLog("ERROR", tag, message + "\n" + Log.getStackTraceString(tr));
        Log.e(tag, message, tr);
    }

    public void v(String tag, String message) {
        addLog("VERBOSE", tag, message);
        Log.v(tag, message);
    }

    public List<LogEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    public List<LogEntry> getLogs(int limit) {
        int size = logs.size();
        int start = Math.max(0, size - limit);
        return new ArrayList<>(logs.subList(start, size));
    }

    public List<LogEntry> getLogsSince(int index) {
        int size = logs.size();
        if (index >= size) return new ArrayList<>();
        return new ArrayList<>(logs.subList(index, size));
    }

    public int getLogCount() {
        return logs.size();
    }

    public void clear() {
        logs.clear();
    }

    public String getLogsAsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        List<LogEntry> logList = getLogs();
        for (int i = 0; i < logList.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(logList.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }

    public String getLogsAsJson(int limit) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        List<LogEntry> logList = getLogs(limit);
        for (int i = 0; i < logList.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(logList.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }
}
