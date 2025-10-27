package com.fongmi.android.tv.bean;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DanmakuBangumi {

    @SerializedName("success")
    private boolean success;
    @SerializedName("bangumi")
    private BangumiData bangumi;

    public boolean isSuccess() {
        return success;
    }

    public List<DanmakuEpisode> getEpisodes() {
        return bangumi != null ? bangumi.episodes : null;
    }

    public static class BangumiData {
        @SerializedName("episodes")
        private List<DanmakuEpisode> episodes;
    }
}
