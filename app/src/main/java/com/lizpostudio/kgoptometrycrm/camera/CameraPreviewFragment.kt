package com.lizpostudio.kgoptometrycrm.camera

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.lizpostudio.kgoptometrycrm.databinding.CameraPreviewFragmentBinding
import id.xxx.module.view.binding.ktx.viewBinding

class CameraPreviewFragment : Fragment() {

    companion object {
        const val DATA_EXTRA = "DATA_URI"
    }

    private val binding by viewBinding<CameraPreviewFragmentBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stringUri = arguments?.getString(DATA_EXTRA)
        val uri = Uri.parse(stringUri)
        binding.imageView.setImageURI(uri)

        binding.btnCancel.setOnClickListener {
            it.context.contentResolver.delete(uri, null, null)
            activity?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(android.R.id.content, CameraFragment::class.java, null, null)
                ?.commitNow()
        }

        binding.btnDone.setOnClickListener {
            val intent = Intent()
            intent.data = uri
            activity?.setResult(Activity.RESULT_OK, intent)
            activity?.finish()
        }
    }
}