package com.example.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.example.BuildConfig
import com.example.IShellService
import com.example.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

enum class ShizukuStatus {
    NOT_RUNNING,
    NO_PERMISSION,
    CONNECTING,
    READY
}

data class ShellCommandResult(
    val exitCode: Int,
    val output: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}

class ShizukuManager(
    private val context: Context,
    private val localizedContextProvider: () -> Context = { context }
) {

    companion object {
        private const val REQUEST_CODE_SHIZUKU = 3090
    }

    private val _status = MutableStateFlow(ShizukuStatus.NOT_RUNNING)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val _shizukuVersion = MutableStateFlow<Int?>(null)
    val shizukuVersion: StateFlow<Int?> = _shizukuVersion.asStateFlow()

    private val bindMutex = Mutex()
    private var shellService: IShellService? = null
    private var serviceDeferred: CompletableDeferred<IShellService?>? = null
    private var isBound = false

    private val userServiceArgs: Shizuku.UserServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShellUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shell_service")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val bound = if (service != null) IShellService.Stub.asInterface(service) else null
            shellService = bound
            isBound = bound != null
            if (bound != null) {
                _status.value = ShizukuStatus.READY
            } else {
                refreshStatus()
            }
            serviceDeferred?.complete(bound)
            serviceDeferred = null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shellService = null
            isBound = false
            serviceDeferred?.complete(null)
            serviceDeferred = null
            refreshStatus()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshStatus()
        if (hasPermission()) {
            ensureServiceBoundAsync()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        shellService = null
        isBound = false
        _shizukuVersion.value = null
        _status.value = ShizukuStatus.NOT_RUNNING
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_SHIZUKU) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    refreshStatus()
                    ensureServiceBoundAsync()
                } else {
                    refreshStatus()
                }
            }
        }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) {
        }
        refreshStatus()
    }

    fun isBinderAlive(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            if (!isBinderAlive()) return false
            if (Shizuku.isPreV11()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun requestPermission() {
        try {
            if (!isBinderAlive()) {
                _status.value = ShizukuStatus.NOT_RUNNING
                return
            }
            if (Shizuku.isPreV11()) {
                _status.value = ShizukuStatus.NO_PERMISSION
                return
            }
            if (hasPermission()) {
                refreshStatus()
                ensureServiceBoundAsync()
                return
            }
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
        } catch (_: Throwable) {
            refreshStatus()
        }
    }

    fun refreshStatus() {
        val alive = isBinderAlive()
        if (!alive) {
            _shizukuVersion.value = null
            _status.value = ShizukuStatus.NOT_RUNNING
            return
        }
        _shizukuVersion.value = try {
            Shizuku.getVersion()
        } catch (_: Throwable) {
            null
        }
        val granted = hasPermission()
        _status.value = when {
            !granted -> ShizukuStatus.NO_PERMISSION
            shellService?.asBinder()?.isBinderAlive == true -> ShizukuStatus.READY
            else -> ShizukuStatus.CONNECTING
        }
    }

    private fun ensureServiceBoundAsync() {
        try {
            if (!hasPermission()) return
            if (shellService?.asBinder()?.isBinderAlive == true) {
                _status.value = ShizukuStatus.READY
                return
            }
            _status.value = ShizukuStatus.CONNECTING
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (_: Throwable) {
            refreshStatus()
        }
    }

    suspend fun getOrBindService(): IShellService? = bindMutex.withLock {
        val current = shellService
        if (current != null && current.asBinder().isBinderAlive) {
            _status.value = ShizukuStatus.READY
            return current
        }
        if (!isBinderAlive() || !hasPermission()) {
            refreshStatus()
            return null
        }

        val deferred = CompletableDeferred<IShellService?>()
        serviceDeferred = deferred
        _status.value = ShizukuStatus.CONNECTING
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (_: Throwable) {
            serviceDeferred = null
            refreshStatus()
            return null
        }

        val bound = withTimeoutOrNull(4500L) {
            deferred.await()
        }
        if (bound == null) {
            serviceDeferred = null
            refreshStatus()
        }
        return bound
    }

    suspend fun executeCommand(command: String): ShellCommandResult = withContext(Dispatchers.IO) {
        val loc = localizedContextProvider()
        val service = getOrBindService()
            ?: return@withContext ShellCommandResult(-1, loc.getString(R.string.msg_shizuku_not_ready))
        return@withContext try {
            val raw = service.executeCommand(command)
                ?: "EXIT:-1\n${loc.getString(R.string.msg_shell_no_reply)}"
            val firstNewLine = raw.indexOf('\n')
            val header = if (firstNewLine == -1) raw else raw.substring(0, firstNewLine)
            val body = if (firstNewLine == -1) "" else raw.substring(firstNewLine + 1)
            val code = header.removePrefix("EXIT:").trim().toIntOrNull() ?: -1
            ShellCommandResult(code, body)
        } catch (t: Throwable) {
            shellService = null
            isBound = false
            refreshStatus()
            ShellCommandResult(-1, t.message ?: loc.getString(R.string.msg_shell_service_error))
        }
    }

    suspend fun grantPersistentPermissions(): ShellCommandResult {
        val pkg = context.packageName
        val commands = listOf(
            "pm grant $pkg android.permission.WRITE_SECURE_SETTINGS",
            "appops set $pkg WRITE_SETTINGS allow",
            "appops set $pkg GET_USAGE_STATS allow"
        ).joinToString(" && ")
        return executeCommand(commands)
    }

    fun release() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            if (isBound) {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            }
        } catch (_: Throwable) {
        }
    }
}
