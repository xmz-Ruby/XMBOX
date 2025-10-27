
package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.databinding.DialogDanmakuBinding;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.ui.adapter.DanmakuAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.FileChooser;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public final class DanmakuDialog extends BaseDialog implements DanmakuAdapter.OnClickListener {

    private final DanmakuAdapter adapter;
    private DialogDanmakuBinding binding;
    private Players player;

    public static DanmakuDialog create() {
        return new DanmakuDialog();
    }

    public DanmakuDialog() {
        this.adapter = new DanmakuAdapter(this);
    }

    public DanmakuDialog player(Players player) {
        this.player = player;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogDanmakuBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter.addAll(player.getDanmakus()));
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(adapter.getSelected()));
        binding.recycler.setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void initEvent() {
        binding.search.setOnClickListener(this::showSearchDialog);
        binding.settings.setOnClickListener(this::showSettings);
        binding.choose.setOnClickListener(this::showChooser);
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

    private void showSearchDialog(View view) {
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("请输入剧名");
        String defaultKeyword = getDefaultSearchKeyword();
        input.setText(defaultKeyword);
        if (!defaultKeyword.isEmpty()) {
            input.setSelection(input.getText().length());
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("搜索弹幕")
                .setView(input)
                .setPositiveButton("搜索", (dialog, which) -> {
                    String keyword = input.getText().toString().trim();
                    if (!keyword.isEmpty()) {
                        searchAnime(keyword);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String getDefaultSearchKeyword() {
        try {
            // 尝试从播放器获取当前视频标题
            if (player != null) {
                String title = player.getTitle();
                if (title != null && !title.isEmpty()) {
                    // 移除常见的集数标识
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

    private void searchAnime(String keyword) {
        android.app.ProgressDialog progress = android.app.ProgressDialog.show(getContext(), "搜索中", "正在搜索弹幕...", true);

        com.fongmi.android.tv.api.DanmakuApi.searchAnime(keyword, new com.fongmi.android.tv.api.DanmakuApi.Callback<java.util.List<com.fongmi.android.tv.bean.DanmakuAnime>>() {
            @Override
            public void onSuccess(java.util.List<com.fongmi.android.tv.bean.DanmakuAnime> animes) {
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    if (animes.isEmpty()) {
                        android.widget.Toast.makeText(getContext(), "未找到相关弹幕", android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        showAnimeList(animes);
                    }
                });
            }

            @Override
            public void onError(String message) {
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showAnimeList(java.util.List<com.fongmi.android.tv.bean.DanmakuAnime> animes) {
        String[] titles = new String[animes.size()];
        for (int i = 0; i < animes.size(); i++) {
            titles[i] = animes.get(i).getDisplayTitle();
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("选择番剧")
                .setItems(titles, (dialog, which) -> {
                    loadEpisodes(animes.get(which).getAnimeId());
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadEpisodes(int animeId) {
        android.app.ProgressDialog progress = android.app.ProgressDialog.show(getContext(), "加载中", "正在加载剧集...", true);

        com.fongmi.android.tv.api.DanmakuApi.getBangumiEpisodes(animeId, new com.fongmi.android.tv.api.DanmakuApi.Callback<java.util.List<com.fongmi.android.tv.bean.DanmakuEpisode>>() {
            @Override
            public void onSuccess(java.util.List<com.fongmi.android.tv.bean.DanmakuEpisode> episodes) {
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    if (episodes.isEmpty()) {
                        android.widget.Toast.makeText(getContext(), "该番剧暂无弹幕", android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        showEpisodeList(episodes);
                    }
                });
            }

            @Override
            public void onError(String message) {
                getActivity().runOnUiThread(() -> {
                    progress.dismiss();
                    android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showEpisodeList(java.util.List<com.fongmi.android.tv.bean.DanmakuEpisode> episodes) {
        String[] titles = new String[episodes.size()];
        for (int i = 0; i < episodes.size(); i++) {
            titles[i] = episodes.get(i).getDisplayTitle();
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("选择集数")
                .setItems(titles, (dialog, which) -> {
                    loadDanmaku(episodes.get(which));
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadDanmaku(com.fongmi.android.tv.bean.DanmakuEpisode episode) {
        String url = com.fongmi.android.tv.api.DanmakuApi.getDanmakuUrl(episode.getEpisodeId());
        Danmaku danmaku = Danmaku.from(url);
        danmaku.setName(episode.getDisplayTitle());
        player.setDanmaku(danmaku);
        android.widget.Toast.makeText(getContext(), "弹幕加载成功", android.widget.Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private void showDensityDialog() {
        int current = com.fongmi.android.tv.Setting.getDanmakuDensity();
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("弹幕密度 (当前: " + current + ")")
                .setItems(new String[]{"极少 (10)", "少 (20)", "中 (30)", "多 (50)", "极多 (100)"}, (dialog, which) -> {
                    int[] values = {10, 20, 30, 50, 100};
                    com.fongmi.android.tv.Setting.putDanmakuDensity(values[which]);
                    player.getDanPlayer().updateConfig();
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
                    player.getDanPlayer().updateConfig();
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
                    player.getDanPlayer().updateConfig();
                })
                .show();
    }

    private void showSpeedDialog() {
        float current = com.fongmi.android.tv.Setting.getDanmakuSpeed();
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("滚动速度 (当前: " + current + "x)")
                .setItems(new String[]{"极慢 (0.8x)", "慢 (1.0x)", "中 (1.2x)", "快 (1.5x)", "极快 (2.0x)"}, (dialog, which) -> {
                    float[] values = {0.8f, 1.0f, 1.2f, 1.5f, 2.0f};
                    com.fongmi.android.tv.Setting.putDanmakuSpeed(values[which]);
                    player.getDanPlayer().updateConfig();
                })
                .show();
    }

    private void showStrokeDialog() {
        boolean current = com.fongmi.android.tv.Setting.getDanmakuStroke();
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("描边效果 (当前: " + (current ? "开启" : "关闭") + ")")
                .setItems(new String[]{"关闭 (性能优先)", "开启 (清晰优先)"}, (dialog, which) -> {
                    com.fongmi.android.tv.Setting.putDanmakuStroke(which == 1);
                    player.getDanPlayer().updateConfig();
                })
                .show();
    }

    private void showChooser(View view) {
        FileChooser.from(this).show(new String[]{"text/*", "application/xml", "application/json"});
        player.pause();
    }

    @Override
    public void onItemClick(Danmaku item) {
        player.setDanmaku(item.isSelected() ? Danmaku.empty() : item);
        dismiss();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != FileChooser.REQUEST_PICK_FILE) return;
        player.setDanmaku(Danmaku.from(FileChooser.getPathFromUri(data.getData())));
        dismiss();
    }
}