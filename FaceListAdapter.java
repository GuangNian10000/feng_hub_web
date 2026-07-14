package com.fengke.robot.face;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fengke.robot.R;
import com.fengke.robot.util.LogManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FaceListAdapter extends RecyclerView.Adapter<FaceListAdapter.VH> {
    private static final String TAG = "FaceListAdapter";
    private List<FaceData> data = new ArrayList<>();
    private OnEditListener onEditListener;
    private OnDeleteListener onDeleteListener;
    
    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<FaceData> d) { 
        LogManager.d(TAG, "=== 更新适配器数据 ===");
        LogManager.d(TAG, "新数据数量: " + (d != null ? d.size() : 0));
        LogManager.d(TAG, "原数据数量: " + data.size());
        
        data = d != null ? d : new ArrayList<>();
        notifyDataSetChanged();
        
        LogManager.d(TAG, "适配器数据更新完成，当前数据量: " + data.size());
    }
    
    public void setOnEditListener(OnEditListener l) { 
        LogManager.d(TAG, "设置编辑监听器: " + (l != null ? "已设置" : "null"));
        onEditListener = l; 
    }
    
    public void setOnDeleteListener(OnDeleteListener l) { 
        LogManager.d(TAG, "设置删除监听器: " + (l != null ? "已设置" : "null"));
        onDeleteListener = l; 
    }
    
    @NonNull
    @Override 
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        LogManager.d(TAG, "创建ViewHolder");
        View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_face, p, false);
        return new VH(view);
    }
    
    @Override 
    public void onBindViewHolder(@NonNull VH h, int i) {
        LogManager.d(TAG, "绑定ViewHolder，位置: " + i);
        
        if (i >= data.size()) {
            LogManager.w(TAG, "位置超出数据范围: " + i + " >= " + data.size());
            return;
        }
        
        FaceData fd = data.get(i);
        LogManager.d(TAG, "绑定数据: " + fd.getLogInfo());
        
        h.tvName.setText(fd.username);
        h.tvDesc.setText(fd.description);
        
        // 图片URL：imageUrl 存储的是HTTP访问地址（如：http://设备IP:8080/api/face/image/face_1）
        if (fd.imageUrl != null && !fd.imageUrl.isEmpty()) {
            LogManager.d(TAG, "加载HTTP图片: " + fd.imageUrl);
            // 使用Glide从HTTP URL加载图片
                Glide.with(h.ivFace.getContext())
                    .load(fd.imageUrl)
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .override(200, 200)
                        .centerCrop()
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                        .skipMemoryCache(false)
                        .into(h.ivFace);
                
                // 设置图片点击事件，打开全屏查看器
                h.ivFace.setOnClickListener(v -> {
                    LogManager.d(TAG, "点击图片查看大图: " + fd.getLogInfo());
                    ImageViewerDialog dialog = new ImageViewerDialog(h.ivFace.getContext(), fd.imageUrl);
                    dialog.show();
                });
            } else {
            LogManager.d(TAG, "没有图片URL，显示默认图片");
            h.ivFace.setImageResource(R.drawable.ic_launcher_background);
            h.ivFace.setOnClickListener(null);
        }
        
        // 设置按钮点击事件
        h.btnEdit.setOnClickListener(v -> { 
            LogManager.d(TAG, "点击编辑按钮: " + fd.getLogInfo());
            if (onEditListener != null) {
                onEditListener.onEdit(fd);
            } else {
                LogManager.w(TAG, "编辑监听器为null");
            }
        });
        
        h.btnDelete.setOnClickListener(v -> { 
            LogManager.d(TAG, "点击删除按钮: " + fd.getLogInfo());
            if (onDeleteListener != null) {
                onDeleteListener.onDelete(fd);
            } else {
                LogManager.w(TAG, "删除监听器为null");
            }
        });
    }
    
    @Override 
    public int getItemCount() { 
        int count = data.size();
        LogManager.d(TAG, "获取数据项数量: " + count);
        return count;
    }
    
    public static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc;
        ImageView ivFace;
        Button btnEdit, btnDelete;
        
        VH(View v) {
            super(v);
            LogManager.d(TAG, "初始化ViewHolder");
            tvName = v.findViewById(R.id.tvName);
            tvDesc = v.findViewById(R.id.tvDesc);
            ivFace = v.findViewById(R.id.ivFace);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
    
    public interface OnEditListener { 
        void onEdit(FaceData d); 
    }
    
    public interface OnDeleteListener { 
        void onDelete(FaceData d); 
    }
} 