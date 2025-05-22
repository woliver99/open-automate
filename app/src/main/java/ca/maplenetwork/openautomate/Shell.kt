package ca.maplenetwork.openautomate

import android.content.pm.PackageManager
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

class Shell(private val shizukuService: IShizukuService) {
    fun exec(cmd: String): String {
        check(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            "Shizuku permission missing"
        }

        val mergedCmd = "$cmd 2>&1"
        val proc = shizukuService.newProcess(arrayOf("sh","-c", mergedCmd), null, null)
        val output = BufferedReader(InputStreamReader(FileInputStream(proc.inputStream.fileDescriptor)))
            .use { it.readText() }
        proc.waitFor()
        proc.destroy()
        return output.trimEnd()
    }

    fun close() {
        shizukuService.exit()
    }

    companion object {
        fun create(): Shell {
            val shizukuService = IShizukuService.Stub.asInterface(
                Shizuku.getBinder() ?: throw IllegalStateException("Shizuku not running")
            )
            return Shell(shizukuService)
        }
    }
}

