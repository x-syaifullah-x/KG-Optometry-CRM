package com.lizpostudio.kgoptometrycrm.forms

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
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
import com.lizpostudio.kgoptometrycrm.databinding.FragmentOrthokBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import java.io.File
import java.io.FileOutputStream

class OrthokFragment : Fragment() {

    companion object {
        private const val TAG = "LogTrace"
        private const val vaDefault = "6/"
    }

    private val patientViewModel: PatientsViewModel by viewModels {
        PatientsViewModelFactory(requireContext())
    }
    private var isAdmin = false

    private var fillMask = mutableListOf<MutableList<PointF>>()
    private var fillMaskBottom = mutableListOf<MutableList<PointF>>()
    private var fillIndex = -1
    private var fillIndexBottom = -1

    private var viewOnlyMode = false

    private var _binding: FragmentOrthokBinding? = null
    private val binding get() = _binding!!
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

    private var filesDir: File = File("com.lizpostudio.kgoptometrycrm")

    private lateinit var storageFile: Uri

    private fun createRefAssignPhFile() {
        photoFile = File(filesDir, "IMG_$recordID.jpg")
        storageRef = patientViewModel.assignStorageRef("IMG_$recordID.jpg")
        storageFile = Uri.fromFile(photoFile)
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        var topOculusVisible = true
        var bottomOculusVisible = false
        var upperLayoutVisible = true

        var startPTRightTop = PointF()
        var startPTRightBottom = PointF()

        val textBoxActiveTop = mutableListOf(false, false, false, false)
        val textBoxActiveBottom = mutableListOf(false, false, false, false)

        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_orthok,
            container,
            false
        )
        val app = requireNotNull(this.activity).application
        filesDir = app.applicationContext.filesDir

        var selectedColor = ContextCompat.getColor(requireContext(), R.color.greenCircle)
        // change BINDING to Respective forms args!

        val safeArgs: OrthokFragmentArgs by navArgs()
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

        // =========  INITIALIZE FIREBASE REFERENCE =============

        val sphListItems = sphList()
        val sphSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                sphListItems
            )

        binding.spinnerLeftSph.adapter = sphSpinnerAdapter
        binding.spinnerRightSph.adapter = sphSpinnerAdapter

        val cylListItems = cylList()
        val cylSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                cylListItems
            )

        binding.spinnerLeftCyl.adapter = cylSpinnerAdapter
        binding.spinnerRightCyl.adapter = cylSpinnerAdapter

        binding.imgColorSelected.setOnClickListener {
            binding.apply {
                imgColorGreen.visibility = View.VISIBLE
                imgColorRed.visibility = View.VISIBLE
                imgColorYellow.visibility = View.VISIBLE
            }
        }

        binding.imgColorRed.setOnClickListener {
            selectedColor = ContextCompat.getColor(requireContext(), R.color.redCircle)
            binding.imgColorSelected.setImageResource(R.drawable.red_circle)
            hideColors()
        }

        binding.imgColorYellow.setOnClickListener {
            selectedColor = ContextCompat.getColor(requireContext(), R.color.yellowCircle)
            binding.imgColorSelected.setImageResource(R.drawable.yellow_circle)
            hideColors()
        }
        binding.imgColorGreen.setOnClickListener {
            selectedColor = ContextCompat.getColor(requireContext(), R.color.greenCircle)
            binding.imgColorSelected.setImageResource(R.drawable.green_circle)
            hideColors()
        }

        binding.imgEditTop1.setOnClickListener {
            textBoxActiveTop[0] = !textBoxActiveTop[0]
            for (item in 1..3) {
                textBoxActiveTop[item] = false
            }
            if (textBoxActiveTop.contains(true)) {
                binding.editAddTextTop.visibility = View.VISIBLE
                binding.editAddTextTop.setText(binding.extraTextTop1.text)
            } else {
                binding.editAddTextTop.visibility = View.GONE
                hideKeyBoard(app)
            }
        }

        binding.imgEditTop2.setOnClickListener {
            textBoxActiveTop[1] = !textBoxActiveTop[1]

            textBoxActiveTop[0] = false
            textBoxActiveTop[2] = false
            textBoxActiveTop[3] = false

            if (textBoxActiveTop.contains(true)) {
                binding.editAddTextTop.visibility = View.VISIBLE
                binding.editAddTextTop.setText(binding.extraTextTop2.text)
            } else {
                binding.editAddTextTop.visibility = View.GONE
                hideKeyBoard(app)
            }
        }

        binding.imgEditTop3.setOnClickListener {
            textBoxActiveTop[2] = !textBoxActiveTop[2]

            textBoxActiveTop[0] = false
            textBoxActiveTop[1] = false
            textBoxActiveTop[3] = false

            if (textBoxActiveTop.contains(true)) {
                binding.editAddTextTop.visibility = View.VISIBLE
                binding.editAddTextTop.setText(binding.extraTextTop3.text)
            } else {
                binding.editAddTextTop.visibility = View.GONE
                hideKeyBoard(app)
            }
        }

        binding.imgEditTop4.setOnClickListener {
            textBoxActiveTop[3] = !textBoxActiveTop[3]

            textBoxActiveTop[0] = false
            textBoxActiveTop[1] = false
            textBoxActiveTop[2] = false

            if (textBoxActiveTop.contains(true)) {
                binding.editAddTextTop.visibility = View.VISIBLE
                binding.editAddTextTop.setText(binding.extraTextTop4.text)
            } else {
                binding.editAddTextTop.visibility = View.GONE
                hideKeyBoard(app)
            }
        }

        binding.imgEditBottom1.setOnClickListener {
            textBoxActiveBottom[0] = !textBoxActiveBottom[0]
            for (item in 1..3) {
                textBoxActiveBottom[item] = false
            }
            if (textBoxActiveBottom.contains(true)) {
                binding.editAddTextBottom.visibility = View.VISIBLE
                binding.editAddTextBottom.setText(binding.extraTextBottom1.text)
            } else {
                binding.editAddTextBottom.visibility = View.GONE
                hideKeyBoard(app)
            }
        }

        binding.imgEditBottom2.setOnClickListener {
            textBoxActiveBottom[1] = !textBoxActiveBottom[1]

            textBoxActiveBottom[0] = false
            textBoxActiveBottom[2] = false
            textBoxActiveBottom[3] = false

            if (textBoxActiveBottom.contains(true)) {
                binding.editAddTextBottom.visibility = View.VISIBLE
                binding.editAddTextBottom.setText(binding.extraTextBottom2.text)
            } else {
                binding.editAddTextBottom.visibility = View.GONE
                hideKeyBoard(app)
            }
        }

        binding.imgEditBottom3.setOnClickListener {
            textBoxActiveBottom[2] = !textBoxActiveBottom[2]

            textBoxActiveBottom[0] = false
            textBoxActiveBottom[1] = false
            textBoxActiveBottom[3] = false

            if (textBoxActiveBottom.contains(true)) {
                binding.editAddTextBottom.visibility = View.VISIBLE
                binding.editAddTextBottom.setText(binding.extraTextBottom3.text)
            } else {
                binding.editAddTextBottom.visibility = View.GONE
                hideKeyBoard(app)
            }
        }

        binding.imgEditBottom4.setOnClickListener {
            textBoxActiveBottom[3] = !textBoxActiveBottom[3]

            textBoxActiveBottom[0] = false
            textBoxActiveBottom[1] = false
            textBoxActiveBottom[2] = false

            if (textBoxActiveBottom.contains(true)) {
                binding.editAddTextBottom.visibility = View.VISIBLE
                binding.editAddTextBottom.setText(binding.extraTextBottom4.text)
            } else {
                binding.editAddTextBottom.visibility = View.GONE
                hideKeyBoard(app)
            }
        }

        binding.editAddTextTop.addTextChangedListener(object :
            TextWatcher {
            @SuppressLint("DefaultLocale", "SetTextI18n")
            override fun afterTextChanged(s: Editable) {
                val inputText = s.toString()
                if (inputText.isNotEmpty()) {
                    when (textBoxActiveTop.indexOf(true)) {
                        0 -> binding.extraTextTop1.text = inputText
                        1 -> binding.extraTextTop2.text = inputText
                        2 -> binding.extraTextTop3.text = inputText
                        else -> binding.extraTextTop4.text = inputText
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        })

        binding.editAddTextBottom.addTextChangedListener(object :
            TextWatcher {
            @SuppressLint("DefaultLocale", "SetTextI18n")
            override fun afterTextChanged(s: Editable) {
                val inputText = s.toString()
                if (inputText.isNotEmpty()) {
                    when (textBoxActiveBottom.indexOf(true)) {
                        0 -> binding.extraTextBottom1.text = inputText
                        1 -> binding.extraTextBottom2.text = inputText
                        2 -> binding.extraTextBottom3.text = inputText
                        else -> binding.extraTextBottom4.text = inputText
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        })

        binding.clearTopPicture.setOnClickListener {
            fillMask.clear()
            fillIndex = -1

            binding.topOculus.fillMask = fillMask
            binding.topOculus.invalidate()
        }

        binding.dateCaption.setOnClickListener {
            changeDate()
        }

        binding.undoTop.setOnClickListener {
            if (fillMask.isNotEmpty()) {
                fillMask.removeLast()
                fillIndex--

                binding.topOculus.fillMask = fillMask
                binding.topOculus.invalidate()
            }
        }

        binding.clearBottomPicture.setOnClickListener {
            fillMaskBottom.clear()
            fillIndexBottom = -1

            binding.bottomOculus.fillMask = fillMaskBottom
            binding.bottomOculus.invalidate()

            //    Log.d(TAG, "size = ${fillMask.size}")
        }

        binding.undoBottom.setOnClickListener {
            if (fillMaskBottom.isNotEmpty()) {
                fillMaskBottom.removeLast()
                fillIndexBottom--

                binding.bottomOculus.fillMask = fillMaskBottom
                binding.bottomOculus.invalidate()
            }
        }
        binding.oculoTopOnOff.setOnClickListener {
            topOculusVisible = !topOculusVisible

            if (topOculusVisible) {
                binding.layoutTop.visibility = View.VISIBLE
                binding.oculoTopOnOff.setImageResource(R.drawable.ic_r_green)

            } else {
                binding.layoutTop.visibility = View.GONE
                binding.oculoTopOnOff.setImageResource(R.drawable.ic_r_grey)
            }
        }

        binding.oculoBottomOnOff.setOnClickListener {
            bottomOculusVisible = !bottomOculusVisible

            if (bottomOculusVisible) {
                binding.layoutBottom.visibility = View.VISIBLE
                binding.oculoBottomOnOff.setImageResource(R.drawable.ic_l_green)
            } else {
                binding.layoutBottom.visibility = View.GONE
                binding.oculoBottomOnOff.setImageResource(R.drawable.ic_l_gray)
            }
        }

        binding.upperLayoutOnOff.setOnClickListener {
            upperLayoutVisible = !upperLayoutVisible

            if (upperLayoutVisible) {
                binding.layoutUpper.visibility = View.VISIBLE
                binding.upperLayoutOnOff.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.greenCircle
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.layoutUpper.visibility = View.GONE
                binding.upperLayoutOnOff.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.greyTint
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }

        patientViewModel.patientForm.observe(viewLifecycleOwner) { patientForm ->
            patientForm?.let {
                patientID = it.patientID
                currentForm = it
                //  === setup listener to this specific child and update the form if fields are changed ===
                patientViewModel.createRecordListener(currentForm.recordID)
                fillTheForm(it)

                patientViewModel.getAllFormsForPatient(patientID)
                updatePhotoView(photoFile)
            }

            binding.showHideButton.setOnClickListener {
                showPhoto = !showPhoto
                if (showPhoto) {
                    binding.autorefPhoto.visibility = View.VISIBLE
                    binding.showHideButton.setImageResource(R.drawable.visibility_32)
                } else {
                    binding.autorefPhoto.visibility = View.INVISIBLE
                    binding.showHideButton.setImageResource(R.drawable.visibility_off_32)
                }
            }
        }

        patientViewModel.photoFromFBReady.observe(viewLifecycleOwner) { ready ->
            ready?.let {
                Log.d(TAG, "Photo file is $it")
                if (it) uploadPhotoFileToImage()
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
                    if (patientForm == "ORTHOK") {
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

                val hPos = newSectionName.indexOf("ORTHOK")
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

        patientViewModel.navTrigger.observe(viewLifecycleOwner) { navOption ->
            navOption?.let {
                launchNavigator(navOption)
            }
        }

        binding.topOculus.setOnTouchListener { v, m ->
            if (m.action == MotionEvent.ACTION_DOWN) {
                // add starting point and increase index
                val newMList = mutableListOf<PointF>()
                fillIndex++
                // zero element = color
                newMList.add(PointF(selectedColor.toFloat(), selectedColor.toFloat()))

                //          Log.d(TAG, "selected color = ${selectedColor}")
                fillMask.add(newMList)
                fillMask[fillIndex].add(PointF(m.x + startPTRightTop.x, m.y + startPTRightTop.y))

                //    Log.d(TAG, "fillMask = ${fillMask}")

                binding.topOculus.fillMask = fillMask
                binding.topOculus.invalidate()

                startPTRightTop = PointF(v.x, v.y)
            }
            if (m.action == MotionEvent.ACTION_MOVE) {

                // print mask
                if (fillIndex > 0) {
                    if (!(fillMask[fillIndex].last().x == m.x + startPTRightTop.x && fillMask[fillIndex].last().y == m.y + startPTRightTop.y))
                        fillMask[fillIndex].add(
                            PointF(
                                m.x + startPTRightTop.x,
                                m.y + startPTRightTop.y
                            )
                        )
                }

                binding.topOculus.fillMask = fillMask
                binding.topOculus.invalidate()

            }
            true
        }

        binding.bottomOculus.setOnTouchListener { v, m ->
            if (m.action == MotionEvent.ACTION_DOWN) {
                // add starting point and increase index
                val newMList = mutableListOf<PointF>()
                fillIndexBottom++
                newMList.add(PointF(selectedColor.toFloat(), selectedColor.toFloat()))
                fillMaskBottom.add(newMList)
                fillMaskBottom[fillIndexBottom].add(
                    PointF(
                        m.x + startPTRightBottom.x,
                        m.y + startPTRightBottom.y
                    )
                )

                binding.bottomOculus.fillMask = fillMaskBottom
                binding.bottomOculus.invalidate()

                startPTRightBottom = PointF(v.x, v.y)
            }
            if (m.action == MotionEvent.ACTION_MOVE) {

                if (fillIndexBottom > 0) {
                    if (!(fillMaskBottom[fillIndexBottom].last().x == m.x + startPTRightBottom.x && fillMaskBottom[fillIndexBottom].last().y == m.y + startPTRightBottom.y))
                        fillMaskBottom[fillIndexBottom].add(
                            PointF(
                                m.x + startPTRightBottom.x,
                                m.y + startPTRightBottom.y
                            )
                        )
                }
                binding.bottomOculus.fillMask = fillMaskBottom
                binding.bottomOculus.invalidate()

            }
            true
        }

        // DELETE FORM FUNCTIONALITY

        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { ifDeleted ->
            ifDeleted?.let {
                if (ifDeleted) navController.navigate(
                    OrthokFragmentDirections.actionOrthokFragmentToFormSelectionFragment(patientID)
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
        // CHANGE DATA in THE FORM if record in FIREBASE was changed.
        patientViewModel.patientFireForm.observe(viewLifecycleOwner) { patientNewRecord ->
            patientNewRecord?.let {
                Log.d(TAG, "Reload Orthok Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(TAG, "Orthok Record from FB loaded")
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

        // Capture photo
        binding.photoButton.setOnClickListener {
            photoUri = FileProvider.getUriForFile(
                requireActivity(), "com.lizpostudio.kgoptometrycrm.fileprovider", photoFile
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
                        captureImage, PackageManager.MATCH_DEFAULT_ONLY
                    )
                for (cameraActivity in cameraActivities) {

                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName, photoUri,
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
                        binding.autorefPhoto.setImageDrawable(null)
//                        binding.refPhoto.setImageDrawable(null)
                    }
                }
            } else {
                Toast.makeText(
                    app.applicationContext, "Nothing to delete!", Toast.LENGTH_SHORT
                ).show()
            }
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

    private var photoUri: Uri? = null

    private var photoFile: File = File("com.lizpostudio.kgoptometrycrm")

    private val REQUEST_PHOTO = 2

    private lateinit var storageRef: StorageReference

    private val PHOTO_W = 330
    private val PHOTO_H = 528

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

    private var downloadPhotoTask: StorageTask<FileDownloadTask.TaskSnapshot>? = null

    private var showPhoto = true

    private fun updatePhotoView(photoFile: File) {
        //   taskToGetFile
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

    private fun saveAndNavigate(navOption: String) {
        patientViewModel.removeRecordsChangesListener()
        if (viewOnlyMode) {
            launchNavigator(navOption)
        } else {
            if (formWasChanged()) {
                Log.d(TAG, "Orthok was CHANGED")
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(), currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                Log.d(TAG, "Orthok is the SAME")
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
                OrthokFragmentDirections.actionOrthokFragmentToFormSelectionFragment(
                    patientID
                )
            )
            "home" -> findNavController().navigate(
                OrthokFragmentDirections.actionToDatabaseSearchFragment()
            )
            else -> navigateToSelectedForm()
        }
    }


    private fun navigateToSelectedForm() {
        val navController = this.findNavController()
        val orderOfSections = listOf(*resources.getStringArray(R.array.forms_order))


        when (navigateFormName) {

            orderOfSections[0] -> navController.navigate(
                OrthokFragmentDirections.actionOrthokFragmentToInfoFragment(navigateFormRecordID)
            )

            orderOfSections[1] -> navController.navigate(
                OrthokFragmentDirections.actionOrthokFragmentToMemoFragment(navigateFormRecordID)
            )

            orderOfSections[2] -> navController.navigate(
                OrthokFragmentDirections.actionOrthokFragmentToCurrentRxFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[3] -> navController.navigate(
                OrthokFragmentDirections.actionOrthokFragmentToRefractionFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[4] -> navController.navigate(
                OrthokFragmentDirections.actionOrthokFragmentToOcularHealthFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[5] -> navController.navigate(
                OrthokFragmentDirections.actionOrthokFragmentToSupplementaryFragment(
                    navigateFormRecordID
                )
            )
            orderOfSections[6] -> navController.navigate(
                OrthokFragmentDirections.actionOrthokFragmentToContactLensFragment(
                    navigateFormRecordID
                )
            )

            orderOfSections[7] -> {
                if (recordID != navigateFormRecordID) {

                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }

            orderOfSections[8] -> {
                navController.navigate(
                    OrthokFragmentDirections
                        .actionOrthokFragmentToCashOrderFragment(navigateFormRecordID)
                )
            }

            orderOfSections[9] -> navController.navigate(
                OrthokFragmentDirections.actionOrthokFragmentToFinalPrescriptionFragment(
                    navigateFormRecordID
                )
            )

            else -> Toast.makeText(
                this.activity?.applicationContext,
                getString(R.string.navigation_else),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun hideColors() {
        binding.apply {
            imgColorGreen.visibility = View.GONE
            imgColorRed.visibility = View.GONE
            imgColorYellow.visibility = View.GONE
        }
    }

    private fun hideKeyBoard(app: Application) {
        val imm =
            (app.applicationContext).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.mainLayout.windowToken, 0)
    }


    @SuppressLint("SetTextI18n")
    private fun fillTheForm(patientForm: PatientsEntity) {

        val extractData = patientForm.sectionData.split('|').toMutableList()
        if (extractData.size < 44) {
            for (index in extractData.size..44) {
                extractData.add("")
            }
        }

        val graphicsTop = patientForm.graphicsRight.split('|')
        val graphicsBottom = patientForm.graphicsLeft.split('|')
        val screenPxDST = Resources.getSystem().displayMetrics.density
        val widthHeightString =
            if (graphicsTop.isNotEmpty()) graphicsTop[0].split(',') else emptyList()

        val wH = if (widthHeightString.lastIndex == 1) {
            Pair(
                widthHeightString[0].toFloatOrNull() ?: 0f,
                widthHeightString[1].toFloatOrNull() ?: 0f
            )
        } else Pair(0f, 0f)

        val topOculusWidth = 0.75f * screenWidthPx().toFloat()
        val widthRatio =
            if (wH.first != 0f && topOculusWidth != 0f) topOculusWidth / wH.first else 1f

        val topOculusHeight = resources.getDimension(R.dimen.orthok_graphic_height) * screenPxDST
        val heightRatio =
            if (wH.second != 0f && topOculusHeight != 0f) topOculusHeight / wH.second else 1f

/*              Log.d(TAG, "saved w = ${wH.first} oculus width = ${topOculusWidth} widthRatio = $widthRatio")
        Log.d(TAG, "saved h = ${wH.second} oculus height = ${topOculusHeight} heightRatio = $heightRatio")*/

        fillMask = convertStringToFillMask(graphicsTop, widthRatio, heightRatio)
        fillIndex = if (fillMask.size > 0) fillMask.lastIndex else -1
        fillMaskBottom = convertStringToFillMask(graphicsBottom, widthRatio, heightRatio)
        fillIndexBottom = if (fillMaskBottom.size > 0) fillMaskBottom.lastIndex else -1

        binding.apply {

            bottomOculus.fillMask = fillMaskBottom
            bottomOculus.invalidate()

            topOculus.fillMask = fillMask
            topOculus.invalidate()

            //       patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(patientForm.dateOfSection)
            sectionEditDate = patientForm.dateOfSection

            editRightVaTop.setText(extractData[1])
            editLeftVaTop.setText(extractData[2])
            editOuVaTop.setText(extractData[3])

            var isEmpty = true

            for (i in 0 until spinnerRightSph.adapter.count) {
                if (extractData[4].trim() != "" &&
                    extractData[4] == spinnerRightSph.adapter.getItem(i).toString()
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

            for (i in 0 until spinnerLeftSph.adapter.count) {
                if (extractData[5].trim() != "" &&
                    extractData[5] == spinnerLeftSph.adapter.getItem(i).toString()
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

            for (i in 0 until spinnerRightCyl.adapter.count) {
                if (extractData[6].trim() != "" &&
                    extractData[6].trim().toDoubleOrNull() == spinnerRightCyl.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerRightCyl.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerRightCyl.setSelection(0)

            isEmpty = true

            for (i in 0 until spinnerLeftCyl.adapter.count) {
                if (extractData[7].trim() != "" &&
                    extractData[7].trim().toDoubleOrNull() == spinnerLeftCyl.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerLeftCyl.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerLeftCyl.setSelection(0)

            editRightAxis.setText(extractData[8])
            editLeftAxis.setText(extractData[9])
            editRightVa.setText(extractData[10])
            editLeftVa.setText(extractData[11])
            editRightOuVa.setText(extractData[12])

            extraTextTop1.text = extractData[14]
            extraTextTop2.text = extractData[15]
            extraTextTop3.text = extractData[16]
            extraTextTop4.text = extractData[17]

            editRxRight.setText(extractData[18])
            editBcRight.setText(extractData[19])
            editDiaRight.setText(extractData[20])

            editTreatmentZoneRight.setText(extractData[21])
            editCentrationRight.setText(extractData[22])

            extraTextBottom1.text = extractData[27]
            extraTextBottom2.text = extractData[28]
            extraTextBottom3.text = extractData[29]
            extraTextBottom4.text = extractData[30]

            // using field 33 = Right Lens and 34 = Left Lens

            editLensRight.setText(extractData[33])
            editLensLeft.setText(extractData[34])

            editRxLeft.setText(extractData[35])
            editBcLeft.setText(extractData[36])
            editDiaLeft.setText(extractData[37])
            editTreatmentZoneLeft.setText(extractData[38])
            editCentrationLeft.setText(extractData[39])
            editManagement.setText(extractData[40])
            editThRight.setText(extractData[41])
            editThLeft.setText(extractData[42])

            remarkInput.setText(patientForm.remarks)


            if (editLeftVaTop.text.toString() == "") editLeftVaTop.setText(vaDefault)
            if (editRightVaTop.text.toString() == "") editRightVaTop.setText(vaDefault)
            if (editOuVaTop.text.toString() == "") editOuVaTop.setText(vaDefault)

            if (editRightOuVa.text.toString() == "") editRightOuVa.setText(vaDefault)
            if (editLeftVa.text.toString() == "") editLeftVa.setText(vaDefault)
            if (editRightVa.text.toString() == "") editRightVa.setText(vaDefault)

            /*    if (editLeftVa2.text.toString() == "") editLeftVa2.setText(vaDefault)
                if (editRightVa2.text.toString() == "") editRightVa2.setText(vaDefault)*/

//            val dataPractitioner = patientForm.practitioner.split("|")
//
//            val adapterPractitioner =
//                ArrayAdapter(requireContext(), R.layout.spinner_list_basic_, dataPractitioner)
//            practitionerName.adapter = adapterPractitioner

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
        val screenPxDST = Resources.getSystem().displayMetrics.density
        val width = (0.75 * screenWidthPx()).toInt()
        val height = (resources.getDimension(R.dimen.orthok_graphic_height) * screenPxDST).toInt()

        val graphicsTop = convertFillMask(fillMask, width, height)
        val graphicsBottom = convertFillMask(fillMaskBottom, width, height)

        binding.apply {

            currentForm.remarks = remarkInput.text.toString().uppercase()
            currentForm.graphicsRight = graphicsTop
            currentForm.graphicsLeft = graphicsBottom
            if (sectionEditDate != -1L) currentForm.dateOfSection = sectionEditDate

            //      Log.d(TAG, "on save: ${convertLongToDDMMYY(patientForm.dateOfSection)}")

            val extractData = "reserved |" +
                    editRightVaTop.text.toString() + "|" +
                    editLeftVaTop.text.toString() + "|" +
                    editOuVaTop.text.toString() + "|" +
                    spinnerRightSph.selectedItem.toString() + "|" +
                    spinnerLeftSph.selectedItem.toString() + "|" +
                    spinnerRightCyl.selectedItem.toString() + "|" +
                    spinnerLeftCyl.selectedItem.toString() + "|" +
                    editRightAxis.text.toString() + "|" +
                    editLeftAxis.text.toString() + "|" +
                    editRightVa.text.toString() + "|" +
                    editLeftVa.text.toString() + "|" +
                    editRightOuVa.text.toString() + "|" + "|" + // 13 number is missing :)
                    extraTextTop1.text.toString() + "|" +
                    extraTextTop2.text.toString() + "|" +
                    extraTextTop3.text.toString() + "|" +
                    extraTextTop4.text.toString() + "|" +
                    editRxRight.text.toString() + "|" +
                    editBcRight.text.toString() + "|" +
                    editDiaRight.text.toString() + "|" +
                    editTreatmentZoneRight.text.toString() + "|" +
                    editCentrationRight.text.toString() + "|" +
                    "|" +
                    "|" +
                    "|" +
                    "|" +
                    extraTextBottom1.text.toString() + "|" +
                    extraTextBottom2.text.toString() + "|" +
                    extraTextBottom3.text.toString() + "|" +
                    extraTextBottom4.text.toString() + "|" +
                    "|" +
                    "|" +
                    editLensRight.text.toString() + "|" +
                    editLensLeft.text.toString() + "|" +
                    editRxLeft.text.toString() + "|" +
                    editBcLeft.text.toString() + "|" +
                    editDiaLeft.text.toString() + "|" +
                    editTreatmentZoneLeft.text.toString() + "|" +
                    editCentrationLeft.text.toString() + "|" +
                    editManagement.text.toString() + "|" +
                    editThRight.text.toString() + "|" +
                    editThLeft.text.toString()

            currentForm.sectionData = extractData.uppercase()
            currentForm.practitioner = (binding.practitionerName.selectedItem as String).uppercase()
        }

        if (takePhoto) {
            takePhoto = false
            binding.rotatePhoto.visibility = View.GONE
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
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.clear()
    }
}
