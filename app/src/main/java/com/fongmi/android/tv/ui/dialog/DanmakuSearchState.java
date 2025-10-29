package com.fongmi.android.tv.ui.dialog;

import com.fongmi.android.tv.bean.DanmakuAnime;
import com.fongmi.android.tv.bean.DanmakuEpisode;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 弹幕搜索状态管理类
 * 用于保存搜索关键词、搜索结果和选中的番剧/集数
 * 确保切换集数时不需要重新搜索
 */
public class DanmakuSearchState {

    private static final String TAG = DanmakuSearchState.class.getSimpleName();
    private static DanmakuSearchState instance;

    private String lastKeyword = "";
    private List<DanmakuAnime> searchResults = new ArrayList<>();
    private DanmakuAnime selectedAnime;
    private List<DanmakuEpisode> episodes = new ArrayList<>();
    private int selectedAnimePosition = -1;

    private DanmakuSearchState() {}

    public static DanmakuSearchState getInstance() {
        if (instance == null) {
            instance = new DanmakuSearchState();
        }
        return instance;
    }

    public String getLastKeyword() {
        return lastKeyword;
    }

    public void setLastKeyword(String keyword) {
        this.lastKeyword = keyword;
    }

    public List<DanmakuAnime> getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(List<DanmakuAnime> results) {
        this.searchResults = results != null ? results : new ArrayList<>();
    }

    public DanmakuAnime getSelectedAnime() {
        return selectedAnime;
    }

    public void setSelectedAnime(DanmakuAnime anime, int position) {
        this.selectedAnime = anime;
        this.selectedAnimePosition = position;
    }

    public int getSelectedAnimePosition() {
        return selectedAnimePosition;
    }

    public List<DanmakuEpisode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<DanmakuEpisode> episodes) {
        this.episodes = episodes != null ? episodes : new ArrayList<>();
    }

    public boolean hasSearchResults() {
        return searchResults != null && !searchResults.isEmpty();
    }

    public boolean hasEpisodes() {
        return episodes != null && !episodes.isEmpty();
    }

    /**
     * 检查当前关键词是否匹配指定的标题
     * 用于判断是否需要清理状态
     */
    public boolean isKeywordMatchTitle(String title) {
        if (lastKeyword.isEmpty() || title == null || title.isEmpty()) {
            return false;
        }
        // 清理标题中的集数信息
        String cleanTitle = title.replaceAll("第\\d+集", "")
                                 .replaceAll("\\d+集", "")
                                 .replaceAll("EP\\d+", "")
                                 .replaceAll("\\[.*?\\]", "")
                                 .replaceAll("\\(.*?\\)", "")
                                 .trim();
        return cleanTitle.contains(lastKeyword) || lastKeyword.contains(cleanTitle);
    }

    public void clear() {
        Logger.t(TAG).d("清理弹幕搜索状态 - keyword: " + lastKeyword + ", searchResults: " + searchResults.size() + ", episodes: " + episodes.size());
        lastKeyword = "";
        searchResults.clear();
        selectedAnime = null;
        episodes.clear();
        selectedAnimePosition = -1;
        Logger.t(TAG).d("状态已清理");
    }

    public void clearEpisodes() {
        episodes.clear();
    }
}
