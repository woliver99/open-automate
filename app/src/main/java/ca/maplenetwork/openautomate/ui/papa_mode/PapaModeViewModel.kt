package ca.maplenetwork.openautomate.ui.papa_mode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PapaModeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Papa Mode Fragment"
    }
    val text: LiveData<String> = _text
}