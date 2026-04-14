package com.example.d_linkmobilymanagement.data.repository.update

import com.example.d_linkmobilymanagement.data.model.remote.UpdateInfo
import com.example.d_linkmobilymanagement.data.remote.update.GitHubUpdateService
import com.example.d_linkmobilymanagement.utils.AppConfig
import timber.log.Timber

class UpdateRepository(
    private val updateService: GitHubUpdateService
) {
    /**
     * يفحص إذا كان هناك تحديث متاح بمقارنة الإصدار المحلي مع GitHub
     */
    suspend fun checkForUpdate(): Result<UpdateInfo> {
        return try {
            val response = updateService.getLatestRelease()
            if (response.isSuccessful) {
                val release = response.body()
                if (release != null) {
                    val latestTag = release.tagName
                    val isUpdateAvailable = isVersionNewer(AppConfig.VERSION_NAME, latestTag)
                    
                    val apkAsset = release.assets.find { it.name == AppConfig.APK_NAME }
                    val downloadUrl = apkAsset?.downloadUrl ?: AppConfig.LATEST_RELEASE_DOWNLOAD_URL

                    Result.success(
                        UpdateInfo(
                            isUpdateAvailable = isUpdateAvailable,
                            latestTag = latestTag,
                            downloadUrl = downloadUrl
                        )
                    )
                } else {
                    Result.failure(Exception("No release data found"))
                }
            } else if (response.code() == 404) {
                // في حال عدم وجود أي Release في المستودع حتى الآن
                Result.success(
                    UpdateInfo(
                        isUpdateAvailable = false,
                        latestTag = AppConfig.VERSION_NAME,
                        downloadUrl = AppConfig.LATEST_RELEASE_DOWNLOAD_URL
                    )
                )
            } else {
                Result.failure(Exception("GitHub API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking for updates")
            Result.failure(e)
        }
    }

    /**
     * منطق مقارنة الإصدارات (يدعم تنسيق v1.0.0 أو 1.0.0)
     */
    private fun isVersionNewer(currentVersion: String, remoteTag: String): Boolean {
        val current = currentVersion.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val remote = remoteTag.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }

        val size = maxOf(current.size, remote.size)
        for (i in 0 until size) {
            val currPart = current.getOrElse(i) { 0 }
            val remPart = remote.getOrElse(i) { 0 }
            if (remPart > currPart) return true
            if (remPart < currPart) return false
        }
        return false
    }
}
