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
import com.lizpostudio.kgoptometrycrm.OptometryApplication
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.PatientsViewModelFactory
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.database.Patients
import com.lizpostudio.kgoptometrycrm.databinding.FragmentMemoBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import java.io.File
import java.io.FileOutputStream

private lateinit var storageRef: StorageReference
private lateinit var storageFile: Uri

private const val REQUEST_PHOTO = 2
private const val PHOTO_W = 600
private const val PHOTO_H = 800

private const val TAG = "LogTrace"

class MemoFragment : Fragment() {
    private var downloadPhotoTask: StorageTask<FileDownloadTask.TaskSnapshot>? = null
    private val patientViewModel: PatientsViewModel by viewModels {
        PatientsViewModelFactory((requireNotNull(this.activity).application as OptometryApplication).repository)
    }

    private var photoUri: Uri? = null
    private var photoFile: File = File("com.lizpostudio.kgoptometrycrm")
    private var filesDir: File = File("com.lizpostudio.kgoptometrycrm")

    private var isAdmin = false
    private var viewOnlyMode = false

    private var _binding: FragmentMemoBinding? = null
    private val binding get() = _binding!!

    private var recordID = 0L
    private var patientID = ""

    private var sectionEditDate = -1L

    private var currentForm = Patients()
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

        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_memo,
            container,
            false
        )
        val app = requireNotNull(this.activity).application

        filesDir = app.applicationContext.filesDir

        val safeArgs: MemoFragmentArgs by navArgs()
        recordID = safeArgs.recordID

        // get Patient data
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
        } else binding.mainLayout.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.lightBackground
            )
        )

        binding.dateCaption.setOnClickListener {
            changeDate()
        }

        patientViewModel.patientForm.observe(viewLifecycleOwner) { patientForm ->
            patientForm?.let { patient ->
                currentForm.copyFrom(patient)
                patientID = patient.patientID

                //  === setup listener to this specific child and update the form if fields are changed ===

                Log.d(TAG, "SETUP fire record listener")
                patientViewModel.createRecordListener(currentForm.recordID)
                fillTheForm(patient)
                patientViewModel.getAllFormsForPatient(patientID)
                //      Log.d(TAG, "Loading photo")
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
                val newList = mutableListOf<Patients>()
                for (section in orderOfSections) {
                    for (forms in sortedList) {
                        if (section == forms.sectionName) newList.add(forms)
                    }
                }

                val navChipGroup = binding.navigationLayout
                val children = newList.map { patientForm ->
                    val chip = TextView(app.applicationContext)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                    chip.layoutParams = params
                    chip.setPadding((8 * screenDst).toInt(), 0, (8 * screenDst).toInt(), 0)

                    if (patientForm.recordID == recordID)
                        chip.setBackgroundColor(
                            ContextCompat.getColor(
                                app.applicationContext,
                                R.color.lightBackground
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
                    chip.text = "$sectionShortName\n${
                        convertLongToDDMMYY(patientForm.dateOfSection)
                    }"

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

                val hPosList = newList.map { form -> form.recordID }
                val hPos = hPosList.indexOf(recordID)
                if (hPos > 3) {
                    val scrollWidth = binding.chipsScroll.width
                    val scrollX = ((hPos - 2) * (scrollWidth / 6.25)).toInt()
                    binding.chipsScroll.postDelayed({
                        if (context != null)
                            binding.chipsScroll.smoothScrollTo(scrollX, 0)
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
                    MemoFragmentDirections.actionMemoFragmentToFormSelectionFragment(patientID)
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
                Log.d(TAG, "Reload Memo Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(TAG, "Memo Record from FB loaded")
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
                startActivityForResult(
                    captureImage, REQUEST_PHOTO
                )
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
                Toast.makeText(app.applicationContext, "Nothing to delete!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.backFromMemoToForms.setOnClickListener {
            saveAndNavigate("back")
        }

        patientViewModel.photoFromFBReady.observe(viewLifecycleOwner, { ready ->
            ready?.let {
                if (it) uploadPhotoFileToImage()
            }
        })

        return binding.root
    }


    private fun scaleBitmap(photoFile: File) {
        //take photoFile, compress it, delete original and replace with scaled
        val bitmap = getScaledBitmap(
            photoFile.path,
            PHOTO_W,
            PHOTO_H
        )
//    photoFile.delete()
        if (bitmap != null) {

            try {
                val os = FileOutputStream(photoFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                os.flush()
                os.close()
            } catch (e: Exception) {
                //  Log.d(TAG, "Error writing bitmap", e)
            }
        }
    }

    private fun updatePhotoView(photoFile: File) {
        if (currentForm.reservedField.isNotBlank() && currentForm.reservedField != "deleted") {
            downloadPhotoTask = storageRef.getFile(photoFile).addOnSuccessListener {
                Log.d(TAG, "FB photo downloaded to Memo")
                patientViewModel.readyToShowPhoto()
                currentForm.reservedField = photoFile.toString()
            }.addOnFailureListener {
                // delete local file
                Log.d(TAG, "No such file at Memo. Delete local file")
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

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //       Log.d(TAG, "initial file size = ${photoFile.length() / 1024} kBytes")
        if (requestCode == REQUEST_PHOTO && resultCode == Activity.RESULT_OK) {
            scaleBitmap(photoFile)
            currentForm.reservedField = storageRef.toString()
            storageRef.putFile(storageFile).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    updatePhotoView(photoFile)
                }
            }
            if (photoUri != null) {
                requireActivity().revokeUriPermission(
                    photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
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
                Log.d(TAG, "Memo form CHANGED")
                Log.d(TAG, "Submiting to FB a DB record ID ${currentForm.recordID}")
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(),
                    currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                Log.d(TAG, "Memo form the SAME!!!")
                launchNavigator(navOption)
            }
        }
    }

    private fun launchNavigator(option: String) {
        when (option) {
            "none" -> {
                Log.d(TAG, "No navigation triggered")
            }
            "back" -> this.findNavController().navigate(
                MemoFragmentDirections.actionMemoFragmentToFormSelectionFragment(
                    patientID
                )
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
        // if same fragment - load new record
        // info section could be onlyUnique
        when (navigateFormName) {

            orderOfSections[0] -> navController.navigate(
                MemoFragmentDirections
                    .actionMemoFragmentToInfoFragment(navigateFormRecordID)
            )

            orderOfSections[1] -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    createRefAssignPhFile()
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }

            orderOfSections[2] -> navController.navigate(
                MemoFragmentDirections.actionMemoFragmentToCurrentRxFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[3] -> navController.navigate(
                MemoFragmentDirections.actionMemoFragmentToRefractionFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[4] -> navController.navigate(
                MemoFragmentDirections.actionMemoFragmentToOcularHealthFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[5] -> navController.navigate(
                MemoFragmentDirections.actionMemoFragmentToSupplementaryFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[6] -> navController.navigate(
                MemoFragmentDirections.actionMemoFragmentToContactLensFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[7] -> navController.navigate(
                MemoFragmentDirections.actionMemoFragmentToOrthokFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[8] -> navController.navigate(
                MemoFragmentDirections.actionMemoFragmentToFinalPrescriptionFragment(
                    navigateFormRecordID
                )
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fillTheForm(patientForm: Patients) {

        val extractData = patientForm.sectionData.split('|').toMutableList()
//      Log.d(TAG, "extract data size before = ${extractData.size}")
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

            val dataPractitioner = arrayOf(patientForm.practitioner)
            val adapterPractitioner =
                ArrayAdapter(requireContext(), R.layout.spinner_list_basic_, dataPractitioner)
            practitionerName.adapter = adapterPractitioner
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
        _binding = null
    }
}