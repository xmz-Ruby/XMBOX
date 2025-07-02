package com.fongmi.android.tv.ui.fragment;

import android.content.Intent;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.FragmentSettingPlayerBinding;
import com.fongmi.android.tv.impl.BufferCallback;
import com.fongmi.android.tv.impl.SpeedCallback;
import com.fongmi.android.tv.impl.UaCallback;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.dialog.BufferDialog;
import com.fongmi.android.tv.ui.dialog.SpeedDialog;
import com.fongmi.android.tv.ui.dialog.UaDialog;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DecimalFormat;

public class SettingPlayerFragment extends BaseFragment implements UaCallback, BufferCallback, SpeedCallback {

    private FragmentSettingPlayerBinding mBinding;
    private DecimalFormat format;
    private String[] background;
    private String[] caption;
    private String[] render;
    private String[] scale;

    public static SettingPlayerFragment newInstance() {
        return new SettingPlayerFragment();
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingPlayerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        format = new DecimalFormat("0.#");
        mBinding.back.setOnClickListener(v -> requireActivity().onBackPressed());
        mBinding.uaText.setText(Setting.getUa());
        mBinding.tunnelSwitch.setChecked(Setting.isTunnel());
        mBinding.audioDecodeSwitch.setChecked(Setting.isAudioPrefer());
        mBinding.aacSwitch.setChecked(Setting.isPreferAAC());
        mBinding.danmakuLoadSwitch.setChecked(Setting.isDanmakuLoad());
        mBinding.speedText.setText(format.format(Setting.getSpeed()));
        mBinding.bufferText.setText(String.valueOf(Setting.getBuffer()));
        mBinding.caption.setVisibility(Setting.hasCaption() ? View.VISIBLE : View.GONE);
        mBinding.scaleText.setText((scale = ResUtil.getStringArray(R.array.select_scale))[Setting.getScale()]);
        mBinding.renderText.setText((render = ResUtil.getStringArray(R.array.select_render))[Setting.getRender()]);
        mBinding.captionText.setText((caption = ResUtil.getStringArray(R.array.select_caption))[Setting.isCaption() ? 1 : 0]);
        mBinding.backgroundText.setText((background = ResUtil.getStringArray(R.array.select_background))[Setting.getBackground()]);
        
        // 设置开关的颜色为黄色
        int accentColor = getResources().getColor(R.color.accent);
        android.content.res.ColorStateList colorStateList = new android.content.res.ColorStateList(
            new int[][]{
                new int[]{-android.R.attr.state_checked},
                new int[]{android.R.attr.state_checked}
            },
            new int[]{
                0x66FFFFFF,  // 未选中时的颜色
                accentColor   // 选中时的颜色
            }
        );
        
        mBinding.tunnelSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        mBinding.tunnelSwitch.setTrackTintList(colorStateList);
        mBinding.audioDecodeSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        mBinding.audioDecodeSwitch.setTrackTintList(colorStateList);
        mBinding.aacSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        mBinding.aacSwitch.setTrackTintList(colorStateList);
        mBinding.danmakuLoadSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        mBinding.danmakuLoadSwitch.setTrackTintList(colorStateList);
    }

    @Override
    protected void initEvent() {
        mBinding.ua.setOnClickListener(this::onUa);
        mBinding.aac.setOnClickListener(this::setAAC);
        mBinding.scale.setOnClickListener(this::onScale);
        mBinding.speed.setOnClickListener(this::onSpeed);
        mBinding.buffer.setOnClickListener(this::onBuffer);
        mBinding.render.setOnClickListener(this::setRender);
        mBinding.tunnel.setOnClickListener(this::setTunnel);
        mBinding.caption.setOnClickListener(this::setCaption);
        mBinding.caption.setOnLongClickListener(this::onCaption);
        mBinding.background.setOnClickListener(this::onBackground);
        mBinding.audioDecode.setOnClickListener(this::setAudioDecode);
        mBinding.danmakuLoad.setOnClickListener(this::setDanmakuLoad);
    }

    private void onUa(View view) {
        UaDialog.create(this).show();
    }

    @Override
    public void setUa(String ua) {
        mBinding.uaText.setText(ua);
        Setting.putUa(ua);
    }

    private void setAAC(View view) {
        boolean isChecked = !Setting.isPreferAAC();
        Setting.putPreferAAC(isChecked);
        mBinding.aacSwitch.setChecked(isChecked);
    }

    private void onScale(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.player_scale).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(scale, Setting.getScale(), (dialog, which) -> {
            mBinding.scaleText.setText(scale[which]);
            Setting.putScale(which);
            dialog.dismiss();
        }).show();
    }

    private void onSpeed(View view) {
        SpeedDialog.create(this).show();
    }

    @Override
    public void setSpeed(float speed) {
        mBinding.speedText.setText(format.format(speed));
        Setting.putSpeed(speed);
    }

    private void onBuffer(View view) {
        BufferDialog.create(this).show();
    }

    @Override
    public void setBuffer(int times) {
        mBinding.bufferText.setText(String.valueOf(times));
        Setting.putBuffer(times);
    }

    private void setRender(View view) {
        int index = Setting.getRender();
        Setting.putRender(index = index == render.length - 1 ? 0 : ++index);
        mBinding.renderText.setText(render[index]);
        if (Setting.isTunnel() && Setting.getRender() == 1) setTunnel(view);
    }

    private void setTunnel(View view) {
        boolean isChecked = !Setting.isTunnel();
        Setting.putTunnel(isChecked);
        mBinding.tunnelSwitch.setChecked(isChecked);
        if (isChecked && Setting.getRender() == 1) setRender(view);
    }

    private void setCaption(View view) {
        Setting.putCaption(!Setting.isCaption());
        mBinding.captionText.setText(caption[Setting.isCaption() ? 1 : 0]);
    }

    private boolean onCaption(View view) {
        if (Setting.isCaption()) startActivity(new Intent(Settings.ACTION_CAPTIONING_SETTINGS));
        return Setting.isCaption();
    }

    private void onBackground(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.player_background).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(background, Setting.getBackground(), (dialog, which) -> {
            mBinding.backgroundText.setText(background[which]);
            Setting.putBackground(which);
            dialog.dismiss();
        }).show();
    }

    private void setAudioDecode(View view) {
        boolean isChecked = !Setting.isAudioPrefer();
        Setting.putAudioPrefer(isChecked);
        mBinding.audioDecodeSwitch.setChecked(isChecked);
    }

    private void setDanmakuLoad(View view) {
        boolean isChecked = !Setting.isDanmakuLoad();
        Setting.putDanmakuLoad(isChecked);
        mBinding.danmakuLoadSwitch.setChecked(isChecked);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) initView();
    }
}
