package com.fongmi.android.tv.api;

import com.fongmi.android.tv.bean.DanmakuAnime;
import com.fongmi.android.tv.bean.DanmakuBangumi;
import com.fongmi.android.tv.bean.DanmakuSearchResult;
import com.github.catvod.net.OkHttp;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import okhttp3.Response;

public class DanmakuApi {

    private static final String BASE_URL = "https://danmu.mangzhexuexi.com/mangzhexuexi/api/v2";
    private static final Gson gson = new Gson();

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public static void searchAnime(String keyword, Callback<List<DanmakuAnime>> callback) {
        new Thread(() -> {
            try {
                String url = BASE_URL + "/search/anime?keyword=" + java.net.URLEncoder.encode(keyword, "UTF-8");
                Response response = OkHttp.newCall(url).execute();
                String json = response.body().string();
                DanmakuSearchResult result = gson.fromJson(json, DanmakuSearchResult.class);

                if (result.isSuccess() && result.getAnimes() != null) {
                    callback.onSuccess(result.getAnimes());
                } else {
                    callback.onSuccess(Collections.emptyList());
                }
            } catch (IOException e) {
                callback.onError("搜索失败: " + e.getMessage());
            }
        }).start();
    }

    public static void getBangumiEpisodes(int animeId, Callback<List<com.fongmi.android.tv.bean.DanmakuEpisode>> callback) {
        new Thread(() -> {
            try {
                String url = BASE_URL + "/bangumi/" + animeId;
                Response response = OkHttp.newCall(url).execute();
                String json = response.body().string();
                DanmakuBangumi result = gson.fromJson(json, DanmakuBangumi.class);

                if (result.isSuccess() && result.getEpisodes() != null) {
                    callback.onSuccess(result.getEpisodes());
                } else {
                    callback.onSuccess(Collections.emptyList());
                }
            } catch (IOException e) {
                callback.onError("获取剧集失败: " + e.getMessage());
            }
        }).start();
    }

    public static String getDanmakuUrl(int episodeId) {
        return BASE_URL + "/comment/" + episodeId + "?format=xml";
    }
}
