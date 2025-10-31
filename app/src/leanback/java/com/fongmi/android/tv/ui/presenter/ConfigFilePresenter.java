package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.databinding.AdapterConfigFileBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ConfigFilePresenter extends Presenter {

    private final OnClickListener mListener;
    private final SimpleDateFormat format;
    private File parentDir;

    public ConfigFilePresenter(OnClickListener listener) {
        this.mListener = listener;
        this.format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    public interface OnClickListener {
        void onItemClick(File item);
    }

    public void setParentDir(File parentDir) {
        this.parentDir = parentDir;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterConfigFileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        File item = (File) object;
        ViewHolder holder = (ViewHolder) viewHolder;

        if (item.equals(parentDir)) {
            holder.binding.name.setText("../");
            holder.binding.time.setText("返回上级目录");
        } else if (item.isDirectory()) {
            holder.binding.name.setText(item.getName() + "/");
            holder.binding.time.setText("文件夹");
        } else {
            holder.binding.name.setText(item.getName());
            holder.binding.time.setText(format.format(item.lastModified()));
        }

        holder.view.setOnClickListener(v -> mListener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterConfigFileBinding binding;

        public ViewHolder(@NonNull AdapterConfigFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
