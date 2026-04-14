package com.example.d_linkmobilymanagement.data.remote.update

import com.example.d_linkmobilymanagement.data.model.remote.GitHubRelease
import retrofit2.Response
import retrofit2.http.GET

interface GitHubUpdateService {
    @GET("repos/DEVTE544/dsl-x1852e-android-app/releases/latest")
    suspend fun getLatestRelease(): Response<GitHubRelease>
}
