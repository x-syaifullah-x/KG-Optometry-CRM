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
import com.lizpostudio.kgoptometrycrm.databinding.FragmentContactLensBinding
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding
import java.util.*

class ContactLensFragment : Fragment() {

    private val patientViewModel: PatientsViewModel by viewModels {
        ViewModelProviderFactory.getInstance(context)
    }

    private var isAdmin = false

    private val bindingRoot by viewBinding<FragmentContactLensBinding>()

    private val binding by lazy { bindingRoot.content }

    private var recordID = 0L
    private var patientID = ""
    private var sectionEditDate = -1L

    private var fillMaskTop = mutableListOf<MutableList<PointF>>()

    private var fillMaskRight = mutableListOf<MutableList<MutableList<PointF>>>(
        mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf()
    )
    private var fillMaskLeft = mutableListOf<MutableList<MutableList<PointF>>>(
        mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf()
    )

    private var fitIndex = 0
    private var fittingFormsAr = mutableListOf<MutableList<String>>()

    private var fillIndexTop = -1
    private var fillIndexRight = mutableListOf(-1, -1, -1, -1)
    private var fillIndexLeft = mutableListOf(-1, -1, -1, -1)

    private var currentForm = PatientEntity()
    private var navigateFormName = ""
    private var navigateFormRecordID = -1L
    private var viewOnlyMode = false

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

        var startPTTop = PointF()
        var startPTRight = PointF()
        var startPTLeft = PointF()

        var selectedColor = ContextCompat.getColor(requireContext(), R.color.greenCircle)
        var selectedColorFitting = ContextCompat.getColor(requireContext(), R.color.greenCircle)

        val textBoxActiveTop = mutableListOf(false, false, false, false)
        val textBoxActiveRight = mutableListOf(false, false)
        val textBoxActiveLeft = mutableListOf(false, false)

        val app = requireNotNull(this.activity).application

        val safeArgs: ContactLensFragmentArgs by navArgs()
        recordID = safeArgs.recordID

        // get Patient data
        patientViewModel.getPatientForm(recordID)

        // get if user is Admin
        val sharedPref = app.getSharedPreferences(
            Constants.PREF_NAME, Context.MODE_PRIVATE
        )
        isAdmin = (sharedPref?.getString("admin", "") ?: "") == "admin"

        binding.dateCaption.setOnClickListener {
            changeDate()
        }

        // =============== LAYOUTS ON / OFF ====================
        binding.patientHistorySwitch.setOnClickListener {
            hideLayouts()
            binding.layoutPatientHistory.visibility = View.VISIBLE
            binding.patientHistorySwitch.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greenCircle
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        binding.ocularHealthSwitch.setOnClickListener {
            hideLayouts()
            binding.layoutOcularHealth.visibility = View.VISIBLE
            binding.ocularHealthSwitch.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greenCircle
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        binding.fittingSwitch.setOnClickListener {
            hideLayouts()
            showFittingsButtons()
            binding.layoutFitting.visibility = View.VISIBLE
            binding.fittingSwitch.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greenCircle
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        binding.fitSwitch1.setOnClickListener {
            // save current fittings - increment fitindex - load new fittings
            saveAndLoadFitting(0)
            binding.fitSwitch1.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greenCircle
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        binding.fitSwitch2.setOnClickListener {
            saveAndLoadFitting(1)
            binding.fitSwitch2.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greenCircle
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        binding.fitSwitch3.setOnClickListener {
            saveAndLoadFitting(2)
            binding.fitSwitch3.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greenCircle
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        binding.fitSwitch4.setOnClickListener {
            saveAndLoadFitting(3)
            binding.fitSwitch4.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greenCircle
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        binding.finalSwitch.setOnClickListener {
            hideLayouts()
            binding.layoutFinalPrescriptionCl.visibility = View.VISIBLE
            binding.finalSwitch.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greenCircle
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        // ================== POPULATE SPINNERS =====================

        val wearingNumbers24 = createNumbersList(24)
        val wearing24SpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                wearingNumbers24
            )
        binding.spinnerWearing24.adapter = wearing24SpinnerAdapter

        val wearingNumbers7 = createNumbersList(7)
        val wearing7SpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                wearingNumbers7
            )
        binding.spinnerWearing7.adapter = wearing7SpinnerAdapter

        val wearingNumbers30 = createNumbersList(30)
        val wearing30SpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                wearingNumbers30
            )
        binding.spinnerWearing30.adapter = wearing30SpinnerAdapter

        val visionScale10 = createNumbersList(10)
        val vision10SpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                visionScale10
            )
        binding.spinnerVision.adapter = vision10SpinnerAdapter
        binding.spinnerComfort.adapter = vision10SpinnerAdapter

        val sphListItems = sphList()
        val sphSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                sphListItems
            )

        binding.spinnerLeftSph.adapter = sphSpinnerAdapter
        binding.spinnerRightSph.adapter = sphSpinnerAdapter
        binding.spinnerLeftSphFinal.adapter = sphSpinnerAdapter
        binding.spinnerRightSphFinal.adapter = sphSpinnerAdapter

        val cylListItems = cylList()
        val cylSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                cylListItems
            )

        binding.spinnerLeftCyl.adapter = cylSpinnerAdapter
        binding.spinnerRightCyl.adapter = cylSpinnerAdapter
        binding.spinnerLeftCylFinal.adapter = cylSpinnerAdapter
        binding.spinnerRightCylFinal.adapter = cylSpinnerAdapter

        val tBUTListItems = tBUTList()
        val tBUTSpinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                app.applicationContext,
                android.R.layout.simple_spinner_item,
                tBUTListItems
            )
        binding.spinnerTbutLeft.adapter = tBUTSpinnerAdapter
        binding.spinnerTbutRight.adapter = tBUTSpinnerAdapter

        // ==============  END OF SPINNERS

        // =================== DRAWING ===================

        // Drawing CROSS AND CIRCLE
        binding.apply {
            imageFittingRightCl.customDrawing = 1
            imageFittingLeftCl.customDrawing = 1

            imageFittingLeftCl.invalidate()
            imageFittingRightCl.invalidate()

        }

        // ==================== HANDLING COLORS =====================

        binding.imgColorSelected.setOnClickListener {
            binding.apply {
                imgColorGreen.visibility = View.VISIBLE
                imgColorRed.visibility = View.VISIBLE
                imgColorYellow.visibility = View.VISIBLE
            }
        }

        binding.imgColorSelectedFitting.setOnClickListener {
            binding.apply {
                imgColorGreenFitting.visibility = View.VISIBLE
                imgColorRedFitting.visibility = View.VISIBLE
                imgColorYellowFitting.visibility = View.VISIBLE
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
        // ======FITTING
        binding.imgColorRedFitting.setOnClickListener {
            selectedColorFitting = ContextCompat.getColor(requireContext(), R.color.redCircle)
            binding.imgColorSelectedFitting.setImageResource(R.drawable.red_circle)
            hideColorsFitting()
        }

        binding.imgColorYellowFitting.setOnClickListener {
            selectedColorFitting = ContextCompat.getColor(requireContext(), R.color.yellowCircle)
            binding.imgColorSelectedFitting.setImageResource(R.drawable.yellow_circle)
            hideColorsFitting()
        }
        binding.imgColorGreenFitting.setOnClickListener {
            selectedColorFitting = ContextCompat.getColor(requireContext(), R.color.greenCircle)
            binding.imgColorSelectedFitting.setImageResource(R.drawable.green_circle)
            hideColorsFitting()
        }

        // ================ TOP EDIT TEXT BOXES INSIDE DRAWINGS ===============

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

        // =============== loading data to top text boxes =================

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

        // ================= Clear and Undo Top Buttons ===============

        binding.clearTopPicture.setOnClickListener {
            fillMaskTop.clear()
            fillIndexTop = -1

            binding.oculoHealthCl.fillMask = fillMaskTop
            binding.oculoHealthCl.invalidate()

        }

        binding.undoTop.setOnClickListener {
            if (fillMaskTop.isNotEmpty()) {
//                fillMaskTop.removeLast()
                fillMaskTop.removeAt(fillMaskTop.lastIndex)
                fillIndexTop--

                binding.oculoHealthCl.fillMask = fillMaskTop
                binding.oculoHealthCl.invalidate()
            }
        }
        // ================= PAINTING ON OCULAR HEALTH =====================

        binding.oculoHealthCl.setOnTouchListener { v, m ->
            if (m.action == MotionEvent.ACTION_DOWN) {
                // add starting point and increase index
                val newMList = mutableListOf<PointF>()
                fillIndexTop++
                // zero element = color
                newMList.add(PointF(selectedColor.toFloat(), selectedColor.toFloat()))

                //          Log.d(Constants.TAG, "selected color = ${selectedColor}")
                fillMaskTop.add(newMList)
                fillMaskTop[fillIndexTop].add(PointF(m.x + startPTTop.x, m.y + startPTTop.y))

                //    Log.d(Constants.TAG, "fillMask = ${fillMask}")

                binding.oculoHealthCl.fillMask = fillMaskTop
                binding.oculoHealthCl.invalidate()

                startPTTop = PointF(v.x, v.y)
            }
            if (m.action == MotionEvent.ACTION_MOVE) {

                // print mask
                if (fillIndexTop > 0) {
                    if (!(fillMaskTop[fillIndexTop].last().x == m.x + startPTTop.x && fillMaskTop[fillIndexTop].last().y == m.y + startPTTop.y))
                        fillMaskTop[fillIndexTop].add(
                            PointF(
                                m.x + startPTTop.x,
                                m.y + startPTTop.y
                            )
                        )
                }

                binding.oculoHealthCl.fillMask = fillMaskTop
                binding.oculoHealthCl.invalidate()

            }
            true
        }


        // ================ FITTING EDIT TEXT BOXES INSIDE DRAWINGS ===============

        binding.imgEditTopRight.setOnClickListener {
            textBoxActiveRight[0] = !textBoxActiveRight[0]
            textBoxActiveRight[1] = false

            if (textBoxActiveRight.contains(true)) {
                binding.editAddTextRight.visibility = View.VISIBLE
                binding.editAddTextRight.setText(binding.extraTextTopRight.text)
            } else {
                binding.editAddTextRight.visibility = View.GONE
                hideKeyBoard(app)
            }
        }

        binding.imgEditTopLeft.setOnClickListener {
            textBoxActiveLeft[0] = !textBoxActiveLeft[0]

            textBoxActiveLeft[1] = false

            if (textBoxActiveLeft.contains(true)) {
                binding.editAddTextLeft.visibility = View.VISIBLE
                binding.editAddTextLeft.setText(binding.extraTextTopLeft.text)
            } else {
                binding.editAddTextLeft.visibility = View.GONE
                hideKeyBoard(app)
            }
        }

        binding.imgEditBottomRight.setOnClickListener {
            textBoxActiveRight[1] = !textBoxActiveRight[1]
            textBoxActiveRight[0] = false

            if (textBoxActiveRight.contains(true)) {
                binding.editAddTextRight.visibility = View.VISIBLE
                binding.editAddTextRight.setText(binding.extraTextBottomRight.text)
            } else {
                binding.editAddTextRight.visibility = View.GONE
                hideKeyBoard(app)
            }
        }

        binding.imgEditBottomLeft.setOnClickListener {
            textBoxActiveLeft[1] = !textBoxActiveLeft[1]

            textBoxActiveLeft[0] = false

            if (textBoxActiveLeft.contains(true)) {
                binding.editAddTextLeft.visibility = View.VISIBLE
                binding.editAddTextLeft.setText(binding.extraTextBottomLeft.text)
            } else {
                binding.editAddTextLeft.visibility = View.GONE
                hideKeyBoard(app)
            }
        }


        // =============== loading data to top text boxes =================

        binding.editAddTextRight.addTextChangedListener(object :
            TextWatcher {
            @SuppressLint("DefaultLocale", "SetTextI18n")
            override fun afterTextChanged(s: Editable) {
                val inputText = s.toString()
                if (inputText.isNotEmpty()) {
                    when (textBoxActiveRight.indexOf(true)) {
                        0 -> binding.extraTextTopRight.text = inputText
                        else -> binding.extraTextBottomRight.text = inputText
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        })

        binding.editAddTextLeft.addTextChangedListener(object :
            TextWatcher {
            @SuppressLint("DefaultLocale", "SetTextI18n")
            override fun afterTextChanged(s: Editable) {
                val inputText = s.toString()
                if (inputText.isNotEmpty()) {
                    when (textBoxActiveLeft.indexOf(true)) {
                        0 -> binding.extraTextTopLeft.text = inputText
                        else -> binding.extraTextBottomLeft.text = inputText
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        })

        // ================= Clear and Undo RIGHT LEFT Buttons ===============

        binding.clearPictureRight.setOnClickListener {
            fillMaskRight[fitIndex].clear()
            fillIndexRight[fitIndex] = -1

            binding.imageFittingRightCl.fillMask = fillMaskRight[fitIndex]
            binding.imageFittingRightCl.invalidate()

        }

        binding.undoRight.setOnClickListener {
            if (fillMaskRight[fitIndex].isNotEmpty()) {
//                fillMaskRight[fitIndex].removeLast()
                fillMaskRight[fitIndex].removeAt(fillMaskRight[fitIndex].lastIndex)
                fillIndexRight[fitIndex]--

                binding.imageFittingRightCl.fillMask = fillMaskRight[fitIndex]
                binding.imageFittingRightCl.invalidate()
            }
        }

        binding.clearPictureLeft.setOnClickListener {
            fillMaskLeft[fitIndex].clear()
            fillIndexLeft[fitIndex] = -1

            binding.imageFittingLeftCl.fillMask = fillMaskLeft[fitIndex]
            binding.imageFittingLeftCl.invalidate()

        }

        binding.undoLeft.setOnClickListener {

            if (fillMaskLeft[fitIndex].isNotEmpty()) {
//                fillMaskLeft[fitIndex].removeLast()
                fillMaskLeft[fitIndex].removeAt(fillMaskLeft[fitIndex].lastIndex)
                fillIndexLeft[fitIndex]--

                binding.imageFittingLeftCl.fillMask = fillMaskLeft[fitIndex]
                binding.imageFittingLeftCl.invalidate()
            }
        }
        // ================= PAINTING ON FITTING IMAGES =====================

        binding.imageFittingRightCl.setOnTouchListener { v, m ->
            if (m.action == MotionEvent.ACTION_DOWN) {
                // add starting point and increase index
                val newMList = mutableListOf<PointF>()
                fillIndexRight[fitIndex]++
                // zero element = color
                newMList.add(PointF(selectedColorFitting.toFloat(), selectedColorFitting.toFloat()))

                //          Log.d(Constants.TAG, "selected color = ${selectedColor}")
                fillMaskRight[fitIndex].add(newMList)
                fillMaskRight[fitIndex][fillIndexRight[fitIndex]].add(
                    PointF(
                        m.x + startPTRight.x,
                        m.y + startPTRight.y
                    )
                )

                //               Log.d(Constants.TAG, "fillMask RIGHT = ${fillMaskRight[FITINDEX]}")

                binding.imageFittingRightCl.fillMask = fillMaskRight[fitIndex]
                binding.imageFittingRightCl.invalidate()

                startPTRight = PointF(v.x, v.y)
            }
            if (m.action == MotionEvent.ACTION_MOVE) {

                // print mask
                if (fillIndexRight[fitIndex] > 0) {
                    if (!(fillMaskRight[fitIndex][fillIndexRight[fitIndex]].last().x == m.x + startPTRight.x && fillMaskRight[fitIndex][fillIndexRight[fitIndex]].last().y == m.y + startPTRight.y))
                        fillMaskRight[fitIndex][fillIndexRight[fitIndex]].add(
                            PointF(
                                m.x + startPTRight.x,
                                m.y + startPTRight.y
                            )
                        )
                }

                binding.imageFittingRightCl.fillMask = fillMaskRight[fitIndex]
                binding.imageFittingRightCl.invalidate()

            }
            true
        }

        binding.imageFittingLeftCl.setOnTouchListener { v, m ->
            if (m.action == MotionEvent.ACTION_DOWN) {
                // add starting point and increase index
                val newMList = mutableListOf<PointF>()
                fillIndexLeft[fitIndex]++
                // zero element = color
                newMList.add(PointF(selectedColorFitting.toFloat(), selectedColorFitting.toFloat()))

                //          Log.d(Constants.TAG, "selected color = ${selectedColor}")
                fillMaskLeft[fitIndex].add(newMList)
                fillMaskLeft[fitIndex][fillIndexLeft[fitIndex]].add(
                    PointF(
                        m.x + startPTLeft.x,
                        m.y + startPTLeft.y
                    )
                )

                // Log.d(Constants.TAG, "fillMask LEFT = ${fillMaskLeft[fitIndex]}")


                binding.imageFittingLeftCl.fillMask = fillMaskLeft[fitIndex]
                binding.imageFittingLeftCl.invalidate()

                startPTLeft = PointF(v.x, v.y)
            }
            if (m.action == MotionEvent.ACTION_MOVE) {

                // print mask
                if (fillIndexLeft[fitIndex] > 0) {
                    if (!(fillMaskLeft[fitIndex][fillIndexLeft[fitIndex]].last().x == m.x + startPTLeft.x && fillMaskLeft[fitIndex][fillIndexLeft[fitIndex]].last().y == m.y + startPTLeft.y))
                        fillMaskLeft[fitIndex][fillIndexLeft[fitIndex]].add(
                            PointF(
                                m.x + startPTLeft.x,
                                m.y + startPTLeft.y
                            )
                        )
                }

                binding.imageFittingLeftCl.fillMask = fillMaskLeft[fitIndex]
                binding.imageFittingLeftCl.invalidate()

            }
            true
        }
        patientViewModel.patientForm.observe(viewLifecycleOwner) { p ->
            currentForm.copyFrom(p)
            patientID = p.patientID

            binding.undoButton.setOnClickListener { fillTheForm(p) }

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
                    if (patientForm == "CONTACT LENS EXAM") {
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

                val hPos = newSectionName.indexOf("CONTACT LENS EXAM")
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

        // DELETE FORM FUNCTIONALITY

        patientViewModel.recordDeleted.observe(viewLifecycleOwner) { ifDeleted ->
            ifDeleted?.let {
                if (ifDeleted) this.findNavController().navigate(
                    ContactLensFragmentDirections.actionToFormSelectionFragment(patientID)
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
                Log.d(Constants.TAG, "Reload CL Form? == ${!currentForm.assertEqual(it)}")
                if (currentForm.recordID == it.recordID && !currentForm.assertEqual(it)) {
                    Log.d(Constants.TAG, "CL Record from FB loaded")
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
                Log.d(Constants.TAG, "CL form CHANGED")
                Log.d(Constants.TAG, "Submiting to FDB record ID ${currentForm.recordID}")
                patientViewModel.submitPatientToFirebase(
                    currentForm.recordID.toString(),
                    currentForm
                )
                // trigger navigation after update
                patientViewModel.updateRecord(currentForm, navOption)
            } else {
                Log.d(Constants.TAG, "CL form the SAME!!!")
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
                ContactLensFragmentDirections.actionToFormSelectionFragment(patientID)
            )

            "home" -> findNavController().navigate(
                ContactLensFragmentDirections.actionToDatabaseSearchFragment()
            )

            else -> navigateToSelectedForm()
        }
    }


    private fun saveAndLoadFitting(newIndex: Int) {
        saveCurrentFittingIntoAr()
        fitIndex = newIndex
        loadCurrentFittingFromAr()
        makeFitSwitchesGrey()

        binding.imageFittingLeftCl.fillMask = fillMaskLeft[fitIndex]
        binding.imageFittingLeftCl.invalidate()
        binding.imageFittingRightCl.fillMask = fillMaskRight[fitIndex]
        binding.imageFittingRightCl.invalidate()
    }

    private fun hideColors() {
        binding.apply {
            imgColorGreen.visibility = View.GONE
            imgColorRed.visibility = View.GONE
            imgColorYellow.visibility = View.GONE
        }
    }

    private fun hideColorsFitting() {
        binding.apply {
            imgColorGreenFitting.visibility = View.GONE
            imgColorRedFitting.visibility = View.GONE
            imgColorYellowFitting.visibility = View.GONE
        }
    }

    private fun makeFitSwitchesGrey() {
        binding.apply {

            fitSwitch1.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greyTint
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            fitSwitch2.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greyTint
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            fitSwitch3.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greyTint
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            fitSwitch4.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greyTint
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }

    private fun showFittingsButtons() {
        binding.apply {
            fitSwitch1.visibility = View.VISIBLE
            fitSwitch2.visibility = View.VISIBLE
            fitSwitch3.visibility = View.VISIBLE
            fitSwitch4.visibility = View.VISIBLE
        }
    }

    private fun hideLayouts() {

        binding.apply {
            layoutPatientHistory.visibility = View.GONE
            layoutOcularHealth.visibility = View.GONE
            layoutFitting.visibility = View.GONE
            layoutFinalPrescriptionCl.visibility = View.GONE

            fitSwitch1.visibility = View.GONE
            fitSwitch2.visibility = View.GONE
            fitSwitch3.visibility = View.GONE
            fitSwitch4.visibility = View.GONE
            patientHistorySwitch.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greyTint
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )

            ocularHealthSwitch.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greyTint
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )

            fittingSwitch.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greyTint
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )

            finalSwitch.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.greyTint
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }

    private fun navigateToSelectedForm() {
        when (navigateFormName) {
            getString(R.string.info_form_caption) -> findNavController().navigate(
                ContactLensFragmentDirections.actionToInfoFragment(navigateFormRecordID)
            )

            getString(R.string.follow_up_form_caption) -> findNavController().navigate(
                ContactLensFragmentDirections.actionToFollowUpFragment(navigateFormRecordID)
            )

            getString(R.string.memo_form_caption) -> findNavController().navigate(
                ContactLensFragmentDirections.actionToMemoFragment(navigateFormRecordID)
            )

            getString(R.string.current_rx_caption) -> findNavController().navigate(
                ContactLensFragmentDirections.actionToCurrentRxFragment(navigateFormRecordID)
            )

            getString(R.string.refraction_caption) -> findNavController().navigate(
                ContactLensFragmentDirections.actionToRefractionFragment(navigateFormRecordID)
            )

            getString(R.string.ocular_health_caption) -> findNavController().navigate(
                ContactLensFragmentDirections.actionToOcularHealthFragment(navigateFormRecordID)
            )

            getString(R.string.supplementary_test_caption) -> findNavController().navigate(
                ContactLensFragmentDirections.actionToSupplementaryFragment(navigateFormRecordID)
            )

            getString(R.string.contact_lens_exam_caption) -> {
                if (recordID != navigateFormRecordID) {
                    recordID = navigateFormRecordID
                    patientViewModel.getPatientForm(navigateFormRecordID)
                }
            }

            getString(R.string.orthox_caption) -> findNavController().navigate(
                ContactLensFragmentDirections.actionToOrthokFragment(navigateFormRecordID)
            )

            getString(R.string.cash_order) -> findNavController().navigate(
                ContactLensFragmentDirections.actionToCashOrderFragment(navigateFormRecordID)
            )

            getString(R.string.sales_order_caption) -> findNavController().navigate(
                ContactLensFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )

            getString(R.string.final_prescription_caption) -> findNavController().navigate(
                ContactLensFragmentDirections.actionToSalesOrderFragment(navigateFormRecordID)
            )

            else -> {
                Toast.makeText(
                    context, "$navigateFormName not implemented yet", Toast.LENGTH_SHORT
                ).show()
            }
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
        if (extractData.size < 138) {
            for (index in extractData.size..138) {
                extractData.add("")
            }
        }

        val graphicsTop = p.reservedField.split('|')

        val graphicsR = p.graphicsRight.split(';')
        val graphicsL = p.graphicsLeft.split(';')

        val screenPxDST = Resources.getSystem().displayMetrics.density
        val widthTop = screenWidthPx().toFloat()
        val heightTop = resources.getDimension(R.dimen.ocular_health_cl_top) * screenPxDST

        val widthRL = widthTop * 0.7f
        val heightRL = resources.getDimension(R.dimen.image_fitting_cl) * screenPxDST


        var widthHeightString =
            if (graphicsTop.isNotEmpty()) graphicsTop[0].split(',') else emptyList()

        var wH = if (widthHeightString.lastIndex == 1) {
            Pair(
                widthHeightString[0].toFloatOrNull() ?: 0f,
                widthHeightString[1].toFloatOrNull() ?: 0f
            )
        } else Pair(0f, 0f)

        val widthRatio = if (wH.first != 0f && widthTop != 0f) widthTop / wH.first else 1f
        val heightRatio = if (wH.second != 0f && heightTop != 0f) heightTop / wH.second else 1f

        fillMaskTop = convertStringToFillMask(graphicsTop, widthRatio, heightRatio)
        fillIndexTop = if (fillMaskTop.size > 0) fillMaskTop.lastIndex else -1

        if (graphicsR.size >= 4) {
            for (index in 0..3) {
                val graphicsRight = graphicsR[index].split('|')
                widthHeightString =
                    if (graphicsRight.isNotEmpty()) graphicsRight[0].split(',') else emptyList()
                wH = if (widthHeightString.lastIndex == 1) {
                    Pair(
                        widthHeightString[0].toFloatOrNull() ?: 0f,
                        widthHeightString[1].toFloatOrNull() ?: 0f
                    )
                } else Pair(0f, 0f)
                val rlWidthRatio = if (wH.first != 0f && widthRL != 0f) widthRL / wH.first else 1f
                val rlHeightRatio =
                    if (wH.second != 0f && heightRL != 0f) heightRL / wH.second else 1f
                fillMaskRight[index] =
                    convertStringToFillMask(graphicsRight, rlWidthRatio, rlHeightRatio)
                fillIndexRight[index] =
                    if (fillMaskRight[index].size > 0) fillMaskRight[index].lastIndex else -1
            }
        }

        if (graphicsL.size >= 4) {
            for (index in 0..3) {
                val graphicsLeft = graphicsL[index].split('|')
                widthHeightString =
                    if (graphicsLeft.isNotEmpty()) graphicsLeft[0].split(',') else emptyList()
                wH = if (widthHeightString.lastIndex == 1) {
                    Pair(
                        widthHeightString[0].toFloatOrNull() ?: 0f,
                        widthHeightString[1].toFloatOrNull() ?: 0f
                    )
                } else Pair(0f, 0f)
                val rlWidthRatio = if (wH.first != 0f && widthRL != 0f) widthRL / wH.first else 1f
                val rlHeightRatio =
                    if (wH.second != 0f && heightRL != 0f) heightRL / wH.second else 1f
                fillMaskLeft[index] =
                    convertStringToFillMask(graphicsLeft, rlWidthRatio, rlHeightRatio)
                fillIndexLeft[index] =
                    if (fillMaskLeft[index].size > 0) fillMaskLeft[index].lastIndex else -1
            }
        }

        binding.apply {

            oculoHealthCl.fillMask = fillMaskTop
            oculoHealthCl.invalidate()


            imageFittingRightCl.fillMask = fillMaskRight[0]
            imageFittingRightCl.invalidate()

            imageFittingLeftCl.fillMask = fillMaskLeft[0]
            imageFittingLeftCl.invalidate()

            //       patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(p.dateOfSection)
            sectionEditDate = p.dateOfSection


            editOccupation.setText(extractData[0])
            editCurrentCl.setText(extractData[1])

            var isEmpty = true
            if (extractData[2].trim() != "") {
                for (i in 0 until spinnerWearing24.adapter.count) {
                    if (extractData[2].trim() == spinnerWearing24.adapter.getItem(i).toString()) {
                        spinnerWearing24.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerWearing24.setSelection(0)

            isEmpty = true
            if (extractData[3].trim() != "") {
                for (i in 0 until spinnerWearing7.adapter.count) {
                    if (extractData[3].trim() == spinnerWearing7.adapter.getItem(i).toString()) {
                        spinnerWearing7.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerWearing7.setSelection(0)

            isEmpty = true
            if (extractData[4].trim() != "") {
                for (i in 0 until spinnerWearing30.adapter.count) {
                    if (extractData[4].trim() == spinnerWearing30.adapter.getItem(i).toString()) {
                        spinnerWearing30.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerWearing30.setSelection(0)

            editCurrentStatus.setText(extractData[5])

            isEmpty = true
            if (extractData[6].trim() != "") {
                for (i in 0 until spinnerVision.adapter.count) {
                    if (extractData[6].trim() == spinnerVision.adapter.getItem(i).toString()) {
                        spinnerVision.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerVision.setSelection(0)

            isEmpty = true
            if (extractData[7].trim() != "") {
                for (i in 0 until spinnerComfort.adapter.count) {
                    if (extractData[7].trim() == spinnerComfort.adapter.getItem(i).toString()) {
                        spinnerComfort.setSelection(i)
                        isEmpty = false
                    }
                }
            }
            if (isEmpty) spinnerComfort.setSelection(0)

            editComplaints.setText(extractData[8])
            editVisualDemand.setText(extractData[9])

            isEmpty = true

            for (i in 0 until spinnerRightSph.adapter.count) {
                if (extractData[10].trim() != "" &&
                    extractData[10] == spinnerRightSph.adapter.getItem(i).toString()
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
                if (extractData[12].trim() != "" &&
                    extractData[12].trim().toDoubleOrNull() == spinnerRightCyl.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerRightCyl.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerRightCyl.setSelection(0)
            isEmpty = true

            for (i in 0 until spinnerLeftSph.adapter.count) {
                if (extractData[11].trim() != "" &&
                    extractData[11] == spinnerLeftSph.adapter.getItem(i).toString()
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
                if (extractData[13].trim() != "" &&
                    extractData[13].trim().toDoubleOrNull() == spinnerLeftCyl.adapter.getItem(i)
                        .toString().toDoubleOrNull()
                ) {
                    spinnerLeftCyl.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerLeftCyl.setSelection(0)

            editRightAxis.setText(extractData[14])
            editLeftAxis.setText(extractData[15])
            editRightVa.setText(extractData[16])
            editLeftVa.setText(extractData[17])

            editCorneaRight.setText(extractData[18])
            editCorneaLeft.setText(extractData[19])
            editLimbusRight.setText(extractData[20])
            editLimbusLeft.setText(extractData[23])

            editLashesRight.setText(extractData[21])
            editLashesLeft.setText(extractData[22])
            editBulbarConjRight.setText(extractData[24])
            editBulbarConjLeft.setText(extractData[27])

            isEmpty = true
            for (i in 0 until spinnerTbutRight.adapter.count) {
                if (extractData[25].trim() != "" &&
                    extractData[25].trim() == spinnerTbutRight.adapter.getItem(i).toString()
                ) {
                    spinnerTbutRight.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerTbutRight.setSelection(0)

            isEmpty = true
            for (i in 0 until spinnerTbutLeft.adapter.count) {
                if (extractData[26].trim() != "" &&
                    extractData[26].trim() == spinnerTbutLeft.adapter.getItem(i).toString()
                ) {
                    spinnerTbutLeft.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerTbutLeft.setSelection(0)

            editLidsRight.setText(extractData[28])
            editLidsLeft.setText(extractData[31])
            editTearQualityRight.setText(extractData[29])
            editTearQualityLeft.setText(extractData[30])

            editRxRight.setText(extractData[32])
            editRxLeft.setText(extractData[38])

            editBcRight.setText(extractData[33])
            editBcLeft.setText(extractData[39])

            editDiaRight.setText(extractData[34])
            editDiaLeft.setText(extractData[40])

            editTaRight.setText(extractData[70])
            editTaLeft.setText(extractData[71])

            editMovementRight.setText(extractData[35])
            editMovementLeft.setText(extractData[41])

            editCentrationRight.setText(extractData[36])
            editCentrationLeft.setText(extractData[42])

            editOverRefractionRight.setText(extractData[37])
            editOverRefractionLeft.setText(extractData[43])

            isEmpty = true
            for (i in 0 until spinnerRightSphFinal.adapter.count) {
                if (extractData[44].trim() != "" &&
                    extractData[44] == spinnerRightSphFinal.adapter.getItem(i).toString()
                ) {
                    spinnerRightSphFinal.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerRightSphFinal.adapter.count) {
                    if (" " == spinnerRightSphFinal.adapter.getItem(i).toString()) {
                        spinnerRightSphFinal.setSelection(i)
                    }
                }
            }

            isEmpty = true

            for (i in 0 until spinnerRightCylFinal.adapter.count) {
                if (extractData[46].trim() != "" &&
                    extractData[46].trim().toDoubleOrNull() == spinnerRightCylFinal.adapter.getItem(
                        i
                    ).toString().toDoubleOrNull()
                ) {
                    spinnerRightCylFinal.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerRightCylFinal.setSelection(0)
            isEmpty = true

            for (i in 0 until spinnerLeftSphFinal.adapter.count) {
                if (extractData[45].trim() != "" &&
                    extractData[45] == spinnerLeftSphFinal.adapter.getItem(i).toString()
                ) {
                    spinnerLeftSphFinal.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) { // set " " as default value
                for (i in 0 until spinnerLeftSphFinal.adapter.count) {
                    if (" " == spinnerLeftSphFinal.adapter.getItem(i).toString()) {
                        spinnerLeftSphFinal.setSelection(i)
                    }
                }
            }
            isEmpty = true

            for (i in 0 until spinnerLeftCylFinal.adapter.count) {
                if (extractData[47].trim() != "" &&
                    extractData[47].trim()
                        .toDoubleOrNull() == spinnerLeftCylFinal.adapter.getItem(i).toString()
                        .toDoubleOrNull()
                ) {
                    spinnerLeftCylFinal.setSelection(i)
                    isEmpty = false
                }
            }
            if (isEmpty) spinnerLeftCylFinal.setSelection(0)

            editRightAxisFinal.setText(extractData[48])
            editLeftAxisFinal.setText(extractData[49])

            editRightBc.setText(extractData[50])
            editLeftBc.setText(extractData[51])

            editRightDia.setText(extractData[52])
            editLeftDia.setText(extractData[53])

            editRightTaFinal.setText(extractData[68])
            editLeftTaFinal.setText(extractData[69])

            editSolution.setText(extractData[54])
            editAdvice.setText(extractData[55])

            editRm1.setText(extractData[56])
            editRm2.setText(extractData[57])
            editRm3.setText(extractData[58])
            editTotal.setText(extractData[59])

            extraTextTop1.text = extractData[60]
            extraTextTop2.text = extractData[61]
            extraTextTop3.text = extractData[62]
            extraTextTop4.text = extractData[63]

            extraTextTopRight.text = extractData[64]
            extraTextBottomRight.text = extractData[65]
            extraTextTopLeft.text = extractData[66]
            extraTextBottomLeft.text = extractData[67]

            editRLens.setText(extractData[127])
            editLLens.setText(extractData[128])
            editLensRight.setText(extractData[129])
            editLensLeft.setText(extractData[130])

            editRemark.setText(p.remarks)

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
        // LOAD FITTINGS DATA
        val fittingForm = mutableListOf(
            extractData[32],
            extractData[33],
            extractData[34],
            extractData[70],
            extractData[35],
            extractData[36],
            extractData[64],
            extractData[65],
            extractData[37],
            extractData[38],
            extractData[39],
            extractData[40],
            extractData[71],
            extractData[41],
            extractData[42],
            extractData[66],
            extractData[67],
            extractData[43],
            extractData[129],
            extractData[130]
        )

        fittingFormsAr.clear()
        fittingFormsAr.add(fittingForm)
        for (index in 0..2) {
            val nextFitting = mutableListOf<String>()
            for (jot in 1..18) {
                val element = extractData[71 + index * 18 + jot]
                nextFitting.add(element)
            }
            nextFitting.add(extractData[131 + index * 2])
            nextFitting.add(extractData[131 + index * 2 + 1])

            fittingFormsAr.add(nextFitting)
        }
    }

    private fun formWasChanged(): Boolean {

        val priorPatient = currentForm.copy()

        // create new Record, fill it in with Form data and pass to ViewModel with recordID to update DB
        val screenPxDST = Resources.getSystem().displayMetrics.density
        val widthTop = screenWidthPx()
        val heightTop = (resources.getDimension(R.dimen.ocular_health_cl_top) * screenPxDST).toInt()

        val widthRL = (widthTop * 0.7).toInt()
        val heightRL = (resources.getDimension(R.dimen.image_fitting_cl) * screenPxDST).toInt()

        val graphicsTop = convertFillMask(fillMaskTop, widthTop, heightTop)


        var graphicsRight = ""
        for (index in 0..3) {
            graphicsRight += convertFillMask(fillMaskRight[index], widthRL, heightRL)
            graphicsRight += ";"
        }

        var graphicsLeft = ""
        for (index in 0..3) {
            graphicsLeft += convertFillMask(fillMaskLeft[index], widthRL, heightRL)
            graphicsLeft += ";"
        }

        /*        Log.d("Orthok_Fragment", "on save: widthTop = $widthTop, heightTop = $heightTop")
                Log.d("Orthok_Fragment", "on save: widthRL = $widthRL, heightRL = $heightRL")*/

        binding.apply {

            saveCurrentFittingIntoAr()

            var fittingElement = ""
            for (index in 1..3) {
                for (jot in 0..17) {
                    fittingElement += fittingFormsAr[index][jot]
                    fittingElement += "|"
                }
            }
            var secondFittingElement = ""
            for (index in 0..3) {
                for (jot in 18..19) {
                    secondFittingElement += fittingFormsAr[index][jot]
                    secondFittingElement += "|"
                }
            }

            currentForm.graphicsRight = graphicsRight
            currentForm.graphicsLeft = graphicsLeft
            currentForm.reservedField = graphicsTop

            currentForm.remarks = editRemark.text.toString().uppercase(Locale.getDefault())

            if (sectionEditDate != -1L) currentForm.dateOfSection = sectionEditDate

            val extractData = editOccupation.text.toString() + "|" +
                    editCurrentCl.text.toString() + "|" +
                    spinnerWearing24.selectedItem.toString() + "|" +
                    spinnerWearing7.selectedItem.toString() + "|" +
                    spinnerWearing30.selectedItem.toString() + "|" +
                    editCurrentStatus.text.toString() + "|" +
                    spinnerVision.selectedItem.toString() + "|" +
                    spinnerComfort.selectedItem.toString() + "|" +
                    editComplaints.text.toString() + "|" +
                    editVisualDemand.text.toString() + "|" +
                    spinnerRightSph.selectedItem.toString() + "|" +
                    spinnerLeftSph.selectedItem.toString() + "|" +
                    spinnerRightCyl.selectedItem.toString() + "|" +
                    spinnerLeftCyl.selectedItem.toString() + "|" +
                    editRightAxis.text.toString() + "|" +
                    editLeftAxis.text.toString() + "|" +
                    editRightVa.text.toString() + "|" +
                    editLeftVa.text.toString() + "|" +
                    editCorneaRight.text.toString() + "|" +
                    editCorneaLeft.text.toString() + "|" +
                    editLimbusRight.text.toString() + "|" +
                    editLashesRight.text.toString() + "|" +
                    editLashesLeft.text.toString() + "|" +
                    editLimbusLeft.text.toString() + "|" +
                    editBulbarConjRight.text.toString() + "|" +
                    spinnerTbutRight.selectedItem.toString() + "|" +
                    spinnerTbutLeft.selectedItem.toString() + "|" +
                    editBulbarConjLeft.text.toString() + "|" +
                    editLidsRight.text.toString() + "|" +
                    editTearQualityRight.text.toString() + "|" +
                    editTearQualityLeft.text.toString() + "|" +
                    editLidsLeft.text.toString() + "|" +
                    fittingFormsAr[0][0] + "|" +  // FItting Zero
                    fittingFormsAr[0][1] + "|" +
                    fittingFormsAr[0][2] + "|" +
                    fittingFormsAr[0][4] + "|" +
                    fittingFormsAr[0][5] + "|" +
                    fittingFormsAr[0][8] + "|" +
                    fittingFormsAr[0][9] + "|" +
                    fittingFormsAr[0][10] + "|" +
                    fittingFormsAr[0][11] + "|" +
                    fittingFormsAr[0][13] + "|" +
                    fittingFormsAr[0][14] + "|" +
                    fittingFormsAr[0][17] + "|" +
                    spinnerRightSphFinal.selectedItem.toString() + "|" +
                    spinnerLeftSphFinal.selectedItem.toString() + "|" +
                    spinnerRightCylFinal.selectedItem.toString() + "|" +
                    spinnerLeftCylFinal.selectedItem.toString() + "|" +
                    editRightAxisFinal.text.toString() + "|" +
                    editLeftAxisFinal.text.toString() + "|" +
                    editRightBc.text.toString() + "|" +
                    editLeftBc.text.toString() + "|" +
                    editRightDia.text.toString() + "|" +
                    editLeftDia.text.toString() + "|" +
                    editSolution.text.toString() + "|" +
                    editAdvice.text.toString() + "|" +
                    editRm1.text.toString() + "|" +
                    editRm2.text.toString() + "|" +
                    editRm3.text.toString() + "|" +
                    editTotal.text.toString() + "|" +
                    extraTextTop1.text + "|" +
                    extraTextTop2.text + "|" +
                    extraTextTop3.text + "|" +
                    extraTextTop4.text + "|" +
                    fittingFormsAr[0][6] + "|" +
                    fittingFormsAr[0][7] + "|" +
                    fittingFormsAr[0][15] + "|" +
                    fittingFormsAr[0][16] + "|" +
                    editRightTaFinal.text.toString() + "|" +
                    editLeftTaFinal.text.toString() + "|" +
                    fittingFormsAr[0][3] + "|" +
                    fittingFormsAr[0][12] + "|" + fittingElement + "|" +
                    editRLens.text.toString() + "|" +
                    editLLens.text.toString() + "|" + secondFittingElement

            currentForm.sectionData = extractData.uppercase()

            currentForm.practitioner = (binding.practitionerName.selectedItem as String).uppercase()
        }

        return !currentForm.assertEqual(priorPatient)
    }

    private fun saveCurrentFittingIntoAr() {
        binding.apply {
            if (fittingFormsAr.isNotEmpty()) {
                fittingFormsAr[fitIndex][0] = editRxRight.text.toString()
                fittingFormsAr[fitIndex][1] = editBcRight.text.toString()
                fittingFormsAr[fitIndex][2] = editDiaRight.text.toString()
                fittingFormsAr[fitIndex][3] = editTaRight.text.toString()
                fittingFormsAr[fitIndex][4] = editMovementRight.text.toString()
                fittingFormsAr[fitIndex][5] = editCentrationRight.text.toString()
                fittingFormsAr[fitIndex][6] = extraTextTopRight.text.toString()
                fittingFormsAr[fitIndex][7] = extraTextBottomRight.text.toString()
                fittingFormsAr[fitIndex][8] = editOverRefractionRight.text.toString()

                fittingFormsAr[fitIndex][9] = editRxLeft.text.toString()
                fittingFormsAr[fitIndex][10] = editBcLeft.text.toString()
                fittingFormsAr[fitIndex][11] = editDiaLeft.text.toString()
                fittingFormsAr[fitIndex][12] = editTaLeft.text.toString()
                fittingFormsAr[fitIndex][13] = editMovementLeft.text.toString()
                fittingFormsAr[fitIndex][14] = editCentrationLeft.text.toString()
                fittingFormsAr[fitIndex][15] = extraTextTopLeft.text.toString()
                fittingFormsAr[fitIndex][16] = extraTextBottomLeft.text.toString()
                fittingFormsAr[fitIndex][17] = editOverRefractionLeft.text.toString()
                fittingFormsAr[fitIndex][18] = editLensRight.text.toString()
                fittingFormsAr[fitIndex][19] = editLensLeft.text.toString()

            }
        }
    }

    private fun loadCurrentFittingFromAr() {
        binding.apply {
            if (fittingFormsAr.isNotEmpty()) {
                editRxRight.setText(fittingFormsAr[fitIndex][0])
                editBcRight.setText(fittingFormsAr[fitIndex][1])
                editDiaRight.setText(fittingFormsAr[fitIndex][2])
                editTaRight.setText(fittingFormsAr[fitIndex][3])
                editMovementRight.setText(fittingFormsAr[fitIndex][4])
                editCentrationRight.setText(fittingFormsAr[fitIndex][5])
                extraTextTopRight.text = fittingFormsAr[fitIndex][6]
                extraTextBottomRight.text = fittingFormsAr[fitIndex][7]
                editOverRefractionRight.setText(fittingFormsAr[fitIndex][8])

                editRxLeft.setText(fittingFormsAr[fitIndex][9])
                editBcLeft.setText(fittingFormsAr[fitIndex][10])
                editDiaLeft.setText(fittingFormsAr[fitIndex][11])
                editTaLeft.setText(fittingFormsAr[fitIndex][12])
                editMovementLeft.setText(fittingFormsAr[fitIndex][13])
                editCentrationLeft.setText(fittingFormsAr[fitIndex][14])
                extraTextTopLeft.text = fittingFormsAr[fitIndex][15]
                extraTextBottomLeft.text = fittingFormsAr[fitIndex][16]
                editOverRefractionLeft.setText(fittingFormsAr[fitIndex][17])
                editLensRight.setText(fittingFormsAr[fitIndex][18])
                editLensLeft.setText(fittingFormsAr[fitIndex][19])
            }
        }
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