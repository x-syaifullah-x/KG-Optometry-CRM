package com.lizpostudio.kgoptometrycrm.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.Log
import com.lizpostudio.kgoptometrycrm.constant.Constants
import com.lizpostudio.kgoptometrycrm.data.repository.PatientRepository
import com.lizpostudio.kgoptometrycrm.data.source.local.AppDatabase
import com.lizpostudio.kgoptometrycrm.data.source.local.entity.PatientEntity
import com.lizpostudio.kgoptometrycrm.data.source.remote.firebase.FBRecords
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.random.Random

private const val ONE_DAY = 24 * 3600 * 1000L

fun convertFBRecordToPatients(f: FBRecords, key: Long): PatientEntity {
    val sectionData = f.sectionData.split("|")
    return PatientEntity(
        key,
        isReadOnly = f.isReadOnly,
        patientID = f.patientID,
        patientIC = f.patientIC,
        patientName = f.patientName,
        phone = f.phone,
        address = f.address,
        familyCode = f.familyCode,
        dateOfSection = f.dateOfSection.toLongOrNull() ?: 0L,
        sectionName = f.sectionName,
        sectionData = f.sectionData,
        remarks = f.remarks,
        graphicsLeft = f.graphicsLeft,
        graphicsRight = f.graphicsRight,
        syncStatus = f.syncStatus == "true",
        reservedField = f.reservedField,
        practitioner = f.practitioner,
        mm = f.mm,
        or = f.or,
        frameSize = f.frameSize,
        frameType = f.frameType,
        cs = f.cs,
        solutionMisc = f.solutionMisc,
        solutionMiscRm = f.solutionMiscRm,
        frame = try {
            when (f.sectionName) {
                "FINAL PRESCRIPTION", "CASH ORDER" -> sectionData?.getOrNull(15) ?: ""
                else -> ""
            }
        } catch (e: IndexOutOfBoundsException) {
            // Handle specific exception if needed
            ""
        } ?: "",
        lens = try {
            when (f.sectionName) {
                "FINAL PRESCRIPTION", "CASH ORDER" -> sectionData?.getOrNull(17) ?: ""
                else -> ""
            }
        } catch (e: IndexOutOfBoundsException) {
            // Handle specific exception if needed
            ""
        } ?: "",
        contactLensSunglasses = try {
            when (f.sectionName) {
                "FINAL PRESCRIPTION", "CASH ORDER" -> sectionData?.getOrNull(19) ?: ""
                else -> ""
            }
        } catch (e: IndexOutOfBoundsException) {
            // Handle specific exception if needed
            ""
        } ?: "",
        practitionerNameOptometrist = f.practitionerNameOptometrist,
        remarkPrint = f.remarkPrint,
        followUpText = f.followUpText,
        deleteAt = f.deleteAt,
        axialLengthRight = f.axialLengthRight,
        axialLengthLeft = f.axialLengthLeft,
        cstotal = f.cstotal,
        ortotal = f.ortotal,
        cspractitioner = f.cspractitioner,
        orpractitioner = f.orpractitioner,
        tin = f.tin,
    )
}

fun convertFormToFBRecord(p: PatientEntity): FBRecords {
    return FBRecords(
//        isReadOnly = p.isReadOnly,
        address = p.address,
        dateOfSection = p.dateOfSection.toString(),
        familyCode = p.familyCode,
        graphicsLeft = p.graphicsLeft,
        graphicsRight = p.graphicsRight,
        patientIC = p.patientIC,
        patientID = p.patientID,
        patientName = p.patientName,
        phone = p.phone,
        remarks = p.remarks,
        reservedField = p.reservedField,
        sectionData = p.sectionData,
        sectionName = p.sectionName,
        syncStatus = "true",
        practitioner = p.practitioner,
        mm = p.mm,
        or = p.or,
        frameSize = p.frameSize,
        frameType = p.frameType,
        cs = p.cs,
        solutionMisc = p.solutionMisc,
        solutionMiscRm = p.solutionMiscRm,
        practitionerNameOptometrist = p.practitionerNameOptometrist,
        remarkPrint = p.remarkPrint,
        followUpText = p.followUpText,
        deleteAt = p.deleteAt,
        axialLengthLeft = p.axialLengthLeft,
        axialLengthRight = p.axialLengthRight,
        cstotal = p.cstotal,
        ortotal = p.ortotal,
        cspractitioner = p.cspractitioner,
        orpractitioner = p.orpractitioner,
        tin = p.tin,
    )
}

fun dayMonthY(): Triple<Int, Int, Int> {
    val today = Calendar.getInstance()
    val todayYear = today.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH)
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    return Triple(todayYear, todayMonth, todayDay)
}

/**
 * Converting from string array to PointF
 */


fun convertStringToFillMask(
    inputAr: List<String>,
    widthRatio: Float = 1f,
    heightRatio: Float = 1f
): MutableList<MutableList<PointF>> {

    var firstIndex = -1
    val fillMask = mutableListOf<MutableList<PointF>>()
    if (inputAr.size > 1) {
        for (index in 1..inputAr.lastIndex) {
            firstIndex++
            fillMask.add(mutableListOf())
            val element = inputAr[index].split(',')
            if (element.isNotEmpty()) {
                for (points in 0 until element.lastIndex step 2) {

                    val pointPosition = PointF(
                        element[points].toFloatOrNull() ?: 0.0f,
                        element[points + 1].toFloatOrNull() ?: 0.0f
                    )
                    if (points != 0) {
                        pointPosition.x *= widthRatio
                        pointPosition.y *= heightRatio
                    }

                    fillMask[firstIndex].add(pointPosition)
                }
            }

        }
    }
    return fillMask
}

/**
 * Converting PointF array of drawing vector coordinates
 * to one dimension string array to be stored at database
 * Vector graphic in a way
 */

fun convertFillMask(fillMask: List<List<PointF>>, width: Int = 0, height: Int = 0): String {
    // making 1 dimension string array from 2 dimensions vector coordinates


    var vectorDrawingAr = "$width,$height|"

    if (fillMask.isNotEmpty()) {
        for (index in 0..fillMask.lastIndex) {
            if (fillMask[index].isNotEmpty()) {
                for (drawIndex in 0..fillMask[index].lastIndex) {
                    vectorDrawingAr += (fillMask[index][drawIndex].x).toInt().toString()
                    vectorDrawingAr += ","
                    vectorDrawingAr += (fillMask[index][drawIndex].y).toInt().toString()
                    vectorDrawingAr += ","
                }
                if (vectorDrawingAr.length > 1) vectorDrawingAr =
                    (vectorDrawingAr.substring(0, vectorDrawingAr.length - 1))
                vectorDrawingAr += "|"
            }
        }
        //     if (vectorDrawingAr.length > 1) vectorDrawingAr = (vectorDrawingAr.substring(0, vectorDrawingAr.length - 1))
    }
    if (vectorDrawingAr.last() == '|') vectorDrawingAr =
        (vectorDrawingAr.substring(0, vectorDrawingAr.length - 1))

    /*    for(index in 0 .. vectorDrawingAr.lastIndex) {
               Log.d(TAG, "${vectorDrawingAr[index]} \n")
           }*/
    return vectorDrawingAr
}

/**
 * Creates string list with values "", "0", "1", ... "max" - for spinner
 */
fun createNumbersList(max: Int): MutableList<String> {
    val list = mutableListOf<String>()
    list.add("")
    var add = 0
    while (add <= max) {
        list.add("$add")
        add++
    }
    return list
}

fun iopList(): MutableList<String> {

    val list = mutableListOf<String>()
    list.add("")
    list.add("<5")
    var add = 5
    while (add < 30) {
        list.add("$add")
        add++
    }
    list.add(">30")
    return list
}

fun cdRatioList(): MutableList<String> {
    val df = DecimalFormat("#0.00")
    val list = mutableListOf<String>()
    list.add("")
    var add = 0.05
    while (add < 1.0) {
        list.add(df.format(add))
        add += 0.05
    }
    return list
}

fun tBUTList(): MutableList<String> {

    val list = mutableListOf<String>()
    list.add("")
    var add = 1
    while (add < 11) {
        list.add("${add}S")
        add++
    }
    list.add(">10S")
    return list
}

fun addList(): MutableList<String> {
    val df = DecimalFormat("#0.00")
    val list = mutableListOf<String>()
    list.add("")
    var add = 0.25
    while (add < 4.25) {
        list.add("+${df.format(add)}")
        add += 0.25
    }
    return list
}

fun cylList(): MutableList<String> {
    val df = DecimalFormat("#0.00")
    val list = mutableListOf<String>()
    list.add("")
    var cyl = -0.25
    while (cyl > -10.25) {
        list.add(df.format(cyl))
        cyl -= 0.25
    }
    return list
}

fun sphList(): MutableList<String> {
    val df = DecimalFormat("#0.00")
    var sph = 15.00
    val list = mutableListOf<String>()
    while (sph > 0) {
        list.add("+${df.format(sph)}")
        sph -= 0.25
    }
    list.add("PL")
    list.add(" ") // default selection
    sph = -0.25
    while (sph > -20.25) {
        list.add(df.format(sph))
        sph -= 0.25
    }
    return list
}

/** takes year, month, day  and returns new time in millis
 * corresponding to date (year, month, day) and time of selectedTime
 */
@SuppressLint("SimpleDateFormat")
fun convertYMDtoTimeMillis(year: Int, month: Int, day: Int): Long {

    return if ((year in 1801..2499) && (month in 0..11) && (day in 1..31)) {
        val parser = SimpleDateFormat("yyyy-MM-dd")
        val source = year.toString() + "-" + (month + 1).toString() + "-" + day.toString()
        val output = parser.parse(source)
        output?.time ?: 0L
    } else 0L
}

fun computeAgeAndDOB(ic: String): Pair<String, String> {
    var dob = ""
    //       var birthInMillis = 0L
    var age = ""

    if (ic.length > 5) {
        val year = ic.substring(0, 2)
        val month = ic.substring(2, 4)
        val day = ic.substring(4, 6)
        dob = "$day/$month/$year"
        val intYear = year.toIntOrNull()
        var finalYear = intYear ?: -1000

        val today: Calendar = Calendar.getInstance()
        val todayYEAR = today.get(Calendar.YEAR) - 2000

        finalYear += if (finalYear > todayYEAR) 1900 else 2000

        val intMonth = month.toIntOrNull()
        val finalMonth = intMonth ?: -1000

        val intDay = day.toIntOrNull()
        val finalDay = intDay ?: -1000

        if (finalDay in 1 until 32 && finalMonth in 0..12 && finalYear in 1900..2100)
            age = getAge(finalYear, finalMonth, finalDay)

    }
    return Pair(dob, age)
}


fun getAgeFromIC(ic: String): String {

    var age = ""

    if (ic.length > 5) {
        val year = ic.substring(0, 2)
        val month = ic.substring(2, 4)
        val day = ic.substring(4, 6)

        val intYear = year.toIntOrNull()
        var finalYear = intYear ?: -1000

        val today: Calendar = Calendar.getInstance()
        val todayYEAR = today.get(Calendar.YEAR) - 2000

        finalYear += if (finalYear > todayYEAR) 1900 else 2000

        val intMonth = month.toIntOrNull()
        val finalMonth = intMonth ?: -1000

        val intDay = day.toIntOrNull()
        val finalDay = intDay ?: -1000

        if (finalDay in 1 until 32 && finalMonth in 0..12 && finalYear in 1900..2100)
            age = getAge(finalYear, finalMonth, finalDay)

    }
    return age
}

fun getAge(year: Int, month: Int, day: Int): String {
    val dob: Calendar = Calendar.getInstance()
    val today: Calendar = Calendar.getInstance()
    dob.set(year, month - 1, day)

    val dobDay = dob.get(Calendar.DAY_OF_YEAR)
    val todayDay = today.get(Calendar.DAY_OF_YEAR)

    var age: Int = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
    if (todayDay < dobDay) {
        age--
    }
    val ageInt = age
    return ageInt.toString()
}

private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')

fun generateID(repository: PatientRepository): String {
    val id = generateID()
    return if (repository.idIsExist(id)) {
        generateID(repository)
    } else {
        id
    }
}

fun generateID(context: Context): String {
    val id = generateID()
    val isExist = AppDatabase.getInstance(context).patientsDao.idIsExist(id)
    return if (isExist) {
        generateID(context)
    } else {
        id
    }
}

fun generateID(): String {
    return (1..10)
        .map { i -> Random.nextInt(i, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}

// used format dd/mm/yy
@SuppressLint("SimpleDateFormat")
fun getDateStartAEndMillis(date: String): Pair<Long, Long> {

    val parser = SimpleDateFormat("dd/MM/yy")
    var dateStart = 0L
    var dateEnd = 0L
    try {
        dateStart = parser.parse(date.trim())?.time ?: 0L
        dateEnd = dateStart + ONE_DAY
    } catch (e: Exception) {
        Log.d(Constants.TAG, "${e.message}")
    }

    return Pair(dateStart, dateEnd)
}

@SuppressLint("SimpleDateFormat")
fun convertLongToDDMMYY(timeMillis: Long): String {
    return SimpleDateFormat("dd/MM/yy").format(timeMillis).toString()
}

@SuppressLint("SimpleDateFormat")
fun convertLongToDDMMYY(timeMillis: String): String {
    return SimpleDateFormat("dd/MM/yy")
        .format(timeMillis).toString()
}

@SuppressLint("SimpleDateFormat")
fun convertLongToDDKey(timeMillis: Long): String {
    return SimpleDateFormat("dd_MM_yy_hh_mm")
        .format(timeMillis).toString()
}

@Suppress("FunctionName")
@SuppressLint("SimpleDateFormat")
fun convertLongTodd_MM_yy_hh_mm_ss(timeMillis: Long): String {
    return SimpleDateFormat("dd_MM_yy_hh_mm_ss")
        .format(timeMillis).toString()
}

@Suppress("FunctionName")
@SuppressLint("SimpleDateFormat")
fun convertTo_dd_MM_yy_hh_mm_a(timeMillis: Long): String {
    return SimpleDateFormat("dd/MM/yy hh:mm a")
        .format(timeMillis).toString()
}

@SuppressLint("SimpleDateFormat")
fun convertDDMMYYtoTimeMillis(inputDate: String): Long {
    val parser = SimpleDateFormat("dd/MM/yy")
    return if (inputDate.length > 6) parser.parse(inputDate.trim())?.time ?: 0L else 0L
}