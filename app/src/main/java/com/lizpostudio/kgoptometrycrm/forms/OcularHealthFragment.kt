package com.lizpostudio.kgoptometrycrm.forms

import android.annotation.SuppressLint
import android.app.Application
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Resources
import android.graphics.PointF
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
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.lizpostudio.kgoptometrycrm.PatientsViewModel
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.ViewModelProviderFactory
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.FragmentOcularHealthBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding

class OcularHealthFragment : Fragment() {

    private val patientViewModel: PatientsViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }

    private var isAdmin = false

    private var fillMask = mutableListOf<MutableList<PointF>>()
    private var fillMaskBottom = mutableListOf<MutableList<PointF>>()
    private var fillIndex = -1
    private var fillIndexBottom = -1

    private var viewOnlyMode = false

    private val bindingRoot by viewBinding<FragmentOcularHealthBinding>()

    private val binding by lazy { bindingRoot.content }

    private var recordID = 0L
    private var patientID = ""

    private var topOculusVisible = true
    private var bottomOculusVisible = true

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

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        var startPTRightTop = PointF()
        var startPTRightBottom = PointF()

        val textBoxActiveTop = mutableListOf(false, false, false, false)
        val textBoxActiveBottom = mutableListOf(false, false, false, false)

        val app = requireNotNull(this.activity).application

        var selectedColor = ContextCompat.getColor(requireContext(), R.color.redCircle)
        // change BINDING to Respective forms args!
        val safeArgs: OcularHealthFragmentArgs by navArgs()
        recordID = safeArgs.recordID

        // get Patient data
        patientViewModel.getPatientForm(recordID)

        val navController = this.findNavController()

        val sharedPref = app.getSharedPreferences(
            Constants.PREF_NAME,
            Context.MODE_PRIVATE
        )
        isAdmin = (sharedPref?.getString("admin", "") ?: "") == "admin"

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

            //    Log.d(Constants.TAG, "size = ${fillMask.size}")
        }

        binding.dateCaption.setOnClickListener {
            changeDate()
        }

        binding.undoTop.setOnClickListener {
            if (fillMask.isNotEmpty()) {
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
                binding.apply {
                    frameTopOculus.visibility = View.VISIBLE
                    clearTopPicture.visibility = View.VISIBLE
                    undoTop.visibility = View.VISIBLE
                    oculoTopOnOff.setImageResource(R.drawable.ic_oculo_icon)
                    imgEditTop1.visibility = View.VISIBLE
                    imgEditTop2.visibility = View.VISIBLE
                    imgEditTop3.visibility = View.VISIBLE
                    imgEditTop4.visibility = View.VISIBLE
                    extraTextTop1.visibility = View.VISIBLE
                    extraTextTop2.visibility = View.VISIBLE
                    extraTextTop3.visibility = View.VISIBLE
                    extraTextTop4.visibility = View.VISIBLE
                }
            } else {
                binding.apply {
                    frameTopOculus.visibility = View.GONE
                    clearTopPicture.visibility = View.GONE
                    undoTop.visibility = View.GONE
                    oculoTopOnOff.setImageResource(R.drawable.ic_oculo_icon_no)
                    imgEditTop1.visibility = View.GONE
                    imgEditTop2.visibility = View.GONE
                    imgEditTop3.visibility = View.GONE
                    imgEditTop4.visibility = View.GONE
                    extraTextTop1.visibility = View.GONE
                    extraTextTop2.visibility = View.GONE
                    extraTextTop3.visibility = View.GONE
                    extraTextTop4.visibility = View.GONE
                    editAddTextTop.visibility = View.GONE
                }
            }
        }

        binding.apply {
            frameBottomOculus.visibility = View.GONE
            clearBottomPicture.visibility = View.GONE
            undoBottom.visibility = View.GONE
            oculoBottomOnOff.setImageResource(R.drawable.ic_oculo_triple_icon_no)
            imgEditBottom1.visibility = View.GONE
            imgEditBottom2.visibility = View.GONE
            imgEditBottom3.visibility = View.GONE
            imgEditBottom4.visibility = View.GONE
            extraTextBottom1.visibility = View.GONE
            extraTextBottom2.visibility = View.GONE
            extraTextBottom3.visibility = View.GONE
            extraTextBottom4.visibility = View.GONE
            editAddTextBottom.visibility = View.GONE
        }

        binding.oculoBottomOnOff.setOnClickListener {
            if (bottomOculusVisible) {
                binding.apply {
                    frameBottomOculus.visibility = View.VISIBLE
                    clearBottomPicture.visibility = View.VISIBLE
                    undoBottom.visibility = View.VISIBLE
                    oculoBottomOnOff.setImageResource(R.drawable.ic_oculo_triple_icon)
                    imgEditBottom1.visibility = View.VISIBLE
                    imgEditBottom2.visibility = View.VISIBLE
                    imgEditBottom3.visibility = View.VISIBLE
                    imgEditBottom4.visibility = View.VISIBLE
                    extraTextBottom1.visibility = View.VISIBLE
                    extraTextBottom2.visibility = View.VISIBLE
                    extraTextBottom3.visibility = View.VISIBLE
                    extraTextBottom4.visibility = View.VISIBLE
                }
            } else {
                binding.apply {
                    frameBottomOculus.visibility = View.GONE
                    clearBottomPicture.visibility = View.GONE
                    undoBottom.visibility = View.GONE
                    oculoBottomOnOff.setImageResource(R.drawable.ic_oculo_triple_icon_no)
                    imgEditBottom1.visibility = View.GONE
                    imgEditBottom2.visibility = View.GONE
                    imgEditBottom3.visibility = View.GONE
                    imgEditBottom4.visibility = View.GONE
                    extraTextBottom1.visibility = View.GONE
                    extraTextBottom2.visibility = View.GONE
                    extraTextBottom3.visibility = View.GONE
                    extraTextBottom4.visibility = View.GONE
                    editAddTextBottom.visibility = View.GONE
                }
            }
            bottomOculusVisible = !bottomOculusVisible
        }

        patientViewModel.patientForm.observe(viewLifecycleOwner) { patientForm ->
            patientForm?.let {
                patientID = it.patientID
                currentForm.copyFrom(it)
                //  === setup listener to this specific child and update the form if fields are changed ===
                patientViewModel.createRecordListener(currentForm.recordID)
                fillTheForm(it)

                patientViewModel.getAllFormsForPatient(patientID)
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
                    if (patientForm == "OCULAR HEALTH") {
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

                val hPos = newSectionName.indexOf("OCULAR HEALTH")
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

            binding.frameTopOculus.layoutParams.height = screenWidthPx() / 2
            binding.frameBottomOculus.layoutParams.height = screenWidthPx() / 2

//            binding.frameTopOculus.visibility = View.VISIBLE
//            binding.frameBottomOculus.visibility = View.VISIBLE
            //     Log.d(Constants.TAG, "${screenWidthPx() / 2}")
        }


        val iopListItems = iopList()
        val iopSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                iopListItems
            )
        binding.spinnerIopLeft.adapter = iopSpinnerAdapter
        binding.spinnerIopRight.adapter = iopSpinnerAdapter

        val cdRatioListItems = cdRatioList()
        val cdRatioSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                cdRatioListItems
            )
        binding.spinnerCdRatioLeft.adapter = cdRatioSpinnerAdapter
        binding.spinnerCdRatioRight.adapter = cdRatioSpinnerAdapter

        val tBUTListItems = tBUTList()
        val tBUTSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                tBUTListItems
            )
        //  addSpinnerAdapter.setDropDownViewResource(R.layout.spinner_list_small_numbers)
        binding.spinnerTbutLeft.adapter = tBUTSpinnerAdapter
        binding.spinnerTbutRight.adapter = tBUTSpinnerAdapter

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
                    OcularHealthFragmentDirections.actionToFormSelectionFragment(patientID)
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

        patientViewModel.patientFireForm.observe(viewLifecycleOwner) { patientNewRecord ->
            patientNewRecord?.let {
                Log.d(Constants.TAG, "Reload OH Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(Constants.TAG, "OH Record from FB loaded")
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
        return bindingRoot.root
    }

    private fun saveAndNavigate(navOption: String) {
        patientViewModel.removeRecordsChangesListener()
        if (viewOnlyMode) {
            launchNavigator(navOption)
        } else {
            if (formWasChanged()) {
                Log.d(Constants.TAG, "OH was changed")
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(),
                    currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                Log.d(Constants.TAG, "OH was not changed")
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
                OcularHealthFragmentDirections.actionToFormSelectionFragment(patientID)
            )

            "home" -> findNavController().navigate(
                OcularHealthFragmentDirections.actionToDatabaseSearchFragment()
            )

            else -> navigateToSelectedForm()
        }
    }

    private fun navigateToSelectedForm() {
        when (navigateFormName) {
            getString(R.string.info_form_caption) -> findNavController().navigate(
                OcularHealthFragmentDirections.actionToInfoFragment(navigateFormRecordID)
            )

            getString(R.string.follow_up_form_caption) -> findNavController().navigate(
                OcularHealthFragmentDirections.actionToFollowUpFragment(navigateFormRecordID)
            )

            getString(R.string.memo_form_caption) -> findNavController().navigate(
                OcularHealthFragmentDirections.actionToMemoFragment(navigateFormRecordID)
            )

            getString(R.string.current_rx_caption) -> findNavController().navigate(
                OcularHealthFragmentDirections.actionToCurrentRxFragment(navigateFormRecordID)
            )

            getString(R.string.refraction_caption) -> findNavController().navigate(
                OcularHealthFragmentDirections.actionToRefractionFragment(navigateFormRecordID)
            )

            getString(R.string.ocular_health_caption) -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }

            getString(R.string.supplementary_test_caption) -> findNavController().navigate(
                OcularHealthFragmentDirections.actionToSupplementaryFragment(navigateFormRecordID)
            )

            getString(R.string.contact_lens_exam_caption) -> findNavController().navigate(
                OcularHealthFragmentDirections.actionToContactLensFragment(navigateFormRecordID)
            )

            getString(R.string.orthox_caption) -> findNavController().navigate(
                OcularHealthFragmentDirections.actionToOrthokFragment(navigateFormRecordID)
            )

            getString(R.string.cash_order) -> findNavController().navigate(
                OcularHealthFragmentDirections.actionToCashOrderFragment(navigateFormRecordID)
            )

            getString(R.string.sales_order_caption) -> findNavController().navigate(
                OcularHealthFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )

            getString(R.string.final_prescription_caption) -> findNavController().navigate(
                OcularHealthFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
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
//      Log.d(Constants.TAG, "extract data size before = ${extractData.size}")
        if (extractData.size < 20) {
            for (index in extractData.size..20) {
                extractData.add("")
            }
        }

        val graphicsTop = p.graphicsRight.split('|')
        val graphicsBottom = p.graphicsLeft.split('|')

        val widthHeightString =
            if (graphicsTop.isNotEmpty()) graphicsTop[0].split(',') else emptyList()

        val wH = if (widthHeightString.lastIndex == 1) {
            Pair(
                widthHeightString[0].toFloatOrNull() ?: 0f,
                widthHeightString[1].toFloatOrNull() ?: 0f
            )
        } else Pair(0f, 0f)

        val topOculusWidth = screenWidthPx().toFloat()
        val widthRatio =
            if (wH.first != 0f && topOculusWidth != 0f) topOculusWidth / wH.first else 1f

        val topOculusHeight = topOculusWidth / 2
        val heightRatio =
            if (wH.second != 0f && topOculusHeight != 0f) topOculusHeight / wH.second else 1f


        fillMask = convertStringToFillMask(graphicsTop, widthRatio, heightRatio)
        fillIndex = if (fillMask.size > 0) fillMask.lastIndex else -1
        fillMaskBottom = convertStringToFillMask(graphicsBottom, widthRatio, heightRatio)
        fillIndexBottom = if (fillMaskBottom.size > 0) fillMaskBottom.lastIndex else -1

        binding.apply {
            editAxialLengthRight.setText(p.axialLengthRight)
            editAxialLengthLeft.setText(p.axialLengthLeft)
            bottomOculus.fillMask = fillMaskBottom
            bottomOculus.invalidate()

            topOculus.fillMask = fillMask
            topOculus.invalidate()

            //        patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(p.dateOfSection)
            sectionEditDate = p.dateOfSection
            //   Log.d(Constants.TAG, " Extracted data: ${convertLongToDDMMYY(patientForm.dateOfSection)}" )

            editLensRight.setText(extractData[0])
            editLensLeft.setText(extractData[1])

            var isEmpty = true
            if (extractData[2].trim() != "") {
                for (i in 0 until spinnerIopRight.adapter.count) {
                    if (extractData[2].trim() == spinnerIopRight.adapter.getItem(i).toString()) {
                        spinnerIopRight.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerIopRight.setSelection(0)

            isEmpty = true
            if (extractData[3].trim() != "") {
                for (i in 0 until spinnerIopLeft.adapter.count) {
                    if (extractData[3].trim() == spinnerIopLeft.adapter.getItem(i).toString()) {
                        spinnerIopLeft.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerIopLeft.setSelection(0)

            editAvRatioRight.setText(extractData[4])
            editAvRatioLeft.setText(extractData[5])

            isEmpty = true
            if (extractData[6].trim() != "") {
                for (i in 0 until spinnerCdRatioRight.adapter.count) {
                    if (extractData[6].trim() == spinnerCdRatioRight.adapter.getItem(i)
                            .toString()
                    ) {
                        spinnerCdRatioRight.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerCdRatioRight.setSelection(0)

            isEmpty = true
            if (extractData[7].trim() != "") {
                for (i in 0 until spinnerCdRatioLeft.adapter.count) {
                    if (extractData[7].trim() == spinnerCdRatioLeft.adapter.getItem(i).toString()) {
                        spinnerCdRatioLeft.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerCdRatioLeft.setSelection(0)


            isEmpty = true
            if (extractData[8].trim() != "") {
                for (i in 0 until spinnerTbutRight.adapter.count) {
                    if (extractData[8].trim() == spinnerTbutRight.adapter.getItem(i).toString()) {
                        spinnerTbutRight.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerTbutRight.setSelection(0)

            isEmpty = true
            if (extractData[9].trim() != "") {
                for (i in 0 until spinnerTbutLeft.adapter.count) {
                    if (extractData[9].trim() == spinnerTbutLeft.adapter.getItem(i).toString()) {
                        spinnerTbutLeft.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerTbutLeft.setSelection(0)
            extraTextTop1.text = extractData[10]
            extraTextTop2.text = extractData[11]
            extraTextTop3.text = extractData[12]
            extraTextTop4.text = extractData[13]

            extraTextBottom1.text = extractData[14]
            extraTextBottom2.text = extractData[15]
            extraTextBottom3.text = extractData[16]
            extraTextBottom4.text = extractData[17]

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

        val width = screenWidthPx()
        val height = width / 2

        //    Log.d(Constants.TAG, "Fill mask = $fillMaskBottom")

        val graphicsTop = convertFillMask(fillMask, width, height)
        val graphicsBottom = convertFillMask(fillMaskBottom, width, height)

        //     Log.d(Constants.TAG, "at assign GL = $graphicsBottom")

        binding.apply {

            currentForm.remarks = remarkInput.text.toString().uppercase()
            currentForm.graphicsRight = graphicsTop
            currentForm.graphicsLeft = graphicsBottom
            currentForm.axialLengthRight = editAxialLengthRight.text.toString()
            currentForm.axialLengthLeft = editAxialLengthLeft.text.toString()
            if (sectionEditDate != -1L)
                currentForm.dateOfSection = sectionEditDate
            val extractData = editLensRight.text.toString() + "|" +
                    editLensLeft.text.toString() + "|" +
                    spinnerIopRight.selectedItem.toString() + "|" +
                    spinnerIopLeft.selectedItem.toString() + "|" +
                    editAvRatioRight.text.toString() + "|" +
                    editAvRatioLeft.text.toString() + "|" +
                    spinnerCdRatioRight.selectedItem.toString() + "|" +
                    spinnerCdRatioLeft.selectedItem.toString() + "|" +
                    spinnerTbutRight.selectedItem.toString() + "|" +
                    spinnerTbutLeft.selectedItem.toString() + "|" +
                    extraTextTop1.text.toString() + "|" +
                    extraTextTop2.text.toString() + "|" +
                    extraTextTop3.text.toString() + "|" +
                    extraTextTop4.text.toString() + "|" +
                    extraTextBottom1.text.toString() + "|" +
                    extraTextBottom2.text.toString() + "|" +
                    extraTextBottom3.text.toString() + "|" +
                    extraTextBottom4.text.toString()
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
                }, todayYear, todayMonth, todayDay
            )
            datePickerDialog.show()
        }
    }
}
