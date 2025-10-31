package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.databinding.AdapterConfigFileBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConfigFileAdapter extends RecyclerView.Adapter<ConfigFileAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final SimpleDateFormat format;
    private final List<File> mItems;

    public ConfigFileAdapter(OnClickListener listener) {
        this.format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        this.mItems = new ArrayList<>();
        this.mListener = listener;
    }

    public interface OnClickListener {
        void onItemClick(File item);
    }

    private File parentDir;

    public void setItems(List<File> items, File parentDir) {
        this.parentDir = parentDir;
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterConfigFileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File item = mItems.get(position);

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

        holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterConfigFileBinding binding;

        ViewHolder(@NonNull AdapterConfigFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
