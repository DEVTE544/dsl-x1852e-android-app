package com.example.d_linkmobilymanagement.utils

/**
 * ملف الإعدادات العام للتطبيق
 * */
object AppConfig {
    /**
     * رقم إصدار التطبيق الأساسي
     */
    const val VERSION_NAME = "1.15.1"
    
    /**
     * نص الإصدار الكامل الذي يظهر للمستخدم في واجهة الإعدادات
     */
    const val APP_FULL_VERSION = VERSION_NAME

    /**
     * إعدادات تحديث التطبيق من GitHub
     */
    const val GITHUB_REPO = "DEVTE544/dsl-x1852e-android-app"
    const val GITHUB_API_BASE_URL = "https://api.github.com/"
    const val APK_NAME = "app-release.apk"
    const val LATEST_RELEASE_DOWNLOAD_URL = "https://github.com/$GITHUB_REPO/releases/latest/download/$APK_NAME"
}
