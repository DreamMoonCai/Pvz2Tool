package io.github.dreammooncai.pvz2tool.service

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

@SuppressLint("VpnServicePolicy")
class LocalVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        // 使用 WeakReference 持有 Service 实例，防止内存泄漏，同时能直接调用内部方法
        private var serviceRef: WeakReference<LocalVpnService>? = null

        private val _isVpnActive = MutableStateFlow(false)
        val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

        fun prepareVpn(context: Context): Intent? = prepare(context)

        @Composable
        fun RequestPermissionsVpn(onRejected: () -> Unit = {}, onSuccess: () -> Unit = {}) {
            val context = LocalContext.current
            val vpnAuthLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    onSuccess()
                } else onRejected()
            }
            LaunchedEffect(vpnAuthLauncher) {
                val authIntent = prepareVpn(context)
                if (authIntent != null) {
                    vpnAuthLauncher.launch(authIntent)
                } else onSuccess()
            }
        }

        fun startVpn(context: Context) {
            if (prepare(context) == null) {
                context.startService(Intent(context, LocalVpnService::class.java))
            }
        }

        /**
         * 关键修复：彻底关闭VPN的入口
         */
        fun stopVpn(context: Context) {
            // 1. 先尝试通过 WeakReference 直接调用 Service 内部的销毁方法（最有效）
            serviceRef?.get()?.shutdownVpnInternally()
            // 2. 同时也调用 stopService，确保服务在系统层面被移除
            context.stopService(Intent(context, LocalVpnService::class.java))
            // 3. 强制更新状态
            _isVpnActive.value = false
        }

        suspend fun isNetworkConnected(testUrl: String = "https://www.baidu.com"): Boolean {
            return withContext(Dispatchers.IO) {
                var connection: HttpURLConnection? = null
                try {
                    val url = URL(testUrl)
                    connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "HEAD"
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.instanceFollowRedirects = true
                    connection.connect()
                    connection.responseCode in 200..399
                } catch (e: UnknownHostException) { false }
                catch (e: SocketTimeoutException) { false }
                catch (e: IOException) { false }
                finally { connection?.disconnect() }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // 保存 Service 实例引用
        serviceRef = WeakReference(this)
        establishVpnTunnel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    private fun establishVpnTunnel() {
        try {
            shutdownVpnInternally(notifyState = false)

            val builder = Builder()
                .addAddress("10.0.0.2", 32)
                .addAddress("fd00::1", 64)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDnsServer("10.0.0.1")
                .addDnsServer("fd00::2")
                .addAllowedApplication(packageName)
                .setSession("AppHardOfflineV2")
                .setMtu(1280)

            // ====== 核心修复区 ======
            // 1. 声明此 VPN 没有任何底层物理网络支持 (断绝系统的网络能力感知)
            // 这会导致底层网络检测库瞬间失败
            builder.setUnderlyingNetworks(emptyArray())
            // 2. 设置一个“无人监听”的本地代理 (端口设为 65535 或其他死端口)
            // 这会让所有 HTTP/HTTPS/Socket 请求瞬间触发 ECONNREFUSED (Connection refused) 异常，耗时 1 毫秒
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val proxyInfo = android.net.ProxyInfo.buildDirectProxy("127.0.0.1", 65535)
                builder.setHttpProxy(proxyInfo)
            }
            // =========================

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                _isVpnActive.value = true
                // 使用平滑的黑洞替代暴力关闭
                startSmoothBlackHole(vpnInterface!!)
            } else {
                stopSelf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    /**
     * 平滑黑洞：不断读取并丢弃数据，防止内核缓冲区塞满
     * 暴力关闭 descriptor 会导致系统不断尝试重试或引起不可预期的底层阻塞
     */
    private fun startSmoothBlackHole(descriptor: ParcelFileDescriptor) {
        serviceScope.launch {
            try {
                val input = FileInputStream(descriptor.fileDescriptor)
                val buffer = ByteArray(32767)
                while (true) {
                    // 不停地把发往 VPN 的包读出来并扔掉（不写入任何响应）
                    val length = input.read(buffer)
                    if (length < 0) break // 接口被关闭时退出循环
                }
            } catch (e: Exception) {
                // 静默处理，通常是 VPN 关闭时触发 IOException
            } finally {
                try { descriptor.close() } catch (e: IOException) {}
            }
        }
    }

    private fun shutdownVpnInternally(notifyState: Boolean = true) {
        if (notifyState) {
            _isVpnActive.value = false
        }
        try {
            vpnInterface?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            vpnInterface = null
        }
    }

    override fun onRevoke() {
        super.onRevoke()
        shutdownVpnInternally()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        shutdownVpnInternally()
        serviceScope.cancel()
        serviceRef?.clear()
        serviceRef = null
    }
}