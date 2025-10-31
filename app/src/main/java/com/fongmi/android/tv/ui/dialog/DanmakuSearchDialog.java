package com.fongmi.android.tv.ui.dialog;

import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.DanmakuApi;
import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.bean.DanmakuAnime;
import com.fongmi.android.tv.bean.DanmakuEpisode;
import com.fongmi.android.tv.player.Players;
import com.orhanobut.logger.Logger;

import java.util.List;

/**
 * 优雅的弹幕搜索对话框
 * 特点：
 * 1. 保留搜索状态，切换集数不需要重新搜索
 * 2. 优雅的加载动画
 * 3. 流畅的交互体验
 * 4. 搜索结果和剧集列表在同一界面切换
 */
public class DanmakuSearchDialog extends BaseDialog {

    private static final String TAG = DanmakuSearchDialog.class.getSimpleName();

    private EditText searchInput;
    private ImageView searchButton;
    private ImageView closeButton;
    private FrameLayout loadingContainer;
    private TextView loadingText;
    private LinearLayout emptyContainer;
    private TextView emptyText;
    private FrameLayout contentContainer;
    private RecyclerView searchResults;
    private LinearLayout episodeContainer;
    private RecyclerView episodeResults;
    private TextView reverseButton;
    private TextView jumpButton;
    private LinearLayout animeListContainer;
    private ImageView animeToggleButton;
    private ImageView animeExpandButton;

    private DanmakuAnimeAdapter animeAdapter;
    private DanmakuEpisodeAdapter episodeAdapter;
    private DanmakuSearchState searchState;
    private Players player;
    private Handler mainHandler;
    private List<DanmakuEpisode> currentEpisodes;
    private boolean isReversed = false;
    private int highlightedEpisodePosition = -1; // 当前高亮的剧集位置
    private boolean isShowingEpisodeList = false; // 当前是否展示剧集列表

    public static DanmakuSearchDialog create() {
        return new DanmakuSearchDialog();
    }

    public DanmakuSearchDialog player(Players player) {
        this.player = player;
        return this;
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        View view = inflater.inflate(R.layout.dialog_danmaku_search, container, false);
        initViews(view);
        return new ViewBinding() {
            @NonNull
            @Override
            public View getRoot() {
                return view;
            }
        };
    }

    private void initViews(View view) {
        searchInput = view.findViewById(R.id.search_input);
        searchButton = view.findViewById(R.id.search_button);
        closeButton = view.findViewById(R.id.close_button);
        loadingContainer = view.findViewById(R.id.loading_container);
        loadingText = view.findViewById(R.id.loading_text);
        emptyContainer = view.findViewById(R.id.empty_container);
        emptyText = view.findViewById(R.id.empty_text);
        contentContainer = view.findViewById(R.id.content_container);
        searchResults = view.findViewById(R.id.search_results);
        episodeContainer = view.findViewById(R.id.episode_container);
        episodeResults = view.findViewById(R.id.episode_results);
        reverseButton = view.findViewById(R.id.reverse_button);
        jumpButton = view.findViewById(R.id.jump_button);
        animeListContainer = view.findViewById(R.id.anime_list_container);
        animeToggleButton = view.findViewById(R.id.anime_toggle_button);
        animeExpandButton = view.findViewById(R.id.anime_expand_button);
    }

    @Override
    protected void initView() {
        mainHandler = new Handler(Looper.getMainLooper());
        searchState = DanmakuSearchState.getInstance();

        // 设置适配器
        animeAdapter = new DanmakuAnimeAdapter(this::onAnimeClick);
        searchResults.setAdapter(animeAdapter);

        episodeAdapter = new DanmakuEpisodeAdapter(this::onEpisodeClick);
        episodeResults.setAdapter(episodeAdapter);
        showAnimeListOnly();

        // 恢复搜索状态
        restoreSearchState();
    }

    @Override
    protected void initEvent() {
        searchButton.setOnClickListener(v -> performSearch());
        closeButton.setOnClickListener(v -> dismiss());
        reverseButton.setOnClickListener(v -> reverseEpisodes());
        jumpButton.setOnClickListener(v -> showJumpDialog());
        animeToggleButton.setOnClickListener(v -> toggleAnimeList());
        animeExpandButton.setOnClickListener(v -> toggleAnimeList());

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                performSearch();
                return true;
            }
            return false;
        });
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                searchInput.post(() -> {
                    showKeyboard(searchInput);
                    searchInput.setSelection(searchInput.getText().length());
                });
            } else {
                hideKeyboard();
            }
        });
        searchInput.setOnClickListener(v -> {
            showKeyboard(searchInput);
            searchInput.setSelection(searchInput.getText().length());
        });
        searchInput.setOnKeyListener((v, keyCode, event) -> {
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    keyCode == KeyEvent.KEYCODE_MENU ||
                    keyCode == KeyEvent.KEYCODE_SEARCH) {
                    showKeyboard(searchInput);
                    return true;
                }
            }
            return false;
        });
        searchResults.setOnKeyListener((v, keyCode, event) -> handleSearchResultsKey(keyCode, event));
        episodeResults.setOnKeyListener((v, keyCode, event) -> handleEpisodeResultsKey(keyCode, event));
    }

    private void restoreSearchState() {
        Logger.t(TAG).d("恢复搜索状态 - hasKeyword: " + !searchState.getLastKeyword().isEmpty() +
                       ", hasSearchResults: " + searchState.hasSearchResults() +
                       ", hasEpisodes: " + searchState.hasEpisodes() +
                       ", hasAutoSearched: " + searchState.hasAutoSearched());

        // 首先检查是否切换了影视剧（最重要的检查）
        String currentTitle = player != null ? player.getTitle() : "";
        if (searchState.isVideoChanged(currentTitle)) {
            Logger.t(TAG).d("检测到影视剧切换，清理所有旧状态");
            searchState.clear();
        }

        // 恢复搜索关键词
        if (!searchState.getLastKeyword().isEmpty()) {
            Logger.t(TAG).d("恢复关键词: " + searchState.getLastKeyword());
            searchInput.setText(searchState.getLastKeyword());
            searchInput.setSelection(searchInput.getText().length());
        } else {
            // 尝试从播放器获取默认关键词
            String defaultKeyword = getDefaultSearchKeyword();
            Logger.t(TAG).d("使用默认关键词: " + defaultKeyword);
            if (!defaultKeyword.isEmpty()) {
                searchInput.setText(defaultKeyword);
                searchInput.setSelection(searchInput.getText().length());
            }
        }

        // 恢复搜索结果
        if (searchState.hasSearchResults() && searchState.hasEpisodes()) {
            Logger.t(TAG).d("显示番剧列表和剧集列表");
            showBothListsView(searchState.getSearchResults(), searchState.getEpisodes());
        } else if (searchState.hasSearchResults()) {
            Logger.t(TAG).d("只显示番剧列表");
            showSearchResultsView(searchState.getSearchResults());
        } else {
            // 首次打开弹幕对话框，自动触发搜索
            if (!searchState.hasAutoSearched() && !searchInput.getText().toString().trim().isEmpty()) {
                Logger.t(TAG).d("首次打开弹幕对话框，自动触发搜索");
                searchState.setAutoSearched(true);
                // 延迟执行搜索，确保UI已经初始化完成
                mainHandler.postDelayed(this::performAutoSearch, 300);
            } else {
                Logger.t(TAG).d("显示空白状态");
                showEmptyView("输入剧名开始搜索");
            }
        }
    }

    private String getDefaultSearchKeyword() {
        try {
            if (player != null) {
                String title = player.getTitle();
                if (title != null && !title.isEmpty()) {
                    return title.replaceAll("第\\d+集", "")
                            .replaceAll("\\d+集", "")
                            .replaceAll("EP\\d+", "")
                            .replaceAll("\\[.*?\\]", "")
                            .replaceAll("\\(.*?\\)", "")
                            .trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 自动搜索（首次打开弹幕时触发）
     */
    private void performAutoSearch() {
        String keyword = searchInput.getText().toString().trim();
        if (keyword.isEmpty()) {
            Logger.t(TAG).d("自动搜索：关键词为空，跳过");
            showEmptyView("输入剧名开始搜索");
            return;
        }

        Logger.t(TAG).d("自动搜索：关键词 = " + keyword);

        // 保存关键词
        searchState.setLastKeyword(keyword);

        // 显示加载动画
        showLoadingView("正在自动搜索弹幕...");

        // 执行搜索
        DanmakuApi.searchAnime(keyword, new DanmakuApi.Callback<List<DanmakuAnime>>() {
            @Override
            public void onSuccess(List<DanmakuAnime> animes) {
                mainHandler.post(() -> {
                    if (animes.isEmpty()) {
                        Logger.t(TAG).d("自动搜索：未找到结果");
                        searchState.setSearchResults(null);
                        showEmptyView("未找到相关弹幕");
                    } else {
                        Logger.t(TAG).d("自动搜索：找到 " + animes.size() + " 个结果");
                        searchState.setSearchResults(animes);
                        // 自动选择第一个番剧
                        if (animes.size() > 0) {
                            Logger.t(TAG).d("自动选择第一个番剧：" + animes.get(0).getDisplayTitle());
                            autoSelectFirstAnime(animes.get(0));
                        } else {
                            showSearchResultsView(animes);
                        }
                    }
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    Logger.t(TAG).d("自动搜索失败：" + message);
                    showEmptyView("搜索失败: " + message);
                });
            }
        });
    }

    private void performSearch() {
        String keyword = searchInput.getText().toString().trim();
        if (keyword.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "请输入搜索关键词", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // 隐藏键盘
        hideKeyboard();

        // 保存关键词
        searchState.setLastKeyword(keyword);

        // 显示加载动画
        showLoadingView("正在搜索弹幕...");

        // 执行搜索
        DanmakuApi.searchAnime(keyword, new DanmakuApi.Callback<List<DanmakuAnime>>() {
            @Override
            public void onSuccess(List<DanmakuAnime> animes) {
                mainHandler.post(() -> {
                    if (animes.isEmpty()) {
                        searchState.setSearchResults(null);
                        showEmptyView("未找到相关弹幕");
                    } else {
                        searchState.setSearchResults(animes);
                        showSearchResultsView(animes);
                    }
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    showEmptyView("搜索失败: " + message);
                    android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 自动选择第一个番剧（用于首次自动搜索）
     */
    private void autoSelectFirstAnime(DanmakuAnime anime) {
        Logger.t(TAG).d("=== autoSelectFirstAnime 被调用 ===");
        Logger.t(TAG).d("番剧：" + anime.getDisplayTitle());

        searchState.setSelectedAnime(anime, 0);
        showLoadingView("正在加载剧集...");

        DanmakuApi.getBangumiEpisodes(anime.getAnimeId(), new DanmakuApi.Callback<List<DanmakuEpisode>>() {
            @Override
            public void onSuccess(List<DanmakuEpisode> episodes) {
                mainHandler.post(() -> {
                    if (episodes.isEmpty()) {
                        Logger.t(TAG).d("该番剧暂无剧集");
                        showEmptyView("该番剧暂无弹幕");
                    } else {
                        Logger.t(TAG).d("加载到 " + episodes.size() + " 集");
                        searchState.setEpisodes(episodes);
                        // 同时显示番剧列表和剧集列表
                        showBothListsView(searchState.getSearchResults(), episodes);
                    }
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    Logger.t(TAG).d("加载剧集失败：" + message);
                    showEmptyView("加载失败: " + message);
                });
            }
        });
    }

    private void onAnimeClick(DanmakuAnime anime, int position) {
        Logger.t(TAG).d("用户点击番剧：" + anime.getDisplayTitle() + ", 位置：" + position);
        searchState.setSelectedAnime(anime, position);
        showLoadingView("正在加载剧集...");

        DanmakuApi.getBangumiEpisodes(anime.getAnimeId(), new DanmakuApi.Callback<List<DanmakuEpisode>>() {
            @Override
            public void onSuccess(List<DanmakuEpisode> episodes) {
                mainHandler.post(() -> {
                    if (episodes.isEmpty()) {
                        showEmptyView("该番剧暂无弹幕");
                    } else {
                        searchState.setEpisodes(episodes);
                        // 同时显示番剧列表和剧集列表
                        showBothListsView(searchState.getSearchResults(), episodes);
                    }
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    showEmptyView("加载失败: " + message);
                    android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void onEpisodeClick(DanmakuEpisode episode) {
        String url = DanmakuApi.getDanmakuUrl(episode.getEpisodeId());
        Danmaku danmaku = Danmaku.from(url);
        danmaku.setName(episode.getDisplayTitle());
        player.setDanmaku(danmaku);
        android.widget.Toast.makeText(getContext(), "弹幕加载成功", android.widget.Toast.LENGTH_SHORT).show();
        dismiss();
    }

    /**
     * 智能匹配当前播放的集数
     * 1. 优先使用DanmakuEpisode的episodeNumber字段直接匹配
     * 2. 其次按剧集名称完全匹配
     * 3. 再按剧集名称模糊匹配
     * 4. 最后按集数数字匹配
     */
    private int findCurrentEpisodePosition(List<DanmakuEpisode> episodes) {
        Logger.t(TAG).d("=== 开始查找当前集数位置 ===");

        if (episodes == null || episodes.isEmpty()) {
            Logger.t(TAG).d("剧集列表为空");
            return -1;
        }

        if (player == null) {
            Logger.t(TAG).d("播放器为空");
            return -1;
        }

        try {
            String currentTitle = player.getTitle();
            String currentArtist = player.getArtist();
            Logger.t(TAG).d("当前播放标题: " + currentTitle);
            Logger.t(TAG).d("当前播放Artist: " + currentArtist);

            // 从 artist 中提取剧集名称（格式：正在播放：第 1 季|涌江龙宫）
            String episodeName = null;
            if (currentArtist != null && currentArtist.contains("|")) {
                episodeName = currentArtist.substring(currentArtist.lastIndexOf("|") + 1).trim();
                Logger.t(TAG).d( "从Artist提取的剧集名称: " + episodeName);
            }

            // 优先从 artist 字段提取集数
            int currentEpisodeNumber = -1;
            if (currentArtist != null && !currentArtist.isEmpty()) {
                currentEpisodeNumber = extractEpisodeNumber(currentArtist);
                Logger.t(TAG).d( "从Artist提取的集数: " + currentEpisodeNumber);
            }

            // 如果 artist 中没有集数，尝试从 title 提取
            if (currentEpisodeNumber <= 0 && currentTitle != null && !currentTitle.isEmpty()) {
                currentEpisodeNumber = extractEpisodeNumber(currentTitle);
                Logger.t(TAG).d( "从Title提取的集数: " + currentEpisodeNumber);
            }

            Logger.t(TAG).d( "最终提取的集数: " + currentEpisodeNumber);

            // 打印所有剧集信息
            Logger.t(TAG).d( "剧集列表 (共" + episodes.size() + "集):");
            for (int i = 0; i < Math.min(episodes.size(), 5); i++) {
                Logger.t(TAG).d( "  [" + i + "] episodeNumber=" + episodes.get(i).getEpisodeNumber() +
                    ", displayTitle=" + episodes.get(i).getDisplayTitle());
            }

            // 方法0: 如果有剧集名称，先用剧集名称匹配（最准确）
            if (episodeName != null && !episodeName.isEmpty()) {
                Logger.t(TAG).d( "尝试方法0: 剧集名称匹配");
                for (int i = 0; i < episodes.size(); i++) {
                    String displayTitle = episodes.get(i).getDisplayTitle();
                    if (displayTitle != null && displayTitle.contains(episodeName)) {
                        Logger.t(TAG).d( "✓ 方法0匹配成功! 位置: " + i + ", 匹配: " + displayTitle);
                        return i;
                    }
                }
                Logger.t(TAG).d( "✗ 方法0未匹配");
            }

            // 方法1: 使用DanmakuEpisode的episodeNumber字段直接匹配
            if (currentEpisodeNumber > 0) {
                Logger.t(TAG).d( "尝试方法1: episodeNumber字段匹配");
                for (int i = 0; i < episodes.size(); i++) {
                    try {
                        String episodeNumberStr = episodes.get(i).getEpisodeNumber();
                        if (episodeNumberStr != null) {
                            int episodeNum = Integer.parseInt(episodeNumberStr.trim());
                            if (episodeNum == currentEpisodeNumber) {
                                Logger.t(TAG).d( "✓ 方法1匹配成功! 位置: " + i);
                                return i;
                            }
                        }
                    } catch (NumberFormatException e) {
                        // 继续尝试其他方法
                    }
                }
                Logger.t(TAG).d( "✗ 方法1未匹配");
            }

            // 方法2: 完全匹配剧集名称
            Logger.t(TAG).d( "尝试方法2: 完全匹配剧集名称");
            for (int i = 0; i < episodes.size(); i++) {
                String episodeTitle = episodes.get(i).getDisplayTitle();
                if (episodeTitle != null && episodeTitle.equals(currentTitle)) {
                    Logger.t(TAG).d( "✓ 方法2匹配成功! 位置: " + i);
                    return i;
                }
            }
            Logger.t(TAG).d( "✗ 方法2未匹配");

            // 方法3: 模糊匹配剧集名称（去除特殊字符后比较）
            Logger.t(TAG).d( "尝试方法3: 模糊匹配");
            String cleanCurrentTitle = cleanTitle(currentTitle);
            Logger.t(TAG).d( "清理后的标题: " + cleanCurrentTitle);
            for (int i = 0; i < episodes.size(); i++) {
                String episodeTitle = episodes.get(i).getDisplayTitle();
                if (episodeTitle != null && cleanTitle(episodeTitle).contains(cleanCurrentTitle)) {
                    Logger.t(TAG).d( "✓ 方法3匹配成功! 位置: " + i);
                    return i;
                }
            }
            Logger.t(TAG).d( "✗ 方法3未匹配");

            // 方法4: 按集数数字匹配（从displayTitle提取）
            if (currentEpisodeNumber > 0) {
                Logger.t(TAG).d( "尝试方法4: 集数数字匹配");
                for (int i = 0; i < episodes.size(); i++) {
                    int episodeNumber = extractEpisodeNumber(episodes.get(i).getDisplayTitle());
                    if (episodeNumber == currentEpisodeNumber) {
                        Logger.t(TAG).d( "✓ 方法4匹配成功! 位置: " + i);
                        return i;
                    }
                }

                // 如果没有完全匹配，找最接近的集数
                Logger.t(TAG).d( "尝试查找最接近的集数");
                int closestIndex = -1;
                int minDiff = Integer.MAX_VALUE;
                for (int i = 0; i < episodes.size(); i++) {
                    int episodeNumber = extractEpisodeNumber(episodes.get(i).getDisplayTitle());
                    if (episodeNumber > 0) {
                        int diff = Math.abs(episodeNumber - currentEpisodeNumber);
                        if (diff < minDiff) {
                            minDiff = diff;
                            closestIndex = i;
                        }
                    }
                }
                if (closestIndex >= 0) {
                    Logger.t(TAG).d( "✓ 找到最接近的集数! 位置: " + closestIndex + ", 差值: " + minDiff);
                    return closestIndex;
                }
                Logger.t(TAG).d( "✗ 方法4未匹配");
            }
        } catch (Exception e) {
            Logger.t(TAG).e( "匹配过程出错", e);
            e.printStackTrace();
        }

        // 如果所有方法都未匹配，默认返回第1集（索引0）
        Logger.t(TAG).d( "所有匹配方法都失败，默认返回第1集");
        return 0;
    }

    /**
     * 从标题中提取集数数字
     * 注意：需要区分"季"和"集"，避免误匹配
     */
    private int extractEpisodeNumber(String title) {
        if (title == null || title.isEmpty()) {
            return -1;
        }

        try {
            // 先检查是否包含"季"字，如果只有"季"没有"集"，则跳过数字提取
            boolean hasSeason = title.contains("季");
            boolean hasEpisode = title.contains("集") || title.contains("话") ||
                                 title.toUpperCase().contains("EP") || title.toUpperCase().contains(" E");

            Logger.t(TAG).d( "标题分析: 包含'季'=" + hasSeason + ", 包含'集/话/EP'=" + hasEpisode);

            // 如果只有"季"没有"集"相关标识，直接返回-1
            if (hasSeason && !hasEpisode) {
                Logger.t(TAG).d( "只包含'季'信息，无集数信息");
                return -1;
            }

            // 匹配各种集数格式（按优先级排序）
            java.util.regex.Pattern[] patterns = {
                // 优先匹配明确的"集"字格式
                java.util.regex.Pattern.compile("第(\\d+)集"),           // 第1集
                java.util.regex.Pattern.compile("(\\d+)集"),             // 14集
                java.util.regex.Pattern.compile("EP(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE), // EP1
                java.util.regex.Pattern.compile("(?<!EP)E(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE), // E1（但不是EP1）
                java.util.regex.Pattern.compile("第(\\d+)话"),           // 第1话
                java.util.regex.Pattern.compile("(\\d+)话"),             // 1话
                // 最后才匹配独立数字（最不准确）
                java.util.regex.Pattern.compile("\\|(\\d+)")             // |14（剧集名称中的数字）
            };

            for (java.util.regex.Pattern pattern : patterns) {
                java.util.regex.Matcher matcher = pattern.matcher(title);
                if (matcher.find()) {
                    int episodeNum = Integer.parseInt(matcher.group(1));
                    Logger.t(TAG).d( "正则匹配: " + pattern.pattern() + " -> " + episodeNum);
                    return episodeNum;
                }
            }
        } catch (Exception e) {
            Logger.t(TAG).e( "提取集数失败", e);
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * 清理标题，去除特殊字符和空格
     */
    private String cleanTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.replaceAll("[\\[\\]()（）【】\\s]+", "")
                    .replaceAll("第\\d+集", "")
                    .replaceAll("\\d+集", "")
                    .replaceAll("EP\\d+", "")
                    .toLowerCase()
                    .trim();
    }


    /**
     * 反转剧集列表顺序，并自动跳转到当前播放集数附近
     */
    private void reverseEpisodes() {
        if (currentEpisodes == null || currentEpisodes.isEmpty()) {
            return;
        }

        // 记录反转前当前播放集数的位置
        int currentPosition = findCurrentEpisodePosition(currentEpisodes);

        // 反转列表
        java.util.Collections.reverse(currentEpisodes);
        isReversed = !isReversed;

        // 更新适配器
        episodeAdapter.setData(currentEpisodes);

        // 提示用户
        android.widget.Toast.makeText(getContext(),
            isReversed ? "已反转：最新集在前" : "已恢复：最早集在前",
            android.widget.Toast.LENGTH_SHORT).show();

        // 如果找到了当前播放的集数，反转后自动跳转到该集数
        if (currentPosition >= 0) {
            int newPosition = currentEpisodes.size() - 1 - currentPosition;
            scrollToPositionWithCenter(newPosition);
        } else {
            // 没有找到当前播放集数，滚动到顶部
            episodeResults.post(() -> {
                episodeResults.scrollToPosition(0);
            });
        }
    }


    /**
     * 显示跳转对话框
     */
    private void showJumpDialog() {
        if (currentEpisodes == null || currentEpisodes.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "没有可跳转的剧集", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建输入框
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("输入集数（1-" + currentEpisodes.size() + "）");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setPadding(50, 30, 50, 30);

        // 创建对话框
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("跳转到指定集数")
                .setMessage("当前共 " + currentEpisodes.size() + " 集")
                .setView(input)
                .setPositiveButton("跳转", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) {
                        android.widget.Toast.makeText(getContext(), "请输入集数", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int targetNumber = Integer.parseInt(text);
                        jumpToEpisode(targetNumber);
                    } catch (NumberFormatException e) {
                        android.widget.Toast.makeText(getContext(), "请输入有效的数字", android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 跳转到指定集数
     */
    private void jumpToEpisode(int targetNumber) {
        if (currentEpisodes == null || currentEpisodes.isEmpty()) {
            return;
        }

        if (targetNumber < 1 || targetNumber > currentEpisodes.size()) {
            android.widget.Toast.makeText(getContext(),
                "集数超出范围（1-" + currentEpisodes.size() + "）",
                android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // 计算目标位置（数组索引从0开始）
        int targetPosition = targetNumber - 1;

        // 如果列表已反转，需要调整位置
        if (isReversed) {
            targetPosition = currentEpisodes.size() - targetNumber;
        }

        // 使用优雅的居中滚动
        scrollToPositionWithCenter(targetPosition);
    }

    private void showLoadingView(String message) {
        loadingText.setText(message);
        loadingContainer.setVisibility(View.VISIBLE);
        emptyContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
    }

    private void showEmptyView(String message) {
        emptyText.setText(message);
        emptyContainer.setVisibility(View.VISIBLE);
        loadingContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
    }

    private void showSearchResultsView(List<DanmakuAnime> animes) {
        animeAdapter.setData(animes);
        contentContainer.setVisibility(View.VISIBLE);
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        // 只显示番剧列表，剧集列表为空
        episodeAdapter.setData(new java.util.ArrayList<>());
        showAnimeListOnly();
    }

    /**
     * 同时显示番剧列表和剧集列表
     */
    private void showBothListsView(List<DanmakuAnime> animes, List<DanmakuEpisode> episodes) {
        Logger.t(TAG).d("=== showBothListsView 被调用 ===");
        Logger.t(TAG).d("番剧数量: " + (animes != null ? animes.size() : 0));
        Logger.t(TAG).d("剧集数量: " + (episodes != null ? episodes.size() : 0));

        // 显示番剧列表
        animeAdapter.setData(animes);

        // 显示剧集列表
        currentEpisodes = new java.util.ArrayList<>(episodes);
        isReversed = false;
        episodeAdapter.setData(currentEpisodes);

        // 显示内容容器
        contentContainer.setVisibility(View.VISIBLE);
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        showEpisodeListOnly();

        Logger.t(TAG).d("UI状态已更新，开始查找当前集数");

        // 智能滚动到当前播放的集数并高亮显示（不自动加载弹幕）
        int currentPosition = findCurrentEpisodePosition(currentEpisodes);
        Logger.t(TAG).d("findCurrentEpisodePosition 返回: " + currentPosition);

        if (currentPosition >= 0) {
            Logger.t(TAG).d("找到匹配集数，开始滚动和高亮显示");
            highlightedEpisodePosition = currentPosition;
            scrollToPositionWithCenter(currentPosition);
            // 刷新适配器以显示高亮
            episodeAdapter.notifyDataSetChanged();

            // 显示提示，告知用户需要手动选择
            episodeResults.postDelayed(() -> {
                android.widget.Toast.makeText(getContext(),
                    "已定位到匹配集数，请点击确认加载弹幕",
                    android.widget.Toast.LENGTH_LONG).show();
            }, 1000);
        } else {
            Logger.t(TAG).d("未找到匹配集数，不执行滚动");
            highlightedEpisodePosition = -1;
        }
    }


    /**
     * 优雅地滚动到指定位置，并将其居中显示
     * 使用多重策略确保滚动成功
     */
    private void scrollToPositionWithCenter(int position) {
        Logger.t(TAG).d( "=== 开始滚动到位置: " + position + " ===");

        // 等待布局完成后再滚动
        episodeResults.post(() -> {
            try {
                androidx.recyclerview.widget.LinearLayoutManager layoutManager =
                    (androidx.recyclerview.widget.LinearLayoutManager) episodeResults.getLayoutManager();

                Logger.t(TAG).d( "LayoutManager: " + (layoutManager != null ? "存在" : "为空"));
                Logger.t(TAG).d( "RecyclerView高度: " + episodeResults.getHeight());
                Logger.t(TAG).d( "RecyclerView可见性: " + episodeResults.getVisibility());

                if (layoutManager != null) {
                    // 策略1: 使用scrollToPositionWithOffset将目标项滚动到可见区域中间
                    // 计算偏移量，使目标项显示在RecyclerView中间
                    int offset = episodeResults.getHeight() / 2;
                    Logger.t(TAG).d( "策略1: scrollToPositionWithOffset, offset=" + offset);
                    layoutManager.scrollToPositionWithOffset(position, offset);

                    // 延迟执行第二次滚动，确保布局稳定后再微调
                    episodeResults.postDelayed(() -> {
                        // 策略2: 使用smoothScrollToPosition进行平滑滚动微调
                        Logger.t(TAG).d( "策略2: smoothScrollToPosition");
                        episodeResults.smoothScrollToPosition(position);

                        // 延迟显示提示，让用户看到滚动效果
                        episodeResults.postDelayed(() -> {
                            try {
                                // 获取实际的集数编号用于提示
                                String episodeNum = currentEpisodes.get(position).getEpisodeNumber();
                                Logger.t(TAG).d( "显示提示: 第 " + episodeNum + " 集");
                                android.widget.Toast.makeText(getContext(),
                                    "已定位到第 " + episodeNum + " 集",
                                    android.widget.Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Logger.t(TAG).d( "显示提示: 第 " + (position + 1) + " 个剧集");
                                android.widget.Toast.makeText(getContext(),
                                    "已定位到第 " + (position + 1) + " 个剧集",
                                    android.widget.Toast.LENGTH_SHORT).show();
                            }
                        }, 400);
                    }, 100);
                } else {
                    Logger.t(TAG).e( "LayoutManager为空，无法滚动");
                }
            } catch (Exception e) {
                Logger.t(TAG).e( "滚动过程出错", e);
                e.printStackTrace();
                // 降级方案：使用简单的smoothScrollToPosition
                episodeResults.smoothScrollToPosition(position);
            }
        });
    }

    private boolean handleSearchResultsKey(int keyCode, KeyEvent event) {
        if (event == null || event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            showEpisodeListOnly();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            searchInput.requestFocus();
            searchInput.post(() -> {
                searchInput.setSelection(searchInput.getText().length());
                showKeyboard(searchInput);
            });
            return true;
        }
        return false;
    }

    private boolean handleEpisodeResultsKey(int keyCode, KeyEvent event) {
        if (event == null || event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            showAnimeListOnly();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            searchInput.requestFocus();
            searchInput.post(() -> {
                searchInput.setSelection(searchInput.getText().length());
                showKeyboard(searchInput);
            });
            return true;
        }
        return false;
    }

    private void showKeyboard(View target) {
        try {
            if (target == null) return;
            target.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(target, android.view.inputmethod.InputMethodManager.SHOW_FORCED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideKeyboard() {
        try {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null && searchInput != null) {
                imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 切换番剧列表的展开/收起状态
     */
    private void toggleAnimeList() {
        if (isShowingEpisodeList) {
            showAnimeListOnly();
        } else if (episodeAdapter != null && episodeAdapter.getItemCount() > 0) {
            showEpisodeListOnly();
        } else {
            android.widget.Toast.makeText(getContext(), "请先选择番剧加载剧集", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 只显示番剧列表
     */
    private void showAnimeListOnly() {
        isShowingEpisodeList = false;
        animeListContainer.setVisibility(View.VISIBLE);
        episodeContainer.setVisibility(View.GONE);
        animeExpandButton.setVisibility(View.GONE);
        if (episodeAdapter == null || episodeAdapter.getItemCount() == 0) {
            animeToggleButton.setVisibility(View.GONE);
        } else {
            animeToggleButton.setVisibility(View.VISIBLE);
        }
        searchResults.post(() -> searchResults.requestFocus());
    }

    /**
     * 只显示剧集列表
     */
    private void showEpisodeListOnly() {
        if (episodeAdapter == null || episodeAdapter.getItemCount() == 0) {
            android.widget.Toast.makeText(getContext(), "请先选择番剧加载剧集", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        isShowingEpisodeList = true;
        animeListContainer.setVisibility(View.GONE);
        episodeContainer.setVisibility(View.VISIBLE);
        animeExpandButton.setVisibility(View.VISIBLE);
        animeToggleButton.setVisibility(View.VISIBLE);
        episodeResults.post(() -> episodeResults.requestFocus());
    }

    // 番剧适配器
    private static class DanmakuAnimeAdapter extends RecyclerView.Adapter<DanmakuAnimeAdapter.ViewHolder> {

        private List<DanmakuAnime> data;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(DanmakuAnime anime, int position);
        }

        DanmakuAnimeAdapter(OnItemClickListener listener) {
            this.listener = listener;
        }

        void setData(List<DanmakuAnime> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_danmaku_anime, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DanmakuAnime anime = data.get(position);
            holder.title.setText(anime.getDisplayTitle());
            holder.type.setText(anime.getTypeDescription());
            holder.itemView.setOnClickListener(v -> listener.onItemClick(anime, position));
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView type;

            ViewHolder(View view) {
                super(view);
                title = view.findViewById(R.id.anime_title);
                type = view.findViewById(R.id.anime_type);
            }
        }
    }

    // 剧集适配器
    private class DanmakuEpisodeAdapter extends RecyclerView.Adapter<DanmakuEpisodeAdapter.ViewHolder> {

        private List<DanmakuEpisode> data;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(DanmakuEpisode episode);
        }

        DanmakuEpisodeAdapter(OnItemClickListener listener) {
            this.listener = listener;
        }

        void setData(List<DanmakuEpisode> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_danmaku_episode, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DanmakuEpisode episode = data.get(position);
            holder.title.setText(episode.getDisplayTitle());
            holder.itemView.setOnClickListener(v -> listener.onItemClick(episode));

            // 高亮显示匹配的剧集
            if (position == highlightedEpisodePosition) {
                // 设置醒目的边框和背景
                holder.highlight.setVisibility(View.VISIBLE);
                holder.highlight.setBackgroundColor(0x4000FF00); // 半透明绿色背景
                // 添加边框效果
                android.graphics.drawable.GradientDrawable border = new android.graphics.drawable.GradientDrawable();
                border.setColor(0x4000FF00); // 半透明绿色背景
                border.setStroke(4, 0xFF00FF00); // 绿色边框，4dp宽度
                border.setCornerRadius(8); // 圆角
                holder.highlight.setBackground(border);
            } else {
                holder.highlight.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            View highlight;

            ViewHolder(View view) {
                super(view);
                title = view.findViewById(R.id.episode_title);
                highlight = view.findViewById(R.id.episode_highlight);
            }
        }
    }
}
