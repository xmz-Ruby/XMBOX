
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
        binding.choose.setOnClickListener(this::showChooser);
        binding.title.setOnClickListener(this::showSettings);
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