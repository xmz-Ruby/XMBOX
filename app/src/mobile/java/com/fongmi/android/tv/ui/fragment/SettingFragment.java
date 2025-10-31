package com.fongmi.android.tv.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.FragmentSettingBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.impl.ProxyCallback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.ui.activity.SettingPlayerActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.dialog.AboutDialog;
import com.fongmi.android.tv.ui.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.dialog.HistoryDialog;
import com.fongmi.android.tv.ui.dialog.LiveDialog;
import com.fongmi.android.tv.ui.dialog.ProxyDialog;
import com.fongmi.android.tv.ui.dialog.RestoreDialog;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.bean.Doh;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.permissionx.guolindev.PermissionX;

import java.util.ArrayList;
import java.util.List;

public class SettingFragment extends BaseFragment implements ConfigCallback, SiteCallback, LiveCallback, ProxyCallback {

    private FragmentSettingBinding mBinding;
    private String[] size;
    private int type;

    public static SettingFragment newInstance() {
        return new SettingFragment();
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    private String getProxy(String proxy) {
        return proxy.isEmpty() ? getString(R.string.none) : UrlUtil.scheme(proxy);
    }

    private int getDohIndex() {
        return Math.max(0, VodConfig.get().getDoh().indexOf(Doh.objectFrom(Setting.getDoh())));
    }

    private String[] getDohList() {
        List<String> list = new ArrayList<>();
        for (Doh item : VodConfig.get().getDoh()) list.add(item.getName());
        return list.toArray(new String[0]);
    }

    private HomeActivity getRoot() {
        return (HomeActivity) getActivity();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setSourceHintText(mBinding.vodUrl, VodConfig.getDesc(), R.string.source_hint_setting);
        setSourceHintText(mBinding.liveUrl, LiveConfig.getDesc(), R.string.source_hint_live);
        // setSourceHintText(mBinding.wallUrl, WallConfig.getDesc(), R.string.source_hint_wall); // 壁纸功能已移除
        mBinding.versionText.setText(getString(R.string.setting_version) + " " + BuildConfig.VERSION_NAME);
        
        setOtherText();
        setCacheText();
        String[] quotes = getResources().getStringArray(R.array.motivational_quotes);
        int randomIndex = new java.util.Random().nextInt(quotes.length);
        mBinding.marquee.setText(quotes[randomIndex]);
    }

    private void setOtherText() {
        mBinding.dohText.setText(getDohList()[getDohIndex()]);
        mBinding.proxyText.setText(getProxy(Setting.getProxy()));
        mBinding.incognitoSwitch.setChecked(Setting.isIncognito());
        mBinding.liveTabVisibleSwitch.setChecked(Setting.isLiveTabVisible());
        mBinding.sizeText.setText((size = ResUtil.getStringArray(R.array.select_size))[Setting.getSize()]);
        setLiveSettingsVisibility();
    }

    private void setLiveSettingsVisibility() {
        boolean isLiveTabVisible = !Setting.isLiveTabVisible(); // 注意：这里取反，因为开关是"隐藏直播"
        
        // 获取直播容器的布局参数
        LinearLayout.LayoutParams liveContainerParams = (LinearLayout.LayoutParams) mBinding.liveContainer.getLayoutParams();
        
        if (isLiveTabVisible) {
            // 直播开关打开：显示直播模块，间距为12dp
            mBinding.liveContainer.setVisibility(View.VISIBLE);
            liveContainerParams.topMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        } else {
            // 直播开关关闭：隐藏直播模块，间距为0dp（这样视频模块和下一个模块之间会有正常间距）
            mBinding.liveContainer.setVisibility(View.GONE);
            liveContainerParams.topMargin = 0;
        }
        
        // 应用布局参数
        mBinding.liveContainer.setLayoutParams(liveContainerParams);
    }

    private void setCacheText() {
        FileUtil.getCacheSize(new Callback() {
            @Override
            public void success(String result) {
                mBinding.cacheText.setText(result);
            }
        });
    }

    @Override
    protected void initEvent() {
        mBinding.vod.setOnClickListener(this::onVod);
        mBinding.live.setOnClickListener(this::onLive);
        // mBinding.wall.setOnClickListener(this::onWall); // 壁纸功能已移除
        mBinding.proxy.setOnClickListener(this::onProxy);
        mBinding.cache.setOnClickListener(this::onCache);
        mBinding.backup.setOnClickListener(this::onBackup);
        mBinding.player.setOnClickListener(this::onPlayer);
        mBinding.restore.setOnClickListener(this::onRestore);
        mBinding.version.setOnClickListener(this::onVersion);
        mBinding.about.setOnClickListener(this::onAbout);
        mBinding.vod.setOnLongClickListener(this::onVodEdit);
        mBinding.vodHome.setOnClickListener(this::onVodHome);
        mBinding.live.setOnLongClickListener(this::onLiveEdit);
        mBinding.liveHome.setOnClickListener(this::onLiveHome);
        // mBinding.wall.setOnLongClickListener(this::onWallEdit); // 壁纸功能已移除
        mBinding.vodHistory.setOnClickListener(this::onVodHistory);
        mBinding.version.setOnLongClickListener(this::onVersionDev);
        mBinding.liveHistory.setOnClickListener(this::onLiveHistory);
        // mBinding.wallDefault.setOnClickListener(this::setWallDefault); // 壁纸功能已移除
        // mBinding.wallRefresh.setOnClickListener(this::setWallRefresh); // 壁纸功能已移除
        mBinding.incognitoSwitch.setOnClickListener(this::setIncognito);
        mBinding.liveTabVisibleSwitch.setOnClickListener(this::setLiveTabVisible);
        mBinding.size.setOnClickListener(this::setSize);
        mBinding.doh.setOnClickListener(this::setDoh);
    }

    @Override
    public void setConfig(Config config) {
        // 添加Fragment状态检查，防止在无效状态下执行
        if (getActivity() == null || !isAdded() || isDetached()) return;

        // 如果URL为空，不进行任何操作
        if (config == null || config.isEmpty()) return;

        try {
            load(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load(Config config) {
        // 再次检查Fragment状态，防止在异步回调中执行
        if (getActivity() == null || !isAdded() || isDetached()) return;
        
        try {
            switch (config.getType()) {
                case 0:
                    Notify.progress(getActivity());
                    VodConfig.load(config, getCallback(0));
                    if (mBinding != null && mBinding.vodUrl != null) {
                        mBinding.vodUrl.setText(config.getDesc());
                    }
                    break;
                case 1:
                    Notify.progress(getActivity());
                    LiveConfig.load(config, getCallback(1));
                    if (mBinding != null && mBinding.liveUrl != null) {
                        mBinding.liveUrl.setText(config.getDesc());
                    }
                    break;
                case 2:
                    Notify.progress(getActivity());
                    WallConfig.load(config, getCallback(2));
                    // if (mBinding != null && mBinding.wallUrl != null) { // 壁纸功能已移除
                    //     mBinding.wallUrl.setText(config.getDesc());
                    // }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Notify.dismiss();
        }
    }

    private Callback getCallback(int type) {
        return new Callback() {
            @Override
            public void success(String result) {
                // 检查Fragment是否还在活动状态
                if (getActivity() == null || !isAdded()) return;
                Notify.show(result);
            }

            @Override
            public void success() {
                // 检查Fragment是否还在活动状态
                if (getActivity() == null || !isAdded()) return;
                setConfig(type);
            }

            @Override
            public void error(String msg) {
                // 检查Fragment是否还在活动状态
                if (getActivity() == null || !isAdded()) return;
                Notify.show(msg);
                Notify.dismiss();
                switch (type) {
                    case 0:
                        setSourceHintText(mBinding.vodUrl, VodConfig.getDesc(), R.string.source_hint_setting);
                        break;
                    case 1:
                        setSourceHintText(mBinding.liveUrl, LiveConfig.getDesc(), R.string.source_hint_live);
                        break;
                    case 2:
                        // setSourceHintText(mBinding.wallUrl, WallConfig.getDesc(), R.string.source_hint_wall); // 壁纸功能已移除
                        break;
                }
            }
        };
    }

    private void setConfig(int type) {
        switch (type) {
            case 0:
                setCacheText();
                Notify.dismiss();
                RefreshEvent.video();
                RefreshEvent.config();
                setSourceHintText(mBinding.vodUrl, VodConfig.getDesc(), R.string.source_hint_setting);
                setSourceHintText(mBinding.liveUrl, LiveConfig.getDesc(), R.string.source_hint_live);
                // setSourceHintText(mBinding.wallUrl, WallConfig.getDesc(), R.string.source_hint_wall); // 壁纸功能已移除
                break;
            case 1:
                setCacheText();
                Notify.dismiss();
                RefreshEvent.config();
                setSourceHintText(mBinding.liveUrl, LiveConfig.getDesc(), R.string.source_hint_live);
                break;
            case 2:
                setCacheText();
                Notify.dismiss();
                // setSourceHintText(mBinding.wallUrl, WallConfig.getDesc(), R.string.source_hint_wall); // 壁纸功能已移除
                break;
        }
    }

    private void setSourceHintText(TextView textView, String desc, int hintStringRes) {
        if (TextUtils.isEmpty(desc)) {
            SpannableString spannable = new SpannableString(getString(hintStringRes));
            spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.white)), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(0.8f), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            int alpha = (int)(255 * 0.5f);
            spannable.setSpan(new ForegroundColorSpan(android.graphics.Color.argb(alpha, 255, 255, 255)), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(spannable);
        } else {
            textView.setText(desc);
        }
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
        RefreshEvent.video();
    }

    @Override
    public void onChanged() {
    }

    @Override
    public void setLive(Live item) {
        LiveConfig.get().setHome(item);
    }

    private void onVod(View view) {
        ConfigDialog.create(this).type(type = 0).show();
    }

    private void onLive(View view) {
        ConfigDialog.create(this).type(type = 1).show();
    }

    private void onWall(View view) {
        ConfigDialog.create(this).type(type = 2).show();
    }

    private boolean onVodEdit(View view) {
        ConfigDialog.create(this).type(type = 0).edit().show();
        return true;
    }

    private boolean onLiveEdit(View view) {
        ConfigDialog.create(this).type(type = 1).edit().show();
        return true;
    }

    private boolean onWallEdit(View view) {
        ConfigDialog.create(this).type(type = 2).edit().show();
        return true;
    }

    private void onVodHome(View view) {
        SiteDialog.create(this).all().show();
    }

    private void onLiveHome(View view) {
        LiveDialog.create(this).action().show();
    }

    private void onVodHistory(View view) {
        HistoryDialog.create(this).type(type = 0).show();
    }

    private void onLiveHistory(View view) {
        HistoryDialog.create(this).type(type = 1).show();
    }

    private void onPlayer(View view) {
        SettingPlayerActivity.start(requireActivity());
    }

    private void onVersion(View view) {
        AboutDialog.show(this);
    }

    private void onAbout(View view) {
        AboutDialog.show(this);
    }

    private boolean onVersionDev(View view) {
        return true;
    }

    private void setWallDefault(View view) {
        WallConfig.refresh(Setting.getWall() == 4 ? 1 : Setting.getWall() + 1);
    }

    private void setWallRefresh(View view) {
        Notify.progress(getActivity());
        WallConfig.get().load(new Callback() {
            @Override
            public void success() {
                Notify.dismiss();
                setCacheText();
            }
        });
    }

    private void setIncognito(View view) {
        boolean isChecked = !Setting.isIncognito();
        Setting.putIncognito(isChecked);
        // 不需要再次调用 setChecked，因为点击已经触发了状态变化
    }

    private void setLiveTabVisible(View view) {
        boolean isChecked = !Setting.isLiveTabVisible();
        Setting.putLiveTabVisible(isChecked);
        // 发送刷新事件，通知主界面更新导航栏
        RefreshEvent.config();
        // 更新直播设置项的可见性
        setLiveSettingsVisibility();
        // 不需要再次调用 setChecked，因为点击已经触发了状态变化
    }

    private void setSize(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_size).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(size, Setting.getSize(), (dialog, which) -> {
            mBinding.sizeText.setText(size[which]);
            Setting.putSize(which);
            RefreshEvent.size();
            dialog.dismiss();
        }).show();
    }

    private void setDoh(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_doh).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(getDohList(), getDohIndex(), (dialog, which) -> {
            setDoh(VodConfig.get().getDoh().get(which));
            dialog.dismiss();
        }).show();
    }

    private void setDoh(Doh doh) {
        Source.get().stop();
        OkHttp.get().setDoh(doh);
        Notify.progress(getActivity());
        Setting.putDoh(doh.toString());
        mBinding.dohText.setText(doh.getName());
        VodConfig.load(Config.vod(), getCallback(0));
    }

    private void onProxy(View view) {
        ProxyDialog.create(this).show();
    }

    @Override
    public void setProxy(String proxy) {
        Source.get().stop();
        Setting.putProxy(proxy);
        OkHttp.selector().clear();
        OkHttp.get().setProxy(proxy);
        Notify.progress(getActivity());
        mBinding.proxyText.setText(getProxy(proxy));
        VodConfig.load(Config.vod(), getCallback(0));
    }

    private void onCache(View view) {
        FileUtil.clearCache(new Callback() {
            @Override
            public void success() {
                setCacheText();
            }
        });
    }

    private void onBackup(View view) {
        AppDatabase.backup(new Callback() {
            @Override
            public void success() {
                Notify.show(R.string.backup_success);
            }

            @Override
            public void error() {
                Notify.show(R.string.backup_fail);
            }
        });
    }

    private void onRestore(View view) {
        RestoreDialog.create().show(getActivity(), new Callback() {
            @Override
            public void success() {
                Notify.show(R.string.restore_success);
                Notify.progress(getActivity());
                setOtherText();
                initConfig();
            }

            @Override
            public void error() {
                Notify.show(R.string.restore_fail);
            }
        });
    }

    private void initConfig() {
        WallConfig.get().init();
        LiveConfig.get().init().load();
        VodConfig.get().init().load(getCallback(0));
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) return;
        setSourceHintText(mBinding.vodUrl, VodConfig.getDesc(), R.string.source_hint_setting);
        setSourceHintText(mBinding.liveUrl, LiveConfig.getDesc(), R.string.source_hint_live);
        // setSourceHintText(mBinding.wallUrl, WallConfig.getDesc(), R.string.source_hint_wall); // 壁纸功能已移除
        setCacheText();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;
        if (requestCode == FileChooser.REQUEST_PICK_CONFIG_FILE) {
            // 处理配置文件选择：添加为配置源并刷新
            String filePath = FileChooser.getPathFromUri(getContext(), data.getData());
            if (filePath != null) {
                setConfig(Config.find("file:/" + filePath.replace(Path.rootPath(), ""), type));
            }
        } else if (requestCode == FileChooser.REQUEST_PICK_FILE) {
            // 处理其他文件选择
            setConfig(Config.find("file:/" + FileChooser.getPathFromUri(getContext(), data.getData()).replace(Path.rootPath(), ""), type));
        }
    }
}
