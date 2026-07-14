package com.example.guangnian.feature.face.data.repository

import com.example.guangnian.feature.console.data.manager.RobotConnectionManager
import com.example.guangnian.feature.face.data.bean.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import io.ktor.http.contentType

class FaceRepository(
    private val httpClient: HttpClient,
    private val connectionManager: RobotConnectionManager
) {
    private val baseUrl: String
        get() = "http://${connectionManager.ip.value}:8000"

    suspend fun getFaceList(): List<FaceData> {
        return try {
            val response: FaceListResponse = httpClient.get("$baseUrl/get_faces").body()
            response.faces?.map {
                FaceData(
                    username = it.name,
                    description = it.desc,
                    imageUrl = if (!it.image_url.isNullOrEmpty()) {
                        if (it.image_url.startsWith("http")) it.image_url else "$baseUrl${it.image_url}"
                    } else null
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteFace(name: String): CommonResponse {
        return httpClient.post("$baseUrl/delete_person") {
            setBody(MultiPartFormDataContent(
                formData {
                    append("name", name)
                }
            ))
        }.body()
    }

    suspend fun uploadFaceImage(name: String, description: String?, imageBytes: ByteArray): CommonResponse {
        return httpClient.post("$baseUrl/add_person") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("name", name)
                        description?.let { append("desc", it) }
                        append("file", imageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=\"face_image.jpg\"")
                        })
                    }
                )
            )
        }.body()
    }

    suspend fun getConfig(): FaceConfig {
        return try {
            httpClient.get("$baseUrl/config").body()
        } catch (e: Exception) {
            FaceConfig()
        }
    }

    suspend fun updateConfig(config: FaceConfig): CommonResponse {
        return httpClient.post("$baseUrl/config") {
            contentType(ContentType.Application.Json)
            setBody(config)
        }.body()
    }

    suspend fun getCurrentFrame(): ByteArray {
        return httpClient.get("$baseUrl/current_frame").body()
    }
}
