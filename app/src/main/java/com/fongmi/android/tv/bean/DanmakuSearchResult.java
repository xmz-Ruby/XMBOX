package com.fongmi.android.tv.bean;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DanmakuSearchResult {

    @SerializedName("success")
    private boolean success;
    @SerializedName("animes")
    private List<DanmakuAnime> animes;

    public boolean isSuccess() {
        return success;
    }

    public List<DanmakuAnime> getAnimes() {
        return animes;
    }
}
