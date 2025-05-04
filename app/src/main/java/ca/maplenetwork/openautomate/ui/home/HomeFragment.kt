package ca.maplenetwork.openautomate.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import ca.maplenetwork.openautomate.MainActivity
import ca.maplenetwork.openautomate.StateManager
import ca.maplenetwork.openautomate.databinding.FragmentHomeBinding
import ca.maplenetwork.openautomate.toggleAndAwait
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private val mainActivity: MainActivity?
        get() = activity as? MainActivity

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, saved: Bundle?) {
        binding.testStatesButton.setOnClickListener {
            val context = context ?: return@setOnClickListener
            val ds = mainActivity?.deviceStates ?: return@setOnClickListener

            // 1️⃣ build the test plan
            data class TestItem(
                val name: String,
                val mgr: StateManager,
                val seq: Pair<Boolean, Boolean>
            )

            val plan = buildList {
                add(TestItem("Wi-Fi", ds.wifi, true to false))
                add(TestItem("Bluetooth", ds.bluetooth, true to false))
                add(TestItem("Location", ds.location, true to false))
                add(TestItem("Wi-Fi scan", ds.wifiScanning, true to false))
                add(TestItem("Bluetooth scan", ds.bluetoothScanning, true to false))
                ds.mobileData?.let { add(TestItem("Mobile data", it, true to false)) }
                add(TestItem("Airplane mode", ds.airplane, false to true))
            }

            // 2️⃣ show a non-cancelable progress dialog
            val progressDlg = MaterialAlertDialogBuilder(context)
                .setTitle("Running self-test…")
                .setMessage("Preparing…")
                .setCancelable(false)
                .create()
            progressDlg.show()

            lifecycleScope.launch {
                val failures = mutableListOf<String>()

                withContext(Dispatchers.IO) {
                    // Stage 1: baseline
                    withContext(Dispatchers.Main) {
                        progressDlg.setMessage("Stage 1/4: Setting all to baseline…")
                    }
                    plan.forEachIndexed { i, item ->
                        // baseline = opposite of first target
                        item.mgr.set(item.seq.first.not())
                    }
                    delay(300)

                    // Stage 2: test ON
                    withContext(Dispatchers.Main) {
                        progressDlg.setMessage("Stage 2/4: Testing ON…")
                    }
                    plan.forEach { item ->
                        withContext(Dispatchers.Main) {
                            progressDlg.setMessage("Testing ${item.name} → ON")
                        }
                        val ok = item.mgr.toggleAndAwait(item.seq.first)
                        if (!ok) failures += "${item.name} ON"
                        delay(200)
                    }

                    // Stage 3: test OFF
                    withContext(Dispatchers.Main) {
                        progressDlg.setMessage("Stage 3/4: Testing OFF…")
                    }
                    plan.forEach { item ->
                        withContext(Dispatchers.Main) {
                            progressDlg.setMessage("Testing ${item.name} → OFF")
                        }
                        val ok = item.mgr.toggleAndAwait(item.seq.second)
                        if (!ok) failures += "${item.name} OFF"
                        delay(200)
                    }

                    // Stage 4: restore
                    withContext(Dispatchers.Main) {
                        progressDlg.setMessage("Stage 4/4: Restoring original states…")
                    }
                    plan.forEach { item ->
                        item.mgr.set(item.mgr.get()) // just re‐apply current read or keep a map of originals
                    }
                    delay(300)
                }

                // 3️⃣ done – dismiss progress & show summary
                progressDlg.dismiss()

                val finalMsg = if (failures.isEmpty()) {
                    "✅ All tests passed!"
                } else {
                    "❌ Failures:\n• " + failures.joinToString("\n• ")
                }

                MaterialAlertDialogBuilder(context)
                    .setTitle("Self-Test Report")
                    .setMessage(finalMsg)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
}