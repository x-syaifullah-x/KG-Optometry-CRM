package com.lizpostudio.kgoptometrycrm.forms

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.PatientsViewModelFactory
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientsEntity
import com.lizpostudio.kgoptometrycrm.databinding.FragmentCurrentRxFormBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import java.io.File
import java.io.FileOutputStream

class CurrentRxFragment : Fragment() {

    companion object {
        private const val TAG = "LogTrace"
        private const val vaDefault = "6/"
        private const val REQUEST_PHOTO = 2
        private const val PHOTO_W = 330
        private const val PHOTO_H = 528

        private lateinit var storageRef: StorageReference
        private lateinit var storageFile: Uri
    }

    private var downloadPhotoTask: StorageTask<FileDownloadTask.TaskSnapshot>? = null
    private val patientViewModel: PatientsViewModel by viewModels {
        PatientsViewModelFactory(requireContext())
    }

    private var _binding: FragmentCurrentRxFormBinding? = null
    private val binding get() = _binding!!

    private var photoUri: Uri? = null

    private var isAdmin = false
    private var viewOnlyMode = false
    private var recordID = 0L
    private var photoFile: File = File("com.lizpostudio.kgoptometrycrm")
    private var filesDir: File = File("com.lizpostudio.kgoptometrycrm")
    private var patientID = ""
    private var showPhoto = true
    private var sectionEditDate = -1L

    private var currentForm = PatientsEntity()
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

        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_current_rx_form,
            container,
            false
        )
        val app = requireNotNull(this.activity).application

        filesDir = app.applicationContext.filesDir

        val safeArgs: CurrentRxFragmentArgs by navArgs()
        recordID = safeArgs.recordID

        // get Patient data
        Log.d(TAG, "Get Rx patient $recordID")
        patientViewModel.getPatientForm(recordID)
        createRefAssignPhFile()

        binding.lifecycleOwner = this
        val navController = this.findNavController()

        // get if user is Admin
        val sharedPref = app.getSharedPreferences(
            "kgoptometry",
            Context.MODE_PRIVATE
        )
        isAdmin = sharedPref?.getString("admin", "") ?: "" == "admin"
        viewOnlyMode = sharedPref?.getBoolean("viewOnly", false) ?: false
        if (viewOnlyMode) {
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.viewOnlyMode
                )
            )
            binding.saveFormButton.visibility = View.GONE
        } else {
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.lightBackground
                )
            )
        }

        patientViewModel.patientForm.observe(viewLifecycleOwner) { patientForm ->
            patientForm?.let {
                currentForm = it
                patientID = it.patientID
                Log.d(TAG, "Rx Patient recordID ${currentForm.recordID} received")
                patientViewModel.createRecordListener(currentForm.recordID)
                fillTheForm(it)
                patientViewModel.getAllFormsForPatient(patientID)
                updatePhotoView(photoFile)
            }
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
                    }
                }
                binding.patientName.text = pAge
                val orderOfSections = listOf(*resources.getStringArray(R.array.forms_order))
                val screenDst = Resources.getSystem().displayMetrics.density

                val sortedList = it.sortedBy { patientsForms -> patientsForms.dateOfSection }
                val newList = mutableListOf<PatientsEntity>()

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
                val mapSectionName = mutableMapOf<String, MutableList<PatientsEntity>>()
                newList.forEach { patient ->
                    val key = mapSectionName[patient.sectionName]
                    if (key == null) {
                        mapSectionName[patient.sectionName] = mutableListOf()
                    }
                    mapSectionName[patient.sectionName]?.add(patient)
                }

                var sectionName = ""
                val navChipGroup = binding.navigationLayout
                val navChipGroup2 = binding.navigationLayout2
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
                    val sectionShortName = makeShortSectionName(patientForm)
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

                        if (patientForm.recordID == recordID)
                            chip.setBackgroundColor(
                                ContextCompat.getColor(
                                    app.applicationContext, R.color.lightBackground
                                )
                            )
                        else
                            chip.setBackgroundColor(
                                ContextCompat.getColor(
                                    app.applicationContext,
                                    R.color.cardBackgroundDarker
                                )
                            )

                        val sectionShortName = makeShortSectionName(patientForm.sectionName)
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
                    val scrollWidth = binding.chipsScroll.width
                    val scrollX = ((hPos - 2) * (scrollWidth / 6.25)).toInt()
                    binding.chipsScroll.postDelayed({
                        if (context != null)
                            binding.chipsScroll.smoothScrollTo(scrollX, 0)
                    }, 100L)
                }

                val hPosList = mapSectionName[sectionName]?.map { form -> form.recordID }?: listOf()
                val hPosBottomNav = hPosList.indexOf(recordID)
                if (hPosBottomNav > 3) {
                    val scrollWidth = binding.chipsScroll2.width
                    val scrollX = ((hPosBottomNav - 2) * (scrollWidth / 6.25)).toInt()
                    binding.chipsScroll2.postDelayed({
                        if (context != null)
                            binding.chipsScroll2.smoothScrollTo(scrollX, 0)
                    }, 100L)
                }
            }
        }

        binding.dateCaption.setOnClickListener {
            changeDate()
        }

// Capture photo
        binding.photoButton.setOnClickListener {
            photoUri = FileProvider.getUriForFile(
                requireActivity(),
                "com.lizpostudio.kgoptometrycrm.fileprovider",
                photoFile
            )
            val packageManager: PackageManager = requireActivity().packageManager

            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(
                    captureImage,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
            if (resolvedActivity != null && photoUri != null) {
                captureImage.putExtra(
                    MediaStore.EXTRA_OUTPUT, photoUri
                )
                val cameraActivities: List<ResolveInfo> =
                    packageManager.queryIntentActivities(
                        captureImage,
                        PackageManager.MATCH_DEFAULT_ONLY
                    )
                for (cameraActivity in cameraActivities) {

                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }


        val sphListItems = sphList()
        val sphSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                sphListItems
            )

        // sphSpinnerAdapter.setDropDownViewResource(R.layout.spinner_list_small_numbers)
        binding.spinnerLeftSph.adapter = sphSpinnerAdapter
        binding.spinnerRightSph.adapter = sphSpinnerAdapter

        val cylListItems = cylList()
        val cylSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                cylListItems
            )

//  cylSpinnerAdapter.setDropDownViewResource(R.layout.spinner_list_small_numbers)
        binding.spinnerLeftCyl.adapter = cylSpinnerAdapter
        binding.spinnerRightCyl.adapter = cylSpinnerAdapter

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
                binding.showHideButton.setImageResource(R.drawable.visibility_32)
            } else {
                binding.pictureFrame.visibility = View.GONE
                binding.showHideButton.setImageResource(R.drawable.visibility_off_32)
            }
        }

        patientViewModel.photoFromFBReady.observe(viewLifecycleOwner) { ready ->
            ready?.let {
                Log.d(TAG, "Photo file is $it")
                if (it) uploadPhotoFileToImage()
            }
        }

        // DELETE FORM FUNCTIONALITY

        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { ifDeleted ->
            ifDeleted?.let {
                if (ifDeleted) navController.navigate(
                    CurrentRxFragmentDirections.actionCurrentRxFragmentToFormSelectionFragment(
                        patientID
                    )
                )
            }
        }

        binding.deleteForm.setOnClickListener {
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
                        patientViewModel.deletePatientFromFirebase(currentForm.recordID.toString())
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
                Log.d(TAG, "Reload Rx Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(TAG, "Rx Record from FB loaded")
                    currentForm.copyFrom(it)
                    fillTheForm(it)
                }
            }
        }

        binding.saveFormButton.setOnClickListener {
            saveAndNavigate("none")
        }

        binding.backButton.setOnClickListener {
            saveAndNavigate("back")
        }

        binding.homeButton.setOnClickListener {
            saveAndNavigate("home")
        }

        var rotation = 0F
        binding.rotatePhoto.setOnClickListener {
            if (rotation == 360F) {
                rotation = 0F
            }
            rotation += 90
            val bitmap =
                BitmapUtils.rotate(BitmapFactory.decodeFile(photoFile.toString()), rotation)

            binding.autorefPhoto.setImageBitmap(bitmap)
        }
        return binding.root
    }

    private fun saveAndNavigate(navOption: String) {
        patientViewModel.removeRecordsChangesListener()
        if (viewOnlyMode) {
            launchNavigator(navOption)
        } else {
            if (formWasChanged()) {
                Log.d(TAG, "Rx form CHANGED")
                Log.d(TAG, "Submiting to firebase and local db record ID ${currentForm.recordID}")
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(),
                    currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                Log.d(TAG, "Rx form the SAME!!!")
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
                CurrentRxFragmentDirections.actionCurrentRxFragmentToFormSelectionFragment(patientID)
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
        val navController = this.findNavController()
        val orderOfSections = listOf(*resources.getStringArray(R.array.forms_order))

        when (navigateFormName) {

            orderOfSections[0] -> navController.navigate(
                CurrentRxFragmentDirections.actionCurrentRxFragmentToInfoFragment(
                    navigateFormRecordID
                )
            )
            orderOfSections[1] -> navController.navigate(
                CurrentRxFragmentDirections.actionCurrentRxFragmentToMemoFragment(
                    navigateFormRecordID
                )
            )
            orderOfSections[2] -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    createRefAssignPhFile()
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }
            orderOfSections[3] -> navController.navigate(
                CurrentRxFragmentDirections.actionCurrentRxFragmentToRefractionFragment(
                    navigateFormRecordID
                )
            )
            orderOfSections[4] -> navController.navigate(
                CurrentRxFragmentDirections.actionCurrentRxFragmentToOcularHealthFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[5] -> navController.navigate(
                CurrentRxFragmentDirections.actionCurrentRxFragmentToSupplementaryFragment(
                    navigateFormRecordID
                )
            )
            orderOfSections[6] -> navController.navigate(
                CurrentRxFragmentDirections.actionCurrentRxFragmentToContactLensFragment(
                    navigateFormRecordID
                )
            )
            orderOfSections[7] -> navController.navigate(
                CurrentRxFragmentDirections.actionCurrentRxFragmentToOrthokFragment(
                    navigateFormRecordID
                )
            )
            orderOfSections[8] -> {
                navController.navigate(
                    CurrentRxFragmentDirections
                        .actionCurrentRxFragmentToCashOrderFragment(navigateFormRecordID)
                )
            }
            orderOfSections[9] -> navController.navigate(
                CurrentRxFragmentDirections.actionCurrentRxFragmentToFinalPrescriptionFragment(
                    navigateFormRecordID
                )
            )
        }
    }

    private var takePhoto = false

    // todo - replace
    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//   Log.d(TAG, "initial file size = ${photoFile.length() / 1024} kBytes")
        if (requestCode == REQUEST_PHOTO && resultCode == Activity.RESULT_OK) {
            takePhoto = true
            scaleBitmap(photoFile)
            currentForm.reservedField = storageRef.toString()
            storageRef.putFile(storageFile)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        binding.rotatePhoto.visibility = View.VISIBLE
                        updatePhotoView(photoFile)
                    }
                }
            if (photoUri != null) {
                requireActivity().revokeUriPermission(
                    photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
    }

    private fun scaleBitmap(photoFile: File) {
        //take photoFile, compress it, delete original and replace with scaled
        val bitmap = getScaledBitmap(photoFile.path, PHOTO_W, PHOTO_H)

        if (bitmap != null) {
            try {
                val os = FileOutputStream(photoFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                os.flush()
                os.close()
            } catch (e: Exception) {
                Log.d(TAG, "Error writing bitmap", e)
            }
        }
    }


    private fun updatePhotoView(photoFile: File) {
        //   taskToGetFile
        if (currentForm.reservedField.isBlank()){
            currentForm.reservedField = storageRef.toString()
        }
        if (currentForm.reservedField.isNotBlank() && currentForm.reservedField != "deleted") {
            downloadPhotoTask = storageRef.getFile(photoFile).addOnSuccessListener {
                Log.d(TAG, "FB photo downloaded to local file")
                patientViewModel.readyToShowPhoto()
                currentForm.reservedField = photoFile.toString()
            }.addOnFailureListener {
                // delete local file
                Log.d(TAG, "No such file exist or error downloading. Delete local file")
                if (photoFile.exists()) photoFile.delete()
                patientViewModel.readyToShowPhoto()
                currentForm.reservedField = ""
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

    @SuppressLint("SetTextI18n")
    private fun fillTheForm(patientForm: PatientsEntity) {

        val extractData = patientForm.sectionData.split('|').toMutableList()
//      Log.d(TAG, "extract data size before = ${extractData.size}")
        if (extractData.size < 13) {
            for (index in extractData.size..13) {
                extractData.add("")
            }
        }

        binding.apply {

            sectionEditDate = patientForm.dateOfSection
            //     patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(patientForm.dateOfSection)
            //  Log.d(TAG, " Extracted data: $extractData" )

            var isEmpty = true
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
            if (isEmpty) spinnerCurrentlyUsing.setSelection(0)

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

            editRightAxis.setText(extractData[3])

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

            editLeftAxis.setText(extractData[6])

            if (extractData[7] != "") editRightVa.setText(extractData[7]) else editRightVa.setText(
                vaDefault
            )
            if (extractData[8] != "") editLeftVa.setText(extractData[8]) else editLeftVa.setText(
                vaDefault
            )
            if (extractData[9] != "") ouVa.setText(extractData[9]) else ouVa.setText(
                vaDefault
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
            isEmpty = true

            currentLens.setText(extractData[11])
            lensYear.setText(extractData[12])

            remarkInput.setText(patientForm.remarks)

            patientViewModel.practitioner.observe(viewLifecycleOwner) {
                val adapterPractitioner =
                    ArrayAdapter(requireContext(), R.layout.spinner_list_basic_, it)
                practitionerName.adapter = adapterPractitioner
                val isCreated = Constants.isCreatedForm(requireContext())
                if (isCreated) {
                    practitionerName.setSelection(1)
                    saveAndNavigate("none")
                } else {
                    it.forEachIndexed { index, s ->
                        if (s == patientForm.practitioner)
                            practitionerName.setSelection(index)
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
                    ouVa.text.toString() + "|" +
                    spinnerAdd.selectedItem.toString() + "|" +
                    currentLens.text.toString() + "|" +
                    lensYear.text.toString()

            currentForm.sectionData = extractData.uppercase()

//            val dataSelected = binding.practitionerName.selectedItem as String
//
//            val dataPractitioner = StringBuilder(dataSelected)
//            val count = binding.practitionerName.adapter.count
//            for (i in 0 until count) {
//                val a = binding.practitionerName.adapter.getItem(i)
//                if (a.toString() != dataSelected) {
//                    dataPractitioner.append("|$a")
//                }
//            }
            currentForm.practitioner = (binding.practitionerName.selectedItem as String).uppercase()

            if (takePhoto) {
                takePhoto = false
                binding.rotatePhoto.visibility = View.INVISIBLE
                currentForm.reservedField = storageRef.toString()
                val bitmapDrawable = binding.autorefPhoto.drawable as? BitmapDrawable
                val bitmap = bitmapDrawable?.bitmap
                if (bitmap != null) {
                    try {
                        val os = FileOutputStream(photoFile)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                        os.flush()
                        os.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                storageRef.putFile(storageFile).addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        updatePhotoView(photoFile)
//                    }
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
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.clear()
    }
}
