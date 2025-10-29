package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.impl.Process;
import com.fongmi.android.tv.utils.LogMonitor;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class LogMonitorProcess implements Process {

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String url) {
        return url.startsWith("/logs");
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String url, Map<String, String> files) {
        Map<String, String> params = session.getParms();

        if (url.equals("/logs/api")) {
            return handleApi(params);
        } else if (url.equals("/logs/clear")) {
            return handleClear();
        } else if (url.equals("/logs/stream")) {
            return handleStream(params);
        }

        return Nano.error(NanoHTTPD.Response.Status.NOT_FOUND, "Not found");
    }

    private NanoHTTPD.Response handleApi(Map<String, String> params) {
        String limitStr = params.get("limit");
        String json;

        if (limitStr != null) {
            try {
                int limit = Integer.parseInt(limitStr);
                json = LogMonitor.get().getLogsAsJson(limit);
            } catch (NumberFormatException e) {
                json = LogMonitor.get().getLogsAsJson();
            }
        } else {
            json = LogMonitor.get().getLogsAsJson();
        }

        NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "application/json",
            json
        );
        addCorsHeaders(response);
        return response;
    }

    private NanoHTTPD.Response handleClear() {
        LogMonitor.get().clear();
        NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "application/json",
            "{\"status\":\"ok\",\"message\":\"Logs cleared\"}"
        );
        addCorsHeaders(response);
        return response;
    }

    private NanoHTTPD.Response handleStream(Map<String, String> params) {
        String sinceStr = params.get("since");
        int since = 0;

        if (sinceStr != null) {
            try {
                since = Integer.parseInt(sinceStr);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        // 获取指定索引之后的新日志
        StringBuilder sb = new StringBuilder();
        sb.append("{\"count\":").append(LogMonitor.get().getLogCount());
        sb.append(",\"logs\":");

        if (since > 0) {
            // 只返回新增的日志
            sb.append(getLogsSinceAsJson(since));
        } else {
            // 返回最近100条
            sb.append(LogMonitor.get().getLogsAsJson(100));
        }

        sb.append("}");

        NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "application/json",
            sb.toString()
        );
        addCorsHeaders(response);
        return response;
    }

    private String getLogsSinceAsJson(int index) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        java.util.List<LogMonitor.LogEntry> logList = LogMonitor.get().getLogsSince(index);
        for (int i = 0; i < logList.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(logList.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }

    private void addCorsHeaders(NanoHTTPD.Response response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type");
    }
}
