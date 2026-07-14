# FengAI HTTP API 接口文档

本文档描述了人脸识别系统的 HTTP 辅助接口。

*   **基础 URL**: `http://[Server_IP]:8000`
*   **内容类型**: 
    *   常规请求/响应使用 `application/json`
    *   上传人脸或删除人脸使用 `multipart/form-data` (Form Data)

---

## 1. 系统配置管理

### 1.1 获取当前配置
*   **方法**: `GET`
*   **路径**: `/config`
*   **请求参数**: 无
*   **响应结构 (JSON)**:
    ```json
    {
      "threshold": 0.8,
      "camera_flip": true,
      "only_known": false
    }
    ```

### 1.2 更新系统配置
*   **方法**: `POST`
*   **路径**: `/config`
*   **请求结构 (JSON)**:
    ```json
    {
      "threshold": 0.85,
      "camera_flip": false,
      "only_known": true
    }
    ```
*   **响应结构 (JSON)**:
    ```json
    {
      "status": "success",
      "message": "配置已生效"
    }
    ```

---

## 2. 人脸库管理

### 2.1 获取所有人脸列表
*   **方法**: `GET`
*   **路径**: `/get_faces`
*   **请求参数**: 无
*   **响应结构 (JSON)**:
    ```json
    {
      "faces": [
        {
          "name": "张三",
          "desc": "技术部工程师",
          "image_url": "/images/%E5%BC%A0%E4%B8%89/1715423456.jpg"
        },
        {
          "name": "李四",
          "desc": "访客",
          "image_url": "/images/%E6%9D%8E%E5%9B%9B/1715423789.jpg"
        }
      ]
    }
    ```

### 2.2 录入/更新人脸 (上传文件)
*   **方法**: `POST`
*   **路径**: `/add_person`
*   **请求类型**: `multipart/form-data`
*   **请求参数 (Form)**:
    *   `name`: 姓名 (必填, String)
    *   `desc`: 描述 (可选, String)
    *   `file`: 图片文件 (必填, File)
*   **响应结构 (JSON)**:
    ```json
    {
      "status": "success",
      "message": "成功将 张三 新增录入 到特征库！"
    }
    ```

### 2.3 删除人脸
*   **方法**: `POST`
*   **路径**: `/delete_person`
*   **请求类型**: `application/x-www-form-urlencoded` 或 `multipart/form-data`
*   **请求参数 (Form)**:
    *   `name`: 姓名 (必填, String)
*   **响应结构 (JSON)**:
    *   **成功 (200)**:
        ```json
        {
          "status": "success",
          "message": "已彻底从特征库和磁盘删除 张三"
        }
        ```
    *   **失败 (404)**:
        ```json
        {
          "status": "error",
          "message": "未找到名为 张三 的人脸"
        }
        ```

---

## 3. 实时画面预览

### 3.1 获取当前摄像头帧
*   **方法**: `GET`
*   **路径**: `/current_frame`
*   **响应**: 
    *   成功：返回 `image/jpeg` 类型的二进制图片流。
    *   失败：返回 `application/json` 错误提示。
*   **错误响应 (JSON)**:
    ```json
    {
      "error": "摄像头尚未捕获到画面"
    }
    ```

---

## 4. 静态资源访问
*   **路径**: `/images/{name}/{filename}`
*   **描述**: 录入人脸时存储的原始图片可通过此路径直接访问（对应 `/get_faces` 返回的 `image_url`）。
