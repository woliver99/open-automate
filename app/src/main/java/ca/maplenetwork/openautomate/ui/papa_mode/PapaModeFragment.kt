package ca.maplenetwork.openautomate.ui.papa_mode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ca.maplenetwork.openautomate.databinding.FragmentPapaModeBinding

class PapaModeFragment : Fragment() {

    private var _binding: FragmentPapaModeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val papaModeViewModel = ViewModelProvider(this)[PapaModeViewModel::class.java]

        _binding = FragmentPapaModeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textPapaMode
        papaModeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}