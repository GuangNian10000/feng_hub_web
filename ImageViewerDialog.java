package com.fengke.robot.face;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fengke.robot.R;
import com.fengke.robot.util.LogManager;
import io.getstream.photoview.PhotoView;

/**
 * 支持手势缩放的全屏图片查看器
 */
public class ImageViewerDialog extends Dialog {
    private static final String TAG = "ImageViewerDialog";
    private final String imageUrl;
    private PhotoView photoView;

    public ImageViewerDialog(@NonNull Context context, String imageUrl) {
        super(context, R.style.FullScreenDialog);
        this.imageUrl = imageUrl;
        LogManager.d(TAG, "创建图片查看器，图片URL: " + imageUrl);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogManager.d(TAG, "初始化图片查看器");
        
        setContentView(R.layout.dialog_image_viewer);
        
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            // 使用新的方式设置全屏，避免过时的FLAG_FULLSCREEN
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
            } else {
                int flags = WindowManager.LayoutParams.FLAG_FULLSCREEN;
                window.setFlags(flags, flags);
            }
        }
        
        initViews();
        loadImage();
    }

    private void initViews() {
        LogManager.d(TAG, "初始化视图组件");
        photoView = findViewById(R.id.photoView);
        ImageView btnClose = findViewById(R.id.btnClose);
        
        // 设置关闭按钮点击事件
        btnClose.setOnClickListener(v -> {
            LogManager.d(TAG, "点击关闭按钮");
            dismiss();
        });
        
        // 设置PhotoView点击事件（点击图片也可以关闭）
        photoView.setOnClickListener(v -> {
            LogManager.d(TAG, "点击图片关闭");
            dismiss();
        });
        
        LogManager.d(TAG, "PhotoView配置完成");
    }

    private void loadImage() {
        LogManager.d(TAG, "开始加载图片: " + imageUrl);
        
        if (imageUrl == null || imageUrl.isEmpty()) {
            LogManager.w(TAG, "图片URL为空，显示默认图片");
            photoView.setImageResource(R.drawable.ic_launcher_background);
            return;
        }
        
        Glide.with(getContext())
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .into(photoView);
        
        LogManager.d(TAG, "图片加载完成");
    }
} 