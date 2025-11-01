
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
import com.fongmi.android.tv.utils.Util;
import com.orhanobut.logger.Logger;

import android.graphics.Bitmap;

import java.util.List;

/**
 * 弹幕对话框 - 直接展示弹幕检索页
 * 特点：
 * 1. 打开即显示搜索界面，无需额外点击
 * 2. 保留搜索状态，切换集数不需要重新搜索
 * 3. 弹幕参数配置独立处理
 */
public final class DanmakuDialog extends BaseDialog {

    private static final String TAG = DanmakuDialog.class.getSimpleName();

    private EditText searchInput;
    private ImageView searchButton;
    private ImageView settingsButton;
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
    private boolean isShowingEpisodeList = false;

    public static DanmakuDialog create() {
        return new DanmakuDialog();
    }

    public DanmakuDialog player(Players player) {
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
        settingsButton = view.findViewById(R.id.settings_button);
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
        // 遮罩已在 BaseDialog 中统一处理
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
        if (Util.isLeanback()) {
            // Leanback版本：输入框只读，支持遥控器和二维码
            initLeanbackMode();
        } else {
            // Mobile版本：输入框可编辑，支持触摸和滑动
            initMobileMode();
        }

        searchButton.setOnClickListener(v -> performSearch());
        settingsButton.setOnClickListener(this::showSettings);
        closeButton.setOnClickListener(v -> dismiss());
        reverseButton.setOnClickListener(v -> reverseEpisodes());
        jumpButton.setOnClickListener(v -> showJumpDialog());
        if (animeToggleButton != null) {
            animeToggleButton.setOnClickListener(v -> toggleAnimeList());
        }
        if (animeExpandButton != null) {
            animeExpandButton.setOnClickListener(v -> toggleAnimeList());
        }

        // 防止剧集列表滑动事件冒泡到父容器
        episodeResults.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        // 同样处理番剧列表
        searchResults.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
    }

    private void initLeanbackMode() {
        // Leanback版本：输入框只读，但保持可聚焦（遥控器可选中）
        searchInput.setFocusable(true);
        searchInput.setFocusableInTouchMode(true);
        searchInput.setCursorVisible(false);
        searchInput.setKeyListener(null); // 禁止键盘输入，保持只读

        // 点击输入框显示投送二维码
        searchInput.setOnClickListener(v -> showCastQRCode());

        searchInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                showCastQRCode();
                return true;
            }

            return handleSearchInputDpad(keyCode);
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                showCastQRCode();
                return true;
            }
            return false;
        });
    }

    private void initMobileMode() {
        // Mobile版本：输入框可编辑
        searchInput.setFocusable(true);
        searchInput.setFocusableInTouchMode(true);
        searchInput.setCursorVisible(true);

        // 支持回车键搜索
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });

        // Mobile版本：支持左右滑动切换番剧列表和剧集列表
        setupSwipeGesture();
    }

    private void setupSwipeGesture() {
        android.view.GestureDetector gestureDetector = new android.view.GestureDetector(getContext(),
            new android.view.GestureDetector.SimpleOnGestureListener() {
                private static final int SWIPE_THRESHOLD = 100;
                private static final int SWIPE_VELOCITY_THRESHOLD = 100;

                @Override
                public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2,
                                       float velocityX, float velocityY) {
                    if (e1 == null || e2 == null) return false;
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    if (Math.abs(diffX) > Math.abs(diffY) &&
                        Math.abs(diffX) > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // 向右滑动 - 显示番剧列表
                            if (isShowingEpisodeList) {
                                showAnimeListOnly();
                            }
                        } else {
                            // 向左滑动 - 显示剧集列表
                            if (!isShowingEpisodeList && episodeAdapter != null && episodeAdapter.getItemCount() > 0) {
                                showEpisodeListOnly();
                            }
                        }
                        return true;
                    }
                    return false;
                }
            });

        View.OnTouchListener touchListener = (v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        };

        if (contentContainer != null) {
            contentContainer.setOnTouchListener(touchListener);
        }
    }

    private boolean handleSearchInputDpad(int keyCode) {
        int selectionStart = Math.max(searchInput.getSelectionStart(), 0);
        int textLength = searchInput.getText() != null ? searchInput.getText().length() : 0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                return moveFocusFromSearchInput(View.FOCUS_UP);
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (isShowingEpisodeList && requestFocusIfAvailable(episodeResults)) return true;
                if (requestFocusIfAvailable(searchResults)) return true;
                return moveFocusFromSearchInput(View.FOCUS_DOWN);
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (selectionStart > 0) return false;
                return moveFocusFromSearchInput(View.FOCUS_LEFT);
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (selectionStart < textLength) return false;
                if (moveFocusFromSearchInput(View.FOCUS_RIGHT)) return true;
                return requestFocusIfAvailable(searchButton);
            default:
                return false;
        }
    }

    private boolean moveFocusFromSearchInput(int direction) {
        View next = searchInput.focusSearch(direction);
        return requestFocusIfAvailable(next);
    }

    private boolean requestFocusIfAvailable(View target) {
        return target != null && target.isShown() && target.isFocusable() && target.requestFocus();
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

        // 检查当前播放的剧集是否切换
        String currentEpisodeTitle = player != null ? player.getArtist() : "";
        String lastEpisodeTitle = searchState.getLastEpisodeTitle();
        Logger.t(TAG).d("剧集切换检查 - 上次: " + lastEpisodeTitle + ", 当前: " + currentEpisodeTitle);

        // 判断是否是真正的剧集切换（两个都不为空且不相等）
        boolean episodeChanged = false;
        if (!currentEpisodeTitle.isEmpty() && !lastEpisodeTitle.isEmpty()) {
            episodeChanged = !currentEpisodeTitle.equals(lastEpisodeTitle);
        } else if (!currentEpisodeTitle.isEmpty() && lastEpisodeTitle.isEmpty()) {
            // 第一次打开，记录当前剧集但不算作切换
            episodeChanged = false;
        }

        Logger.t(TAG).d("剧集是否切换: " + episodeChanged);

        if (episodeChanged) {
            Logger.t(TAG).d("检测到剧集切换，重置首次显示标志并清理弹幕选择状态");
            searchState.setFirstTimeShowingEpisodes(true);
            searchState.setHighlightedEpisodePosition(-1);
            searchState.setCurrentSelectedDanmaku(null);
            searchState.setUserSelectedEpisodePosition(-1); // 清理用户选择的位置
        }

        // 更新上次的剧集标题（无论是否切换都要更新）
        if (!currentEpisodeTitle.isEmpty()) {
            searchState.setLastEpisodeTitle(currentEpisodeTitle);
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

    private void performAutoSearch() {
        String keyword = searchInput.getText().toString().trim();
        if (keyword.isEmpty()) {
            Logger.t(TAG).d("自动搜索：关键词为空，跳过");
            showEmptyView("输入剧名开始搜索");
            return;
        }

        Logger.t(TAG).d("自动搜索：关键词 = " + keyword);
        searchState.setLastKeyword(keyword);
        showLoadingView("正在自动搜索弹幕...");

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

        hideKeyboard();
        searchState.setLastKeyword(keyword);
        showLoadingView("正在搜索弹幕...");

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
        // Mobile版本：直接加载弹幕，无需二次确认
        String url = DanmakuApi.getDanmakuUrl(episode.getEpisodeId());
        Danmaku danmaku = Danmaku.from(url);
        danmaku.setName(episode.getDisplayTitle());
        searchState.setCurrentSelectedDanmaku(danmaku);

        // 保存用户选择的剧集位置
        int selectedPosition = currentEpisodes.indexOf(episode);
        if (selectedPosition >= 0) {
            searchState.setUserSelectedEpisodePosition(selectedPosition);
            Logger.t(TAG).d("保存用户选择的剧集位置: " + selectedPosition);
        }

        // 直接加载弹幕并关闭对话框
        player.setDanmaku(danmaku);
        android.widget.Toast.makeText(getContext(), "弹幕加载成功", android.widget.Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private void showSettings(View view) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("弹幕设置")
                .setItems(new String[]{"弹幕密度", "透明度", "字体大小", "滚动速度", "描边效果"}, (dialog, which) -> {
                    switch (which) {
                        case 0: showDensityDialog(); break;
                        case 1: showAlphaDialog(); break;
                        case 2: showTextSizeDialog(); break;
                        case 3: showSpeedDialog(); break;
                        case 4: showStrokeDialog(); break;
                    }
                })
                .show();
    }

    private void showDensityDialog() {
        int current = com.fongmi.android.tv.Setting.getDanmakuDensity();
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("弹幕密度 (当前: " + current + ")")
                .setItems(new String[]{"极少 (10)", "少 (20)", "中 (30)", "多 (50)", "极多 (100)"}, (dialog, which) -> {
                    int[] values = {10, 20, 30, 50, 100};
                    com.fongmi.android.tv.Setting.putDanmakuDensity(values[which]);
                    updateDanmakuConfig();
                    android.widget.Toast.makeText(getContext(), "弹幕密度已更新", android.widget.Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showAlphaDialog() {
        float current = com.fongmi.android.tv.Setting.getDanmakuAlpha();
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("透明度 (当前: " + (int)(current * 100) + "%)")
                .setItems(new String[]{"20%", "40%", "60%", "80%", "100%"}, (dialog, which) -> {
                    float[] values = {0.2f, 0.4f, 0.6f, 0.8f, 1.0f};
                    com.fongmi.android.tv.Setting.putDanmakuAlpha(values[which]);
                    updateDanmakuConfig();
                    android.widget.Toast.makeText(getContext(), "透明度已更新", android.widget.Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showTextSizeDialog() {
        float current = com.fongmi.android.tv.Setting.getDanmakuTextSize();
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("字体大小 (当前: " + (int)(current * 100) + "%)")
                .setItems(new String[]{"极小 (50%)", "小 (65%)", "中 (75%)", "大 (85%)", "极大 (100%)"}, (dialog, which) -> {
                    float[] values = {0.5f, 0.65f, 0.75f, 0.85f, 1.0f};
                    com.fongmi.android.tv.Setting.putDanmakuTextSize(values[which]);
                    updateDanmakuConfig();
                    android.widget.Toast.makeText(getContext(), "字体大小已更新", android.widget.Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showSpeedDialog() {
        float current = com.fongmi.android.tv.Setting.getDanmakuSpeed();
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("滚动速度 (当前: " + current + "x)")
                .setItems(new String[]{"极快 (0.8x)", "快 (1.0x)", "中 (1.2x)", "慢 (1.5x)", "极慢 (2.0x)"}, (dialog, which) -> {
                    float[] values = {0.8f, 1.0f, 1.2f, 1.5f, 2.0f};
                    com.fongmi.android.tv.Setting.putDanmakuSpeed(values[which]);
                    updateDanmakuConfig();
                    android.widget.Toast.makeText(getContext(), "滚动速度已更新", android.widget.Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showStrokeDialog() {
        boolean current = com.fongmi.android.tv.Setting.getDanmakuStroke();
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("描边效果 (当前: " + (current ? "开启" : "关闭") + ")")
                .setItems(new String[]{"关闭 (性能优先)", "开启 (清晰优先)"}, (dialog, which) -> {
                    com.fongmi.android.tv.Setting.putDanmakuStroke(which == 1);
                    updateDanmakuConfig();
                    android.widget.Toast.makeText(getContext(), "描边效果已更新", android.widget.Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void updateDanmakuConfig() {
        try {
            if (player != null && player.getDanPlayer() != null) {
                Logger.t(TAG).d("更新弹幕配置");

                // 获取当前选中的弹幕
                Danmaku currentDanmaku = searchState.getCurrentSelectedDanmaku();

                // 如果没有保存的引用，尝试从 player 中查找
                if (currentDanmaku == null && player.getDanmakus() != null) {
                    for (Danmaku danmaku : player.getDanmakus()) {
                        if (danmaku.isSelected()) {
                            currentDanmaku = danmaku;
                            searchState.setCurrentSelectedDanmaku(danmaku);
                            Logger.t(TAG).d("从 player 中找到选中的弹幕: " + danmaku.getName());
                            break;
                        }
                    }
                }

                final Danmaku finalDanmaku = currentDanmaku;

                // 在主线程中更新配置并重新加载弹幕
                mainHandler.post(() -> {
                    try {
                        // 检查 player 是否仍然有效
                        if (player == null || player.getDanPlayer() == null) {
                            Logger.t(TAG).w("player 已失效，取消配置更新");
                            return;
                        }

                        // 先更新配置
                        player.getDanPlayer().updateConfig();
                        Logger.t(TAG).d("弹幕配置已更新");

                        // 如果有弹幕，重新加载以应用新配置
                        if (finalDanmaku != null) {
                            mainHandler.postDelayed(() -> {
                                try {
                                    if (player == null || player.getDanPlayer() == null) {
                                        Logger.t(TAG).w("player 已失效，取消弹幕重新加载");
                                        return;
                                    }
                                    Logger.t(TAG).d("重新加载弹幕以应用新配置: " + finalDanmaku.getName());
                                    // 直接调用 danPlayer.setDanmaku() 重新加载弹幕数据，不改变选中状态
                                    player.getDanPlayer().setDanmaku(finalDanmaku);
                                    Logger.t(TAG).d("弹幕重新加载完成");
                                } catch (Exception e) {
                                    Logger.t(TAG).e("重新加载弹幕时出错", e);
                                    e.printStackTrace();
                                }
                            }, 300);
                        } else {
                            Logger.t(TAG).d("当前没有选中的弹幕，仅更新配置");
                        }
                    } catch (Exception e) {
                        Logger.t(TAG).e("更新弹幕配置时出错", e);
                        e.printStackTrace();
                    }
                });
            } else {
                Logger.t(TAG).e("无法更新弹幕配置：player 或 danPlayer 为空");
            }
        } catch (Exception e) {
            Logger.t(TAG).e("更新弹幕配置时出错", e);
            e.printStackTrace();
        }
    }

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

            String episodeName = null;
            if (currentArtist != null && currentArtist.contains("|")) {
                episodeName = currentArtist.substring(currentArtist.lastIndexOf("|") + 1).trim();
                Logger.t(TAG).d("从Artist提取的剧集名称: " + episodeName);
            }

            int currentEpisodeNumber = -1;
            if (currentArtist != null && !currentArtist.isEmpty()) {
                currentEpisodeNumber = extractEpisodeNumber(currentArtist);
                Logger.t(TAG).d("从Artist提取的集数: " + currentEpisodeNumber);
            }

            if (currentEpisodeNumber <= 0 && currentTitle != null && !currentTitle.isEmpty()) {
                currentEpisodeNumber = extractEpisodeNumber(currentTitle);
                Logger.t(TAG).d("从Title提取的集数: " + currentEpisodeNumber);
            }

            Logger.t(TAG).d("最终提取的集数: " + currentEpisodeNumber);

            Logger.t(TAG).d("剧集列表 (共" + episodes.size() + "集):");
            for (int i = 0; i < Math.min(episodes.size(), 5); i++) {
                Logger.t(TAG).d("  [" + i + "] episodeNumber=" + episodes.get(i).getEpisodeNumber() +
                    ", displayTitle=" + episodes.get(i).getDisplayTitle());
            }

            if (episodeName != null && !episodeName.isEmpty()) {
                Logger.t(TAG).d("尝试方法0: 剧集名称匹配");
                for (int i = 0; i < episodes.size(); i++) {
                    String displayTitle = episodes.get(i).getDisplayTitle();
                    if (displayTitle != null && displayTitle.contains(episodeName)) {
                        Logger.t(TAG).d("✓ 方法0匹配成功! 位置: " + i + ", 匹配: " + displayTitle);
                        return i;
                    }
                }
                Logger.t(TAG).d("✗ 方法0未匹配");
            }

            if (currentEpisodeNumber > 0) {
                Logger.t(TAG).d("尝试方法1: episodeNumber字段匹配");
                for (int i = 0; i < episodes.size(); i++) {
                    try {
                        String episodeNumberStr = episodes.get(i).getEpisodeNumber();
                        if (episodeNumberStr != null) {
                            int episodeNum = Integer.parseInt(episodeNumberStr.trim());
                            if (episodeNum == currentEpisodeNumber) {
                                Logger.t(TAG).d("✓ 方法1匹配成功! 位置: " + i);
                                return i;
                            }
                        }
                    } catch (NumberFormatException e) {
                        // 继续尝试其他方法
                    }
                }
                Logger.t(TAG).d("✗ 方法1未匹配");
            }

            Logger.t(TAG).d("尝试方法2: 完全匹配剧集名称");
            for (int i = 0; i < episodes.size(); i++) {
                String episodeTitle = episodes.get(i).getDisplayTitle();
                if (episodeTitle != null && episodeTitle.equals(currentTitle)) {
                    Logger.t(TAG).d("✓ 方法2匹配成功! 位置: " + i);
                    return i;
                }
            }
            Logger.t(TAG).d("✗ 方法2未匹配");

            Logger.t(TAG).d("尝试方法3: 模糊匹配");
            String cleanCurrentTitle = cleanTitle(currentTitle);
            Logger.t(TAG).d("清理后的标题: " + cleanCurrentTitle);
            for (int i = 0; i < episodes.size(); i++) {
                String episodeTitle = episodes.get(i).getDisplayTitle();
                if (episodeTitle != null && cleanTitle(episodeTitle).contains(cleanCurrentTitle)) {
                    Logger.t(TAG).d("✓ 方法3匹配成功! 位置: " + i);
                    return i;
                }
            }
            Logger.t(TAG).d("✗ 方法3未匹配");

            if (currentEpisodeNumber > 0) {
                Logger.t(TAG).d("尝试方法4: 集数数字匹配");
                for (int i = 0; i < episodes.size(); i++) {
                    int episodeNumber = extractEpisodeNumber(episodes.get(i).getDisplayTitle());
                    if (episodeNumber == currentEpisodeNumber) {
                        Logger.t(TAG).d("✓ 方法4匹配成功! 位置: " + i);
                        return i;
                    }
                }

                Logger.t(TAG).d("尝试查找最接近的集数");
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
                    Logger.t(TAG).d("✓ 找到最接近的集数! 位置: " + closestIndex + ", 差值: " + minDiff);
                    return closestIndex;
                }
                Logger.t(TAG).d("✗ 方法4未匹配");
            }
        } catch (Exception e) {
            Logger.t(TAG).e("匹配过程出错", e);
            e.printStackTrace();
        }

        Logger.t(TAG).d("所有匹配方法都失败，默认返回第1集");
        return 0;
    }

    private int extractEpisodeNumber(String title) {
        if (title == null || title.isEmpty()) {
            return -1;
        }

        try {
            boolean hasSeason = title.contains("季");
            boolean hasEpisode = title.contains("集") || title.contains("话") ||
                                 title.toUpperCase().contains("EP") || title.toUpperCase().contains(" E");

            Logger.t(TAG).d("标题分析: 包含'季'=" + hasSeason + ", 包含'集/话/EP'=" + hasEpisode);

            if (hasSeason && !hasEpisode) {
                Logger.t(TAG).d("只包含'季'信息，无集数信息");
                return -1;
            }

            java.util.regex.Pattern[] patterns = {
                java.util.regex.Pattern.compile("第(\\d+)集"),
                java.util.regex.Pattern.compile("(\\d+)集"),
                java.util.regex.Pattern.compile("EP(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE),
                java.util.regex.Pattern.compile("(?<!EP)E(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE),
                java.util.regex.Pattern.compile("第(\\d+)话"),
                java.util.regex.Pattern.compile("(\\d+)话"),
                java.util.regex.Pattern.compile("\\|(\\d+)")
            };

            for (java.util.regex.Pattern pattern : patterns) {
                java.util.regex.Matcher matcher = pattern.matcher(title);
                if (matcher.find()) {
                    int episodeNum = Integer.parseInt(matcher.group(1));
                    Logger.t(TAG).d("正则匹配: " + pattern.pattern() + " -> " + episodeNum);
                    return episodeNum;
                }
            }
        } catch (Exception e) {
            Logger.t(TAG).e("提取集数失败", e);
            e.printStackTrace();
        }

        return -1;
    }

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

    private void reverseEpisodes() {
        if (currentEpisodes == null || currentEpisodes.isEmpty()) {
            return;
        }

        // 获取当前高亮位置
        int currentHighlightPos = searchState.getHighlightedEpisodePosition();

        // 反转列表
        java.util.Collections.reverse(currentEpisodes);
        isReversed = !isReversed;
        episodeAdapter.setData(currentEpisodes);
        showEpisodeListOnly();

        android.widget.Toast.makeText(getContext(),
            isReversed ? "已反转：最新集在前" : "已恢复：最早集在前",
            android.widget.Toast.LENGTH_SHORT).show();

        // 如果有高亮位置,计算反转后的新位置
        if (currentHighlightPos >= 0 && currentHighlightPos < currentEpisodes.size()) {
            int newPosition = currentEpisodes.size() - 1 - currentHighlightPos;
            searchState.setHighlightedEpisodePosition(newPosition);
            Logger.t(TAG).d("反转后高亮位置: " + currentHighlightPos + " -> " + newPosition);

            episodeResults.postDelayed(() -> {
                episodeAdapter.notifyDataSetChanged();
                scrollToPositionWithCenter(newPosition, false);
            }, 100);
        } else {
            episodeResults.post(() -> {
                episodeResults.scrollToPosition(0);
            });
        }
    }

    private void showJumpDialog() {
        if (currentEpisodes == null || currentEpisodes.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "没有可跳转的剧集", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("输入集数（1-" + currentEpisodes.size() + "）");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setPadding(50, 30, 50, 30);

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

        int targetPosition = targetNumber - 1;

        if (isReversed) {
            targetPosition = currentEpisodes.size() - targetNumber;
        }

        // 更新高亮位置
        int oldPosition = searchState.getHighlightedEpisodePosition();
        searchState.setHighlightedEpisodePosition(targetPosition);
        Logger.t(TAG).d("跳转到集数: " + targetNumber + ", 位置: " + targetPosition);

        // 刷新列表显示
        if (oldPosition >= 0) {
            episodeAdapter.notifyItemChanged(oldPosition);
        }
        episodeAdapter.notifyItemChanged(targetPosition);

        showEpisodeListOnly();
        scrollToPositionWithCenter(targetPosition, true);
    }

    private void toggleAnimeList() {
        if (isShowingEpisodeList) {
            showAnimeListOnly();
        } else if (episodeAdapter != null && episodeAdapter.getItemCount() > 0) {
            showEpisodeListOnly();
        } else {
            android.widget.Toast.makeText(getContext(), "请先选择番剧加载剧集", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void showAnimeListOnly() {
        if (animeListContainer == null || episodeContainer == null) {
            return;
        }
        isShowingEpisodeList = false;
        animeListContainer.setVisibility(View.VISIBLE);
        episodeContainer.setVisibility(View.GONE);
        if (animeExpandButton != null) {
            animeExpandButton.setVisibility(View.GONE);
        }
        if (animeToggleButton != null) {
            if (episodeAdapter == null || episodeAdapter.getItemCount() == 0) {
                animeToggleButton.setVisibility(View.GONE);
            } else {
                animeToggleButton.setVisibility(View.VISIBLE);
            }
        }
        if (searchResults != null) {
            searchResults.post(() -> searchResults.requestFocus());
        }
    }

    private void showEpisodeListOnly() {
        if (animeListContainer == null || episodeContainer == null) {
            return;
        }
        if (episodeAdapter == null || episodeAdapter.getItemCount() == 0) {
            android.widget.Toast.makeText(getContext(), "请先选择番剧加载剧集", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        isShowingEpisodeList = true;
        animeListContainer.setVisibility(View.GONE);
        episodeContainer.setVisibility(View.VISIBLE);
        if (animeExpandButton != null) {
            animeExpandButton.setVisibility(View.VISIBLE);
        }
        if (animeToggleButton != null) {
            animeToggleButton.setVisibility(View.VISIBLE);
        }
        if (episodeResults != null) {
            episodeResults.post(() -> episodeResults.requestFocus());
        }
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
        episodeAdapter.setData(new java.util.ArrayList<>());
        showAnimeListOnly();
    }

    private void showBothListsView(List<DanmakuAnime> animes, List<DanmakuEpisode> episodes) {
        Logger.t(TAG).d("=== showBothListsView 被调用 ===");
        Logger.t(TAG).d("番剧数量: " + (animes != null ? animes.size() : 0));
        Logger.t(TAG).d("剧集数量: " + (episodes != null ? episodes.size() : 0));
        Logger.t(TAG).d("是否首次显示: " + searchState.isFirstTimeShowingEpisodes());

        animeAdapter.setData(animes);

        // 只在首次显示时才重置状态和重新创建列表
        if (searchState.isFirstTimeShowingEpisodes()) {
            Logger.t(TAG).d("首次显示剧集列表，重置状态并开始查找当前集数");
            currentEpisodes = new java.util.ArrayList<>(episodes);
            isReversed = false;
            episodeAdapter.setData(currentEpisodes);
            searchState.setFirstTimeShowingEpisodes(false);

            contentContainer.setVisibility(View.VISIBLE);
            loadingContainer.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.GONE);
            showEpisodeListOnly();

            // 优先使用用户选择的位置，如果没有则查找当前播放的集数
            int targetPosition = searchState.getUserSelectedEpisodePosition();
            boolean isUserSelected = false;

            if (targetPosition >= 0 && targetPosition < currentEpisodes.size()) {
                Logger.t(TAG).d("使用用户上次选择的位置: " + targetPosition);
                isUserSelected = true;
            } else {
                targetPosition = findCurrentEpisodePosition(currentEpisodes);
                Logger.t(TAG).d("findCurrentEpisodePosition 返回: " + targetPosition);
            }

            if (targetPosition >= 0) {
                Logger.t(TAG).d("定位到集数位置: " + targetPosition + "，开始滚动和高亮显示");
                searchState.setHighlightedEpisodePosition(targetPosition);
                scrollToPositionWithCenter(targetPosition, !isUserSelected);
                episodeAdapter.notifyDataSetChanged();

                if (!isUserSelected) {
                    episodeResults.postDelayed(() -> {
                        android.widget.Toast.makeText(getContext(),
                            "已定位到匹配集数，可上下滑动选择其他集数",
                            android.widget.Toast.LENGTH_LONG).show();
                    }, 1000);
                }
            } else {
                Logger.t(TAG).d("未找到匹配集数，不执行滚动");
                searchState.setHighlightedEpisodePosition(-1);
            }
        } else {
            Logger.t(TAG).d("非首次显示，恢复数据并定位到高亮位置");
            // 非首次显示时，需要恢复数据（因为是新的Dialog实例）
            currentEpisodes = new java.util.ArrayList<>(episodes);
            isReversed = false;
            episodeAdapter.setData(currentEpisodes);

            contentContainer.setVisibility(View.VISIBLE);
            loadingContainer.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.GONE);
            showEpisodeListOnly();

            // 延迟通知数据更新，确保 RecyclerView 已经布局完成
            episodeResults.post(() -> {
                episodeAdapter.notifyDataSetChanged();
                Logger.t(TAG).d("数据已恢复，保持高亮位置: " + searchState.getHighlightedEpisodePosition());

                // 如果有高亮位置，滚动到该位置并居中显示
                int highlightPos = searchState.getHighlightedEpisodePosition();
                if (highlightPos >= 0 && highlightPos < currentEpisodes.size()) {
                    episodeResults.postDelayed(() -> {
                        // 使用居中滚动方法，确保高亮项在可视区域中央
                        androidx.recyclerview.widget.LinearLayoutManager layoutManager =
                            (androidx.recyclerview.widget.LinearLayoutManager) episodeResults.getLayoutManager();
                        if (layoutManager != null) {
                            int offset = episodeResults.getHeight() / 2;
                            layoutManager.scrollToPositionWithOffset(highlightPos, offset);
                            Logger.t(TAG).d("滚动到高亮位置并居中: " + highlightPos);
                        } else {
                            episodeResults.smoothScrollToPosition(highlightPos);
                            Logger.t(TAG).d("滚动到高亮位置: " + highlightPos);
                        }
                    }, 100);
                }
            });
        }
    }

    private void scrollToPositionWithCenter(int position, boolean showToast) {
        Logger.t(TAG).d("=== 开始滚动到位置: " + position + ", 显示提示: " + showToast + " ===");

        // 保存Context引用，避免在延迟执行时Context为null
        final android.content.Context context = getContext();

        episodeResults.post(() -> {
            try {
                androidx.recyclerview.widget.LinearLayoutManager layoutManager =
                    (androidx.recyclerview.widget.LinearLayoutManager) episodeResults.getLayoutManager();

                Logger.t(TAG).d("LayoutManager: " + (layoutManager != null ? "存在" : "为空"));
                Logger.t(TAG).d("RecyclerView高度: " + episodeResults.getHeight());
                Logger.t(TAG).d("RecyclerView可见性: " + episodeResults.getVisibility());

                if (layoutManager != null) {
                    int offset = episodeResults.getHeight() / 2;
                    Logger.t(TAG).d("策略1: scrollToPositionWithOffset, offset=" + offset);
                    layoutManager.scrollToPositionWithOffset(position, offset);

                    episodeResults.postDelayed(() -> {
                        Logger.t(TAG).d("策略2: smoothScrollToPosition");
                        episodeResults.smoothScrollToPosition(position);

                        if (showToast && context != null) {
                            episodeResults.postDelayed(() -> {
                                try {
                                    String episodeNum = currentEpisodes.get(position).getEpisodeNumber();
                                    Logger.t(TAG).d("显示提示: 第 " + episodeNum + " 集");
                                    android.widget.Toast.makeText(context,
                                        "已定位到第 " + episodeNum + " 集",
                                        android.widget.Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Logger.t(TAG).d("显示提示: 第 " + (position + 1) + " 个剧集");
                                    android.widget.Toast.makeText(context,
                                        "已定位到第 " + (position + 1) + " 个剧集",
                                        android.widget.Toast.LENGTH_SHORT).show();
                                }
                            }, 400);
                        }
                    }, 100);
                } else {
                    Logger.t(TAG).e("LayoutManager为空，无法滚动");
                }
            } catch (Exception e) {
                Logger.t(TAG).e("滚动过程出错", e);
                e.printStackTrace();
                episodeResults.smoothScrollToPosition(position);
            }
        });
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

    // 番剧适配器
    private class DanmakuAnimeAdapter extends RecyclerView.Adapter<DanmakuAnimeAdapter.ViewHolder> {

        private List<DanmakuAnime> data;
        private final OnItemClickListener listener;
        private int selectedPosition = -1;

        interface OnItemClickListener {
            void onItemClick(DanmakuAnime anime, int position);
        }

        DanmakuAnimeAdapter(OnItemClickListener listener) {
            this.listener = listener;
        }

        void setData(List<DanmakuAnime> data) {
            this.data = data;
            // 恢复选中位置
            this.selectedPosition = searchState.getSelectedAnimePosition();
            notifyDataSetChanged();
        }

        void setSelectedPosition(int position) {
            int oldPosition = selectedPosition;
            selectedPosition = position;
            if (oldPosition >= 0) {
                notifyItemChanged(oldPosition);
            }
            if (selectedPosition >= 0) {
                notifyItemChanged(selectedPosition);
            }
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
            holder.itemView.setOnClickListener(v -> {
                setSelectedPosition(position);
                listener.onItemClick(anime, position);
            });

            // 显示选中状态 - 使用activated状态触发selector
            holder.itemView.setActivated(position == selectedPosition);

            // 添加焦点监听器，确保焦点变化时重绘
            holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
                // 焦点变化时会自动触发selector的state_focused
                // 这里只需要确保view刷新
                v.invalidate();
            });
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
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
            holder.itemView.setOnClickListener(v -> {
                // 更新高亮位置为用户选择的位置
                int oldPosition = searchState.getHighlightedEpisodePosition();
                searchState.setHighlightedEpisodePosition(position);
                if (oldPosition >= 0 && oldPosition != position) {
                    notifyItemChanged(oldPosition);
                }
                notifyItemChanged(position);
                listener.onItemClick(episode);
            });

            // 使用activated状态触发selector，同时保留highlight视觉效果
            boolean isHighlighted = position == searchState.getHighlightedEpisodePosition();
            holder.itemView.setActivated(isHighlighted);

            // 添加焦点监听器
            holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
                // 焦点变化时自动触发selector的state_focused
                v.invalidate();
            });

            if (isHighlighted) {
                holder.highlight.setVisibility(View.VISIBLE);
                android.graphics.drawable.GradientDrawable border = new android.graphics.drawable.GradientDrawable();
                border.setColor(0x00000000); // 透明背景，不影响selector
                border.setStroke(4, 0xFFFFEB3B); // 使用primary颜色作为边框
                border.setCornerRadius(6);
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

    /**
     * 显示投送二维码对话框（仅 Leanback 版本）
     */
    private void showCastQRCode() {
        try {
            // 获取局域网地址和端口
            String ip = com.github.catvod.utils.Util.getIp();
            int port = com.github.catvod.Proxy.getPort();

            if (ip.isEmpty()) {
                android.widget.Toast.makeText(getContext(), "无法获取局域网IP地址", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            // 生成投送URL - 指向弹幕搜索页面
            String castUrl = "http://" + ip + ":" + port + "/danmaku";
            Logger.t(TAG).d("生成投送URL: " + castUrl);

            // 使用反射调用 QRCode.getBitmap (仅 leanback 版本有此类)
            Class<?> qrCodeClass = Class.forName("com.fongmi.android.tv.utils.QRCode");
            java.lang.reflect.Method getBitmapMethod = qrCodeClass.getMethod("getBitmap", String.class, int.class, int.class);
            Bitmap qrBitmap = (Bitmap) getBitmapMethod.invoke(null, castUrl, 200, 1);

            if (qrBitmap == null) {
                android.widget.Toast.makeText(getContext(), "生成二维码失败", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建对话框显示二维码
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());

            // 创建自定义布局
            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 40, 40, 40);
            layout.setGravity(android.view.Gravity.CENTER);

            // 添加标题文本
            TextView titleText = new TextView(getContext());
            titleText.setText("扫描二维码进行弹幕投送");
            titleText.setTextSize(16);
            titleText.setGravity(android.view.Gravity.CENTER);
            titleText.setPadding(0, 0, 0, 20);
            layout.addView(titleText);

            // 添加二维码图片
            ImageView qrImageView = new ImageView(getContext());
            qrImageView.setImageBitmap(qrBitmap);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            imageParams.gravity = android.view.Gravity.CENTER;
            qrImageView.setLayoutParams(imageParams);
            layout.addView(qrImageView);

            // 添加URL文本
            TextView urlText = new TextView(getContext());
            urlText.setText(castUrl);
            urlText.setTextSize(12);
            urlText.setGravity(android.view.Gravity.CENTER);
            urlText.setPadding(0, 20, 0, 0);
            urlText.setTextColor(0xFF999999);
            layout.addView(urlText);

            // 添加说明文本
            TextView hintText = new TextView(getContext());
            hintText.setText("使用手机扫描二维码\n在手机上搜索并投送弹幕到电视");
            hintText.setTextSize(12);
            hintText.setGravity(android.view.Gravity.CENTER);
            hintText.setPadding(0, 10, 0, 0);
            hintText.setTextColor(0xFF666666);
            layout.addView(hintText);

            builder.setView(layout);
            builder.setPositiveButton("关闭", null);
            builder.show();

        } catch (Exception e) {
            Logger.t(TAG).e("显示投送二维码失败", e);
            android.widget.Toast.makeText(getContext(), "显示二维码失败: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
