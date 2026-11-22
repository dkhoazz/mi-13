package com.example.shizuku

import androidx.annotation.Keep
import com.example.IShellService
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

@Keep
class ShellUserService() : IShellService.Stub() {

    override fun executeCommand(command: String?): String {
        if (command.isNullOrBlank()) {
            return "EXIT:1\nEmpty command"
        }
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val outputBuilder = StringBuilder()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    outputBuilder.append(line).append('\n')
                }
            }
            val exitCode = process.waitFor()
            val body = outputBuilder.toString().trim()
            if (body.isEmpty()) "EXIT:$exitCode" else "EXIT:$exitCode\n$body"
        } catch (t: Throwable) {
            "EXIT:-1\n${t.message ?: t.javaClass.simpleName}"
        }
    }

    override fun destroy() {
        exitProcess(0)
    }
}
