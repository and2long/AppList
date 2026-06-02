package com.and2long.applist

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.and2long.applist.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val TYPE_USER = 0
        const val TYPE_SYSTEM = 1
    }

    private val TAG = this.javaClass.simpleName

    private lateinit var appAdapter: AppAdapter
    private val mData = mutableListOf<AppInfo>()

    private var type = TYPE_USER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolBar.title = ""
        setSupportActionBar(binding.toolBar)

        val spinnerAdapter = ArrayAdapter.createFromResource(
            this, R.array.options_main, android.R.layout.simple_spinner_dropdown_item
        )
        binding.spinner.adapter = spinnerAdapter
        binding.spinner.onItemSelectedListener = this

        appAdapter = AppAdapter(mData)
        binding.appList.adapter = appAdapter
        binding.appList.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
        appAdapter.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val appInfo = mData[position]
                AlertDialog.Builder(this@MainActivity)
                    .setIcon(appInfo.appIcon)
                    .setTitle(appInfo.appName)
                    .setMessage("PackageName: " + appInfo.packageName + "\n\n" + "VersionName: " + appInfo.versionName + "\n\nVersionCode: " + appInfo.versionCode)
                    .setPositiveButton("OPEN") { dialog, _ ->
                        dialog?.dismiss()
                        packageManager.getLaunchIntentForPackage(appInfo.packageName)?.let(::startActivity)
                    }
                    .setNegativeButton("DETAIL") { dialog, _ ->
                        dialog?.dismiss()
                        goToAppDetail(mData[position].packageName)
                    }
                    .setNeutralButton("CANCEL") { dialog, _ -> dialog?.dismiss() }
                    .show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        showApps()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.i_refresh -> showApps()
        }
        return true
    }


    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.i(TAG, "nothing select")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        type = position
        showApps()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showApps() {
        binding.pb.visibility = View.VISIBLE
        lifecycleScope.launch {
            val appInfoList = withContext(Dispatchers.IO) {
                val result = mutableListOf<AppInfo>()
                try {
                    val packageInfoList = packageManager.getInstalledPackages(0)
                    val temp = when (type) {
                        TYPE_SYSTEM -> {
                            packageInfoList.filter {
                                val applicationInfo = it.applicationInfo ?: return@filter false
                                (ApplicationInfo.FLAG_SYSTEM and applicationInfo.flags) != 0
                            }
                        }

                        TYPE_USER -> {
                            packageInfoList.filter {
                                val applicationInfo = it.applicationInfo ?: return@filter false
                                (ApplicationInfo.FLAG_SYSTEM and applicationInfo.flags) == 0
                            }
                        }

                        else -> {
                            packageInfoList
                        }
                    }

                    temp.forEach {
                        val applicationInfo = it.applicationInfo ?: return@forEach
                        if (it.packageName != packageName) {
                            val appInfo = AppInfo()
                            appInfo.appName =
                                packageManager.getApplicationLabel(applicationInfo).toString()
                            appInfo.packageName = it.packageName
                            appInfo.versionName = it.versionName.orEmpty()
                            appInfo.versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                it.longVersionCode.toString()
                            } else {
                                @Suppress("DEPRECATION")
                                it.versionCode.toString()
                            }
                            appInfo.appIcon = applicationInfo.loadIcon(packageManager)
                            result.add(appInfo)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "获取应用包信息失败")
                }
                result.sortBy { it.appName }
                result
            }
            mData.clear()
            mData.addAll(appInfoList)
            appAdapter.notifyDataSetChanged()
            binding.pb.visibility = View.GONE
        }
    }

    private fun goToAppDetail(packageName: String) {
        try {
            // 通过程序的包名创建URI
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
