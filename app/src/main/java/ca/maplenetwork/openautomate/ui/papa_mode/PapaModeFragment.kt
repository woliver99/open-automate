package ca.maplenetwork.openautomate.ui.papa_mode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import ca.maplenetwork.openautomate.DeviceStates
import ca.maplenetwork.openautomate.R
import ca.maplenetwork.openautomate.databinding.FragmentPapaModeBinding

class PapaModeFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Tell it which SharedPreferences file to use
        preferenceManager.sharedPreferencesName = "papa_mode"

        // Inflate the XML we just created
        setPreferencesFromResource(R.xml.papa_mode_preferences, rootKey)
    }
}