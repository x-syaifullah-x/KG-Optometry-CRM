package com.lizpostudio.kgoptometrycrm.forms

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.storage.StorageReference
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.ViewModelProviderFactory
import com.lizpostudio.kgoptometrycrm.camera.CameraActivity
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.FragmentRefractionBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

class RefractionFragment : Fragment() {

    companion object {
        const val VA_DEFAULT = "6/"
        private const val PHOTO_W = 500
        private const val PHOTO_H = 700

        private lateinit var storageRef: StorageReference
    }

    private val patientViewModel: PatientsViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }

    private val bindingRoot by viewBinding<FragmentRefractionBinding>()

    private val binding by lazy { bindingRoot.content }

    private var isAdmin = false

    private var viewOnlyMode = false

    private var recordID = 0L

    private var patientID = ""
    private var sectionEditDate = -1L
    private var showPhoto = true

    private var currentForm = PatientEntity()
    private var navigateFormName = ""
    private var navigateFormRecordID = -1L

    private var isTakePhoto = false

    private val takePicture = registerForActivityResult(CameraActivity.ResultContract()) { result ->
        if (result != null) {
            val parcelFileDescriptor =
                requireContext().contentResolver.openFileDescriptor(result, "rw")
                    ?: return@registerForActivityResult
            isTakePhoto = true
            val bitmap = getScaledBitmap(
                parcelFileDescriptor.fileDescriptor, PHOTO_W, PHOTO_H
            )
            parcelFileDescriptor.close()
            requireContext().contentResolver.delete(result, null, null)
            binding.rotatePhoto.visibility = View.VISIBLE
            binding.autorefPhoto.setImageBitmap(bitmap)
            currentForm.reservedField = storageRef.toString()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            saveAndNavigate("back")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val app = requireNotNull(this.activity).application

//        filesDir = app.applicationContext.filesDir

        // change BINDING to Respective forms args!
        val safeArgs: RefractionFragmentArgs by navArgs()
        recordID = safeArgs.recordID

        // get Patient data
        Log.d(Constants.TAG, "Get RF patient $recordID")
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
                val navChipGroup2 = bindingRoot.nav2.navigationLayout2
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
                            createRefAssignPhFile()
                            updatePhotoView()
                        } else
                            chip.setBackgroundColor(
                                ContextCompat.getColor(
                                    app.applicationContext,
                                    R.color.cardBackgroundDarker
                                )
                            )

                        val sectionShortName =
                            makeShortSectionName(requireContext(), patientForm.sectionName)
                        val text =
                            "$sectionShortName\n${convertLongToDDMMYY(patientForm.dateOfSection)}"
                        chip.text = text

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
                    val scrollWidth = bindingRoot.nav2.chipsScroll2.width
                    val scrollX = ((hPosBottomNav - 2) * (scrollWidth / 6.25)).toInt()
                    bindingRoot.nav2.chipsScroll2.postDelayed({
                        if (context != null)
                            bindingRoot.nav2.chipsScroll2.smoothScrollTo(scrollX, 0)
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
                binding.showHideButton.setImageResource(R.drawable.visibility_36)
            } else {
                binding.autorefPhoto.visibility = View.GONE
                binding.showHideButton.setImageResource(R.drawable.visibility_off_32)
            }
        }

        binding.photoButton.setOnClickListener {
            val uri = FileProvider.getUriForFile(
                it.context,
                Constants.FILE_PROVIDER_AUTHORITY,
                File.createTempFile("tmp_", ".png")
            )
            takePicture.launch(uri)
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

        ArrayAdapter.createFromResource(
            app.applicationContext,
            R.array.ret_currentrx_unaided,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerRetCurrentrxUnaided.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            app.applicationContext,
            R.array.dominance,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_list_basic)
            binding.spinnerDominance.adapter = adapter
        }

        patientViewModel.navTrigger.observe(viewLifecycleOwner) { navOption ->
            navOption?.let {
                Log.d(Constants.TAG, "RF: Launching Navigator: Nav Option == $navOption")
                launchNavigator(navOption)
            }
        }

//        patientViewModel.photoFromFBReady.observe(viewLifecycleOwner) { ready ->
//            ready?.let {
//                Log.d(Constants.TAG, "Photo file is $it")
//                if (it)
//                    uploadPhotoFileToImage()
//            }
//        }

// DELETE FORM FUNCTIONALITY

        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { ifDeleted ->
            ifDeleted?.let {
                if (ifDeleted)
                    navController.navigate(
                        RefractionFragmentDirections.actionToFormSelectionFragment(patientID)
                    )
            }
        }

        bindingRoot.nav2.deleteForm.setOnClickListener {
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
            if (binding.autorefPhoto.drawable != null) {
                actionConfirmDeletion(
                    title = resources.getString(R.string.photo_delete_title),
                    message = resources.getString(R.string.photo_delete),
                    isAdmin, requireContext(), checkPassword = false
                ) { allowed ->
                    if (allowed) {
//                        if (photoFile.exists())
//                            photoFile.delete()
                        currentForm.reservedField = ""
                        storageRef.delete() // .addOnCompleteListener { task ->}
                        binding.autorefPhoto.setImageDrawable(null)
                    }
                }
            }
//            if (photoFile.exists()) {
//                actionConfirmDeletion(
//                    title = resources.getString(R.string.photo_delete_title),
//                    message = resources.getString(R.string.photo_delete),
//                    isAdmin, requireContext(), checkPassword = false
//                ) { allowed ->
//                    if (allowed) {
//                        if (photoFile.exists()) photoFile.delete()
//                        currentForm.reservedField = ""
//                        storageRef.delete() // .addOnCompleteListener { task ->}
//                        binding.autorefPhoto.setImageDrawable(null)
//                    }
//                }
//            } else {
//                Toast.makeText(app.applicationContext, "Nothing to delete!", Toast.LENGTH_SHORT).show()
//            }
        }

        binding.copyToMpmva.setOnClickListener {
            binding.apply {
                spinnerLeftSph3.setSelection(spinnerLeftSph2.selectedItemPosition)
                spinnerRightSph3.setSelection(spinnerRightSph2.selectedItemPosition)
                spinnerLeftCyl3.setSelection(spinnerLeftCyl2.selectedItemPosition)
                spinnerRightCyl3.setSelection(spinnerRightCyl2.selectedItemPosition)
                editLeftAxis3.setText(editLeftAxis2.text.toString())
                editRightAxis3.setText(editRightAxis2.text.toString())
                editOuva2.setText(editOuva.text.toString())
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
                Log.d(Constants.TAG, "Reload RF Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(Constants.TAG, "Refraction Record from FB loaded")
                    currentForm.copyFrom(it)
                    fillTheForm(it)
                }
            }
        }

        bindingRoot.nav2.saveFormButton.setOnClickListener {
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
                BitmapUtils.rotate(
                    (binding.autorefPhoto.drawable as BitmapDrawable).bitmap, 90F
                )

            binding.autorefPhoto.setImageBitmap(bitmap)
        }
        return bindingRoot.root
    }

    private fun saveAndNavigate(navOption: String) {
        patientViewModel.removeRecordsChangesListener()
        if (viewOnlyMode) {
            launchNavigator(navOption)
        } else {
            if (isTakePhoto) {
                isTakePhoto = false
                binding.rotatePhoto.visibility = View.INVISIBLE
                currentForm.reservedField = storageRef.toString()
                val bitmapDrawable = (binding.autorefPhoto.drawable as? BitmapDrawable)
                val bitmap = bitmapDrawable?.bitmap
                if (bitmap != null) {
                    try {
                        val os = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                        storageRef.putBytes(os.toByteArray()).addOnCompleteListener {
                            os.close()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            if (formWasChanged()) {
                Log.d(Constants.TAG, "Ref form CHANGED")
                Log.d(Constants.TAG, "Submiting to FDB record ID ${currentForm.recordID}")
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(),
                    currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                Log.d(Constants.TAG, "Ref form the SAME!!!")
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
                RefractionFragmentDirections.actionToFormSelectionFragment(patientID)
            )

            "home" -> findNavController().navigate(
                RefractionFragmentDirections.actionToDatabaseSearchFragment()
            )

            else -> navigateToSelectedForm()
        }
    }

    private fun createRefAssignPhFile() {
//        photoFile = File(filesDir, "IMG_$recordID.jpg")
        storageRef = patientViewModel.assignStorageRef("IMG_$recordID.jpg")
//        storageFile = Uri.fromFile(photoFile)
    }

    private fun updatePhotoView() {
        uploadPhotoFileToImage(null)
        if (currentForm.reservedField.isBlank()) {
            currentForm.reservedField = storageRef.toString()
        }

        if (currentForm.reservedField.isNotBlank()) {
            storageRef.getStream { state, stream ->
                if (state.error != null) {
                    uploadPhotoFileToImage(null)
                } else {
                    uploadPhotoFileToImage(stream)
                }
            }.addOnFailureListener {
                uploadPhotoFileToImage(null)
            }
        } else {
            uploadPhotoFileToImage(null)
        }
    }

    private fun uploadPhotoFileToImage(inputStream: InputStream?) {
        CoroutineScope(Dispatchers.IO).launch {
            val bitmap = BitmapFactory.decodeStream(inputStream)
            withContext(Dispatchers.Main) {
                try {
                    if (bitmap != null) {
                        binding.autorefPhoto.setImageBitmap(bitmap)
                    } else {
                        binding.autorefPhoto.setImageBitmap(null)
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }

    private fun fillTheForm(p: PatientEntity) {
        viewOnlyMode = p.isReadOnly
        if (viewOnlyMode) {
            bindingRoot.nav2.viewOnlyButton.setImageResource(R.drawable.visibility_36)
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.viewOnlyMode)
            )
            bindingRoot.nav2.saveFormButton.visibility = View.GONE
        } else {
            bindingRoot.nav2.viewOnlyButton.setImageResource(R.drawable.ic_read_write_36)
            binding.mainLayout.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.lightBackground)
            )
            bindingRoot.nav2.saveFormButton.visibility = View.VISIBLE
        }

        bindingRoot.nav2.viewOnlyButton.setOnClickListener {
            viewOnlyMode = !viewOnlyMode
            if (viewOnlyMode) {
                bindingRoot.nav2.viewOnlyButton.setImageResource(R.drawable.visibility_36)
                binding.mainLayout.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.viewOnlyMode
                    )
                )
                bindingRoot.nav2.saveFormButton.visibility = View.GONE
            } else {
                bindingRoot.nav2.viewOnlyButton.setImageResource(R.drawable.ic_read_write_36)
                binding.mainLayout.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.lightBackground)
                )
                bindingRoot.nav2.saveFormButton.visibility = View.VISIBLE
            }

            patientViewModel.updateIsReadOnly("${p.recordID}", viewOnlyMode)
        }

        val extractData = p.sectionData.split('|').toMutableList()
        if (extractData.size < 51) {
            for (index in extractData.size..51) {
                extractData.add("")
            }
        }

        binding.apply {

            sectionEditDate = p.dateOfSection
            //    patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(p.dateOfSection)

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

            isEmpty = true
            if (extractData[46] != "") {
                for (i in 0 until spinnerRetCurrentrxUnaided.adapter.count) {
                    if (extractData[46] == spinnerRetCurrentrxUnaided.adapter.getItem(i)
                            .toString()
                    ) {
                        spinnerRetCurrentrxUnaided.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerRetCurrentrxUnaided.setSelection(0)

            isEmpty = true
            if (extractData[49] != "") {
                for (i in 0 until spinnerDominance.adapter.count) {
                    if (extractData[49] == spinnerDominance.adapter.getItem(i).toString()) {
                        spinnerDominance.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerDominance.setSelection(0)

//      Log.d(_root_ide_package_.com.lizpostudio.kgoptometrycrm.constant.Constants.TAG, "Chart value = ${extractData[6]} ")

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
                VA_DEFAULT
            )

            if (extractData[47] != "") editRightVa2.setText(extractData[47]) else editRightVa2.setText(
                VA_DEFAULT
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

            if (extractData[15] != "") editOuva.setText(extractData[15]) else editOuva.setText(
                VA_DEFAULT
            )


            if (extractData[16] != "") editLeftVa.setText(extractData[16]) else editLeftVa.setText(
                VA_DEFAULT
            )

            if (extractData[48] != "") editLeftVa2.setText(extractData[48]) else editLeftVa2.setText(
                VA_DEFAULT
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

            if (extractData[28] != "") editOuva2.setText(extractData[28]) else editOuva2.setText(
                VA_DEFAULT
            )

            if (extractData[50] != "") editOuva3.setText(extractData[50]) else editOuva3.setText(
                VA_DEFAULT
            )


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
                VA_DEFAULT
            )
            if (extractData[43] != "") vaLeft4.setText(extractData[43]) else vaLeft4.setText(
                VA_DEFAULT
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

    private fun navigateToSelectedForm() {
        when (navigateFormName) {
            getString(R.string.info_form_caption) -> findNavController().navigate(
                RefractionFragmentDirections.actionToInfoFragment(navigateFormRecordID)
            )

            getString(R.string.follow_up_form_caption) -> findNavController().navigate(
                RefractionFragmentDirections.actionToFollowUpFragment(navigateFormRecordID)
            )

            getString(R.string.memo_form_caption) -> findNavController().navigate(
                RefractionFragmentDirections.actionToMemoFragment(navigateFormRecordID)
            )

            getString(R.string.current_rx_caption) -> findNavController().navigate(
                RefractionFragmentDirections.actionToCurrentRxFragment(navigateFormRecordID)
            )

            getString(R.string.refraction_caption) -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }

            getString(R.string.ocular_health_caption) -> findNavController().navigate(
                RefractionFragmentDirections.actionToOcularHealthFragment(navigateFormRecordID)
            )

            getString(R.string.supplementary_test_caption) -> findNavController().navigate(
                RefractionFragmentDirections.actionToSupplementaryFragment(navigateFormRecordID)
            )

            getString(R.string.contact_lens_exam_caption) -> findNavController().navigate(
                RefractionFragmentDirections.actionToContactLensFragment(navigateFormRecordID)
            )

            getString(R.string.orthox_caption) -> findNavController().navigate(
                RefractionFragmentDirections.actionToOrthokFragment(navigateFormRecordID)
            )

            getString(R.string.cash_order) -> findNavController().navigate(
                RefractionFragmentDirections.actionToCashOrderFragment(navigateFormRecordID)
            )

            getString(R.string.sales_order_caption) -> findNavController().navigate(
                RefractionFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )

            getString(R.string.final_prescription_caption) -> findNavController().navigate(
                RefractionFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )

            else -> {
                Toast.makeText(
                    context, "$navigateFormName not implemented yet", Toast.LENGTH_SHORT
                ).show()
            }
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

            val extractData = spinnerRightSph1.selectedItem.toString() + "|" +  //0
                    spinnerRightCyl1.selectedItem.toString() + "|" +
                    editRightAxis1.text.toString() + "|" +
                    spinnerLeftSph1.selectedItem.toString() + "|" +
                    spinnerLeftCyl1.selectedItem.toString() + "|" +
                    editLeftAxis1.text.toString() + "|" +
                    spinnerChart.selectedItem.toString() + "|" +
                    spinnerRightSph2.selectedItem.toString() + "|" +
                    spinnerRightCyl2.selectedItem.toString() + "|" +
                    editRightAxis2.text.toString() + "|" +
                    spinnerLeftSph2.selectedItem.toString() + "|" +  //10
                    spinnerLeftCyl2.selectedItem.toString() + "|" +
                    editLeftAxis2.text.toString() + "|" +
                    editRightVa.text.toString() + "|" +
                    spinnerRightAdd.selectedItem.toString() + "|" +
                    editOuva.text.toString() + "|" +
                    editLeftVa.text.toString() + "|" +
                    spinnerLeftAdd.selectedItem.toString() + "|" +
                    "|" +
                    nearVa.text.toString() + "|" +
                    nearVa2.text.toString() + "|" +  //20
                    spinnerAdd2.selectedItem.toString() + "|" +
                    spinnerRightSph3.selectedItem.toString() + "|" +
                    spinnerRightCyl3.selectedItem.toString() + "|" +
                    editRightAxis3.text.toString() + "|" +
                    spinnerLeftSph3.selectedItem.toString() + "|" +
                    spinnerLeftCyl3.selectedItem.toString() + "|" +
                    editLeftAxis3.text.toString() + "|" +
                    editOuva2.text.toString() + "|" +
                    spinnerAddMp.selectedItem.toString() + "|" +
                    currentStatusInput.text.toString() + "|" +  //30
                    "|" +  // history Input [OLD]
                    "|" +  // main complaint [OLD]
                    spinnerRightSph4.selectedItem.toString() + "|" +
                    spinnerRightCyl4.selectedItem.toString() + "|" +
                    editRightAxis4.text.toString() + "|" +
                    spinnerLeftSph4.selectedItem.toString() + "|" +
                    spinnerLeftCyl4.selectedItem.toString() + "|" +
                    editLeftAxis4.text.toString() + "|" +
                    "|" +
                    "|" +  //40
                    editManagementRefraction.text.toString() + "|" + // 41
                    vaRight4.text.toString() + "|" + //42
                    vaLeft4.text.toString() + "|" + //43
                    spinnerAddRight4.selectedItem.toString() + "|" + //44
                    spinnerAddLeft4.selectedItem.toString() + "|" + //45
                    spinnerRetCurrentrxUnaided.selectedItem.toString() + "|" + //46
                    editRightVa2.text.toString() + "|" + //47
                    editLeftVa2.text.toString() + "|" +//48
                    spinnerDominance.selectedItem.toString() + "|" + //49
                    editOuva3.text.toString() //50
            currentForm.sectionData = extractData.uppercase()

            currentForm.practitioner = (binding.practitionerName.selectedItem as String).uppercase()
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
                },
                todayYear,
                todayMonth,
                todayDay
            )
            datePickerDialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        storageRef.stream.cancel()
    }
}