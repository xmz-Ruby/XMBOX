package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogConfigFileBinding;
import com.fongmi.android.tv.ui.adapter.ConfigFileAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.github.catvod.Init;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ConfigFileDialog extends BaseDialog implements ConfigFileAdapter.OnClickListener {

    private DialogConfigFileBinding binding;
    private ConfigFileAdapter adapter;
    private OnFileSelectedListener listener;
    private File currentDir;
    private File rootDir;

    public interface OnFileSelectedListener {
        void onFileSelected(File file);
    }

    public static ConfigFileDialog create() {
        return new ConfigFileDialog();
    }

    public void show(FragmentActivity activity, OnFileSelectedListener listener) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) {
            if (f instanceof BottomSheetDialogFragment) return;
        }
        show(activity.getSupportFragmentManager(), null);
        this.listener = listener;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogConfigFileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        rootDir = Init.context().getFilesDir();
        currentDir = rootDir;
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(false);
        binding.recycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        binding.recycler.setAdapter(adapter = new ConfigFileAdapter(this));
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.back.setOnClickListener(v -> onBackClick());
        loadFiles(currentDir);
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

        adapter.setItems(items, currentDir.equals(rootDir) ? null : currentDir.getParentFile());
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

    @Override
    public void onItemClick(File item) {
        if (item.isDirectory()) {
            loadFiles(item);
        } else {
            if (listener != null) {
                listener.onFileSelected(item);
            }
            dismiss();
        }
    }
}
