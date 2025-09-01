package com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.forms

import android.annotation.SuppressLint
import android.app.Application
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.lizpostudio.kgoptometrycrm.R
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.databinding.ContentOcularHealthBinding
import com.lizpostudio.kgoptometrycrm.search.recycle_bin.details.DetailActivity
import com.lizpostudio.kgoptometrycrm.utils.*
import id.xxx.module.view.binding.ktx.viewBinding

class OcularHealthFragment : Fragment() {

    private var fillMask = mutableListOf<MutableList<PointF>>()
    private var fillMaskBottom = mutableListOf<MutableList<PointF>>()
    private var fillIndex = -1
    private var fillIndexBottom = -1

    private val binding by viewBinding<ContentOcularHealthBinding>()

    private var topOculusVisible = true
    private var bottomOculusVisible = true

    private var sectionEditDate = -1L

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

        var selectedColor = ContextCompat.getColor(requireContext(), R.color.greenCircle)
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val intent = requireActivity().intent
        val patient =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(
                    DetailActivity.EXTRA_NAME_PATIENT,
                    PatientEntity::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(DetailActivity.EXTRA_NAME_PATIENT) as? PatientEntity
            }
        if (patient == null)
            return

        setPatientName(patient)
        setPractitionerName(patient.practitioner)
        fillTheForm(patient)
    }

    private fun setPatientName(patient: PatientEntity) {
        var pAge = patient.patientName
        val ic = patient.patientIC
        val (dob, age) = computeAgeAndDOB(ic)

        pAge += resources.getString(R.string.number_of_years_patient, age, dob)
        binding.patientName.text = pAge
    }

    private fun setPractitionerName(name: String) {
        val adapterPractitioner =
            ArrayAdapter(
                requireContext(),
                R.layout.spinner_list_basic_,
                listOf(name)
            )
        binding.practitionerName.adapter = adapterPractitioner
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
    private fun fillTheForm(patientForm: PatientEntity) {

        val extractData = patientForm.sectionData.split('|').toMutableList()
//      Log.d(Constants.TAG, "extract data size before = ${extractData.size}")
        if (extractData.size < 20) {
            for (index in extractData.size..20) {
                extractData.add("")
            }
        }

        val graphicsTop = patientForm.graphicsRight.split('|')
        val graphicsBottom = patientForm.graphicsLeft.split('|')

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

            bottomOculus.fillMask = fillMaskBottom
            bottomOculus.invalidate()

            topOculus.fillMask = fillMask
            topOculus.invalidate()

            //        patientName.text = patientForm.patientName
            dateCaption.text = convertLongToDDMMYY(patientForm.dateOfSection)
            sectionEditDate = patientForm.dateOfSection
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

            remarkInput.setText(patientForm.remarks)
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
