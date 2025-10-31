package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.DanmakuApi;
import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.bean.DanmakuAnime;
import com.fongmi.android.tv.bean.DanmakuEpisode;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.server.impl.Process;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import fi.iki.elonen.NanoHTTPD;

/**
 * 弹幕投送页面处理器
 * 提供手机端弹幕搜索和投送功能
 */
public class DanmakuPage implements Process {

    private static final Gson gson = new Gson();

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String url) {
        return url.startsWith("/danmaku");
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String url, Map<String, String> files) {
        // 处理 API 请求
        if (url.startsWith("/danmaku/api/")) {
            return handleApiRequest(session, url);
        }

        // 返回弹幕投送页面
        return getDanmakuPage();
    }

    /**
     * 处理 API 请求
     */
    private NanoHTTPD.Response handleApiRequest(NanoHTTPD.IHTTPSession session, String url) {
        Map<String, String> params = session.getParms();

        // 搜索番剧
        if (url.startsWith("/danmaku/api/search")) {
            return handleSearch(params);
        }

        // 获取剧集列表
        if (url.startsWith("/danmaku/api/episodes")) {
            return handleGetEpisodes(params);
        }

        // 投送弹幕
        if (url.startsWith("/danmaku/api/cast")) {
            return handleCast(params);
        }

        return Nano.error("Unknown API endpoint");
    }

    /**
     * 处理搜索请求
     */
    private NanoHTTPD.Response handleSearch(Map<String, String> params) {
        String keyword = params.get("keyword");
        if (keyword == null || keyword.trim().isEmpty()) {
            return jsonResponse(createErrorResponse("关键词不能为空"));
        }

        try {
            keyword = URLDecoder.decode(keyword, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // 忽略解码错误
        }

        // 使用 CountDownLatch 等待异步结果
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<DanmakuAnime>> resultRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        DanmakuApi.searchAnime(keyword, new DanmakuApi.Callback<List<DanmakuAnime>>() {
            @Override
            public void onSuccess(List<DanmakuAnime> data) {
                resultRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                errorRef.set(message);
                latch.countDown();
            }
        });

        try {
            // 等待最多10秒
            if (!latch.await(10, TimeUnit.SECONDS)) {
                return jsonResponse(createErrorResponse("搜索超时"));
            }

            if (errorRef.get() != null) {
                return jsonResponse(createErrorResponse(errorRef.get()));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", resultRef.get());
            return jsonResponse(response);

        } catch (InterruptedException e) {
            return jsonResponse(createErrorResponse("搜索被中断"));
        }
    }

    /**
     * 处理获取剧集列表请求
     */
    private NanoHTTPD.Response handleGetEpisodes(Map<String, String> params) {
        String animeIdStr = params.get("animeId");
        if (animeIdStr == null || animeIdStr.trim().isEmpty()) {
            return jsonResponse(createErrorResponse("番剧ID不能为空"));
        }

        int animeId;
        try {
            animeId = Integer.parseInt(animeIdStr);
        } catch (NumberFormatException e) {
            return jsonResponse(createErrorResponse("番剧ID格式错误"));
        }

        // 使用 CountDownLatch 等待异步结果
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<DanmakuEpisode>> resultRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        DanmakuApi.getBangumiEpisodes(animeId, new DanmakuApi.Callback<List<DanmakuEpisode>>() {
            @Override
            public void onSuccess(List<DanmakuEpisode> data) {
                resultRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                errorRef.set(message);
                latch.countDown();
            }
        });

        try {
            // 等待最多10秒
            if (!latch.await(10, TimeUnit.SECONDS)) {
                return jsonResponse(createErrorResponse("获取剧集超时"));
            }

            if (errorRef.get() != null) {
                return jsonResponse(createErrorResponse(errorRef.get()));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", resultRef.get());
            return jsonResponse(response);

        } catch (InterruptedException e) {
            return jsonResponse(createErrorResponse("获取剧集被中断"));
        }
    }

    /**
     * 处理投送弹幕请求
     */
    private NanoHTTPD.Response handleCast(Map<String, String> params) {
        String episodeIdStr = params.get("episodeId");
        String episodeName = params.get("episodeName");

        if (episodeIdStr == null || episodeIdStr.trim().isEmpty()) {
            return jsonResponse(createErrorResponse("剧集ID不能为空"));
        }

        int episodeId;
        try {
            episodeId = Integer.parseInt(episodeIdStr);
        } catch (NumberFormatException e) {
            return jsonResponse(createErrorResponse("剧集ID格式错误"));
        }

        try {
            if (episodeName != null) {
                episodeName = URLDecoder.decode(episodeName, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            // 忽略解码错误
        }

        // 获取弹幕URL
        String danmakuUrl = DanmakuApi.getDanmakuUrl(episodeId);

        // 创建弹幕对象
        Danmaku danmaku = Danmaku.from(danmakuUrl);
        if (episodeName != null && !episodeName.isEmpty()) {
            danmaku.setName(episodeName);
        }

        // 投送到播放器
        if (Server.get().getPlayer() != null) {
            App.post(() -> {
                Server.get().getPlayer().setDanmaku(danmaku);
                RefreshEvent.danmaku(danmakuUrl);
            });

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "弹幕投送成功");
            return jsonResponse(response);
        } else {
            return jsonResponse(createErrorResponse("播放器未就绪，请先播放视频"));
        }
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    /**
     * 返回 JSON 响应
     */
    private NanoHTTPD.Response jsonResponse(Object data) {
        String json = gson.toJson(data);
        NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "application/json; charset=utf-8",
            json
        );
        addCorsHeaders(response);
        return response;
    }

    /**
     * 添加 CORS 头
     */
    private void addCorsHeaders(NanoHTTPD.Response response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    /**
     * 返回弹幕投送页面 HTML
     */
    private NanoHTTPD.Response getDanmakuPage() {
        String html = "<!DOCTYPE html>\n" +
            "<html lang=\"zh-CN\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
            "    <title>弹幕投送</title>\n" +
            "    <style>\n" +
            "        * {\n" +
            "            margin: 0;\n" +
            "            padding: 0;\n" +
            "            box-sizing: border-box;\n" +
            "        }\n" +
            "        body {\n" +
            "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;\n" +
            "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
            "            min-height: 100vh;\n" +
            "            padding: 20px;\n" +
            "        }\n" +
            "        .container {\n" +
            "            max-width: 600px;\n" +
            "            margin: 0 auto;\n" +
            "        }\n" +
            "        .header {\n" +
            "            text-align: center;\n" +
            "            color: white;\n" +
            "            margin-bottom: 30px;\n" +
            "        }\n" +
            "        .header h1 {\n" +
            "            font-size: 28px;\n" +
            "            margin-bottom: 10px;\n" +
            "        }\n" +
            "        .header p {\n" +
            "            font-size: 14px;\n" +
            "            opacity: 0.9;\n" +
            "        }\n" +
            "        .search-box {\n" +
            "            background: white;\n" +
            "            border-radius: 12px;\n" +
            "            padding: 20px;\n" +
            "            box-shadow: 0 10px 30px rgba(0,0,0,0.2);\n" +
            "            margin-bottom: 20px;\n" +
            "        }\n" +
            "        .search-input-group {\n" +
            "            display: flex;\n" +
            "            gap: 10px;\n" +
            "        }\n" +
            "        .search-input {\n" +
            "            flex: 1;\n" +
            "            padding: 12px 16px;\n" +
            "            border: 2px solid #e0e0e0;\n" +
            "            border-radius: 8px;\n" +
            "            font-size: 16px;\n" +
            "            transition: border-color 0.3s;\n" +
            "        }\n" +
            "        .search-input:focus {\n" +
            "            outline: none;\n" +
            "            border-color: #667eea;\n" +
            "        }\n" +
            "        .search-btn {\n" +
            "            padding: 12px 24px;\n" +
            "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
            "            color: white;\n" +
            "            border: none;\n" +
            "            border-radius: 8px;\n" +
            "            font-size: 16px;\n" +
            "            cursor: pointer;\n" +
            "            transition: transform 0.2s;\n" +
            "        }\n" +
            "        .search-btn:active {\n" +
            "            transform: scale(0.95);\n" +
            "        }\n" +
            "        .results {\n" +
            "            background: white;\n" +
            "            border-radius: 12px;\n" +
            "            padding: 20px;\n" +
            "            box-shadow: 0 10px 30px rgba(0,0,0,0.2);\n" +
            "            display: none;\n" +
            "        }\n" +
            "        .results.show {\n" +
            "            display: block;\n" +
            "        }\n" +
            "        .result-item {\n" +
            "            padding: 15px;\n" +
            "            border-bottom: 1px solid #f0f0f0;\n" +
            "            cursor: pointer;\n" +
            "            transition: background 0.2s;\n" +
            "        }\n" +
            "        .result-item:last-child {\n" +
            "            border-bottom: none;\n" +
            "        }\n" +
            "        .result-item:active {\n" +
            "            background: #f5f5f5;\n" +
            "        }\n" +
            "        .result-title {\n" +
            "            font-size: 16px;\n" +
            "            font-weight: 500;\n" +
            "            color: #333;\n" +
            "            margin-bottom: 5px;\n" +
            "        }\n" +
            "        .result-type {\n" +
            "            font-size: 12px;\n" +
            "            color: #999;\n" +
            "        }\n" +
            "        .loading {\n" +
            "            text-align: center;\n" +
            "            padding: 40px;\n" +
            "            color: #999;\n" +
            "        }\n" +
            "        .empty {\n" +
            "            text-align: center;\n" +
            "            padding: 40px;\n" +
            "            color: #999;\n" +
            "        }\n" +
            "        .back-btn {\n" +
            "            display: inline-block;\n" +
            "            padding: 8px 16px;\n" +
            "            background: #f0f0f0;\n" +
            "            color: #666;\n" +
            "            border-radius: 6px;\n" +
            "            font-size: 14px;\n" +
            "            cursor: pointer;\n" +
            "            margin-bottom: 15px;\n" +
            "        }\n" +
            "        .back-btn:active {\n" +
            "            background: #e0e0e0;\n" +
            "        }\n" +
            "        .toast {\n" +
            "            position: fixed;\n" +
            "            top: 50%;\n" +
            "            left: 50%;\n" +
            "            transform: translate(-50%, -50%);\n" +
            "            background: rgba(0,0,0,0.8);\n" +
            "            color: white;\n" +
            "            padding: 15px 30px;\n" +
            "            border-radius: 8px;\n" +
            "            font-size: 14px;\n" +
            "            z-index: 9999;\n" +
            "            display: none;\n" +
            "        }\n" +
            "        .toast.show {\n" +
            "            display: block;\n" +
            "        }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <div class=\"header\">\n" +
            "            <h1>🎬 弹幕投送</h1>\n" +
            "            <p>搜索并投送弹幕到电视</p>\n" +
            "        </div>\n" +
            "        \n" +
            "        <div class=\"search-box\">\n" +
            "            <div class=\"search-input-group\">\n" +
            "                <input type=\"text\" class=\"search-input\" id=\"searchInput\" placeholder=\"输入剧名搜索...\">\n" +
            "                <button class=\"search-btn\" id=\"searchBtn\">搜索</button>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        \n" +
            "        <div class=\"results\" id=\"results\">\n" +
            "            <div id=\"resultsContent\"></div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    \n" +
            "    <div class=\"toast\" id=\"toast\"></div>\n" +
            "    \n" +
            "    <script>\n" +
            "        let currentAnimeId = null;\n" +
            "        let currentAnimeName = '';\n" +
            "        \n" +
            "        // 搜索按钮点击\n" +
            "        document.getElementById('searchBtn').addEventListener('click', performSearch);\n" +
            "        \n" +
            "        // 回车搜索\n" +
            "        document.getElementById('searchInput').addEventListener('keypress', function(e) {\n" +
            "            if (e.key === 'Enter') {\n" +
            "                performSearch();\n" +
            "            }\n" +
            "        });\n" +
            "        \n" +
            "        // 执行搜索\n" +
            "        function performSearch() {\n" +
            "            const keyword = document.getElementById('searchInput').value.trim();\n" +
            "            if (!keyword) {\n" +
            "                showToast('请输入搜索关键词');\n" +
            "                return;\n" +
            "            }\n" +
            "            \n" +
            "            showLoading();\n" +
            "            \n" +
            "            fetch('/danmaku/api/search?keyword=' + encodeURIComponent(keyword))\n" +
            "                .then(res => res.json())\n" +
            "                .then(data => {\n" +
            "                    if (data.success) {\n" +
            "                        showAnimeList(data.data);\n" +
            "                    } else {\n" +
            "                        showEmpty(data.message || '搜索失败');\n" +
            "                    }\n" +
            "                })\n" +
            "                .catch(err => {\n" +
            "                    showEmpty('网络错误: ' + err.message);\n" +
            "                });\n" +
            "        }\n" +
            "        \n" +
            "        // 显示番剧列表\n" +
            "        function showAnimeList(animes) {\n" +
            "            if (!animes || animes.length === 0) {\n" +
            "                showEmpty('未找到相关番剧');\n" +
            "                return;\n" +
            "            }\n" +
            "            \n" +
            "            let html = '';\n" +
            "            animes.forEach(anime => {\n" +
            "                html += `\n" +
            "                    <div class=\"result-item\" onclick=\"selectAnime(${anime.animeId}, '${escapeHtml(anime.animeTitle)}')\">\n" +
            "                        <div class=\"result-title\">${escapeHtml(anime.animeTitle)}</div>\n" +
            "                        <div class=\"result-type\">${escapeHtml(anime.typeDescription || '')}</div>\n" +
            "                    </div>\n" +
            "                `;\n" +
            "            });\n" +
            "            \n" +
            "            document.getElementById('resultsContent').innerHTML = html;\n" +
            "            document.getElementById('results').classList.add('show');\n" +
            "        }\n" +
            "        \n" +
            "        // 选择番剧\n" +
            "        function selectAnime(animeId, animeName) {\n" +
            "            currentAnimeId = animeId;\n" +
            "            currentAnimeName = animeName;\n" +
            "            \n" +
            "            showLoading();\n" +
            "            \n" +
            "            fetch('/danmaku/api/episodes?animeId=' + animeId)\n" +
            "                .then(res => res.json())\n" +
            "                .then(data => {\n" +
            "                    if (data.success) {\n" +
            "                        showEpisodeList(data.data);\n" +
            "                    } else {\n" +
            "                        showEmpty(data.message || '获取剧集失败');\n" +
            "                    }\n" +
            "                })\n" +
            "                .catch(err => {\n" +
            "                    showEmpty('网络错误: ' + err.message);\n" +
            "                });\n" +
            "        }\n" +
            "        \n" +
            "        // 显示剧集列表\n" +
            "        function showEpisodeList(episodes) {\n" +
            "            if (!episodes || episodes.length === 0) {\n" +
            "                showEmpty('该番剧暂无剧集');\n" +
            "                return;\n" +
            "            }\n" +
            "            \n" +
            "            let html = '<div class=\"back-btn\" onclick=\"performSearch()\">← 返回搜索结果</div>';\n" +
            "            episodes.forEach(episode => {\n" +
            "                const title = episode.episodeTitle || '第' + episode.episodeNumber + '集';\n" +
            "                html += `\n" +
            "                    <div class=\"result-item\" onclick=\"castEpisode(${episode.episodeId}, '${escapeHtml(title)}')\">\n" +
            "                        <div class=\"result-title\">${escapeHtml(title)}</div>\n" +
            "                    </div>\n" +
            "                `;\n" +
            "            });\n" +
            "            \n" +
            "            document.getElementById('resultsContent').innerHTML = html;\n" +
            "            document.getElementById('results').classList.add('show');\n" +
            "        }\n" +
            "        \n" +
            "        // 投送剧集\n" +
            "        function castEpisode(episodeId, episodeName) {\n" +
            "            showToast('正在投送...');\n" +
            "            \n" +
            "            fetch('/danmaku/api/cast?episodeId=' + episodeId + '&episodeName=' + encodeURIComponent(episodeName))\n" +
            "                .then(res => res.json())\n" +
            "                .then(data => {\n" +
            "                    if (data.success) {\n" +
            "                        showToast('✓ ' + (data.message || '投送成功'));\n" +
            "                    } else {\n" +
            "                        showToast('✗ ' + (data.message || '投送失败'));\n" +
            "                    }\n" +
            "                })\n" +
            "                .catch(err => {\n" +
            "                    showToast('✗ 网络错误');\n" +
            "                });\n" +
            "        }\n" +
            "        \n" +
            "        // 显示加载中\n" +
            "        function showLoading() {\n" +
            "            document.getElementById('resultsContent').innerHTML = '<div class=\"loading\">加载中...</div>';\n" +
            "            document.getElementById('results').classList.add('show');\n" +
            "        }\n" +
            "        \n" +
            "        // 显示空状态\n" +
            "        function showEmpty(message) {\n" +
            "            document.getElementById('resultsContent').innerHTML = '<div class=\"empty\">' + escapeHtml(message) + '</div>';\n" +
            "            document.getElementById('results').classList.add('show');\n" +
            "        }\n" +
            "        \n" +
            "        // 显示提示\n" +
            "        function showToast(message) {\n" +
            "            const toast = document.getElementById('toast');\n" +
            "            toast.textContent = message;\n" +
            "            toast.classList.add('show');\n" +
            "            setTimeout(() => {\n" +
            "                toast.classList.remove('show');\n" +
            "            }, 2000);\n" +
            "        }\n" +
            "        \n" +
            "        // HTML 转义\n" +
            "        function escapeHtml(text) {\n" +
            "            const div = document.createElement('div');\n" +
            "            div.textContent = text;\n" +
            "            return div.innerHTML;\n" +
            "        }\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";

        NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "text/html; charset=utf-8",
            html
        );
        addCorsHeaders(response);
        return response;
    }
}
