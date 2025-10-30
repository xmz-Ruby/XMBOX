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
    private boolean hasAutoSearched = false; // 标记是否已经自动搜索过

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

    public boolean hasAutoSearched() {
        return hasAutoSearched;
    }

    public void setAutoSearched(boolean searched) {
        this.hasAutoSearched = searched;
    }

    public void clear() {
        Logger.t(TAG).d("清理弹幕搜索状态 - keyword: " + lastKeyword + ", searchResults: " + searchResults.size() + ", episodes: " + episodes.size());
        lastKeyword = "";
        searchResults.clear();
        selectedAnime = null;
        episodes.clear();
        selectedAnimePosition = -1;
        hasAutoSearched = false;
        Logger.t(TAG).d("状态已清理");
    }

    public void clearEpisodes() {
        episodes.clear();
    }

    /**
     * 根据集数查找对应的弹幕剧集
     * @param episodeIndex 集数索引（从1开始）
     * @return 找到的弹幕剧集，如果没找到返回null
     */
    public DanmakuEpisode findEpisodeByIndex(int episodeIndex) {
        if (!hasEpisodes() || episodeIndex <= 0) {
            return null;
        }

        Logger.t(TAG).d("查找弹幕剧集 - 集数索引: " + episodeIndex + ", 弹幕剧集总数: " + episodes.size());

        // 方法1: 使用 episodeNumber 字段直接匹配
        for (DanmakuEpisode episode : episodes) {
            try {
                String episodeNumberStr = episode.getEpisodeNumber();
                if (episodeNumberStr != null) {
                    int episodeNum = Integer.parseInt(episodeNumberStr.trim());
                    if (episodeNum == episodeIndex) {
                        Logger.t(TAG).d("✓ 通过episodeNumber匹配成功: " + episode.getDisplayTitle());
                        return episode;
                    }
                }
            } catch (NumberFormatException e) {
                // 继续尝试其他方法
            }
        }

        // 方法2: 从 displayTitle 提取集数进行匹配
        for (DanmakuEpisode episode : episodes) {
            String title = episode.getDisplayTitle();
            if (title != null) {
                int extractedNumber = extractEpisodeNumber(title);
                if (extractedNumber == episodeIndex) {
                    Logger.t(TAG).d("✓ 通过displayTitle匹配成功: " + title);
                    return episode;
                }
            }
        }

        Logger.t(TAG).d("✗ 未找到匹配的弹幕剧集");
        return null;
    }

    /**
     * 从标题中提取集数数字
     */
    private int extractEpisodeNumber(String title) {
        if (title == null || title.isEmpty()) {
            return -1;
        }

        try {
            // 匹配各种集数格式
            java.util.regex.Pattern[] patterns = {
                java.util.regex.Pattern.compile("第(\\d+)集"),
                java.util.regex.Pattern.compile("(\\d+)集"),
                java.util.regex.Pattern.compile("EP(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE),
                java.util.regex.Pattern.compile("(?<!EP)E(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE),
                java.util.regex.Pattern.compile("第(\\d+)话"),
                java.util.regex.Pattern.compile("(\\d+)话")
            };

            for (java.util.regex.Pattern pattern : patterns) {
                java.util.regex.Matcher matcher = pattern.matcher(title);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }

        return -1;
    }
}
