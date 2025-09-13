package com.lizpostudio.kgoptometrycrm.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.databinding.FragmentPreviewWithNameBinding
import com.lizpostudio.kgoptometrycrm.utils.BitmapUtils
import com.lizpostudio.kgoptometrycrm.utils.convertLongTodd_MM_yy_hh_mm_ss
import id.xxx.module.view.binding.ktx.viewBinding
import java.io.File

class PreviewWithNameFragment : Fragment() {

    private val binding by viewBinding<FragmentPreviewWithNameBinding>()

    private val args by navArgs<PreviewWithNameFragmentArgs>()

    private fun navigateBack() {
        findNavController().navigate(
            PreviewWithNameFragmentDirections.actionToSalesOrder(args.model.recordID)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner) { navigateBack() }
        return binding.run {
            backButton.setOnClickListener { navigateBack() }
            binding.data = args.model
            root
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val onGlobalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.rootScroll.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val bitmap = BitmapUtils.create(binding.rootCard)
                binding.rootScroll.visibility = View.INVISIBLE
                binding.imgPreview.setImageBitmap(bitmap)
                binding.btnSaveImage.visibility = View.VISIBLE
                binding.btnSaveImage.setOnClickListener { it ->
                    val fileName =
                        "IMG_${convertLongTodd_MM_yy_hh_mm_ss(System.currentTimeMillis())}"
                    val file = File(Constants.ROOT_DIR_PICTURES, fileName)
                    val uri = BitmapUtils.saveAsJpeg(it.context, bitmap, file)
                    if (uri != null) {
                        val dialog = AlertDialog.Builder(it.context)
                        dialog.setTitle("Export Successful")
                        val message =
                            "\nSuccessfully export file to folder ${file.parent}, want to open it?"
                        dialog.setMessage(message)
                        dialog.setPositiveButton("Yes") { _, _ ->
                            showImage(it.context, uri)
                            navigateBack()
                        }
                        dialog.setNegativeButton("No") { d, _ ->
                            navigateBack()
                            d.dismiss()
                        }
                        dialog.show()
                    }
                }
            }
        }
        binding.rootScroll.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    private fun showImage(c: Context, uri: Uri) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(uri, "image/jpeg")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        c.startActivity(Intent.createChooser(intent, "Show Image"))
    }
}