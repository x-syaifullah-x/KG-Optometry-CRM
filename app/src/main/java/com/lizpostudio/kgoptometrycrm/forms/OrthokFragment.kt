package com.lizpostudio.kgoptometrycrm.forms

import android.annotation.SuppressLint
import android.app.Application
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
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
import com.lizpostudio.kgoptometrycrm.databinding.FragmentOrthokBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding
import java.io.ByteArrayOutputStream
import java.io.File

class OrthokFragment : Fragment() {

    companion object {
        private const val VA_DEFAULT = "6/"

        private const val PHOTO_W = 330
        private const val PHOTO_H = 528
    }

    private val patientViewModel: PatientsViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }
    private var isAdmin = false

    private var fillMask = mutableListOf<MutableList<PointF>>()
    private var fillMaskBottom = mutableListOf<MutableList<PointF>>()
    private var fillIndex = -1
    private var fillIndexBottom = -1

    private var viewOnlyMode = false

    private val bindingRoot by viewBinding<FragmentOrthokBinding>()

    private val binding by lazy { bindingRoot.content }

    private var recordID = 0L
    private var patientID = ""

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

        val app = requireNotNull(this.activity).application
        filesDir = app.applicationContext.filesDir

        var selectedColor = ContextCompat.getColor(requireContext(), R.color.greenCircle)
        // change BINDING to Respective forms args!

        val safeArgs: OrthokFragmentArgs by navArgs()
        recordID = safeArgs.recordID

        // get Patient data
        patientViewModel.getPatientForm(recordID)

        createRefAssignPhFile()

        val navController = this.findNavController()

        // get if user is Admin
        val sharedPref = app.getSharedPreferences(
            Constants.PREF_NAME,
            Context.MODE_PRIVATE
        )
        isAdmin = (sharedPref?.getString("admin", "") ?: "") == "admin"

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
//                fillMask.removeLast()
                fillMask.removeAt(fillMask.size - 1)
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

            //    Log.d(Constants.TAG, "size = ${fillMask.size}")
        }

        binding.undoBottom.setOnClickListener {
            if (fillMaskBottom.isNotEmpty()) {
//                fillMaskBottom.removeLast()
                fillMaskBottom.removeAt(fillMaskBottom.size - 1)
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

        patientViewModel.patientForm.observe(viewLifecycleOwner) { p ->
            patientID = p.patientID
            currentForm = p

            binding.undoButton.setOnClickListener { fillTheForm(currentForm) }

            patientViewModel.createRecordListener(currentForm.recordID)
            fillTheForm(p)

            patientViewModel.getAllFormsForPatient(patientID)

            binding.showHideButton.setOnClickListener {
                showPhoto = !showPhoto
                if (showPhoto) {
                    binding.autorefPhoto.visibility = View.VISIBLE
                    binding.showHideButton.setImageResource(R.drawable.visibility_36)
                } else {
                    binding.autorefPhoto.visibility = View.INVISIBLE
                    binding.showHideButton.setImageResource(R.drawable.visibility_off_32)
                }
            }
        }

        patientViewModel.photoFromFBReady.observe(viewLifecycleOwner) { ready ->
            ready?.let {
                Log.d(Constants.TAG, "Photo file is $it")
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

                val hPos = newSectionName.indexOf("ORTHOK")
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

                //          Log.d(Constants.TAG, "selected color = ${selectedColor}")
                fillMask.add(newMList)
                fillMask[fillIndex].add(PointF(m.x + startPTRightTop.x, m.y + startPTRightTop.y))

                //    Log.d(Constants.TAG, "fillMask = ${fillMask}")

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
                    OrthokFragmentDirections.actionToFormSelectionFragment(patientID)
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
        // CHANGE DATA in THE FORM if record in FIREBASE was changed.
        patientViewModel.patientFireForm.observe(viewLifecycleOwner) { patientNewRecord ->
            patientNewRecord?.let {
                Log.d(Constants.TAG, "Reload Orthok Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(Constants.TAG, "Orthok Record from FB loaded")
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

        // Capture photo
        binding.photoButton.setOnClickListener {
            photoUri = FileProvider
                .getUriForFile(requireActivity(), Constants.FILE_PROVIDER_AUTHORITY, photoFile)
            takePicture.launch(photoUri)
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

        binding.rotatePhoto.setOnClickListener {
            val bitmap =
                BitmapUtils.rotate((binding.autorefPhoto.drawable as BitmapDrawable).bitmap, 90F)

            binding.autorefPhoto.setImageBitmap(bitmap)
        }

        return bindingRoot.root
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

    private lateinit var storageRef: StorageReference

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

    private var downloadPhotoTask: StorageTask<FileDownloadTask.TaskSnapshot>? = null

    private var showPhoto = true

    private fun updatePhotoView(photoFile: File) {
        binding.autorefPhoto.setImageDrawable(null)

        //   taskToGetFile
        if (currentForm.reservedField.isNotBlank() && currentForm.reservedField != "deleted") {
            downloadPhotoTask = storageRef.getFile(photoFile).addOnSuccessListener {
                Log.d(Constants.TAG, "FB photo downloaded to local file")
                patientViewModel.readyToShowPhoto()
                currentForm.reservedField = photoFile.toString()
            }.addOnFailureListener {
                // delete local file
                Log.d(Constants.TAG, "No such file exist or error downloading. Delete local file")
                if (photoFile.exists()) photoFile.delete()
                patientViewModel.readyToShowPhoto()
                currentForm.reservedField = ""
                binding.autorefPhoto.setImageDrawable(null)
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
                Log.d(Constants.TAG, "Orthok was CHANGED")
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(), currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                Log.d(Constants.TAG, "Orthok is the SAME")
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
                OrthokFragmentDirections.actionToFormSelectionFragment(
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
        when (navigateFormName) {
            getString(R.string.info_form_caption) -> findNavController().navigate(
                OrthokFragmentDirections.actionToInfoFragment(navigateFormRecordID)
            )

            getString(R.string.follow_up_form_caption) -> findNavController().navigate(
                OrthokFragmentDirections.actionToFollowUpFragment(navigateFormRecordID)
            )

            getString(R.string.memo_form_caption) -> findNavController().navigate(
                OrthokFragmentDirections.actionToMemoFragment(navigateFormRecordID)
            )

            getString(R.string.current_rx_caption) -> findNavController().navigate(
                OrthokFragmentDirections.actionToCurrentRxFragment(navigateFormRecordID)
            )

            getString(R.string.refraction_caption) -> findNavController().navigate(
                OrthokFragmentDirections.actionToRefractionFragment(navigateFormRecordID)
            )

            getString(R.string.ocular_health_caption) -> findNavController().navigate(
                OrthokFragmentDirections.actionToOcularHealthFragment(navigateFormRecordID)
            )

            getString(R.string.supplementary_test_caption) -> findNavController().navigate(
                OrthokFragmentDirections.actionToSupplementaryFragment(navigateFormRecordID)
            )

            getString(R.string.contact_lens_exam_caption) -> findNavController().navigate(
                OrthokFragmentDirections.actionToContactLensFragment(navigateFormRecordID)
            )

            getString(R.string.orthox_caption) -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }

            getString(R.string.cash_order) -> findNavController().navigate(
                OrthokFragmentDirections.actionToCashOrderFragment(navigateFormRecordID)
            )

            getString(R.string.sales_order_caption) -> findNavController().navigate(
                OrthokFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )

            getString(R.string.final_prescription_caption) -> findNavController().navigate(
                OrthokFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )

            else -> {
                Toast.makeText(
                    context, "$navigateFormName not implemented yet", Toast.LENGTH_SHORT
                ).show()
            }
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
        if (extractData.size < 44) {
            for (index in extractData.size..44) {
                extractData.add("")
            }
        }

        val graphicsTop = p.graphicsRight.split('|')
        val graphicsBottom = p.graphicsLeft.split('|')
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

        /*              Log.d(Constants.TAG, "saved w = ${wH.first} oculus width = ${topOculusWidth} widthRatio = $widthRatio")
                Log.d(Constants.TAG, "saved h = ${wH.second} oculus height = ${topOculusHeight} heightRatio = $heightRatio")*/

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
            dateCaption.text = convertLongToDDMMYY(p.dateOfSection)
            sectionEditDate = p.dateOfSection

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

            remarkInput.setText(p.remarks)


            if (editLeftVaTop.text.toString() == "") editLeftVaTop.setText(VA_DEFAULT)
            if (editRightVaTop.text.toString() == "") editRightVaTop.setText(VA_DEFAULT)
            if (editOuVaTop.text.toString() == "") editOuVaTop.setText(VA_DEFAULT)

            if (editRightOuVa.text.toString() == "") editRightOuVa.setText(VA_DEFAULT)
            if (editLeftVa.text.toString() == "") editLeftVa.setText(VA_DEFAULT)
            if (editRightVa.text.toString() == "") editRightVa.setText(VA_DEFAULT)

            /*    if (editLeftVa2.text.toString() == "") editLeftVa2.setText(vaDefault)
                if (editRightVa2.text.toString() == "") editRightVa2.setText(vaDefault)*/

//            val dataPractitioner = patientForm.practitioner.split("|")
//
//            val adapterPractitioner =
//                ArrayAdapter(requireContext(), R.layout.spinner_list_basic_, dataPractitioner)
//            practitionerName.adapter = adapterPractitioner

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

            //      Log.d(Constants.TAG, "on save: ${convertLongToDDMMYY(patientForm.dateOfSection)}")

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
}
