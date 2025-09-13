package com.lizpostudio.kgoptometrycrm.forms

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.ViewModelProviderFactory
import com.lizpostudio.kgoptometrycrm.camera.CameraActivity
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.FragmentCurrentRxFormBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding
import java.io.ByteArrayOutputStream
import java.io.File

class CurrentRxFragment : Fragment() {

    companion object {
        private const val VA_DEFAULT = "6/"
        private const val PHOTO_W = 330
        private const val PHOTO_H = 528

        private lateinit var storageRef: StorageReference
        private lateinit var storageFile: Uri
    }

    private var downloadPhotoTask: StorageTask<FileDownloadTask.TaskSnapshot>? = null
    private val patientViewModel: PatientsViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }

    private val bindingRoot by viewBinding<FragmentCurrentRxFormBinding>()

    private val binding by lazy { bindingRoot.content }

    private var photoUri: Uri? = null

    private var isAdmin = false
    private var viewOnlyMode = false
    private var recordID = 0L
    private var photoFile: File = File("com.lizpostudio.kgoptometrycrm")
    private var filesDir: File = File("com.lizpostudio.kgoptometrycrm")
    private var patientID = ""
    private var showPhoto = true
    private var sectionEditDate = -1L

    private var currentForm = PatientEntity()
    private var navigateFormName = ""
    private var navigateFormRecordID = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            saveAndNavigate("back")
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val app = requireNotNull(this.activity).application

        filesDir = app.applicationContext.filesDir

        val safeArgs: CurrentRxFragmentArgs by navArgs()
        recordID = safeArgs.recordID

        // get Patient data
        Log.d(Constants.TAG, "Get Rx patient $recordID")
        patientViewModel.getPatientForm(recordID)
        createRefAssignPhFile()

        val navController = this.findNavController()

        // get if user is Admin
        val sharedPref = app.getSharedPreferences(
            Constants.PREF_NAME, Context.MODE_PRIVATE
        )
        isAdmin = (sharedPref?.getString("admin", "") ?: "") == "admin"

        patientViewModel.patientForm.observe(viewLifecycleOwner) { p ->
            currentForm = p
            patientID = p.patientID
            binding.undoButton.setOnClickListener { fillTheForm(currentForm) }
            patientViewModel.createRecordListener(currentForm.recordID)
            fillTheForm(p)
            patientViewModel.getAllFormsForPatient(patientID)
        }

        patientViewModel.patientInitForms.observe(viewLifecycleOwner) { allForms ->
            allForms?.let {
                var pAge = it.first().patientName + " "
                for (patientsRec in it) {
                    if (patientsRec.sectionName == resources.getString(R.string.info_form_caption)) {
                        val ic = patientsRec.patientIC
                        val (dob, age) = computeAgeAndDOB(ic)

                        pAge += resources.getString(
                            R.string.number_of_years_patient,
                            age, dob
                        )
                        if (currentForm.patientIC != ic) {
                            currentForm.patientIC = ic
                        }
                    }
                }
                binding.patientName.text = pAge
                val orderOfSections = listOf(*resources.getStringArray(R.array.forms_order))
                val screenDst = Resources.getSystem().displayMetrics.density

                val sortedList = it.sortedBy { patientsForms -> patientsForms.dateOfSection }
                val newList = mutableListOf<PatientEntity>()

                for (section in orderOfSections) {
                    for (forms in sortedList) {
                        var sectionName = forms.sectionName
                        if (sectionName == getString(R.string.final_prescription_caption)) {
                            sectionName = getString(R.string.sales_order_from_selection)
                            forms.sectionName = getString(R.string.sales_order_from_selection)
                        }
                        if (section == sectionName)
                            newList.add(forms)
                    }
                }

                val newSectionName = newList
                    .map { patientsForms -> patientsForms.sectionName }
                    .toSet()

                /* FOR BOTTOM NAVIGATION */
                val mapSectionName = mutableMapOf<String, MutableList<PatientEntity>>()
                newList.forEach { patient ->
                    val key = mapSectionName[patient.sectionName]
                    if (key == null) {
                        mapSectionName[patient.sectionName] = mutableListOf()
                    }
                    mapSectionName[patient.sectionName]?.add(patient)
                }

                var sectionName = ""
                val navChipGroup = bindingRoot.navigationLayout
                val navChipGroup2 = bindingRoot.navigationLayout2
//                val children = newList.map { patientForm ->
                val children = newSectionName.map { patientForm ->
                    val chip = TextView(app.applicationContext)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.gravity = Gravity.CENTER
                    chip.layoutParams = params
                    chip.setPadding(
                        (8 * screenDst).toInt(),
                        (8 * screenDst).toInt(),
                        (8 * screenDst).toInt(),
                        (8 * screenDst).toInt()
                    )

//                    if (patientForm.recordID == recordID)
                    if (patientForm == "CURRENT / OLD Rx") {
                        sectionName = patientForm
                        chip.setBackgroundColor(
                            ContextCompat.getColor(
                                app.applicationContext, R.color.lightBackground
                            )
                        )
                    } else {
                        chip.setBackgroundColor(
                            ContextCompat.getColor(
                                app.applicationContext,
                                R.color.cardBackgroundDarker
                            )
                        )
                    }

//                    val sectionShortName = makeShortSectionName(patientForm.sectionName)
                    val sectionShortName = makeShortSectionName(requireContext(), patientForm)
//                    chip.text = "$sectionShortName\n${convertLongToDDMMYY(patientForm.dateOfSection)}"
                    chip.text = sectionShortName

//                    chip.tag = patientForm.sectionName + "\n" + "${patientForm.recordID}"
                    chip.tag =
                        patientForm + "\n" + "${mapSectionName[patientForm]?.lastOrNull()?.recordID}"

                    chip.setOnClickListener { button ->
                        navigateFormName = button.tag.toString().split("\n").first()
                        navigateFormRecordID =
                            button.tag.toString().split("\n").last().toLongOrNull() ?: -1L

                        if (navigateFormRecordID != -1L) {
                            saveAndNavigate(navigateFormName)
                        }
                    }
                    chip
                }

                val children2 = mapSectionName[sectionName]
                    ?.sortedBy { p -> p.dateOfSection }
                    ?.map { patientForm ->
                        val chip = TextView(app.applicationContext)

                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.CENTER
                        }
                        chip.layoutParams = params
                        chip.setPadding(
                            (8 * screenDst).toInt(),
                            (4 * screenDst).toInt(),
                            (8 * screenDst).toInt(),
                            (4 * screenDst).toInt()
                        )

                        if (patientForm.recordID == recordID) {
                            chip.setBackgroundColor(
                                ContextCompat.getColor(
                                    app.applicationContext, R.color.lightBackground
                                )
                            )
                            if (!photoFile.path.contains("${patientForm.recordID}")) {
                                createRefAssignPhFile()
                            }
                            updatePhotoView(photoFile)
                        } else
                            chip.setBackgroundColor(
                                ContextCompat.getColor(
                                    app.applicationContext,
                                    R.color.cardBackgroundDarker
                                )
                            )

                        val sectionShortName =
                            makeShortSectionName(requireContext(), patientForm.sectionName)
                        chip.text =
                            "$sectionShortName\n${convertLongToDDMMYY(patientForm.dateOfSection)}"

                        chip.tag = patientForm.sectionName + "\n" + "${patientForm.recordID}"

                        chip.setOnClickListener { button ->
                            navigateFormName = button.tag.toString().split("\n").first()
                            navigateFormRecordID =
                                button.tag.toString().split("\n").last().toLongOrNull() ?: -1L

                            if (navigateFormRecordID != -1L) {
                                saveAndNavigate(navigateFormName)
                            }
                        }
                        chip
                    }

                navChipGroup.removeAllViews()
                for (chip in children) {
                    val chipDivider = TextView(app.applicationContext)
                    chipDivider.text = "  "
                    navChipGroup.addView(chip)
                    navChipGroup.addView(chipDivider)
                }

                navChipGroup2.removeAllViews()
                children2?.forEach { chip ->
                    val chipDivider = TextView(app.applicationContext)
                    chipDivider.text = "  "
                    navChipGroup2.addView(chip)
                    navChipGroup2.addView(chipDivider)
                }

                val hPos = newSectionName.indexOf("CURRENT / OLD Rx")
                if (hPos > 3) {
                    val scrollWidth = bindingRoot.chipsScroll.width
                    val scrollX = ((hPos - 2) * (scrollWidth / 6.25)).toInt()
                    bindingRoot.chipsScroll.postDelayed({
                        if (context != null)
                            bindingRoot.chipsScroll.smoothScrollTo(scrollX, 0)
                    }, 100L)
                }

                val hPosList =
                    mapSectionName[sectionName]?.map { form -> form.recordID } ?: listOf()
                val hPosBottomNav = hPosList.indexOf(recordID)
                if (hPosBottomNav > 3) {
                    val scrollWidth = bindingRoot.chipsScroll2.width
                    val scrollX = ((hPosBottomNav - 2) * (scrollWidth / 6.25)).toInt()
                    bindingRoot.chipsScroll2.postDelayed({
                        if (context != null)
                            bindingRoot.chipsScroll2.smoothScrollTo(scrollX, 0)
                    }, 100L)
                }
            }
        }

        binding.dateCaption.setOnClickListener {
            changeDate()
        }

// Capture photo
        binding.photoButton.setOnClickListener {
            photoUri = FileProvider
                .getUriForFile(requireActivity(), Constants.FILE_PROVIDER_AUTHORITY, photoFile)
//            val packageManager: PackageManager = requireActivity().packageManager

//            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//            val resolvedActivity: ResolveInfo? =
//                packageManager.resolveActivity(
//                    captureImage, PackageManager.MATCH_DEFAULT_ONLY
//                )
//            if (resolvedActivity != null && photoUri != null) {
//                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
//                val cameraActivities: List<ResolveInfo> =
//                    packageManager.queryIntentActivities(
//                        captureImage, PackageManager.MATCH_DEFAULT_ONLY
//                    )
//                for (cameraActivity in cameraActivities) {
//                    requireActivity().grantUriPermission(
//                        cameraActivity.activityInfo.packageName,
//                        photoUri,
//                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//                    )
//                }
//                startActivityForResult(captureImage, REQUEST_PHOTO)
//            }
            takePicture.launch(photoUri)
        }


        val sphSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                sphList()
            )

        // sphSpinnerAdapter.setDropDownViewResource(R.layout.spinner_list_small_numbers)
        binding.spinnerLeftSph.adapter = sphSpinnerAdapter
        binding.spinnerRightSph.adapter = sphSpinnerAdapter
        binding.spinnerRightSph5.adapter = sphSpinnerAdapter
        binding.spinnerLeftSph5.adapter = sphSpinnerAdapter

        val cylSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                cylList()
            )

//  cylSpinnerAdapter.setDropDownViewResource(R.layout.spinner_list_small_numbers)
        binding.spinnerLeftCyl.adapter = cylSpinnerAdapter
        binding.spinnerRightCyl.adapter = cylSpinnerAdapter
        binding.spinnerRightCyl5.adapter = cylSpinnerAdapter
        binding.spinnerLeftCyl5.adapter = cylSpinnerAdapter

        val addListItems = addList()
        val addSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                addListItems
            )
        //  addSpinnerAdapter.setDropDownViewResource(R.layout.spinner_list_small_numbers)
        binding.spinnerAdd.adapter = addSpinnerAdapter

        // chart input adapter
        ArrayAdapter.createFromResource(
            app.applicationContext,
            R.array.currently_using_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerCurrentlyUsing.adapter = adapter
        }

        patientViewModel.navTrigger.observe(viewLifecycleOwner) { navOption ->
            navOption?.let {
                launchNavigator(navOption)
            }
        }

        binding.showHideButton.setOnClickListener {
            showPhoto = !showPhoto
            if (showPhoto) {
                binding.pictureFrame.visibility = View.VISIBLE
                binding.showHideButton.setImageResource(R.drawable.visibility_36)
            } else {
                binding.pictureFrame.visibility = View.GONE
                binding.showHideButton.setImageResource(R.drawable.visibility_off_32)
            }
        }

        patientViewModel.photoFromFBReady.observe(viewLifecycleOwner) { ready ->
            ready?.let {
                Log.d(Constants.TAG, "Photo file is $it")
                if (it) uploadPhotoFileToImage()
            }
        }

        // DELETE FORM FUNCTIONALITY

        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { ifDeleted ->
            ifDeleted?.let {
                if (ifDeleted) navController.navigate(
                    CurrentRxFragmentDirections.actionToFormSelectionFragment(patientID)
                )
            }
        }

        bindingRoot.deleteForm.setOnClickListener {
            if (context != null)
                actionConfirmDeletion(
                    title = resources.getString(R.string.form_delete_title),
                    message = resources.getString(
                        R.string.customer_form_delete,
                        currentForm.sectionName,
                        currentForm.patientName
                    ),
                    isAdmin, requireContext()
                ) { allowed ->
                    if (allowed) {
                        patientViewModel.deleteRecord(currentForm)
                        patientViewModel.deletePatientFromFirebase(currentForm)
                    }
                }
        }

        binding.deletePhoto.setOnClickListener {
            if (photoFile.exists()) {
                actionConfirmDeletion(
                    title = resources.getString(R.string.photo_delete_title),
                    message = resources.getString(R.string.photo_delete),
                    isAdmin, requireContext(), checkPassword = false
                ) { allowed ->
                    if (allowed) {
                        currentForm.reservedField = "Deleted"
                        if (photoFile.exists()) photoFile.delete()
                        currentForm.reservedField = ""
                        storageRef.delete() //.addOnCompleteListener { task -> }
                        binding.autorefPhoto.setImageDrawable(null)
                        // updatePhotoView(photoFile)
                    }
                }
            } else {
                Toast.makeText(app.applicationContext, "Nothing to delete!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        patientViewModel.patientFireForm.observe(viewLifecycleOwner) { patientNewRecord ->
            patientNewRecord?.let {
                Log.d(Constants.TAG, "Reload Rx Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(Constants.TAG, "Rx Record from FB loaded")
                    currentForm.copyFrom(it)
                    fillTheForm(it)
                }
            }
        }

        bindingRoot.saveFormButton.setOnClickListener {
            saveAndNavigate("none")
        }

        bindingRoot.backButton.setOnClickListener {
            saveAndNavigate("back")
        }

        bindingRoot.homeButton.setOnClickListener {
            saveAndNavigate("home")
        }

        binding.rotatePhoto.setOnClickListener {
            val bitmap =
                BitmapUtils.rotate((binding.autorefPhoto.drawable as BitmapDrawable).bitmap, 90F)

            binding.autorefPhoto.setImageBitmap(bitmap)
        }
        return bindingRoot.root
    }

    private fun saveAndNavigate(navOption: String) {
        patientViewModel.removeRecordsChangesListener()
        if (viewOnlyMode) {
            launchNavigator(navOption)
        } else {
            if (formWasChanged()) {
                Log.d(Constants.TAG, "Rx form CHANGED")
                Log.d(
                    Constants.TAG,
                    "Submiting to firebase and local db record ID ${currentForm.recordID}"
                )
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(),
                    currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                Log.d(Constants.TAG, "Rx form the SAME!!!")
                launchNavigator(navOption)
            }
        }
    }

    private fun launchNavigator(option: String) {
        when (option) {
            "none" -> {
                fillTheForm(currentForm)
            }

            "back" -> this.findNavController().navigate(
                CurrentRxFragmentDirections.actionToFormSelectionFragment(patientID)
            )

            "home" -> findNavController().navigate(
                CurrentRxFragmentDirections.actionToDatabaseSearchFragment()
            )

            else -> navigateToSelectedForm()
        }
    }

    private fun createRefAssignPhFile() {
        photoFile = File(filesDir, "IMG_$recordID.jpg")
        storageRef = patientViewModel.assignStorageRef("IMG_$recordID.jpg")
        storageFile = Uri.fromFile(photoFile)
    }

    private fun navigateToSelectedForm() {
        when (navigateFormName) {
            getString(R.string.info_form_caption) -> findNavController().navigate(
                CurrentRxFragmentDirections.actionToInfoFragment(navigateFormRecordID)
            )

            getString(R.string.follow_up_form_caption) -> findNavController().navigate(
                CurrentRxFragmentDirections.actionToFollowUpFragment(navigateFormRecordID)
            )

            getString(R.string.memo_form_caption) -> findNavController().navigate(
                CurrentRxFragmentDirections.actionToMemoFragment(navigateFormRecordID)
            )

            getString(R.string.current_rx_caption) -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }

            getString(R.string.refraction_caption) -> findNavController().navigate(
                CurrentRxFragmentDirections.actionToRefractionFragment(navigateFormRecordID)
            )

            getString(R.string.ocular_health_caption) -> findNavController().navigate(
                CurrentRxFragmentDirections.actionToOcularHealthFragment(navigateFormRecordID)
            )

            getString(R.string.supplementary_test_caption) -> findNavController().navigate(
                CurrentRxFragmentDirections.actionToSupplementaryFragment(navigateFormRecordID)
            )

            getString(R.string.contact_lens_exam_caption) -> findNavController().navigate(
                CurrentRxFragmentDirections.actionToContactLensFragment(navigateFormRecordID)
            )

            getString(R.string.orthox_caption) -> findNavController().navigate(
                CurrentRxFragmentDirections.actionToOrthokFragment(navigateFormRecordID)
            )

            getString(R.string.cash_order) -> findNavController().navigate(
                ContactLensFragmentDirections.actionToCashOrderFragment(navigateFormRecordID)
            )

            getString(R.string.sales_order_caption) -> findNavController().navigate(
                CurrentRxFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )

            getString(R.string.final_prescription_caption) -> findNavController().navigate(
                CurrentRxFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )

            else -> {
                Toast.makeText(
                    context, "$navigateFormName not implemented yet", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private var takePhoto = false

    private val takePicture = registerForActivityResult(CameraActivity.ResultContract()) { result ->
        if (result != null) {

            val parcelFileDescriptor =
                requireContext().contentResolver.openFileDescriptor(result, "rw")
                    ?: return@registerForActivityResult
            takePhoto = true
            val bitmap = getScaledBitmap(
                parcelFileDescriptor.fileDescriptor,
                PHOTO_W,
                PHOTO_H
            )
            parcelFileDescriptor.close()
            requireContext().contentResolver.delete(result, null, null)
            binding.autorefPhoto.setImageBitmap(bitmap)
            currentForm.reservedField = storageRef.toString()
            binding.rotatePhoto.visibility = View.VISIBLE
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)

            storageRef.putBytes(bos.toByteArray())
                .addOnCompleteListener {
                    bos.close()
                }
                .addOnFailureListener {
                    bos.close()
                    Toast.makeText(
                        requireContext(), it.localizedMessage, Toast.LENGTH_LONG
                    ).show()
                }
        }
    }


    private fun updatePhotoView(photoFile: File) {
        binding.autorefPhoto.setImageDrawable(null)

        //   taskToGetFile
        if (currentForm.reservedField.isBlank()) {
            currentForm.reservedField = storageRef.toString()
        }
        if (currentForm.reservedField.isNotBlank() && currentForm.reservedField != "deleted") {
            downloadPhotoTask = storageRef.getFile(photoFile).addOnSuccessListener {
                Log.d(Constants.TAG, "FB photo downloaded to local file")
                patientViewModel.readyToShowPhoto()
                currentForm.reservedField = photoFile.toString()
            }.addOnFailureListener {
                // delete local file
                Log.d(Constants.TAG, "No such file exist or error downloading. Delete local file")
                if (photoFile.exists())
                    photoFile.delete()
                patientViewModel.readyToShowPhoto()
                currentForm.reservedField = ""
                binding.autorefPhoto.setImageDrawable(null)
            }
        } else {
            binding.autorefPhoto.setImageDrawable(null)
        }
    }

    private fun uploadPhotoFileToImage() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, PHOTO_W, PHOTO_H)
            if (bitmap != null) {
                binding.autorefPhoto.setImageBitmap(bitmap)
            }
        } else {
            binding.autorefPhoto.setImageDrawable(null)
        }
    }

    private fun fillTheForm(p: PatientEntity) {
        viewOnlyMode = p.isReadOnly
        if (viewOnlyMode) {
            bindingRoot.viewOnlyButton.setImageResource(R.drawable.visibility_36)
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.viewOnlyMode)
            )
            bindingRoot.saveFormButton.visibility = View.GONE
        } else {
            bindingRoot.viewOnlyButton.setImageResource(R.drawable.ic_read_write_36)
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.lightBackground)
            )
            bindingRoot.saveFormButton.visibility = View.VISIBLE
        }

        bindingRoot.viewOnlyButton.setOnClickListener {
            viewOnlyMode = !viewOnlyMode
            if (viewOnlyMode) {
                bindingRoot.viewOnlyButton.setImageResource(R.drawable.visibility_36)
                binding.mainLayout.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.viewOnlyMode
                    )
                )
                bindingRoot.saveFormButton.visibility = View.GONE
            } else {
                bindingRoot.viewOnlyButton.setImageResource(R.drawable.ic_read_write_36)
                binding.mainLayout.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.lightBackground)
                )
                bindingRoot.saveFormButton.visibility = View.VISIBLE
            }

            patientViewModel.updateIsReadOnly("${p.recordID}", viewOnlyMode)
        }

        val extractData = p.sectionData.split('|').toMutableList()
//      Log.d(Constants.TAG, "extract data size before = ${extractData.size}")
        if (extractData.size < 22) {
            for (index in extractData.size..22) {
                extractData.add("")
            }
        }

        binding.apply {

            sectionEditDate = p.dateOfSection
            //     patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(p.dateOfSection)
            //  Log.d(Constants.TAG, " Extracted data: $extractData" )

            var isEmpty: Boolean

            if (extractData[0].trim() == "") {
                // set default == 0  element
                spinnerCurrentlyUsing.setSelection(0)
                isEmpty = false
            } else {
                for (i in 0 until spinnerCurrentlyUsing.adapter.count) {
                    if (extractData[0].trim() == spinnerCurrentlyUsing.adapter.getItem(i)
                            .toString()
                    ) {
                        spinnerCurrentlyUsing.setSelection(i)
                    }
                }
                isEmpty = false
            }
            // set default == 0  element
            if (isEmpty)
                spinnerCurrentlyUsing.setSelection(0)

            isEmpty = true

            for (i in 0 until spinnerRightSph.adapter.count) {
                if (extractData[1].trim() != "" &&
                    extractData[1] == spinnerRightSph.adapter.getItem(i).toString()
                ) {
                    spinnerRightSph.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerRightSph.adapter.count) {
                    if (" " == spinnerRightSph.adapter.getItem(i).toString()) {
                        spinnerRightSph.setSelection(i)
                    }
                }
            }
            isEmpty = true

            for (i in 0 until spinnerRightSph5.adapter.count) {
                if (extractData[14].trim() != "" &&
                    extractData[14] == spinnerRightSph5.adapter.getItem(i).toString()
                ) {
                    spinnerRightSph5.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerRightSph5.adapter.count) {
                    if (" " == spinnerRightSph5.adapter.getItem(i).toString()) {
                        spinnerRightSph5.setSelection(i)
                    }
                }
            }
            isEmpty = true

            for (i in 0 until spinnerRightCyl.adapter.count) {
                if (extractData[2].trim() != "" &&
                    extractData[2].trim().toDoubleOrNull() == spinnerRightCyl.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerRightCyl.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerRightCyl.setSelection(0)
            isEmpty = true

            for (i in 0 until spinnerRightCyl5.adapter.count) {
                if (extractData[16].trim() != "" &&
                    extractData[16].trim().toDoubleOrNull() == spinnerRightCyl5.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerRightCyl5.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerRightCyl5.setSelection(0)
            isEmpty = true

            editRightAxis.setText(extractData[3])

            editRightAxis5.setText(extractData[18])

            for (i in 0 until spinnerLeftSph.adapter.count) {
                if (extractData[4].trim() != "" &&
                    extractData[4] == spinnerLeftSph.adapter.getItem(i).toString()
                ) {
                    spinnerLeftSph.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerLeftSph.adapter.count) {
                    if (" " == spinnerLeftSph.adapter.getItem(i).toString()) {
                        spinnerLeftSph.setSelection(i)
                    }
                }
            }
            isEmpty = true

            for (i in 0 until spinnerLeftSph5.adapter.count) {
                if (extractData[15].trim() != "" &&
                    extractData[15] == spinnerLeftSph5.adapter.getItem(i).toString()
                ) {
                    spinnerLeftSph5.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerLeftSph5.adapter.count) {
                    if (" " == spinnerLeftSph5.adapter.getItem(i).toString()) {
                        spinnerLeftSph5.setSelection(i)
                    }
                }
            }
            isEmpty = true

            for (i in 0 until spinnerLeftCyl.adapter.count) {
                if (extractData[5].trim() != "" &&
                    extractData[5].trim().toDoubleOrNull() == spinnerLeftCyl.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerLeftCyl.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerLeftCyl.setSelection(0)
            isEmpty = true

            for (i in 0 until spinnerLeftCyl5.adapter.count) {
                if (extractData[17].trim() != "" &&
                    extractData[17].trim().toDoubleOrNull() == spinnerLeftCyl5.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerLeftCyl5.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerLeftCyl5.setSelection(0)
            isEmpty = true

            editLeftAxis.setText(extractData[6])

            editLeftAxis5.setText(extractData[19])

            if (extractData[7] != "") editRightVa.setText(extractData[7]) else editRightVa.setText(
                VA_DEFAULT
            )
            if (extractData[20] != "") editRightVa3.setText(extractData[20]) else editRightVa3.setText(
                VA_DEFAULT
            )
            if (extractData[8] != "") editLeftVa.setText(extractData[8]) else editLeftVa.setText(
                VA_DEFAULT
            )
            if (extractData[21] != "") editLeftVa3.setText(extractData[21]) else editLeftVa3.setText(
                VA_DEFAULT
            )
            if (extractData[9] != "") editOuva.setText(extractData[9]) else editOuva.setText(
                VA_DEFAULT
            )

            for (i in 0 until spinnerAdd.adapter.count) {
                if (extractData[10].trim() != "" &&
                    extractData[10].trim().toDoubleOrNull() == spinnerAdd.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerAdd.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerAdd.setSelection(0)

            currentLens.setText(extractData[11])
            lensYear.setText(extractData[12])
            currentContactLens.setText(extractData[13])

            remarkInput.setText(p.remarks)

            patientViewModel.practitioner.observe(viewLifecycleOwner) {
                val data =
                    if (it.contains(p.practitioner)) {
                        it
                    } else {
                        it.toMutableList().apply { add(p.practitioner) }
                    }
                val adapterPractitioner =
                    ArrayAdapter(requireContext(), R.layout.spinner_list_basic_, data)
                practitionerName.adapter = adapterPractitioner
                val isCreated = Constants.isCreatedForm(requireContext())
                if (isCreated) {
                    practitionerName.setSelection(1)
                    saveAndNavigate("none")
                } else {
                    data.forEachIndexed { index, s ->
                        if (s == p.practitioner) {
                            practitionerName.setSelection(index)
                        }
                    }
                }
            }
// END of Binding
        }
    }

    /**
     * If UI was changed - returns true
     */
    private fun formWasChanged(): Boolean {
        // create new Record, fill it in with Form data and pass to ViewModel with recordID to update DB
        val priorPatient = currentForm.copy()
        binding.apply {

            currentForm.remarks = remarkInput.text.toString().uppercase()
            if (sectionEditDate != -1L) currentForm.dateOfSection = sectionEditDate

            val extractData = spinnerCurrentlyUsing.selectedItem.toString() + "|" +
                    spinnerRightSph.selectedItem.toString() + "|" +
                    spinnerRightCyl.selectedItem.toString() + "|" +
                    editRightAxis.text.toString() + "|" +
                    spinnerLeftSph.selectedItem.toString() + "|" +
                    spinnerLeftCyl.selectedItem.toString() + "|" +
                    editLeftAxis.text.toString() + "|" +
                    editRightVa.text.toString() + "|" +
                    editLeftVa.text.toString() + "|" +
                    editOuva.text.toString() + "|" +
                    spinnerAdd.selectedItem.toString() + "|" +
                    currentLens.text.toString() + "|" +
                    lensYear.text.toString() + "|" +
                    currentContactLens.text.toString() + "|" +  //13
                    spinnerRightSph5.selectedItem.toString() + "|" +  //14
                    spinnerLeftSph5.selectedItem.toString() + "|" +  //15
                    spinnerRightCyl5.selectedItem.toString() + "|" +  //16
                    spinnerLeftCyl5.selectedItem.toString() + "|" +  //17
                    editRightAxis5.text.toString() + "|" +  //18
                    editLeftAxis5.text.toString() + "|" +  //19
                    editRightVa3.text.toString() + "|" +   //20
                    editLeftVa3.text.toString()   //21

            currentForm.sectionData = extractData.uppercase()
            currentForm.practitioner = (binding.practitionerName.selectedItem as String).uppercase()

            if (binding.rotatePhoto.isVisible) {
                takePhoto = false
                binding.rotatePhoto.visibility = View.GONE

                val bitmapDrawable = binding.autorefPhoto.drawable as? BitmapDrawable
                val bitmap = bitmapDrawable?.bitmap

                val bos = ByteArrayOutputStream()
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, bos)

                storageRef.putBytes(bos.toByteArray())
                    .addOnCompleteListener {
                        bos.close()
                    }
                    .addOnFailureListener {
                        bos.close()
                        Toast.makeText(
                            requireContext(), it.localizedMessage, Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }
        return !currentForm.assertEqual(priorPatient)
    }

    private fun changeDate() {
        val (todayYear, todayMonth, todayDay) = dayMonthY()
        val myActivity = activity

        myActivity?.let {
            val datePickerDialog = DatePickerDialog(
                it,
                { _, year, monthOfYear, dayOfMonth -> // set day of month , month and year value in the edit text
                    sectionEditDate = convertYMDtoTimeMillis(year, monthOfYear, dayOfMonth)
                    if (sectionEditDate != -1L) binding.dateCaption.text =
                        convertLongToDDMMYY(sectionEditDate)
                }, todayYear, todayMonth, todayDay
            )
            datePickerDialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        downloadPhotoTask?.cancel()
    }
}
