package ca.maplenetwork.openautomate

import android.content.pm.PackageManager
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import com.topjohnwu.superuser.Shell

object BetterShell {
    fun exec(cmd: String): String {
        val res = Shell.cmd(cmd).exec()        // ↩ synchronous
        return (res.out + res.err).joinToString("\n").trim()
    }

    fun isRootAvailable(): Boolean = Shell.isAppGrantedRoot() != false       // null = undetermined yet
}

