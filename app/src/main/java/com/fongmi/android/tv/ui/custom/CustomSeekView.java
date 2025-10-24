package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.Util;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.TimeBar;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.Players;

import java.util.concurrent.TimeUnit;

public class CustomSeekView extends FrameLayout implements TimeBar.OnScrubListener {

    private static final int MAX_UPDATE_INTERVAL_MS = 1000;
    private static final int MIN_UPDATE_INTERVAL_MS = 200;

    private TextView positionView;
    private TextView durationView;
    private DefaultTimeBar timeBar;

    private Runnable refresh;
    private Players player;

    private long currentDuration;
    private long currentPosition;
    private long currentBuffered;
    private boolean scrubbing;
    private boolean isPressed;

    public CustomSeekView(Context context) {
        this(context, null);
    }

    public CustomSeekView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomSeekView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_control_seek, this);
        init();
        start();
    }

    private void init() {
        positionView = findViewById(R.id.position);
        durationView = findViewById(R.id.duration);
        timeBar = findViewById(R.id.timeBar);
        timeBar.addListener(this);
        refresh = this::refresh;
        
        // 设置触摸事件监听器，实现动态尺寸调整
        timeBar.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!isPressed) {
                        isPressed = true;
                        // 按下时：滑杆4dp，圆球16dp
                        setTimeBarSize(4, 16);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isPressed) {
                        isPressed = false;
                        // 松开时：滑杆2dp，圆球12dp
                        setTimeBarSize(2, 12);
                    }
                    break;
            }
            return false; // 不拦截事件，让DefaultTimeBar正常处理
        });
    }

    public void setListener(Players player) {
        this.player = player;
    }
    
    public void setPosition(long position) {
        timeBar.setPosition(position);
    }
    
    public void setDuration(long duration) {
        timeBar.setDuration(duration);
    }
    
    /**
     * 动态设置进度条高度和拖拽手柄大小
     * @param barHeightDp 滑杆高度值（dp）
     * @param scrubberSizeDp 拖拽手柄大小（dp）
     */
    private void setTimeBarSize(int barHeightDp, int scrubberSizeDp) {
        // 设置滑杆高度
        int barHeightPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 
                barHeightDp, 
                getContext().getResources().getDisplayMetrics()
        );
        
        // 设置拖拽手柄大小
        int scrubberSizePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 
                scrubberSizeDp, 
                getContext().getResources().getDisplayMetrics()
        );
        
        // 通过反射设置DefaultTimeBar的内部属性
        try {
            // 设置滑杆高度
            java.lang.reflect.Field barHeightField = timeBar.getClass().getDeclaredField("barHeight");
            barHeightField.setAccessible(true);
            barHeightField.setInt(timeBar, barHeightPx);
            
            // 设置拖拽手柄大小 - 尝试多个可能的字段名
            String[] scrubberFields = {"scrubberSize", "scrubberEnabledSize", "scrubberDisabledSize"};
            for (String fieldName : scrubberFields) {
                try {
                    java.lang.reflect.Field scrubberField = timeBar.getClass().getDeclaredField(fieldName);
                    scrubberField.setAccessible(true);
                    scrubberField.setInt(timeBar, scrubberSizePx);
                    break; // 成功设置后退出循环
                } catch (NoSuchFieldException e) {
                    // 继续尝试下一个字段名
                }
            }
            
            // 刷新视图
            timeBar.requestLayout();
            timeBar.invalidate();
        } catch (Exception e) {
            // 如果反射失败，使用备用方案
            e.printStackTrace();
            // 备用方案：重新设置布局参数
            timeBar.getLayoutParams().height = barHeightPx;
            timeBar.requestLayout();
        }
    }

    private void start() {
        removeCallbacks(refresh);
        post(refresh);
    }

    private void refresh() {
        long duration = player.getDuration();
        long position = player.getPosition();
        long buffered = player.getBuffered();
        boolean positionChanged = position != currentPosition;
        boolean durationChanged = duration != currentDuration;
        boolean bufferedChanged = buffered != currentBuffered;
        currentDuration = duration;
        currentPosition = position;
        currentBuffered = buffered;
        if (durationChanged) {
            setKeyTimeIncrement(duration);
            timeBar.setDuration(duration);
            durationView.setText(player.stringToTime(duration < 0 ? 0 : duration));
        }
        if (positionChanged && !scrubbing) {
            timeBar.setPosition(position);
            positionView.setText(player.stringToTime(position < 0 ? 0 : position));
        }
        if (bufferedChanged) {
            timeBar.setBufferedPosition(buffered);
        }
        removeCallbacks(refresh);
        if (player.isEmpty()) {
            positionView.setText("00:00");
            durationView.setText("00:00");
            timeBar.setPosition(currentPosition = 0);
            timeBar.setDuration(currentDuration = 0);
            postDelayed(refresh, MIN_UPDATE_INTERVAL_MS);
        } else if (player.isPlaying()) {
            postDelayed(refresh, delayMs(position));
        } else {
            postDelayed(refresh, MAX_UPDATE_INTERVAL_MS);
        }
    }

    public void setKeyTimeIncrement(long duration) {
        if (duration > TimeUnit.HOURS.toMillis(3)) {
            timeBar.setKeyTimeIncrement(TimeUnit.MINUTES.toMillis(5));
        } else if (duration > TimeUnit.MINUTES.toMillis(30)) {
            timeBar.setKeyTimeIncrement(TimeUnit.MINUTES.toMillis(1));
        } else if (duration > TimeUnit.MINUTES.toMillis(15)) {
            timeBar.setKeyTimeIncrement(TimeUnit.SECONDS.toMillis(30));
        } else if (duration > TimeUnit.MINUTES.toMillis(10)) {
            timeBar.setKeyTimeIncrement(TimeUnit.SECONDS.toMillis(15));
        } else if (duration > 0) {
            timeBar.setKeyTimeIncrement(TimeUnit.SECONDS.toMillis(10));
        }
    }

    private long delayMs(long position) {
        long mediaTimeUntilNextFullSecondMs = 1000 - position % 1000;
        long mediaTimeDelayMs = Math.min(timeBar.getPreferredUpdateDelay(), mediaTimeUntilNextFullSecondMs);
        long delayMs = (long) (mediaTimeDelayMs / player.getSpeed());
        return Util.constrainValue(delayMs, MIN_UPDATE_INTERVAL_MS, MAX_UPDATE_INTERVAL_MS);
    }

    private void seekToTimeBarPosition(long positionMs) {
        // 先设置播放位置
        player.seekTo(positionMs);
        // 延迟刷新进度条，确保播放器已经处理了跳转操作
        removeCallbacks(refresh);
        postDelayed(() -> {
            // 只有在非拖动状态下才刷新进度条位置
            if (!scrubbing) {
                refresh();
                // 确保进度条位置与实际播放位置一致
                long actualPosition = player.getPosition();
                if (Math.abs(actualPosition - positionMs) > 100) { // 如果差异超过100ms，再次调整
                    timeBar.setPosition(actualPosition);
                    positionView.setText(player.stringToTime(actualPosition));
                }
            }
        }, 50); // 延迟50ms刷新
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(refresh);
    }

    @Override
    public void onScrubStart(@NonNull TimeBar timeBar, long position) {
        scrubbing = true;
        positionView.setText(player.stringToTime(position));
    }

    @Override
    public void onScrubMove(@NonNull TimeBar timeBar, long position) {
        positionView.setText(player.stringToTime(position));
    }

    @Override
    public void onScrubStop(@NonNull TimeBar timeBar, long position, boolean canceled) {
        scrubbing = false;
        if (!canceled) {
            // 立即设置进度条位置到目标位置，避免圆球跳回原始位置
            timeBar.setPosition(position);
            positionView.setText(player.stringToTime(position));
            
            // 调整播放位置
            seekToTimeBarPosition(position);
            // 确保播放状态正确
            if (!player.isPlaying()) {
                player.play();
            }
        }
    }
}
