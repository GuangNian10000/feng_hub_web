package com.example.guangnian.feature.face.data.repository

import com.example.guangnian.core.utils.KLog
import com.example.guangnian.feature.console.data.manager.RobotConnectionManager
import com.example.guangnian.feature.face.data.bean.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.util.encodeBase64
import kotlinx.datetime.Clock

class FaceRepository(
    private val httpClient: HttpClient,
    private val connectionManager: RobotConnectionManager
) {
    private companion object {
        const val TAG = "FaceRepository"
    }

    private val baseUrl: String
        get() = "http://${connectionManager.ip.value}:5001"

    suspend fun getFaceList(): List<FaceData> {
        val startTime = Clock.System.now().toEpochMilliseconds()
        return try {
            val response: FaceListResponse = httpClient.get("$baseUrl/get_faces").body()
            val faces = response.faces?.map {
                FaceData(
                    username = it.name,
                    description = it.desc,
                    imageUrl = if (!it.image_url.isNullOrEmpty()) {
                        if (it.image_url.startsWith("http")) it.image_url else "$baseUrl${it.image_url}"
                    } else null
                )
            } ?: emptyList()
            KLog.d(TAG, "获取人脸列表成功: url=$baseUrl/get_faces, count=${faces.size}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            faces
        } catch (e: Exception) {
            KLog.w(TAG, "获取人脸列表失败，返回空列表: url=$baseUrl/get_faces, error=${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
            emptyList()
        }
    }

    suspend fun deleteFace(name: String): CommonResponse {
        val startTime = Clock.System.now().toEpochMilliseconds()
        return try {
            val response: CommonResponse = httpClient.post("$baseUrl/delete_person") {
                setBody(MultiPartFormDataContent(
                    formData {
                        append("name", name)
                    }
                ))
            }.body()
            if (response.status.lowercase() !in listOf("success", "ok")) {
                KLog.w(TAG, "删除人脸失败，服务器返回错误: name=$name, status=${response.status}, message=${response.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
                throw Exception(response.message.ifEmpty { "删除失败: status=${response.status}" })
            }
            KLog.d(TAG, "删除人脸成功: name=$name, status=${response.status}, message=${response.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            response
        } catch (e: Exception) {
            KLog.w(TAG, "删除人脸失败: name=$name, error=${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
            throw e
        }
    }

    suspend fun uploadFaceImage(name: String, description: String, imageBytes: ByteArray): CommonResponse {
        val startTime = Clock.System.now().toEpochMilliseconds()
        return try {
            KLog.d(TAG, "开始上传人脸图片(Multipart): name=$name, desc=$description, bytes=${imageBytes.size}")

            val response: CommonResponse = httpClient.post("$baseUrl/add_person") {
                setBody(MultiPartFormDataContent(
                    formData {
                        append("name", name)
                        if (description.isNotBlank()) {
                            append("desc", description)
                        }
                        val isPng = imageBytes.size > 4 &&
                                imageBytes[0] == 0x89.toByte() &&
                                imageBytes[1] == 0x50.toByte() &&
                                imageBytes[2] == 0x4E.toByte() &&
                                imageBytes[3] == 0x47.toByte()
                        val contentType = if (isPng) "image/png" else "image/jpeg"
                        val extension = if (isPng) "png" else "jpg"

                        append("file", imageBytes, Headers.build {
                            append(HttpHeaders.ContentType, contentType)
                            append(HttpHeaders.ContentDisposition, "filename=\"face.$extension\"")
                        })
                    }
                ))
            }.body()
            
            if (response.status.lowercase() !in listOf("success", "ok")) {
                KLog.w(TAG, "上传人脸图片失败，服务器返回错误: name=$name, status=${response.status}, message=${response.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
                throw Exception(response.message.ifEmpty { "上传失败: status=${response.status}" })
            }
            KLog.d(TAG, "上传人脸图片成功: name=$name, status=${response.status}, message=${response.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            response
        } catch (e: Exception) {
            KLog.w(TAG, "上传人脸图片失败: name=$name, error=${e.message}", e)
            throw e
        }
    }

    suspend fun getConfig(): FaceConfig {
        val startTime = Clock.System.now().toEpochMilliseconds()
        return try {
            val config: FaceConfig = httpClient.get("$baseUrl/face/config").body()
            KLog.d(TAG, "获取人脸配置成功: threshold=${config.threshold}, camera_flip=${config.camera_flip}, only_known=${config.only_known}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            config
        } catch (e: Exception) {
            KLog.w(TAG, "获取人脸配置失败，使用默认配置: error=${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
            FaceConfig()
        }
    }

    suspend fun updateConfig(config: FaceConfig): CommonResponse {
        val startTime = Clock.System.now().toEpochMilliseconds()
        return try {
            val response: CommonResponse = httpClient.post("$baseUrl/face/config") {
                contentType(ContentType.Application.Json)
                setBody(config)
            }.body()
            if (response.status.lowercase() !in listOf("success", "ok")) {
                KLog.w(TAG, "更新人脸配置失败，服务器返回错误: threshold=${config.threshold}, cameraFlip=${config.camera_flip}, onlyKnown=${config.only_known}, status=${response.status}, message=${response.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
                throw Exception(response.message.ifEmpty { "配置更新失败: status=${response.status}" })
            }
            KLog.d(TAG, "更新人脸配置成功: threshold=${config.threshold}, cameraFlip=${config.camera_flip}, onlyKnown=${config.only_known}, status=${response.status}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            response
        } catch (e: Exception) {
            KLog.w(TAG, "更新人脸配置失败: threshold=${config.threshold}, cameraFlip=${config.camera_flip}, onlyKnown=${config.only_known}, error=${e.message}", e)
            throw e
        }
    }

    suspend fun getCurrentFrame(): ByteArray {
        val startTime = Clock.System.now().toEpochMilliseconds()
        return try {
            val response = httpClient.get("$baseUrl/face/camera/snapshot.jpg")
            val bytes: ByteArray = response.body()

            // 校验返回的数据大小。日志显示异常时只有 22 字节，这通常是错误 JSON
            if (bytes.size < 200) {
                val content = bytes.decodeToString()
                if (content.contains("error") || content.startsWith("{")) {
                    KLog.w(TAG, "获取画面失败，机器人返回错误: $content")
                    throw Exception("机器人画面未就绪: $content")
                }
            }

            KLog.d(TAG, "获取当前画面成功: bytes=${bytes.size}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms")
            bytes
        } catch (e: Exception) {
            KLog.w(TAG, "获取当前画面失败: error=${e.message}, 耗时=${Clock.System.now().toEpochMilliseconds() - startTime}ms", e)
            throw e
        }
    }
}
