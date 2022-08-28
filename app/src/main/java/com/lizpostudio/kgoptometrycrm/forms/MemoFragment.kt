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
import com.lizpostudio.kgoptometrycrm.databinding.FragmentMemoBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding
import java.io.File
import java.io.FileOutputStream

class MemoFragment : Fragment() {

    companion object {
        private lateinit var storageRef: StorageReference
        private lateinit var storageFile: Uri

        private const val REQUEST_PHOTO = 2
        private const val PHOTO_W = 600
        private const val PHOTO_H = 800
    }

    private var downloadPhotoTask: StorageTask<FileDownloadTask.TaskSnapshot>? = null
    private val patientViewModel: PatientsViewModel by viewModels {
        PatientsViewModelFactory(requireContext())
    }

    private var photoUri: Uri? = null
    private var photoFile: File = File(Constants.APPLICATION_ID)
    private var filesDir: File = File(Constants.APPLICATION_ID)

    private var isAdmin = false
    private var viewOnlyMode = false

    private val binding by viewBinding<FragmentMemoBinding>()

    private var recordID = 0L
    private var patientID = ""

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

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val app = requireNotNull(this.activity).application

        filesDir = app.applicationContext.filesDir

        val safeArgs: MemoFragmentArgs by navArgs()
        recordID = safeArgs.recordID

        // get Patient data
        patientViewModel.getPatientForm(recordID)

        createRefAssignPhFile()

        val navController = this.findNavController()

        // get if user is Admin
        val sharedPref = app.getSharedPreferences(
            Constants.PREF_NAME, Context.MODE_PRIVATE
        )
        isAdmin = (sharedPref?.getString("admin", "") ?: "") == "admin"

        viewOnlyMode = sharedPref?.getBoolean("viewOnly", false) ?: false
        if (viewOnlyMode) {
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.viewOnlyMode)
            )
            binding.saveFormButton.visibility = View.GONE
        } else binding.mainLayout.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.lightBackground)
        )

        binding.dateCaption.setOnClickListener {
            changeDate()
        }

        patientViewModel.patientForm.observe(viewLifecycleOwner) { patientForm ->
            patientForm?.let { patient ->
                currentForm.copyFrom(patient)
                patientID = patient.patientID

                //  === setup listener to this specific child and update the form if fields are changed ===

                Log.d(Constants.TAG, "SETUP fire record listener")
                patientViewModel.createRecordListener(currentForm.recordID)
                fillTheForm(patient)
                patientViewModel.getAllFormsForPatient(patientID)
                //      Log.d(Constants.TAG, "Loading photo")
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
                    if (patientForm == "MEMO") {
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

                val hPos = newSectionName.indexOf("MEMO")
                if (hPos > 3) {
                    val scrollWidth = binding.chipsScroll.width
                    val scrollX = ((hPos - 2) * (scrollWidth / 6.25)).toInt()
                    binding.chipsScroll.postDelayed({
                        if (context != null)
                            binding.chipsScroll.smoothScrollTo(scrollX, 0)
                    }, 100L)
                }

                val hPosList =
                    mapSectionName[sectionName]?.map { form -> form.recordID } ?: listOf()
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

        patientViewModel.navTrigger.observe(viewLifecycleOwner) { navOption ->
            navOption?.let {
                launchNavigator(navOption)
            }
        }
        // DELETE FORM FUNCTIONALITY

        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { ifDeleted ->
            ifDeleted?.let {
                if (ifDeleted) navController.navigate(
                    MemoFragmentDirections.actionToFormSelectionFragment(patientID)
                )
            }
        }

        binding.deleteForm.setOnClickListener {
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

        patientViewModel.patientFireForm.observe(viewLifecycleOwner) { patientNewRecord ->
            patientNewRecord?.let {
                Log.d(Constants.TAG, "Reload Memo Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(Constants.TAG, "Memo Record from FB loaded")
                    currentForm.copyFrom(it)
                    fillTheForm(it)
                }
            }
        }

        binding.saveFormButton.setOnClickListener {
            saveAndNavigate("none")
        }

        binding.photoButton.setOnClickListener {
            photoUri = FileProvider.getUriForFile(
                requireActivity(), Constants.FILE_PROVIDER_AUTHORITY, photoFile
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
                        captureImage, PackageManager.MATCH_DEFAULT_ONLY
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

        binding.deletePhoto.setOnClickListener {
            if (photoFile.exists()) {
                actionConfirmDeletion(
                    title = resources.getString(R.string.photo_delete_title),
                    message = resources.getString(R.string.photo_delete),
                    isAdmin, requireContext(), checkPassword = false
                ) { allowed ->
                    if (allowed) {
                        currentForm.reservedField = ""
                        photoFile.delete()
                        storageRef.delete() //.addOnCompleteListener { task -> }
                        //    binding.autorefPhoto.setImageDrawable(null)
                        binding.refPhoto.setImageDrawable(null)
                    }
                }
            } else {
                Toast.makeText(context, "Nothing to delete!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backButton.setOnClickListener {
            saveAndNavigate("back")
        }

        binding.homeButton.setOnClickListener {
            saveAndNavigate("home")
        }

        patientViewModel.photoFromFBReady.observe(viewLifecycleOwner) { ready ->
            ready?.let {
                if (it) uploadPhotoFileToImage()
            }
        }

        binding.rotatePhoto.setOnClickListener {
            val bitmap =
                BitmapUtils.rotate((binding.refPhoto.drawable as BitmapDrawable).bitmap, 90F)

            binding.refPhoto.setImageBitmap(bitmap)
//            try {
//                val os = FileOutputStream(photoFile)
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
//                os.flush()
//                os.close()
//                storageRef.putFile(storageFile).addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        updatePhotoView(photoFile)
//                    }
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
        }
        return binding.root
    }


    private fun scaleBitmap(photoFile: File) {
        //take photoFile, compress it, delete original and replace with scaled
        val bitmap = getScaledBitmap(
            photoFile.path, PHOTO_W, PHOTO_H
        )
//    photoFile.delete()
        if (bitmap != null) {
            try {
                val os = FileOutputStream(photoFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                os.flush()
                os.close()
            } catch (e: Exception) {
                //  Log.d(Constants.TAG, "Error writing bitmap", e)
            }
        }
    }

    private fun updatePhotoView(photoFile: File) {
        if (currentForm.reservedField.isNotBlank() && currentForm.reservedField != "deleted") {
            downloadPhotoTask = storageRef.getFile(photoFile).addOnSuccessListener {
                Log.d(Constants.TAG, "FB photo downloaded to Memo")
                patientViewModel.readyToShowPhoto()
                currentForm.reservedField = photoFile.toString()
            }.addOnFailureListener {
                // delete local file
                Log.d(Constants.TAG, "No such file at Memo. Delete local file")
                if (photoFile.exists()) photoFile.delete()
                patientViewModel.readyToShowPhoto()
                currentForm.reservedField = ""
            }
        } else {
            binding.refPhoto.setImageDrawable(null)
        }
    }

    private fun uploadPhotoFileToImage() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, PHOTO_W, PHOTO_H)
            if (bitmap != null) {
                binding.refPhoto.setImageBitmap(bitmap)
            }
        }
    }

    private var takePhoto = false

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PHOTO && resultCode == Activity.RESULT_OK) {
            takePhoto = true
            scaleBitmap(photoFile)
            currentForm.reservedField = storageRef.toString()
            val ois = requireContext().contentResolver.openInputStream(storageFile)
            storageRef.putStream(ois ?: return)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        binding.rotatePhoto.visibility = View.VISIBLE
                        updatePhotoView(photoFile)
                    }
                    ois.close()
                }
            if (photoUri != null) {
                requireActivity().revokeUriPermission(
                    photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }

        }
    }

    private fun saveAndNavigate(navOption: String) {
        patientViewModel.removeRecordsChangesListener()
        if (viewOnlyMode) {
            launchNavigator(navOption)
        } else {
            if (formWasChanged()) {
                Log.d(Constants.TAG, "Memo form CHANGED")
                Log.d(Constants.TAG, "Submiting to FB a DB record ID ${currentForm.recordID}")
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(),
                    currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                Log.d(Constants.TAG, "Memo form the SAME!!!")
                launchNavigator(navOption)
            }
        }
    }

    private fun launchNavigator(option: String) {
        when (option) {
            "none" -> {
                fillTheForm(currentForm)
            }
            "back" -> findNavController().navigate(
                MemoFragmentDirections.actionToFormSelectionFragment(
                    patientID
                )
            )
            "home" -> findNavController().navigate(
                MemoFragmentDirections.actionToDatabaseSearchFragment()
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
                MemoFragmentDirections.actionToInfoFragment(navigateFormRecordID)
            )
            getString(R.string.follow_up_form_caption) -> findNavController().navigate(
                MemoFragmentDirections.actionToFollowUpFragment(navigateFormRecordID)
            )
            getString(R.string.memo_form_caption) -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }
            getString(R.string.current_rx_caption) -> findNavController().navigate(
                MemoFragmentDirections.actionToCurrentRxFragment(navigateFormRecordID)
            )
            getString(R.string.refraction_caption) -> findNavController().navigate(
                MemoFragmentDirections.actionToRefractionFragment(navigateFormRecordID)
            )
            getString(R.string.ocular_health_caption) -> findNavController().navigate(
                MemoFragmentDirections.actionToOcularHealthFragment(navigateFormRecordID)
            )
            getString(R.string.supplementary_test_caption) -> findNavController().navigate(
                MemoFragmentDirections.actionToSupplementaryFragment(navigateFormRecordID)
            )
            getString(R.string.contact_lens_exam_caption) -> findNavController().navigate(
                MemoFragmentDirections.actionToContactLensFragment(navigateFormRecordID)
            )
            getString(R.string.orthox_caption) -> findNavController().navigate(
                MemoFragmentDirections.actionToOrthokFragment(navigateFormRecordID)
            )
            getString(R.string.cash_order) -> findNavController().navigate(
                MemoFragmentDirections.actionToCashOrderFragment(navigateFormRecordID)
            )
            getString(R.string.sales_order_caption) -> findNavController().navigate(
                MemoFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )
            getString(R.string.final_prescription_caption) -> findNavController().navigate(
                MemoFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )
            else -> {
                Toast.makeText(
                    context, "$navigateFormName not implemented yet", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fillTheForm(patientForm: PatientsEntity) {

        val extractData = patientForm.sectionData.split('|').toMutableList()
//      Log.d(Constants.TAG, "extract data size before = ${extractData.size}")
        if (extractData.size < 2) {
            for (index in extractData.size..2) {
                extractData.add("")
            }
        }
        binding.apply {
            //      patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(patientForm.dateOfSection)
            sectionEditDate = patientForm.dateOfSection
            settledCheck.isChecked = extractData[0] == "TRUE"
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

            mmInput.setText(patientForm.mm)
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
            val extractData = settledCheck.isChecked.toString() // + "|"
            currentForm.sectionData = extractData.uppercase()
            currentForm.practitioner = (binding.practitionerName.selectedItem as String).uppercase()

            currentForm.mm = "${binding.mmInput.text}".uppercase()

            if (takePhoto) {
                takePhoto = false
                binding.rotatePhoto.visibility = View.GONE
                currentForm.reservedField = storageRef.toString()
                val bitmapDrawable = binding.refPhoto.drawable as? BitmapDrawable
                val bitmap = bitmapDrawable?.bitmap
                if (bitmap != null) {
                    try {
                        val os = FileOutputStream(photoFile)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                        os.flush()
                        os.close()

                        val ois = requireContext().contentResolver.openInputStream(storageFile)
                        ois?.apply {
                            storageRef.putStream(ois)
                                .addOnCompleteListener {
                                    ois.close()
                                }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
                    if (sectionEditDate != -1L) binding.dateCaption.text = convertLongToDDMMYY(
                        sectionEditDate
                    )
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