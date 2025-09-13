package com.lizpostudio.kgoptometrycrm.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.lizpostudio.kgoptometrycrm.databinding.FragmentSettingsLoginBinding
import id.xxx.module.view.binding.ktx.viewBinding

class SettingLoginFragment : Fragment() {

    private val binding by viewBinding<FragmentSettingsLoginBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        childFragmentManager
            .beginTransaction()
            .replace(binding.container.id, SettingLoginPreferenceFragment())
            .commit()

        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
        return binding.root
    }
}