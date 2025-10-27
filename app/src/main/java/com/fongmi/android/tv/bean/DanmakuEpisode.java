package com.fongmi.android.tv.bean;

import com.google.gson.annotations.SerializedName;

public class DanmakuEpisode {

    @SerializedName("episodeId")
    private int episodeId;
    @SerializedName("episodeTitle")
    private String episodeTitle;
    @SerializedName("episodeNumber")
    private String episodeNumber;

    public int getEpisodeId() {
        return episodeId;
    }

    public String getEpisodeTitle() {
        return episodeTitle;
    }

    public String getEpisodeNumber() {
        return episodeNumber;
    }

    public String getDisplayTitle() {
        return "第" + episodeNumber + "集 - " + episodeTitle;
    }
}
