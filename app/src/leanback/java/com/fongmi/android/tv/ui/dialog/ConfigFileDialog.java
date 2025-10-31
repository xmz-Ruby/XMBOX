package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;

import com.fongmi.android.tv.databinding.DialogConfigFileBinding;
import com.fongmi.android.tv.ui.presenter.ConfigFilePresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.Init;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ConfigFileDialog implements ConfigFilePresenter.OnClickListener {

    private final DialogConfigFileBinding binding;
    private final ConfigFilePresenter presenter;
    private final ArrayObjectAdapter adapter;
    private final AlertDialog dialog;
    private OnFileSelectedListener listener;
    private File currentDir;
    private File rootDir;

    public interface OnFileSelectedListener {
        void onFileSelected(File file);
    }

    public static ConfigFileDialog create(Activity activity) {
        return new ConfigFileDialog(activity);
    }

    public ConfigFileDialog(Activity activity) {
        this.binding = DialogConfigFileBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        this.presenter = new ConfigFilePresenter(this);
        this.adapter = new ArrayObjectAdapter(presenter);
    }

    public void show(OnFileSelectedListener listener) {
        this.listener = listener;
        rootDir = Init.context().getFilesDir();
        currentDir = rootDir;
        setRecyclerView();
        binding.back.setOnClickListener(v -> onBackClick());
        loadFiles(currentDir);
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(new ItemBridgeAdapter(adapter));
        binding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
    }

    private void loadFiles(File dir) {
        currentDir = dir;
        List<File> items = new ArrayList<>();

        // Add parent directory item if not at root
        if (!currentDir.equals(rootDir)) {
            items.add(currentDir.getParentFile());
        }

        if (dir.exists() && dir.isDirectory()) {
            File[] fileArray = dir.listFiles();
            if (fileArray != null) {
                List<File> dirs = new ArrayList<>();
                List<File> files = new ArrayList<>();

                for (File file : fileArray) {
                    if (file.isDirectory()) {
                        dirs.add(file);
                    } else if (file.getName().endsWith(".json") ||
                               file.getName().endsWith(".txt") ||
                               file.getName().endsWith(".xml")) {
                        files.add(file);
                    }
                }

                // Sort directories and files separately
                Collections.sort(dirs, Comparator.comparing(File::getName));
                Collections.sort(files, Comparator.comparing(File::getName));

                items.addAll(dirs);
                items.addAll(files);
            }
        }

        presenter.setParentDir(currentDir.equals(rootDir) ? null : currentDir.getParentFile());
        adapter.clear();
        adapter.addAll(items);
        binding.recycler.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        binding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.back.setVisibility(currentDir.equals(rootDir) ? View.GONE : View.VISIBLE);
        binding.path.setText(currentDir.getAbsolutePath());
    }

    private void onBackClick() {
        if (!currentDir.equals(rootDir)) {
            loadFiles(currentDir.getParentFile());
        }
    }

    private void setDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * 0.5f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    @Override
    public void onItemClick(File item) {
        if (item.isDirectory()) {
            loadFiles(item);
        } else {
            if (listener != null) {
                listener.onFileSelected(item);
            }
            dialog.dismiss();
        }
    }
}
