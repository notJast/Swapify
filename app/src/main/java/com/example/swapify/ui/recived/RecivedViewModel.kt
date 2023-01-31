package com.example.swapify.ui.recived

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RecivedViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is recived Fragment"
    }
    val text: LiveData<String> = _text
}