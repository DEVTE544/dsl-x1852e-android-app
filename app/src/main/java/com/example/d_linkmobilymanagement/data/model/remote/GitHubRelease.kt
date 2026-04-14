package com.example.d_linkmobilymanagement.data.model.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String
)

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestTag: String,
    val downloadUrl: String
)
