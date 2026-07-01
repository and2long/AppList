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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.and2long.applist.ui.AppFilter
import com.and2long.applist.ui.AppListScreen
import com.and2long.applist.ui.AppListTheme
import java.text.DateFormat
import java.util.Date
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private var refreshToken by mutableIntStateOf(0)
    private val tag = this.javaClass.simpleName
    private val uninstallQueue = ArrayDeque<String>()
    private val uninstallLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        uninstallNextPackage()
        refreshToken++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppListTheme {
                AppListScreen(
                    refreshToken = refreshToken,
                    onRefresh = { refreshToken++ },
                    onLoadApps = ::loadApps,
                    onOpenApp = ::openApp,
                    onOpenDetail = ::goToAppDetail,
                    onShareApp = ::shareAppInfo,
                    onUninstallApps = ::uninstallApps
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
            val result = mutableListOf<Pair<Long, AppInfo>>()
            try {
                val packageInfoList = getInstalledPackages()
                logAppCounts(packageInfoList.mapNotNull { it.applicationInfo })
                val filteredPackages = when (type) {
                    AppFilter.SYSTEM -> {
                        packageInfoList.filter {
                            val applicationInfo = it.applicationInfo ?: return@filter false
                            applicationInfo.isSystemApp()
                        }
                    }

                    AppFilter.USER -> {
                        packageInfoList.filter {
                            val applicationInfo = it.applicationInfo ?: return@filter false
                            applicationInfo.isUserApp()
                        }
                    }

                    else -> {
                        packageInfoList.filter {
                            val applicationInfo = it.applicationInfo ?: return@filter false
                            applicationInfo.isUserApp()
                        }
                    }
                }

                filteredPackages.forEach {
                    val applicationInfo = it.applicationInfo ?: return@forEach
                    val signatureDigests = signatureDigests(it.packageName)
                    result.add(
                        it.lastUpdateTime to
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
                                firstInstallTime = formatPackageTime(it.firstInstallTime),
                                lastUpdateTime = formatPackageTime(it.lastUpdateTime),
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
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(tag, "获取应用包信息失败")
            }
            result.sortedByDescending { it.first }.map { it.second }
        }
    }

    private fun formatPackageTime(timeMillis: Long): String {
        if (timeMillis <= 0L) return "-"
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        return dateFormat.format(Date(timeMillis))
    }

    private fun getInstalledPackages() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_QUERY_FLAGS))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getInstalledPackages(PACKAGE_QUERY_FLAGS.toInt())
    }

    private fun ApplicationInfo.isUserApp(): Boolean {
        return !isSystemApp()
    }

    private fun logAppCounts(applicationInfoList: List<ApplicationInfo>) {
        val updatedSystemAppCount = applicationInfoList.count { it.isUpdatedSystemApp() }
        val systemAppCount = applicationInfoList.count { it.isSystemApp() }
        val userAppCount = applicationInfoList.count { it.isUserApp() }

        Log.i(
            tag,
            "应用统计: 系统应用 $systemAppCount, 更新过的系统应用 $updatedSystemAppCount, 用户程序 $userAppCount"
        )
    }

    private fun ApplicationInfo.isSystemApp(): Boolean {
        return hasSystemFlag() || isUpdatedSystemApp()
    }

    private fun ApplicationInfo.isUpdatedSystemApp(): Boolean {
        return (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    private fun ApplicationInfo.hasSystemFlag(): Boolean {
        return (flags and ApplicationInfo.FLAG_SYSTEM) != 0
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
                data = "package:$packageName".toUri()
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun uninstallApps(packageNames: List<String>) {
        uninstallQueue.clear()
        uninstallQueue.addAll(
            packageNames
                .distinct()
        )
        uninstallNextPackage()
    }

    @Suppress("DEPRECATION")
    private fun uninstallNextPackage() {
        val packageName = uninstallQueue.removeFirstOrNull() ?: return
        try {
            val intent = Intent(
                Intent.ACTION_UNINSTALL_PACKAGE,
                Uri.fromParts("package", packageName, null)
            ).apply {
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            uninstallLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            uninstallNextPackage()
        }
    }

    private fun shareAppInfo(appInfo: AppInfo) {
        val message = buildShareMessage(appInfo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, appInfo.appName)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun buildShareMessage(appInfo: AppInfo): String {
        return buildString {
            appendLine(appInfo.appName)
            appendLine()
            appendLine("${getString(R.string.package_name)}: ${appInfo.packageName}")
            appendLine("${getString(R.string.version_name)}: ${appInfo.versionName}")
            appendLine("${getString(R.string.version_code)}: ${appInfo.versionCode}")
            appendLine("${getString(R.string.first_install_time)}: ${appInfo.firstInstallTime}")
            appendLine("${getString(R.string.last_update_time)}: ${appInfo.lastUpdateTime}")
            appendLine("${getString(R.string.min_sdk_version)}: ${appInfo.minSdkVersion}")
            appendLine("${getString(R.string.target_sdk_version)}: ${appInfo.targetSdkVersion}")
            appendLine()
            appendLine(getString(R.string.signature))
            appendLine("${getString(R.string.md5)}: ${appInfo.signatureMd5}")
            appendLine("${getString(R.string.sha1)}: ${appInfo.signatureSha1}")
            appendLine("${getString(R.string.sha256)}: ${appInfo.signatureSha256}")
        }.trimEnd()
    }

    private companion object {
        private const val PACKAGE_QUERY_FLAGS =
            PackageManager.MATCH_DISABLED_COMPONENTS.toLong() or
                PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS.toLong()
    }
}
