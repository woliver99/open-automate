package ca.maplenetwork.openautomate

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import ca.maplenetwork.openautomate.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import rikka.shizuku.Shizuku
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder

const val TAG = "Open Automate"

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var shizukuDlg: AlertDialog? = null
    private var shizukuListener: Shizuku.OnRequestPermissionResultListener? = null

    var deviceStates: DeviceStates? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        ensureShizuku {
            deviceStates = DeviceStates(this)
            Toast.makeText(this, "App Shizuku Enabled", Toast.LENGTH_LONG).show()
            val airplaneListener = StateListener { Log.d(TAG, "onCreate: Airplane Mode is $it") }
            val wifiListener = StateListener { Log.d(TAG, "onCreate: WiFi is $it") }
            val btListener = StateListener { Log.d(TAG, "onCreate: Bluetooth is $it") }
            val locationListener = StateListener { Log.d(TAG, "onCreate: Location is $it") }
            val wifiScanningListener = StateListener { Log.d(TAG, "onCreate: WiFi scanning is $it") }
            val googleLocationListener = StateListener { Log.d(TAG, "onCreate: Google Location is $it") }
            deviceStates?.airplane?.addListener(airplaneListener)
            deviceStates?.wifi?.addListener(wifiListener)
            deviceStates?.bluetooth?.addListener(btListener)
            deviceStates?.location?.addListener(locationListener)
            deviceStates?.wifiScanning?.addListener(wifiScanningListener)
            deviceStates?.googleAccuracy?.addListener(googleLocationListener)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        shizukuListener?.let { Shizuku.removeRequestPermissionResultListener(it) }
        shizukuDlg?.dismiss()
    }

    private fun ensureShizuku(onGranted: () -> Unit) {

        fun showDialog(showToast: Boolean = false) {
            /* 1️⃣ abort if Activity is finishing or destroyed */
            if (isFinishing || isDestroyed) return

            if (showToast) {
                Toast.makeText(
                    this,
                    "Shizuku permission not granted",
                    Toast.LENGTH_SHORT
                ).show()
            }

            /* 2️⃣ always run on main thread */
            runOnUiThread {
                if (shizukuDlg?.isShowing == true) return@runOnUiThread

                shizukuDlg = MaterialAlertDialogBuilder(this)
                    .setTitle("Shizuku required")
                    .setMessage(
                        "This app can only run when the Shizuku service is " +
                                "installed, started, and its permission is granted."
                    )
                    .setCancelable(false)
                    .setNeutralButton("Shizuku") { _, _ ->
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://github.com/RikkaApps/Shizuku".toUri()
                            )
                        )
                        finish()
                    }
                    .setNegativeButton("Close") { _, _ -> finish() }
                    .setPositiveButton("Request") { _, _ ->
                        Shizuku.requestPermission(0x5A11)
                    }
                    .show()
            }
        }

        /* ---------------- permission listener -------------------- */
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(code: Int, result: Int) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    shizukuDlg?.dismiss()
                    onGranted()
                } else {
                    showDialog(showToast = true)
                }
            }
        }
        shizukuListener = listener

        /* ---------------- initial flow ---------------------------- */
        if (Shizuku.isPreV11()) {
            Toast.makeText(this,
                "Shizuku v11+ is required", Toast.LENGTH_LONG).show()
            finish(); return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            onGranted(); return
        }

        Shizuku.addRequestPermissionResultListener(listener)
        showDialog()
    }

}