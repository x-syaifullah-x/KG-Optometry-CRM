package com.lizpostudio.kgoptometrycrm

import org.junit.Test

class ExampleUnitTest {

    private val dataList = listOf<Any>(1, 2)

    @Test
    fun addition_isCorrect() {
        val isDate = "30/08/22"
        val result = isDate.matches(Regex("\\d{2}/\\d{2}/\\d{2}"))
        println(dataList[2])
    }
}