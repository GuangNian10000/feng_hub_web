package com.fengke.robot.face;

import androidx.annotation.NonNull;

public class FaceData {
    public int id; // 兼容旧HTTP数据，arcface不使用
    public String faceId; // arcface 协议的人脸ID
    public String username;
    public String description;
    public String imageUrl;
    public String createTime;
    public String updateTime;
    
    @NonNull
    @Override
    public String toString() {
        return "FaceData{" +
                "id=" + id +
                ", faceId='" + faceId + '\'' +
                ", username='" + username + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", createTime='" + createTime + '\'' +
                ", updateTime='" + updateTime + '\'' +
                '}';
    }
    
    /**
     * 获取简要信息用于日志记录
     */
    public String getLogInfo() {
        return "FaceData[ID=" + id + ", FACE_ID=" + (faceId != null ? faceId : "") + ", 姓名=" + username + ", 图片=" + 
               imageUrl + "]";
    }
} 