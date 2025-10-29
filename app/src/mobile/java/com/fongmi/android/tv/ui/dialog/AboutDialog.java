package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogAboutBinding;

public class AboutDialog extends BaseDialog {

    private DialogAboutBinding binding;

    public static void show(FragmentActivity activity) {
        new AboutDialog().show(activity.getSupportFragmentManager(), "AboutDialog");
    }

    public static void show(Fragment fragment) {
        new AboutDialog().show(fragment.getChildFragmentManager(), "AboutDialog");
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = DialogAboutBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected void initEvent() {
        // GitHub link removed
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 