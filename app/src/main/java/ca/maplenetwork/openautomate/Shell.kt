package ca.maplenetwork.openautomate

import android.content.pm.PackageManager
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

object Shell {

    private fun requireService(): IShizukuService =
        IShizukuService.Stub.asInterface(
            Shizuku.getBinder()
                ?: throw IllegalStateException("Shizuku not running")
        )

    fun exec(cmd: String): String {
        check(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            "Shizuku permission missing"
        }

        val proc = requireService().newProcess(arrayOf("sh", "-c", cmd), null, null)

        val out = BufferedReader(
            InputStreamReader(FileInputStream(proc.inputStream.fileDescriptor))
        ).use { it.readText() }

        val err = BufferedReader(
            InputStreamReader(FileInputStream(proc.errorStream.fileDescriptor))
        ).use { it.readText() }

        proc.waitFor()        // optional; streams already consumed
        return (out + err).trimEnd()
    }
}

