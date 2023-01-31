package com.example.swapify.ui.recived

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.swapify.databinding.FragmentRecivedBinding

class RecivedFragment : Fragment() {


    private var _binding: FragmentRecivedBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val recivedViewModel =
                ViewModelProvider(this).get(RecivedViewModel::class.java)

        _binding = FragmentRecivedBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textRecived
        recivedViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}