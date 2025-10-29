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

        // 获取最新的日志
        String json = LogMonitor.get().getLogsAsJson(100);

        NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "application/json",
            json
        );
        addCorsHeaders(response);
        return response;
    }

    private void addCorsHeaders(NanoHTTPD.Response response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type");
    }
}
