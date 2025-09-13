package com.lizpostudio.kgoptometrycrm.camera

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.lizpostudio.kgoptometrycrm.databinding.CameraFragmentBinding
import id.xxx.module.view.binding.ktx.viewBinding

class CameraFragment : Fragment() {

    private val binding by viewBinding<CameraFragmentBinding>()

    private var imageCapture: ImageCapture? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startCamera(view.context)

        binding.btnCameraCapture.setOnClickListener {
            takePicture(view.context)
        }
        binding.btnCloseCamera.setOnClickListener {
            requireActivity().finishAfterTransition()
        }
    }

    private fun takePicture(context: Context) {
        val imageCapture = imageCapture ?: return
        val uri = requireActivity().intent.data ?: return
        val openOutputStream = context.contentResolver.openOutputStream(uri) ?: return
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            openOutputStream
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    openOutputStream.close()
                    exc.printStackTrace()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    openOutputStream.close()
                    val bundle = Bundle()
                    bundle.putString(CameraPreviewFragment.DATA_EXTRA, uri.toString())
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(android.R.id.content, CameraPreviewFragment::class.java, bundle)
                        .commit()
                }
            }
        )
    }

    private fun startCamera(context: Context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .setTargetRotation(Surface.ROTATION_180)
                .setResolutionSelector(
                    ResolutionSelector.Builder().setAspectRatioStrategy(
                        AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                    ).build()
                )
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .setTargetResolution(Size(binding.viewFinder.width, binding.viewFinder.height))
                .build()
            preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)


            imageCapture = ImageCapture.Builder()
                .build()

            var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                var camera = cameraProvider
                    .bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)

                binding.btnSwitchCamera.setOnClickListener {
                    cameraProvider.unbindAll()
                    if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    } else if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    camera = cameraProvider
                        .bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
                }

                binding.viewFinder.afterMeasured { viewFinder ->
                    viewFinder.setOnTouchListener { view, event ->
                        val previewView = view as PreviewView
                        if (previewView.performClick()) {
                            return@setOnTouchListener true
                        }
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> return@setOnTouchListener true
                            MotionEvent.ACTION_UP -> {
                                val factory = SurfaceOrientedMeteringPointFactory(
                                    view.width.toFloat(), view.height.toFloat()
                                )
                                val autoFocusPoint = factory.createPoint(event.x, event.y)
                                try {
                                    camera.cameraControl.startFocusAndMetering(
                                        FocusMeteringAction.Builder(
                                            autoFocusPoint,
                                            FocusMeteringAction.FLAG_AF
                                        ).apply {
                                            disableAutoCancel()
                                        }.build()
                                    )
                                } catch (e: CameraInfoUnavailableException) {
                                    e.printStackTrace()
                                }
                                return@setOnTouchListener true
                            }

                            else -> return@setOnTouchListener false
                        }
//                        val result = !view.performClick()
//                        if (result) {
//                            val factory = SurfaceOrientedMeteringPointFactory(
//                                view.rootView.width.toFloat(), view.rootView.height.toFloat()
//                            )
//                            val autoFocusPoint = factory.createPoint(event.x, event.y,0F)
//                            try {
//                                camera.cameraControl.startFocusAndMetering(
//                                    FocusMeteringAction.Builder(
//                                        autoFocusPoint,
//                                        FocusMeteringAction.FLAG_AF
//                                    ).apply {
//                                        disableAutoCancel()
//                                    }.build()
//                                )
//                            } catch (e: CameraInfoUnavailableException) {
//                                e.printStackTrace()
//                            }
//                        }
//                        return@setOnTouchListener result
                    }
                }

            } catch (exc: Exception) {
                exc.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private inline fun View.afterMeasured(crossinline block: (View) -> Unit) {
        if (measuredWidth > 0 && measuredHeight > 0) {
            block(this)
        } else {
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (measuredWidth > 0 && measuredHeight > 0) {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        block(this@afterMeasured)
                    }
                }
            })
        }
    }
}