package com.and2long.applist

import android.graphics.drawable.Drawable

data class AppInfo(
    var appName: String = "",
    var packageName: String = "",
    var versionName: String = "",
    var versionCode: String = "",
    var minSdkVersion: String = "",
    var targetSdkVersion: String = "",
    var signatureMd5: String = "",
    var signatureSha1: String = "",
    var signatureSha256: String = "",
    var appIcon: Drawable? = null
)
