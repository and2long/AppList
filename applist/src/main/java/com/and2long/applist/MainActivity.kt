package com.and2long.applist

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.and2long.applist.ui.AppFilter
import com.and2long.applist.ui.AppListScreen
import com.and2long.applist.ui.AppListTheme
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var refreshToken by mutableIntStateOf(0)
    private val tag = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppListTheme {
                AppListScreen(
                    refreshToken = refreshToken,
                    onRefresh = { refreshToken++ },
                    onLoadApps = ::loadApps,
                    onOpenApp = ::openApp,
                    onOpenDetail = ::goToAppDetail
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshToken++
    }

    private suspend fun loadApps(type: Int): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<AppInfo>()
            try {
                val packageInfoList = packageManager.getInstalledPackages(0)
                val filteredPackages = when (type) {
                    AppFilter.SYSTEM -> {
                        packageInfoList.filter {
                            val applicationInfo = it.applicationInfo ?: return@filter false
                            (ApplicationInfo.FLAG_SYSTEM and applicationInfo.flags) != 0
                        }
                    }

                    AppFilter.USER -> {
                        packageInfoList.filter {
                            val applicationInfo = it.applicationInfo ?: return@filter false
                            (ApplicationInfo.FLAG_SYSTEM and applicationInfo.flags) == 0
                        }
                    }

                    else -> {
                        packageInfoList.filter {
                            val applicationInfo = it.applicationInfo ?: return@filter false
                            (ApplicationInfo.FLAG_SYSTEM and applicationInfo.flags) == 0
                        }
                    }
                }

                filteredPackages.forEach {
                    val applicationInfo = it.applicationInfo ?: return@forEach
                    if (it.packageName != packageName) {
                        val signatureDigests = signatureDigests(it.packageName)
                        result.add(
                            AppInfo(
                                appName = packageManager.getApplicationLabel(applicationInfo).toString(),
                                packageName = it.packageName,
                                versionName = it.versionName.orEmpty(),
                                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    it.longVersionCode.toString()
                                } else {
                                    @Suppress("DEPRECATION")
                                    it.versionCode.toString()
                                },
                                minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    applicationInfo.minSdkVersion.toString()
                                } else {
                                    "-"
                                },
                                targetSdkVersion = applicationInfo.targetSdkVersion.toString(),
                                signatureMd5 = signatureDigests.md5,
                                signatureSha1 = signatureDigests.sha1,
                                signatureSha256 = signatureDigests.sha256,
                                appIcon = applicationInfo.loadIcon(packageManager)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(tag, "获取应用包信息失败")
            }
            result.sortBy { it.appName }
            result
        }
    }

    private fun signatureDigests(packageName: String): SignatureDigests {
        val signatures = getPackageSignatures(packageName)
        if (signatures.isEmpty()) return SignatureDigests()

        return SignatureDigests(
            md5 = signatures.digest("MD5"),
            sha1 = signatures.digest("SHA-1"),
            sha256 = signatures.digest("SHA-256")
        )
    }

    private fun Array<Signature>.digest(algorithm: String): String {
        return joinToString(separator = "\n") { signature ->
            MessageDigest.getInstance(algorithm)
                .digest(signature.toByteArray())
                .joinToString(separator = ":") { byte -> "%02X".format(byte.toInt() and 0xFF) }
        }
    }

    private fun getPackageSignatures(packageName: String): Array<Signature> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                }
                val signingInfo = packageInfo.signingInfo ?: return emptyArray()
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory ?: signingInfo.apkContentsSigners
                }
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
                    ?: emptyArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyArray()
        }
    }

    private data class SignatureDigests(
        val md5: String = "-",
        val sha1: String = "-",
        val sha256: String = "-"
    )

    private fun openApp(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let(::startActivity)
    }

    private fun goToAppDetail(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
