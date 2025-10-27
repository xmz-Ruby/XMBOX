package com.fongmi.android.tv.bean;

import com.google.gson.annotations.SerializedName;

public class DanmakuAnime {

    @SerializedName("animeId")
    private int animeId;
    @SerializedName("bangumiId")
    private String bangumiId;
    @SerializedName("animeTitle")
    private String animeTitle;
    @SerializedName("type")
    private String type;
    @SerializedName("typeDescription")
    private String typeDescription;
    @SerializedName("imageUrl")
    private String imageUrl;
    @SerializedName("episodeCount")
    private int episodeCount;

    public int getAnimeId() {
        return animeId;
    }

    public String getAnimeTitle() {
        return animeTitle;
    }

    public String getType() {
        return type;
    }

    public String getDisplayTitle() {
        return type + " - " + animeTitle;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }
}
