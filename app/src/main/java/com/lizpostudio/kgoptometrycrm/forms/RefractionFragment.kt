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
import com.lizpostudio.kgoptometrycrm.database.Patients
import com.lizpostudio.kgoptometrycrm.databinding.FragmentRefractionBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import java.io.File
import java.io.FileOutputStream

class RefractionFragment : Fragment() {

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

    private var photoUri: Uri? = null

    private var _binding: FragmentRefractionBinding? = null
    private val binding get() = _binding!!

    private var isAdmin = false

    private var viewOnlyMode = false

    private var recordID = 0L
    private var photoFile: File = File("com.lizpostudio.kgoptometrycrm")
    private var filesDir = File("com.lizpostudio.kgoptometrycrm")
    private var patientID = ""
    private var sectionEditDate = -1L
    private var showPhoto = true

    private var currentForm = Patients()
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
            R.layout.fragment_refraction,
            container,
            false
        )
        val app = requireNotNull(this.activity).application

        filesDir = app.applicationContext.filesDir

        // change BINDING to Respective forms args!
        val safeArgs: RefractionFragmentArgs by navArgs()
        recordID = safeArgs.recordID

        // get Patient data
        Log.d(TAG, "Get RF patient $recordID")
        patientViewModel.getPatientForm(recordID)
        createRefAssignPhFile()

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
        } else binding.mainLayout.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.lightBackground
            )
        )

        patientViewModel.patientForm.observe(viewLifecycleOwner) { patientForm ->
            patientForm?.let {
                currentForm = it
                patientID = it.patientID
                Log.d(TAG, "RF Patient recordID ${currentForm.recordID} received")
                patientViewModel.createRecordListener(currentForm.recordID)
                fillTheForm(it)
                patientViewModel.getAllFormsForPatient(patientID)
                updatePhotoView()
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
                val newList = mutableListOf<Patients>()

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
                val mapSectionName = mutableMapOf<String, MutableList<Patients>>()
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
                    if (patientForm == "REFRACTION") {
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

                val hPos = newSectionName.indexOf("REFRACTION")
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

        binding.showHideButton.setOnClickListener {
            showPhoto = !showPhoto
            if (showPhoto) {
                binding.autorefPhoto.visibility = View.VISIBLE
                binding.showHideButton.setImageResource(R.drawable.visibility_32)
            } else {
                binding.autorefPhoto.visibility = View.GONE
                binding.showHideButton.setImageResource(R.drawable.visibility_off_32)
            }
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
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
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
        binding.spinnerLeftSph1.adapter = sphSpinnerAdapter
        binding.spinnerRightSph1.adapter = sphSpinnerAdapter
        binding.spinnerLeftSph2.adapter = sphSpinnerAdapter
        binding.spinnerRightSph2.adapter = sphSpinnerAdapter
        binding.spinnerLeftSph3.adapter = sphSpinnerAdapter
        binding.spinnerRightSph3.adapter = sphSpinnerAdapter
        binding.spinnerLeftSph4.adapter = sphSpinnerAdapter
        binding.spinnerRightSph4.adapter = sphSpinnerAdapter

        //      sphSpinnerAdapter.notifyDataSetChanged()


        val cylListItems = cylList()
        val cylSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                cylListItems
            )

        //  cylSpinnerAdapter.setDropDownViewResource(R.layout.spinner_list_small_numbers)
        binding.spinnerLeftCyl1.adapter = cylSpinnerAdapter
        binding.spinnerRightCyl1.adapter = cylSpinnerAdapter
        binding.spinnerLeftCyl2.adapter = cylSpinnerAdapter
        binding.spinnerRightCyl2.adapter = cylSpinnerAdapter
        binding.spinnerLeftCyl3.adapter = cylSpinnerAdapter
        binding.spinnerRightCyl3.adapter = cylSpinnerAdapter
        binding.spinnerLeftCyl4.adapter = cylSpinnerAdapter
        binding.spinnerRightCyl4.adapter = cylSpinnerAdapter
        //   cylSpinnerAdapter.notifyDataSetChanged()

        val addListItems = addList()
        val addSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                addListItems
            )

        //  addSpinnerAdapter.setDropDownViewResource(R.layout.spinner_list_small_numbers)
        binding.spinnerLeftAdd.adapter = addSpinnerAdapter
        binding.spinnerRightAdd.adapter = addSpinnerAdapter
        binding.spinnerAdd2.adapter = addSpinnerAdapter
        binding.spinnerAddMp.adapter = addSpinnerAdapter
        binding.spinnerAddLeft4.adapter = addSpinnerAdapter
        binding.spinnerAddRight4.adapter = addSpinnerAdapter
        //    addSpinnerAdapter.notifyDataSetChanged()

        // chart input adapter
        ArrayAdapter.createFromResource(
            app.applicationContext,
            R.array.chart_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerChart.adapter = adapter
        }

        patientViewModel.navTrigger.observe(viewLifecycleOwner) { navOption ->
            navOption?.let {
                Log.d(TAG, "RF: Launching Navigator: Nav Option == ${navOption}")
                launchNavigator(navOption)
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
                if (ifDeleted)
                    navController.navigate(
                        RefractionFragmentDirections.actionRefractionFragmentToFormSelectionFragment(
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
                        if (photoFile.exists()) photoFile.delete()
                        currentForm.reservedField = ""
                        storageRef.delete() // .addOnCompleteListener { task ->}
                        binding.autorefPhoto.setImageDrawable(null)
                    }
                }
            } else {
                Toast.makeText(app.applicationContext, "Nothing to delete!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.copyToMpmva.setOnClickListener {
            binding.apply {
                spinnerLeftSph3.setSelection(spinnerLeftSph2.selectedItemPosition)
                spinnerRightSph3.setSelection(spinnerRightSph2.selectedItemPosition)
                spinnerLeftCyl3.setSelection(spinnerLeftCyl2.selectedItemPosition)
                spinnerRightCyl3.setSelection(spinnerRightCyl2.selectedItemPosition)
                editLeftAxis3.setText(editLeftAxis2.text.toString())
                editRightAxis3.setText(editRightAxis2.text.toString())
                ouVa.setText(editRightPd.text.toString())
                spinnerAddMp.setSelection(spinnerAdd2.selectedItemPosition)
            }
        }

        binding.copyToPrescribe.setOnClickListener {
            binding.apply {
                spinnerLeftSph4.setSelection(spinnerLeftSph2.selectedItemPosition)
                spinnerRightSph4.setSelection(spinnerRightSph2.selectedItemPosition)
                spinnerLeftCyl4.setSelection(spinnerLeftCyl2.selectedItemPosition)
                spinnerRightCyl4.setSelection(spinnerRightCyl2.selectedItemPosition)
                editLeftAxis4.setText(editLeftAxis2.text.toString())
                editRightAxis4.setText(editRightAxis2.text.toString())
                vaRight4.setText(editRightVa.text.toString())
                vaLeft4.setText(editLeftVa.text.toString())
                spinnerAddRight4.setSelection(spinnerRightAdd.selectedItemPosition)
                spinnerAddLeft4.setSelection(spinnerLeftAdd.selectedItemPosition)

            }
        }

        patientViewModel.patientFireForm.observe(viewLifecycleOwner) { patientNewRecord ->
            patientNewRecord?.let {
                Log.d(TAG, "Reload RF Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(TAG, "Refraction Record from FB loaded")
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
                Log.d(TAG, "Ref form CHANGED")
                Log.d(TAG, "Submiting to FDB record ID ${currentForm.recordID}")
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(),
                    currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                Log.d(TAG, "Ref form the SAME!!!")
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
                RefractionFragmentDirections.actionRefractionFragmentToFormSelectionFragment(
                    patientID
                )
            )
            "home" -> findNavController().navigate(
                RefractionFragmentDirections.actionToDatabaseSearchFragment()
            )
            else -> navigateToSelectedForm()
        }
    }

    private fun createRefAssignPhFile() {
        photoFile = File(filesDir, "IMG_$recordID.jpg")
        storageRef = patientViewModel.assignStorageRef("IMG_$recordID.jpg")
        storageFile = Uri.fromFile(photoFile)
    }

    private var takePhoto = false

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
                        updatePhotoView()
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

        //     photoFile.delete()
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

    private fun updatePhotoView() {
        //   taskToGetFile
        if (currentForm.reservedField.isBlank()){
            currentForm.reservedField = storageRef.toString()
        }

        if (currentForm.reservedField.isNotBlank() && currentForm.reservedField != "deleted") {
            downloadPhotoTask = storageRef.getFile(photoFile).addOnSuccessListener {
                Log.d(TAG, "FB photo downloaded to local file")
                patientViewModel.readyToShowPhoto()
//                currentForm.reservedField = photoFile.toString()
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
    private fun fillTheForm(patientForm: Patients) {

        val extractData = patientForm.sectionData.split('|').toMutableList()
        //      Log.d(TAG, "extract data size before = ${extractData.size}")
        if (extractData.size < 47) {
            for (index in extractData.size..47) {
                extractData.add("")
            }
        }

        binding.apply {

            sectionEditDate = patientForm.dateOfSection
            //    patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(patientForm.dateOfSection)

            var isEmpty = true
            for (i in 0 until spinnerRightSph1.adapter.count) {
                if (extractData[0].trim() != "" &&
                    extractData[0] == spinnerRightSph1.adapter.getItem(i).toString()
                ) {
                    spinnerRightSph1.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerRightSph1.adapter.count) {
                    if (" " == spinnerRightSph1.adapter.getItem(i).toString()) {
                        spinnerRightSph1.setSelection(i)
                    }
                }
            }
            isEmpty = true

            for (i in 0 until spinnerRightCyl1.adapter.count) {
                if (extractData[1].trim() != "" &&
                    extractData[1].trim().toDoubleOrNull() == spinnerRightCyl1.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerRightCyl1.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerRightCyl1.setSelection(0)
            isEmpty = true

            editRightAxis1.setText(extractData[2])

            for (i in 0 until spinnerLeftSph1.adapter.count) {
                if (extractData[3].trim() != "" &&
                    extractData[3] == spinnerLeftSph1.adapter.getItem(i).toString()
                ) {
                    spinnerLeftSph1.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerLeftSph1.adapter.count) {
                    if (" " == spinnerLeftSph1.adapter.getItem(i).toString()) {
                        spinnerLeftSph1.setSelection(i)
                    }
                }
            }
            isEmpty = true

            for (i in 0 until spinnerLeftCyl1.adapter.count) {
                if (extractData[4].trim() != "" &&
                    extractData[4].trim().toDoubleOrNull() == spinnerLeftCyl1.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerLeftCyl1.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerLeftCyl1.setSelection(0)
            isEmpty = true

            editLeftAxis1.setText(extractData[5])

            for (i in 0 until spinnerChart.adapter.count) {
                if (extractData[6] != "" &&
                    extractData[6] == spinnerChart.adapter.getItem(i).toString()
                ) {
                    spinnerChart.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerChart.setSelection(1)

//      Log.d(TAG, "Chart value = ${extractData[6]} ")

            isEmpty = true

            for (i in 0 until spinnerRightSph2.adapter.count) {
                if (extractData[7].trim() != "" &&
                    extractData[7] == spinnerRightSph2.adapter.getItem(i).toString()
                ) {
                    spinnerRightSph2.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerRightSph2.adapter.count) {
                    if (" " == spinnerRightSph2.adapter.getItem(i).toString()) {
                        spinnerRightSph2.setSelection(i)
                    }
                }
            }
            isEmpty = true

            for (i in 0 until spinnerRightCyl2.adapter.count) {
                if (extractData[8].trim() != "" &&
                    extractData[8].trim().toDoubleOrNull() == spinnerRightCyl2.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerRightCyl2.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerRightCyl2.setSelection(0)
            isEmpty = true

            editRightAxis2.setText(extractData[9])

            for (i in 0 until spinnerLeftSph2.adapter.count) {
                if (extractData[10].trim() != "" &&
                    extractData[10] == spinnerLeftSph2.adapter.getItem(i).toString()
                ) {
                    spinnerLeftSph2.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerLeftSph2.adapter.count) {
                    if (" " == spinnerLeftSph2.adapter.getItem(i).toString()) {
                        spinnerLeftSph2.setSelection(i)
                    }
                }
            }
            isEmpty = true

            for (i in 0 until spinnerLeftCyl2.adapter.count) {
                if (extractData[11].trim() != "" &&
                    extractData[11].trim().toDoubleOrNull() == spinnerLeftCyl2.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerLeftCyl2.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerLeftCyl2.setSelection(0)
            isEmpty = true

            editLeftAxis2.setText(extractData[12])
            if (extractData[13] != "") editRightVa.setText(extractData[13]) else editRightVa.setText(
                vaDefault
            )
            for (i in 0 until spinnerRightAdd.adapter.count) {
                if (extractData[14].trim() != "" &&
                    extractData[14].trim().toDoubleOrNull() == spinnerRightAdd.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerRightAdd.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerRightAdd.setSelection(0)
            isEmpty = true

            editRightPd.setText(extractData[15])

            if (extractData[16] != "") editLeftVa.setText(extractData[16]) else editLeftVa.setText(
                vaDefault
            )
            for (i in 0 until spinnerLeftAdd.adapter.count) {
                if (extractData[17].trim() != "" &&
                    extractData[17].trim().toDoubleOrNull() == spinnerLeftAdd.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerLeftAdd.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerLeftAdd.setSelection(0)
            isEmpty = true

            //       editLeftPd.setText(extractData[18])
            nearVa.setText(extractData[19])
            nearVa2.setText(extractData[20])

            for (i in 0 until spinnerAdd2.adapter.count) {
                if (extractData[21].trim() != "" &&
                    extractData[21].trim().toDoubleOrNull() == spinnerAdd2.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerAdd2.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerAdd2.setSelection(0)
            isEmpty = true

            for (i in 0 until spinnerRightSph3.adapter.count) {
                if (extractData[22].trim() != "" &&
                    extractData[22] == spinnerRightSph3.adapter.getItem(i).toString()
                ) {
                    spinnerRightSph3.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerRightSph3.adapter.count) {
                    if (" " == spinnerRightSph3.adapter.getItem(i).toString()) {
                        spinnerRightSph3.setSelection(i)
                    }
                }
            }
            isEmpty = true

            for (i in 0 until spinnerRightCyl3.adapter.count) {
                if (extractData[23].trim() != "" &&
                    extractData[23].trim().toDoubleOrNull() == spinnerRightCyl3.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerRightCyl3.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerRightCyl3.setSelection(0)
            isEmpty = true

            editRightAxis3.setText(extractData[24])

            for (i in 0 until spinnerLeftSph3.adapter.count) {
                if (extractData[25].trim() != "" &&
                    extractData[25] == spinnerLeftSph3.adapter.getItem(i).toString()
                ) {
                    spinnerLeftSph3.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerLeftSph3.adapter.count) {
                    if (" " == spinnerLeftSph3.adapter.getItem(i).toString()) {
                        spinnerLeftSph3.setSelection(i)
                    }
                }
            }
            isEmpty = true

            for (i in 0 until spinnerLeftCyl3.adapter.count) {
                if (extractData[26].trim() != "" &&
                    extractData[26].trim().toDoubleOrNull() == spinnerLeftCyl3.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerLeftCyl3.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerLeftCyl3.setSelection(0)
            isEmpty = true

            editLeftAxis3.setText(extractData[27])
            if (extractData[28] != "") ouVa.setText(extractData[28]) else ouVa.setText(vaDefault)

            for (i in 0 until spinnerAddMp.adapter.count) {
                if (extractData[29].trim() != "" &&
                    extractData[29].trim().toDoubleOrNull() == spinnerAddMp.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerAddMp.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerAddMp.setSelection(0)

            val mergedStatus = "${extractData[30]}${extractData[31]}${extractData[32]}"
            currentStatusInput.setText(mergedStatus)
            /*     historyInput.setText(extractData[31])
                 mainComplaintInput.setText(extractData[32])*/

            // ===================TO PRESCRIBE SECTION 33 -40 ========================
            isEmpty = true

            for (i in 0 until spinnerRightSph4.adapter.count) {
                if (extractData[33].trim() != "" &&
                    extractData[33] == spinnerRightSph4.adapter.getItem(i).toString()
                ) {
                    spinnerRightSph4.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerRightSph4.adapter.count) {
                    if (" " == spinnerRightSph4.adapter.getItem(i).toString()) {
                        spinnerRightSph4.setSelection(i)
                    }
                }
            }
            isEmpty = true

            for (i in 0 until spinnerRightCyl4.adapter.count) {
                if (extractData[34].trim() != "" &&
                    extractData[34].trim().toDoubleOrNull() == spinnerRightCyl4.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerRightCyl4.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerRightCyl4.setSelection(0)
            isEmpty = true

            editRightAxis4.setText(extractData[35])

            for (i in 0 until spinnerLeftSph4.adapter.count) {
                if (extractData[36].trim() != "" &&
                    extractData[36] == spinnerLeftSph4.adapter.getItem(i).toString()
                ) {
                    spinnerLeftSph4.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerLeftSph4.adapter.count) {
                    if (" " == spinnerLeftSph4.adapter.getItem(i).toString()) {
                        spinnerLeftSph4.setSelection(i)
                    }
                }
            }
            isEmpty = true

            for (i in 0 until spinnerLeftCyl4.adapter.count) {
                if (extractData[37].trim() != "" &&
                    extractData[37].trim().toDoubleOrNull() == spinnerLeftCyl4.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerLeftCyl4.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerLeftCyl4.setSelection(0)

            editLeftAxis4.setText(extractData[38])

            if (extractData[42] != "") vaRight4.setText(extractData[42]) else vaRight4.setText(
                vaDefault
            )
            if (extractData[43] != "") vaLeft4.setText(extractData[43]) else vaLeft4.setText(
                vaDefault
            )

            isEmpty = true
            for (i in 0 until spinnerAddRight4.adapter.count) {
                if (extractData[44].trim() != "" &&
                    extractData[44].trim().toDoubleOrNull() == spinnerAddRight4.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerAddRight4.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerAddRight4.setSelection(0)

            isEmpty = true
            for (i in 0 until spinnerAddLeft4.adapter.count) {
                if (extractData[45].trim() != "" &&
                    extractData[45].trim().toDoubleOrNull() == spinnerAddLeft4.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerAddLeft4.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerAddLeft4.setSelection(0)


            editManagementRefraction.setText(extractData[41])


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

    private fun navigateToSelectedForm() {
        val orderOfSections = listOf(*resources.getStringArray(R.array.forms_order))
        // if same fragment - load new record
        val navController = this.findNavController()

        when (navigateFormName) {

            orderOfSections[0] -> navController.navigate(
                RefractionFragmentDirections
                    .actionRefractionFragmentToInfoFragment(navigateFormRecordID)
            )

            orderOfSections[1] -> navController.navigate(
                RefractionFragmentDirections.actionRefractionFragmentToMemoFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[2] -> navController.navigate(
                RefractionFragmentDirections.actionRefractionFragmentToCurrentRxFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[3] -> {
                Log.d(TAG, "Navigating to another RF form:")
                Log.d(TAG, "Old Record ID = $recordID")
                Log.d(TAG, "New Record ID = $navigateFormRecordID")
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    createRefAssignPhFile()
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }

            }
            orderOfSections[4] -> navController.navigate(
                RefractionFragmentDirections.actionRefractionFragmentToOcularHealthFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[5] -> navController.navigate(
                RefractionFragmentDirections.actionRefractionFragmentToSupplementaryFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[6] -> navController.navigate(
                RefractionFragmentDirections.actionRefractionFragmentToContactLensFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[7] -> navController.navigate(
                RefractionFragmentDirections.actionRefractionFragmentToOrthokFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[8] -> {
                navController.navigate(
                    RefractionFragmentDirections
                        .actionRefractionFragmentToCashOrderFragment(navigateFormRecordID)
                )
            }

            orderOfSections[9] -> navController.navigate(
                RefractionFragmentDirections.actionRefractionFragmentToFinalPrescriptionFragment(
                    navigateFormRecordID
                )
            )
            else -> Toast.makeText(context, getString(R.string.navigation_else), Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * If UI was changed - returns true
     */
    private fun formWasChanged(): Boolean {
        // create new Record, fill it in with Form data and pass to ViewModel with recordID to update DB

        val priorPatient = currentForm.copy()

        binding.apply {

            if (sectionEditDate != -1L) currentForm.dateOfSection = sectionEditDate

            currentForm.remarks = remarkInput.text.toString().uppercase()

            val extractData = spinnerRightSph1.selectedItem.toString() + "|" +
                    spinnerRightCyl1.selectedItem.toString() + "|" +
                    editRightAxis1.text.toString() + "|" +
                    spinnerLeftSph1.selectedItem.toString() + "|" +
                    spinnerLeftCyl1.selectedItem.toString() + "|" +
                    editLeftAxis1.text.toString() + "|" +
                    spinnerChart.selectedItem.toString() + "|" +
                    spinnerRightSph2.selectedItem.toString() + "|" +
                    spinnerRightCyl2.selectedItem.toString() + "|" +
                    editRightAxis2.text.toString() + "|" +
                    spinnerLeftSph2.selectedItem.toString() + "|" +
                    spinnerLeftCyl2.selectedItem.toString() + "|" +
                    editLeftAxis2.text.toString() + "|" +
                    editRightVa.text.toString() + "|" +
                    spinnerRightAdd.selectedItem.toString() + "|" +
                    editRightPd.text.toString() + "|" +
                    editLeftVa.text.toString() + "|" +
                    spinnerLeftAdd.selectedItem.toString() + "|" +
                    "|" +
                    nearVa.text.toString() + "|" +
                    nearVa2.text.toString() + "|" +
                    spinnerAdd2.selectedItem.toString() + "|" +
                    spinnerRightSph3.selectedItem.toString() + "|" +
                    spinnerRightCyl3.selectedItem.toString() + "|" +
                    editRightAxis3.text.toString() + "|" +
                    spinnerLeftSph3.selectedItem.toString() + "|" +
                    spinnerLeftCyl3.selectedItem.toString() + "|" +
                    editLeftAxis3.text.toString() + "|" +
                    ouVa.text.toString() + "|" +
                    spinnerAddMp.selectedItem.toString() + "|" +
                    currentStatusInput.text.toString() + "|" +
                    "|" +  // history Input [OLD]
                    "|" +  // main complaint [OLD]
                    spinnerRightSph4.selectedItem.toString() + "|" +
                    spinnerRightCyl4.selectedItem.toString() + "|" +
                    editRightAxis4.text.toString() + "|" +
                    spinnerLeftSph4.selectedItem.toString() + "|" +
                    spinnerLeftCyl4.selectedItem.toString() + "|" +
                    editLeftAxis4.text.toString() + "|" +
                    "|" +
                    "|" +
                    editManagementRefraction.text.toString() + "|" + // 41
                    vaRight4.text.toString() + "|" + //42
                    vaLeft4.text.toString() + "|" + //43
                    spinnerAddRight4.selectedItem.toString() + "|" + //44
                    spinnerAddLeft4.selectedItem.toString() //45

            currentForm.sectionData = extractData.uppercase()

            currentForm.practitioner = (binding.practitionerName.selectedItem as String).uppercase()

            if (takePhoto) {
                takePhoto = false
                binding.rotatePhoto.visibility = View.INVISIBLE
                currentForm.reservedField = storageRef.toString()
                val bitmapDrawable = binding.autorefPhoto.drawable as BitmapDrawable
                val bitmap = bitmapDrawable.bitmap
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
            val datePickerDialog = DatePickerDialog(it, { _, year, monthOfYear, dayOfMonth -> // set day of month , month and year value in the edit text
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